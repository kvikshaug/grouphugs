package no.kvikshaug.gh.modules

import no.kvikshaug.gh.{Grouphug, ModuleHandler, Config}
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.{SQL, Web};

import no.kvikshaug.scatsd.client.ScatsD;
import no.kvikshaug.worm.Worm

import org.jdom.JDOMException;

import java.io.{IOException, FileNotFoundException}
import java.util.{Timer => JavaTimer, TimerTask}
import java.net.URL

import scala.collection.JavaConverters._
import scala.actors.Actor
import scala.actors.Actor._
import scala.xml._

case class Package(trackingId: String, owner: String, var status: String,
                   var statusCode: String, channel: String) extends Worm {
  override def equals(other: Any) = other match {
    case that: Package => trackingId == that.trackingId
    case thatId: String => trackingId == thatId
    case _ => false
  }
}

class Tracking(moduleHandler: ModuleHandler) extends TimerTask with TriggerListener {

    val bot = Grouphug.getInstance
    var items: List[Package] = Nil
    val pollingTime = 30 // minutes
    new JavaTimer().schedule(this, 30 * 1000, pollingTime * 60 * 1000)

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

    if(SQL.isAvailable) {
      // load all trackingitems and packages from db
      items = Worm.get[Package]
    } else {
      println("Warning: Tracking module will start, but will not be able to load existing packages " +
        "or store new ones to be loaded after reboot, because SQL is unavailable.")
    }

    def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = trigger match {
      case TRIGGER_FORCE => bot.msg(channel, "Forcing updates."); run
      case TRIGGER_LIST  => listPackages(channel)
      case TRIGGER_DEL   => removePackage(channel, message)
      case TRIGGER => message match {
        case "" => listPackages(channel)
        case _ =>  addPackage(channel, message, sender)
      }
    }

    def listPackages(channel: String) {
      if(items.size == 0) {
        bot.msg(channel, "No packages are being tracked. What's going on, are you all bankrupt?")
      }
      items.filter(_.channel == channel).foreach {
        item => bot.msg(channel, item.trackingId + " for " + item.owner + ": " + item.status)
      }
    }

    def removePackage(channel: String, id: String) {
      val item = items.find(_ == id)
      if(item.isEmpty) {
        bot.msg(channel, "I'm not tracking any package with ID " + id + ". Try !" + TRIGGER_LIST)
      } else {
        item.get.delete
        items = items.filterNot(_ == item.get)
        bot.msg(channel, "Ok, stopped tracking package " + id + ".")
        ScatsD.retain(String.format("gh.bot.modules.tracking.%s.packages", channel), items.size)
      }
    }


    def addPackage(channel: String, id: String, sender: String) {
      // the following two checks are for temporary backwards compatibility
      if(id.startsWith("-rm")) {
        bot.msg(channel, sender + ": Please use !" + TRIGGER_DEL + " in the future, this trigger is deprecated.")
        removePackage(channel, id.replaceAll("-rm", "").trim)
        return
      } else if(id.startsWith("-ls")) {
        bot.msg(channel, sender + ": Please use !" + TRIGGER_LIST + " in the future, this trigger is deprecated.")
        listPackages(channel)
        return
      }

      if(!(id matches "[a-zA-Z0-9]+")) {
        bot.msg(channel, "The ID " + id + " doesn't match [a-zA-Z0-9]+.")
        return
      }

      if(items.exists(_ == id)) {
        bot.msg(channel, "I'm already tracking " + id + ". Use !" + TRIGGER_FORCE + " to force an update.")
        return
      }

      // track and add it
      try {
        val newItem = Package(id, sender, "", "", channel)
        TrackingXMLParser.track(newItem)
        if(newItem.statusCode == "DELIVERED") {
            bot.msg(channel, "Your package has already been delivered. I will not track it further.")
            bot.msg(channel, newItem.status)
            return
        }
        newItem.insert
        items = newItem :: items

        bot.msg(channel, "Adding package " + id + " to tracking list.")
        bot.msg(channel, "Status: " + newItem.status)
      } catch {
        case e =>
          bot.msg(channel, "Oh no, I caught some horrible exception! Please check my logs. SQL/memory may or may not be synchronized.")
          e.printStackTrace
      }
      ScatsD.retain(String.format("gh.bot.modules.tracking.%s.packages", channel), items.size)
    }

