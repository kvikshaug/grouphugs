package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.MessageListener;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;

/**
 * URLCatcher module
 * <p/>
 * This module catches http:// or https:// URLs in messages to the cahnnel, tries to look up the URL, then parses
 * whatever it finds at  the URL, looking for <title>somethingsomething</title>, and sends somethingsomething back
 * to the channel.
 */
public class URLCatcher implements MessageListener {

    private static final String[] URI_SCHEMES = new String[]{"http://", "https://"};
    private static final String HELP_TRIGGER = "urlcatcher";
    private static final int TITLE_MAX_LENGTH = 100;

    public URLCatcher(ModuleHandler moduleHandler) {
        moduleHandler.addMessageListener(this);
        moduleHandler.registerHelp(HELP_TRIGGER, "URLCatcher tries to catch http:// or https:// URLs in messages to the channel, tries " +
                "to look up the URL, then parses whatever it finds at the URL, looking for " +
                "a html <title>, and sends the title back to the channel.");
        System.out.println("URLCatcher module loaded.");
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        try {
            for(String url : findAllUrls(message)) {
                String title = Web.fetchTitle(url);
                if(!title.equals("")) {
                    if(title.length() > TITLE_MAX_LENGTH) {
                        title = title.substring(0, TITLE_MAX_LENGTH - 6); // minus the 6 ' (...)'-chars
                        title = title.concat(" (...)");
                    }
                    Grouphug.getInstance().sendMessage("Title: " + Web.entitiesToChars(title.trim()));
                }
            }
        } catch(IllegalArgumentException ex) {
            System.err.println("URLCatcher was unable to fetch title: " + ex.getMessage());
        } catch(IOException ex) {
            System.err.println("URLCatcher was unable to fetch title, ignoring this URL.");
            ex.printStackTrace();
            // Maybe it's not a good idea to talk when an error occurs; people may very well paste
            // invalid lines and we wouldn't want gh to complain every time that happens now would we?
            //Grouphug.getInstance().sendMessage("Sorry, couldn't fetch the title for you, I caught an IOException.");
        }
    }


    /**
     * Find all urls matching a URI scheme in URI_SCHEMES in string.
     *
     * @param string the strings to look for urls in.
     * @return any urls found, in an arraylist.
     */
    private ArrayList<String> findAllUrls(String string) {
        ArrayList<String> urls = new ArrayList<String>();

        for (String s : URI_SCHEMES) {
            urls.addAll(Web.findURIs(s, string));
        }

        return urls;
    }
}
