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
import java.net.URL;

/**
 * A quickly written FML module
 */
public class Fml implements TriggerListener {

    private Grouphug bot;

    public Fml(ModuleHandler handler) {
        bot = Grouphug.getInstance();
        handler.registerHelp("fml", "Fuck my life\n!fml  -- show a fuck my life story from fmylife.com");
        handler.addTriggerListener("fml", this);
        System.out.println("FML module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            Document doc = Web.getJDOMDocument(new URL("http://www.fmylife.com/random"));
            XPath xpath = XPath.newInstance("//h:a[@class='fmllink']/parent::*");
            xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");
            Element element = (Element)xpath.selectSingleNode(doc);
            for(Object childElement : element.getChildren()) {
                bot.sendMessageChannel(channel, ((Element)childElement).getText());
            }
        } catch (IOException e) {
            bot.sendMessageChannel(channel, "Omg IOException.");
            e.printStackTrace();
        } catch (JDOMException e) {
            bot.sendMessageChannel(channel, "Omg omg JDOMException");
            e.printStackTrace();
        }
    }
}
