package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.listeners.TriggerListener;

public class Timer implements TriggerListener {

    private Grouphug bot;

    public Timer(ModuleHandler handler) {
        handler.addTriggerListener("timer", this);
        String helpText = "Use timer to time stuff, like your pizza.\n" +
                "!time count[s/m/h/d] [message]\n" +
                "s/m/h/d = seconds/minutes/hours/days (optional, default is minutes)\n" +
                "Example: !timer 14m grandis\n" +
                "Note that I will forget to notify you if I am rebooted!";
        handler.registerHelp("timer", helpText);
        bot = Grouphug.getInstance();
        System.out.println("Timer module loaded.");
    }

    @Override
    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        int indexAfterCount = 0;
        try {
            while(true) {
                if(indexAfterCount == message.length()) {
                    // there are no more chars after this one
                    break;
                }
                Integer.parseInt(message.substring(indexAfterCount, indexAfterCount+1));
                indexAfterCount++;
            }
        } catch(NumberFormatException e) {
            if(indexAfterCount == 0) {
                // message didn't start with a number
                bot.sendMessage("'" + message + "' doesn't start with a valid number, does it now? Try '!help timer'.");
                return;
            }
            // indexAfterCount is now the index after the count
        }
        int factor;
        String reply;
        String notifyMessage;
        int count = Integer.parseInt(message.substring(0, indexAfterCount));
        // if there are no chars after the count
        if(indexAfterCount == message.length()) {
            factor = 60;
            reply = "minutes";
            notifyMessage = "";
        } else {
            switch(message.charAt(indexAfterCount)) {
                case 's':
                    factor = 1;
                    reply = "seconds";
                    break;

                case 'm':
                case ' ':
                    factor = 60;
                    reply = "minutes";

                    break;

                case 'h':
                case 't':
                    factor = 3600;
                    reply = "hours";

                    break;

                case 'd':
                    factor = 86400;
                    reply = "days";
                    break;

                default:
                    bot.sendMessage("No. Try '!help timer'.");
                    return;

            }
            notifyMessage = message.substring(indexAfterCount + 1).trim();
        }
        if(count == 1) {
            // not plural, strip 's'
            reply = reply.substring(0, reply.length()-1);
        }
        bot.sendMessage("Ok, I will highlight you in " + count + " " + reply + ".");
        new Sleeper(sender, count * factor * 1000, notifyMessage);
    }

    private class Sleeper implements Runnable {
        private String nick;
        private int sleepAmount; // ms
        private String notifyMessage;

        private Sleeper(String nick, int sleepAmount, String notifyMessage) {
            this.nick = nick;
            this.sleepAmount = sleepAmount;
            this.notifyMessage = notifyMessage;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                Thread.sleep(sleepAmount);
            } catch(InterruptedException e) {
                bot.sendMessage(nick + ": Sorry, I caught an InterruptedException! I was supposed to highlight you " +
                        "after " + (sleepAmount / 1000) + " seconds, but I don't know how long I've slept.");
                return;
            }
            if("".equals(notifyMessage)) {
                bot.sendMessage(nick + ": Time's up!");
            } else {
                bot.sendMessage(nick + ": " + notifyMessage);
            }
        }
    }
}
