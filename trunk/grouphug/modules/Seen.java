package grouphug.modules;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Date;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.SQL;
import grouphug.util.PasswordManager;

public class Seen implements GrouphugModule {

    private static final String DEFAULT_SQL_HOST = "127.0.0.1";
    private static final String DEFAULT_SQL_USER = "gh";
    private static final String TRIGGER_HELP = "seen";
    private static final String TRIGGER = "seen ";
    
    private static final String SEEN_DB = "gh_seen";
	
	
	
	
	
	
	@Override
	public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
		return TRIGGER_HELP;
	}

	@Override
	public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
		if(message.equals(TRIGGER_HELP)) {
            return "Seen: When someone last said something in this channel\n" +
                   "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>\n";
        }
        return null;
    }

	@Override
	public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
		SQL sql = new SQL();
		try{
			sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
			PreparedStatement statement = sql.getConnection().prepareStatement("SELECT id, nick FROM ? WHERE nick=?;");
			
			statement.setString(0, SEEN_DB);
			statement.setString(1, sender);
			sql.executePreparedUpdate(statement);
						
			if(!sql.getNext()) {
				statement = sql.getConnection().prepareStatement("INSERT INTO ? (nick, date, lastwords) VALUES (?, now(),?);");
				statement.setString(0, SEEN_DB);
				statement.setString(1, sender);
				statement.setString(2, message);
				sql.executePreparedUpdate(statement);
			}else{
				Object[] values = sql.getValueList();
				sql.query("UPDATE "+SEEN_DB+" SET date=now(), lastwords='"+ message+"' 	WHERE id='"+values[0]+"';");
				
				statement = sql.getConnection().prepareStatement("UPDATE ? SET date=now(), lastwords=? WHERE id=?;");
				statement.setString(0, SEEN_DB);
				statement.setString(1, message);
				statement.setString(2, (String)values[0]);
			}

		}catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
		}finally {
            sql.disconnect();
        }
		
	}

	@Override
	public void trigger(String channel, String sender, String login, String hostname, String message) {
		if(message.startsWith(TRIGGER)){
			print(message);
		}
		
	}
	
	private void print(String message){
		SQL sql = new SQL();
        String nick = message.substring(TRIGGER.length());
        try{
            sql.connect(DEFAULT_SQL_HOST, "sunn", DEFAULT_SQL_USER, PasswordManager.getHinuxPass());
            sql.query("SELECT id, nick, date, lastwords FROM "+SEEN_DB+" WHERE nick='"+nick+"';");


            if(!sql.getNext()) {
                Grouphug.getInstance().sendMessage(nick + " hasn't said anything yet.", false);
            }else{
                Object[] values = sql.getValueList();
                Date last = new Date(((Timestamp)values[2]).getTime());
                String lastwords = (String)values[3];

                Grouphug.getInstance().sendMessage(nick + " uttered \""+ lastwords+ "\" on " +last, false);
            }

        }catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }finally {
            sql.disconnect();
        }
    }

}
