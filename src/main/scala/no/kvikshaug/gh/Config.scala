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
  def nicks = ifExists (root \ "Nicks") get ((ns) => (ns \ "Nick").map(_.text).asJava)
  def channels = ifExists (root \ "Channels") get ((ns) => (ns \ "Channel").map(_.attribute("chan").get.text).asJava)
  def servers = ifExists (root \ "Servers") get ((ns) => (ns \ "Server").map(_.text).asJava)

  @throws(classOf[PreferenceNotSetException])
  def bitlyUser = (ifExists (root \\ "BitLyUser")) text
  @throws(classOf[PreferenceNotSetException])
  def bitlyApiKey = (ifExists (root \\ "BitLyApiKey")) text

  // Upload module
  def uploadDirs = ifExists (root \ "Channels") get {
    (ns) => (ns \ "Channel").map {
      (x) => ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "UploadDir").text)
    }.toMap.asJava
  }
  def publicUrls = ifExists (root \ "Channels") get {
    (ns) => (ns \ "Channel").map {
      (x) => ((x \ "@chan").text -> (x \ "Modules" \ "Upload" \ "PublicURL").text)
    }.toMap.asJava
  }

  // Operator module
  def operatorList = ifExists (XML.loadFile(Grouphug.configFile) \ "Channels") get {
    (ns) => (ns \ "Channel" toList).map {
      (x) => ((x \ "@chan").text -> (x \ "Modules" \ "Operator" \ "Nick").toList.map(_.text))
    }.toMap
  }

  // GithubPostReceiveServer
  @throws(classOf[PreferenceNotSetException])
  def githubHookUrl = (ifExists (root \\ "GithubHookUrl")) text

    @throws(classOf[PreferenceNotSetException])
    def githubHookPort = (ifExists (root \\ "GithubHookPort")).text.toInt

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

