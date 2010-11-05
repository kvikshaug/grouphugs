package no.kvikshaug.gh.modules

import java.net.{URL, URLEncoder}

import listeners.TriggerListener
import util.Web

class Translate(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("translate", this)
  handler.addTriggerListener("tcode", this)
  handler.registerHelp("translate", "!translate [-f from_code] [-t to_code] text\n" +
    "!tcode <language> => Gives langauge code\n" +
    "Default: from english to norwegian\n" +
    "F.ex. '!translate -t hu hello' => From english to hungarian")
  println("Translate module loaded.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    trigger match {
      case "tcode" => bot.sendMessage(codeFor(message.toLowerCase)); return
      case "translate" =>

      val input = parseMessage(message)
      val buffer = Web.prepareEncodedBufferedReader(new URL("http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&q=" + URLEncoder.encode(input._3, "UTF-8") + "&langpair=" + input._1 + "|" + input._2))

      // TODO use a json library
      val json = buffer.readLine
      val srch = "\"translatedText\":\"";
      if(!json.contains(srch)) {
        bot.sendMessage("Google didn't reply, maybe your language codes are incorrect?")
      } else {
        val translation = json.substring(json.indexOf(srch) + srch.length, json.indexOf("\"", json.indexOf(srch) + srch.length))
        bot.sendMessage(translation)
      }
    }
  }

  def parseMessage(message: String) = {
    // i suck at string manipulation :( but it works :)
    var handle = message.replaceAll("translate", "").trim;

    var toCode = "no"
    var fromCode = "en"

    if(handle.contains("-t ")) {
      toCode = handle.substring(handle.indexOf("-t ") + 3, handle.indexOf(" ", handle.indexOf("-t ") + 3))
      handle = handle.replaceAll("-t " + toCode, "").trim
    }

    if(handle.contains("-f ")) {
      fromCode = handle.substring(handle.indexOf("-f ") + 3, handle.indexOf(" ", handle.indexOf("-f ") + 3))
      handle = handle.replaceAll("-f " + fromCode, "").trim
    }

    (fromCode, toCode, handle)
  }

  def codeFor(language: String) = language match {
    case "afrikaans"      => "af"
    case "african"        => "af"
    case "albanian"       => "sq"
    case "arabic"         => "ar"
    case "armenian"       => "hy"
    case "azerbaijani"    => "az"
    case "basque"         => "eu"
    case "belarusian"     => "be"
    case "bulgarian"      => "bg"
    case "catalan"        => "ca"
    case "chinese"        => "zh-cn"
    case "croatian"       => "hr"
    case "czech"          => "cs"
    case "danish"         => "da"
    case "dutch"          => "nl"
    case "english"        => "en"
    case "estonian"       => "et"
    case "filipino"       => "tl"
    case "finnish"        => "fi"
    case "french"         => "fr"
    case "galician"       => "gl"
    case "georgian"       => "ka"
    case "german"         => "de"
    case "greek"          => "el"
    case "haitian creole" => "ht"
    case "hebrew"         => "iw"
    case "hindi"          => "hi"
    case "hungarian"      => "hu"
    case "icelandic"      => "is"
    case "indonesian"     => "id"
    case "irish"          => "ga"
    case "italian"        => "it"
    case "japanese"       => "ja"
    case "korean"         => "ko"
    case "latin"          => "la"
    case "latvian"        => "lv"
    case "lithuanian"     => "lt"
    case "macedonian"     => "mk"
    case "malay"          => "ms"
    case "maltese"        => "mt"
    case "norwegian"      => "no"
    case "persian"        => "fa"
    case "polish"         => "pl"
    case "portuguese"     => "pt"
    case "romanian"       => "ro"
    case "russian"        => "ru"
    case "serbian"        => "sr"
    case "slovak"         => "sk"
    case "slovenian"      => "sl"
    case "spanish"        => "es"
    case "swahili"        => "sw"
    case "swedish"        => "sv"
    case "thai"           => "th"
    case "turkish"        => "tr"
    case "ukrainian"      => "uk"
    case "urdu"           => "ur"
    case "vietnamese"     => "vi"
    case "welsh"          => "cy"
    case "yiddish"        => "yi"
    case _                => "Sorry, I don't recognize that language. Maybe you could find it manually and add it to my list :)"
  }
}

