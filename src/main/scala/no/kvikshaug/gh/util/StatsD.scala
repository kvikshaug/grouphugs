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

  val random = new java.util.Random

  def timing(stat: String, time: Double): Unit = timing(stat, time, 1)
  def timing(stat: String, time: Double, sampleRate: Double = 1) = send(stat + ":" + time + "|ms", sampleRate)

  def increment(stat: String): Unit = increment(stat, 1)
  def increment(stat: String, sampleRate: Double = 1) = updateStats(stat, 1, sampleRate)

  def decrement(stat: String): Unit = decrement(stat, 1)
  def decrement(stat: String, sampleRate: Double = 1) = updateStats(stat, -1, sampleRate)

  def updateStats(stat: String, delta: Double): Unit = updateStats(stat, delta, 1)
  def updateStats(stat: String, delta: Double, sampleRate: Double = 1) =
    send(stat + ":" + delta + "|c", sampleRate)

  def send(data: String, sampleRate: Double = 1): Unit = {
    // this method may be called from everywhere we want to measure a metric in the code,
    // so we'll have to check if the config parameters weren't set each time we're called
    if(host.isEmpty || port.isEmpty) {
      return
    }
    if(sampleRate < 1 && sampleRate < random.nextDouble) {
      return
    }
    val socket = new DatagramSocket
    val payload = if(sampleRate == 1) {
      data.getBytes
    } else {
      (data + "|@" + sampleRate).getBytes
    }
    val packet = new DatagramPacket(payload, payload.length, host.get, port.get)
    socket.send(packet)
  }
}

