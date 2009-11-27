package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.MessageListener;
import grouphug.util.Web;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * URLCatcher implemented with tagsoup and jchardet
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

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        for(String url : findAllUrls(message)) {
            String title = null;
            try {
                title = getHTMLTitle(url);
            } catch (IOException e) {
                System.err.println("[URLCatcher] caught IOException while parsing HTML");
            } catch (JDOMException e) {
                System.err.println("[URLCatcher] caught JDOMException while parsing HTML");
            }

            if (title != null && title.length() > 0) {
                if(title.length() > TITLE_MAX_LENGTH) {
                    title = title.substring(0, TITLE_MAX_LENGTH - 6); // minus the 6 ' (...)'-chars
                    title = title.concat(" (...)");
                }
                
                Grouphug.getInstance().sendMessage("Title: " + title);
            }
        }
    }

    /**
     * Try to find the title of the html document that maybe is at url.
     *
     * @param url the url that maybe points to a html document.
     * @return the title of the html document (if it's there, obviously).
     * @throws java.io.IOException if we run in to trouble somewhere.
     * @throws org.jdom.JDOMException if parsing fails
     */
    private String getHTMLTitle(String url) throws IOException, JDOMException {
        URL realURL = new URL(url);

        SAXBuilder builder = new SAXBuilder("org.ccil.cowan.tagsoup.Parser"); // build a JDOM tree from the SAX stream provided by tagsoup
        Document doc = builder.build(realURL);

        XPath titlePath = XPath.newInstance("/h:html/h:head/h:title"); // find the <title> element using XPath
        titlePath.addNamespace("h","http://www.w3.org/1999/xhtml");
        return ((Element)titlePath.selectSingleNode(doc)).getText(); // <title>return this</title>
    }

    /**
     * Find all urls matching a URI scheme in URI_SCHEMES in string.
     *
     * @param string the strings to look for urls in.
     * @return any urls found, in a List.
     */
    private List<String> findAllUrls(String string) {
        ArrayList<String> urls = new ArrayList<String>();

        for (String s : URI_SCHEMES) {
            urls.addAll(Web.findURIs(s, string));
        }

        return urls;
    }
}
