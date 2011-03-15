package no.kvikshaug.gh

import no.kvikshaug.gh.exceptions.PreferenceNotSetException
import scalaj.collection.Imports._
import scala.collection.JavaConversions
import scala.xml._

object Config {
  implicit def wrapNodeSeq(ns: NodeSeq) = new NodeSeqWrapper(ns)
  val configFile = "props.xml"
  var root = XML.loadFile(configFile)
  def reparse = root = XML.loadFile(configFile)

  // General options
  def nicks = (root \ "Nicks" \ "Nick").map(_.text).asJava
  def channels = (root \ "Channels" \ "Channel").map(_.attribute("chan").get.text).asJava
  def servers = (root \ "Servers" \ "Server").map(_.text).asJava

  @throws(classOf[PreferenceNotSetException])
  def bitlyUser = {
    val e = (root \\ "BitLyUser").text
    if(e isEmpty) {
      throw new PreferenceNotSetException("No BitLyUser option specified in " + configFile)
    } else {
      e
    }
  }

  @throws(classOf[PreferenceNotSetException])
  def bitlyApiKey = {
    val e = (root \\ "BitLyApiKey").text
    if(e isEmpty) {
      throw new PreferenceNotSetException("No BitLyApiKey option specified in " + configFile)
    } else {
      e
    }
  }
  
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
  @throws(classOf[PreferenceNotSetException])
  def githubHookUrl = {
    val e = (root \\ "GithubHookUrl").text
    if(e isEmpty) {
      throw new PreferenceNotSetException("No GithubHookUrl option specified in " + configFile)
    } else {
      e
    }
  }
    @throws(classOf[PreferenceNotSetException])
    def githubHookPort = {
      val e = (root \\ "GithubHookPort").text
      if(e isEmpty) {
        throw new PreferenceNotSetException("No GithubHookPort option specified in " + configFile)
      } else {
        e.toInt
      }
    }

  /* Throws a PNSE if the node doesn't exist */
  def ifExists(ns: NodeSeq, message: String = "Missing corresponding option in " + configFile) = {
    if(ns isEmpty) { throw new PreferenceNotSetException(message) }
    else { ns }
  }

}

class NodeSeqWrapper(val value: NodeSeq) {
  // takes and calls a function with the NodeSeq as parameter which can return anything
  def get[A](fun: (NodeSeq) => A): A = fun(value)
}

