package no.kvikshaug.gh.modules

import scala.xml._

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.listeners.{JoinListener, NickChangeListener}

class Operator(handler: ModuleHandler) extends JoinListener with NickChangeListener {

  val bot = Grouphug.getInstance
  handler.addJoinListener(this)
  handler.addNickChangeListener(this)

  val channels = (XML.loadFile("props.xml") \ "Channels" \ "Channel" toList).map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Operator" \ "Nick").toList.map(_.text))
  }.toMap

  println("Operator module loaded.")

  def onJoin(channel: String, sender: String, login: String, hostname: String) {
    if(!(hasOp(sender, channel)) && channels.get(channel).get.exists(_ == sender)) {
      bot.op(channel, sender)
    }
  }

  def onNickChange(oldNick: String, login: String, hostname: String, newNick: String) {
    channels foreach { (m) =>
      if(!(hasOp(newNick, m._1)) && m._2.exists(_ == newNick)) {
        bot.op(m._1, newNick)
      }
    }
  }

  def hasOp(nick: String, channel: String) =
    bot.getUsers(channel).exists((x) => x.getNick == nick && x.isOp)
}

