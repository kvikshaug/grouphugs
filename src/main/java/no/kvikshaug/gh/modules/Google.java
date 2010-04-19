package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.NoTitleException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;
import org.jdom.JDOMException;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Perform a google search and display the results in the channel
 */
public class Google implements TriggerListener {

    private static final String TRIGGER = "google";
    private static final String TRIGGER_ALT = "g";
    private static final String TRIGGER_HELP = "google";
    private static final int TITLE_MAX_LENGTH = 80;

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
            List<URL> urls;
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
                return;
            }

            for(int i=1; i<=resultCount; i++) {
                String title = null;
                try {
                    title = Web.fetchTitle(urls.get(i-1));
                    if(title.length() > TITLE_MAX_LENGTH) {
                        title = title.substring(0, TITLE_MAX_LENGTH - 6); // minus the 6 ' (...)'-chars
                        title = title.concat(" (...)");
                    }
                } catch(IllegalArgumentException e) {
                    System.err.println("Unable to fetch title due to IllegalArgumentException; " +
                            "probably image/audio/video");
                    e.printStackTrace();
                } catch(IOException e) {
                    System.err.println("Unable to fetch title due to JDOMException:");
                    e.printStackTrace();
                } catch(JDOMException e) {
                    System.err.println("Unable to fetch title due to JDOMException:");
                    e.printStackTrace();
                } catch(NoTitleException e) {
                    System.err.println("Unable to fetch title: " + e.getMessage());
                    e.printStackTrace();
                }
                String reply = urls.get(i-1).toString();
                if(title != null) {
                    reply += " - " + title;
                }
                Grouphug.getInstance().sendMessage(reply);
                if(urls.size() == i && resultCount > i) {
                    if(i == 1) {
                        Grouphug.getInstance().sendMessage("This was the only result.");
                    } else if(i >= 10) {
                        Grouphug.getInstance().sendMessage("Google only provided "+i+" results for this page.");
                    } else {
                        Grouphug.getInstance().sendMessage("There were only these " + i + " results.");
                    }
                    return;
                }
            }
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)");
            System.err.println(ex);
            ex.printStackTrace();
        }
    }
}
