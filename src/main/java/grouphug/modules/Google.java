package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Google implements TriggerListener {

    private static final String TRIGGER = "google";
    private static final String TRIGGER_HELP = "google";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Google(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Google search:\n" +
                   "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<searchword(s)>");
        System.out.println("Google module loaded.");
    }


    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        URL url;
        try {
            url = Google.search(message);
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(url == null) {
            Grouphug.getInstance().sendMessage("No results for "+message+".", false);
        } else {
            Grouphug.getInstance().sendMessage(url.toString(), false);
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

        String parseSearch = "<h3 class=r><a href=\"";
        String line;
        do {
            line = google.readLine();
        } while(line != null && !line.contains(parseSearch));
        if(line == null)
          return null;

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