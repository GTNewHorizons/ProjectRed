package mrtjp.projectred.fabrication

import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert._
import org.junit.Test

class ICDeadlineTest {
  @Test
  def deadlineSavesRemainingDelay(): Unit = {
    val deadline = new TickDeadline
    deadline.set(1000, 20)
    val tag = new NBTTagCompound
    deadline.save(tag, "delay", 1005)

    assertEquals(15L, tag.getLong("delay"))
  }

  @Test
  def loadedDeadlineAnchorsOnNewClock(): Unit = {
    val tag = new NBTTagCompound
    tag.setLong("delay", 15)
    val deadline = new TickDeadline
    deadline.load(tag, "delay", "legacy")

    assertTrue(deadline.isSet)
    assertEquals(25L, deadline.deadline(10))
    assertFalse(deadline.isDue(10))
    assertFalse(deadline.isDue(24))
    assertTrue(deadline.isDue(25))
  }

  @Test
  def legacyAbsoluteDeadlineIsDueOnFirstCheck(): Unit = {
    val tag = new NBTTagCompound
    tag.setLong("legacy", 1000000)
    val deadline = new TickDeadline
    deadline.load(tag, "delay", "legacy")

    assertTrue(deadline.isDue(10))
  }

  @Test
  def legacyClearedDeadlineStaysClear(): Unit = {
    val tag = new NBTTagCompound
    tag.setLong("legacy", -1)
    val deadline = new TickDeadline
    deadline.load(tag, "delay", "legacy")

    assertFalse(deadline.isSet)
    assertEquals(-1L, deadline.remaining(10))
  }

  @Test
  def pulseSavedMidwayEndsInYoungerWorld(): Unit = {
    val placed = new Board
    val part = placed.loadTimer(midPulseTimer())
    placed.attach(10)

    assertTrue(part.outputHigh)
    assertEquals(2, ticksUntil(placed, 100)(part.update())(!part.outputHigh))
  }

  @Test
  def legacyPulseDeadlineEndsOnFirstTick(): Unit = {
    val tag = midPulseTimer()
    tag.removeTag("schedDelay")
    tag.setLong("schedTime", 1000000)
    val placed = new Board
    val part = placed.loadTimer(tag)
    placed.attach(10)

    assertEquals(0, ticksUntil(placed, 100)(part.update())(!part.outputHigh))
  }

  @Test
  def buttonSavedPressedDepressesInYoungerWorld(): Unit = {
    val source = new Board
    source.attach(1000)
    val pressed = newButton(source)
    pressed.press()
    source.world.time += 5
    val tag = new NBTTagCompound
    pressed.save(tag)

    val placed = new Board
    val button = new ButtonICPart
    button.bind(placed.ic, 2, 2)
    button.load(tag)
    placed.attach(10)

    assertTrue(button.on)
    assertEquals(15, ticksUntil(placed, 100)(button.update())(!button.on))
  }

  private def midPulseTimer(): NBTTagCompound = {
    val board = new Board
    board.attach(100000)
    val part = board.newTimer()
    assertTrue(board.ticksUntilPulse(part, 100) >= 0)
    val tag = new NBTTagCompound
    part.save(tag)
    tag
  }

  private def newButton(board: Board): ButtonICPart = {
    val button = new ButtonICPart
    button.bind(board.ic, 2, 2)
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
