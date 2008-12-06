package grouphug.modules;

import java.sql.SQLException;
import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.SQL;


public class WordCount implements GrouphugModule {
	private static Grouphug bot;
	
	private final String TRIGGER = "Wordcount";
	private final String WORDS_DB= "gh_words";

	
    public WordCount(Grouphug bot) {
        this.bot = bot;
    }
	
	public void addWords(String sender, String message){
		SQL sql = new SQL();
		String[] words = message.split(" ");
		int count = words.length;
		
	
		try{
			sql.connect();
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
		//maybe later some time
	}
	public void trigger(String channel, String sender, String login, String hostname, String message){
		//laterz
	}
	public String helpMainTrigger(String channel, String sender, String login, String hostname, String message){
        //Not now
    }
	 public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message){
		 //Bah
	 }
			
}