  var failCount = 0
  def run {
    items foreach { i =>
      try {
        val (changed, packageCount) = TrackingXMLParser.track(i)
        i.update
        if(changed) {
          i.statusCode match {
            case "DELIVERED" =>
              bot.msg(i.channel, i.owner + " has just picked up " + i.trackingId + ":")
              bot.msg(i.channel, i.status)
              items = items.filterNot(_ == i)
              i.delete
              bot.msg(i.channel, "Removing this one from my list. Now tracking " + items.filter(_.channel == i.channel).size + " packages.")
            case "RETURNED" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " has been returned to sender.")
              bot.msg(i.channel, i.status)
              items = items.filterNot(_ == i)
              i.delete
              bot.msg(i.channel, "Removing this one from my list. Now tracking " + items.filter(_.channel == i.channel).size + " packages.")
            case "RETURN" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " is being returned to sender!")
              bot.msg(i.channel, i.status)
            case "PRE_NOTIFIED" =>
              bot.msg(i.channel, i.owner + ": Posten now knows about your package.")
              bot.msg(i.channel, i.status)
            case "INTERNATIONAL" =>
              bot.msg(i.channel, i.owner + ": Your package is still far away.")
              bot.msg(i.channel, i.status)
            case "NOTIFICATION_SENT" =>
              bot.msg(i.channel, i.owner + ": Notification for package " + i.trackingId + " has been sent!")
              bot.msg(i.channel, i.status)
            case "TRANSPORT_TO_RECIPIENT" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " is on its way to you right now!")
              bot.msg(i.channel, i.status)
            case "READY_FOR_PICKUP" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " is ready for pickup!")
              bot.msg(i.channel, i.status)
            case "IN_TRANSIT" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " has changed:")
              bot.msg(i.channel, i.status)
            case "CUSTOMS" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " is due for inspection!")
              bot.msg(i.channel, i.status)
            case "NO_PACKAGES" =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " has suddenly lost its contents! You might want to check it manually.")
              bot.msg(i.channel, i.status)
            case x =>
              bot.msg(i.channel, i.owner + ": Package " + i.trackingId + " has changed to '" + x + "', which I don't recognize!")
              bot.msg(i.channel, i.status)
          }
          if(packageCount > 1) {
            bot.msg(i.channel, "Note: This package has >1 items.")
          }
        ScatsD.retain(String.format("gh.bot.modules.tracking.%s.packages", i.channel), items.size)
        }
      } catch {
        case e => println("Tracking poller just failed: "); e.printStackTrace; failCount = failCount + 1
      }
    }
    if(failCount >= 5) {
      failCount = 0
      for(channel <- Config.channels.asScala) {
        bot.msg(channel, "The package tracking module has now failed 5 times in a row. " +
          "If this continues, you might want to check the logs and your package status manually.")
      }
    }
  }
}

object TrackingXMLParser {

  @throws(classOf[IOException])
  // returns a tuple of ("Has the package changed?", "The number of packages in this item")
  def track(item: Package): Tuple2[Boolean, Int] = {
    var newStatus = ""
    var newStatusCode = ""
    var packageCount = 0

    try {
      // load the xml
      val root = XML.load(Web.prepareEncodedBufferedReader(
        new URL("http://sporing.posten.no/sporing.xml?q=" + item.trackingId)))

      val e = ((root \\ "PackageSet" \ "Package")(0) \ "EventSet" \ "Event")(0)
      newStatus = (e \ "Description").text.replaceAll("<.*?>", "").trim + " " +
                  (e \ "PostalCode").text.replaceAll("<.*?>", "").trim  + " " +
                  (e \ "City").text.replaceAll("<.*?>", "").trim + " " +
                  (e \ "OccuredAtDisplayTime").text.replaceAll("<.*?>", "").trim + " " +
                  (e \ "OccuredAtDisplayDate").text.replaceAll("<.*?>", "").trim
      newStatusCode = (e \ "Status").text.replaceAll("<.*?>", "").trim
      packageCount = (root \\ "PackageSet" \ "Package").size
    } catch {
      // thrown by the API when the item ID isn't found at posten (404)
      case e: FileNotFoundException =>
        if(item.statusCode == "") {
          // if this is a new package, it has no status code
          newStatus = "Not found (but it might appear soon if it was recently added)"
          newStatusCode = "NO_PACKAGES"
        } else {
          // this package already has a status.
          // posten sometimes gives a temporary incorrect 404 value, so just ignore it
          return (false, 0)
        }
    }

    var changed = false
    if(newStatus != item.status || newStatusCode != item.statusCode) {
      changed = true
    }
    item.status = newStatus
    item.statusCode = newStatusCode

    (changed, packageCount)
  }
}
