package no.kvikshaug.gh.modules;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;

public class Remember implements TriggerListener {

    private static final String TRIGGER_HELP = "remember";

    private static final String TRIGGER_ADD = "remember";
    private static final String TRIGGER_REMOVE = "remove";

    private static final String REMEMBER_TABLE = "remember";
    private static final String TRIGGER_GET_TAG = "gettag";
    private static final String TRIGGER_GET_SENDER = "getnick";
    private SQLHandler sqlHandler;

    private static Grouphug bot;

    public Remember(ModuleHandler moduleHandler) {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER_ADD, this);
            moduleHandler.addTriggerListener(TRIGGER_REMOVE, this);
            moduleHandler.addTriggerListener(TRIGGER_GET_SENDER, this);
            moduleHandler.addTriggerListener(TRIGGER_GET_TAG, this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Remember: Add URLS or the like to remember annotated with tags. Only one word can be remembered\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_ADD +    " message tag1 tag2 ... tagN\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_REMOVE +    " message\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_GET_SENDER + " nick\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER_GET_TAG + " tag\n");
            bot = Grouphug.getInstance();
            System.out.println("Remember module loaded.");
        } catch(SQLUnavailableException ex) {
            System.err.println("Remember startup: SQL is unavailable!");
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
    	
    	String[] messageParts = message.split(" ");
    	
        if(trigger.equals(TRIGGER_ADD)) {
            // Trying to add a new thing to remember
        	
        	if (messageParts.length == 1) {
        		bot.msg(channel, "You need to add at least one tag to remember this by " + sender);
        	
            } else if (messageParts.length == 0 ){
            	bot.msg(channel, "Uhm, I think you forgot something there skipper");
            } else {
            	for (int i = 1; i < messageParts.length; i++) {
            		try {
						sqlHandler.insert("INSERT INTO " + REMEMBER_TABLE + " (`message`, `sender`, `tag`, `channel`) VALUES ('?', '?', '?', '?');", Arrays.asList(new String[] {messageParts[0], sender, messageParts[i], channel}));
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
            	bot.msg(channel, "OK, "+sender+".");
            }
        } else if(trigger.equals(TRIGGER_REMOVE)) {
            // First remove it from the SQL db
            try {
                sqlHandler.delete("DELETE FROM " + REMEMBER_TABLE + "  WHERE `message` = '?' AND `channel` = '?';", 
                		Arrays.asList(new String[] {messageParts[0], channel}));
                bot.msg(channel, "Message deleted!");
            } catch(SQLException e) {
                bot.msg(channel, "You should know that I caught an SQL exception.");
                System.err.println("Remember deletion: SQL Exception!");
                e.printStackTrace();
            }

        } else if(trigger.equals(TRIGGER_GET_SENDER) || trigger.equals(TRIGGER_GET_TAG)) {
        	List<Object[]> rows = null;
        	
        	if(trigger.equals(TRIGGER_GET_SENDER)){
        		try {
        			rows = sqlHandler.select("SELECT message FROM " + REMEMBER_TABLE + " WHERE `channel`='?' AND `sender`='?'", Arrays.asList(new String[] {channel, messageParts[0]}));
        		} catch (SQLException e) {
        			e.printStackTrace();
        		}
        	} else if(trigger.equals(TRIGGER_GET_TAG)){
        		try {
        			rows = sqlHandler.select("SELECT message FROM " + REMEMBER_TABLE + " WHERE `channel`='?' AND `tag`='?'", Arrays.asList(new String[] {channel, messageParts[0]}));
        		} catch (SQLException e) {
        			e.printStackTrace();
        		}
        	}
        	
        	for (Object object : rows) {
				bot.msg(channel, (String)object);
			}
        }
    }
}
