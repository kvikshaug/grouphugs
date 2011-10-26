package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import no.kvikshaug.gh.util.Web;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.util.List;

public class Karma implements TriggerListener, MessageListener {

    private static final String TRIGGER_HELP = "karma";
    private static final String TRIGGER = "karma";
    private static final String TRIGGER_TOP = "karmatop";
    private static final String TRIGGER_BOTTOM = "karmabottom";
    private static final String TRIGGER_RESET = "karmareset";
    //private static final boolean CAN_RESET = false;

    private static final int LIMIT = 5; // how many items to show in karmatop/karmabottom

    private Grouphug bot;

    public Karma(ModuleHandler moduleHandler) {
        if(SQL.isAvailable()) {
            bot = Grouphug.getInstance();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Karma: Increase, decrease, or show an objects karma.\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + " <object>\n" +
                    "  <object>++\n" +
                    "  <object>--\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER_TOP+"\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER_BOTTOM+"\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER_RESET + " <object>" + " if resetting is enabled");
        } else {
            System.err.println("Karma disabled: SQL is unavailable.");
        }
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if(message.endsWith("++")) {
            add(channel, sender, message.substring(0, message.length()-2), 1);
        } else if(message.endsWith("--")) {
            add(channel,sender, message.substring(0, message.length()-2), -1);
        } else if(message.equals(Grouphug.MAIN_TRIGGER + TRIGGER_TOP)) {
            showScore(channel, true);
        } else if(message.equals(Grouphug.MAIN_TRIGGER + TRIGGER_BOTTOM)) {
            showScore(channel, false);
//        } else if(message.startsWith(Grouphug.MAIN_TRIGGER + TRIGGER_RESET) && CAN_RESET) {
//            add(sender, message.substring(11, message.length()), 0);
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        print(channel, message);
    }

    private String norwegianCharsToHtmlEntities(String str) {
        str = str.replace("æ", "&aelig;");
        str = str.replace("ø", "&oslash;");
        str = str.replace("å", "&aring;");
        str = str.replace("Æ", "&AElig;");
        str = str.replace("Ø", "&Oslash;");
        str = str.replace("Å", "&Aring;");
        return str;
    }

    private void print(String channel, String name) {
        String sqlName = norwegianCharsToHtmlEntities(name);
        List<KarmaItem> items = JWorm.getWith(KarmaItem.class, "where name like '" +
          sqlName + "' and channel='" + channel + "'");
        if(items.size() == 0) {
            bot.msg(channel, name+" has neutral karma.");
        } else if(items.size() > 1) {
            bot.msg(channel, name+" must have bad karma, because there are " +
              "several entries in the DB for that item. Please clean this up.");
        } else {
            bot.msg(channel, items.get(0).getName()+" has "+items.get(0).getKarma()+" karma.");
        }
    }

    private void add(String channel, String sender, String name, int karma) {
        if(name.equals(sender)) {
            bot.msg(channel, sender+", self karma is bad karma.");
            return;
        }

        String sqlName = norwegianCharsToHtmlEntities(name);

        // we sleep for a little while, in case the user is very fast - to avoid duplicate inserts
        try {
            Thread.sleep(600);
        } catch(InterruptedException e) {
            // interrupted, ok, just continue
        }
        List<KarmaItem> items = JWorm.getWith(KarmaItem.class, "where name like '" +
          sqlName + "' and channel='" + channel + "'");
        if(items.size() == 0) {
            KarmaItem newItem = new KarmaItem(sqlName, karma, channel);
            newItem.insert();
        } else if(items.size() > 1) {
            bot.msg(channel, "There are " + items.size() + " entries in the " +
            "DB for " + name + ". Please clean this up first.");
        } else {
            items.get(0).addKarma(karma);
            items.get(0).update();
        }
    }

    private void showScore(String channel, boolean top) {
        String reply;
        if(top) {
            reply = "Top five karma winners:\n";
        } else {
            reply = "Bottom five karma losers:\n";
        }
        String query = top ? "desc" : "asc";
        List<KarmaItem> items = JWorm.getWith(KarmaItem.class, "where channel='" +
          channel + "' order by `karma` limit " + LIMIT);
        int place = 1;
        for(KarmaItem item : items) {
            reply += (place++)+". "+Web.entitiesToChars(item.getName())+" ("+item.getKarma()+")\n";
        }
        if(top) {
            reply += "May their lives be filled with sunlight and pink stuff.";
        } else {
            reply += "May they burn forever in the pits of "+ channel+".";
        }
        bot.msg(channel, reply);
    }

    public static class KarmaItem extends Worm {

        private String name;
        private int karma;
        private String channel;

        public String getName() {
            return name;
        }

        public int getKarma() {
            return karma;
        }
        
        public String getChannel(){
        	return channel;
        }

        public KarmaItem(String name, int karma, String channel) {
            this.name = name;
            this.karma = karma;
            this.channel = channel;
        }

        public void addKarma(int karma) {
            this.karma += karma;
        }

        public String toString() {
            if(karma == 0)
                return "neutral";
            else
                return ""+karma;
        }
    }

}
