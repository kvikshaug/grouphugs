package grouphug;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

class Tracking implements GrouphugModule {
    
    private static Grouphug bot;
    private static final String TRIGGER = "track ";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Tracking(Grouphug bot) {
        Tracking.bot = bot;
    }

    public void helpTrigger(String channel, String sender, String login, String hostname, String message) {
        bot.sendNotice(sender, "Posten.no package tracking:");
        bot.sendNotice(sender, "  "+Grouphug.MAIN_TRIGGER+Tracking.TRIGGER +"<package id / kollinr>");
    }

    public void specialTrigger(String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }

    public void trigger(String channel, String sender, String login, String hostname, String message) {

        if(!message.startsWith(TRIGGER))
            return;

        String tracked = null;
        try {
            tracked = Tracking.search(message.substring(TRIGGER.length()));
        } catch(IOException e) {
            bot.sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(tracked == null) {
            bot.sendMessage("No results for "+message.substring(TRIGGER.length())+".", false);
        } else {
            bot.sendMessage(tracked, false);
        }
    }

    public static String search(String query) throws IOException {
        //trim any surrounding spaces
        query = query.trim();
        query = query.replace(" ", "");

        URLConnection urlConn;
        urlConn = new URL("http", "sporing.posten.no", "/Sporing/KMSporingInternett.aspx?ShipmentNumber="+query).openConnection();

        urlConn.setConnectTimeout(CONN_TIMEOUT);
        urlConn.setRequestProperty("User-Agent", "Firefox/3.0"); // Trick google into thinking we're a proper browser. ;)

        BufferedReader posten = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

        // phear teh ugly hax <3
        String curLine = " ";
        int status = 0;
        String output = "";
        while (status < 5){
            curLine = posten.readLine();
            if (curLine == null) 
                return null;
            String errorSearch = "SporingUserControl_ErrorMessage";
            int errorIndex = curLine.indexOf(errorSearch);
            
            if(errorIndex != -1)
                return null;

            if (status == 0) 
            {
                String resultSearch = "TH colspan=";
                int resultIndex = curLine.indexOf(resultSearch);
                if (resultIndex != -1)
                    status = 1;
            }
            else {            
                String resultSearch = "<td>";
                int resultIndex = curLine.indexOf(resultSearch);
                if (resultIndex != -1)
                {
                    output += posten.readLine().trim() + " ";
                    status++;
                }
            }
        }
        return Grouphug.fixEncoding(bot, output);
    }
}
