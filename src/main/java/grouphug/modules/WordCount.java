package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.util.SQL;
import grouphug.util.SQLHandler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class WordCount implements GrouphugModule {

    private static final String TRIGGER_HELP = "wordcount";
	private static final String TRIGGER = "wordcount ";
	private static final String WORDS_DB= "words";
    private static final String TRIGGER_TOP = "wordcounttop";
    private static final String TRIGGER_BOTTOM = "wordcountbottom";
    private static final int LIMIT = 5;
    private static final DateFormat df = new SimpleDateFormat("d. MMMMM");

    private SQLHandler sqlHandler;

    public WordCount() {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
        } catch(ClassNotFoundException ex) {
            System.err.println("WordCount startup error: SQL unavailable!");
            // TODO should disable this module at this point.
        }
    }

	
	public void addWords(String sender, String message) {
        // This method to count words should be more or less failsafe:
        int newWords = message.trim().replaceAll(" {2,}+", " ").split(" ").length;

		try{
			Object[] row = sqlHandler.selectSingle("SELECT id, words, `lines` FROM "+WORDS_DB+" WHERE nick='"+sender+"';");
			if(row == null) {
                ArrayList<String> params = new ArrayList<String>();
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
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }
	}
	
	public void specialTrigger(String channel, String sender, String login, String hostname, String message){
		addWords(sender, message);
	}
	public void trigger(String channel, String sender, String login, String hostname, String message){
        if(message.startsWith(TRIGGER)){
            print(message);
        }
        else if(message.equals(TRIGGER_TOP))
            showScore(true);
        else if(message.equals(TRIGGER_BOTTOM))
            showScore(false);


	}
	public String helpMainTrigger(String channel, String sender, String login, String hostname, String message){
        return TRIGGER_HELP;
    }
	public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message){
		if(message.equals(TRIGGER_HELP)) {
            return "Counts the number of words/lines a person has said\n" +
                   "To check how many words someone has said, use " +Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>";
        }
        return null;
	}

    // TODO some duplicated code in the following two methods, can this be simplified ?

    private void showScore(boolean top) {
        String reply;
        if(top)
            reply = "The biggest losers are:\n";
        else
            reply = "The laziest idlers are:\n";
        try {
            String query = ("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" ORDER BY words ");
            if(top)
                query += "DESC ";
            query += "LIMIT "+LIMIT+";";
            ArrayList<Object[]> rows = sqlHandler.select(query);
            int place = 1;
            for(Object[] row : rows) {
                long words = ((Integer)row[2]);
                long lines = ((Integer)row[3]);
                double wpl = (double)words / (double)lines;
                Date since = new Date(((Timestamp)row[4]).getTime());
                reply += (place++)+". "+row[1]+ " ("+words+" words, "+lines+" lines, "+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl) since "+df.format(since)+"\n";
            }
            if(top)
                reply += "I think they are going to need a new keyboard soon.";
            else
                reply += "Lazy bastards...";
            Grouphug.getInstance().sendMessage(reply, false);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }
    }

    private void print(String message){
        String nick = message.substring(10, message.length());
        try{
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" WHERE nick='"+nick+"';");

            if(row == null) {
                Grouphug.getInstance().sendMessage(nick + " doesn't have any words counted.", false);
            } else {
                long words = ((Integer)row[2]);
                long lines = ((Integer)row[3]);
                Date since = new Date(((Timestamp)row[4]).getTime());
                double wpl = (double)words / (double)lines;

                Grouphug.getInstance().sendMessage(nick + " has uttered "+words+ " words in "+lines+" lines ("+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl) since "+df.format(since), false);
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }
    }
}
