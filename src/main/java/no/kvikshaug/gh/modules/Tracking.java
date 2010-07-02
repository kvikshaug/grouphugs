package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;
import no.kvikshaug.gh.util.Web;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Tracking implements TriggerListener, Runnable {

    private static final String TRIGGER = "track";
    private static final String TRIGGER_HELP = "track";
    private static final String TRIGGER_LIST = "-ls";
    private static final String TRIGGER_DEL = "-rm";
    private static final String DB_NAME = "tracking";
    private static final int POLLING_TIME = 30; // minutes

    private static final int NOT_CHANGED = 0;
    private static final int CHANGED = 1;
    private static final int DELIVERED = 2;

    private boolean threadWorking = false;
    private int itemsRemaining = 0;

    // outputted when user tries to remove or add a package while the poller thread is polling
    private String warnThreadWorking = "Sorry, I'm currently polling for updates. Modifying the " +
            "package list now would make me go haywire. I have %s packages left to check, " +
            "count to 10 for each of them and try again.";

    private Vector<TrackingItem> items = new Vector<TrackingItem>();
    private SQLHandler sqlHandler;
    private Grouphug bot;

    public Tracking(ModuleHandler moduleHandler) {
        try {
            bot = Grouphug.getInstance();
            sqlHandler = SQLHandler.getSQLHandler();
            List<Object[]> rows = sqlHandler.select("select * from " + DB_NAME + ";");
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

    public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
        if(message.equals(TRIGGER_LIST) || message.equals("")) {
            listPackages();
        } else if(threadWorking) {
            // the remaining options all modify the tracked items list. so if the thread is working,
            // let the user know that we need to wait until it's done before we modify it.
            bot.sendMessage(String.format(warnThreadWorking, itemsRemaining));
        } else if(message.startsWith(TRIGGER_DEL)) {
            removePackage(message.replace(TRIGGER_DEL, "").trim());
        } else {
            // User wants to add a new item for tracking
            addPackage(message, sender);
        }
    }

    /**
     * List all packages that are currently being tracked
     */
    public void listPackages() {
        if(items.size() == 0) {
            bot.sendMessage("No packages are being tracked. What's wrong with you people?");
        } else {
            for(TrackingItem ti : items) {
                bot.sendMessage(ti.getTrackingNumber() + ": " + ti.getStatus() + " (for " + ti.getOwner() + ")");
            }
        }
    }

    /**
     * Stop tracking a package which is currently being tracked
     * @param id the id of the package to stop tracking
     */
    public void removePackage(String id) {
        for(int i=0; i<items.size(); i++) {
            if(items.get(i).getTrackingNumber().equals(id)) {
                try {
                    removeItem(items.get(i));
                    bot.sendMessage("Ok, stopped tracking package '" + id + "'.");
                } catch (SQLException e) {
                    bot.sendMessage("I have the package but failed to remove it from the SQL db " +
                            "for some reason! Please check for inconsistencies between memory and SQL.");
                    e.printStackTrace();
                }
                return;
            }
        }
        bot.sendMessage("Sorry, I'm not tracking any package with ID '" + id + "'. Try !track -ls");
    }

    /**
     * Start tracking a new package, or update the status of a package which is already being tracked
     * @param id the id of the package to add or update
     * @param sender the nick of the one tracking the package
     */
    public void addPackage(String id, String sender) {
        try {
            // first check if the package is already tracked
            for(TrackingItem ti : items) {
                if(ti.getTrackingNumber().equals(id)) {
                    checkForUpdate(ti);
                    return;
                }
            }

            // nope, let's add the new package
            TrackingItem newItem = new TrackingItem(id, sender);

            // if it's delivered, we're not going to track it further
            if(newItem.update() == DELIVERED) {
                bot.sendMessage("Your package has already been delivered. I will not track it further.");
                bot.sendMessage("Status: " + newItem.getStatus());
                return;
            }
            bot.sendMessage("Adding package '" + id + "' to tracking list.");
            List<String> params = new ArrayList<String>();
            params.add(newItem.getTrackingNumber());
            params.add(newItem.getStatus());
            params.add(newItem.getOwner());
            int sqlId = sqlHandler.insert("insert into " + DB_NAME + " (trackingnr, status, owner) VALUES ('?', '?', '?');", params);
            newItem.setId(sqlId);

            // if we came this far, no exception was thrown. if it was, the item won't get added to the list.
            items.add(newItem);
            bot.sendMessage("Status: " + newItem.getStatus());
        } catch(IOException e) {
            bot.sendMessage("Sorry, I caught an IOException. Try again later or something.");
            e.printStackTrace();
        } catch (SQLException e) {
            bot.sendMessage("Sorry, SQL failed on me. Please fix the problem and try again.");
            e.printStackTrace();
        } catch (JDOMException e) {
            bot.sendMessage("Sorry, I was unable to build a JDOM tree. Go check what's up with posten.no.");
            e.printStackTrace();
        }
    }

    /**
     * Checks an existing TrackingItem for updates. Called when the user explicitly
     * tries to !track a package which is already tracked. Therefore, something
     * should be outputted in all cases (even if there is no update).
     * @param item the item to check for updates on
     * @throws java.io.IOException if I/O fails
     * @throws java.sql.SQLException if SQL fails
     * @throws org.jdom.JDOMException if JDOM fails
     */
    public void checkForUpdate(TrackingItem item) throws JDOMException, IOException, SQLException {
        int result = item.update();
        if(result == CHANGED) {
            bot.sendMessage("New status for '" + item.getId() + "': " + item.getStatus(), true);
        } else if(result == NOT_CHANGED) {
            bot.sendMessage("No change for '" + item.getId() + "': " + item.getStatus(), true);
        } else if(result == DELIVERED) {
            bot.sendMessage("Your package has been delivered. Removing it from my list.");
            bot.sendMessage("Status: " + item.getStatus());
            removeItem(item);
            bot.sendMessage("Now tracking " + items.size() + " packages.");
        }
    }


    /**
     * This is the thread which handles waiting and polling of packages that are being tracked.
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
                            bot.sendMessage(ti.getOwner() + ": Package '" + ti.getTrackingNumber() + "' has exciting new changes!");
                            bot.sendMessage(ti.getStatus(), true);
                            break;

                        case NOT_CHANGED:
                            break;

                        case DELIVERED:
                            bot.sendMessage(ti.getOwner() + " has just picked up his/her package '" + ti.getTrackingNumber() + "':");
                            bot.sendMessage(ti.getStatus(), true);
                            itemsToRemove.add(ti);
                            bot.sendMessage("Removing this one from my list. Currently tracking " + (items.size() - itemsToRemove.size()) + " packages.");
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
                    removeItem(toRemove);
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
            } catch(JDOMException ex) {
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
                bot.sendMessage("The package tracking module has now failed 5 times in a row. " +
                        "If this continues, you might want to check the logs and your package status manually.");
            }
            try {
                Thread.sleep(POLLING_TIME * 60 * 1000);
            } catch(InterruptedException ex) {
                // just continue
            }
        }
    }

    /**
     * Remove a tracked item from memory and database
     * @param item the TrackingItem reference to remove
     * @throws SQLException if SQL fails
     */
    private void removeItem(TrackingItem item) throws SQLException {
        List<String> params = new ArrayList<String>();
        params.add(String.valueOf(item.getId()));
        sqlHandler.delete("delete from " + DB_NAME + " where id='?';", params);
        items.remove(item);
    }

    /**
     * Update the status of a tracked item
     * @param item the reference to the item of which to update status
     * @param status the items new status
     * @throws SQLException if SQL fails
     */
    private void updateStatus(TrackingItem item, String status) throws SQLException {
        item.setStatus(status);
        List<String> params = new ArrayList<String>();
        params.add(status);
        params.add(String.valueOf(item.getId()));
        sqlHandler.update("update " + DB_NAME + " set status='?' where id='?';", params);
    }


    private static class TrackingItem {

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

        public void setStatus(String status) {
            this.status = status;
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
         * @return CHANGED, NOT_CHANGED or DELIVERED according to its status change
         * @throws IOException if IO fails
         * @throws java.sql.SQLException if SQL fails
         * @throws org.jdom.JDOMException if JDOM-parsing fails
         */
        public int update() throws IOException, SQLException, JDOMException {
            Document postDocument = Web.getJDOMDocument(new URL("http://sporing.posten.no/sporing.html?q="+trackingNumber));

            XPath xpath = XPath.newInstance("//h:div[@class='sporing-sendingandkolli-latestevent-text-container']/h:div[@class='sporing-sendingandkolli-latestevent-text']/h:strong");
            xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");

            Element content = (Element)xpath.selectSingleNode(postDocument);
            if(content == null) {
                // no results
                String newStatus = "The package ID is invalid (according to the tracking service)";
                String oldStatus = getStatus();
                if(!oldStatus.equals(newStatus)) {
                    updateStatus(this, newStatus);
                    return CHANGED;
                } else {
                    return NOT_CHANGED;
                }
            }

            String message = content.getText().replaceAll("\\s+", " ").replaceAll("<br/?>", " ").replaceAll("<.*?>","").trim();

            // now find the date
            xpath = XPath.newInstance("//h:div[@class='sporing-sendingandkolli-latestevent-date']");
            xpath.addNamespace("h", "http://www.w3.org/1999/xhtml");

            String date;
            content = (Element)xpath.selectSingleNode(postDocument);
            if(content == null) {
                date = "";
            } else {
                String datePartOne = content.getText().replaceAll("\\s+", " ").replaceAll("<.*?>","").trim();
                Element dateSpan = content.getChild("span", Namespace.getNamespace("h", "http://www.w3.org/1999/xhtml"));
                String datePartTwo;
                if(dateSpan == null) {
                    datePartTwo = "";
                } else {
                    datePartTwo = dateSpan.getText();
                }
                date = datePartOne + " " + datePartTwo;
            }

            String newStatus = message + " " + date;

            String oldStatus = getStatus();
            if(newStatus.startsWith("Sendingen er utlevert")) {
                updateStatus(this, newStatus);
                return DELIVERED;
            } else if(!oldStatus.equals(newStatus)) {
                updateStatus(this, newStatus);
                return CHANGED;
            } else {
                return NOT_CHANGED;
            }
        }
    }
}
