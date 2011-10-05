package no.kvikshaug.gh.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import no.kvikshaug.scatsd.client.ScatsD;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

/**
 * The Factoid module lets a user save two strings, one which the bot should react upon
 * and a corresponding string which it should reply with.
 *
 * Instead of connecting to SQL and fetching all the triggers each time someone sends something
 * to the channel, we maintain a local list in memory, and just keep the SQL db synchronized, so
 * that on startup we can fetch all the factoids back.
 */
public class Factoid implements MessageListener, TriggerListener {

    private List<FactoidItem> factoids;

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

    private static Grouphug bot;

    public Factoid(ModuleHandler moduleHandler) {
        // Load up all existing factoids from sql
        if(SQL.isAvailable()) {
            factoids = JWorm.get(FactoidItem.class);
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
            bot = Grouphug.getInstance();
        } else {
            System.err.println("Factoid disabled: SQL is unavailable.");
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {

        if(trigger.equals(TRIGGER_ADD)) {
            // Trying to add a new factoid

            String type;
            String factoidTrigger, reply;

            if(message.contains(SEPARATOR_MESSAGE)) {
                type = "message";
                factoidTrigger = message.substring(0, message.indexOf(SEPARATOR_MESSAGE));
                reply = message.substring(message.indexOf(SEPARATOR_MESSAGE) + SEPARATOR_MESSAGE.length());
            } else if(message.contains(SEPARATOR_ACTION)) {
                type = "action";
                factoidTrigger = message.substring(0, message.indexOf(SEPARATOR_ACTION));
                reply = message.substring(message.indexOf(SEPARATOR_ACTION) + SEPARATOR_ACTION.length());
            } else {
                // If it's neither a message nor an action
                bot.msg(channel, "What? Don't give me that nonsense, "+sender+".");
                return;
            }

            if(find(channel, factoidTrigger, false).size() != 0) {
                bot.msg(channel, "But, "+sender+", "+factoidTrigger+".");
                return;
            }

            // Add the new item to the SQL db and memory
            FactoidItem item = new FactoidItem(type.equals("message"), factoidTrigger, reply, sender, channel);
            item.insert();
            factoids.add(item);

            bot.msg(channel, "OK, "+sender+".");
        } else if(trigger.equals(TRIGGER_DEL)) {
            // Trying to remove a factoid
            List<FactoidItem> factoids = find(channel, message, false);
            if(factoids.size() == 0) {
                bot.msg(channel, sender+", I can't remember "+ message +" in the first place.");
            } else if(factoids.size() != 1) {
                bot.msg(channel, "I actually have " + factoids.size() + " such factoids, how did that happen? " +
                        "Please remove them manually and fix this bug.");
                System.err.println("More than one factoid exists with '"+message+"' as trigger:");
                for(FactoidItem factoid : factoids) {
                    System.err.println(factoid.toString());
                }
            } else {
                factoids.get(0).delete();
                this.factoids.remove(this.factoids.indexOf(factoids.get(0)));
                bot.msg(channel, "I no longer know of this "+ message +" that you speak of.");
            }
        } else if(trigger.equals(TRIGGER_MAIN)) {
            // Trying to view data about a factoid
            List<FactoidItem> factoids = find(channel, message, false);
            if(factoids.size() == 0) {
                bot.msg(channel, sender+", I do not know of this "+message+" that you speak of.");
            } else {
                for(FactoidItem factoid : factoids) {
                    bot.msg(channel, factoid.toString());
                }
            }
        } else if(trigger.equals(TRIGGER_RANDOM)) {
            List<FactoidItem> items = JWorm.getWith(FactoidItem.class, "where channel='" +
              SQL.sanitize(channel) + "' order by random() limit 1");
            if(items.size() > 0) {
                bot.msg(channel, items.get(0).getReply());
            } else {
                bot.msg(channel, "No factoids are added");
            }
        } else if(trigger.equals(TRIGGER_FOR)) {
            List<FactoidItem> factoids = find(channel, message, true);
            if(factoids.size() == 0) {
                bot.msg(channel, "Sorry, that expression doesn't ring any bell.");
            } else {
                for(FactoidItem factoid : factoids) {
                    bot.msg(channel, factoid.toString());
                }
            }
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        // avoid outputting when the trigger is being added, removed or searched for
        if(message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_MAIN) ||
                message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_ADD) ||
                message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_DEL) ||
                message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_FOR)) {
            return;
        }

        List<FactoidItem> factoids = find(channel, message, true);
        for(FactoidItem factoid : factoids) {
            factoid.send(sender);
        }
        ScatsD.count("gh.bot.modules.factoid.triggers", factoids.size());
    }

    /**
     * Tries to find factoid in local memory
     * @param expression The expression that might trigger factoids
     * @param regex true if a regex should be used to find the trigger, false if it should be exact search
     * @return The found FactoidItem, or null if no item was found
     */
    private List<FactoidItem> find(String channel, String expression, boolean regex) {
        List<FactoidItem> items = new ArrayList<FactoidItem>();
        for(FactoidItem factoid : factoids) {
        	if (!factoid.getChannel().equals(channel))
        	{
        		continue;
        	}
            if(regex) {
                if(factoid.trigger(expression)) {
                    items.add(factoid);
                }
            } else {
                if(factoid.getTrigger().equals(expression)) {
                    items.add(factoid);
                }
            }
        }
        return items;
    }

    public static class FactoidItem extends Worm {

        private boolean message;
        private String trigger;
        private String reply;
        private String author;
        private String channel;
        
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
        
        public String getChannel() {
            return channel;
        }

        public FactoidItem(boolean message, String trigger, String reply, String author, String channel) {
            this.message = message;
            this.trigger = trigger;
            this.reply = reply;
            this.author = author;
            this.channel = channel;
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
                bot.msg(getChannel(), getReply().replace("$sender", sender), true);
            } else {
                // TODO - action evades spam, and all the local sendMessage routines
                bot.sendAction(getChannel(), getReply().replace("$sender", sender));
            }
        }

        @Override
        public String toString() {
            return "Factoid: [ trigger = "+getTrigger()+" ] [ reply = "+getReply()+" ] [ type = "+
                    (isMessage() ? "message" : "action")+" ]  [ author = "+getAuthor()+" ]";
        }
    }
}
