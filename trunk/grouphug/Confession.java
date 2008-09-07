package grouphug;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.SocketTimeoutException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Confession implements GrouphugModule {
    // TODO: h�ndter timeouts, ogs� for gh (ikke bare google) :p

    protected static final String TRIGGER = "!gh";
    private static final String KEYWORD_NEWEST = "-newest";
    private static final int CONN_TIMEOUT = 1000; // ms
    private static final int GH_CONN_TIMEOUT = 10000; // ms
    private static String errorConfession = "I have nothing to confess at the moment, please try again later.";

    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {

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
            bot.sendMessage(errorConfession);
        else
            bot.sendMessage(conf.toString());
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

    /**
     * Searches for a keyword in a confession. Returns null on failure. (Should rather throw a manual exception).
     * @param query the keyword(s) to search for
     * @return the first confession found with the keyword
     */
    private ConfessionItem search(String query) {
        try {
            query = query.replace(' ', '+');
            System.out.print("Opening google connection... ");

            URLConnection urlConn;
            try {
                urlConn = new URL("http", "www.google.com", "/search?q="+query+"+site:grouphug.us/confessions/").openConnection();
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

            int startIndex;
            // TODO: why catch NPE here??
            try {
                startIndex = line.indexOf("<h3 class=r><a href=\"");
            } catch(NullPointerException e) {
                System.err.println("Grouphug confession error: Nullpointerexception at google search!");
                return null;
            }

            // if -1, then the phrase wasn't found
            if(startIndex == -1) {
                return new ConfessionItem("No one has confessed about their "+query+" problem yet.\n", -1);
            }

            startIndex += 21; // because we search for "<h2 class=r><a href=\"" above, skip over that
            int i = startIndex;
            for(; line.charAt(i) != '"'; i++) {
                if(i == line.length()) {
                    System.err.println("Grouphug confession error: Couldn't find ending \" in hyperlink reference");
                    return null;
                }
            }

            return getConfession(new URL(line.substring(startIndex, i)));

        } catch(IOException e) {
            System.err.println("Grouphug confession error: IOException: "+e+"\n");
            e.printStackTrace();
            return null;
        }
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
            urlConn.setConnectTimeout(GH_CONN_TIMEOUT);

            BufferedReader gh = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            // We dig down to the confession content
            // HACK: Skip to the third content div - as the (currently) two newsitems also use this layout
            int i=0;
            while((line = gh.readLine()) != null) {
                if(line.equals("  <div class=\"content\">")) {
                    if(i==2)
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

        confession = confession.replace("&nbsp;", " ");
        confession = confession.replace("&#8216;", "'");
        confession = confession.replace("&#8217;", "'");
        confession = confession.replace("&#8220;", "\"");
        confession = confession.replace("&#8221;", "\"");
        confession = confession.replace("&#8230;", "...");
        confession = confession.replace("&#8212;", " - ");

        // strip tags
        confession = confession.replaceAll("\\<.*?\\>","");

        return new ConfessionItem(confession, hugs);
    }
}