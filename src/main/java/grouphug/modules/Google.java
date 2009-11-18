package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Perform a google search and display the results in the channel
 */
public class Google implements TriggerListener {

    private static final String TRIGGER = "google";
    private static final String TRIGGER_ALT = "g";
    private static final String TRIGGER_HELP = "google";

    public Google(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.addTriggerListener(TRIGGER_ALT, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Google search:\n" +
                "  !google <searchword(s)>\n" +
                "  !g <searchword(s)>\n" +
                "  !google -n <result count> <searchword(s)>\n" +
                "Result count if not specified is 1.");
        System.out.println("Google module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            int resultCount;
            ArrayList<URL> urls;
            String query;
            if(message.startsWith("-n")) {
                int firstSpaceIndex = message.indexOf(' ');
                int secondSpaceIndex = message.indexOf(' ', firstSpaceIndex+1);
                resultCount = Integer.parseInt(message.substring(firstSpaceIndex+1, secondSpaceIndex));
                query = message.substring(secondSpaceIndex + 1);
                urls = Web.googleSearch(query);
            } else {
                resultCount = 1;
                query = message;
                urls = Web.googleSearch(query);
            }

            if(urls.size() == 0) {
                Grouphug.getInstance().sendMessage("No results for " + query + ".");
            }

            for(int i=1; i<=resultCount; i++) {
                Grouphug.getInstance().sendMessage(urls.get(i-1).toString());
                if(urls.size() == i && resultCount > i) {
                    if(i == 1) {
                        Grouphug.getInstance().sendMessage("This was the only result.");
                    } else if(i == 10) {
                        Grouphug.getInstance().sendMessage("Google only provides 10 results per page.");
                    } else {
                        Grouphug.getInstance().sendMessage("There were only these " + i + " results.");
                    }
                    return;
                }
            }
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)");
            ex.printStackTrace();
        }
    }
}
