package no.kvikshaug.gh.modules

import scala.xml._
import java.net.URL

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Commit(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("commit", this)
  handler.registerHelp("commit", "!commit - Show a random commit message from http://whatthecommit.com/")
  System.out.println("Commit module registered.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = {
    val root = XML.load(Web.prepareEncodedBufferedReader(
      new URL("http://whatthecommit.com/")))

    bot.sendMessageChannel(channel, (root \\ "p").text)
  }
}

