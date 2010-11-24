package no.kvikshaug.gh

import scalaj.collection.Imports._
import scala.collection.JavaConversions
import scala.xml._

object Config {
  val configFile = "props.xml"
  val root = XML.loadFile(configFile)

  // General options
  val nicks = (root \ "Nicks" \ "Nick").map(_.text).asJava
  val channels = (root \ "Channels" \ "Channel").map(_.attribute("chan").get.text).asJava
  val servers = (root \ "Servers" \ "Server").map(_.text).asJava

  // Upload module
  val uploadDirs = (root \ "Channels" \ "Channel").map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "UploadDir").text)
  }.toMap.asJava
  val publicUrls = (root \ "Channels" \ "Channel").map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "PublicURL").text)
  }.toMap.asJava

  // Operator module
  val operatorList = (XML.loadFile(Grouphug.configFile) \ "Channels" \ "Channel" toList).map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Operator" \ "Nick").toList.map(_.text))
  }.toMap

}
