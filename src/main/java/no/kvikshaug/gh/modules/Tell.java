package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.NickChangeListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.SQLHandler;
import org.jibble.pircbot.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Tell implements JoinListener, TriggerListener, NickChangeListener, MessageListener {
    private static final String TRIGGER_HELP = "tell";
    private static final String TRIGGER = "tell";
    private static final String TELL_DB = "tell";

    private SQLHandler sqlHandler;
    private Grouphug bot;

    public Tell(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addJoinListener(this);
            moduleHandler.addNickChangeListener(this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Tell: Tell something to someone who's not here when they eventually join\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick> <message>\n");
            System.out.println("Tell module loaded.");
        } catch (SQLUnavailableException ex) {
            System.err.println("Tell module startup error: SQL is unavailable!");
        }
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
        tell(channel, sender);
    }

    private void tell(String channel, String toNick) {
        List<String> params = new ArrayList<String>();
        params.add(toNick);
        params.add(channel);
        List<Object[]> rows = null;
        try {
            rows = sqlHandler.select("SELECT id, from_nick, date, msg, channel FROM " + TELL_DB + " WHERE to_nick=? AND channel=?;", params);
        } catch (SQLException e) {
            System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
            bot.msg(channel, "Sorry, couldn't query Tell DB, an SQL error occured.");
            return;
        }

        for (Object[] row : rows) {
            String fromNick = (String) row[1];
            String msg = (String) row[3];
            StringBuilder message = new StringBuilder();
            message.append(toNick).append(": ").append(fromNick).append(" told me to tell you this: ").append(msg);
            bot.msg(channel, message.toString());

            params.clear();
            params.add(row[0].toString());
            try {
                sqlHandler.delete("DELETE FROM " + TELL_DB + " WHERE id=?;", params);
            } catch (SQLException e) {
                System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
                Grouphug.getInstance().msg(channel, "Sorry, couldn't delete Tell, an SQL error occured.");
            }
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String toNick = null;
        String msg = null;
        try {
            toNick = message.substring(0, message.indexOf(' '));
            msg = message.substring(message.indexOf(' '));
        } catch (IndexOutOfBoundsException ioobe) {
            bot.msg(channel, "Bogus message format: try !" + TRIGGER + " <nick> <message>.");
            return;
        }

        for (User user : bot.getUsers(channel)) {
            if (user.equals(toNick)) {
                bot.msg(channel, toNick + " is here right now, you dumbass!");
                return;
            }
        }
        saveTell(channel, sender, toNick, msg);
    }

    private void saveTell(String channel, String fromNick, String toNick, String msg) {
        List<String> params = new ArrayList<String>();
        params.add(fromNick);
        params.add(toNick);
        params.add(SQL.dateToSQLDateTime(new Date()));
        params.add(msg);
        params.add(channel);

        try {
            sqlHandler.insert("INSERT INTO " + TELL_DB + " (from_nick, to_nick, date, msg, channel) VALUES (?, ?, ?, ?, ?);", params);
        } catch (SQLException e) {
            System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
            bot.msg(channel, "Sorry, unable to update Tell DB, an SQL error occured.");
        }

        bot.msg(channel, "I'll tell " + toNick + " this: " + msg);
    }

    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
        for (String chan : bot.getChannels()) {
            for (User user : bot.getUsers(chan)) {
                if (user.equals(newNick)) {
                    tell(chan, newNick);
                }
            }
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.matches("^(\\w+):.+") && !message.matches("(\\w+)://.+")) {
            String toNick = message.substring(0, message.indexOf(':'));
            String msg = message.substring(message.indexOf(':') + 1, message.length());

            List<Object[]> rows = null;
            try {
                rows = sqlHandler.select("SELECT nick FROM seen;");
            } catch (SQLException e) {
                System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
            }

            boolean save = false;
            if (rows != null) {
                for (Object[] row : rows) {
                    if (row[0].equals(toNick)) {
                        save = true;
                        break;
                    }
                }
            }

            for (User user : bot.getUsers(channel)) {
                if (user.equals(toNick)) {
                    return;
                }
            }

            if (save) {
                saveTell(channel, sender, toNick, msg);
            }
        }
    }
}
