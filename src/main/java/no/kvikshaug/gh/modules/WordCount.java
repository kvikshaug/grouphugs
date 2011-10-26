package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import org.joda.time.DateTime;
import org.joda.time.Days;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class WordCount implements TriggerListener, MessageListener {

    private static final String TRIGGER_HELP = "wordcount";
    private static final String TRIGGER = "wc";
    private static final String TRIGGER_TOP = "wctop";
    private static final String TRIGGER_BOTTOM = "wcbottom";
    private static final String TRIGGER_REMOVE = "wcrm";
    private static final int LIMIT = 5;
    private static final DateFormat df = new SimpleDateFormat("d. MMMMM yyyy");

    private Grouphug bot;

    public WordCount(ModuleHandler moduleHandler) {
        if(SQL.isAvailable()) {
            bot = Grouphug.getInstance();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addTriggerListener(TRIGGER_TOP, this);
            moduleHandler.addTriggerListener(TRIGGER_BOTTOM, this);
            moduleHandler.addTriggerListener(TRIGGER_REMOVE, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Counts the number of words/lines a person has said\n" +
                    "To check how many words someone has said, use " +Grouphug.MAIN_TRIGGER + TRIGGER + " <nick>\n" +
                    "Top 5: " + Grouphug.MAIN_TRIGGER + TRIGGER_TOP + "\n" +
                    "Bottom 5: " + Grouphug.MAIN_TRIGGER + TRIGGER_BOTTOM + "\n" +
                    "Remove stats count: " + Grouphug.MAIN_TRIGGER + TRIGGER_REMOVE + " <nick>");
        } else {
            System.err.println("WordCount module disabled: SQL is unavailable.");
        }
    }
    public void addWords(String channel, String sender, String message) {
        // This method to count words should be more or less failsafe:
        int newWords = message.trim().replaceAll(" {2,}+", " ").split(" ").length;

        List<Words> items = JWorm.getWith(Words.class, "where `nick`='" + sender +
          "' and `channel`='" + channel + "'");
        if(items.size() == 0) {
            Words w = new Words(newWords, 1, sender, new Date().getTime(), channel);
            w.insert();
        } else {
            Words w = items.get(0);
            w.setWords(w.getWords() + newWords);
            w.setLines(w.getLines() + 1);
            w.update();
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_REMOVE) || !message.endsWith(sender)) {
            addWords(channel, sender, message);
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(trigger.equals(TRIGGER)) {
            print(channel, message);
        } else if(trigger.equals(TRIGGER_TOP)) {
            showScore(channel, true);
        } else if(trigger.equals(TRIGGER_BOTTOM)) {
            showScore(channel, false);
        } else if(trigger.equals(TRIGGER_REMOVE)) {
        	if(true){ //Should be specified in the config, do later :3
        		bot.msg(channel, "Sorry, this functionality has been disabled for now");
                return;
        	}
            if(!sender.equalsIgnoreCase(message)) {
                bot.msg(channel, "Sorry, as a safety precaution, this function can only be used " +
                        "by a user with the same nick as the one that's being removed.");
            } else {
                List<Words> items = JWorm.getWith(Words.class, "where nick='" + message +
                    "' and channel='" + channel + "'");
                if(items.size() == 0) {
                    bot.msg(channel, "DB reports that no such nick has been recorded.");
                } else {
                    items.get(0).delete();
                    bot.msg(channel, sender+", you now have no words counted.");
                }
            }
        }
    }

    private void showScore(String channel, boolean top) {
        String reply;
        if(top) {
            reply = "The biggest losers are:\n";
        } else {
            reply = "The laziest idlers are:\n";
        }
        String order = top ? "desc" : "asc";
        List<Words> items = JWorm.getWith(Words.class, "where channel='" + channel +
            "' order by `words` " + order + " limit " + LIMIT);
        int place = 1;
        for(Words w : items) {
            long words = w.getWords();
            long lines = w.getLines();
            DateTime since = new DateTime(w.getSince());
            int days = Days.daysBetween(since, new DateTime()).getDays();
            double wpl = (double)words / (double)lines;
            double wpd = (double)words / days;
            double lpd = (double)lines / days;
            DecimalFormat sf = new DecimalFormat("0.0");

            reply += (place++)+". "+w.getNick()+ " ("+words+" words, "+lines+" lines, "+days+" days, "+
                    sf.format(wpl) + " wpl, " +
                    sf.format(wpd) + " wpd, " +
                    sf.format(lpd) + " lpd)\n";
        }
        if(top) {
            reply += "I think they are going to need a new keyboard soon.";
        } else {
            reply += "Lazy bastards...";
        }
        bot.msg(channel, reply);
    }

    private void print(String channel, String message){
        List<Words> items = JWorm.getWith(Words.class, "where nick like '" + message +
            "' and channel='" + channel + "'");
        if(items.size() == 0) {
            bot.msg(channel, message + " doesn't have any words counted.");
        } else {
            Words w = items.get(0);
            long words = w.getWords();
            long lines = w.getLines();
            DateTime since = new DateTime(w.getSince());
            int days = Days.daysBetween(since, new DateTime()).getDays();
            double wpl = (double)words / (double)lines;
            double wpd = (double)words / days;
            double lpd = (double)lines / days;
            DecimalFormat sf = new DecimalFormat("0.0");

            bot.msg(channel, message + " has uttered "+words+ " words in "+lines+" lines over " + days + " days ("+
                    sf.format(wpl) + " wpl, " +
                    sf.format(wpd) + " wpd, " +
                    sf.format(lpd) + " lpd)");
        }
    }

    public static class Words extends Worm {
        private long words;
        private long lines;
        private String nick;
        private long since;
        private String channel;

        public Words(long words, long lines, String nick, long since, String channel) {
            this.words = words;
            this.lines = lines;
            this.nick = nick;
            this.since = since;
            this.channel = channel;
        }

        public long getWords() {
            return this.words;
        }

        public long getLines() {
            return this.lines;
        }

        public String getNick() {
            return this.nick;
        }

        public long getSince() {
            return this.since;
        }

        public String getChannel() {
            return this.channel;
        }

        public void setWords(long words) {
            this.words = words;
        }

        public void setLines(long lines) {
            this.lines = lines;
        }
    }
}
