package no.kvikshaug.gh

import scala.actors.Actor
import scala.actors.Actor._

import no.kvikshaug.scatsd.client.ScatsD

class JvmProfiler extends Actor {

  val sleepTime = 10 // seconds
  val runtime = Runtime.getRuntime
  val memory = java.lang.management.ManagementFactory.getMemoryMXBean
  val classes = java.lang.management.ManagementFactory.getClassLoadingMXBean

  def act {
    loop {
      val hUsage = memory.getHeapMemoryUsage
      val nhUsage = memory.getNonHeapMemoryUsage

      ScatsD.count("gh.jvm.memory.heap.used", hUsage.getUsed)
      ScatsD.count("gh.jvm.memory.heap.committed", hUsage.getCommitted)
      ScatsD.count("gh.jvm.memory.heap.max", hUsage.getMax)
      ScatsD.count("gh.jvm.memory.non-heap.used", nhUsage.getUsed)
      ScatsD.count("gh.jvm.memory.non-heap.committed", nhUsage.getCommitted)
      ScatsD.count("gh.jvm.memory.non-heap.max", nhUsage.getMax)

      ScatsD.count("gh.jvm.classes.loaded.current", classes.getLoadedClassCount)
      ScatsD.count("gh.jvm.classes.loaded.total", classes.getTotalLoadedClassCount)
      ScatsD.count("gh.jvm.classes.unloaded", classes.getUnloadedClassCount)

      Thread.sleep(sleepTime * 1000)
    }
  }
}

