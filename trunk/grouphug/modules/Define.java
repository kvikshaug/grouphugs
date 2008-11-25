package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Define implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "define ";
    private static final String TRIGGER_HELP = "define";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Define(Grouphug bot) {
        Define.bot = bot;
    }


    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Define: Use google to give a proper definition of a word.");
            bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<keyword>");
            return true;
        }
        return false;
    }


    public void trigger(String channel, String sender, String login, String hostname, String message) {
        if(!message.startsWith(TRIGGER))
            return;

        String answer;

        try {
            answer = Define.search(message.substring(TRIGGER.length()));
        } catch(IOException e) {
            bot.sendMessage("The intartubes seems to be clogged up (IOException).", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(answer == null)
            bot.sendMessage("No definition found for "+message.substring(TRIGGER.length())+".", false);
        else
            bot.sendMessage(Grouphug.entitiesToChars(answer), false);
    }

    public static String search(String query) throws IOException {

        query = query.replace(' ', '+');
        System.out.print("Opening google connection... ");

        URLConnection urlConn;
        try {
            urlConn = new URL("http", "www.google.com", "/search?q=define:"+query+"").openConnection();
        } catch(MalformedURLException ex) {
            System.err.println("Define search error: MalformedURLException in partially dynamic URL in search()!");
            return null;
        }

        urlConn.setConnectTimeout(CONN_TIMEOUT);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

        BufferedReader google = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        System.out.println("OK");

        // Search for some hardcoded stuff to try to parse a definition. this is fugly
        String line;
        int startIndex;
        String parseSearch = "<ul type=\"disc\"><font size=-1><li>";
        while((line = google.readLine()) != null) {
            startIndex = line.indexOf(parseSearch);

            // if -1, then the phrase wasn't found
            if(startIndex == -1)
                continue;

            // If we reach this point, we found the wanted string, so find the end of the definition
            startIndex += parseSearch.length();
            int i = startIndex;
            for(; line.charAt(i) != '<'; i++) {
                if(i == line.length()) {
                    throw new IOException("Define search error: Couldn't find ending < in definition");
                }
            }
            return line.substring(startIndex, i);
        }

        // If we get here, we couldn't find the definition, or parsing went wrong - but we can't know which, for sure
        return null;
    }
}
