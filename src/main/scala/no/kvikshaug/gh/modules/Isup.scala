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

class IsUp(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("isup", this)
  handler.registerHelp("isup", "isup - Checks if a web site is down, or if it's just your connection that sucks somehow.\n" +
    "Usage:\n" +
    "!isup http://foo.tld       - Checks if http://foo.tld is up or not.\n" +
    "!isup -t 20 http://foo.tld - Same, with a 20 second timeout value. Default is " + defaultTimeout + ".")
  println("IsUp module loaded.")

  val defaultTimeout = 10 // seconds

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    // Parse input
    var url = message
    var timeout = defaultTimeout
    val timeoutRegex = Pattern.compile("-t ([0-9]+) (.+)").matcher(message)
    if(timeoutRegex matches) {
      url = timeoutRegex.group(2)
      timeout = timeoutRegex.group(1).toInt
    }
    if(!(url.startsWith("http://"))) {
      url = "http://" + url
    }

    try {
      // Try to connect in asynchronous actors
      val con = ConnectionAttempt(new URL(url), timeout * 1000, channel)
      connecter ! con
      pinger ! con
    } catch {
      case e: MalformedURLException =>
        bot.msg(channel, "Is '" + url + "' supposed to look like a url?")
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
            case e: NoRouteToHostException =>
            case e: ConnectException =>
              bot.msg(c.channel, c.url.getHost + " doesn't respond to ping: " + e.getMessage + ".")
            case e: IOException =>
              bot.msg(c.channel, c.url.getHost + " doesn't respond to ping: " + e.getMessage + ".")
          }
      }
    }
  }
}
