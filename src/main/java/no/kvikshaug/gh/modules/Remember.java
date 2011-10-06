package no.kvikshaug.gh.modules;

import java.util.List;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

public class Remember implements TriggerListener {

    private static final String TRIGGER_HELP = "remember";

    private static final String TRIGGER_ADD = "remember";
    private static final String TRIGGER_REMOVE = "remove";

    private static final String TRIGGER_GET_TAG = "gettag";
    private static final String TRIGGER_GET_SENDER = "getnick";

    private static Grouphug bot;

    public Remember(ModuleHandler moduleHandler) {
        if(SQL.isAvailable()) {
            moduleHandler.addTriggerListener(TRIGGER_ADD, this);
            moduleHandler.addTriggerListener(TRIGGER_REMOVE, this);
            moduleHandler.addTriggerListener(TRIGGER_GET_SENDER, this);
            moduleHandler.addTriggerListener(TRIGGER_GET_TAG, this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Remember: Add URLS or the like to remember annotated with a tag. Only one tag at a time\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_ADD +    " message tag\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_REMOVE +    " message\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_GET_SENDER + " nick\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_GET_TAG + " tag\n");
            bot = Grouphug.getInstance();
        } else {
            System.err.println("Remember disabled: SQL is unavailable.");
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
    	
    	String[] messageParts = message.split(" ");
    	
        if(trigger.equals(TRIGGER_ADD)) {
            // Trying to add a new thing to remember
        	
        	if (messageParts.length == 1) {
        		bot.msg(channel, "You need to add a tag to remember this by " + sender);
        	
            } else if (messageParts.length == 0 ){
            	bot.msg(channel, "Uhm, I think you forgot something there skipper");
            } else {
                RememberItem newItem = new RememberItem(
                  message.substring(0, message.length() - messageParts[messageParts.length-1].length()),
                  sender, messageParts[messageParts.length -1], channel);
                newItem.insert();
				bot.msg(channel, "OK, "+sender+".");
            }
        } else if(trigger.equals(TRIGGER_REMOVE)) {
            // First remove it from the SQL db
            List<RememberItem> items = JWorm.getWith(RememberItem.class, "where `message` = '" +
              SQL.sanitize(messageParts[0]) + "' and `channel` = '" + SQL.sanitize(channel) + "'");
            if(items.size() == 0) {
                bot.msg(channel, "Couldn't find that item.");
            } else {
                items.get(0).delete();
                bot.msg(channel, "Message deleted!");
            }
        } else if(trigger.equals(TRIGGER_GET_SENDER) || trigger.equals(TRIGGER_GET_TAG)) {
        	List<RememberItem> rows = null;
        	
        	if(trigger.equals(TRIGGER_GET_SENDER)){
                rows = JWorm.getWith(RememberItem.class, "where `channel`='" + SQL.sanitize(channel) +
                  "' and `sender`='" + SQL.sanitize(messageParts[0]) + "'");
        	} else if(trigger.equals(TRIGGER_GET_TAG)){
                rows = JWorm.getWith(RememberItem.class, "where `channel`='" + SQL.sanitize(channel) +
                  "' and `tag`='" + SQL.sanitize(messageParts[0]) + "'");
        	}

            for(RememberItem i : rows) {
                bot.msg(channel, i.getMessage());
            }
        }
    }

    public static class RememberItem extends Worm {
        private String message;
        private String sender;
        private String tag;
        private String channel;

        public RememberItem(String message, String sender, String tag, String channel) {
            this.message = message;
            this.sender = sender;
            this.tag = tag;
            this.channel = channel;
        }

        public String getMessage() {
            return this.message;
        }
        
        public String getSender() {
            return this.sender;
        }
        
        public String getTag() {
            return this.tag;
        }
        
        public String getChannel() {
            return this.channel;
        }
    }
}
