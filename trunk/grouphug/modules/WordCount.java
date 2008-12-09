package grouphug.modules;

import java.sql.SQLException;
import java.text.DecimalFormat;

import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.SQL;


public class WordCount implements GrouphugModule {
	private static Grouphug bot;

    private static final String DEFAULT_SQL_HOST = "127.0.0.1";
    private static final String DEFAULT_SQL_USER = "gh";
    private static final String TRIGGER_HELP = "wordcount";
	private static final String TRIGGER = "wordcount ";
	private static final String WORDS_DB= "gh_words";
    private static final String TRIGGER_TOP = "wordcounttop";
    private static final String TRIGGER_BOTTOM = "wordcountbottom";
    private static final int LIMIT = 5;

	
    public WordCount(Grouphug bot) {
        WordCount.bot = bot;
    }
	
	public void addWords(String sender, String message){
		SQL sql = new SQL();

        bot.sendMessage("Starting method", false);

        //Sunn cheats
        message = message.replaceAll("  ", "");
		int newWords = message.split(" ").length;

        bot.sendMessage("Split on words, wordcount is "+newWords, false);
		
	
		try{
			sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, null);
			sql.query("SELECT id, words, `lines` FROM "+WORDS_DB+" WHERE nick='"+sender+"';");
			
			
			if(!sql.getNext()) {
                bot.sendMessage("Inserting new user '"+sender+"'", false);
				sql.query("INSERT INTO "+WORDS_DB+" (nick, words, `lines`) VALUES ('"+sender+"', '"+newWords+"', '1');");
			}else{
                bot.sendMessage("Found user "+sender+", inserting", false);
				Object[] values = sql.getValueList();
                long existingWords = 0;
                long existingLines = 0;
                bot.sendMessage("Starting words extraction", false);
                try {
                    existingWords = ((Integer)values[1]);
                } catch(ClassCastException ex) {
                    bot.sendMessage("Caught WORDS CCE: "+ex, false);
                }
                bot.sendMessage("Starting lines extraction", false);
                try {
                    existingLines = ((Integer)values[2]);
                } catch(ClassCastException ex) {
                    bot.sendMessage("Caught LINES CCE: "+ex, false);
                }

				sql.query("UPDATE "+WORDS_DB+" SET words='"+(existingWords + newWords)+"', `lines`='"+(existingLines + 1)+"' WHERE id='"+values[0]+"';");
			}

		}catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occured.", false);
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
	public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message){
		if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Counts the number of words/lines a person has said");
            bot.sendNotice(sender, "To check how many words someone has said, use " +Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>" );
        }
        return true;
	}

    private void showScore(boolean top) {
        SQL sql = new SQL();
        String reply;
        if(top)
            reply = "The biggest losers are:\n";
        else
            reply = "The laziest idlers are:\n";
        try {
            sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, null);
            String query = ("SELECT id, nick, words, `lines` FROM "+WORDS_DB+" ORDER BY words ");
            if(top)
                query += "DESC ";
            query += "LIMIT "+LIMIT+";";
            sql.query(query);
            int place = 1;
            while(sql.getNext()) {
                Object[] values = sql.getValueList();
                long words = ((Long)values[2]);
                long lines = ((Long)values[3]);
                long wpl = words / lines;
                reply += (place++)+". "+ values[1]+ " ("+words+" words, "+lines+" lines, "+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl)\n";
            }
            if(top)
                reply += "I think they are going to need a new keyboard soon.";
            else
                reply += "Lazy bastards...";
            bot.sendMessage(reply, false);
        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occured.", false);
        }finally {
            sql.disconnect();
        }
    }

    private void print(String message){
        SQL sql = new SQL();
        String nick = message.substring(10, message.length());
        try{
            sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, null);
            sql.query("SELECT id, nick, words, `lines` FROM "+WORDS_DB+" WHERE nick='"+nick+"';");


            if(!sql.getNext()) {
                bot.sendMessage(nick + " doesn't have any words counted.", false);
            }else{
                Object[] values = sql.getValueList();
                long words = ((Long)values[2]);
                long lines = ((Long)values[3]);
                long wpl = words / lines;

                bot.sendMessage(nick + " has uttered "+words+ " words in "+lines+" lines ("+
                        (new DecimalFormat("0.0")).format(wpl)+
                        " wpl)", false);
            }

        }catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occured.", false);
        }finally {
            sql.disconnect();
        }

    }
}
