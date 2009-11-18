package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;

public class Insulter implements TriggerListener {

    private static final String TRIGGER = "insult";
    private static final String TRIGGER_HELP = "insult";

    public Insulter(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Insult someone you don't like:\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <person>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER);
        System.out.println("Insult module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String insulted = null;
        if(!message.equals("")) {
            insulted = message;
        }
        String searchQ = "<td bordercolor=\"#FFFFFF\"><font face=\"Verdana\" size=\"4\"><strong><i>";

        try {
            ArrayList<String> lines = Web.fetchHtmlLines("http://www.randominsults.net/");
            for(String line : lines) {
                int insultStart = line.indexOf(searchQ);
                if(insultStart == -1) {
                    continue;
                }
                int insultEnd = line.indexOf('<', insultStart + searchQ.length());
                if(insultEnd == -1) {
                    continue;
                }
                String insult = line.substring(insultStart + searchQ.length(), insultEnd);

                if(insulted != null) {
                    Grouphug.getInstance().sendMessage(insulted+": "+insult);
                } else {
                    Grouphug.getInstance().sendMessage(insult);
                }
                return;
            }
            if(insulted != null) {
                Grouphug.getInstance().sendMessage("Sorry, I was unable to parse randominsults.net because I was too " +
                    "busy throwing up by " + insulted + "'s ghastly presence.");
            } else {
                Grouphug.getInstance().sendMessage("Sorry, I was unable to parse randominsults.net because I was too " +
                        "busy throwing up by your ghastly presence.");
            }
        } catch(IOException ex) {
            if(insulted != null) {
                Grouphug.getInstance().sendMessage("Sorry, " + insulted + "'s ghastly presence made me throw up an IOException.");
            } else {
                Grouphug.getInstance().sendMessage("Sorry, your ghastly presence made me throw up an IOException.");
            }
            ex.printStackTrace();
        }
    }
}
