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

  // NOTE: A throw PNSE declaration requires the corresponding java code to catch the exception.
  // Only options that are OPTIONAL are declared to throw PNSE, because that case should
  // be handled by the corresponding module.
  // Options that are required aren't declared because if they aren't set, the program should
  // exit and it will because the unchecked exception will propagate like a RuntimeException,
  // and we won't have to handle it in the code using the option.

  // Required bot options
  def nicks = ifExists (root \ "Nicks") get ((ns) => (ns \ "Nick").map(_.text).asJava)
  def channels = ifExists (root \ "Channels") get ((ns) => (ns \ "Channel").map(_.attribute("chan").get.text).asJava)
  def servers = ifExists (root \ "Servers") get ((ns) => (ns \ "Server").map(_.text).asJava)

  // Bitly authentication
  @throws(classOf[PreferenceNotSetException])
  def bitlyUser = (ifExists (root \ "BitLyUser")) text
  @throws(classOf[PreferenceNotSetException])
  def bitlyApiKey = (ifExists (root \ "BitLyApiKey")) text

  // Upload module
  @throws(classOf[PreferenceNotSetException])
  def uploadDirs = ifExists (root \ "Channels") get {
    (ns) => (ns \ "Channel").map {
      (x) => ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "UploadDir").text)
    }.toMap.asJava
  }
  @throws(classOf[PreferenceNotSetException])
  def publicUrls = ifExists (root \ "Channels") get {
    (ns) => (ns \ "Channel").map {
      (x) => ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "PublicURL").text)
    }.toMap.asJava
  }

  // Operator module
  @throws(classOf[PreferenceNotSetException])
  def operatorList = ifExists (root \ "Channels") get {
    (ns) => (ns \ "Channel" toList).map {
      (x) => ((x \ "@chan").text -> (x \ "Modules" \ "Operator" \ "Nick").toList.map(_.text))
    }.toMap
  }

  // GithubPostReceiveServer
  @throws(classOf[PreferenceNotSetException])
  def githubHookUrl = (ifExists (root \ "GithubHookUrl")) text

  @throws(classOf[PreferenceNotSetException])
  def githubHookPort = (ifExists (root \ "GithubHookPort")).text.toInt

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

