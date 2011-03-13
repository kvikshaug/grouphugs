package no.kvikshaug.gh

import no.kvikshaug.gh.exceptions.GithubHookDisabledException
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

  // GithubPostReceiveServer
  @throws(classOf[GithubHookDisabledException])
  def githubHookUrl = {
    val e = (root \\ "GithubHookUrl").text
    if(e isEmpty) {
      throw new GithubHookDisabledException("No URL option specified in " + configFile)
    } else {
      e
    }
  }
    @throws(classOf[GithubHookDisabledException])
    def githubHookPort = {
      val e = (root \\ "GithubHookPort").text
      if(e isEmpty) {
        throw new GithubHookDisabledException("No port option specified in " + configFile)
      } else {
        e.toInt
      }
    }
}
