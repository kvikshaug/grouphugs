package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class Restart implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "restart";
    private static final String TRIGGER_ALT = "rs";
    private static final String TRIGGER_HELP = "restart";

    public Restart(Grouphug bot) {
        Restart.bot = bot;
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Restart: Updates svn, recompiles the bot and restarts it.");
            bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER);
            bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER_ALT);
            return true;
        }
        return false;
    }


    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER) || message.equals(TRIGGER_ALT)) {
            try {
                Process child = Runtime.getRuntime().exec("/bin/sh");
                BufferedWriter outCommand = new BufferedWriter(new
                OutputStreamWriter(child.getOutputStream()));
                outCommand.write("/home/DT2006/murray/gh/updategh.sh");
                outCommand.newLine();
                outCommand.flush();
            } catch (IOException e) {
                bot.sendMessage("Looks like HiNux barfed on me, caught IOException while trying to restart.", false);
                System.err.println("Caught IOException while trying to restart the bot:");
                System.err.println(e);
            }
        }
    }
}
