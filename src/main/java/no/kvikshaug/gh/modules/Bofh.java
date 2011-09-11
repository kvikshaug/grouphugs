package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Need a quick excuse to shut a luser up? Look no further, the BOFH module will assist you.
 *
 * User: sunn/oyvindio
 * Date: 27.nov.2008
 * Time: 23:34:16
 */
public class Bofh implements TriggerListener {

    private static final String RANDOM_TRIGGER = "bofh";
    private static final String SPECIFIC_TRIGGER = "\\d+";
    public static final String HELP_TRIGGER = RANDOM_TRIGGER;

    public Bofh(ModuleHandler moduleHandler) {
            moduleHandler.addTriggerListener(RANDOM_TRIGGER, this);
            moduleHandler.registerHelp(HELP_TRIGGER, "BOFH - Fend off lusers with Bastard Operator From Hell excuses for their system \"problems\".\n" +
                    "Usage:\n" +
                    Grouphug.MAIN_TRIGGER + RANDOM_TRIGGER + " returns a random excuse.\n" +
                    Grouphug.MAIN_TRIGGER + RANDOM_TRIGGER + " " + SPECIFIC_TRIGGER + " returns an excuse by number. (\\d+ means any digit, 1-n times)");
    }

    private String formatExcuse(String id, String excuse) {
        return String.format("BOFH excuse #%s: %s", id, excuse);
    }

    private String getExcuse(String number) {
        String excuse = null;
        try {

            SQLHandler sql = SQLHandler.getSQLHandler();
            Object[] row = sql.selectSingle("SELECT id, excuse FROM bofh WHERE id='?'", Arrays.asList(number));
            if (row != null) {
                excuse = formatExcuse(((Integer)row[0]).toString(), (String)row[1]);
            }
        } catch (ClassCastException e) {
            System.err.println("BOFH failed: Yell at the developers!");
        } catch (SQLUnavailableException e) {
            System.err.println("BOFH failed: SQL is unavailable!");
        } catch (SQLException e) {
            return "No bofh excuse for that number. Try a lower number.";
        }
        return excuse;
    }

    private String getRandomExcuse() {
        String excuse = null;
        try {

            SQLHandler sql = SQLHandler.getSQLHandler();
            Object[] row = sql.selectSingle("SELECT id, excuse FROM bofh ORDER BY RANDOM() LIMIT 1");
            if (row != null) {
                excuse = formatExcuse(((Integer)row[0]).toString(), (String)row[1]);
            }
        } catch (ClassCastException e) {
            System.err.println("BOFH failed: Yell at the developers!");
        } catch (SQLUnavailableException e) {
            System.err.println("BOFH failed: SQL is unavailable!");
        } catch (SQLException e) {
            return "No bofh excuse for that number. Try a lower number.";
        }
        return excuse;
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String reply;
        if(message.equals("")) {
            reply = getRandomExcuse();
        } else {
            reply = getExcuse(message);
        }

        if (reply != null) {
            Grouphug.getInstance().msg(channel, reply);
        }
    }
}
