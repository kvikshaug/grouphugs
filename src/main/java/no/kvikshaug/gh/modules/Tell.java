package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.NickChangeListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.SQLHandler;
import org.jibble.pircbot.User;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Tell implements JoinListener, TriggerListener, NickChangeListener {
    private static final String TRIGGER_HELP = "tell";
    private static final String TRIGGER = "tell";
    private static final String TELL_DB = "tell";
    public static final PeriodFormatter FORMATTER = new PeriodFormatterBuilder()
                        .appendSeconds().appendSuffix(" seconds ago")
                        .appendMinutes().appendSuffix(" minutes ago")
                        .appendHours().appendSuffix(" hours ago")
                        .appendDays().appendSuffix(" days ago")
                        .appendMonths().appendSuffix(" months ago")
                        .appendYears().appendSuffix(" years ago")
                        .printZeroNever()
                        .toFormatter();

    private SQLHandler sqlHandler;
    private Grouphug bot;

    public Tell(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addJoinListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Tell: Tell something to someone who's not here when they eventually join\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>\n");
            System.out.println("Tell module loaded.");
        } catch (SQLUnavailableException ex) {
            System.err.println("Tell module startup error: SQL is unavailable!");
        }
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
        tell(channel, sender);
    }

    private void tell(String channel, String sender) {
        List<String> params = new ArrayList<String>();
        params.add(sender);
        params.add(channel);
        List<Object[]> rows = null;
        try {
            rows = sqlHandler.select("SELECT id, from_nick, date, msg, channel FROM " + TELL_DB + " WHERE to_nick=? AND channel=?;", params);
        } catch (SQLException e) {
            System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
            bot.sendMessageChannel(channel, "Sorry, couldn't query Tell DB, an SQL error occured.");
            return;
        }

        for (Object[] row : rows) {
            if (!channel.equals(row[4])) {
                continue; // don't tell to wrong channel
            }
            String fromNick = (String) row[1];
            Date savedAt = null;
            String msg = (String) row[3];
            try {
                savedAt = SQL.sqlDateTimeToDate((String) row[2]);
            } catch (ParseException e) {
                System.err.println(" > Date Parse Exception: " + e.getMessage() + "\n" + e.getCause());
            }

            StringBuilder message = new StringBuilder();
            if (savedAt != null) {
                Period period = new Period(new DateTime(savedAt), new DateTime());
                message.append(FORMATTER.print(period)).append(", ");
            }
            message.append(fromNick).append(" told me to tell you this: ").append(msg);
            bot.sendMessageChannel(channel, message.toString());

            params.clear();
            params.add((String)row[0]);
            try {
                sqlHandler.delete("DELETE FROM " + TELL_DB + " WHERE id=?;", params);
            } catch (SQLException e) {
                System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
                Grouphug.getInstance().sendMessageChannel(channel, "Sorry, couldn't delete Tell, an SQL error occured.");
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
            bot.sendMessageChannel(channel, "Bogus message format: try !" + TRIGGER + " <nick> <message>.");
            return;
        }

        for (User user : bot.getUsers(channel)) {
            if (user.equals(toNick)) {
                bot.sendMessageChannel(channel, toNick + " is here right now, you dumbass!");
                return;
            }
        }

        List<String> params = new ArrayList<String>();
        params.add(sender);
        params.add(toNick);
        params.add(SQL.dateToSQLDateTime(new Date()));
        params.add(msg);
        params.add(channel);

        try {
            sqlHandler.insert("INSERT INTO " + TELL_DB + " (from_nick, to_nick, date, msg, channel) VALUES (?, ?, ?, ?, ?);", params);
        } catch (SQLException e) {
            System.err.println(" > SQL Exception: " + e.getMessage() + "\n" + e.getCause());
            bot.sendMessageChannel(channel, "Sorry, unable to update Tell DB, an SQL error occured.");
        }

        bot.sendMessageChannel(channel, "I'll tell " + toNick + " this: " + msg);
    }

    public void onNickChange(String oldNick, String login, String hostname, String newNick) {
        for (String c : bot.getChannels()) {
            tell(c, newNick);
        }
    }
}
