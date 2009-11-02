package grouphug.modules;

import grouphug.Grouphug;
import grouphug.ModuleHandler;
import grouphug.exceptions.SQLUnavailableException;
import grouphug.listeners.TriggerListener;
import grouphug.util.SQLHandler;
import grouphug.util.Web;

import java.io.BufferedReader;
import java.io.IOException;
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

    private static final int NOT_CHANGED = 0;
    private static final int CHANGED = 1;
    private static final int DELIVERED = 2;

    private boolean threadWorking = false;
    private int itemsRemaining = 0;

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
        } catch(SQLUnavailableException ex) {
            System.err.println("Package tracking module unable to load because SQL is unavailable.");
        } catch (SQLException e) {
            System.err.println("Package tracking module unable to load because it was unable to load " +
                    "existing package list from SQL!");
            e.printStackTrace();
        }
    }

    public void onTrigger(String channel, String sender, String login, String hostname, String message) {
        if(message.equals(TRIGGER_LIST)) {
            if(items.size() == 0) {
                Grouphug.getInstance().sendMessage("No packages are being tracked. What's wrong with you people?");
            } else {
                for(TrackingItem ti : items) {
                    Grouphug.getInstance().sendMessage(ti.getTrackingNumber() + ": " + ti.getStatus() +
                            " (for " + ti.getOwner() + ")");
                }
            }
        } else if(message.startsWith(TRIGGER_DEL)) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).getTrackingNumber().equals(message.replace(TRIGGER_DEL, "").trim())) {
                    if(threadWorking) {
                        Grouphug.getInstance().sendMessage("Sorry, I'm currently polling for updates. Modifying the " +
                                "package list now would make me go haywire. I have " + itemsRemaining + " packages left to check, " +
                                "count to 10 for each of them and try again.");
                        return;
                    }
                    try {
                        // i know it's wrong to say that it's done before you do it but we need the trackingnumber before it's really removed!
                        Grouphug.getInstance().sendMessage("Ok, stopped tracking package '" + items.get(i).getTrackingNumber() + "'.");
                        items.get(i).remove();
                    } catch (SQLException e) {
                        Grouphug.getInstance().sendMessage("I have the package but failed to remove it from the SQL db for some reason!");
                        e.printStackTrace();
                    }
                    return;
                }
            }
            Grouphug.getInstance().sendMessage("Sorry, I'm not tracking any package with ID '" +
                    message.replace(TRIGGER_DEL, "").trim() + "'. Try " + Grouphug.MAIN_TRIGGER + TRIGGER + " " + TRIGGER_LIST);
        } else {
            // User wants to add a new item for tracking, but check if we're already tracking it
            try {
                TrackingItem arrived = null;
                for(TrackingItem ti : items) {
                    if(ti.getTrackingNumber().equals(message)) {
                        int result = ti.update();
                        if(result == CHANGED) {
                            Grouphug.getInstance().sendMessage("New status for '" + message + "': " + ti.getStatus(), true);
                            return;
                        } else if(result == NOT_CHANGED) {
                                Grouphug.getInstance().sendMessage("No change for '" + message + "': " + ti.getStatus(), true);
                            return;
                        } else if(result == DELIVERED) {
                            arrived = ti;
                            break;
                        }
                    }
                }
                if(threadWorking) {
                    Grouphug.getInstance().sendMessage("Sorry, I'm currently polling for updates. Modifying the " +
                            "package list now would make me go haywire. I have " + itemsRemaining + " packages left " +
                            "to check, count to 10 for each of them and try again.");
                    return;
                }
                if(arrived != null) {
                    Grouphug.getInstance().sendMessage("Your package has been delivered. Removing it from my list.");
                    Grouphug.getInstance().sendMessage("Status: " + arrived.getStatus());
                    arrived.remove();
                    Grouphug.getInstance().sendMessage("Now tracking " + items.size() + " packages.");
                    return;
                }
                TrackingItem newItem = new TrackingItem(message.trim(), sender);
                if(newItem.update() == DELIVERED) {
                    Grouphug.getInstance().sendMessage("Your package has already been delivered. I will not track it further.");
                    Grouphug.getInstance().sendMessage("Status: " + newItem.getStatus());
                    return;
                }
                Grouphug.getInstance().sendMessage("Adding package '" + message + "' to tracking list.");
                ArrayList<String> params = new ArrayList<String>();
                params.add(newItem.getTrackingNumber());
                params.add(newItem.getStatus());
                params.add(newItem.getOwner());
                int id = sqlHandler.insert("insert into " + DB_NAME + " (trackingnr, status, owner) VALUES ('?', '?', '?');", params);
                newItem.setId(id);

                // if we came this far, no exception was thrown. if it was, the item won't get added to the list.
                items.add(newItem);
                Grouphug.getInstance().sendMessage("Status: " + newItem.getStatus());
            } catch(IOException e) {
                Grouphug.getInstance().sendMessage("Sorry, I caught an IOException. Try again later or something.");
                e.printStackTrace();
            } catch (SQLException e) {
                Grouphug.getInstance().sendMessage("Sorry, SQL failed on me. Please fix the problem and try again.");
                e.printStackTrace();
            }
        }
    }

    /**
     * This is the posten.no poller
     */
    public void run() {
        int fails = 0;
        // we're started before the bot has connected, so sleep a while first
        try {
            Thread.sleep(30 * 1000);
        } catch(InterruptedException ex) {
            // just continue
        }
        Vector<TrackingItem> itemsToRemove = new Vector<TrackingItem>();
        while(true) {
            try {
                threadWorking = true;
                itemsRemaining = items.size();
                for(TrackingItem ti : items) {
                    switch(ti.update()) {
                        case CHANGED:
                            Grouphug.getInstance().sendMessage(ti.getOwner() + ": Package '" + ti.getTrackingNumber() + "' has exciting new changes!");
                            Grouphug.getInstance().sendMessage(ti.getStatus(), true);
                            break;

                        case NOT_CHANGED:
                            break;

                        case DELIVERED:
                            Grouphug.getInstance().sendMessage(ti.getOwner() + " has just picked up his/her package '" + ti.getTrackingNumber() + "':");
                            Grouphug.getInstance().sendMessage(ti.getStatus(), true);
                            itemsToRemove.add(ti);
                            Grouphug.getInstance().sendMessage("Removing this one from my list. Currently tracking " + (items.size() - itemsToRemove.size()) + " packages.");
                            break;
                    }
                    // let's sleep a few seconds between each item and go easy on the web server
                    try {
                        Thread.sleep(5 * 1000);
                    } catch(InterruptedException ex) {
                        // continue
                    }
                }
                for(TrackingItem toRemove : itemsToRemove) {
                    toRemove.remove();
                }
                itemsToRemove.clear();
                itemsRemaining--;
                threadWorking = false;
                fails = 0;
            } catch(IOException ex) {
                fails++;
                ex.printStackTrace();
            } catch (SQLException ex) {
                fails++;
                ex.printStackTrace();
            } catch(Exception ex) {
                fails++;
                System.err.println("Tracking module thread caught an exception.");
                System.err.println("I will pretend like nothing happened and try again soon, " +
                        "let's hope it is recoverable.");
                ex.printStackTrace();
            }
            if(fails > 5) {
                fails = 0;
                Grouphug.getInstance().sendMessage("The package tracking module has now failed 5 times in a row. " +
                        "If this continues, you might want to check the logs and your package status manually.");
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
         * Removes this item from memory and db
         * @throws SQLException if SQL fails
         */
        public void remove() throws SQLException {
            ArrayList<String> params = new ArrayList<String>();
            params.add(String.valueOf(getId()));
            sqlHandler.delete("delete from " + DB_NAME + " where id='?';", params);
            items.remove(this);
        }

        /**
         * Updates the result of this tracking item. Use when polling.
         * @return CHANGED, NOT_CHANGED or DELIVERED according to its status change
         * @throws IOException if IO fails
         * @throws java.sql.SQLException if SQL fails
         */
        public int update() throws IOException, SQLException {
            BufferedReader posten = Web.prepareBufferedReader("http://sporing.posten.no/Sporing/KMSporingInternett.aspx?ShipmentNumber="+trackingNumber);

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
                        return CHANGED;
                    } else {
                        return NOT_CHANGED;
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
            if(newStatus.contains("UTLEVERT")) {
                setStatus(newStatus);
                return DELIVERED;
            } else if(!oldStatus.equals(newStatus)) {
                setStatus(newStatus);
                return CHANGED;
            } else {
                return NOT_CHANGED;
            }
        }
    }
}
