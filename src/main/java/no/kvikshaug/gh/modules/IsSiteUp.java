package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.net.URL;

/**
 * isup - parses http://downforeveryoneorjustme.com/some.url.tld to check if a website is up or down.
 */
public class IsSiteUp implements TriggerListener {
    private static final String TRIGGER = "isup";
    private static final String TRIGGER_HELP = "isup";
    private static final String DFEOJM_URI = "http://downforeveryoneorjustme.com";

    private Grouphug bot;

    public IsSiteUp(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "isup - Checks if a web site is down, or if it's just your connection that sucks somehow.\n" +
                "Usage:\n" +
                Grouphug.MAIN_TRIGGER + TRIGGER_HELP + " http://foo.tld\n" +
                "Checks if http://foo.tld is up or not.");
        System.out.println("IsSiteUp module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            bot.sendMessage(parseHTML(new URL(DFEOJM_URI + '/' + message)));
        } catch(ParseException ex) {
            bot.sendMessage("Sorry, I was unable to parse downforeveryoneorjustme.com.");
            System.err.println(ex);
        } catch(MalformedURLException e) {
            bot.sendMessage("I think your URL contained characters I couldn't transform into a DFEOJM-URL.");
            e.printStackTrace();
        } catch (IOException e) {
            bot.sendMessage("Sorry, I caught an IOException in my throat.");
            e.printStackTrace();
        } catch (JDOMException e) {
            bot.sendMessage("Sorry, JDOM is being a bitch.");
            e.printStackTrace();
        }
    }

    /**
     * Parses the html retrieved from downforeveryoneorjustme.com, to fetch the message returned by the site.
     *
     * @param url the site url
     * @return the message returned by the site.
     * @throws java.text.ParseException if unable to parse
     * @throws java.io.IOException at inconvenient moments
     * @throws org.jdom.JDOMException when it suits you the least
     */
    private String parseHTML(URL url) throws ParseException, JDOMException, IOException {
        Document doc = Web.getJDOMDocument(url);

        XPath container = XPath.newInstance("//h:div[@id='container']");
        container.addNamespace("h","http://www.w3.org/1999/xhtml");
        //XPath href = XPath.newInstance("//h:div[@id='container']/h:a");
        //href.addNamespace("h","http://www.w3.org/1999/xhtml");

        Element containerElement = (Element)container.selectSingleNode(doc);
        //Element hrefElement = (Element)href.selectSingleNode(doc);
        if(containerElement == null) {
            throw new ParseException("Unable to parse site; couldn't find container element.", 0);
        }
        //if(hrefElement == null) {
        //    throw new ParseException("Unable to parse site; couldn't find a (as in hyperreference) element.", 0);
        //}

        // since simply printing out the text of the 'a' element where it occurs, without including
        // the 'p' and 'center' elements which come later in the containerElement proved to
        // be a PAIN IN THE ARSE, we just print out all text, assume that the following phrases are
        // included and remove them.
        String result = containerElement.getValue();
        result = result.replace("Check another site?", "");
        result = result.replace("Try again?", "");
        result = result.replace("We Guarantee Our Uptime! Switch to Site5 Web Hosting!", "");
        result = result.replace("Tired Of Downtime? Try Site5's Redundant Hosting!", "");
        result = result.replace("(Use the coupon \"notdownforme\" for 20% off for our visitors!)", "");

        // now clean it up and deliver
        result = result.replaceAll(" +", " ");
        result = result.replaceAll("\n", "");
        result = result.trim();
        return result;
    }
}
