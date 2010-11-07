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

public class Insulter implements TriggerListener {

    private static final String TRIGGER = "insult";
    private static final String TRIGGER_HELP = "insult";
    private static final String INSULT_URL = "http://www.randominsults.net/";

    private Grouphug bot;

    public Insulter(ModuleHandler moduleHandler) {
        bot = Grouphug.getInstance();
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Insult someone you don't like:\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <person>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER);
        System.out.println("Insult module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        String insulted = null;
        if(!message.equals("")) {
            insulted = message;
        }

        try {
            String insult = parseInsult();
            if(insulted != null) {
                bot.sendMessageChannel(channel, insulted + ": " + insult);
            } else {
                bot.sendMessageChannel(channel, insult);
            }
        } catch(IOException e) {
            if(insulted != null) {
                bot.sendMessageChannel(channel, "Sorry, " + insulted + "'s ghastly presence made me throw up an IOException.");
            } else {
                bot.sendMessageChannel(channel, "Sorry, your ghastly presence made me throw up an IOException.");
            }
            e.printStackTrace();
        } catch (JDOMException e) {
            if(insulted != null) {
                bot.sendMessageChannel(channel, "Sorry, " + insulted + "'s ghastly presence made me throw up a JDOMException.");
            } else {
                bot.sendMessageChannel(channel, "Sorry, your ghastly presence made me throw up a JDOMException.");
            }
            e.printStackTrace();
        }
    }

    private String parseInsult() throws IOException, JDOMException {
        Document doc = Web.getJDOMDocument(new URL(INSULT_URL));
        XPath insultPath = XPath.newInstance("//h:strong/h:i");
        insultPath.addNamespace("h","http://www.w3.org/1999/xhtml");
        Element insultElement = (Element)insultPath.selectSingleNode(doc);
        if(insultElement == null) {
            return "The insult generator is so appalled by your prescense that it didn't even supply an insult.";
        }
        return insultElement.getText();
    }
}
