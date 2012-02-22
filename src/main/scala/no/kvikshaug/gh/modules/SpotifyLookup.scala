package no.kvikshaug.gh.modules

import scala.xml.XML
import scala.collection.JavaConversions._

import java.net.URL

import no.kvikshaug.gh.listeners.MessageListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}


class SpotifyLookup(val handler: ModuleHandler) extends MessageListener {

  handler.addMessageListener(this)
//  handler.registerHelp("spotify", "")
  
  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {

    val baseURL = "http://ws.spotify.com/lookup/1/.xml?uri="

    // should this preferably be done async in its own thread ... ?
    for(uri <- Web.findURIs("spotify:", message)) {
      try {
	val x = XML.load(new URL(baseURL + uri))
	
	val title = (x \ "name").text.trim
	val artist = (x \ "artist").text.trim
	
	Grouphug.getInstance.msg(channel, "Spotify track lookup: %s - %s".format(artist, title))
      } catch {
	case e: Exception =>
	  // unable to lookup track
      }	
    }
  }

}
