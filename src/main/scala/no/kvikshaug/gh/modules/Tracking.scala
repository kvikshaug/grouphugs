package no.kvikshaug.gh.modules

import no.kvikshaug.gh.{Grouphug, ModuleHandler, Config}
import no.kvikshaug.gh.exceptions.SQLUnavailableException;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.SQLHandler;
import org.jdom.JDOMException;

import java.io.{IOException, FileNotFoundException}
import java.sql.SQLException;

import scalaj.collection.Imports._
import scala.collection.immutable.List
import scala.collection.JavaConversions
import scala.actors.Actor
import scala.actors.Actor._

class Tracking(moduleHandler: ModuleHandler) extends Actor with TriggerListener with Runnable {

    var sqlHandler: SQLHandler = null
    val bot = Grouphug.getInstance
    var items: List[Package] = Nil
    val dbName = "tracking"
    val pollingTime = 30 // minutes
    new Thread(this).start
    start

    val TRIGGER = "track";
    val TRIGGER_FORCE = "trackpoll";
    val TRIGGER_LIST = "trackls";
    val TRIGGER_DEL = "trackrm";
    val TRIGGER_HELP = "track";

    moduleHandler.addTriggerListener(TRIGGER, this)
    moduleHandler.addTriggerListener(TRIGGER_FORCE, this)
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
      ).asScala.map { row => Package(
          row(0).asInstanceOf[String], row(1).asInstanceOf[String],
          row(2).asInstanceOf[String], row(3).asInstanceOf[String],
          row(4).asInstanceOf[String])
      }.toList

      println("Package tracking module loaded.")
    } catch {
      case e: SQLUnavailableException => println("Package tracking module unable to load because SQL is unavailable.")
      case e: SQLException => println("Package tracking module unable to load because it was unable to load existing package list from SQL!")
        e.printStackTrace();
    }

    def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = trigger match {
      case TRIGGER_FORCE => bot.sendMessageChannel(channel, "Forcing updates."); this ! items
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
      items.filter(_.channel == channel).foreach {
        item => bot.sendMessageChannel(channel, item.id + " for " + item.owner + ": " + item.status)
      }
    }

    def removePackage(channel: String, id: String) {
      try {
        if(!(items.exists(_ == id))) {
          bot.sendMessageChannel(channel, "I'm not tracking any package with ID " + id + ". Try !" + TRIGGER_LIST)
          return
        }
        items = items.filterNot(_ == id)
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

      if(items.exists(_ == id)) {
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
        case e =>
          bot.sendMessageChannel(channel, "Oh no, I caught some horrible exception! Please check my logs. SQL/memory may or may not be synchronized.")
          e.printStackTrace
      }
    }

  var failCount = 0
  def run {
    // wait until the bot has connected and is ready
    Thread.sleep(30 * 1000)
    while(true) {
      this ! items
      if(failCount >= 5) {
        failCount = 0
        for(channel <- Config.channels) {
          bot.sendMessageChannel(channel, "The package tracking module has now failed 5 times in a row. " +
            "If this continues, you might want to check the logs and your package status manually.")
        }
      }
      Thread.sleep(pollingTime * 60 * 1000)
    }
  }

  def act {
    loop {
      react {
        case itemList: List[Package] => itemList foreach { i =>
          try {
            val status = TrackingXMLParser.track(i)
            if(status._1) {
              i.statusCode match {
                case "DELIVERED" =>
                  bot.sendMessageChannel(i.channel, i.owner + " has just picked up " + i.id + ":")
                  bot.sendMessageChannel(i.channel, i.status)
                  items = items.filterNot(_ == i)
                  sqlHandler.delete("delete from " + dbName + " where trackingId=?;", List(i.id).asJava)
                  bot.sendMessageChannel(i.channel, "Removing this one from my list. Now tracking " + items.size + " packages.")
                case "RETURNED" =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Package " + i.id + " has been returned to sender.")
                  bot.sendMessageChannel(i.channel, i.status)
                  items = items.filterNot(_ == i)
                  sqlHandler.delete("delete from " + dbName + " where trackingId=?;", List(i.id).asJava)
                  bot.sendMessageChannel(i.channel, "Removing this one from my list. Now tracking " + items.size + " packages.")
                case "NOTIFICATION_SENT" =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Notification for package " + i.id + " has been sent!")
                  bot.sendMessageChannel(i.channel, i.status)
                  if(status._2 > 1) {
                    bot.sendMessageChannel(i.channel, "Note: This package has >1 items, you might wanna track the other ones manually.")
                  }
                case "READY_FOR_PICKUP" =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Package " + i.id + " is ready for pickup!")
                  bot.sendMessageChannel(i.channel, i.status)
                  if(status._2 > 1) {
                    bot.sendMessageChannel(i.channel, "Note: This package has >1 items, you might wanna track the other ones manually.")
                  }
                case "IN_TRANSIT" =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Package " + i.id + " has changed:")
                  bot.sendMessageChannel(i.channel, i.status)
                  if(status._2 > 1) {
                    bot.sendMessageChannel(i.channel, "Note: This package has >1 items, you might wanna track the other ones manually.")
                  }
                case "CUSTOMS" =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Package " + i.id + " due for inspection!")
                  bot.sendMessageChannel(i.channel, i.status)
                  if(status._2 > 1) {
                    bot.sendMessageChannel(i.channel, "Note: This package has >1 items, you might wanna track the other ones manually.")
                  }
                case "NO_PACKAGES" =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Package " + i.id + " has suddenly lost its contents! You might want to check it manually.")
                  bot.sendMessageChannel(i.channel, i.status)
                case x =>
                  bot.sendMessageChannel(i.channel, i.owner + ": Package " + i.id + " has changed to '" + x + "', which I don't recognize!")
                  bot.sendMessageChannel(i.channel, i.status)
                  if(status._2 > 1) {
                    bot.sendMessageChannel(i.channel, "Note: This package has >1 items, you might wanna track the other ones manually.")
                  }
              }
            }
          } catch {
            case e => println("Tracking poller just failed: "); e.printStackTrace; failCount = failCount + 1
          }
        }
      }
    }
  }
}
