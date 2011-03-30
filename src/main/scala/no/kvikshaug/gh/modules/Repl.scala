package no.kvikshaug.gh.modules

import java.net.{URL, URLEncoder}
import java.util.regex._

import scala.actors.Actor
import scala.actors.Actor._

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Repl(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("repl", this)
  handler.registerHelp("repl", "!repl <command> - Send a command to simplyscala.com for REPL output")
  println("Repl module loaded.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String): Unit = {
    // run this asynchronous
    listener ! (message, channel)
  }

  val maxTries = 5
  val sleepTime = 5 // seconds

  val listener = actor {
    loop {
      receive {
        case message: Tuple2[String, String] =>
          var line = ""
          var counter = -1
          while(line == "") {
            counter = counter + 1
            if(counter == maxTries) {
              bot.msg(message._2, "I tried polling simplyscala.com " + maxTries + " times now but they still say " +
                "that our interpreter is being created. Maybe there's some bug somewhere? I'm giving up.")
            }
            line = run(message._1)
            if(line matches "(?s).*New interpreter instance being created for you.*") {
              line = ""
              println("REPL: Interpreter is being created, trying again in " + sleepTime + " seconds.")
            }
            Thread.sleep(sleepTime * 1000)
          }
          val pattern = Pattern.compile("""(?s).*<pre class="code">.*</pre>.*<pre.*>(.*)</pre>.*""").matcher(line)
          if(pattern matches) {
            def nIndex(h: String, n: String, s: Int, c: Int): Int = {
              if(c == 0 || s == -1) {
                s
              } else {
                nIndex(h, n, h.indexOf(n, s + 1), c - 1)
              }
            }
            // max 3 lines
            line = pattern.group(1)
            val thirdLineBreak = nIndex(line, "\n", 0, 3)
            if(thirdLineBreak != -1) {
              line = line.substring(0, thirdLineBreak)
            }
            bot.msg(message._2, line)
          } else {
            bot.msg(message._2, "Whoops, looks like the expected output at simplyscala.com has changed. My regex didn't match.")
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
}

