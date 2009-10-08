package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Tracking implements TriggerListener {

    private static final String TRIGGER = "track";
    private static final String TRIGGER_HELP = "tracking";
    private static final int CONN_TIMEOUT = 10000; // ms

    public Tracking(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Posten.no package tracking:\n" +
                    "  "+Grouphug.MAIN_TRIGGER+TRIGGER +"<package id / kollinr>");
        System.out.println("Package tracking module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        String tracked;
        try {
            tracked = Tracking.search(message);
        } catch(IOException e) {
            Grouphug.getInstance().sendMessage("Sorry, the intartubes seems to be clogged up (IOException)", false);
            System.err.println(e.getMessage()+"\n"+e.getCause());
            return;
        }

        if(tracked == null) {
            Grouphug.getInstance().sendMessage("No results for "+message+".", false);
        } else {
            Grouphug.getInstance().sendMessage(tracked, false);
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
        String curLine;
        int status = 0;
        String output = "";
        while (status < 5) {
            curLine = posten.readLine();
            if (curLine == null) {
                return null;
            }
            String errorSearch = "SporingUserControl_ErrorMessage";
            int errorIndex = curLine.indexOf(errorSearch);

            if(errorIndex != -1) {
                return null;
            }

            if (status == 0) {
                String resultSearch = "TH colspan=";
                int resultIndex = curLine.indexOf(resultSearch);
                if (resultIndex != -1) {
                    status = 1;
                }
            } else {
                String resultSearch = "<td>";
                int resultIndex = curLine.indexOf(resultSearch);
                if (resultIndex != -1) {
                    output += posten.readLine().trim() + " ";
                    status++;
                }
            }
        }
        return output.replace("<br/>", " - ");
    }
}
