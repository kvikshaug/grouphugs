package no.kvikshaug.gh.modules

import scala.xml._

import no.kvikshaug.gh.{Grouphug, ModuleHandler, Config}
import no.kvikshaug.gh.listeners.{JoinListener, NickChangeListener}

class Operator(handler: ModuleHandler) extends JoinListener with NickChangeListener {

  val bot = Grouphug.getInstance
  handler.addJoinListener(this)
  handler.addNickChangeListener(this)

  def onJoin(channel: String, sender: String, login: String, hostname: String) {
    Config.reparse // make sure we have the newest operator list
    if(!(hasOp(sender, channel)) && Config.operatorList.get(channel).get.exists(_ == sender)) {
      bot.op(channel, sender)
    }
  }

  def onNickChange(oldNick: String, login: String, hostname: String, newNick: String) {
    Config.operatorList foreach { (m) =>
      if(!(hasOp(newNick, m._1)) && m._2.exists(_ == newNick)) {
        bot.op(m._1, newNick)
      }
    }
  }

  def hasOp(nick: String, channel: String) =
    bot.getUsers(channel).exists((x) => x.getNick == nick && x.isOp)
}

