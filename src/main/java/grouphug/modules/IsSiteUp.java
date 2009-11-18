package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * isup - parses http://downforeveryoneorjustme.com/some.url.tld to check if a website is up or down.
 */
public class IsSiteUp implements TriggerListener {
    private static final String TRIGGER = "isup";
    private static final String TRIGGER_HELP = "isup";
    private static final String DFEOJM_URI = "http://downforeveryoneorjustme.com";
    private static final Pattern URI_START_REGEX = Pattern.compile("http(s)?://(www)?");

    private static final Pattern DFEOJM_MSG_START = Pattern.compile("<div id=\"container\">");
    private static final Pattern DFEOJM_MSG_END = Pattern.compile("(is up.)|(down from here.)");
    private static final String DFEOJM_MSG_HTML_REGEX = "(<a href=\".+\" class=\".+\">)|(</a></span>)";
    private static final String A_HTML_REGEX = "(</a>)";

    public IsSiteUp(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "isup - Checks if a web site is down, or if it's just your connection that sucks somehow.\n" +
                "Usage:\n" +
                Grouphug.MAIN_TRIGGER + TRIGGER_HELP + " http://foo.tld\n" +
                "Checks if http://foo.tld is up or not.\n" +
                "NOTE: URIs must start with http:// or https://.");
        System.out.println("IsSiteUp module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            String result = parseHTML(Web.fetchHtmlLine(DFEOJM_URI + '/' + cleanURI(message)));
            Grouphug.getInstance().sendMessage(message + " :: " + result);
        } catch(ParseException ex) {
            Grouphug.getInstance().sendMessage("Sorry, I was unable to parse downforeveryoneorjustme.com.");
        } catch (IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, I caught an IOException in my throat.");
        }
    }

    /**
     * Removes http[s]://[www] from the start of a String.
     *
     * @param uri the string we want to clean.
     * @return the cleaned string.
     */
    private String cleanURI(String uri) {
        Matcher uriStart = URI_START_REGEX.matcher(uri);

        if (uriStart.find()) {
            uri = uri.substring(uriStart.end(), uri.length());
        }

        return uri;
    }

    /**
     * Parses the html retrieved from downforeveryoneorjustme.com, to fetch the message returned by the site.
     *
     * @param html the site html
     * @return the message returned by the site.
     * @throws java.text.ParseException if unable to parse
     */
    private String parseHTML(String html) throws ParseException {
        int msgStartIndex;
        int msgEndIndex;

        Matcher msgStart = DFEOJM_MSG_START.matcher(html);
        Matcher msgEnd = DFEOJM_MSG_END.matcher(html);

        if (msgStart.find()) {
            msgStartIndex = msgStart.end();
        } else {
            throw new ParseException("Unable to parse site.", 0);
        }

        if (msgEnd.find()) {
            msgEndIndex = msgEnd.end();
        } else {
            throw new ParseException("Unable to parse site.", 0);
        }

        String result = html.substring(msgStartIndex, msgEndIndex);

        // clean up the message string
        result = result.trim();                                     // trim surrounding whitespace
        result = result.replaceAll(" +", " ");                      // replace 1+n space chars with 1 space char
        result = result.replaceAll(DFEOJM_MSG_HTML_REGEX, "");      // remove html from string
        result = result.replaceAll(A_HTML_REGEX, "");               // remove </a> tag from the middle of string

        return result;
    }
}
