package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.MessageListener;
import grouphug.util.Web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URLCatcher module
 * <p/>
 * This module catches http:// or https:// URLs in messages to the cahnnel, tries to look up the URL, then parses
 * whatever it finds at  the URL, looking for <title>somethingsomething</title>, and sends somethingsomething back
 * to the channel.
 */
public class URLCatcher implements MessageListener {

    private static final String[] URI_SCHEMES = new String[]{"http://", "https://"};
    private static final Pattern TITLE_BEGIN = Pattern.compile("<title.*?>|<TITLE.*?>");
    private static final Pattern TITLE_END = Pattern.compile("</title>|</TITLE>");
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
            ArrayList<String> urls = findAllUrls(message);
            for (String url : urls) {
                String title = getHTMLTitle(url);
                if(!title.equals("")) {
                    if (title.length() > TITLE_MAX_LENGTH) {
                        title = title.substring(0, TITLE_MAX_LENGTH - 6); // minus the 6 ' (...)'-chars
                        title = title.concat(" (...)");
                    }
                    Grouphug.getInstance().sendMessage("Title: " + Web.entitiesToChars(title.trim()), false);
                }
            }
        } catch(IOException ex) {
            System.err.println("URLCatcher was unable to fetch title, ignoring this URL.");
            System.err.println("Note: This error is pretty much harmless.");
            ex.printStackTrace();
            // Maybe it's not a good idea to talk when an error occurs; people may very well paste
            // invalid lines and we wouldn't want gh to complain every time that happens now would we?
            //Grouphug.getInstance().sendMessage("Sorry, couldn't fetch the title for you, I caught an IOException.", false);
        }
    }


    /**
     * Try to find the title of the html document that maybe is at url.
     *
     * @param url the url that maybe points to a html document.
     * @return the title of the html document (if it's there, obviously).
     * @throws java.io.IOException if we run in to trouble somewhere.
     */
    private String getHTMLTitle(String url) throws IOException {
        // the original code used one large string, so instead of modifying it too much we just make that string
        StringBuilder sb = new StringBuilder();
        for(String line : Web.fetchHtmlLines(url)) {
            sb.append(line);
        }
        String html = sb.toString();

        int titleBeginIndex, titleEndIndex;
        Matcher titleBegin = TITLE_BEGIN.matcher(html);
        Matcher titleEnd = TITLE_END.matcher(html);

        //  find the index at which <title> ends in html ( if it's there at all )
        if (titleBegin.find()) {
            titleBeginIndex = titleBegin.end();
        } else {
            throw new IOException("No start tag for title was found.");
        }

        // find the index at which </title> starts in html ( if it's there at all )
        if (titleEnd.find()) {
            titleEndIndex = titleEnd.start();
        } else {
            throw new IOException("No end tag for title was found.");
        }

        return html.substring(titleBeginIndex, titleEndIndex);
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
