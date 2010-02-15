package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.exceptions.SQLUnavailableException;
import grouphug.listeners.MessageListener;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQL;
import grouphug.util.SQLHandler;

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

    public WordCount(ModuleHandler moduleHandler) {
        try {
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
            System.out.println("Wordcount module loaded.");
        } catch(SQLUnavailableException ex) {
            System.err.println("WordCount startup error: SQL is unavailable!");
        }
    }
    public void addWords(String sender, String message) {
        // This method to count words should be more or less failsafe:
        int newWords = message.trim().replaceAll(" {2,}+", " ").split(" ").length;

        try{
            Object[] row = sqlHandler.selectSingle("SELECT id, words, `lines` FROM "+WORDS_DB+" WHERE nick='"+sender+"';");
            if(row == null) {
                List<String> params = new ArrayList<String>();
                params.add(sender);
                params.add(newWords + "");
                params.add(SQL.dateToSQLDateTime(new Date()));
                sqlHandler.insert("INSERT INTO "+WORDS_DB+" (nick, words, `lines`, since) VALUES ('?', '?', '1', '?');", params);
            } else {
                long existingWords = ((Integer)row[1]);
                long existingLines = ((Integer)row[2]);
                sqlHandler.update("UPDATE "+WORDS_DB+" SET words='"+(existingWords + newWords)+"', `lines`='"+(existingLines + 1)+"' WHERE id='"+row[0]+"';");
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            e.printStackTrace();
            Grouphug.getInstance().sendMessage("Sorry, unable to update WordCounter DB; an SQL error occured.");
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_REMOVE) || !message.endsWith(sender)) {
            addWords(sender, message);
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(trigger.equals(TRIGGER)) {
            print(message);
        } else if(trigger.equals(TRIGGER_TOP)) {
            showScore(true);
        } else if(trigger.equals(TRIGGER_BOTTOM)) {
            showScore(false);
        } else if(trigger.equals(TRIGGER_REMOVE)) {
            if(!sender.equalsIgnoreCase(message)) {
                Grouphug.getInstance().sendMessage("Sorry, as a safety precaution, this function can only be used " +
                        "by a user with the same nick as the one that's being removed.");
            } else {
                try {
                    List<String> params = new ArrayList<String>();
                    params.add(message);
                    if(sqlHandler.delete("delete from "+WORDS_DB+" where nick=?;", params) == 0) {
                        Grouphug.getInstance().sendMessage("DB reports that no such nick has been recorded.");
                    } else {
                        Grouphug.getInstance().sendMessage(sender+", you now have no words counted.");
                    }
                } catch(SQLException ex) {
                    Grouphug.getInstance().sendMessage("Crap, SQL barfed on me. Check the logs if you wanna know why.");
                    System.err.println(ex);
                    ex.printStackTrace(System.err);
                }
            }
        }
    }

    private void showScore(boolean top) {
        String reply;
        if(top) {
            reply = "The biggest losers are:\n";
        } else {
            reply = "The laziest idlers are:\n";
        }
        try {
            String query = ("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" ORDER BY words ");
            if(top) {
                query += "DESC ";
            }
            query += "LIMIT "+LIMIT+";";
            List<Object[]> rows = sqlHandler.select(query);
            int place = 1;
            for(Object[] row : rows) {
                long words = ((Integer)row[2]);
                long lines = ((Integer)row[3]);
                double wpl = (double)words / (double)lines;
                Date since = SQL.sqlDateTimeToDate((String)row[4]);
                reply += (place++)+". "+row[1]+ " ("+words+" words, "+lines+" lines, "+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl)\n";// since "+df.format(since)+"\n";
            }
            if(top) {
                reply += "I think they are going to need a new keyboard soon.";
            } else {
                reply += "Lazy bastards...";
            }
            Grouphug.getInstance().sendMessage(reply);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.");
        } catch(ParseException e) {
            System.err.println("Unable to parse the SQL datetime!");
            Grouphug.getInstance().sendMessage("Sorry, I was unable to parse the date of this wordcount! Patches are welcome.");
        }
    }

    private void print(String message){
        try{
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" WHERE nick LIKE'"+ message +"';");

            if(row == null) {
                Grouphug.getInstance().sendMessage(message + " doesn't have any words counted.");
            } else {
                long words = ((Integer)row[2]);
                long lines = ((Integer)row[3]);
                Date since = SQL.sqlDateTimeToDate((String)row[4]);
                double wpl = (double)words / (double)lines;

                Grouphug.getInstance().sendMessage(message + " has uttered "+words+ " words in "+lines+" lines ("+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl)");// since "+df.format(since));
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, unable to fetch WordCount data; an SQL error occured.");
        } catch(ParseException e) {
            System.err.println("Unable to parse the SQL datetime!");
            Grouphug.getInstance().sendMessage("Sorry, I was unable to parse the date of this wordcount! Patches are welcome.");
        }
    }
}
