package no.kvikshaug.gh.modules;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;
import org.jdom.JDOMException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Tracking implements TriggerListener, Runnable {

    private static final String TRIGGER = "track";
    private static final String TRIGGER_HELP = "track";
    private static final String TRIGGER_LIST = "-ls";
    private static final String TRIGGER_DEL = "-rm";
    private static final String TRIGGER_DETAILS = "-details";
    private static final String TRIGGER_HISTORY = "-history";

    // tracking is the main tracking id, packages are each package in the tracking id, trackingpackages combines the two
    public static final String DB_TRACKING_NAME = "tracking";
    public static final String DB_PACKAGES_NAME = "package";
    public static final String DB_TRACKINGPACKAGES_NAME = "trackingpackage";
    private static final int POLLING_TIME = 30; // minutes

    public static final int NOT_CHANGED = 0;
    public static final int CHANGED = 1;

    public static final int STATUS_NO_PACKAGES = -1;
    public static final int STATUS_IN_TRANSIT = 0;
    public static final int STATUS_READY_FOR_PICKUP = 1;
    public static final int STATUS_NOTIFICATION_SENT = 4;
    public static final int STATUS_RETURNED = 2;
    public static final int STATUS_DELIVERED = 3;

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

            // load all trackingitems and packages from db
            List<Object[]> trackingRows = sqlHandler.select("select * from " + DB_TRACKING_NAME + ";");
            for(Object[] tRow : trackingRows) {
                List<TrackingItemPackage> packages = new ArrayList<TrackingItemPackage>();
                List<Object[]> packageList = sqlHandler.select(
                        "select packageId from " + DB_TRACKINGPACKAGES_NAME + " where trackingId='" + tRow[0] + "';");
                for(Object[] tpRow : packageList) {
                    Object[] packageRow = sqlHandler.selectSingle(
                            "select * from " + DB_PACKAGES_NAME + " where id='" + tpRow[0] + "';");
                    packages.add(new TrackingItemPackage((Integer)packageRow[0], (String)packageRow[1],
                            (String)packageRow[2], (String)packageRow[3], (String)packageRow[4]));
                }
                items.add(new TrackingItem((Integer)tRow[0], (String)tRow[1], (String)tRow[2], packages, (String)tRow[3]));
            }

            moduleHandler.addTriggerListener(TRIGGER, this);
            moduleHandler.registerHelp(TRIGGER_HELP, "Posten.no package tracking. I will keep track of the package by " +
                    "polling and let you know when anything changes.\n" +
                    "  Start tracking:  !track <package id / kollinr>\n" +
                    "  Stop tracking:   !track " + TRIGGER_DEL + " <package id / kollinr>\n" +
                    "  Show history:    !track " + TRIGGER_HISTORY + " <package id / kollinr>\n" +
                    "  Show details:    !track " + TRIGGER_DETAILS + " <package id / kollinr>\n" +
                    "  List all:        !track " + TRIGGER_LIST + "\n" +
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
            listPackages(channel);
        } else if(message.contains(TRIGGER_HISTORY)) {
            printHistory(channel, message.replace(TRIGGER_HISTORY, "").trim());
        } else if(message.contains(TRIGGER_DETAILS)) {
            printDetails(channel, message.replace(TRIGGER_DETAILS, "").trim());
        } else if(threadWorking) {
            // the remaining options all modify the tracked items list. so if the thread is working,
            // let the user know that we need to wait until it's done before we modify it.
            bot.sendMessageChannel(channel, String.format(warnThreadWorking, itemsRemaining));
        } else if(message.startsWith(TRIGGER_DEL)) {
            removePackage(channel, message.replace(TRIGGER_DEL, "").trim());
        } else {
            // User wants to add a new item for tracking
            addPackage(channel, message, sender);
        }
    }

    /**
     * List all packages that are currently being tracked
     */
    public void listPackages(String channel) {
        if(items.size() == 0) {
            bot.sendMessageChannel(channel, "No packages are being tracked. What's going on, are you all bankrupt?");
        } else {
            for(TrackingItem ti : items) {
            	if (ti.channel().equals(channel)){
            		bot.sendMessageChannel(ti.channel(), ti.trackingId() + " for " + ti.owner() + ": " + ti.oneLineStatus());
            	}
            }
        }
    }

    /**
     * Stop tracking a package which is currently being tracked
     * @param id the id of the package to stop tracking
     */
    public void removePackage(String channel, String id) {
        for(int i=0; i<items.size(); i++) {
            if(items.get(i).trackingId().equals(id) && items.get(i).channel().equals(channel)) {
                try {
                    removeItem(items.get(i));
                    bot.sendMessageChannel(items.get(i).channel(), "Ok, stopped tracking package " + id + ".");
                } catch (SQLException e) {
                    bot.sendMessageChannel(items.get(i).channel(), "I have the package but failed to remove it from the SQL db " +
                            "for some reason! Please check for inconsistencies between memory and SQL.");
                    e.printStackTrace();
                }
                return;
            }
        }
        bot.sendMessageChannel(channel, "Sorry, I'm not tracking any package with ID '" + id + "'. Try !track -ls");
    }

    /**
     * Start tracking a new package, or update the status of a package which is already being tracked
     * @param trackingId the trackingId of the package to add or update
     * @param sender the nick of the one tracking the package
     */
    public void addPackage(String channel, String trackingId, String sender) {
        try {
            // first check if the package is already tracked
            for(TrackingItem ti : items) {
                if(ti.trackingId().equals(trackingId) && ti.channel().equals(channel)) {
                    checkForUpdate(channel, ti);
                    return;
                }
            }

            // nope. let's add the new package, if it's not bogus
            if(trackingId.contains(" ")) {
                bot.sendMessageChannel(channel, "Add a tracking id which contains spaces? I THINK NOT. Try '!help track'.");
                return;

            } else if(trackingId.contains("-")) {
                bot.sendMessageChannel(channel, "When did posten start to use hyphens in their tracking id? Try '!help track'.");
                return;
            }
            List<String> params = new ArrayList<String>();
            params.add(trackingId);
            params.add(sender);
            params.add(channel);
            int dbId = sqlHandler.insert(
                    "insert into " + DB_TRACKING_NAME + " (trackingid, owner, channel) VALUES (?, ?, ?);", params);
            TrackingItem newItem = new TrackingItem(dbId, trackingId, sender, new ArrayList<TrackingItemPackage>(), channel);
            items.add(newItem);

            // now check its status
            TrackingXMLParser.track(newItem);
            // if it's delivered, we're not going to track it further
            if(newItem.statusCode() == STATUS_DELIVERED) {
                bot.sendMessageChannel(newItem.channel(), "Your package has already been delivered. I will not track it further.");
                bot.sendMessageChannel(newItem.channel(), newItem.totalStatus());
                if(dbId != -1) {
                    params.clear();
                    params.add(String.valueOf(dbId));
                    sqlHandler.delete("delete from " + DB_TRACKING_NAME + " where id =?;", params);
                }
                return;
            }

            bot.sendMessageChannel(channel, "Adding package " + trackingId + " to tracking list.");
            TrackingItemInfo info = TrackingXMLParser.infoFor(newItem);
            bot.sendMessageChannel(channel, info.totalWeight() + ", " + info.totalVolume() + ", " + info.packageInfo().size() +
                    " package(s)");
            int i=0;
            for(TrackingItemPackage p : newItem.packages()) {
                if(newItem.packages().size() > 1) {
                    bot.sendMessageChannel(newItem.channel(), "Package " + (++i) + ": " +
                            info.packageInfo().get(0).weight() + ", " +
                            info.packageInfo().get(0).width() + "x" +
                            info.packageInfo().get(0).length() + "x" +
                            info.packageInfo().get(0).height() + " cm (" +
                            info.packageInfo().get(0).productName() + ", " +
                            info.packageInfo().get(0).productCode() + ", " +
                            info.packageInfo().get(0).brand() + "): " +
                            p.description() + ", " + p.dateTime());
                } else {
                    bot.sendMessageChannel(newItem.channel(), info.packageInfo().get(0).width() + "x" +
                            info.packageInfo().get(0).length() + "x" +
                            info.packageInfo().get(0).height() + " cm (" +
                            info.packageInfo().get(0).productName() + ", " +
                            info.packageInfo().get(0).productCode() + ", " +
                            info.packageInfo().get(0).brand() + "): " +
                            p.description() + ", " + p.dateTime());
                }
            }
        } catch(FileNotFoundException e) {
            bot.sendMessageChannel(channel, "Adding package " + trackingId + " to tracking list.");
            bot.sendMessageChannel(channel, "Posten's XML API gave a 404 on the tracking ID though. " +
                    "It might appear in a short while; I'll keep it and try again soon.");
        } catch(IOException e) {
            bot.sendMessageChannel(channel, "Sorry, I caught an IOException. I'll keep the item and try again soon.");
            e.printStackTrace();
        } catch (SQLException e) {
            bot.sendMessageChannel(channel, "Sorry, SQL is being a bitch. Please check my DB for inconsistencies.");
            e.printStackTrace();
        } catch (JDOMException e) {
            bot.sendMessageChannel(channel, "Sorry, I was unable to build a JDOM tree. Go check what's up with posten.no.");
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
    public void checkForUpdate(String channel, TrackingItem item) throws JDOMException, IOException, SQLException {
    	if (!item.channel().equals(channel)){
    		return;
    	}
        int result = TrackingXMLParser.track(item);
        if(result == CHANGED) {
            if(item.statusCode() == STATUS_DELIVERED && item.channel().equals(channel)) {
                bot.sendMessageChannel(channel, item.trackingId() + " has been delivered. Removing it from my list.");
                bot.sendMessageChannel(channel, item.totalStatus(), true);
                removeItem(item);
                bot.sendMessageChannel(channel, "Now tracking " + items.size() + " packages.");
            } else if(item.statusCode() == STATUS_RETURNED) {
                bot.sendMessageChannel(channel, item.trackingId() + " has been returned to sender. Removing it from my list.");
                bot.sendMessageChannel(channel, item.totalStatus(), true);
                removeItem(item);
                bot.sendMessageChannel(channel,"Now tracking " + items.size() + " packages.");
            } else {
                bot.sendMessageChannel(channel, "New status for " + item.trackingId() + ":");
                bot.sendMessageChannel(channel, item.totalStatus(), true);
            }
        } else if(result == NOT_CHANGED) {
            bot.sendMessageChannel(channel, "No change for " + item.trackingId() + ":");
            bot.sendMessageChannel(channel, item.totalStatus(), true);
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
                    try {
                        if(TrackingXMLParser.track(ti) == CHANGED) {
                            int statusCode = ti.statusCode();
                            if(statusCode == STATUS_DELIVERED) {
                                bot.sendMessageChannel(ti.channel(), ti.owner() + " has just picked up his/her package " + ti.trackingId() + ":");
                                bot.sendMessageChannel(ti.channel(), ti.totalStatus(), true);
                                String channel = ti.channel();
                                itemsToRemove.add(ti);
                                bot.sendMessageChannel(channel, "Removing this one from my list. Now tracking " +
                                        (items.size() - itemsToRemove.size()) + " packages.");
                            } else if(statusCode == STATUS_READY_FOR_PICKUP) {
                                bot.sendMessageChannel(ti.channel(), ti.owner() + ": Package " + ti.trackingId() + " is ready for pickup!");
                                bot.sendMessageChannel(ti.channel(), ti.totalStatus(), true);
                            } else if(statusCode == STATUS_RETURNED) {
                                bot.sendMessageChannel(ti.channel(), ti.owner() + ": Package " + ti.trackingId() + " has been returned to sender.");
                                bot.sendMessageChannel(ti.channel(), ti.totalStatus(), true);
                                String channel = ti.channel();
                                itemsToRemove.add(ti);
                                bot.sendMessageChannel(channel, "Removing this one from my list. Now tracking " +
                                        (items.size() - itemsToRemove.size()) + " packages.");
                            } else if(statusCode == STATUS_IN_TRANSIT) {
                                bot.sendMessageChannel(ti.channel(), ti.owner() + ": Package " + ti.trackingId() + " has changed:");
                                bot.sendMessageChannel(ti.channel(), ti.totalStatus(), true);
                            } else if(statusCode == STATUS_NO_PACKAGES) {
                                bot.sendMessageChannel(ti.channel(), ti.owner() + ": Package " + ti.trackingId() +
                                        " has.. changed, but has no packages (kolli)? Wtf?");
                                bot.sendMessageChannel(ti.channel(), ti.totalStatus(), true);
                            }
                            break;
                        }
                    } catch(FileNotFoundException ignored) {
                        // ignored; this is thrown when we query for a non-existing tracking ID
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
            } catch(Exception ex) {
                fails++;
                System.err.println("Tracking module thread caught an exception.");
                System.err.println("I will pretend like nothing happened and try again soon, " +
                        "let's hope it is recoverable.");
                ex.printStackTrace();
            }
            if(fails > 5) {
                fails = 0;
                for (String channel: bot.CHANNELS){
                	bot.sendMessageChannel(channel, "The package tracking module has now failed 5 times in a row. " +
                	"If this continues, you might want to check the logs and your package status manually.");
                	
                }
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
        for(TrackingItemPackage p : item.packages()) {
            params.clear();
            params.add(String.valueOf(item.dbId()));
            params.add(String.valueOf(p.dbId()));
            sqlHandler.delete("delete from " + DB_TRACKINGPACKAGES_NAME + " where trackingId=? and packageId=?;",
                    params);
            params.clear();
            params.add(String.valueOf(p.dbId()));
            sqlHandler.delete("delete from " + DB_PACKAGES_NAME + " where id=?;", params);
        }
        params.clear();
        params.add(String.valueOf(item.dbId()));
        sqlHandler.delete("delete from " + DB_TRACKING_NAME + " where id=?;", params);
        items.remove(item);
    }

    /**
     * Print the event history of a tracking item
     * @param trackingId the tracking id of the tracking item
     */
    private void printHistory(String channel, String trackingId) {
        boolean found = false;
        for(TrackingItem item : items) {
            if(item.trackingId().equals(trackingId) && item.channel().equals(channel)) {
                found = true;
                for(TrackingItemPackage p : item.packages()) {
                    List<TrackingItemEvent> events = TrackingXMLParser.historyFor(item, p.packageId());
                    if(events == null) {
                        bot.sendMessageChannel(item.channel(), "The tracking ID still isn't recognized by posten's XML API.");
                        return;
                    }
                    if(item.packages().size() > 1) {
                        bot.sendMessageChannel(item.channel(), "Event history for package " + p.packageId() + ":");
                    } else {
                        bot.sendMessageChannel(item.channel(), "Event history for " + trackingId + ":");
                    }
                    if(events.size() == 0) {
                        bot.sendMessageChannel(item.channel(), "Uhm, I didn't find any events in the XML. I suspect the package ID " +
                                "'" + p.packageId() + "' was wrong.");
                    }
                    for(TrackingItemEvent event : events) {
                        bot.sendMessageChannel(item.channel(), event.isoDateTime() + ": " + event.desc() + " (" + event.status() + "), " +
                                event.postalCode() + " " + event.city() + ". Consignment event: " +
                                event.consignmentEvent() + ". UnitId: " + event.unitId() + 
                                (event.signature().equals("") ? "" : ", signature: " + event.signature()));
                    }
                }
            }
        }
        if(!found) {
            bot.sendMessageChannel(channel, "I can't recall having the tracking ID '" + trackingId + "' in my list. Try !track -ls");
        }
    }

    /**
     * Print all available details of a tracking item upon user request
     * @param trackingId the tracking id of the tracking item
     */
    private void printDetails(String channel, String trackingId) {
        boolean found = false;
        for(TrackingItem item : items) {
            if(item.trackingId().equals(trackingId) && item.channel().equals(channel)) {
                found = true;
                TrackingItemInfo info = TrackingXMLParser.infoFor(item);
                if(info == null) {
                    bot.sendMessageChannel(item.channel(), "The tracking ID still isn't recognized by posten's XML API.");
                    return;
                }
                bot.sendMessageChannel(channel, "Details for " + trackingId + ":");
                actuallyPrintDetails(item.channel(), info);
            }
        }
        if(!found) {
            bot.sendMessageChannel(channel, "I can't recall having the tracking ID '" + trackingId + "' in my list. Try !track -ls");
        }
    }

    private void actuallyPrintDetails(String channel, TrackingItemInfo info) {
        bot.sendMessageChannel(channel, info.totalWeight() + ", " + info.totalVolume() + ", " + info.packageInfo().size() + " packages");
        int i = 0;
        for(TrackingItemPackageInfo pInfo : info.packageInfo()) {
            // TODO assuming unit code 'cm', should use provided unitCode attribute
            if(info.packageInfo().size() == 1) {
                bot.sendMessageChannel(channel, pInfo.width() + "x" + pInfo.length() + "x" +
                        pInfo.height() + " cm (" + pInfo.productName() + ", " +
                        pInfo.productCode() + ", " + pInfo.brand() + ")");
            } else {
                bot.sendMessageChannel(channel, "Package " + (++i) + ": " + pInfo.weight() + ", " +
                        pInfo.width() + "x" + pInfo.length() + "x" +
                        pInfo.height() + " cm (" + pInfo.productName() + ", " +
                        pInfo.productCode() + ", " + pInfo.brand() + ")");
            }
        }
    }
}
