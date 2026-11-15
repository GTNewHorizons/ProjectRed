package mrtjp.projectred.transportation

import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.junit.Assert._
import org.junit.Test

class PerformanceFollowupsTest {
  @Test def growthPreservesOldPolicyAndSentinels(): Unit = {
    for (index <- 0 to 10000 by 97) {
      val input = Array[AnyRef]("saved", null)
      val out = RouterArrayGrowth.references(input, index)
      var expected = input
      if (expected.length <= index)
        while (expected.length <= (index * 1.5).toInt + 1) expected :+= null
      assertArrayEquals(expected, out)
      val versions = RouterArrayGrowth.versions(Array(7, -1), index)
      assertEquals(out.length, versions.length)
      assertEquals(7, versions(0))
      assertTrue(versions.drop(1).forall(_ == -1))
    }
  }

  private def filter(color: Int): PathFilter = {
    val f = new PathFilter
    f.colors = color
    f
  }

  @Test def equalFiltersDeduplicateInHashTrieAndRemainStable(): Unit = {
    val filters = (1 to 8).map(filter).toSet
    val copy = filter(5)
    assertTrue(filters.contains(copy))
    assertEquals(8, (filters + copy).size)
    val path = new Path(filters)
    assertEquals(filters.hashCode, path.filters.hashCode)
    val published = path.filters.head
    val hash = published.hashCode
    val changes = Seq[() => Unit](
      () => published.colors = 99,
      () => published.pathFlags = 0,
      () => published.filterExclude = false,
      () => published.itemFilter = Set.empty,
      () => published.colorExclude = false
    )
    changes.foreach { change =>
      try { change(); fail("Published filter was mutable") }
      catch { case _: IllegalStateException => () }
      assertEquals(hash, published.hashCode)
    }
    val a = filter(5)
    a.colorExclude = false
    a.pathFlags = 1
    val p = new Path(Set(a))
    assertTrue(p.allowColor(0)); assertFalse(p.allowColor(1));
    assertTrue(p.allowColor(2))
    assertTrue(p.allowColor(-1)); assertTrue(p.flagRouteTo);
    assertFalse(p.flagRouteFrom)
  }

  private class Key { @volatile var version = 1 }
  private def queue(action: (Key, Int) => Unit) =
    new CoalescingUpdateQueue[Key](
      _.version,
      (k, v) => new Runnable { def run(): Unit = action(k, v) },
      (_, a, _, b) => java.lang.Integer.compare(b, a)
    )

  @Test def queuedUpdatesAreCoalescedAndReplacedWithLatest(): Unit = {
    var ran = Vector.empty[Int]
    val q = queue((_, v) => ran :+= v)
    val key = new Key
    for (_ <- 1 to 1000) q.add(key)
    assertEquals(1, q.size)
    key.version = 2; q.add(key)
    key.version = 3; q.add(key)
    assertEquals(1, q.size)
    q.take().run()
    assertEquals(Vector(3), ran)
    assertEquals(0, q.size)
    q.add(key)
    val cancelled = q.take()
    // Taken/running jobs cannot be cancelled by removing a queued task.
    assertFalse(q.remove(cancelled))
    cancelled.run()
    q.add(key)
    assertEquals(1, q.size)
  }

  @Test def inFlightUpdateRetainsNewestVersion(): Unit = {
    val entered = new CountDownLatch(1)
    val release = new CountDownLatch(1)
    val ran = new java.util.concurrent.CopyOnWriteArrayList[Integer]()
    val q = queue { (_, v) =>
      ran.add(v)
      if (v == 1) {
        entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS))
      }
    }
    val key = new Key
    q.add(key)
    val worker = new Thread(new Runnable { def run(): Unit = q.take().run() })
    worker.start()
    try {
      assertTrue(entered.await(5, TimeUnit.SECONDS))
      for (v <- 2 to 1000) { key.version = v; q.add(key) }
      assertEquals(0, q.size)
    } finally { release.countDown(); worker.join(5000) }
    assertFalse(worker.isAlive)
    assertEquals(1, q.size)
    q.take().run()
    assertEquals(java.util.Arrays.asList[Integer](1, 1000), ran)
    assertEquals(0, q.size)
  }

  @Test def failedUpdatesReleaseTheirPendingIdentity(): Unit = {
    val q = queue((_, _) => throw new IllegalStateException("test"))
    val a = new Key; val b = new Key
    q.add(a); q.add(b)
    assertEquals(2, q.size)
    try q.take().run()
    catch { case _: IllegalStateException => () }
    q.add(a); q.add(b)
    assertEquals(2, q.size)
  }
  @Test def fourWorkersDoNotLoseUpdatesOrRunSameKeyConcurrently(): Unit = {
    val keys = Vector.fill(24)(new Key)
    val completed = new CountDownLatch(keys.size)
    val errors = new java.util.concurrent.ConcurrentLinkedQueue[Throwable]()
    val active = new java.util.concurrent.ConcurrentHashMap[
      Key,
      java.util.concurrent.atomic.AtomicInteger
    ]()
    keys.foreach(k =>
      active.put(k, new java.util.concurrent.atomic.AtomicInteger())
    )
    val q = queue { (key, version) =>
      try {
        assertEquals(
          "Concurrent jobs for one router",
          1,
          active.get(key).incrementAndGet()
        )
        Thread.`yield`()
        if (version == 2000) completed.countDown()
      } catch { case t: Throwable => errors.add(t) }
      finally active.get(key).decrementAndGet()
    }
    val workers = Vector.fill(4)(new Thread(new Runnable {
      def run(): Unit = try { while (true) q.take().run() }
      catch { case _: InterruptedException => () }
    }))
    workers.foreach(_.start())
    try {
      for (version <- 1 to 2000; key <- keys) {
        key.version = version
        q.add(key)
      }
      assertTrue(
        "Newest updates were lost",
        completed.await(10, TimeUnit.SECONDS)
      )
      assertTrue(errors.toString, errors.isEmpty)
    } finally {
      workers.foreach(_.interrupt())
      workers.foreach(_.join(5000))
    }
    assertTrue(workers.forall(!_.isAlive))
    assertEquals(0, q.size)
  }

  @Test def removalClearsIdentityAndAllowsReschedule(): Unit = {
    var task: Runnable = null
    var executions = 0
    val q = new CoalescingUpdateQueue[Key](
      _.version,
      (_, _) => {
        task = new Runnable { def run(): Unit = { executions += 1 } }; task
      },
      (_, _, _, _) => 0
    )
    val key = new Key
    q.add(key)
    assertTrue(q.remove(task))
    assertEquals(0, q.size)
    q.add(key)
    q.take().run()
    assertEquals(1, executions)
  }

}
