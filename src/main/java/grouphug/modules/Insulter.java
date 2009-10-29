package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.Web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class Insulter implements TriggerListener {

    private static final String TRIGGER = "insult";
    private static final String TRIGGER_HELP = "insult";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Insulter(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Insult someone you don't like:\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER +" <person>\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER);
        System.out.println("Insult module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        String insulted = null;
        if(!message.equals("")) {
            insulted = message;
        }
        String searchQ = "<td bordercolor=\"#FFFFFF\"><font face=\"Verdana\" size=\"4\"><strong><i>";

        try {
            ArrayList<String> lines = Web.fetchHtmlLines("http://www.randominsults.net/");
            for(String line : lines) {
                int insultStart = line.indexOf(searchQ);
                if(insultStart == -1) {
                    continue;
                }
                int insultEnd = line.indexOf('<', insultStart + searchQ.length());
                if(insultEnd == -1) {
                    continue;
                }
                String insult = line.substring(insultStart + searchQ.length(), insultEnd);

                if(insulted != null) {
                    Grouphug.getInstance().sendMessage(insulted+": "+insult, false);
                } else {
                    Grouphug.getInstance().sendMessage(insult, false);
                }
                return;
            }
            if(insulted != null) {
                Grouphug.getInstance().sendMessage("Sorry, I was unable to parse randominsults.net because I was too " +
                    "busy throwing up by " + insulted + "'s ghastly presence.", false);
            } else {
                Grouphug.getInstance().sendMessage("Sorry, I was unable to parse randominsults.net because I was too " +
                        "busy throwing up by your ghastly presence.", false);
            }
        } catch(IOException ex) {
            if(insulted != null) {
                Grouphug.getInstance().sendMessage("Sorry, " + insulted + "'s ghastly presence made me throw up an IOException.", false);
            } else {
                Grouphug.getInstance().sendMessage("Sorry, your ghastly presence made me throw up an IOException.", false);
            }
            ex.printStackTrace();
        }
    }

    // TODO this shouldn't return null when it doesn't find anything, but throw an exception or something! fix!
    public static URL search(String query) throws IOException {

        query = query.replace(' ', '+');

        URLConnection urlConn;
        try {
            urlConn = new URL("http", "www.google.com", "/search?q="+query+"").openConnection();
        } catch(MalformedURLException ex) {
            System.err.println("Google search error: MalformedURLException in partially dynamic URL in search()!");
            return null;
        }

        urlConn.setConnectTimeout(CONN_TIMEOUT);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

        BufferedReader google = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        // Find an URL to the search result
        // HACK fugly jump to line 3 (should check for nullptr)
        google.readLine();
        google.readLine();
        String line = google.readLine();

        if(line == null)
            return null;

        String parseSearch = "<h3 class=r><a href=\"";
        int startIndex = line.indexOf(parseSearch);

        // if -1, then the phrase wasn't found - should return error here, not "not found"
        if(startIndex == -1)
            return null;

        startIndex += parseSearch.length();
        int i = startIndex;
        for(; line.charAt(i) != '"'; i++) {
            if(i == line.length()) {
                throw new IOException("Google search error: Couldn't find ending \" in hyperlink reference");
            }
        }
        return new URL(line.substring(startIndex, i));
    }
}