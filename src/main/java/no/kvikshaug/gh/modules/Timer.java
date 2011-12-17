package no.kvikshaug.gh.modules;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.*;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQL;

import no.kvikshaug.worm.Worm;
import no.kvikshaug.worm.JWorm;

public class Timer implements TriggerListener {

    private Grouphug bot;
    private final DateTimeFormatter f = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy");

    public Timer(ModuleHandler handler) {
        handler.addTriggerListener("timer", this);
        String helpText = "Use timer to time stuff, like your pizza.\n" +
        "!timer count[s/m/h/d] [message] (seconds/minutes/hours/days)\n" +
        "!timer -for foo 10m Call me (highlights foo instead of yourself)\n" +
        "!timer hh:mm [message]\n" +
        "!timer dd/mm [message] \n" +
        "!timer dd/mm-hh:mm [message] \n" +
        "!timer day-hh:mm [message] \n" +
        "Example: !timer 14m grandis\n";
        handler.registerHelp("timer", helpText);
        bot = Grouphug.getInstance();

        if(SQL.isAvailable()) {
            // Load timers from database, if there were any there when the bot shut down
            List<Sleeper> sleepers = JWorm.get(Sleeper.class);
            for(Sleeper s : sleepers) {
                if(s.getTime() <= System.currentTimeMillis()) {
                    // The timer expired when the bot was down
                    if("".equals(s.getMessage())) {
                        bot.msg(s.getChannel(), s.getNick() + ": Time ran out while I was shut down. " +
                        "I was supposed to nofity you at " + new DateTime(s.getTime()));
                    } else {
                        bot.msg(s.getChannel(), s.getNick() + ": Time ran out while I was shut down. " +
                        "I was supposed to tell you: " + s.getMessage() + " at " +
                        new DateTime(s.getTime()));
                    }
                    s.delete();
                } else {
                    new Thread(s).start();
                }
            }
        } else {
            System.err.println("Warning: Timer will start, but not be able to load existing timers, " +
              "or store new timers, which will be lost upon reboot because SQL is unavailable.");
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String receiver = sender;
        Matcher m = Pattern.compile("(?i)^(-for (\\S+)).+").matcher(message);
        if(m.matches()) {
            receiver = m.group(2);
            message = message.replaceFirst("(?i)-for \\S+\\s+", "");
        }

        String timerTime = message.split(" ")[0]; //getting the 14d, 23:59 part from the message

        if (timerTime.matches(".*[a-zA-Z]")){ //Has a letter in it, aka the 14d kind of timer
            countdownTimer(channel, sender, receiver, message);
            return;
        }

        //Change this checking to regex checking which type it is perhaps, this is ugly :3

        SimpleDateFormat hour_min_format = new SimpleDateFormat("HH:mm"); // 21:45
        SimpleDateFormat date_hour_min_format = new SimpleDateFormat("dd/MM-HH:mm"); //24/12-20:34
        SimpleDateFormat date_format = new SimpleDateFormat("dd/MM"); // 2/4, 23/12
        SimpleDateFormat day_format = new SimpleDateFormat("E-HH:mm"); // Monday-23:14

        DateTime parseHighlight = null;
        DateTime timeToHighlight = null;
        DateTime now = new DateTime();
        try {
            parseHighlight = new DateTime(hour_min_format.parse(timerTime));
            timeToHighlight = new DateTime(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth(),
                    parseHighlight.getHourOfDay(), parseHighlight.getMinuteOfHour(), 0, 0);

            if (timeToHighlight.isBefore(now)){ //Aka a timestamp that already has been today
                timeToHighlight = timeToHighlight.plusDays(1); //So that means we mean tomorrow
            }

        } catch (ParseException e) {
            //Not of this type, try another one
            try {
                parseHighlight = new DateTime(date_hour_min_format.parse(timerTime));
                timeToHighlight = new DateTime(now.getYear(), parseHighlight.getMonthOfYear(), parseHighlight.getDayOfMonth(),
                        parseHighlight.getHourOfDay(), parseHighlight.getMinuteOfHour(), 0, 0);

                if (timeToHighlight.isBefore(now)){ //Aka a date and time that has been this year
                    timeToHighlight = timeToHighlight.plusYears(1);
                }

            } catch (ParseException e2) {
                //Not this one either, try the last one
                try {
                    parseHighlight = new DateTime(date_format.parse(timerTime));
                    timeToHighlight = new DateTime(now.getYear(), parseHighlight.getMonthOfYear(), parseHighlight.getDayOfMonth(),
                            0, 0, 0, 0);

                    if (timeToHighlight.isBefore(now)){ //A date that has already been this year
                        timeToHighlight = timeToHighlight.plusYears(1); //So that means we mean next year
                    }
                } catch (ParseException e3) {
                    try{
                        parseHighlight = new DateTime(day_format.parse(timerTime));

                        timeToHighlight = now.withDayOfWeek(parseHighlight.getDayOfWeek())
                        .withHourOfDay(parseHighlight.getHourOfDay())
                        .withMinuteOfHour(parseHighlight.getMinuteOfHour());

                        if (timeToHighlight.isBefore(now)) { //That day has already been  this week
                            timeToHighlight = timeToHighlight.plusWeeks(1);
                        }
                    }catch (ParseException e4){
                        //Not this one either, this is just bogus
                        bot.msg(channel, "Stop trying to trick me, this isn't a valid timer-argument. Try !help timer");
                        return;
                    }
                }
            }
        }

        //We now have the time
        String notifyMessage = message.substring(timerTime.length()).trim();
        Sleeper s = new Sleeper(receiver, timeToHighlight.getMillis(), notifyMessage, channel);
        s.insert();
        new Thread(s).start();
        String who = receiver.equals(sender) ? "you" : receiver;
        bot.msg(channel, String.format("Ok, I will highlight %s at %s.", who, f.print(timeToHighlight)));
    }

    /**
     * Normal countdown, where days, hours, minutes and seconds are specified
     * @param channel
     * @param sender
     * @param message
     */
    private void countdownTimer(String channel, String sender, String receiver, String message) {
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
                bot.msg(channel, "'" + message + "' doesn't start with a valid number, does it now? Try '!help timer'.");
                return;
            }
            // indexAfterCount is now the index after the count
        }
        int factor;
        String notifyMessage;
        int count = Integer.parseInt(message.substring(0, indexAfterCount));
        // if there are no chars after the count
        if(indexAfterCount == message.length()) {
            factor = 60;
            notifyMessage = "";
        } else {
            switch(message.charAt(indexAfterCount)) {
                case 's':
                    factor = 1;
                    break;

                case 'm':
                    factor = 60;
                    break;

                case 'h':
                case 't':
                    factor = 3600;

                    break;

                case 'd':
                    factor = 86400;
                    break;

                default:
                    bot.msg(channel, "No. Try '!help timer'.");
                    return;

            }
            notifyMessage = message.substring(indexAfterCount + 1).trim();
        }

