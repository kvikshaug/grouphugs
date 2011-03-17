package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.SQLHandler;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            List<String> params = new ArrayList<String>();
            params.add(sender);
            params.add(channel);
            Object[] row = sqlHandler.selectSingle("SELECT id FROM "+ SEEN_DB +" WHERE nick=? AND channel=?;", params);

            if(row == null) {
                params.clear();
                params.add(sender);
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(message);
                params.add(channel);
                sqlHandler.insert("INSERT INTO "+SEEN_DB+" (nick, date, lastwords, channel) VALUES (?, ?, ?, ?);", params);
            } else {
                params.clear();
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(message);
                params.add(row[0] + "");
                sqlHandler.update("UPDATE "+SEEN_DB+" SET date='?', lastwords='?' WHERE id='?' ;", params);
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().msg(channel, "Sorry, unable to update Seen DB, an SQL error occured.");
        }

    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            List<String> params = new ArrayList<String>();
            params.add(message);
            params.add(channel);
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, date, lastwords FROM "+SEEN_DB+" WHERE nick=? AND channel=?;", params);

            if(row == null) {
                Grouphug.getInstance().msg(channel, message + " hasn't said anything yet.");
            } else {
                Date last = SQL.sqlDateTimeToDate((String)row[2]);
                String lastwords = (String)row[3];

                Grouphug.getInstance().msg(channel, message + " uttered \""+ lastwords+ "\" on " +last);
            }

        } catch(SQLException e) {
            System.err.println(" > SQL Exception: "+e.getMessage()+"\n"+e.getCause());
            Grouphug.getInstance().msg(channel, "Sorry, unable to look up the requested data; an SQL error occured.");
        } catch (ParseException e) {
            System.err.println(" > Unable to parse the SQL date! This was very unexpected.");
            e.printStackTrace();
        }
    }
}
