package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;
import grouphug.util.Web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class Confession implements GrouphugModule {
    private static final String TRIGGER = "gh";
    private static final String TRIGGER_HELP = "confession";
    private static final String KEYWORD_NEWEST = "-newest";
    private static String errorConfession = "I have nothing to confess at the moment, please try again later.";



    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public String helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            return "Confession: Outputs a confession: random, newest or by search.\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER+"\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" -newest\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <searchword(s)>";
        }
        return null;
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(Confession.TRIGGER))
            return;

        try {

            // Check if argument is provided
            ConfessionItem conf;
            if(message.contains(KEYWORD_NEWEST)) {
                conf = newest();
            }
            else {
                // check for search-words
                if(!message.substring(TRIGGER.length()).trim().equals("")) {
                    conf = search(message.substring(TRIGGER.length()).trim());
                }
                else {
                    conf = random();
                }
            }

            if(conf == null)
                Grouphug.getInstance().sendMessage(errorConfession, false);
            else
                Grouphug.getInstance().sendMessage(conf.toString(), true);
        } catch(IOException ex) {
            Grouphug.getInstance().sendMessage("Sorry, the intertubes seem to be clogged up, " +
                    "I catched an IOException.", false);
            ex.printStackTrace();
        }
    }

    private ConfessionItem random() throws IOException {
        return getConfession("http://confessions.grouphug.us/random");
    }

    private ConfessionItem newest() throws IOException {
        return getConfession("http://confessions.grouphug.us/confessions/new");
    }

    // TODO - this should not "invent" a new ConfessionItem in order to provide an error message.
    /**
     * Searches for a keyword in a confession.
     * @param query the keyword(s) to search for
     * @return the first confession found with the keyword
     * @throws java.io.IOException sometimes
     */
    private ConfessionItem search(String query) throws IOException {
        URL confessionURL = Google.search(query+"+site:grouphug.us/confessions/");
        if(confessionURL == null)
            return new ConfessionItem("No one has confessed about their "+query+" problem yet.");
        else
            return getConfession(confessionURL.toString());
    }

    /**
     * Gets a specified confession from grouphug.us
     * @param urlString The URL of the grouphug confession - should be one specific confession URL
     * @return the successfully fetched confession
     * @throws java.io.IOException sometimes
     */
    private ConfessionItem getConfession(String urlString) throws IOException {

        String confession = "";
        System.out.println("Confession: Opening '"+urlString+"'...");
        ArrayList<String> lines = Web.fetchHtmlList(urlString);

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

        confession = Grouphug.entitiesToChars(confession);
        confession = confession.replaceAll("\\<.*?\\>",""); // strip html tags
        confession = confession.substring(0, confession.length()-1);

        System.out.println("The string is: " + confession);

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
