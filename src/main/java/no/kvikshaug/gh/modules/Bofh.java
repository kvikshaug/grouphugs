package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.util.List;

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
        if(SQL.isAvailable()) {
            moduleHandler.addTriggerListener(RANDOM_TRIGGER, this);
            moduleHandler.registerHelp(HELP_TRIGGER, "BOFH - Fend off lusers with Bastard Operator From Hell excuses for their system \"problems\".\n" +
                    "Usage:\n" +
                    Grouphug.MAIN_TRIGGER + RANDOM_TRIGGER + " returns a random excuse.\n" +
                    Grouphug.MAIN_TRIGGER + RANDOM_TRIGGER + " " + SPECIFIC_TRIGGER + " returns an excuse by number. (\\d+ means any digit, 1-n times)");
        } else {
            System.err.println("BOFH module disabled: needs to load stored data from SQL.");
        }
    }

    private String getExcuse(String number) {
        List<BofhExcuse> excuses = JWorm.getWith(BofhExcuse.class, "where id='" + number + "'");
        if(excuses.size() == 1) {
            return excuses.get(0).getFormattedExcuse();
        } else {
            return "No bofh excuse for that number. Try a lower number.";
        }
    }

    private String getRandomExcuse() {
        List<BofhExcuse> excuses = JWorm.getWith(BofhExcuse.class, " order by random() limit 1");
        if(excuses.size() == 1) {
            return excuses.get(0).getFormattedExcuse();
        } else {
            return "Random managed to pick an excuse that didn't exist.";
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(message.equals("")) {
            Grouphug.getInstance().msg(channel, getRandomExcuse());
        } else {
            Grouphug.getInstance().msg(channel, getExcuse(message));
        }
    }

    public static class BofhExcuse extends Worm {
        private String excuse;

        public BofhExcuse(String excuse) {
            this.excuse = excuse;
        }

        public String getExcuse() {
            return excuse;
        }

        public String getFormattedExcuse() {
            return String.format("BOFH excuse #%s: %s", wormDbId().get(), getExcuse());
        }
    }
}
