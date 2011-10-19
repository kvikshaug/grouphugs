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

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    // Mmmmyeah, if changes are needed here, just rewrite the whole thing.
    // The current code is the way it is for 3 reasons:
    // 1. No gulesider.no API
    // 2. Overuse of and difficult-to-parse HTML tags
    // 3. Inexperience with the scala XML api, and also not caring so much since it's supposedly going
    //    to be replaced by https://github.com/djspiewak/anti-xml
    val query = java.net.URLEncoder.encode(message, "UTF-8")
    val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(Web.prepareEncodedBufferedReader(
      new URL("http://www.gulesider.no/person/resultat/" + query)))

    val m = Pattern.compile("(?s).*?([0-9]++).treff på personer.*").matcher(root.text)
    var hits = ""
    if(root.text.contains("Vis alle ")) {
      for(div <- root \\ "div" if((div \ "@class").text == "profile-main")) {
        val firstName = (((div \ "div")(0) \ "h1")(0) \ "span")(0).text.trim
        val lastName = (((div \ "div")(0) \ "h1")(0) \ "span")(1).text.trim
        var address = "" // zzz
        for(p <- ((div \ "div")(0) \ "div")(0) \ "p" if((p \ "@class").text.contains("adr-index-0"))) {
          val adrSpan = (p(0) \ "span")(1) \ "span"
          address = adrSpan(0).text.trim + ", " + adrSpan(1).text.trim + " " + adrSpan(2).text.trim
        }
        // telephones
        var tlf = ""
        for(p <- ((div \ "div")(0) \ "div")(0) \ "p" if((p \ "@class").isEmpty)) {
          tlf += ((p \ "span")(0) \ "a")(0).text.trim + ", "
        }
        for(p <- ((div \ "div")(0) \ "div")(0) \ "p" if((p \ "@class").text.contains("adr-index-0"))) {
          tlf += ((p \ "span")(0) \ "a")(0).text.trim
        }
        bot.msg(channel, firstName + " " + lastName + ", " + address + ": " + tlf + " (only hit)" +
          " - http://www.gulesider.no/person/resultat/" + query)
      }
    } else if(m matches) {
      for(ol <- root \\ "ol" if((ol \ "@class").text == "hitlist")) {
        val li = (ol \ "li")(0)
        val name = (li \\ "span")(0).text + " " + (li \\ "span")(1).text.trim
        var address = ", "
        var addressHits = false
        for(p <- li \\ "p" if((p \ "@class").text == "adr")) {
          val spans = p \ "span"
          if(!spans(0).text.isEmpty) {
            addressHits = true
          }
          address += spans(0).text.trim + ", " + spans(1).text.trim + " " + spans(2).text.trim
        }
        if(!addressHits) {
          address = ""
        }
        var tlf = ""
        for(span <- li \\ "span" if((span \ "@class").text.startsWith("tel"))) {
          tlf += (span \ "a").text.trim + ", "
        }
        bot.msg(channel, name + address + ": " + tlf.substring(0, tlf.length()-2) + " (first of " +
          m.group(1) + " hits) - http://www.gulesider.no/person/resultat/" + query)
      }
    } else {
      bot.msg(channel, "Sorry, I got no hits.")
      return
    }
  }
}

