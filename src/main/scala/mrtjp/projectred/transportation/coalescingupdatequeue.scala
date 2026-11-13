package mrtjp.projectred.transportation

import java.util.IdentityHashMap
import java.util.concurrent.PriorityBlockingQueue

/** One queued or running update per identity; retain the newest requested
  * version.
  */
private[transportation] final class CoalescingUpdateQueue[K <: AnyRef](
    versionOf: K => Int,
    work: (K, Int) => Runnable,
    priority: (K, Int, K, Int) => Int
) {
  private val pending = new IdentityHashMap[K, Job]()
  private val queue = new PriorityBlockingQueue[Job]()

  private class Job(val key: K, val version: Int)
      extends Runnable
      with Comparable[Job] {
    var requestedVersion =
      version // Access only while holding pending's monitor.
    val task = work(key, version)
    override def compareTo(that: Job): Int =
      priority(key, version, that.key, that.version)
    override def run(): Unit = try task.run()
    finally
      pending.synchronized {
        if (pending.get(key) eq this) {
          pending.remove(key)
          if (requestedVersion != version) enqueue(key, requestedVersion)
        }
      }
  }

  private def enqueue(key: K, version: Int): Unit = {
    val job = new Job(key, version)
    pending.put(key, job)
    queue.add(job)
  }

  def add(key: K): Unit = pending.synchronized {
    val version = versionOf(key)
    val old = pending.get(key)
    if (old == null) enqueue(key, version)
    else if (old.requestedVersion != version) {
      old.requestedVersion = version
      // Do not change priority fields in-place in the heap. A running job will
      // enqueue the newest requested version from its completion handler.
      if (queue.remove(old)) {
        pending.remove(key)
        enqueue(key, version)
      }
    }
  }

  def take(): Runnable = queue.take()
  def size: Int = queue.size()

  def remove(task: Runnable): Boolean = pending.synchronized {
    val entries = pending.entrySet().iterator()
    while (entries.hasNext) {
      val job = entries.next().getValue
      if (((job eq task) || (job.task eq task)) && queue.remove(job)) {
        entries.remove()
        return true
      }
    }
    false
  }
}
