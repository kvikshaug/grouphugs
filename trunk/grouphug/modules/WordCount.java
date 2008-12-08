package grouphug.modules;

import java.sql.SQLException;
import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.SQL;


public class WordCount implements GrouphugModule {
	private static Grouphug bot;

    private static final String DEFAULT_SQL_HOST = "127.0.0.1";
    private static final String DEFAULT_SQL_USER = "gh";
    private final String TRIGGER_HELP = "wordcount";
	private final String TRIGGER = "wordcount ";
	private final String WORDS_DB= "gh_words";

	
    public WordCount(Grouphug bot) {
        this.bot = bot;
    }
	
	public void addWords(String sender, String message){
		SQL sql = new SQL();
		String[] words = message.split(" ");
		int count = words.length;
		
	
		try{
			sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, null);
			sql.query("SELECT id, nick, count FROM "+WORDS_DB+" WHERE nick='"+sender+"';");
			
			
			if(!sql.getNext()) {
				sql.query("INSERT INTO "+WORDS_DB+" (nick, words) VALUES ('"+sender+"', '"+count+"');");
			}else{
				Object[] values = sql.getValueList();
				sql.query("UPDATE "+WORDS_DB+" SET count='"+((Integer)(values[2]) + count)+"' WHERE id='"+values[0]+"';");
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
        SQL sql = new SQL();
		String nick = message.substring(10, message.length());
        try{
			sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, null);
			sql.query("SELECT id, nick, count FROM "+WORDS_DB+" WHERE nick='"+nick+"';");


			if(!sql.getNext()) {
				bot.sendMessage(nick + "doesn't have any words counted.", false);
			}else{
				Object[] values = sql.getValueList();
				bot.sendMessage(nick + "has said "+values[2], false);
			}

		}catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            bot.sendMessage("Sorry, an SQL error occured.", false);
		}finally {
            sql.disconnect();
        }

	}
	public String helpMainTrigger(String channel, String sender, String login, String hostname, String message){
        if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Counts the number of words a person has said");
            bot.sendNotice(sender, "To check how many words someone has said, use " +Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>" );
        }
    }
	public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message){
		return false;
	}
			
}
