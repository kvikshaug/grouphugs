package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.exceptions.SQLUnavailableException;
import grouphug.listeners.MessageListener;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQL;
import grouphug.util.SQLHandler;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

public class Seen implements TriggerListener, MessageListener {

    private static final String TRIGGER_HELP = "seen";
    private static final String TRIGGER = "seen";
    private static final String SEEN_DB = "seen";

    private SQLHandler sqlHandler;

    public Seen(ModuleHandler moduleHandler) {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Seen: When someone last said something in this channel\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>\n");
            System.out.println("Seen module loaded.");
        } catch(SQLUnavailableException ex) {
            System.err.println("Seen module startup error: SQL is unavailable!");
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add(sender);
            Object[] row = sqlHandler.selectSingle("SELECT id FROM "+ SEEN_DB +" WHERE nick='?';", params);

            if(row == null) {
                params.clear();
                params.add(sender);
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(message);
                sqlHandler.insert("INSERT INTO "+SEEN_DB+" (nick, date, lastwords) VALUES ('?', '?', '?');", params);
            } else {
                params.clear();
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(message);
                params.add(row[0] + "");
                sqlHandler.update("UPDATE "+SEEN_DB+" SET date='?', lastwords='?' WHERE id='?' ;", params);
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
            Grouphug.getInstance().sendMessage("Sorry, unable to update Seen DB, an SQL error occured.");
        }

    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add(message);
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, date, lastwords FROM "+SEEN_DB+" WHERE nick='?';", params);

            if(row == null) {
                Grouphug.getInstance().sendMessage(message + " hasn't said anything yet.");
            } else {
                Date last = SQL.sqlDateTimeToDate((String)row[2]);
                String lastwords = (String)row[3];

                Grouphug.getInstance().sendMessage(message + " uttered \""+ lastwords+ "\" on " +last);
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().sendMessage("Sorry, unable to look up the requested data; an SQL error occured.");
        } catch (ParseException e) {
            System.err.println(" > Unable to parse the SQL date! This was very unexpected.");
            e.printStackTrace();
        }
    }
}
