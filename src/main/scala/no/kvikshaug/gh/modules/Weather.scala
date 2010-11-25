package no.kvikshaug.gh.modules

import scala.xml._
import java.net.URL
import java.util.regex._

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Weather(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("weather", this)
  handler.registerHelp("weather", """!weather - Display current weather and/or forecast for a place
!weather <place>      - Current weather for <place>
!weather 2 <place>    - Weather in two days at <place>
!weather 1-2 <place>  - Weather next two days at <place>
!weather -a <place>   - All available weather data for <place>
0 = forecast today; longest ahead is 3 days""")
  println("Weather module registered.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = {
    val input = parseLine(message)
    val root = XML.load(Web.prepareEncodedBufferedReader(
      new URL("http://www.google.com/ig/api?weather=" + place)))

    bot.sendMessageChannel(channel, (root \\ "p").text)
  }

  def parseLine(str: String) = {
    if str matches "-a (.+)"
  }
}

