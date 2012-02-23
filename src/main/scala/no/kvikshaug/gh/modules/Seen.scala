package no.kvikshaug.gh.modules

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.listeners.{TriggerListener, MessageListener,
  NickChangeListener, JoinListener, QuitListener, PartListener}
import no.kvikshaug.gh.util.SQL

import no.kvikshaug.worm.Worm

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import java.util.concurrent.locks.ReentrantLock

import scala.collection.JavaConverters._

case class SeenUser(var nicks: List[String], var lastAction: String, var activeNick: String,
  var date: Long, var channel: String) extends Worm {
    def getNicks() = nicks.asJava
  }

class Seen(val moduleHandler: ModuleHandler) extends TriggerListener with MessageListener with
  NickChangeListener with JoinListener with QuitListener with PartListener {
  
  val lock = new ReentrantLock
  val f = DateTimeFormat.forPattern("HH:mm dd.MM.yyyy")
  val bot = Grouphug.getInstance
  if(SQL.isAvailable) {
    moduleHandler.addTriggerListener("seen", this)
    moduleHandler.addMessageListener(this)
    moduleHandler.addNickChangeListener(this)
    moduleHandler.addJoinListener(this)
    moduleHandler.addQuitListener(this)
    moduleHandler.addPartListener(this)
    moduleHandler.registerHelp("seen", "Seen: When someone last did something in this channel\n" +
      "  !seen <nick>\n");
  } else {
    println("Seen module disabled: SQL is unavailable.");
  }

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String,
    trigger: String) {
    val user = Worm.get[SeenUser].find(_.nicks.contains(message))
    if(user isDefined) {
      val channelInform = if(channel == user.get.channel || user.get.channel.isEmpty) ""
                          else String.format("(in %s) ", user.get.channel)
      val nickInform = if(message == user.get.activeNick || user.get.activeNick.isEmpty) ""
                       else String.format("(as %s) ", user.get.activeNick)
      
      if (channel.equals(user.get.channel)) {
        bot.msg(channel, String.format("%s was last seen %s%s%s at %s", message, channelInform,
        nickInform, user.get.lastAction, f.print(user.get.date)))
      } else {
        bot.msg(channel, String.format("%s was last seen %s%s at %s", message, channelInform,
        nickInform, f.print(user.get.date)))
      }
    } else {
      bot.msg(channel, String.format("%s hasn't done anything yet.", message))
    }
  }

  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) =
    registerAction(sender, String.format("saying '%s'", message), sender, channel)

  def onNickChange(oldNick: String, login: String, hostname: String, newNick: String) {
    lock.lock
    val user = Worm.get[SeenUser].find(_.nicks.contains(oldNick))
    if(user isDefined) {
      user.get.nicks = newNick +: user.get.nicks
      user.get.update
      registerAction(newNick, String.format("changing nick from %s to %s", oldNick, newNick))
    } else {
      // A very rare case: The user has never performed any action here before,
      // and the first action is changing nick
      val newUser = SeenUser(List(newNick, oldNick), String.format(
        "changing nick from %s to %s", oldNick, newNick), "", new DateTime().getMillis, "")
      newUser.insert
    }
    lock.unlock
  }

  def onJoin(channel: String, sender: String, login: String, hostname: String) =
    registerAction(sender, String.format("joining"), sender, channel)

  def onQuit(sourceNick: String, sourceLogin: String, sourceHostname: String, reason: String) =
    registerAction(sourceNick, String.format("quitting IRC, saying '%s'", reason), sourceNick)

  def onPart(channel: String, sender: String, login: String, hostname: String) =
    registerAction(sender, "leaving the channel", sender, channel)

  private def registerAction(nick: String, action: String, activeNick: String = "", channel: String = "") {
    lock.lock
    val list = Worm.get[SeenUser]
    val user = list.find(_.nicks.contains(nick))
    if(user isDefined) {
      user.get.lastAction = action
      user.get.activeNick = activeNick
      user.get.date = new DateTime().getMillis
      user.get.channel = channel
      user.get.update
    } else {
      val newUser = SeenUser(List(nick), action, activeNick, new DateTime().getMillis, channel)
      newUser.insert
    }
    lock.unlock
  }
}