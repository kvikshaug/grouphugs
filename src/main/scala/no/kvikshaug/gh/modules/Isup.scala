package no.kvikshaug.gh.modules

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.net._
import java.util.regex._

import scala.actors.Actor
import scala.actors.Actor._

case class ConnectionAttempt(url: URL, timeout: Int, channel: String)
case class PollSite(url: URL, nick: String, channel: String)

class IsUp(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("isup", this)
  handler.addTriggerListener("isupls", this)
  handler.addTriggerListener("isuprm", this)
  handler.addTriggerListener("whenup", this)
  handler.registerHelp("isup", "isup - Checks if a web site is down, or if it's just your connection that sucks somehow.\n" +
    "Usage:\n" +
    "!isup http://foo.tld       - Checks if http://foo.tld responds to HTTP requests.\n" +
    "!isup -p http://foo.tld    - Checks if http://foo.tld responds to ping.\n" +
    "!isup -t 20 http://foo.tld - Specify a 20 second timeout value. Default is " + defaultTimeout + ".\n" +
    "!whenup http://foo.tld     - Watch http://foo.tld and notify me when it comes back up.\n" +
    "!isuprm http://foo.tld     - Stop watching http://foo.tld.\n" +
    "!isupls                    - Show all URLs I'm watching\n" +
    "Note: I'll forget all the URLs I'm watching between restarts.")

  val sleepTime = 1 // minutes
  val defaultTimeout = 10 // seconds
  var sites = List[PollSite]()

  // Add 'urlify' method to string, which prepends "http://" if not already there
  implicit def urlifyString(s: String) = new Urlifier(s)
  class Urlifier(val value: String) {
    def urlify = if(value.startsWith("http://")) value else "http://" + value
  }

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = trigger match {
      case "isup" => isup(message, channel)
      case "isupls" =>
        sites foreach (s => bot.msg(channel, "Watching " + s.url + " for " + s.nick))
        if(sites isEmpty) {
          bot.msg(channel, "I'm not watching any URLs.")
        }
      case "isuprm" =>
        if(sites.exists(_.url.toString == message.urlify)) {
          sites = sites.filterNot(_.url.toString == message.urlify)
          bot.msg(channel, "Removed '" + message.urlify + "' from watchlist.")
        } else {
          bot.msg(channel, "I'm not watching '" + message + "'.")
        }
      case "whenup" =>
        try {
          if(sites.exists(_.url.toString == message.urlify)) {
            bot.msg(channel, "I'm already watching '" + message + "'.")
            return
          }
          sites = PollSite(new URL(message.urlify), sender, channel) :: sites
          bot.msg(channel, "Started watching '" + message + "', I'll notify you when it's back up.")
        } catch {
          case e: MalformedURLException =>
            bot.msg(channel, "Is '" + message.urlify + "' supposed to look like a url?")
        }
  }

  def isup(message: String, channel: String) = {
    // Parse input
    var ping = false
    var url = if(message contains "-p ") {
      ping = true
      message.replaceAll("-p ", "").trim
    } else {
      message
    }
    var timeout = defaultTimeout
    val timeoutRegex = Pattern.compile("-t ([0-9]+) (.+)").matcher(url)
    if(timeoutRegex matches) {
      url = timeoutRegex.group(2)
      timeout = timeoutRegex.group(1).toInt
    }
    url = url.urlify

    try {
      // Try to connect in asynchronous actors
      val con = ConnectionAttempt(new URL(url), timeout * 1000, channel)
      if(ping) {
        pinger ! con
      } else {
        connecter ! con
      }
    } catch {
      case e: MalformedURLException =>
        bot.msg(channel, "Is '" + url + "' supposed to look like a url?")
    }
  }

  val periodalConnecter = actor {
    loop {
      Thread.sleep(sleepTime * 60 * 1000)
      sites foreach { s =>
        try {
          val con = s.url.openConnection.asInstanceOf[HttpURLConnection]
          con.setInstanceFollowRedirects(false)
          con.setRequestMethod("HEAD")
          con.setConnectTimeout(defaultTimeout * 1000)
          con.connect
          bot.msg(s.channel, s.nick + ": Looks like " + s.url.getHost + " is back up! Removing it from my list.")
          sites = sites.filterNot(_ == s)
        } catch {
          case e => // ignore and retry later
        }
      }
    }
  }

  val connecter = actor {
    loop {
      receive {
        case c: ConnectionAttempt =>
          try {
            val con = c.url.openConnection.asInstanceOf[HttpURLConnection]
            con.setInstanceFollowRedirects(false)
            con.setRequestMethod("HEAD")
            con.setConnectTimeout(c.timeout)
            con.connect
            bot.msg(c.channel, "It's just you. I can reach " + c.url.getHost + " just fine.")
          } catch {
            case e: UnknownHostException =>
              bot.msg(c.channel, c.url.getHost + " appears to be down! Unknown host; I couldn't find an IP for the name.")
            case e: NoRouteToHostException =>
              bot.msg(c.channel, c.url.getHost + " appears to be down! There is no available route to the host.")
            case e: ConnectException =>
              bot.msg(c.channel, c.url.getHost + " appears to be down! I couldn't connect: " + e.getMessage + ".")
            case e: IOException =>
              bot.msg(c.channel, c.url.getHost + " appears to be down: " + e.getMessage + ".")
          }
      }
    }
  }

  val pinger = actor {
    loop {
      receive {
        case c: ConnectionAttempt =>
          val name = c.url.getHost
          try {
            if(InetAddress.getByName(name).isReachable(c.timeout)) {
              bot.msg(c.channel, name + " responds to ping!")
            } else {
              bot.msg(c.channel, name + " doesn't respond to ping.")
            }
          } catch {
            // Ignore unkown host and no route to host; they will be displayed by the httpconnecter
            // and it's implicit that it can't respond to ping if they are thrown
            case e: UnknownHostException =>
              bot.msg(c.channel, "Can't ping " + c.url.getHost + "! Unknown host; I couldn't find an IP for the name.")
            case e: NoRouteToHostException =>
              bot.msg(c.channel, "Can't ping " + c.url.getHost + "! There is no available route to the host.")
            case e: ConnectException =>
              bot.msg(c.channel, c.url.getHost + " doesn't respond to ping: " + e.getMessage + ".")
            case e: IOException =>
              bot.msg(c.channel, c.url.getHost + " doesn't respond to ping: " + e.getMessage + ".")
          }
      }
    }
  }
}
