package no.kvikshaug.gh.modules

import no.kvikshaug.gh.listeners.TriggerListener
import no.kvikshaug.gh.{Grouphug, ModuleHandler}

class Scala(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("scala", this)
  System.out.println("Scala module registered.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = {
    bot.msg(channel, "Hi from Scala!")
  }
}

