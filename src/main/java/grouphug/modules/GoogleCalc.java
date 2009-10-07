package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;

public class GoogleCalc implements GrouphugModule {
    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith("gc")) {
            return;
        }

        message = message.substring("gc ".length());

        try {
            ArrayList<String> lines = Web.fetchHtmlList("http://www.google.no/search?q=" + message.replace(" ", "+"));
            String reply = null;
            for(String line : lines) {
                if(line.contains("<h2 class=r style=\"font-size:138%\"><b>")) {
                    int startIndex = line.indexOf("<h2 class=r style=\"font-size:138%\"><b>");
                    reply = line.substring(startIndex + "<h2 class=r style=\"font-size:138%\"><b>".length(),
                            line.indexOf("</b>", startIndex));
                }
            }
            if(reply == null) {
                Grouphug.getInstance().sendMessage("The google calculator had nothing to say about that.", false);
            } else {
                Grouphug.getInstance().sendMessage(reply, true);
            }
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("The intertubes seem to be clogged up (I got an IOException)", false);
            ex.printStackTrace();
        }
    }


    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return "googlecalc";
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals("googlecalc")) {
            return "Use the google calculator to calculate something. Usage:\n" +
                    "!gc 25 celsius in fahrenheit\n" +
                    "!gc 4lbs 14oz in kg\n" +
                    "!gc 100 usd in nok";
        }
        return null;
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {

    }
}
