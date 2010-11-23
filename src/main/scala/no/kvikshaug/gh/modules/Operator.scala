package no.kvikshaug.gh.modules

import java.io.{File, IOException}
import scala.xml._

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.listeners.{JoinListener, NickChangeListener}

import org.jibble.pircbot.User

case class UserMask(nick: String, login: String, hostname: String) {
  override def equals(other: Any) = other match {
    case that: UserMask => nick == that.nick
    case _ => false
  }
}

class Operator(handler: ModuleHandler) extends JoinListener with NickChangeListener {

  val bot = Grouphug.getInstance
  handler.addJoinListener(this)
  handler.addNickChangeListener(this)

  val channels = (XML.loadFile("props.xml") \ "Channels" \ "Channel" toList).map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Operator" \ "Nick").toList.map(_.text))
  }.toMap

  println("Operator module loaded.")
}

