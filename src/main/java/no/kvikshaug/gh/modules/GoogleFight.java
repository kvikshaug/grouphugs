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

public class GoogleFight implements TriggerListener {

    private static final String TRIGGER = "gf";
    private static final String TRIGGER_HELP = "googlefight";
    private static final String TRIGGER_VS = " <vs> ";

    public GoogleFight(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Google fight:\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER + " [1st search]" + TRIGGER_VS + "[2nd search]");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(!message.contains(TRIGGER_VS)) {
            Grouphug.getInstance().msg(channel, sender+", try "+Grouphug.MAIN_TRIGGER+Grouphug.HELP_TRIGGER+" "+TRIGGER_HELP);
            return;
        }

        String query1 = message.substring(0, message.indexOf(TRIGGER_VS));
        String query2 = message.substring(message.indexOf(TRIGGER_VS) + TRIGGER_VS.length());

        try {
            String hits1 = searchResults(query1);
            String hits2 = searchResults(query2);

            Grouphug.getInstance().msg(channel, query1+": "+hits1+"\n"+query2+": "+hits2);
        } catch(IOException e) {
            Grouphug.getInstance().msg(channel, "Sorry, the intartubes seems to be clogged up (IOException)");
            e.printStackTrace();
        } catch (JDOMException e) {
            Grouphug.getInstance().msg(channel, "Woopsie, I caught a JDOMException.");
            e.printStackTrace();
        }
    }

    public String searchResults(String query) throws IOException, JDOMException {
        Document document = Web.getJDOMDocument(new URL("http://www.google.com/search?q="+query.replace(' ', '+')));
        XPath xpath = XPath.newInstance("//h:div[@id='resultStats']");
        xpath.addNamespace("h","http://www.w3.org/1999/xhtml");

        Element element = (Element)xpath.selectSingleNode(document);

        if(element == null) {
            return "no";
        }

        String results = element.getText().replace("About ", "");
        return results.substring(0, results.indexOf("results")+"results".length()).trim();
    }
}
