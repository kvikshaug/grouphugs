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
    val mAll = Pattern.compile("-a (.+)").matcher(str)
    val mSeq = Pattern.compile("([0-9])-([0-9]) (.+)").matcher(str)
    val mOne = Pattern.compile("([0-9]) (.+)").matcher(str)
    //val mFor = Pattern.compile(".+").matcher(str)

    if(mAll matches): {
      (List(0, 1, 2, 3), true, mAll.group(1))
    } else if(mSeq matches) {
      ((mSeq.group(1).toInt to mSeq.group(2).toInt toList), false, mSeq.group(3))
    } else if(mOne matches) {
      (List(mOne.group(1).toInt), false, mOne.group(2))
    } else /* if(mFor matches) */ {
      (List(), true, str)
    }
  }
}

