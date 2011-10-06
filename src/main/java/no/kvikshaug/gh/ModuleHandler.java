package no.kvikshaug.gh;

import no.kvikshaug.gh.listeners.JoinListener;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.listeners.NickChangeListener;
import no.kvikshaug.gh.modules.*;

import no.kvikshaug.scatsd.client.ScatsD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * The modulehandler handles modules by letting them register for trigger or message events
 * and register help responses. All modules must be added in this class's constructor.
 * If the modules want to have methods called upon triggers or all messages, they must
 * implement the corresponding module and call the corresponding addListener method.
 *
 * The functions of this class COULD reside in Grouphug.java, but they
 * are seperated to make that file slimmer and make it easier to find what you want.
 */
public class ModuleHandler {

    private Grouphug bot;
    private ThreadGroup eventThreads;

    private HashMap<String, String> helpers = new HashMap<String, String>();
    private List<TriggerListener> triggerListeners = new ArrayList<TriggerListener>();
    private List<MessageListener> messageListeners = new ArrayList<MessageListener>();
    private List<JoinListener> joinListeners = new ArrayList<JoinListener>();
    private List<NickChangeListener> nickChangeListeners = new ArrayList<NickChangeListener>();

    public ModuleHandler(Grouphug bot) {
        this.bot = bot;
        this.eventThreads = new ThreadGroup("ModuleHandler event threads");

        // add all the modules here
        System.out.println("\nInitializing modules...");

        new Bofh(this);
        new Confession(this);
        new Decider(this);
        new Define(this);
        new EightBall(this);
        new Factoid(this);
        new Google(this);
        new GoogleCalc(this);
        new GoogleFight(this);
        new IMDb(this);
        new Insulter(this);
        new IsUp(this);
        new Karma(this);
        new Seen(this);
        new Slang(this);
        new Tracking(this);
        new Upload(this);
        new URLCatcher(this);
        new WordCount(this);
        new Operator(this);
        new Timer(this);
        new SnowReport(this);
        new Fml(this);
        new Pre(this);
        new Jargon(this);
        new Commit(this);
        new EpisodeInfo(this);
        new Translate(this);
        new Weather(this);
        new Tell(this);
        new Quote(this);
        new Mlia(this);
        new Bash(this);
        new Vote(this);
        new Tlf(this);
        new Repl(this);
        new PingAll(this);
        new Eyebleach(this);
        new Remember(this);
        new JoinMessage(this);

        System.out.println();
        System.out.println(helpers.size() + " help responses registered");
        System.out.println(triggerListeners.size() + " message triggers registered");
        System.out.println(messageListeners.size() + " modules are listening for any message");
        System.out.println(joinListeners.size() + " modules are listening for channel joins");
        System.out.println(nickChangeListeners.size() + " modules are listening for nick changes");
    }

    /**
     * Register a trigger that will show a specific help text when the help trigger is called.
     * @param trigger The text that will trigger this help text
     * @param text The full help text
     */
    public void registerHelp(String trigger, String text) {
        helpers.put(trigger, text);
    }

    /**
     * Modules that have a trigger word which makes them react should implement the
     * TriggerListener interface and then call this method with a reference to themselves.
     * When someone triggers them, their onTrigger method will be called with the details of the
     * message (omitting the trigger word itself).
     * @param trigger the trigger that the listener will be called upon
     * @param listener the listener to call
     */
    public void addTriggerListener(String trigger, no.kvikshaug.gh.listeners.TriggerListener listener) {
        triggerListeners.add(new TriggerListener(trigger, listener));
    }

