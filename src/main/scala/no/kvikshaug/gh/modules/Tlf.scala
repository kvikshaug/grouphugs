package no.kvikshaug.gh.modules

import scala.xml._

import java.net.URL
import java.util.regex._
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Tlf(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("tlf", this)
  handler.registerHelp("tlf", "!tlf - Lookup a name or telephone number at gulesider.no")
  println("Tlf module loaded.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    val query = java.net.URLEncoder.encode(message)
    val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(Web.prepareEncodedBufferedReader(
      new URL("http://www.gulesider.no/tk/search.c?q=" + query + "&x=0&y=0")))

    var hits = ""
    for(div <- root \\ "div" if((div \ "@class").text == "result_head")) {
      val results = (div \ "h1").text.trim
      val m = Pattern.compile(".*ga (.*) treff").matcher(results)
      m.matches
      hits = m.group(1)
    }
    if(hits isEmpty) {
      bot.sendMessageChannel(channel, "Sorry, I got no hits.")
      return
    }

    var name = ""
    for(div <- root \\ "div" if((div \ "@id").text == "kolonne-hoved")) {
      name = (((div \ "div")(0) \ "div")(0) \ "h2").text.replaceAll("MER INFO", "").trim
    }

    var address = ""
    for(div <- root \\ "div" if((div \ "@id").text == "kolonne-hoved")) {
      var count = 0
      while((address isEmpty) && count < 10) {
        try {
          val d = (((div \ "div")(0) \ "div")(0) \ "div")(count)
          if((d \ "@class").text == "address") {
            address = ", " + d.text.trim
          }
        } catch {
          case e => // ignore
        }
        count = count + 1
      }
    }

    var numbers = ""
    for(div <- root \\ "div" if((div \ "@id").text == "kolonne-hoved")) {
      for(number <- ((div \ "div")(0) \ "div")(0) \ "div" if((number \ "@class").text == "numberHolder")) {
        numbers += ", " + (number \ "p")(1).text.trim
      }
    }
    numbers = numbers.substring(2)
    var hitText = ""
    if(hits == "1") {
      hitText = " (only hit)"
    } else {
      hitText = " (first of " + hits + " hits)"
    }
    bot.sendMessageChannel(channel, name + address + ": " + numbers + hitText + " — http://www.gulesider.no/tk/search.c?q=" + query)
  }
}

