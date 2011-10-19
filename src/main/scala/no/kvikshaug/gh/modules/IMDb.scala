package no.kvikshaug.gh.modules

import scala.xml._

import java.net.URL
import java.util.regex._
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class IMDb(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("imdb", this)
  handler.registerHelp("imdb", "!imdb - Show IMDb info for a movie")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    try {
      val imdbURLs = Web.googleSearch(message+"+site:www.imdb.com")
      if(imdbURLs isEmpty) {
        bot.msg(channel, "Sorry, I didn't find "+message+" on IMDb.")
        return
      }
      val imdbURL = imdbURLs.get(0)
      if(!imdbURL.toString.contains("/title/tt")) {
        bot.msg(channel, "I found that on IMDb but it doesn't look like a movie: " + imdbURL)
        return
      }
      var title = Web.fetchTitle(imdbURL)
      title = title.substring(0, title.length()-7) //To remove the  - IMDb part of the title      
      val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(
        Web.prepareEncodedBufferedReader(imdbURL))
      var score = ""
      for(span <- root \\ "span" if((span \ "@itemprop").text == "ratingValue")) {
        score = span.text + "/10"
      }
      var plot = ""
      for(p <- root \\ "p" if((p \ "@itemprop").text == "description")) {
        if(p.text.contains("See full summary")) {
          plot = p.text.substring(0, p.text.indexOf("See full summary")).trim
        } else {
          plot = p.text.trim          
        }
      }
      bot.msg(channel, title+"\n"+plot+"\n"+score+"\n"+imdbURL)
    } catch {
      case e => bot.msg(channel, "Meh, I got some exception, did IMDb change their layout AGAIN!?")
                e.printStackTrace
    }
  }
}

