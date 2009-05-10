package grouphug.modules;

import grouphug.Grouphug;
import grouphug.SQL;
import grouphug.GrouphugModule;

import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Random;

/**
 * The Factoid module lets a user save two strings, one which the bot should react upon
 * and a corresponding string which it should reply with.
 *
 * Instead of connecting to SQL and fetching all the triggers each time someone sends something
 * to the channel, we maintain a local list in memory, and just keep the SQL db synchronized, so
 * that on startup we can fetch all the factoids back.
 */
public class Factoid implements GrouphugModule {

    // TODO - dynamic reply, with username, more?
    // TODO - more details of factiod ? time ?

    private ArrayList<FactoidItem> factoids = new ArrayList<FactoidItem>();

    private static final String TRIGGER_HELP = "factoid";

    private static final String TRIGGER_MAIN = "factoid ";
    private static final String TRIGGER_RANDOM = "randomfactoid";

    private static final String TRIGGER_MAIN_ADD = "<on> ";
    private static final String TRIGGER_MAIN_DEL = "<forget> ";

    private static final String TRIGGER_SHORT_ADD = "on ";
    private static final String TRIGGER_SHORT_DEL = "forget ";

    private static final String SEPARATOR_MESSAGE = " <say> ";
    private static final String SEPARATOR_ACTION = " <do> ";

    private static Random random = new Random(System.nanoTime());

    private long lastAddedTime; // HACK to avoid specialTrigger() being called on the same line

