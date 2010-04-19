package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;
import org.jdom.JDOMException;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Confession implements TriggerListener {
    private static final String TRIGGER = "gh";
    private static final String KEYWORD_NEWEST = "-newest";

    public Confession(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp("confession", "Confession: Outputs a confession: random, newest or by search.\n" +
                "  "+Grouphug.MAIN_TRIGGER+TRIGGER+"\n" +
                "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" -newest\n" +
                "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <searchword(s)>");
        System.out.println("Confession module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        try {
            // Check if argument is provided
            ConfessionItem conf;
            if(message.contains(KEYWORD_NEWEST)) {
                conf = getConfession("http://confessions.grouphug.us/confessions/new");
            } else {
                // check for search-words
                if(!message.equals("")) {
                    try {
                        URL confessionURL = Web.googleSearch(message+"+site:grouphug.us/confessions/").get(0);
                        conf = getConfession(confessionURL.toString());
                    } catch(IndexOutOfBoundsException ex) {
                        Grouphug.getInstance().sendMessage("No one has confessed about their "+message+" problem yet.");
                        return;
                    } catch (JDOMException e) {
                        Grouphug.getInstance().sendMessage("Woops, i seem to have thrown a JDOMException.");
                        return;
                    }
                } else {
                    conf = getConfession("http://confessions.grouphug.us/random");
                }
            }

            Grouphug.getInstance().sendMessage(conf.toString(), true);
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intertubes seem to be clogged up, " +
                    "I catched an IOException.");
            ex.printStackTrace();
        }
    }

    /**
     * Gets a specified confession from grouphug.us
     * @param urlString The URL of the grouphug confession - should be one specific confession URL
     * @return the successfully fetched confession
     * @throws java.io.IOException sometimes
     */
    private ConfessionItem getConfession(String urlString) throws IOException {

        String confession = "";
        List<String> lines = Web.fetchHtmlLines(new URL(urlString));

        // we dig from the BOTTOM and up, searching for the first confession we find
        int line = 0;
        for(int i = lines.size() - 1; i >= 0; i--) {
            if(lines.get(i).startsWith("  <div class=\"content\">")) {
                line = i + 1;
                break;
            }
        }

        if(line == 0) {
            throw new IOException("Grouphug confession error: Couldn't find confession content!");
        }

        // now we save all data until we reach the end of the confession
        for(; !lines.get(line).equals("  </div>"); line++) {
            confession += lines.get(line).trim()+"\n";
        }

        confession = Web.entitiesToChars(confession);
        confession = confession.replaceAll("<.*?>",""); // strip html tags
        confession = confession.substring(0, confession.length()-1);

        return new ConfessionItem(confession);
    }

    /**
     * This object is a confession from grouphug.us
     * Contains a string of the confession. This "wrapper" class is kept because
     * we might want to store more info per confession in the future.
     */
    private static class ConfessionItem {

        private String confession;

        public String getConfession() {
            return confession;
        }

        public ConfessionItem(String confession) {
            this.confession = confession;
        }

        public String toString() {
            return confession;
        }
    }

}
