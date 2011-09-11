package no.kvikshaug.gh

import no.kvikshaug.gh.exceptions.PreferenceNotSetException

import scala.collection.JavaConverters._
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

  // ScatsD settings
  @throws(classOf[PreferenceNotSetException])
  def scatsDHost = (ifExists (root \ "ScatsD" \ "Host")) text
  @throws(classOf[PreferenceNotSetException])
  def scatsDPort = (ifExists (root \ "ScatsD" \ "Port")).text.toInt

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

  @throws(classOf[PreferenceNotSetException])
  def githubHookUsername(channel: String): String = {
    var username = ""
    for(chan <- (root \ "Channels" \ "Channel") if((chan \\ "@chan").text == channel)) {
      username = (ifExists (chan \ "Modules" \ "GithubHook" \ "Username")).text
    }
    username
  }

  @throws(classOf[PreferenceNotSetException])
  def githubHookPassword(channel: String): String = {
    var password = ""
    for(chan <- (root \ "Channels" \ "Channel") if((chan \\ "@chan").text == channel)) {
      password = (ifExists (chan \ "Modules" \ "GithubHook" \ "Password")).text
    }
    password
  }

  // Interface hostname
  @throws(classOf[PreferenceNotSetException])
  def interfaceHost = (ifExists (root \ "InterfaceHost")) text

  // Database
  @throws(classOf[PreferenceNotSetException])
  def dbFile = ifExists (root \ "Database") get ((ns) => (ns \ "File")) text

  /* Throws a PNSE if the node doesn't exist */
  def ifExists(ns: NodeSeq, message: String = "Missing corresponding option in " + configFile) = {
    if(ns.isEmpty || ns.text.isEmpty) { throw new PreferenceNotSetException(message) }
    else { ns }
  }

}

class NodeSeqWrapper(val value: NodeSeq) {
  // takes and calls a function with the NodeSeq as parameter which can return anything
  def get[A](fun: (NodeSeq) => A): A = fun(value)
}