    public Factoid() {
        // Load up all existing factoids from sql
        SQL sql = new SQL();
        try {
            sql.connect();
            sql.query("SELECT `type`, `trigger`, `reply`, `author` FROM gh_factoid;");
            while(sql.getNext()) {
                Object[] values = sql.getValueList();
                boolean message = values[0].equals("message");
                factoids.add(new FactoidItem(message, (String)values[1], (String)values[2], (String)values[3]));
            }
        } catch(SQLSyntaxErrorException e) {
            System.err.println("Factoid startup: SQL Syntax error: "+e);
        } catch(SQLException e) {
            System.err.println("Factoid startup: SQL Exception: "+e);
        } finally {
            sql.disconnect();
        }
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            return "Factoid: Make me say or do \"reply\" when someone says \"trigger\".\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_SHORT_ADD + "trigger <say> reply\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_SHORT_ADD + "trigger <do> reply\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_SHORT_DEL + "trigger\n" +
                   "Other triggers, to view info, or get a random factoid:\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_MAIN + "trigger\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_RANDOM + "\n" +
                   " - The string \"$sender\" will be replaced with the nick of the one triggering the factoid.\n" +
                   " - A star (*) can be any string of characters.\n" +
                   " - Regex can be used, but 1) remember that * is replaced with .* and 2) this will probably change very soon.";
        }
        return null;
    }


    // Remember that this is only run on a line starting with the Grouphug.MAIN_TRIGGER (! at the time of writing) (
    public void trigger(String channel, String sender, String login, String hostname, String message) {

        // If trying to ADD a NEW factoid (with the main trigger !factoid <on> or the shorttrigger !on)
        if(message.startsWith(TRIGGER_MAIN + TRIGGER_MAIN_ADD) || message.startsWith(TRIGGER_SHORT_ADD) && !sender.equalsIgnoreCase("Twst") && !sender.equalsIgnoreCase("Twisty")){

            // First parse the line to find the trigger, reply, and if it's a message or action
            // Do this based on what kind of trigger that was used
            String line;
            if(message.startsWith(TRIGGER_MAIN + TRIGGER_MAIN_ADD))
                line = message.substring(TRIGGER_MAIN.length()+TRIGGER_MAIN_ADD.length());
            else
                line = message.substring(TRIGGER_SHORT_ADD.length());

            boolean replyMessage;
            String trigger, reply;

            if(line.contains(SEPARATOR_MESSAGE)) {
                replyMessage = true;
                trigger = line.substring(0, line.indexOf(SEPARATOR_MESSAGE));
                reply = line.substring(line.indexOf(SEPARATOR_MESSAGE) + SEPARATOR_MESSAGE.length());
            } else if(line.contains(SEPARATOR_ACTION)) {
                replyMessage = false;
                trigger = line.substring(0, line.indexOf(SEPARATOR_ACTION));
                reply = line.substring(line.indexOf(SEPARATOR_ACTION) + SEPARATOR_ACTION.length());
            } else {
                // If it's neither a message nor an action
                Grouphug.getInstance().sendMessage("What? Don't give me that nonsense, "+sender+".", false);
                return;
            }

            // add() returns true if the factoid is added, or false if the trigger is already taken
            if(add(replyMessage, trigger, reply, sender)) {
                Grouphug.getInstance().sendMessage("OK, "+sender+".", false);

                lastAddedTime = System.nanoTime(); // HACK to avoid specialTrigger() being called on the same line
            } else {
                Grouphug.getInstance().sendMessage("But, "+sender+", "+trigger+".", false);
            }

        }
        // Not trying to ADD a new factoid, so check if we're trying to REMOVE one
        else if(message.startsWith(TRIGGER_MAIN + TRIGGER_MAIN_DEL) || message.startsWith(TRIGGER_SHORT_DEL)) {

            // Parse the line, based on what kind of trigger that was used
            String trigger;
            if(message.startsWith(TRIGGER_MAIN + TRIGGER_MAIN_DEL))
                trigger = message.substring(TRIGGER_MAIN.length()+ TRIGGER_MAIN_DEL.length());
            else
                trigger = message.substring(TRIGGER_SHORT_DEL.length());

            // and try to remove it - del() returns true if it's removed, false if there is no such trigger
            if(del(trigger)) {
                Grouphug.getInstance().sendMessage("I no longer know of this "+trigger+" that you speak of.", false);
            } else {
                Grouphug.getInstance().sendMessage(sender+", I can't remember "+trigger+" in the first place.", false);
            }

        }
        // Ok, neither ADDing nor REMOVING, so check if we're just trying to see data about a factoid
        else if(message.startsWith(TRIGGER_MAIN)) {
            FactoidItem factoid;
            String trigger = message.substring(TRIGGER_MAIN.length());
            if((factoid = find(trigger, false)) != null) {
                Grouphug.getInstance().sendMessage("Factoid: [ trigger = "+factoid.getTrigger()+" ] [ reply = "+factoid.getReply()+" ] [ author = "+factoid.getAuthor()+" ]", false);
            } else {
                Grouphug.getInstance().sendMessage(sender+", I do not know of this "+trigger+" that you speak of.", false);
            }

        }
        // The last triggered alternative would be the trigger for getting a random factoid
        else if(message.startsWith(TRIGGER_RANDOM)) {
            factoids.get(random.nextInt(factoids.size())).send(sender);
        }
    }

    // this is run for every message sent to the channel - it checks if the line matches any factoid
    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {

        if ((System.nanoTime() - lastAddedTime) < (2 * Math.pow(10.0,9.0))) {
            return; // HACK to avoid specialTrigger being called on the same line used to add the trigger in the first place
        }

        FactoidItem factoid;
        if((factoid = find(message, true)) != null) {
            factoid.send(sender);
        }
    }

    /**
     * Attempts to add a new factoid.
     * @param message true if we should reply with a message, false if we should reply with an action
     * @param trigger The trigger upon which we will react
     * @param reply The reply corresponding to the trigger
     * @param author The nick of the person adding the factoid
     * @return true if the factoid was successfully added, or false if the trigger already exists
     */
    private boolean add(boolean message, String trigger, String reply, String author) {

        if(find(trigger, false) != null) {
            return false;
        }

        // First add the new item to the SQL db
        SQL sql = new SQL();
        try {
            sql.connect();
            sql.query("INSERT INTO gh_factoid (`type`, `trigger`, `reply`, `author`) VALUES ('"+(message ? "message" : "action")+"', '"+trigger+"', '"+reply+"', '"+author+"');");
        } catch(SQLSyntaxErrorException e) {
            System.err.println("Factoid insertion: SQL Syntax error: "+e);
        } catch(SQLException e) {
            System.err.println("Factoid insertion: SQL Exception: "+e);
        } finally {
            sql.disconnect();
        }

        // Then add it to memory
        factoids.add(new FactoidItem(message, trigger, reply, author));
        return true;
    }

    /**
     * Attempts to delete an existing factoid specified by trigger
     * @param trigger The trigger string of the factoid to delete
     * @return true if the factoid was found and removed, or false if it wasn't found
     */
    private boolean del(String trigger) {
        FactoidItem factoid;
        if((factoid = find(trigger, false)) != null) {
            // First remove it from the SQL db
            SQL sql = new SQL();
            try {
                sql.connect();
                sql.query("DELETE FROM gh_factoid WHERE `trigger` = '"+trigger+"';");
                if(sql.getAffectedRows() == 0) {
                    System.err.println("Factoid deletion warning: Item was found in local arraylist, but not in SQL DB!");
                    Grouphug.getInstance().sendMessage("OMG inconsistency; I have the factoid in memory but not in the SQL db.", false);
                    return false;
                }
            } catch(SQLSyntaxErrorException e) {
                System.err.println("Factoid deletion: SQL Syntax error: "+e);
            } catch(SQLException e) {
                System.err.println("Factoid deletion: SQL Exception: "+e);
            } finally {
                sql.disconnect();
            }

            // Then remove it from memory
            factoids.remove(factoid);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Tries to find a factoid in local memory
     * @param trigger The trigger string of the factoid to search for
     * @param regex true if a regex should be used to find the trigger, false if it should be exact search
     * @return The found FactoidItem, or null if no item was found
     */
    private FactoidItem find(String trigger, boolean regex) {
        for(FactoidItem factoid : factoids) {
            if(regex) {
                if(factoid.trigger(trigger))
                    return factoid;
            } else {
                if(factoid.getTrigger().equals(trigger))
                    return factoid;
            }
        }
        return null;
    }

    private static class FactoidItem {

        private boolean message;
        private String trigger;
        private String reply;
        private String author;

        public boolean isMessage() {
            return message;
        }

        public String getTrigger() {
            return trigger;
        }

        public String getReply() {
            return reply;
        }

        public String getAuthor() {
            return author;
        }

        private FactoidItem(boolean message, String trigger, String reply, String author) {
            this.message = message;
            this.trigger = trigger;
            this.reply = reply;
            this.author = author;
        }

        private boolean trigger(String message) {
            return message.matches(trigger.replace("*", ".*"));
        }

        /**
         * Sends this factoid to the channel
         * @param sender The nick of the sender
         */
        private void send(String sender) {
            if(isMessage()) {
                Grouphug.getInstance().sendMessage(getReply().replace("$sender", sender), true);
            } else {
                // TODO - action evades spam, and all the local sendMessage routines
                Grouphug.getInstance().sendAction(Grouphug.CHANNEL, getReply().replace("$sender", sender));
            }
        }
    }
}
