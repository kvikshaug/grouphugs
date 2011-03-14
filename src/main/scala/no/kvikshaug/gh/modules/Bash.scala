package no.kvikshaug.gh.modules

import scala.xml._

import java.net.URL
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Bash(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("bash", this)
  handler.registerHelp("bash", "!bash - Show a random entry from http://bash.org/?random1")
  println("Bash module loaded.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(Web.prepareEncodedBufferedReader(
      new URL("http://bash.org/?random1")))

    for(p <- root \\ "p") {
      if((p \ "@class").text == "qt") {
        bot.sendMessageChannel(channel, p.text.trim)
        return
      }
    }
  }
}

