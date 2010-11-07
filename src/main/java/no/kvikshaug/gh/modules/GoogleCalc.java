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
import java.util.List;

public class GoogleCalc implements TriggerListener {

    private Grouphug bot;

    public GoogleCalc(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        moduleHandler.addTriggerListener("gc", this);
        moduleHandler.registerHelp("googlecalc", "Use the google calculator to calculate something. Usage:\n" +
                    "!gc 25 celsius in fahrenheit\n" +
                    "!gc 4lbs 14oz in kg\n" +
                    "!gc 100 usd in nok");
        System.out.println("Google calculator module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            Document doc = Web.getJDOMDocument(new URL("http://www.google.no/search?q=" + message.replace(" ", "+")));
            XPath calcPath = XPath.newInstance("//h:h2[@class='r']/h:b");
            calcPath.addNamespace("h","http://www.w3.org/1999/xhtml");
            Element calcElement = (Element)calcPath.selectSingleNode(doc);
            if(calcElement == null) {
                bot.sendMessageChannel(channel, "The google calculator had nothing to say about that.");
            } else {
                bot.sendMessageChannel(channel, calcElement.getValue(), true);
            }
        } catch(IOException e) {
            bot.sendMessageChannel(channel, "Google showed me the finger and mumbled something about 'go throw an IOException' :(");
            e.printStackTrace();
        } catch (JDOMException e) {
            bot.sendMessageChannel(channel, "JDOM threw up in my face, what the hell.");
            e.printStackTrace();
        }
    }
}
