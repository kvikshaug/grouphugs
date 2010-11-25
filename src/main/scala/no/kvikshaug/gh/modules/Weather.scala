package no.kvikshaug.gh.modules

import scala.xml._
import java.net.{URL, URLEncoder}
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
!weather -f <place>   - Full forecasts for <place>
!weather -a <place>   - All available weather data for <place>
0 = forecast today; longest ahead is 3 days""")
  println("Weather module registered.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) {
    val input = parseLine(message)

    val root = XML.load(Web.prepareEncodedBufferedReader(
      new URL("http://www.google.com/ig/api?weather=" + URLEncoder.encode(input._3, "UTF-8")))) \ "weather"

    // check that the place was found
    if((root \ "problem_cause").size != 0) {
      bot.sendMessageChannel(channel, "I don't think google tracks the weather in " + input._3 + ".")
      return
    }

    val maxDays = (root \ "forecast_conditions" size) - 1
    if(input._1.exists(_ > maxDays)) {
      bot.sendMessageChannel(channel, "Google only provides forecast data for " + maxDays + " days ahead.")
      return
    }

    bot.sendMessageChannel(channel,
      (root \ "forecast_information" \ "city" \ "@data").text + ", " +
      (root \ "forecast_information" \ "current_date_time" \ "@data").text)

    // current conditions
    if(input._2) {
      bot.sendMessageChannel(channel, "Currently: " +
        (root \ "current_conditions" \ "temp_c" \ "@data").text + "°C, " +
        (root \ "current_conditions" \ "condition" \ "@data").text + ". " +
        (root \ "current_conditions" \ "humidity" \ "@data").text + ". " +
        (root \ "current_conditions" \ "wind_condition" \ "@data").text)
    }

    // forecast
    for(day <- input._1) {
      bot.sendMessageChannel(channel, forecastFor(day, root))
    }
  }

  def forecastFor(day: Int, root: NodeSeq) =
    ((root \ "forecast_conditions")(day) \ "day_of_week" \ "@data").text + ": " +
    math.round(ftoc(((root \ "forecast_conditions")(day) \ "low" \ "@data").text.toDouble)) + "-" +
    math.round(ftoc(((root \ "forecast_conditions")(day) \ "high" \ "@data").text.toDouble)) + "°C, " +
    ((root \ "forecast_conditions")(day) \ "condition" \ "@data").text

  def parseLine(str: String) = {
    val mAll = Pattern.compile("-a (.+)").matcher(str)
    val mFor = Pattern.compile("-f (.+)").matcher(str)
    val mSeq = Pattern.compile("([0-9])-([0-9]) (.+)").matcher(str)
    val mOne = Pattern.compile("([0-9]) (.+)").matcher(str)
    //val mCur = Pattern.compile(".+").matcher(str)

    if(mAll matches) {
      (List(0, 1, 2, 3), true, mAll.group(1))
    } else if(mFor matches) {
      (List(0, 1, 2, 3), false, mFor.group(1))
    } else if(mSeq matches) {
      ((mSeq.group(1).toInt to mSeq.group(2).toInt toList), false, mSeq.group(3))
    } else if(mOne matches) {
      (List(mOne.group(1).toInt), false, mOne.group(2))
    } else /* if(mCur matches) */ {
      (List(), true, str)
    }
  }

  def ftoc(f: Double) = (f - 32) * (5.0/9)
}

