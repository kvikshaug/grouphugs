package no.kvikshaug.gh.modules

import scala.xml._

import java.net.URL
import java.util.regex._
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Confession(val handler: ModuleHandler) extends TriggerListener {

  val r = new java.util.Random
  val bot = Grouphug.getInstance
  handler.addTriggerListener("gh", this)
  handler.registerHelp("gh", """!gh - The original GH command.""")

  val pageCount = 89305 // Based on the page number the "Last" link oges to on http://archive.grouphug.us/

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    try {
      if(!message.isEmpty) {
        bot.msg(channel, "No, just !gh.")
        return
      }
      val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(
        Web.prepareEncodedBufferedReader(new URL("http://archive.grouphug.us/frontpage?page=" +
        (r.nextInt(pageCount) + 1))))
      var confession = ""
      val div = (root \\ "div").find(div => (div \ "@id").text.startsWith("node-"))
      for(p <- (div.get \ "div")(0) \ "p") {
        confession += p.text + '\n'
      }
      bot.msg(channel, confession)
    } catch {
      case e =>
        bot.msg(channel, "I confess that I threw an exception today. :(")
        e.printStackTrace
    }
  }
}
