package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class GoogleFight implements TriggerListener {

    private static final String TRIGGER = "gf";
    private static final String TRIGGER_HELP = "googlefight";
    private static final String TRIGGER_VS = " <vs> ";

    public GoogleFight(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Google fight:\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER + " [1st search]" + TRIGGER_VS + "[2nd search]");
        System.out.println("Googlefight module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(!message.contains(TRIGGER_VS)) {
            Grouphug.getInstance().sendMessage(sender+", try "+Grouphug.MAIN_TRIGGER+Grouphug.HELP_TRIGGER+" "+TRIGGER_HELP);
            return;
        }

        String query1 = message.substring(0, message.indexOf(TRIGGER_VS));
        String query2 = message.substring(message.indexOf(TRIGGER_VS) + TRIGGER_VS.length());

        try {
            String hits1 = search(query1);
            String hits2 = search(query2);

            if(hits1 == null) {
                hits1 = "no";
            }
            if(hits2 == null) {
                hits2 = "no";
            }

            Grouphug.getInstance().sendMessage(query1+": "+hits1+" results\n"+query2+": "+hits2+" results");
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)");
            System.err.println(e.getMessage()+"\n"+e.getCause());
            e.printStackTrace(System.err);
        }
    }

    public String search(String query) throws IOException {
        BufferedReader google = Web.prepareEncodedBufferedReader(new URL("http://www.google.com/search?q="+query.replace(' ', '+')));

        String line;
        String search = "</b> of about <b>";
        while((line = google.readLine()) != null) {
            if(line.contains(search)) {
                IOUtils.closeQuietly(google);
                return line.substring(line.indexOf(search) + search.length(), line.indexOf("</b>", line.indexOf(search) + search.length()));
            }
        }
        return null;
    }
}
