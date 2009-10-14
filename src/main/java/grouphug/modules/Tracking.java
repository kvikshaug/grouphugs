package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQLHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

public class Tracking implements TriggerListener, Runnable {

    private static final String TRIGGER = "track";
    private static final String TRIGGER_HELP = "tracking";
    private static final String TRIGGER_LIST = "-ls";
    private static final String TRIGGER_DEL = "-rm";
    private static final String DB_NAME = "tracking";
    private static final int POLLING_TIME = 30; // minutes

    private boolean threadWorking = false;

    private Vector<TrackingItem> items = new Vector<TrackingItem>();
    private SQLHandler sqlHandler;

    public Tracking(ModuleHandler moduleHandler) {
        try {
            sqlHandler = SQLHandler.getSQLHandler();
            ArrayList<Object[]> rows = sqlHandler.select("select * from " + DB_NAME + ";");
            for(Object[] row : rows) {
                items.add(new TrackingItem((Integer)row[0], (String)row[1], (String)row[2], (String)row[3]));
            }


            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Posten.no package tracking. I will keep track of the package by " +
                    "polling and let you know when anything changes.\n" +
                    "  Start tracking:  "+Grouphug.MAIN_TRIGGER+TRIGGER + " <package id / kollinr>\n" +
                    "  Stop tracking:   "+Grouphug.MAIN_TRIGGER+TRIGGER + " " + TRIGGER_DEL + " <package id / kollinr>\n" +
                    "  List all:        "+Grouphug.MAIN_TRIGGER+TRIGGER + " " + TRIGGER_LIST + "\n" +
                    "Adding a package that's already added will force an update on its status.");
            new Thread(this).start();
            System.out.println("Package tracking module loaded.");
        } catch(ClassNotFoundException ex) {
            System.err.println("Package tracking module unable to load because the SQL driver is unavailable.");
        } catch (SQLException e) {
            System.err.println("Package tracking module unable to load because it was unable to load " +
                    "existing package list from SQL!");
            e.printStackTrace();
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_LIST)) {
            if(items.size() == 0) {
                Grouphug.getInstance().sendMessage("No packages are being tracked. What's wrong with you people?", false);
            } else {
                for(TrackingItem ti : items) {
                    Grouphug.getInstance().sendMessage(ti.getTrackingNumber() + ": " + ti.getStatus() +
                            " (for " + ti.getOwner() + ")", false);
                }
            }
        } else if(message.startsWith(TRIGGER_DEL)) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).getTrackingNumber().equals(message.replace(TRIGGER_DEL, "").trim())) {
                    if(threadWorking) {
                        Grouphug.getInstance().sendMessage("Sorry, I'm currently polling for updates. Modifying the " +
                                "package list now would make me go haywire. Please try again in a minute or so.", false);
                        return;
                    }
                    try {
                        ArrayList<String> params = new ArrayList<String>();
                        params.add(String.valueOf(items.get(i).getId()));
                        sqlHandler.delete("delete from " + DB_NAME + " where id='?';", params);

                        // i know it's wrong to say that it's done before you do it but we need the trackingnumber before it's really removed!
                        Grouphug.getInstance().sendMessage("Ok, stopped tracking package '" + items.get(i).getTrackingNumber() + "'.", false);
                        items.remove(i);
                    } catch (SQLException e) {
                        Grouphug.getInstance().sendMessage("I have the package but failed to remove it from the SQL db for some reason!", false);
                        e.printStackTrace();
                    }
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
                            Grouphug.getInstance().sendMessage("New status for '" + message + "': " + ti.getStatus(), true);
                        } else {
                            Grouphug.getInstance().sendMessage("No change for '" + message + "': " + ti.getStatus(), true);
                        }
                        return;
                    }
                }
                if(threadWorking) {
                    Grouphug.getInstance().sendMessage("Sorry, I'm currently polling for updates. Modifying the " +
                            "package list now would make me go haywire. Please try again in a minute or so.", false);
                    return;
                }
                Grouphug.getInstance().sendMessage("Adding package '" + message + "' to tracking list.", false);
                TrackingItem newItem = new TrackingItem(message.trim().replace(" ", ""), sender);
                newItem.update();
                ArrayList<String> params = new ArrayList<String>();
                params.add(newItem.getTrackingNumber());
                params.add(newItem.getStatus());
                params.add(newItem.getOwner());
                int id = sqlHandler.insert("insert into " + DB_NAME + " (trackingnr, status, owner) VALUES ('?', '?', '?');", params);
                newItem.setId(id);

                // if we came this far, no exception was thrown. if it was, the item won't get added to the list.
                items.add(newItem);
                Grouphug.getInstance().sendMessage("Status: " + newItem.getStatus(), false);
            } catch(IOException e) {
                Grouphug.getInstance().sendMessage("Sorry, I caught an IOException. Try again later or something.", false);
                e.printStackTrace();
            } catch (SQLException e) {
                Grouphug.getInstance().sendMessage("Sorry, SQL failed on me. Please fix the problem and try again.", false);
                e.printStackTrace();
            }
        }
    }

    /**
     * This is the posten.no poller
     */
    public void run() {
        // we're started before the bot has connected, so sleep a while first
        try {
            Thread.sleep(30 * 1000);
        } catch(InterruptedException ex) {
            // just continue
        }
        while(true) {
            try {
                threadWorking = true;
                for(TrackingItem ti : items) {
                    if(ti.update()) {
                        Grouphug.getInstance().sendMessage(ti.getOwner() + ": Package '" + ti.getTrackingNumber() + "' has exciting new changes!", false);
                        Grouphug.getInstance().sendMessage(ti.getStatus(), true);
                    }
                    // let's sleep a few seconds between each item and go easy on the web server
                    try {
                        Thread.sleep(5 * 1000);
                    } catch(InterruptedException ex) {
                        // continue
                    }
                }
                threadWorking = false;
            } catch(IOException ex) {
                Grouphug.getInstance().sendMessage("Hi guys, just thought I should let you know that the " +
                        "package tracking module caught an IOException when polling for changes.\n" +
                        "You might want to check your package status manually.", false);
                ex.printStackTrace();
            } catch (SQLException ex) {
                Grouphug.getInstance().sendMessage("Hi guys, just thought I should let you know that the " +
                        "package tracking module caught an SQLException when polling for changes.\n" +
                        "You might want to check your package status manually.", false);
                ex.printStackTrace();
            } catch(Exception ex) {
                System.err.println("Tracking module thread caught an exception.");
                System.err.println("I will pretend like nothing happened and try again soon, " +
                        "let's hope it is recoverable.");
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

        private int id;
        private String trackingNumber;
        private String status;
        private String owner;

        public long getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTrackingNumber() {
            return trackingNumber;
        }

        public String getStatus() {
            return status == null ? "Status unknown" : status;
        }

        public void setStatus(String status) throws SQLException {
            this.status = status;
            ArrayList<String> params = new ArrayList<String>();
            params.add(status);
            params.add(String.valueOf(id));
            sqlHandler.update("update " + DB_NAME + " set status='?' where id='?';", params);
        }

        public String getOwner() {
            return owner;
        }

        private TrackingItem(String trackingNumber, String owner) {
            this.trackingNumber = trackingNumber;
            this.owner = owner;
        }

        private TrackingItem(int id, String trackingNumber, String status, String owner) {
            this.id = id;
            this.trackingNumber = trackingNumber;
            this.status = status;
            this.owner = owner;
        }

        /**
         * Updates the result of this tracking item. Use when polling.
         * @return true if a change was detected, false if it has the same status
         * @throws IOException if IO fails
         * @throws java.sql.SQLException if SQL fails
         */
        public boolean update() throws IOException, SQLException {
            URLConnection urlConn;
            urlConn = new URL("http", "sporing.posten.no", "/Sporing/KMSporingInternett.aspx?ShipmentNumber="+trackingNumber).openConnection();

            urlConn.setConnectTimeout(10000);
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
                    String newStatus = "The package ID is invalid (according to the tracking service)";
                    String oldStatus = getStatus();
                    if(!oldStatus.equals(newStatus)) {
                        setStatus(newStatus);
                        return true;
                    } else {
                        return false;
                    }
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
            String oldStatus = getStatus();
            String newStatus = output.replace("<br/>", " - ").trim();
            if(!oldStatus.equals(newStatus)) {
                setStatus(newStatus);
                return true;
            } else {
                return false;
            }
        }
    }
}
