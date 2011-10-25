package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.NickChangeListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;
import org.jibble.pircbot.User;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.util.Date;
import java.util.List;


public class Tell implements JoinListener, TriggerListener, NickChangeListener, MessageListener {
    private static final String TRIGGER_HELP = "tell";
    private static final String TRIGGER = "tell";

    private Grouphug bot;

    public Tell(ModuleHandler moduleHandler) {
        if(SQL.isAvailable()) {
            bot = Grouphug.getInstance();
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addJoinListener(this);
            moduleHandler.addNickChangeListener(this);
            moduleHandler.addMessageListener(this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Tell: Tell something to someone who's not here when they eventually join\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick> <message>\n");
        } else {
            System.err.println("Tell module disabled: SQL is unavailable.");
        }
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
        tell(channel, sender);
    }

    private void tell(String channel, String toNick) {
        List<TellItem> items = JWorm.getWith(TellItem.class, "where `to`='" + SQL.sanitize(toNick) +
          "' and `channel`='" + SQL.sanitize(channel) + "'");
        for(TellItem item : items) {
            StringBuilder message = new StringBuilder();
            message.append(item.getTo()).append(": ").append(item.getFrom())
              .append(" told me to tell you this: ").append(item.getMessage());
            bot.msg(channel, message.toString());
            item.delete();
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
        TellItem newItem = new TellItem(fromNick, toNick, msg, new Date().getTime(), channel);
        newItem.insert();
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

            List<Seen.SeenItem> seens = JWorm.get(Seen.SeenItem.class);
            boolean save = false;
            for(Seen.SeenItem s : seens) {
                if(s.getNick().equals(toNick)) {
                    save = true;
                    break;
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

    public static class TellItem extends Worm {
        private String from;
        private String to;
        private String message;
        private Long date;
        private String channel;

        public TellItem(String from, String to, String message, Long date, String channel) {
            this.from = from;
            this.to = to;
            this.message = message;
            this.date = date;
            this.channel = channel;
        }

        public String getFrom() {
            return this.from;
        }

        public String getTo() {
            return this.to;
        }

        public String getMessage() {
            return this.message;
        }

        public Long getDate() {
            return this.date;
        }

        public String getChannel() {
            return this.channel;
        }
    }
}
