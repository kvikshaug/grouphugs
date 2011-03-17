package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.NoTitleException;
import no.kvikshaug.gh.listeners.MessageListener;
import no.kvikshaug.gh.util.Web;
import org.jdom.JDOMException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * URLCatcher implemented with tagsoup and jchardet
 */
public class URLCatcher implements MessageListener, Runnable {
    private static final String[] URI_SCHEMES = new String[]{"http://", "https://"};
    private static final String HELP_TRIGGER = "urlcatcher";
    private static final int TITLE_MAX_LENGTH = 100;

    private String channel;
    private String message;

    public URLCatcher(ModuleHandler moduleHandler) {
        moduleHandler.addMessageListener(this);
        moduleHandler.registerHelp(HELP_TRIGGER, "URLCatcher tries to catch http:// or https:// URLs in messages to the channel, tries " +
                "to look up the URL, then parses whatever it finds at the URL, looking for " +
                "a html <title>, and sends the title back to the channel.");
        System.out.println("URLCatcher module loaded.");
    }

    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        this.channel = channel;
        this.message = message;
        new Thread(this).start();
    }

    public void run() {
        // get the public vars into this local thread immediately,
        // so they haven't changed by the time we're gonna use them.
        String channel = this.channel;
        String message = this.message;
        for(URL url : findAllUrls(message)) {
            String title = null;
            try {
                title = Web.fetchTitle(url);
            } catch(IllegalArgumentException e) {
                System.err.println("[URLCatcher]: IllegalArgumentException; probably image/audio/video link");
                e.printStackTrace();
            } catch (JDOMException e) {
                System.err.println("[URLCatcher]: unable to fetch title (JDOMException)");
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("[URLCatcher]: unable to fetch title (IOException)");
                e.printStackTrace();
            } catch(NoTitleException e) {
                System.err.println("[URLCatcher]: No title: " + e.getMessage());
            }

            if (title != null && title.length() > 0) {
                if(title.length() > TITLE_MAX_LENGTH) {
                    title = title.substring(0, TITLE_MAX_LENGTH - 6); // minus the 6 ' (...)'-chars
                    title = title.concat(" (...)");
                }

                Grouphug.getInstance().sendMessageChannel(channel, "Title: " + title);
            }
        }
    }

    /**
     * Find all urls matching a URI scheme in URI_SCHEMES in string.
     *
     * @param string the strings to look for urls in.
     * @return any urls found, in a List.
     */
    private List<URL> findAllUrls(String string) {
        List<URL> urls = new ArrayList<URL>();

        for (String scheme : URI_SCHEMES) {
            for (String url : Web.findURIs(scheme, string)) {
                try {
                    urls.add(new URL(url));
                } catch (MalformedURLException murle) {/* do not want */}
            }
        }

        return urls;
    }


}
