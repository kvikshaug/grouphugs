package no.kvikshaug.gh.util

import java.net._

import no.kvikshaug.gh.Config

object StatsD {
  var host: Option[InetAddress] = None
  var port: Option[Int] = None
  try {
    host = Some(InetAddress.getByName(Config.statsDHost))
    port = Some(Config.statsDPort)
  } catch {
    case e => println("StatsD reporter disabled: " + e.getMessage)
  }

  // wrapper methods for java
  def count(stat: String, value: Double): Unit = count(stat, value, 0)
  def time(stat: String, value: Double): Unit = count(stat, value, 0)

  def count(stat: String, value: Double, interval: Double = 0) = send(format("%s|%s|%s|count", stat, value, interval))
  def retain(stat: String, value: Double) = send(format("%s|%s|0|retain", stat, value))
  def time(stat: String, value: Double, interval: Double = 0) = send(format("%s|%s|%s|time", stat, value, interval))

  def send(data: String): Unit = {
    // this method may be called from everywhere we want to measure a metric in the code,
    // so we'll have to check if the config parameters weren't set each time we're called
    if(host.isEmpty || port.isEmpty) {
      return
    }
    val socket = new DatagramSocket
    val payload = data.getBytes
    val packet = new DatagramPacket(payload, payload.length, host.get, port.get)
    socket.send(packet)
  }
}

