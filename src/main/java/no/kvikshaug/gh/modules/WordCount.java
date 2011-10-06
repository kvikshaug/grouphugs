package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.SQLHandler;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class WordCount implements TriggerListener, MessageListener {

    private static final String TRIGGER_HELP = "wordcount";
    private static final String TRIGGER = "wc";
    private static final String TRIGGER_TOP = "wctop";
    private static final String TRIGGER_BOTTOM = "wcbottom";
    private static final String TRIGGER_REMOVE = "wcrm";
    private static final String WORDS_DB= "words";
    private static final int LIMIT = 5;
    private static final DateFormat df = new SimpleDateFormat("d. MMMMM yyyy");

    private SQLHandler sqlHandler;
    private Grouphug bot;

    public WordCount(ModuleHandler moduleHandler) {
        try {
            bot = Grouphug.getInstance();
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addTriggerListener(TRIGGER_TOP, this);
            moduleHandler.addTriggerListener(TRIGGER_BOTTOM, this);
            moduleHandler.addTriggerListener(TRIGGER_REMOVE, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Counts the number of words/lines a person has said\n" +
                    "To check how many words someone has said, use " +Grouphug.MAIN_TRIGGER + TRIGGER + " <nick>\n" +
                    "Top 5: " + Grouphug.MAIN_TRIGGER + TRIGGER_TOP + "\n" +
                    "Bottom 5: " + Grouphug.MAIN_TRIGGER + TRIGGER_BOTTOM + "\n" +
                    "Remove stats count: " + Grouphug.MAIN_TRIGGER + TRIGGER_REMOVE + " <nick>");
        } catch(SQLUnavailableException ex) {
            System.err.println("WordCount startup error: SQL is unavailable!");
        }
    }
    public void addWords(String channel, String sender, String message) {
        // This method to count words should be more or less failsafe:
        int newWords = message.trim().replaceAll(" {2,}+", " ").split(" ").length;

        try{
        	List<String> params = new ArrayList<String>();
            params.add(sender);
            params.add(channel);
            Object[] row = sqlHandler.selectSingle("SELECT id, words, `lines` FROM "+WORDS_DB+" WHERE nick=? AND channel=?;", params);
            if(row == null) {
                params = new ArrayList<String>();
                params.add(sender);
                params.add(newWords + "");
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(channel);
                sqlHandler.insert("INSERT INTO "+WORDS_DB+" (nick, words, `lines`, since, channel) VALUES (?, ?, '1', ?, ?);", params);
            } else {
                long existingWords = ((Integer)row[1]);
                long existingLines = ((Integer)row[2]);
                sqlHandler.update("UPDATE "+WORDS_DB+" SET words='"+(existingWords + newWords)+"', `lines`='"+(existingLines + 1)+"' WHERE id='"+row[0]+"';");
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            e.printStackTrace();
            bot.msg(channel, "Sorry, unable to update WordCounter DB; an SQL error occured.");
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_REMOVE) || !message.endsWith(sender)) {
            addWords(channel, sender, message);
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(trigger.equals(TRIGGER)) {
            print(channel, message);
        } else if(trigger.equals(TRIGGER_TOP)) {
            showScore(channel, true);
        } else if(trigger.equals(TRIGGER_BOTTOM)) {
            showScore(channel, false);
        } else if(trigger.equals(TRIGGER_REMOVE)) {
        	if(true){ //Should be specified in the config, do later :3
        		bot.msg(channel, "Sorry, this functionality has been disabled for now");
                return;
        	}
            if(!sender.equalsIgnoreCase(message)) {
                bot.msg(channel, "Sorry, as a safety precaution, this function can only be used " +
                        "by a user with the same nick as the one that's being removed.");
            } else {
                try {
                    List<String> params = new ArrayList<String>();
                    params.add(message);
                    params.add(channel);
                    if(sqlHandler.delete("delete from "+WORDS_DB+" where nick=? AND channel=?;", params) == 0) {
                        bot.msg(channel, "DB reports that no such nick has been recorded.");
                    } else {
                        bot.msg(channel, sender+", you now have no words counted.");
                    }
                } catch(SQLException ex) {
                    bot.msg(channel, "Crap, SQL barfed on me. Check the logs if you wanna know why.");
                    System.err.println(ex);
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

    private void showScore(String channel, boolean top) {
        String reply;
        if(top) {
            reply = "The biggest losers are:\n";
        } else {
            reply = "The laziest idlers are:\n";
        }
        try {
        	List<String> params = new ArrayList<String>();
            params.add(channel);
            String query = ("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" WHERE channel=? ORDER BY words");
            if(top) {
                query += " DESC";
            }
            query += " LIMIT "+LIMIT+";";
            List<Object[]> rows = sqlHandler.select(query, params);
            int place = 1;
            for(Object[] row : rows) {
                long words = ((Integer)row[2]);
                long lines = ((Integer)row[3]);
                DateTime since = new DateTime(SQL.sqlDateTimeToDate((String)row[4]));
                System.out.println(row[1] + ": " + since);
                int days = Days.daysBetween(since, new DateTime()).getDays();
                double wpl = (double)words / (double)lines;
                double wpd = (double)words / days;
                double lpd = (double)lines / days;
                DecimalFormat sf = new DecimalFormat("0.0");

                reply += (place++)+". "+row[1]+ " ("+words+" words, "+lines+" lines, "+days+" days, "+
                        sf.format(wpl) + " wpl, " +
                        sf.format(wpd) + " wpd, " +
                        sf.format(lpd) + " lpd)\n";
            }
            if(top) {
                reply += "I think they are going to need a new keyboard soon.";
            } else {
                reply += "Lazy bastards...";
            }
            bot.msg(channel, reply);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.msg(channel, "Sorry, an SQL error occured.");
        } catch(ParseException e) {
            System.err.println("Unable to parse the SQL datetime!");
            bot.msg(channel, "Sorry, I was unable to parse the date of this wordcount! Patches are welcome.");
        }
    }

    private void print(String channel, String message){
        try{
        	List<String> params = new ArrayList<String>();
        	params.add(message);
            params.add(channel);
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" WHERE nick LIKE ? AND channel=?;", params);

            if(row == null) {
                bot.msg(channel, message + " doesn't have any words counted.");
            } else {
                long words = ((Integer)row[2]);
                long lines = ((Integer)row[3]);
                DateTime since = new DateTime(SQL.sqlDateTimeToDate((String)row[4]));
                int days = Days.daysBetween(since, new DateTime()).getDays();
                double wpl = (double)words / (double)lines;
                double wpd = (double)words / days;
                double lpd = (double)lines / days;
                DecimalFormat sf = new DecimalFormat("0.0");

                bot.msg(channel, message + " has uttered "+words+ " words in "+lines+" lines over " + days + " days ("+
                        sf.format(wpl) + " wpl, " +
                        sf.format(wpd) + " wpd, " +
                        sf.format(lpd) + " lpd)");
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.msg(channel, "Sorry, unable to fetch WordCount data; an SQL error occured.");
        } catch(ParseException e) {
            System.err.println("Unable to parse the SQL datetime!");
            bot.msg(channel, "Sorry, I was unable to parse the date of this wordcount! Patches are welcome.");
        }
    }
}