    /**
     * Modules that want to react to absolutely all messages sent to the channel should
     * implement the MessageListener interface and then call this method with a reference to
     * themselves. All messages will be forwarded to their onMessage method.
     * @param listener the listener to call
     */
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }

    /**
     * Modules that want to react to someone joining our channel should implement the
     * JoinListener interface and then call this method with a reference to themselves.
     * The onJoin method in that interface will be called upon any join.
     * @param listener the listener to call
     */
    public void addJoinListener(JoinListener listener) {
        joinListeners.add(listener);
    }

    /**
     * Modules that want to react to someone changing their nick should implement the
     * NickChangeListener interface and then call this method with a reference to themselves.
     * The onNickChange method in that interface will be called upon any join.
     * @param listener the listener to call
     */
    public void addNickChangeListener(NickChangeListener listener) {
        nickChangeListeners.add(listener);
    }

    /**
     * If a modules trigger string is triggered when a user writes something to the channel,
     * this method will send the details of the message to that module.
     * @param channel channel of the event
     * @param sender users nick
     * @param login users login
     * @param hostname users hostname
     * @param message the message sent from the user, excluding the trigger character, trigger string and
     * any leading or trailing whitespace. So a '!trigger hi all'-message will be sent to the module as
     * 'hi all'.
     */
    public void onTrigger(final String channel, final String sender, final String login, final String hostname, final String message) {
        for(final TriggerListener listener : triggerListeners) {
            if(listener.trigger(message)) {
                ScatsD.count("gh.bot."+channel+".triggers", 1);
                Thread listenerThread = new Thread(eventThreads, new Runnable() {
                    public void run() {
                        // we trim the trigger and any following whitespace from the message
                        listener.getListener().onTrigger(channel, sender, login, hostname,
                                message.substring(listener.getTrigger().length()).trim(), listener.getTrigger());
                    }
                });
                listenerThread.start();
            }
        }
    }

    /**
     * All messages sent to the server will be sent here.
     * @param channel channel of the event
     * @param sender users nick
     * @param login users login
     * @param hostname users hostname
     * @param message the users complete message
     */
    public void onMessage(final String channel, final String sender, final String login, final String hostname, final String message) {
        ScatsD.count("gh.bot."+channel+".messages", 1, 60);
        for(final MessageListener listener : messageListeners) {
            Thread listenerThread = new Thread(eventThreads, new Runnable(){
                    public void run() {
                        listener.onMessage(channel, sender, login, hostname, message);
                    }
                });
            listenerThread.start();

        }
    }

    /**
     * When someone has triggered help, this method will be called
     * @param sender where the message came from, and where it should be sent back to
     * @param trigger the trigger word/module the user wants help for, empty if none
     */
    public void onHelp(String sender, String trigger) {
        ScatsD.count("gh.bot.helps", 1);
        if(trigger.equals("")) {
            // no specific help text was requested
            bot.msg(sender, "Try \"!help <module>\" for one of the following modules:", false);
            String helpString = "";
            Collection<String> helperText = helpers.keySet();
            for(String texts : helperText) {
                helpString += texts + ", ";
            }
            bot.msg(sender, helpString.substring(0, helpString.length()-2), false);
        } else {
            // looking for a specific module
            String text = helpers.get(trigger);
            if(text == null) {
                bot.msg(sender, "No one has implemented a "+trigger+" module yet. Patches are welcome!", false);
            } else {
                bot.msg(sender, text, false);
            }
        }
    }

    /**
     * Every time someone joins our channel, registered listeners will be notified via this method
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    public void onJoin(final String channel, final String sender, final String login, final String hostname) {
        ScatsD.count("gh.bot."+channel+".joins", 1);
        for(final JoinListener listener : joinListeners) {
            Thread listenerThread = new Thread(eventThreads, new Runnable() {
                    public void run() {
                        listener.onJoin(channel, sender, login, hostname);
                    }
                });
            listenerThread.start();
        }
    }

    /**
     * Every time someone changes their nick, registered listeners will be notified via this method
     * @param oldNick The old nick
     * @param login The login of the user
     * @param hostname The hostname of the user
     * @param newNick The new nick
     */
    public void onNickChange(final String oldNick, final String login, final String hostname, final String newNick) {
        ScatsD.count("gh.bot.nickchanges", 1);
        for(final NickChangeListener listener : nickChangeListeners) {
            Thread listenerThread = new Thread(eventThreads, new Runnable() {
                    public void run() {
                        listener.onNickChange(oldNick, login, hostname, newNick);
                    }
                });
            listenerThread.start();
        }
    }

    /**
     * This class contains all the modules listening for a trigger string, and that specific string.
     */
    private class TriggerListener {
        private String trigger;
        private no.kvikshaug.gh.listeners.TriggerListener listener;

        private no.kvikshaug.gh.listeners.TriggerListener getListener() {
            return listener;
        }

        private String getTrigger() {
            return trigger;
        }

        private TriggerListener(String trigger, no.kvikshaug.gh.listeners.TriggerListener listener) {
            this.trigger = trigger;
            this.listener = listener;
        }

        /**
         * Checks if a message would trigger this listener. This will be the case if it
         * starts with the trigger word and the following character is a space (' ').
         * @param message the message to check
         * @return true if the message triggers this listener, false if not
         */
        private boolean trigger(String message) {
            return message.startsWith(trigger) && (message.length() == trigger.length() || message.charAt(trigger.length()) == ' ');
        }
    }
}
