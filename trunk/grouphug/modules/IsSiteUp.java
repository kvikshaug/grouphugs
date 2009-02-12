package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;
import grouphug.util.Web;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * isup - parses http://downforeveryoneorjustme.com/some.url.tld to check if a website is up or down.
 */
public class IsSiteUp implements GrouphugModule
{
    private static final String TRIGGER = "isup .+";
    private static final String HELP_TRIGGER = "isup";
    private static final String DFEOJM_URI = "http://downforeveryoneorjustme.com";
    private static final Pattern URI_START_REGEX = Pattern.compile("http(s)?://(www)?");

    private static final Pattern DFEOJM_MSG_START = Pattern.compile("<div id=\"container\">");
    private static final Pattern DFEOJM_MSG_END = Pattern.compile("(is up.)|(on the interwho.)");
    private static final String DFEOJM_MSG_HTML_REGEX = "(<a href=\".+\" class=\".+\">)|(</a></span>)";

    /**
     * This method is called by the bot when someone sends a chat line that starts with the trigger command.
     * It is then up to each module to parse the line and, if applicable, respond.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     */
    public void trigger(String channel, String sender, String login, String hostname, String message)
    {
        if (message.matches(TRIGGER))
        {
            // strip trigger
            String uri = message.split(TRIGGER)[2];

            String html = Web.fetchHTML(DFEOJM_URI + '/' + cleanURI(uri));
            if (null != html)
            {
                String result = parseHTML(html);
                Grouphug.getInstance().sendMessage("" + uri + " :: " + result, false);
            }
        }
    }

    /**
     * Removes http[s]://[www] from the start of a String.
     * @param uri the string we want to clean.
     * @return the cleaned string.
     */
    private String cleanURI(String uri)
    {
        Matcher uriStart = URI_START_REGEX.matcher(uri);

        if (uriStart.find())
        {
            uri = uri.substring(uriStart.end(), uri.length());
        }

        return uri;
    }

    /**
     * Parses the html retrieved from downforeveryoneorjustme.com, to fetch the message returned by the site.
     * @param html a String containing html.
     * @return the message returned by the site.
     */
    private String parseHTML(String html)
    {
        int msgStartIndex = 0;
        int msgEndIndex = 0;

        Matcher msgStart = DFEOJM_MSG_START.matcher(html);
        Matcher msgEnd = DFEOJM_MSG_END.matcher(html);

        if (msgStart.find())
        {
            msgStartIndex = msgStart.end();
        }

        if (msgEnd.find())
        {
            msgEndIndex = msgEnd.end();
        }

        String result = html.substring(msgStartIndex, msgEndIndex);

        // clean up the message string
        result = result.trim();                                     // trim surrounding whitespace
        result = result.replaceAll(" +", " ");                      // replace 1+n space chars with 1 space char
        result = result.replaceAll(DFEOJM_MSG_HTML_REGEX, "");      // remove html from string

        return result;
    }

    /**
     * This method is similar to the <code>trigger</code> method, but is called when the chat line contains
     * no specific trigger command. The module may still choose to parse this, e.g. the karma module fetching
     * up sentences ending with ++/--, but be careful as an important part of this bot is not to bother anyone
     * unless specifically requested.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     */
    public void specialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        // not used
    }

    /**
     * This is called when the user is believed to ask for general help about the bot.
     * All modules should return a small string stripped for whitespace, containing a lowercase-representation
     * of its name, that could be used with the special help trigger, to get more specific help of the module.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel.
     * @return the name of the current module that will trigger a help-message on the special help trigger
     */
    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message)
    {
        return HELP_TRIGGER;
    }

    /**
     * This is called when the user is believed to ask for specific help of a module.
     * The module should parse the message, and if it includes the trigger that would be sent back in the
     * helpMainTrigger method, then this module should reply, with notices in pm to the sender,
     * all info about how this module is used, under the presumtion that this is the only module replying.
     *
     * @param channel  - The channel to which the message was sent.
     * @param sender   - The nick of the person who sent the message.
     * @param login    - The login of the person who sent the message.
     * @param hostname - The hostname of the person who sent the message.
     * @param message  - The actual message sent to the channel, stripped for the triggers + 1 char for space
     * @return boolean - true if the module reacted to the message, false otherwise
     */
    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message)
    {
        if (message.equals(HELP_TRIGGER))
        {
            return "isup - Checks if a web site is down, or if it's just your connection that sucks somehow.\n" +
                   "Usage:\n" +
                   Grouphug.MAIN_TRIGGER + HELP_TRIGGER + " http://foo.tld\n" +
                   "Checks if http://foo.tld is up or not.\n" +
                   "NOTE: URIs must start with http:// or https://.";
        }
        return null;
    }
}
