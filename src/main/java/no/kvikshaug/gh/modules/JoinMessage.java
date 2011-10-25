package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

import java.util.Date;
import java.util.List;

public class JoinMessage implements TriggerListener, JoinListener {
    private static final String TRIGGER = "onjoin";

    private Grouphug bot;
    private List<JoinMessageItem> messages;

    public JoinMessage(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        if(SQL.isAvailable()) {
            messages = JWorm.get(JoinMessageItem.class);
            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.addJoinListener(this);
            moduleHandler.registerHelp(TRIGGER, "Join message: say a message every time somebody joins\n" +
                    "  " + Grouphug.MAIN_TRIGGER + TRIGGER + "<nick> <message>\n");
        } else {
            System.err.println("JoinMessage module disabled: SQL is unavailable.");
        }
    }

    public void onJoin(String channel, String sender, String login, String hostname) {
        for(JoinMessageItem m : messages) {
            if(m.getNick().equals(sender) && m.getChannel().equals(channel)) {
                bot.msg(channel, String.format("%s: %s", sender, m.getMessage()));
            }
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

        boolean exists = false;
        for(JoinMessageItem m : messages) {
            if(m.getNick().equals(nick) && m.getChannel().equals(channel)) {
                m.setMessage(onJoin);
                m.setAuthor(sender);
                m.setDate(new Date().getTime());
                m.update();
                exists = true;
            }
        }
        if(!exists) {
            JoinMessageItem newItem = new JoinMessageItem(
                nick, onJoin, sender, new Date().getTime(), channel);
            newItem.insert();
            messages.add(newItem);
        }
        bot.msg(channel, String.format("Set join message for %s to %s.", nick, onJoin));
    }

    public static class JoinMessageItem extends Worm {
        private String nick;
        private String message;
        private String author;
        private Long date;
        private String channel;
        
        public JoinMessageItem(String nick, String message, String author, Long date, String channel) {
            this.nick = nick;
            this.message = message;
            this.author = author;
            this.date = date;
            this.channel = channel;
        }

        public String getNick() {
            return nick;
        }

        public String getMessage() {
            return message;
        }

        public String getAuthor() {
            return author;
        }

        public Long getDate() {
            return date;
        }

        public String getChannel() {
            return channel;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public void setDate(Long date) {
            this.date = date;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }
    }
}
