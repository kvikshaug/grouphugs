package grouphug.modules;

import grouphug.GrouphugModule;
import grouphug.Grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GoogleFight implements GrouphugModule {

    private static Grouphug bot;
    private static final String TRIGGER = "gfight ";
    private static final String TRIGGER_HELP = "googlefight";
    private static final String TRIGGER_VS = " <vs> ";
    private static final int CONN_TIMEOUT = 10000; // ms

    public GoogleFight(Grouphug bot) {
        GoogleFight.bot = bot;
    }

    public String helpMainTrigger(String channel, String sender, String login, String hostname, String message) {
        return TRIGGER_HELP;
    }

    public boolean helpSpecialTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_HELP)) {
            bot.sendNotice(sender, "Google fight:");
            bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+TRIGGER + "<1st search>" + TRIGGER_VS + "<2nd search>");
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

        if(!message.contains(TRIGGER_VS)) {
            bot.sendMessage(sender+", try "+Grouphug.MAIN_TRIGGER+Grouphug.HELP_TRIGGER+" "+TRIGGER_HELP, false);
            return;
        }

        String query1 = message.substring(TRIGGER.length(), message.indexOf(TRIGGER_VS));
        String query2 = message.substring(message.indexOf(TRIGGER_VS) + TRIGGER_VS.length());

        try {
            String hits1 = search(query1);
            String hits2 = search(query2);

            if(hits1 == null)
                hits1 = "no";
            if(hits2 == null)
                hits2 = "no";

            bot.sendMessage(query1+": "+hits1+" results\n"+query2+": "+hits2+" results", false);

        } catch(IOException e) {
            bot.sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
        }
    }

    // TODO uhm, review this whole file, it was done in a hurry just to see if it'd work
    public String search(String query) throws IOException {

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

        String line;
        String search = "</b> of about <b>";
        while((line = google.readLine()) != null) {
            if(line.contains(search)) {
                return line.substring(line.indexOf(search) + search.length(), line.indexOf("</b>", line.indexOf(search) + search.length()));
            }
        }
        return null;
    }
}
