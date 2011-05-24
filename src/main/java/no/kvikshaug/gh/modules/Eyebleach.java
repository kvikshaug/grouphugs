package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;

public class Eyebleach implements TriggerListener {

    private static final String TRIGGER = "eyebleach";
    private static final String TRIGGER_HELP = "eyebleach";

    public Eyebleach(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Eyebleach: administers a dose of eyebleach.\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+ "\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+" <message>");
        System.out.println("Eyebleach module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String output = "http://eyebleach.com/eyebleach/eyebleach_";
        output += String.format("%03d", (Math.random() * 98.0 + 1.0));
        output += ".jpg";

        Grouphug.getInstance().msg(channel, output);
    }
}
