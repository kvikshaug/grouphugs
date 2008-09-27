package grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

class Google implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "google ";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Google(Grouphug bot) {
        Google.bot = bot;
    }

    public void helpTrigger(String channel, String sender, String login, String hostname, String message) {
        bot.sendMessage(sender, "Google search:");
        bot.sendMessage(sender, " - Trigger: "+Grouphug.MAIN_TRIGGER+Google.TRIGGER +"<searchword(s)>");
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {

        if(!message.startsWith(TRIGGER))
            return;

        URL url = null;
        try {
            url = Google.search(message.substring(TRIGGER.length()));
        } catch(IOException e) {
            bot.sendMessage("The intartubes seems to be clogged up (IOException).", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(url == null) {
            bot.sendMessage("No results for "+message.substring(TRIGGER.length())+".", false);
        } else {
            bot.sendMessage(url.toString(), false);
        }
    }

    public static URL search(String query) throws IOException {

        query = query.replace(' ', '+');
        System.out.print("Opening google connection... ");

        URLConnection urlConn;
        try {
            urlConn = new URL("http", "www.google.com", "/search?q="+query+"").openConnection();
        } catch(MalformedURLException ex) {
            System.err.println("Grouphug confession error: MalformedURLException in partially dynamic URL in search()!");
            return null;
        }

        urlConn.setConnectTimeout(CONN_TIMEOUT);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

        BufferedReader google = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        System.out.println("OK");

        // Find an URL to the search result
        // HACK fugly jump to line 3 (should check for nullptr)
        google.readLine();
        google.readLine();
        String line = google.readLine();

        if(line == null)
            return null;

        int startIndex = line.indexOf("<h3 class=r><a href=\"");

        // if -1, then the phrase wasn't found
        if(startIndex == -1)
            return null;

        startIndex += 21; // because we search for "<h2 class=r><a href=\"" above, skip over that
        int i = startIndex;
        for(; line.charAt(i) != '"'; i++) {
            if(i == line.length()) {
                throw new IOException("Grouphug confession error: Couldn't find ending \" in hyperlink reference");
            }
        }
        return new URL(line.substring(startIndex, i));
    }
}