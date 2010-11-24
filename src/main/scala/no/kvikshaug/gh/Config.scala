package no.kvikshaug.gh

import scalaj.collection.Imports._
import scala.collection.JavaConversions
import scala.xml._

object Config {
  val configFile = "props.xml"
  val root = XML.loadFile(configFile)

  val nicks = (root \ "Nicks" \ "Nick").map(_.text).asJava
}
