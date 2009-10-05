package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.util.SQL;
import grouphug.util.SQLHandler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

public class Seen implements GrouphugModule {

    private static final String TRIGGER_HELP = "seen";
    private static final String TRIGGER = "seen ";
    
    private static final String SEEN_DB = "seen";
	
	private SQLHandler sqlHandler;

    public Seen() {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
        } catch(ClassNotFoundException ex) {
            System.err.println("Seen startup error: SQL unavailable!");
            // TODO should disable this module at this point.
        }
    }
	
	
	
	public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
		return TRIGGER_HELP;
	}

	public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
		if(message.equals(TRIGGER_HELP)) {
            return "Seen: When someone last said something in this channel\n" +
                   "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>\n";
        }
        return null;
    }

	public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
		try {
            ArrayList<String> params = new ArrayList<String>();
            params.add(sender);
            Object[] row = sqlHandler.selectSingle("SELECT id, nick FROM "+ SEEN_DB +" WHERE nick='?' ;", params);

            if(row == null) {
                params.clear();
                params.add(sender);
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(message);
                sqlHandler.insert("INSERT INTO "+SEEN_DB+" (nick, date, lastwords) VALUES ('?', '?', '?');");
            } else {
                params.clear();
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(message);
                params.add(row[0] + "");
                sqlHandler.update("UPDATE "+SEEN_DB+" SET date='?', lastwords='?' WHERE id='?' ;");
            }

            /* The following is some good-faith attempt to do SQL properly

			PreparedStatement statement = sqlHandler.getConnection().prepareStatement("SELECT id, nick FROM "+ SEEN_DB+" WHERE nick=? ;");
			
			statement.setString(1, sender);
			sql.executePreparedSelect(statement);
						
			if(!sql.getNext()) {
				statement = sql.getConnection().prepareStatement("INSERT INTO "+SEEN_DB+" (nick, date, lastwords) VALUES (? , now(), ? );");
				statement.setString(1, sender);
				statement.setString(2, message);
				sql.executePreparedUpdate(statement);
			} else {
				Object[] values = sql.getValueList();				
				statement = sql.getConnection().prepareStatement("UPDATE "+SEEN_DB+" SET date=now(), lastwords= ? WHERE id= ? ;");
				statement.setString(1, message);
				BigInteger id = (BigInteger)(values[0]);
				int id2 = id.intValue();
				statement.setInt(2, id2);
				sql.executePreparedUpdate(statement);
			}
			*/

		} catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }
		
	}

	public void trigger(String channel, String sender, String login, String hostname, String message) {
		if(message.startsWith(TRIGGER)){
			print(message);
		}
		
	}
	
	private void print(String message){
        String nick = message.substring(TRIGGER.length());
        try{
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, date, lastwords FROM "+SEEN_DB+" WHERE nick='"+nick+"';");

            if(row == null) {
                Grouphug.getInstance().sendMessage(nick + " hasn't said anything yet.", false);
            } else {
                Date last = new Date(((Timestamp)row[2]).getTime());
                String lastwords = (String)row[3];

                Grouphug.getInstance().sendMessage(nick + " uttered \""+ lastwords+ "\" on " +last, false);
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, an SQL error occured.", false);
        }
    }

}
