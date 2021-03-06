package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;

import java.util.Random;

public class Decider implements TriggerListener {

    private static final String TRIGGER = "decide";
    private static final String TRIGGER_HELP = "decide";
    private Random random = new Random(System.currentTimeMillis());

    public Decider(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Decider: Helps you make tough decisions.\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+" <choice 1> | <choice 2> | <choice n...>\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+" homework | wow | keep ircing");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String[] choices = message.split("\\|");
        Grouphug.getInstance().msg(channel, "The roll of the dice picks: "+choices[random.nextInt(choices.length)].trim());
    }
}
