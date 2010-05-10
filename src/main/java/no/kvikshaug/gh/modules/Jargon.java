package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class Jargon implements TriggerListener {

    private Grouphug bot;

    public Jargon(ModuleHandler handler) {
        bot = Grouphug.getInstance();
        handler.addTriggerListener("jargon", this);
        handler.registerHelp("jargon", "Search the jargon files for an expression." +
                "!jargon <phrase>");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(message.equals("")) {
            bot.sendMessage("Try !help jargon. It's not that hard.");
        }

        String folder = message.substring(0, 1).toUpperCase();
        String file = message.replace(" ", "-").replace(".", "-").trim();

        try {
            Document document = Web.getJDOMDocument(new URL("http://catb.org/jargon/html/" + folder + "/" + file + ".html"));
            XPath xpathDt = XPath.newInstance("//h:dt");
            xpathDt.addNamespace("h","http://www.w3.org/1999/xhtml");
            String definitionTerm = null;
            // this is so ugly. i am lazy. hugzzzz
            for(Object o : xpathDt.selectNodes(document)) {
                Element e = (Element)o;
                definitionTerm = e.getAttributeValue("id");
                if(definitionTerm != null) {
                    for(Object c : e.getChildren()) {
                        if(((Element)c).getName().equals("span")) {
                            definitionTerm += " (" + ((Element)c).getText() + ")";
                            break;
                        }
                    }
                    break;
                }
            }
            if(definitionTerm == null) {
                throw new NullPointerException("The definition wasn't there!");
            }

            XPath xpathDd = XPath.newInstance("//h:dd/h:p");
            xpathDd.addNamespace("h","http://www.w3.org/1999/xhtml");
            String definition = "";

            for(Object o : xpathDd.selectNodes(document)) {
                Element e = (Element)o;
                definition += "\n" + e.getValue().replace("\n", " ").replaceAll("\\s+", " ").trim();
            }
            definition = definition.trim();
            bot.sendMessage(definitionTerm + ": " + definition, true);

        } catch (NullPointerException e) {
            bot.sendMessage("Looks like someone made a woopise with xpath and the DOM; there was an NPE somewhere.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            bot.sendMessage("Couldn't find that in the jargon files. Try !slang.");
        } catch (IOException e) {
            bot.sendMessage("Whoa whoa, an IOException caught me by surprise.");
            e.printStackTrace();
        } catch (JDOMException e) {
            bot.sendMessage("Lookie here, a JDOMExcpetion.");
            e.printStackTrace();
        }
    }

}
