package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.exceptions.SQLUnavailableException;
import grouphug.listeners.MessageListener;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQLHandler;

import java.sql.SQLException;
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
public class Factoid implements MessageListener, TriggerListener {

    // TODO - dynamic reply, with username, more?
    // TODO - more details of factiod ? time ?

    private ArrayList<FactoidItem> factoids = new ArrayList<FactoidItem>();

    private static final String TRIGGER_HELP = "factoid";

    private static final String TRIGGER_MAIN = "factoid";
    private static final String TRIGGER_RANDOM = "randomfactoid";
    private static final String TRIGGER_FOR = "trigger";

    private static final String TRIGGER_ADD = "on";
    private static final String TRIGGER_DEL = "forget";

    private static final String SEPARATOR_MESSAGE = " <say> ";
    private static final String SEPARATOR_ACTION = " <do> ";

    private static final String FACTOID_TABLE = "factoid";

    private static Random random = new Random(System.nanoTime());

    private SQLHandler sqlHandler;

    public Factoid(ModuleHandler moduleHandler) {
        // Load up all existing factoids from sql
        try {
            sqlHandler = new SQLHandler(true);
            ArrayList<Object[]> rows = sqlHandler.select("SELECT `type`, `trigger`, `reply`, `author` FROM " + FACTOID_TABLE + ";");
            for(Object[] row : rows) {
                boolean message = row[0].equals("message");
                factoids.add(new FactoidItem(message, (String)row[1], (String)row[2], (String)row[3]));
            }
            moduleHandler.addTriggerListener(TRIGGER_MAIN, this);
            moduleHandler.addTriggerListener(TRIGGER_ADD, this);
            moduleHandler.addTriggerListener(TRIGGER_DEL, this);
            moduleHandler.addTriggerListener(TRIGGER_RANDOM, this);
            moduleHandler.addTriggerListener(TRIGGER_FOR, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Factoid: Make me say or do \"reply\" when someone says \"trigger\".\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_ADD +    " trigger <say> reply\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_ADD +    " trigger <do> something\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_DEL +    " trigger\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_MAIN +   " trigger      - show information about a factoid\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_RANDOM + "        - trigger a random factoid\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER_FOR + " <expression> - show what factoid, if any, that is " +
                    "triggered by that expression\n" +
                   " - The string \"$sender\" will be replaced with the nick of the one triggering the factoid.\n" +
                   " - A star (*) can be any string of characters.\n" +
                   " - Regex can be used, but remember that * is replaced with .*");
            System.out.println("Factoid module loaded.");
        } catch(SQLUnavailableException ex) {
            System.err.println("Factoid startup: SQL is unavailable!");
        } catch (SQLException e) {
            System.err.println("Factoid startup: SQL Exception: "+e);
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {

        if(trigger.equals(TRIGGER_ADD)) {
            // Trying to add a new factoid

            boolean replyMessage;
            String factoidTrigger, reply;

            if(message.contains(SEPARATOR_MESSAGE)) {
                replyMessage = true;
                factoidTrigger = message.substring(0, message.indexOf(SEPARATOR_MESSAGE));
                reply = message.substring(message.indexOf(SEPARATOR_MESSAGE) + SEPARATOR_MESSAGE.length());
            } else if(message.contains(SEPARATOR_ACTION)) {
                replyMessage = false;
                factoidTrigger = message.substring(0, message.indexOf(SEPARATOR_ACTION));
                reply = message.substring(message.indexOf(SEPARATOR_ACTION) + SEPARATOR_ACTION.length());
            } else {
                // If it's neither a message nor an action
                Grouphug.getInstance().sendMessage("What? Don't give me that nonsense, "+sender+".");
                return;
            }

            // add() returns true if the factoid is added, or false if the trigger is already taken
            if(add(replyMessage, factoidTrigger, reply, sender)) {
                Grouphug.getInstance().sendMessage("OK, "+sender+".");
            } else {
                Grouphug.getInstance().sendMessage("But, "+sender+", "+factoidTrigger+".");
            }
        } else if(trigger.equals(TRIGGER_DEL)) {
            // Trying to remove a factoid
            // del() returns true if it's removed, false if there is no such trigger
            if(del(message)) {
                Grouphug.getInstance().sendMessage("I no longer know of this "+ message +" that you speak of.");
            } else {
                Grouphug.getInstance().sendMessage(sender+", I can't remember "+ message +" in the first place.");
            }
        } else if(trigger.equals(TRIGGER_MAIN)) {
            // Trying to view data about a factoid
            FactoidItem factoid;
            if((factoid = find(message, false)) != null) {
                Grouphug.getInstance().sendMessage("Factoid: [ trigger = "+factoid.getTrigger()+" ] [ reply = "+
                        factoid.getReply()+" ] [ type = "+(factoid.isMessage() ? "message" : "action")+
                        " ]  [ author = "+factoid.getAuthor()+" ]");
            } else {
                Grouphug.getInstance().sendMessage(sender+", I do not know of this "+message+" that you speak of.");
            }
        } else if(trigger.equals(TRIGGER_RANDOM)) {
            factoids.get(random.nextInt(factoids.size())).send(sender);
        } else if(trigger.equals(TRIGGER_FOR)) {
            FactoidItem factoid;
            if((factoid = find(message, true)) != null) {
                Grouphug.getInstance().sendMessage("Factoid: [ trigger = "+factoid.getTrigger()+" ] [ reply = "+
                        factoid.getReply()+" ] [ type = "+(factoid.isMessage() ? "message" : "action")+
                        " ]  [ author = "+factoid.getAuthor()+" ]");
            } else {
                Grouphug.getInstance().sendMessage("Sorry, that expression doesn't ring any bell.");
            }
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        // avoid outputting when the trigger is being added, removed or searched for
        if(message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_ADD) ||
                message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_DEL) ||
                message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_FOR)) {
            return;
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
        try {
            sqlHandler.insert("INSERT INTO " + FACTOID_TABLE + " (`type`, `trigger`, `reply`, `author`) VALUES ('"+(message ? "message" : "action")+"', '"+trigger+"', '"+reply+"', '"+author+"');");
        } catch(SQLException e) {
            System.err.println("Factoid insertion: SQL Exception: "+e);
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
            try {
                if(sqlHandler.delete("DELETE FROM " + FACTOID_TABLE + "  WHERE `trigger` = '"+trigger+"';") == 0) {
                    System.err.println("Factoid deletion warning: Item was found in local arraylist, but not in SQL DB!");
                    Grouphug.getInstance().sendMessage("OMG inconsistency; I have the factoid in memory but not in the SQL db.");
                    return false;

                }
            } catch(SQLException e) {
                System.err.println("Factoid deletion: SQL Exception: "+e);
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
