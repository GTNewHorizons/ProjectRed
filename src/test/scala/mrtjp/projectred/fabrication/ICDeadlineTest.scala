package mrtjp.projectred.fabrication

import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert._
import org.junit.Test

class ICDeadlineTest {
  @Test
  def deadlineSavesRemainingDelay(): Unit = {
    val deadline = newDeadline()
    deadline.set(1000, 20)
    val tag = new NBTTagCompound
    deadline.save(tag, 1005)

    assertEquals(15L, tag.getLong("delay"))
    assertFalse(tag.hasKey("legacy"))
  }

  @Test
  def loadedDeadlineAnchorsOnNewClock(): Unit = {
    val deadline = loaded("delay", 15)

    assertTrue(deadline.isSet)
    assertEquals(25L, deadline.deadline(10))
    assertFalse(deadline.isDue(10))
    assertFalse(deadline.isDue(24))
    assertTrue(deadline.isDue(25))
  }

  @Test
  def legacyDeadlineOnSameClockKeepsExactTick(): Unit = {
    val deadline = loaded("legacy", 1015)

    assertEquals(1015L, deadline.deadline(1000))
    assertFalse(deadline.isDue(1000))
    assertFalse(deadline.isDue(1014))
    assertTrue(deadline.isDue(1015))
  }

  @Test
  def legacyDeadlineInThePastIsDueNow(): Unit = {
    assertTrue(loaded("legacy", 500).isDue(1000))
  }

  @Test
  def legacyDeadlineBeyondMaxDelayIsDueNow(): Unit = {
    assertTrue(loaded("legacy", 1000000).isDue(10))
  }

  @Test
  def legacyDeadlineAtMaxDelayIsKept(): Unit = {
    val deadline = loaded("legacy", 10 + 256)

    assertFalse(deadline.isDue(10))
    assertTrue(deadline.isDue(266))
  }

  @Test
  def legacyClearedDeadlineStaysClear(): Unit = {
    val deadline = loaded("legacy", -1)

    assertFalse(deadline.isSet)
    assertEquals(-1L, deadline.remaining(10))
  }

  @Test
  def uncheckedLegacyDeadlineSavesWithoutClock(): Unit = {
    val deadline = loaded("legacy", 1015)
    val tag = new NBTTagCompound
    deadline.save(tag, throw new AssertionError("saving read the clock"))

    assertEquals(1015L, tag.getLong("legacy"))
    assertFalse(tag.hasKey("delay"))
  }

  @Test
  def legacyDeadlineWithSaveTickKeepsRemainingDelay(): Unit = {
    val tag = new NBTTagCompound
    tag.setLong("legacy", 1002)
    val deadline = newDeadline()
    deadline.load(tag, legacySavedAt = 1000)

    assertEquals(2L, deadline.remaining(throw new AssertionError("read clock")))
    assertFalse(deadline.isDue(900))
    assertFalse(deadline.isDue(901))
    assertTrue(deadline.isDue(902))
  }

  @Test
  def legacyDeadlineWithSaveTickBeyondMaxDelayIsDueNow(): Unit = {
    val tag = new NBTTagCompound
    tag.setLong("legacy", 5500)
    val deadline = newDeadline()
    deadline.load(tag, legacySavedAt = 5000)

    assertTrue(deadline.isDue(10))
  }

  @Test
  def legacyTimerPulseUsesSaveTick(): Unit = {
    val (tag, pulseTick) = midPulseTimer()
    tag.removeTag("schedDelay")
    tag.setLong("schedTime", pulseTick + 2)
    tag.setLong("tsave", pulseTick)
    val placed = new Board
    val part = placed.loadTimer(tag)
    placed.attach(pulseTick - 100)

    assertEquals(2, ticksUntil(placed, 200)(part.update())(!part.outputHigh))
  }

  @Test
  def pulseSavedMidwayEndsInYoungerWorld(): Unit = {
    val (tag, _) = midPulseTimer()
    val placed = new Board
    val part = placed.loadTimer(tag)
    placed.attach(10)

    assertTrue(part.outputHigh)
    assertEquals(2, ticksUntil(placed, 100)(part.update())(!part.outputHigh))
  }

  @Test
  def legacyPulseOnSameClockKeepsTiming(): Unit = {
    val (tag, pulseTick) = midPulseTimer()
    tag.removeTag("schedDelay")
    tag.setLong("schedTime", pulseTick + 2)
    val placed = new Board
    val part = placed.loadTimer(tag)
    placed.attach(pulseTick)

    assertEquals(2, ticksUntil(placed, 100)(part.update())(!part.outputHigh))
  }

  @Test
  def legacyPulseFromOtherClockEndsOnFirstTick(): Unit = {
    val (tag, _) = midPulseTimer()
    tag.removeTag("schedDelay")
    tag.setLong("schedTime", 1000000)
    val placed = new Board
    val part = placed.loadTimer(tag)
    placed.attach(10)

    assertEquals(0, ticksUntil(placed, 100)(part.update())(!part.outputHigh))
  }

  @Test
  def buttonSavedPressedDepressesInYoungerWorld(): Unit = {
    val placed = new Board
    val button = loadButton(placed, pressedButton())
    placed.attach(10)

    assertTrue(button.on)
    assertEquals(15, ticksUntil(placed, 100)(button.update())(!button.on))
  }

  @Test
  def legacyButtonOnSameClockKeepsTiming(): Unit = {
    val tag = pressedButton()
    tag.removeTag("schedDelay")
    tag.setLong("sched", 1020)
    val placed = new Board
    val button = loadButton(placed, tag)
    placed.attach(1005)

    assertEquals(15, ticksUntil(placed, 100)(button.update())(!button.on))
  }

  private def newDeadline() = new TickDeadline(256, "delay", "legacy")

  private def loaded(key: String, value: Long): TickDeadline = {
    val tag = new NBTTagCompound
    tag.setLong(key, value)
    val deadline = newDeadline()
    deadline.load(tag)
    deadline
  }

  /** A Timer saved on its pulse tick, and that tick. */
  private def midPulseTimer(): (NBTTagCompound, Long) = {
    val board = new Board
    board.attach(100000)
    val part = board.newTimer()
    assertTrue(board.ticksUntilPulse(part, 100) >= 0)
    val tag = new NBTTagCompound
    part.save(tag)
    (tag, board.world.time)
  }

  /** A button pressed at tick 1000 and saved at 1005. */
  private def pressedButton(): NBTTagCompound = {
    val board = new Board
    board.attach(1000)
    val button = new ButtonICPart
    button.bind(board.ic, 2, 2)
    button.press()
    board.world.time += 5
    val tag = new NBTTagCompound
    button.save(tag)
    tag
  }

  private def loadButton(board: Board, tag: NBTTagCompound): ButtonICPart = {
    val button = new ButtonICPart
    button.bind(board.ic, 2, 2)
    button.load(tag)
    button
  }

  // ticks until done holds; -1 if it never does
  private def ticksUntil(board: Board, limit: Int)(tick: => Unit)(
      done: => Boolean
  ): Int = {
    for (i <- 0 until limit) {
      tick
      if (done) return i
      board.world.time += 1
    }
    -1
  }
}
