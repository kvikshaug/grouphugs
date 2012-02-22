package no.kvikshaug.gh.modules

import scala.xml.XML
import scala.collection.JavaConversions._

import java.net.URL

import no.kvikshaug.gh.listeners.MessageListener
import no.kvikshaug.gh.util.Web
import no.kvikshaug.gh.{Grouphug, ModuleHandler}


class SpotifyLookup(val handler: ModuleHandler) extends MessageListener {

  handler.addMessageListener(this)
  handler.registerHelp("spotify", "The spotify URI catcher attemps lookup of spotify URIs and posts " + 
    "the name of the artist/song in the channel.")
  
  def onMessage(channel: String, sender: String, login: String, hostname: String, message: String) {

    val baseURL = "http://ws.spotify.com/lookup/1/.xml?uri="

    for(uri <- Web.findURIs("spotify:", message)) {
      try {
	val x = XML.load(new URL(baseURL + uri))
	
	// artist lookup
	if(uri.indexOf("spotify:artist:") != -1) {
	  Grouphug.getInstance.msg(channel, "Spotify: " + x.text.trim)
	}

	// album/track lookup
	else {
	  val title = (x \ "name").text.trim
	  val artist = (x \ "artist").map(_.text.trim).mkString(", ")
	
	  Grouphug.getInstance.msg(channel, "Spotify: %s - %s".format(artist, title))
	}
      } catch {
	case e: Exception =>
	  System.err.println("Spotify URI lookup failed: " + e.toString)
      }	
    }
  }

}
