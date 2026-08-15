package mrtjp.projectred.integration

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}

import org.junit.Assert.{assertEquals, assertSame}
import org.junit.Test

class LazyRendererSlotsTest {

  @Test
  def constructsSlotOnceOnConcurrentFirstAccess(): Unit = {
    val constructions = new AtomicInteger
    val slots = new LazyRendererSlots[String](Array(() => {
      constructions.incrementAndGet()
      new String("renderer")
    }))
    assertEquals(0, constructions.get())

    val start = new CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(8)
    try {
      val futures = (0 until 8).map { _ =>
        executor.submit(new Callable[String] {
          override def call(): String = {
            start.await()
            slots(0)
          }
        })
      }
      start.countDown()

      val expected = futures.head.get(5, TimeUnit.SECONDS)
      futures.tail.foreach(f =>
        assertSame(expected, f.get(5, TimeUnit.SECONDS))
      )
      assertEquals(1, constructions.get())
    } finally executor.shutdownNow()
  }

  @Test
  def replacementStaysLazyBeforeAndAfterAccess(): Unit = {
    val originalConstructions = new AtomicInteger
    val replacementConstructions = new AtomicInteger
    val slots = new LazyRendererSlots[String](Array(() => {
      originalConstructions.incrementAndGet()
      "original"
    }))

    slots.replace(
      0,
      () => {
        replacementConstructions.incrementAndGet()
        "first replacement"
      }
    )
    assertEquals(0, originalConstructions.get())
    assertEquals(0, replacementConstructions.get())
    assertEquals("first replacement", slots(0))
    assertEquals(1, replacementConstructions.get())

    slots.replace(
      0,
      () => {
        replacementConstructions.incrementAndGet()
        "second replacement"
      }
    )
    assertEquals(1, replacementConstructions.get())
    assertEquals("second replacement", slots(0))
    assertEquals(2, replacementConstructions.get())
  }
}
