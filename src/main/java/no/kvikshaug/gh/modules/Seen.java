package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.util.Date;
import java.util.List;

public class Seen implements TriggerListener, MessageListener {

    private static final String TRIGGER_HELP = "seen";
    private static final String TRIGGER = "seen";

    public Seen(ModuleHandler moduleHandler) {
        if(SQL.isAvailable()) {
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Seen: When someone last said something in this channel\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick>\n");
        } else {
            System.err.println("Seen module disabled: SQL is unavailable.");
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        List<SeenItem> items = JWorm.getWith(SeenItem.class, "where nick='" + SQL.sanitize(sender) +
          "' and channel='" + SQL.sanitize(channel) + "'");
        if(items.size() == 0) {
            SeenItem newItem = new SeenItem(sender, message, new Date().getTime(), channel);
            newItem.insert();
        } else {
            items.get(0).setLastWords(message);
            items.get(0).setDate(new Date().getTime());
            items.get(0).update();
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        List<SeenItem> items = JWorm.getWith(SeenItem.class, "where nick='" + SQL.sanitize(message) +
          "' and channel='" + SQL.sanitize(channel) + "'");
        if(items.size() == 0) {
            Grouphug.getInstance().msg(channel, message + " hasn't said anything yet.");
        } else {
            Grouphug.getInstance().msg(channel, message + " uttered \""+ items.get(0).getLastWords() +
              "\" on " + new Date(items.get(0).getDate()));
        }
    }

    public static class SeenItem extends Worm {
        private String nick;
        private String lastWords;
        private long date;
        private String channel;

        public SeenItem(String nick, String lastWords, long date, String channel) {
            this.nick = nick;
            this.lastWords = lastWords;
            this.date = date;
            this.channel = channel;
        }

        public String getNick() {
            return this.nick;
        }

        public String getLastWords() {
            return this.lastWords;
        }

        public long getDate() {
            return this.date;
        }

        public String getChannel() {
            return this.channel;
        }

        public void setLastWords(String lastWords) {
            this.lastWords = lastWords;
        }

        public void setDate(long date) {
            this.date = date;
        }
    }
}
