package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class Tracking implements TriggerListener, Runnable {

    // TODO make items persistent (save them in SQL)

    private static final String TRIGGER = "track";
    private static final String TRIGGER_HELP = "tracking";
    private static final String TRIGGER_LIST = "-list";
    private static final String TRIGGER_DEL = "-d";
    private static final int CONN_TIMEOUT = 10000; // ms
    private static final int POLLING_TIME = 30; // minutes

    private ArrayList<TrackingItem> items = new ArrayList<TrackingItem>();

    public Tracking(ModuleHandler moduleHandler) {
        moduleHandler.addTriggerListener(TRIGGER, this);
        moduleHandler.registerHelp(TRIGGER_HELP, "Posten.no package tracking. I will keep track of the package by " +
                "polling and let you know when anything changes.\n" +
                "  Start tracking:  "+Grouphug.MAIN_TRIGGER+TRIGGER + " <package id / kollinr>\n" +
                "  Stop tracking:   "+Grouphug.MAIN_TRIGGER+TRIGGER + " " + TRIGGER_DEL + " <package id / kollinr>\n" +
                "  List all:        "+Grouphug.MAIN_TRIGGER+TRIGGER + " " + TRIGGER_LIST + "\n" +
                "Adding a package that's already added will force an update on its status.");
        new Thread(this).start();
        System.out.println("Package tracking module loaded.");
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_LIST)) {
            if(items.size() == 0) {
                Grouphug.getInstance().sendMessage("No packages are being tracked. Are you all sick or something?", false);
            } else {
                for(TrackingItem ti : items) {
                    Grouphug.getInstance().sendMessage(ti.getTrackingNumber() + ": " + ti.getResult() +
                            " (for " + ti.getOwner() + ")", false);
                }
            }
        } else if(message.startsWith(TRIGGER_DEL)) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).getTrackingNumber().equals(message.replace(TRIGGER_DEL, "").trim())) {
                    Grouphug.getInstance().sendMessage("Ok, stopped tracking package '" + items.get(i).getTrackingNumber() + "'.", false);
                    items.remove(i);
                    return;
                }
            }
            Grouphug.getInstance().sendMessage("Sorry, I'm not tracking any package with ID '" +
                    message.replace(TRIGGER_DEL, "").trim() + "'. Try " + Grouphug.MAIN_TRIGGER + TRIGGER + " " + TRIGGER_LIST, false);
        } else {
            // User wants to add a new item for tracking, but check if we're already tracking it
            try {
                for(TrackingItem ti : items) {
                    if(ti.getTrackingNumber().equals(message)) {
                        if(ti.update()) {
                            Grouphug.getInstance().sendMessage("New status for '" + message + "': " + ti.getResult(), true);
                        } else {
                            Grouphug.getInstance().sendMessage("No change for '" + message + "': " + ti.getResult(), true);
                        }
                        return;
                    }
                }
                Grouphug.getInstance().sendMessage("Adding package '" + message + "' to tracking list.", false);
                TrackingItem newItem = new TrackingItem(message.trim().replace(" ", ""), sender);
                newItem.update();
                Grouphug.getInstance().sendMessage("Status: " + newItem.getResult(), false);
                items.add(newItem);
            } catch(IOException e) {
                Grouphug.getInstance().sendMessage("Sorry, I caught an IOException. Try again later or something.", false);
                e.printStackTrace();
            }
        }
    }

    /**
     * This is the posten.no poller
     */
    public void run() {
        while(true) {
            try {
                for(TrackingItem ti : items) {
                    if(ti.update()) {
                        Grouphug.getInstance().sendMessage(ti.getOwner() + ": Package '" + ti.getTrackingNumber() + "' has exciting new changes!", false);
                        Grouphug.getInstance().sendMessage(ti.getResult(), true);
                    }
                    // let's sleep a few seconds before the next item and go easy on the web server
                    try {
                        Thread.sleep(10 * 1000);
                    } catch(InterruptedException ex) {
                        // just continue
                    }
                }
            } catch(IOException ex) {
                Grouphug.getInstance().sendMessage("Hi guys, just thought I should let you know that the " +
                        "package tracking module caught an IOException when polling for changes.\n" +
                        "You might want to check your package status manually.", false);
                ex.printStackTrace();
            }
            try {
                Thread.sleep(POLLING_TIME * 60 * 1000);
            } catch(InterruptedException ex) {
                // just continue
            }
        }
    }

    private class TrackingItem {

        private String trackingNumber;
        private String result;
        private String owner;

        public String getTrackingNumber() {
            return trackingNumber;
        }

        public void setTrackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
        }

        public String getResult() {
            return result == null ? "Status unknown" : result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        private TrackingItem(String trackingNumber, String owner) {
            this.trackingNumber = trackingNumber;
            this.owner = owner;
        }

        /**
         * Updates the result of this tracking item. Use when polling.
         * @return true if a change was detected, false if it has the same status
         * @throws IOException if IO fails
         */
        public boolean update() throws IOException {
            URLConnection urlConn;
            urlConn = new URL("http", "sporing.posten.no", "/Sporing/KMSporingInternett.aspx?ShipmentNumber="+trackingNumber).openConnection();

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
                    throw new IOException("Unable to parse target site, have they changed their layout or something?");
                }
                String errorSearch = "SporingUserControl_ErrorMessage";
                int errorIndex = curLine.indexOf(errorSearch);

                if(errorIndex != -1) {
                    // no results
                    String oldResult = getResult();
                    setResult("The package ID is invalid (according to the tracking service).");
                    return !oldResult.equals(getResult());
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
            String oldResult = getResult();
            setResult(output.replace("<br/>", " - "));
            return !oldResult.equals(getResult());
        }
    }
}
