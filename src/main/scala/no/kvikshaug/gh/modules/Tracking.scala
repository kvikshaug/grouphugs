package no.kvikshaug.gh.modules

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;
import org.jdom.JDOMException;

import java.io.{IOException, FileNotFoundException}
import java.sql.SQLException;

import scalaj.collection.Imports._
import scala.collection.immutable.List
import scala.collection.JavaConversions

        // TODO bot.sendMessageChannel(channel, "Note: This package contains >1 items.")
class Tracking(moduleHandler: ModuleHandler) extends TriggerListener /*with Runnable*/ {

    var sqlHandler: SQLHandler = null
    val bot = Grouphug.getInstance
    var items: List[Package] = Nil
    val dbName = "tracking"
    val pollTime = 30 // minutes

    val TRIGGER = "track";
    val TRIGGER_FORCE = "trackpoll";
    val TRIGGER_LIST = "trackls";
    val TRIGGER_DEL = "trackrm";
    val TRIGGER_HELP = "track";

    moduleHandler.addTriggerListener(TRIGGER, this)
    moduleHandler.addTriggerListener(TRIGGER_LIST, this)
    moduleHandler.addTriggerListener(TRIGGER_DEL, this)
    moduleHandler.registerHelp(TRIGGER_HELP, "Posten.no package tracking. I will keep track of the package by " +
      "polling and let you know when anything changes.\n" +
      "  Start tracking:  !" + TRIGGER + " <package id / kollinr>\n" +
      "  Stop tracking:   !" + TRIGGER_DEL + " <package id / kollinr>\n" +
      "  List all:        !" + TRIGGER_LIST + "\n" +
      "  Force an update: !" + TRIGGER_FORCE)

    // load all trackingitems and packages from db
    try {
      sqlHandler = SQLHandler.getSQLHandler
      items = sqlHandler.select(
        "select trackingId, status, statusCode, owner, channel from " + dbName + ";"
      ).asScala.map { row =>
        Package(
          row(0).asInstanceOf[String],
          row(1).asInstanceOf[String],
          row(2).asInstanceOf[String],
          row(3).asInstanceOf[String],
          row(4).asInstanceOf[String])
      }.toList

      // new Thread(this).start();
      println("Package tracking module loaded.")
    } catch {
      case e: SQLUnavailableException => println("Package tracking module unable to load because SQL is unavailable.")
      case e: SQLException => println("Package tracking module unable to load because it was unable to load existing package list from SQL!")
        e.printStackTrace();
    }

    def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = trigger match {
      case TRIGGER_FORCE => // TODO send actor message or something?
      case TRIGGER_LIST  => listPackages(channel)
      case TRIGGER_DEL   => removePackage(channel, message)
      case TRIGGER => message match {
        case "" => listPackages(channel)
        case _ =>  addPackage(channel, message, sender)
      }
    }

    def listPackages(channel: String) {
      if(items.size == 0) {
        bot.sendMessageChannel(channel, "No packages are being tracked. What's going on, are you all bankrupt?")
      }
      items.filter(_.channel == channel).foreach { item =>
        bot.sendMessageChannel(channel, item.id + " for " + item.owner + ": " + item.status)
      }
    }

    def removePackage(channel: String, id: String) {
      try {
        if(items.filter(_.id == id).size == 0) {
          bot.sendMessageChannel(channel, "I'm not tracking any package with ID " + id + ". Try !" + TRIGGER_LIST)
          return
        }
        items = items.filterNot(_.id == id)
        sqlHandler.delete("delete from " + dbName + " where trackingId=?;", List(id).asJava)
        bot.sendMessageChannel(channel, "Ok, stopped tracking package " + id + ".")
      } catch {
        case e: SQLException =>
          bot.sendMessageChannel(channel, "Removed it from memory but not from SQL. Check my logs for more info.")
          e.printStackTrace
      }
    }


    def addPackage(channel: String, id: String, sender: String) {
      // the following two checks are for temporary backwards compatibility
      if(id.startsWith("-rm")) {
        bot.sendMessageChannel(channel, sender + ": Please use !" + TRIGGER_DEL + " in the future, this trigger is deprecated.")
        removePackage(channel, id.replaceAll("-rm", "").trim)
        return
      } else if(id.startsWith("-ls")) {
        bot.sendMessageChannel(channel, sender + ": Please use !" + TRIGGER_LIST + " in the future, this trigger is deprecated.")
        listPackages(channel)
        return
      }

      if(!(id matches "[a-zA-Z0-9]+")) {
        bot.sendMessageChannel(channel, "The ID " + id + " doesn't match [a-zA-Z0-9]+.")
        return
      }

      if(items.filter(_.id == id).size != 0) {
        bot.sendMessageChannel(channel, "I'm already tracking " + id + ". Use !" + TRIGGER_FORCE + " to force an update.")
        return
      }

      // track and add it
      try {
        val newItem = Package(id, "", "", sender, channel)
        TrackingXMLParser.track(newItem)
        if(newItem.statusCode == "DELIVERED") {
            bot.sendMessageChannel(channel, "Your package has already been delivered. I will not track it further.")
            bot.sendMessageChannel(channel, newItem.status)
            return
        }
        sqlHandler.insert("insert into " + dbName + " (trackingId, status, statusCode, owner, channel) VALUES (?, ?, ?, ?, ?);", List(id, newItem.status, newItem.statusCode, sender, channel).asJava)
        items = newItem :: items

        bot.sendMessageChannel(channel, "Adding package " + id + " to tracking list.")
        bot.sendMessageChannel(channel, "Status: " + newItem.status)
      } catch {
        case e: FileNotFoundException =>
            bot.sendMessageChannel(channel, "Adding package " + id + " to tracking list.");
            bot.sendMessageChannel(channel, "Status: Not found (but it might appear soon if it was recently added)")
        case e =>
          bot.sendMessageChannel(channel, "Oh no, I caught some horrible exception! Please check my logs. SQL/memory may or may not be synchronized.")
          e.printStackTrace
      }
    }

    /**
     * This is the thread which handles waiting and polling of packages that are being tracked.
     */
    /*
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
    */
}
