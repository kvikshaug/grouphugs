package no.kvikshaug.gh.util

import java.net._

object StatsD {
  val host = InetAddress.getByName("kvikshaug.no")
  val port = 8125

  val random = new java.util.Random

  def timing(stat: String, time: String, sampleRate: Double = 1) = send(stat + ":" + time + "|ms", sampleRate)
  def increment(stat: String, sampleRate: Double = 1) = updateStats(stat, 1, sampleRate)
  def decrement(stat: String, sampleRate: Double = 1) = updateStats(stat, -1, sampleRate)

  def updateStats(stat: String, delta: Int, sampleRate: Double = 1) =
    send(stat + ":" + delta + "|c", sampleRate)

  def send(data: String, sampleRate: Double = 1): Unit = {
    if(sampleRate < 1 && sampleRate < random.nextDouble) {
      return
    }
    val socket = new DatagramSocket
    val payload = if(sampleRate == 1) {
      data.getBytes
    } else {
      (data + "|@" + sampleRate).getBytes
    }
    val packet = new DatagramPacket(payload, payload.length, host, port)
    socket.send(packet)
  }
}

