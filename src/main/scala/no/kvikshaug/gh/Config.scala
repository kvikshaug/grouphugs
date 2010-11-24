package no.kvikshaug.gh

import scalaj.collection.Imports._
import scala.collection.JavaConversions
import scala.xml._

object Config {
  val configFile = "props.xml"
  var root = XML.loadFile(configFile)
  def reparse = root = XML.loadFile(configFile)

  // General options
  def nicks = (root \ "Nicks" \ "Nick").map(_.text).asJava
  def channels = (root \ "Channels" \ "Channel").map(_.attribute("chan").get.text).asJava
  def servers = (root \ "Servers" \ "Server").map(_.text).asJava

  // Upload module
  def uploadDirs = (root \ "Channels" \ "Channel").map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "UploadDir").text)
  }.toMap.asJava
  def publicUrls = (root \ "Channels" \ "Channel").map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "PublicURL").text)
  }.toMap.asJava

  // Operator module
  def operatorList = (XML.loadFile(Grouphug.configFile) \ "Channels" \ "Channel" toList).map { (x) =>
    ((x \ "@chan").text -> (x \ "Modules" \ "Operator" \ "Nick").toList.map(_.text))
  }.toMap

}
