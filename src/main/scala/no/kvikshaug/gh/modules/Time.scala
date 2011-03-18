package no.kvikshaug.gh.modules

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Time(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("time", this)
  handler.registerHelp("time", "!time <command> - Perform some bot command and time how long it takes\n" +
    "Calling time without any arguments will just show how long it takes before I recieve your message.")
  println("Time module loaded.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) {
    val now = System.currentTimeMillis
    bot.onMessage(channel, sender, login, hostname, message)
    val later = System.currentTimeMillis
    bot.msg(channel, "Time used: " + (later - now) + "ms")
  }
}

