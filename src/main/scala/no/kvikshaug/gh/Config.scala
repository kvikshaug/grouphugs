package no.kvikshaug.gh

import scala.xml._

object Config {
  val configFile = "props.xml"
  val root = XML.loadFile(configFile)


}
