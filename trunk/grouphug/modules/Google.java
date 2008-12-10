package grouphug.modules;

import grouphug.Grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Google implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "google ";
    private static final String TRIGGER_HELP = "google";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Google(Grouphug bot) {
        Google.bot = bot;
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Google search:");
            bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<searchword(s)>");
            return true;
        }
        return false;
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {

        if(!message.startsWith(TRIGGER))
            return;

        URL url;
        try {
            url = Google.search(message.substring(TRIGGER.length()));
        } catch(IOException e) {
            bot.sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(url == null) {
            bot.sendMessage("No results for "+message.substring(TRIGGER.length())+".", false);
        } else {
            bot.sendMessage(url.toString(), false);
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