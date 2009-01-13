package grouphug.modules;

import grouphug.Grouphug;
import grouphug.GrouphugModule;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.SocketTimeoutException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import com.tecnick.htmlutils.htmlentities.HTMLEntities;

public class Confession implements GrouphugModule {
    // TODO: reverify that timeouts are handled properly

    private static final String TRIGGER = "gh";
    private static final String TRIGGER_HELP = "confession";
    private static final String KEYWORD_NEWEST = "-newest";
    private static final int CONN_TIMEOUT = 10000; // ms
    private static String errorConfession = "I have nothing to confess at the moment, please try again later.";

    // HACK this needs to be changed according to how many newsitems http://grouphug.us/ has. Se HACK tag further down.
    private static final int NO_OF_NEWSITEMS = 3;



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
    }

    private ConfessionItem random() {
        try {
            return getConfession(new URL("http", "beta.grouphug.us", "/random?page=1"));
        } catch(MalformedURLException ex) {
            System.err.println("Grouphug confession error: MalformedURLException of hard-coded URL in function random()!");
            return null;
        }
    }

    private ConfessionItem newest() {
        try {
            return getConfession(new URL("http", "beta.grouphug.us", "/confessions/new"));
        } catch(MalformedURLException ex) {
            System.err.println("Grouphug confession error: MalformedURLException of hard-coded URL in function newest()!");
            return null;
        }
    }

    // TODO - this should not "invent" a new ConfessionItem in order to provide an error message.
    /**
     * Searches for a keyword in a confession.
     * @param query the keyword(s) to search for
     * @return the first confession found with the keyword
     */
    private ConfessionItem search(String query) {
        URL confessionURL;
        try {
            confessionURL = Google.search(query+"+site:grouphug.us/confessions/");
        } catch(IOException e) {
            return new ConfessionItem("Google wouldn't let me search for "+query+" problems.\n", -1);
        }
        if(confessionURL == null)
            return new ConfessionItem("No one has confessed about their "+query+" problem yet.\n", -1);
        else
            return getConfession(confessionURL);
    }

    /**
     * Gets a specified confession from grouphug.us
     * @param url The URL of the grouphug confession - should be one specific confession URL
     * @return the successfully fetched confession
     */
    private ConfessionItem getConfession(URL url) {
        String line;
        String confession = "";
        int hugs;
        try {

            System.out.print("Opening grouphug connection... ");
            URLConnection urlConn = url.openConnection();
            urlConn.setConnectTimeout(CONN_TIMEOUT);

            BufferedReader gh = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            // We dig down to the confession content
            // HACK: Skip to the x'th content div - as the (currently) x newsitems also use this layout
            int i=0;
            while((line = gh.readLine()) != null) {
                if(line.equals("  <div class=\"content\">")) {
                    if(i==NO_OF_NEWSITEMS)
                        break;
                    else
                        i++;
                }
            }

            System.out.println("OK");

            // check that the searches didn't fail
            if(line == null) {
                System.err.println("Grouphug confession error: Couldn't find confession content!");
                return null;
            }


            // now we save all data until we reach the end
            while((line = gh.readLine()) != null) {
                if(line.equals("  </div>"))
                    break;
                confession += line.trim()+"\n";
            }

            // check that the searches didn't fail
            if(line == null) {
                System.err.println("Grouphug confession error: Couldn't find end of confession content!");
                return null;
            }

            // find no. of hugs
            while((line = gh.readLine()) != null) {
                if(line.equals("    <div class=\"links\">"))
                    break;
            }

            // check that the searches didn't fail
            if(line == null) {
                System.err.println("Grouphug confession error: Couldn't find no. of hugs content!");
                return null;
            }

            // trim - strip tags - remove " hugs" and parse int
            line = gh.readLine().trim().replaceAll("\\<.*?\\>","");
            i = 0;
            for(; line.charAt(i) != ' '; i++) {
                if(i >= line.length()) {
                    System.err.println("Grouphug confession error: Couldn't find a space in expected no. of hugs-line!");
                    return null;
                }
            }
            line = line.substring(0, i);
            try {
                hugs = Integer.parseInt(line);
            } catch(Exception e) {
                System.err.println("Grouphug confession error: Couldn't parse expected hugs int - line: "+line);
                return null;
            }

        } catch(SocketTimeoutException e) {
            System.err.println("Timeout");
            return null;
        } catch(IOException e) {
            System.err.println("Grouphug confession error: IOException: "+e+"\n");
            e.printStackTrace();
            return null;
        }

        confession = HTMLEntities.unhtmlentities(confession); 

        // strip tags
        confession = confession.replaceAll("\\<.*?\\>","");

        return new ConfessionItem(confession, hugs);
    }

    /**
     * This object is a confession from grouphug.us
     * Contains a String of the confession, and an int of the no. of hugs
     */
    private static class ConfessionItem {

        private String confession;
        private int hugs;

        public String getConfession() {
            return confession;
        }

        public int getHugs() {
            return hugs;
        }

        public ConfessionItem(String confession, int hugs) {
            this.confession = confession;
            this.hugs = hugs;
        }

        public String toString() {
            if(hugs == -1)
                return confession;
                // TODO: test this: need to cut last char?
                //return confession.substring(0, confession.length()-1); // substring because last char is \n
            else
                return confession+hugs+" klemz";
        }
    }

}
