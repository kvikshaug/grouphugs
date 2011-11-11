package no.kvikshaug.gh.modules

import scala.xml._

import java.net.URL
import java.util.regex._
import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Slang(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("slang", this);
  handler.registerHelp("slang", "Slang: Define an expression in slang terms.\n" +
                   "  !slang <expr>\n" +
                   "  !slang -n <number> <expr>\n" +
                   "  !slang -ex <expr>")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    try {
      val showExample = message.contains("-ex")
      val m = Pattern.compile("(-n ([0-9]+) )?(.*)").matcher(message)
      m.matches()
      var number = 1
      if(m.group(2) != null) {
          number = m.group(2).toInt
      }
      val query = java.net.URLEncoder.encode(m.group(3).replaceAll("-ex", "").trim, "UTF-8")
      val root = XML.withSAXParser(SAXParserImpl.newInstance(null)).load(
        //Web.prepareEncodedBufferedReader(new URL("http://www.urbandictionary.com/define.php?term="+query)))
        "http://www.urbandictionary.com/define.php?term="+query)
      var hit = false
      var done = false
      for(table <- root \\ "table" if((table \ "@id").text == "entries")) {
        hit = true
        var i = -1
        var current = 1
        val trs = table \ "tr"
        for(tr <- trs) {
          i += 1
          val td = (tr \ "td")
          if((!td.isEmpty) && (td(0) \ "@class").text == "index" && !done) {
            if(current < number) {
              current += 1
            } else {
              done = true
              val word = (trs(i) \ "td")(1).text.trim
              val definition = ((trs(i + 1) \ "td")(1) \ "div")(0).text.trim
              val example = ((trs(i + 1) \ "td")(1) \ "div")(1).text.trim
              if(showExample) {
                bot.msg(channel, example, true)
              } else {
                bot.msg(channel, word + ": " + definition, true)
              }
            }
          }
        }
      }
      if(!hit) {
        bot.msg(channel, "There's no slang for that.")
      } else if(!done) {
        if(number <= 7) {
          bot.msg(channel, "No entry found at that number.")
        } else {
          bot.msg(channel, "No entry found at that number, try 7 or less (UD normally only shows 7 entries on one page).")
        }
      }
    } catch {
      case e =>
        bot.msg(channel, "Exception (1 of 1): These are not the slangs you're looking for.")
        e.printStackTrace
    }
  }
}
