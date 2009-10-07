package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Uses google translate to translate a text
 */
public class Translate implements GrouphugModule {

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith("translate")) {
            return;
        }

        message = message.substring("translate ".length());

        String fromLanguage = "no";
        String toLanguage = "en";

        if(message.indexOf("f=") != -1) {
            fromLanguage = message.substring(message.indexOf("f=") + 2, message.indexOf("f=") + 4);
            String part1 = message.substring(0, message.indexOf("f="));
            String part2 = message.substring(message.indexOf("f=") + 4);
            message = part1 + part2;
        }

        if(message.indexOf("t=") != -1) {
            toLanguage = message.substring(message.indexOf("t=") + 2, message.indexOf("t=") + 4);
            String part1 = message.substring(0, message.indexOf("t="));
            String part2 = message.substring(message.indexOf("t=") + 4);
            message = part1 + part2;
        }

        message = message.trim();

        try {
            ArrayList<String> lines = Web.fetchHtmlList("http://translate.google.com/translate_t?hl=no&text=" + message +
                    "&file=&sl=" + fromLanguage + "&tl=" + toLanguage + "&history_state0=#");
            for(String line : lines) {
                if(line.contains("<div id=result_box dir=\"ltr\">")) {
                    int index = line.indexOf("<div id=result_box dir=\"ltr\">") + "<div id=result_box dir=\"ltr\">".length();
                    int nextIndex = line.indexOf("</div>", index);
                    Grouphug.getInstance().sendMessage(line.substring(index, nextIndex), true);
                    return;
                }
            }
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intertubes seem to be clogged up (IOException)", false);
            ex.printStackTrace();
        }

    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {

    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return "translate";
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        // lazy, sorry
        if(message.equals("translate")) {
            return "Uses Google Translate to translate a string. Usage:\n" +
                    "!translate <string>\n" +
                    "!translate f=<from_lang> t=<to_lang> <string>\n" +
                    "Norwegian to english is default. Languages has to be specified by their 2-letter code (en, no, etc.); check translate.google.com for these.";
        }
        return null;
    }
}
