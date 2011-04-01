package no.kvikshaug.gh.modules

import java.net.{URL, URLEncoder}
import java.util.regex._

import scala.actors.Actor
import scala.actors.Actor._

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

case class Message(val code: String, val channel: String)
class Repl(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("repl", this)
  handler.registerHelp("repl", """!repl <command> - Send a command to simplyscala.com for REPL output
!repl -l 1 <command> - Show only 1 line (the default is 3 lines)
!repl -l a <command> - Show all lines""")
  println("Repl module loaded.")

  val defaultLineCount = 3

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    // run this asynchronous
    val m = Pattern.compile("-l ([0-9|a]+) (.*)").matcher(message)
    if(m matches) {
      if(m.group(1) == "a") {
        listener ! Message(m.group(2), -1, channel)
      } else {
        listener ! Message(m.group(2), m.group(1).toInt, channel)
      }
    } else {
      listener ! Message(message, defaultLineCount, channel)
    }
  }

  val maxTries = 5
  val sleepTime = 5 // seconds

  val listener = actor {
    loop {
      receive {
        case message: Message =>
          var line = ""
          var counter = -1
          while(line == "") {
            counter = counter + 1
            if(counter == maxTries) {
              bot.msg(message.channel, "I tried polling simplyscala.com " + maxTries + " times now but they still say " +
                "that our interpreter is being created. Maybe there's some bug somewhere? I'm giving up.")
            }
            line = run(message.code)
            if(line matches "(?s).*New interpreter instance being created for you.*") {
              line = ""
              println("REPL: Interpreter is being created, trying again in " + sleepTime + " seconds.")
            }
            Thread.sleep(sleepTime * 1000)
          }
          val pattern = Pattern.compile("""(?s).*<pre class="code">.*</pre>.*<pre.*>(.*)</pre>.*""").matcher(line)
          if(pattern matches) {
            bot.msg(message.channel, maxLines(pattern.group(1), message.lineCount), true)
          } else {
            bot.msg(message.channel, "Whoops, looks like the expected output at simplyscala.com has changed. My regex didn't match.")
          }
      }
    }
  }

  def run(code: String): String = {
    val br = Web.prepareEncodedBufferedReader(
      new URL("http://www.simplyscala.com/interp?code=" + URLEncoder.encode(code, "UTF-8")))
    val builder = new StringBuilder
    var l = br.readLine
    while(l != null) {
      builder.append(l).append("\n")
      l = br.readLine
    }
    br.close
    builder.toString
  }

  def maxLines(str: String, max: Int): String = max match {
    case 0 => ""
    case _ => str.indexOf('\n', 1) match {
      case -1 => str
      case i  => str.substring(0, i) + maxLines(str.substring(i), max - 1)
    }
  }
}

