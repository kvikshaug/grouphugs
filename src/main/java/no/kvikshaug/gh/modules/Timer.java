package no.kvikshaug.gh.modules;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;

public class Timer implements TriggerListener {

	//CREATE TABLE timer(id INTEGER PRIMARY KEY, nick TEXT, time INTEGER, message TEXT);
    private Grouphug bot;
    
    private static final String TIMER_TABLE = "timer";
    private SQLHandler sqlHandler;

    public Timer(ModuleHandler handler) {
    	handler.addTriggerListener("timer", this);
    	String helpText = "Use timer to time stuff, like your pizza.\n" +
    	"!time count[s/m/h/d] [message]\n" +
    	"s/m/h/d = seconds/minutes/hours/days (optional, default is minutes)\n" +
    	"Example: !timer 14m grandis\n";
    	handler.registerHelp("timer", helpText);
    	bot = Grouphug.getInstance();
    	System.out.println("Timer module loaded.");


    	//Load timers from database, if there were any there when the bot shut down
    	try {
    		sqlHandler = new SQLHandler(true);
    		List<Object[]> rows = sqlHandler.select("SELECT `id`, `nick`, `time`, `message`, `channel` FROM " + TIMER_TABLE + ";");
    		for(Object[] row : rows) {
    			int id = (Integer) row[0];
    			String nick = (String) row[1];
    			long time = (Long) row[2];
    			String message = (String) row[3];
    			String channel = (String) row[4];

    			//The timer expired when the bot was down
    			if (time <= System.currentTimeMillis() ){
    				if("".equals(message)) {
    					bot.sendMessageChannel(channel, nick + ": Time ran out while I was shut down. I am notifying you anyway!");
    				} else {
    					bot.sendMessageChannel(channel, nick + ": Time ran out while I was shut down. I was supposed to notify you about: " + message);
    				}
    				sqlHandler.delete("DELETE FROM " + TIMER_TABLE + "  WHERE `id` = '"+id+"';");
    			}else{
    				new Sleeper(id, nick, (int) (time-System.currentTimeMillis()), message, channel);
    			}
    		}
    	} catch(SQLUnavailableException ex) {
    		System.err.println("Factoid startup: SQL is unavailable!");
    	} catch (SQLException e) {
    		e.printStackTrace();
    	}
    }

    @Override
    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        int indexAfterCount = 0;
        try {
            while(true) {
                if(indexAfterCount == message.length()) {
                    // there are no more chars after this one
                    break;
                }
                Integer.parseInt(message.substring(indexAfterCount, indexAfterCount+1));
                indexAfterCount++;
            }
        } catch(NumberFormatException e) {
            if(indexAfterCount == 0) {
                // message didn't start with a number
                bot.sendMessageChannel(channel, "'" + message + "' doesn't start with a valid number, does it now? Try '!help timer'.");
                return;
            }
            // indexAfterCount is now the index after the count
        }
        int factor;
        String reply;
        String notifyMessage;
        int count = Integer.parseInt(message.substring(0, indexAfterCount));
        // if there are no chars after the count
        if(indexAfterCount == message.length()) {
            factor = 60;
            reply = "minutes";
            notifyMessage = "";
        } else {
            switch(message.charAt(indexAfterCount)) {
                case 's':
                    factor = 1;
                    reply = "seconds";
                    break;

                case 'm':
                case ' ':
                    factor = 60;
                    reply = "minutes";

                    break;

                case 'h':
                case 't':
                    factor = 3600;
                    reply = "hours";

                    break;

                case 'd':
                    factor = 86400;
                    reply = "days";
                    break;

                default:
                    bot.sendMessageChannel(channel, "No. Try '!help timer'.");
                    return;

            }
            notifyMessage = message.substring(indexAfterCount + 1).trim();
        }
        if(count == 1) {
            // not plural, strip 's'
            reply = reply.substring(0, reply.length()-1);
        }
        
        int sleepTime = count * factor * 1000;
        
        long time = System.currentTimeMillis() + sleepTime;
        
        int id = -1;
        try {
        	List<String> params = new ArrayList<String>();
        	params.add(sender);
        	params.add(""+time);
        	params.add(notifyMessage);
        	params.add(channel);
        	id = sqlHandler.insert("INSERT INTO " + TIMER_TABLE + " (`nick`, `time`, `message`, `channel`) VALUES (?, ?, ?, ?);", params);
        } catch(SQLException e) {
            System.err.println("Timer insertion: SQL Exception: "+e);
        }
        
        bot.sendMessageChannel(channel, "Ok, I will highlight you in " + count + " " + reply + ".");
        
        new Sleeper(id, sender, sleepTime, notifyMessage, channel);
    }

    private class Sleeper implements Runnable {
    	private int id;
        private String nick;
        private int sleepAmount; // ms
        private String notifyMessage;
        private String channel;

        private Sleeper(int id, String nick, int sleepAmount, String notifyMessage, String channel) {
            this.nick = nick;
            this.sleepAmount = sleepAmount;
            this.notifyMessage = notifyMessage;
            this.id = id;
            this.channel = channel;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepAmount);
            } catch(InterruptedException e) {
                bot.sendMessageChannel(channel, nick + ": Sorry, I caught an InterruptedException! I was supposed to highlight you " +
                        "after " + (sleepAmount / 1000) + " seconds, but I don't know how long I've slept.");
                try {
                	if (this.id != -1){
                		sqlHandler.delete("DELETE FROM " + TIMER_TABLE + "  WHERE `id` = '"+this.id+"';");
                	}
                } catch(SQLException e2) {
                    e.printStackTrace();
                }
                return;
            }
            if("".equals(notifyMessage)) {
                bot.sendMessageChannel(channel, nick + ": Time's up!");
            } else {
                bot.sendMessageChannel(channel, nick + ": " + notifyMessage);
            }
            try {
            	if (this.id != -1){
            		sqlHandler.delete("DELETE FROM " + TIMER_TABLE + "  WHERE `id` = '"+this.id+"';");
            	}
            } catch(SQLException e) {
                e.printStackTrace();
            }
            
        }
    }
}
