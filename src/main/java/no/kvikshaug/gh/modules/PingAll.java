package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;

import java.util.ArrayList;
import java.util.Random;

import org.jibble.pircbot.User;

public class PingAll implements TriggerListener {

    private static final String TRIGGER = "pingall";
    private static final String TRIGGER_HELP = "pingall";

    public PingAll(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Pingall: Highlights everyone in the channel.\n" +
                    Grouphug.MAIN_TRIGGER+TRIGGER+
                    Grouphug.MAIN_TRIGGER+TRIGGER+" <message>");
        System.out.println("Pingall module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        User[] users = Grouphug.getInstance().getUsers(channel);
        
        String output = "";
        
        for (int i = 0; i < users.length; i++) {
			output += users[i].getNick()+", ";
		}
        //Nick1, Nick2, Nick3, Nick4, 
        
        output = output.substring(0, output.length()-2); //Remove space and ,
        output += ": ";
        output += "".equals(message) ? sender + " is trying to reach you!" : sender + " is telling you: "+message;

        Grouphug.getInstance().msg(channel, output);
    }
}