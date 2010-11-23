package no.kvikshaug.gh.modules

import java.io.{File, IOException}
import scala.xml._

import no.kvikshaug.gh.{Grouphug, ModuleHandler}
import no.kvikshaug.gh.listeners.{JoinListener, NickChangeListener}

import org.jibble.pircbot.User

class Operator(handler: ModuleHandler) extends JoinListener with NickChangeListener

