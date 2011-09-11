package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.SQLHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JoinMessage implements TriggerListener, JoinListener {
    private static final String TRIGGER = "onjoin";
    private static final String JOIN_DB = "joinmsg";

    private SQLHandler sqlHandler;
    private Grouphug bot;

    public JoinMessage(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addJoinListener(this);
            moduleHandler.registerHelp(TRIGGER, "Join message: say a message every time somebody joins\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick> <message>\n");
        } catch (SQLUnavailableException ex) {
            System.err.println("JoinMessage module startup error: SQL is unavailable!");
        }
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
        List<String> selectParams = new ArrayList<String>();
        selectParams.add(sender);
        selectParams.add(channel);
        Object[] row = null;
        try {
            row = sqlHandler.selectSingle("SELECT msg FROM " + JOIN_DB + " WHERE nick=? AND channel=?;", selectParams);
        } catch (SQLException e) {
            System.err.println(String.format("Caught an SQLException while trying to get join message for %s on %s", sender, channel));
        }
        if (row != null) {
            bot.msg(channel, String.format("%s: %s", sender, row[0]));
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String nick;
        String onJoin;
        try {
            String[] arr = message.split(" ", 2);
            nick = arr[0];
            onJoin = arr[1];
        } catch (IndexOutOfBoundsException ioobe) {
            bot.msg(channel, "Invalid join message arguments.");
            return;
        }
        List<String> selectParams = new ArrayList<String>();
        selectParams.add(nick);
        selectParams.add(channel);
        try {
            Object[] row = sqlHandler.selectSingle("SELECT id, nick, channel FROM " + JOIN_DB + " WHERE nick=? AND channel=?;", selectParams);
            if (row != null) { // nick already has an onJoin message, update it with the new one
                List<String> params = new ArrayList<String>();
                params.add(sender);
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(onJoin);
                params.add(Integer.toString((Integer) row[0]));
                sqlHandler.update("UPDATE " + JOIN_DB + " SET added_by=?, date=?, msg=? WHERE id=?", params);
            } else { // nick has no onJoin message already, do insert
                List<String> params = new ArrayList<String>();
                params.add(channel);
                params.add(sender);
                params.add(SQL.dateToSQLDateTime(new Date()));
                params.add(nick);
                params.add(onJoin);
                sqlHandler.insert("INSERT INTO " + JOIN_DB + "(channel, added_by, date, nick, msg) VALUES (?, ?, ?, ?, ?)", params);
            }
        } catch (SQLException e) {
            bot.msg(channel, String.format("Unable to set join message for %s because of an SQLException.", nick));
            return;
        }
        bot.msg(channel, String.format("Set join message for %s to %s.", nick, onJoin));
    }
}
