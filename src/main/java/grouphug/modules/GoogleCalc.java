package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.net.URL;

public class GoogleCalc implements TriggerListener {

    public GoogleCalc(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener("gc", this);
        moduleHandler.registerHelp("googlecalc", "Use the google calculator to calculate something. Usage:\n" +
                    "!gc 25 celsius in fahrenheit\n" +
                    "!gc 4lbs 14oz in kg\n" +
                    "!gc 100 usd in nok");
        System.out.println("Google calculator module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            ArrayList<String> lines = Web.fetchHtmlLines(new URL("http://www.google.no/search?q=" + message.replace(" ", "+")));
            String reply = null;
            for(String line : lines) {
                if(line.contains("<h2 class=r style=\"font-size:138%\"><b>")) {
                    int startIndex = line.indexOf("<h2 class=r style=\"font-size:138%\"><b>");
                    reply = line.substring(startIndex + "<h2 class=r style=\"font-size:138%\"><b>".length(),
                            line.indexOf("</b>", startIndex));
                }
            }
            if(reply == null) {
                Grouphug.getInstance().sendMessage("The google calculator had nothing to say about that.");
            } else {
                Grouphug.getInstance().sendMessage(reply.replaceAll("\\<.*?\\>",""), true);
            }
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("The intertubes seem to be clogged up (I got an IOException)");
            ex.printStackTrace();
        }
    }
}
