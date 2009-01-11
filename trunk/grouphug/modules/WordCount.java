package grouphug.modules;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.text.DecimalFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import grouphug.Grouphug;
import grouphug.SQL;
import grouphug.GrouphugModule;
import grouphug.util.PasswordManager;


public class WordCount implements GrouphugModule {

    private static final String DEFAULT_SQL_HOST = "127.0.0.1";
    private static final String DEFAULT_SQL_USER = "gh";
    private static final String TRIGGER_HELP = "wordcount";
	private static final String TRIGGER = "wordcount ";
	private static final String WORDS_DB= "gh_words";
    private static final String TRIGGER_TOP = "wordcounttop";
    private static final String TRIGGER_BOTTOM = "wordcountbottom";
    private static final int LIMIT = 5;
    private static final DateFormat df = new SimpleDateFormat("d. MMMMM");

	
	public void addWords(String sender, String message) {
		SQL sql = new SQL();

        // This method to count words should be more or less failsafe:
        int newWords = message.trim().replaceAll(" {2,}+", " ").split(" ").length;

		try{
			sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
			sql.query("SELECT id, words, `lines` FROM "+WORDS_DB+" WHERE nick='"+sender+"';");
			
			
			if(!sql.getNext()) {
				sql.query("INSERT INTO "+WORDS_DB+" (nick, words, `lines`, since) VALUES ('"+sender+"', '"+newWords+"', '1', '"+SQL.dateToSQLDateTime(new Date())+"');");
			}else{
				Object[] values = sql.getValueList();
                long existingWords = ((Long)values[1]);
                long existingLines = ((Long)values[2]);
				sql.query("UPDATE "+WORDS_DB+" SET words='"+(existingWords + newWords)+"', `lines`='"+(existingLines + 1)+"' WHERE id='"+values[0]+"';");
			}

		}catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
		}finally {
            sql.disconnect();
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
        SQL sql = new SQL();
        String reply;
        if(top)
            reply = "The biggest losers are:\n";
        else
            reply = "The laziest idlers are:\n";
        try {
            sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
            String query = ("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" ORDER BY words ");
            if(top)
                query += "DESC ";
            query += "LIMIT "+LIMIT+";";
            sql.query(query);
            int place = 1;
            while(sql.getNext()) {
                Object[] values = sql.getValueList();
                long words = ((Long)values[2]);
                long lines = ((Long)values[3]);
                double wpl = (double)words / (double)lines;
                Date since = new Date(((Timestamp)values[4]).getTime());
                reply += (place++)+". "+ values[1]+ " ("+words+" words, "+lines+" lines, "+
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
        }finally {
            sql.disconnect();
        }
    }

    private void print(String message){
        SQL sql = new SQL();
        String nick = message.substring(10, message.length());
        try{
            sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
            sql.query("SELECT id, nick, words, `lines`, since FROM "+WORDS_DB+" WHERE nick='"+nick+"';");


            if(!sql.getNext()) {
                Grouphug.getInstance().sendMessage(nick + " doesn't have any words counted.", false);
            }else{
                Object[] values = sql.getValueList();
                long words = ((Long)values[2]);
                long lines = ((Long)values[3]);
                Date since = new Date(((Timestamp)values[4]).getTime());
                double wpl = (double)words / (double)lines;

                Grouphug.getInstance().sendMessage(nick + " has uttered "+words+ " words in "+lines+" lines ("+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl) since "+df.format(since), false);
            }

        }catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }finally {
            sql.disconnect();
        }
    }
}
