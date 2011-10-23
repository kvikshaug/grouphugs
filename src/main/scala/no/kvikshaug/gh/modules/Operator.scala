package no.kvikshaug.gh.modules

import no.kvikshaug.gh.{Grouphug, ModuleHandler, Config}
import no.kvikshaug.gh.listeners.{MessageListener, JoinListener, NickChangeListener}

class Operator(handler: ModuleHandler) extends JoinListener with NickChangeListener with MessageListener {

  val bot = Grouphug.getInstance
  handler.addJoinListener(this)
  handler.addNickChangeListener(this)

  private def isOp(channel: String, nick: String) =
    Config.operatorList.get(channel).get.exists(_ == nick)

  private def hasOp(channel: String, nick: String) =
    bot.getUsers(channel).exists((x) => x.getNick == nick && x.isOp)

  def onJoin(channel: String, sender: String, login: String, hostname: String) {
    if(isOp(channel, sender) && !(hasOp(sender, channel))) {
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

  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {
    if (isOp(channel, sender) && !hasOp(channel, sender)) {
      bot.op(channel, sender);
    }
  }
}