        long time = System.currentTimeMillis() + (count * factor * 1000);
        Sleeper s = new Sleeper(receiver, time, notifyMessage, channel);
        s.insert();
        new Thread(s).start();
        String who = receiver.equals(sender) ? "you" : receiver;
        bot.msg(channel, String.format("Ok, I will highlight %s at %s.", who, f.print(new DateTime(time))));
    }

    public static class Sleeper extends Worm implements Runnable {
        private String nick;
        private long time; // time of highlight, in "java unix time" (with ms granularity)
        private String message;
        private String channel;

        public Sleeper(String nick, long time, String message, String channel) {
            this.nick = nick;
            this.time = time;
            this.message = message;
            this.channel = channel;
        }

        public String getNick() {
            return this.nick;
        }

        public long getTime() {
            return this.time;
        }

        public String getMessage() {
            return this.message;
        }

        public String getChannel() {
            return this.channel;
        }

        public void run() {
            long sleepAmount = time - System.currentTimeMillis();
            if(sleepAmount <= 0) {
                Grouphug.getInstance().msg(channel, "I can't notify you about something in the " +
                    "past until someone implements a time machine in me.");
                this.delete();
                return;
            }
            try {
                Thread.sleep(sleepAmount);
            } catch(InterruptedException e) {
                // We won't be interrupted AFTER the thread is done sleeping, so restart it.
                // It will just start sleeping again with the correct time
                new Thread(this).start();
                return;
            }
            if("".equals(message)) {
                Grouphug.getInstance().msg(channel, nick + ": Time's up!");
            } else {
                Grouphug.getInstance().msg(channel, nick + ": " + message);
            }
            this.delete();
        }
    }
}
