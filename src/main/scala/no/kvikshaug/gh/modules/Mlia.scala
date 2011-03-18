package no.kvikshaug.gh.modules

import scala.xml._

import java.net.URL
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Mlia(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("mlia", this)
  handler.registerHelp("mlia", "!mlia - Show a random entry from http://mylifeisaverage.com/")
  println("Mlia module loaded.")

  /* At the time of this writing there are 8481 pages at mylifeisaverage.com.
   * We will select one of these randomly and display the top entry.
   * We COULD load the page and get the count for each trigger instead of
   * hardcoding it, but that would mean making two connections to the site
   * for each trigger.
   */
  val pageCount = 8481
  val random = new java.util.Random
  def randomPage = random.nextInt(pageCount + 1) + 1 // read java.util.Random.nextInt(int) javadoc if you wonder about +1

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(Web.prepareEncodedBufferedReader(
      new URL("http://mylifeisaverage.com/" + randomPage)))

    for(div <- root \\ "div") {
      if((div \ "@class").text == "sc") {
        bot.msg(channel, div.text.trim)
        return
      }
    }
  }
}

