package no.kvikshaug.gh.modules

import listeners.TriggerListener

class Scala(val handler: ModuleHandler) extends TriggerListener {

  val bot = Grouphug.getInstance
  handler.addTriggerListener("scala", this)
  System.out.println("Scala module registered.")

  def onTrigger(channel: String, sender: String, login: String, hostname: String, message: String, trigger: String) = {
    bot.sendMessage("Hi from Scala!")
  }
}

