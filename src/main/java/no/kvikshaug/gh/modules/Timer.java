package no.kvikshaug.gh.modules;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;

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
    	"!timer count[s/m/h/d] [message]\n" +
    	"!timer hh:mm [message]\n" +
    	"!timer dd/mm [message] \n" +
    	"!timer dd/mm-hh:mm [message] \n" +
    	"s/m/h/d = seconds/minutes/hours/days (optional, default is minutes)\n" +
    	"Example: !timer 14m grandis\n";
    	handler.registerHelp("timer", helpText);
    	bot = Grouphug.getInstance();
    	System.out.println("Timer module loaded.");


    	//Load timers from database, if there were any there when the bot shut down
    	try {
    		sqlHandler = SQLHandler.getSQLHandler();
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
    					bot.msg(channel, nick + ": Time ran out while I was shut down. I am notifying you anyway!");
    				} else {
    					bot.msg(channel, nick + ": Time ran out while I was shut down. I was supposed to notify you about: " + message);
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

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
    	
    	String timerTime = message.split(" ")[0]; //getting the 14d, 23:59 part from the message
    	
    	if (timerTime.matches(".*[a-zA-Z]")){ //Has a letter in it, aka the 14d kind of timer
    		countdownTimer(channel, sender, message);
    		return;
    	}
    	
    	//Change this checking to regex checking which type it is perhaps, this is ugly :3
    	
    	SimpleDateFormat hour_min_format = new SimpleDateFormat("HH:mm"); // 21:45
    	SimpleDateFormat date_format = new SimpleDateFormat("dd/MM"); // 2/4, 23/12
    	SimpleDateFormat date_hour_min_format = new SimpleDateFormat("dd/MM-HH:mm"); //24/12-20:34
    	
    	DateTime parseHighlight = null;
    	DateTime timeToHighlight = null;
    	DateTime now = new DateTime();
		try {
			parseHighlight = new DateTime(hour_min_format.parse(timerTime));
			timeToHighlight = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(), 
					parseHighlight.getHourOfDay(), parseHighlight.getMinuteOfHour(), 0, 0);
		
			if (timeToHighlight.isBefore(now)){ //Aka a timestamp that already has been today
				timeToHighlight = timeToHighlight.plusDays(1); //So that means we mean tomorrow
			}

		} catch (ParseException e) {
			//Not of this type, try another one
			try {
				parseHighlight = new DateTime(date_format.parse(timerTime));
				timeToHighlight = new DateTime(now.getYear(), parseHighlight.getMonthOfYear(), parseHighlight.getDayOfMonth(), 
						0, 0, 0, 0);
				
				if (timeToHighlight.isBefore(now)){ //A date that has already been this year
					timeToHighlight = timeToHighlight.plusYears(1); //So that means we mean next year
				}
				
			} catch (ParseException e1) {
				//Not this one either, try the last one
				try {
					parseHighlight = new DateTime(date_hour_min_format.parse(timerTime));
					timeToHighlight = new DateTime(now.getYear(), parseHighlight.getMonthOfYear(), parseHighlight.getDayOfMonth(), 
							parseHighlight.getHourOfDay(), parseHighlight.getMinuteOfHour(), 0, 0);
					
					if (timeToHighlight.isBefore(now)){ //Aka a date and time that has been this year
						timeToHighlight = timeToHighlight.plusYears(1);						
					}
				} catch (ParseException e2) {
					//Not this one either, this is just bogus
					bot.msg(channel, "Stop trying to trick me, this isn't a valid timer-argument. Try !help timer");
					return;
				}
			}
		}
		
		//We now have the time, now we just have to find out how long it is until that time
		
		Duration duration = null;
		try {
			duration = new Duration(new DateTime(), timeToHighlight);
		} catch (IllegalArgumentException e) {
			System.out.println("Error in timer, trying to get the duration between now, and a datetime that has already been");
			return;
		}
		
		String notifyMessage = message.substring(timerTime.length()).trim();
		
		
		int id = insertTimerIntoDb(channel, sender, notifyMessage, timeToHighlight.getMillis());
        
        bot.msg(channel, "Ok, I will highlight you at " + timerTime +".");
        
        new Sleeper(id, sender, (int) duration.getMillis(), notifyMessage, channel);
    }
    

    /**
     * Normal countdown, where days, hours, minutes and seconds are specified
     * @param channel
     * @param sender
     * @param message
     */
	private void countdownTimer(String channel, String sender, String message) {
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
                bot.msg(channel, "'" + message + "' doesn't start with a valid number, does it now? Try '!help timer'.");
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
                    bot.msg(channel, "No. Try '!help timer'.");
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
        
        int id = insertTimerIntoDb(channel, sender, notifyMessage, time);
        
        bot.msg(channel, "Ok, I will highlight you in " + count + " " + reply + ".");
        
        new Sleeper(id, sender, sleepTime, notifyMessage, channel);
	}

	private int insertTimerIntoDb(String channel, String sender, String notifyMessage, long time) {
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
		return id;
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

        public void run() {
            try {
                Thread.sleep(sleepAmount);
            } catch(InterruptedException e) {
                bot.msg(channel, nick + ": Sorry, I caught an InterruptedException! I was supposed to highlight you " +
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
                bot.msg(channel, nick + ": Time's up!");
            } else {
                bot.msg(channel, nick + ": " + notifyMessage);
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
