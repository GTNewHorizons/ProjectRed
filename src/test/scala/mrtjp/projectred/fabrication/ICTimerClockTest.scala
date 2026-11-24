package mrtjp.projectred.fabrication

import codechicken.lib.packet.PacketCustom
import mrtjp.core.vec.Size
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.profiler.Profiler
import net.minecraft.world.storage.ISaveHandler
import net.minecraft.world.{World, WorldProvider, WorldSettings}
import org.junit.Assert._
import org.junit.Test

class ICTimerClockTest {
  @Test
  def placementResumesSavedCountdown(): Unit = {
    val tag = savedTimer(savedAt = 100, elapsed = 10)
    val placed = new Board
    val part = placed.loadTimer(tag)
    placed.attach(200)

    assertEquals(10, part.timer.pointerValue)
    assertEquals(28, placed.ticksUntilPulse(part, 100))
  }

  @Test
  def placementInYoungerWorldResumesCountdown(): Unit = {
    val placed = new Board
    val part = placed.loadTimer(savedTimer(savedAt = 100, elapsed = 10))
    placed.attach(5)

    assertTrue(part.timer.isRunning)
    assertEquals(10, part.timer.pointerValue)
    assertEquals(28, placed.ticksUntilPulse(part, 100))
  }

  @Test
  def poweredPlacementStaysStopped(): Unit = {
    val placed = new Board
    val part = placed.loadTimer(savedTimer(savedAt = 100, elapsed = 10))
    placed.attach(200)
    part.powered = true
    part
      .onChange() // placement propagates neighbour power before the first tick

    assertFalse(part.timer.isRunning)
    assertEquals(-1, placed.ticksUntilPulse(part, 100))
  }

  @Test
  def saveBeforeAttachKeepsElapsed(): Unit = {
    val placed = new Board
    val part = placed.loadTimer(savedTimer(savedAt = 100, elapsed = 10))
    val tag = new NBTTagCompound
    part.save(tag)

    assertEquals(10L, tag.getLong("pelapsed"))
  }

  @Test
  def stoppedTimerStaysStoppedAfterLoad(): Unit = {
    val placed = new Board
    val part = placed.loadTimer(savedTimer(savedAt = 100, elapsed = -1))
    placed.attach(200)

    assertFalse(part.timer.isRunning)
    assertEquals(0, part.timer.pointerValue)
  }

  @Test
  def detachedCircuitReportsSavedElapsed(): Unit = {
    val ic = new IntegratedCircuit
    ic.size = Size(16, 16)
    val part = new TimerPart
    part.bind(ic, 1, 1)
    part.load(savedTimer(savedAt = 100, elapsed = 10))

    assertEquals(10, part.timer.pointerValue)
  }

  @Test
  def descRebasesOnReceiverClock(): Unit = {
    val server = new Board
    server.attach(1000)
    val sent = server.newTimer()
    server.world.time += 10

    val out = new PacketCustom("ICTimerClockTest", 1)
    sent.writeDesc(out)
    val client = new Board
    client.attach(50)
    val received = client.newTimer()
    received.readDesc(new PacketCustom(out.getByteBuf))

    assertEquals(10, received.timer.pointerValue)
    client.world.time += 5
    assertEquals(15, received.timer.pointerValue)
  }

  private def savedTimer(savedAt: Long, elapsed: Int): NBTTagCompound = {
    val board = new Board
    board.attach(savedAt - math.max(elapsed, 0))
    val part = board.newTimer()
    if (elapsed < 0) part.timer.resetPointer()
    board.world.time = savedAt
    val tag = new NBTTagCompound
    part.save(tag)
    tag
  }
}

private class TimerPart extends SequentialGateICPart {
  preparePlacement(0, ICGateDefinition.Timer.ordinal)
  assertLogic()

  var powered = false

  def timer = getLogic[Timer]
  def outputHigh = (state >> 4) != 0

  override def getRedstoneInput(r: Int) = if (powered && r == 2) 255 else 0
}

private class Board extends SimulatedWorldCircuit {
  val ic = new IntegratedCircuit
  ic.size = Size(16, 16)
  ic.network = this
  var world: ClockWorld = null

  override def getIC = ic
  override def getWorld: World = world
  override def markSave(): Unit = {}

  def attach(time: Long): Unit = {
    world = ClockWorld(time)
  }

  def newTimer(): TimerPart = {
    val part = new TimerPart
    part.bind(ic, 1, 1)
    part.timer.startPointer()
    part
  }

  def loadTimer(tag: NBTTagCompound): TimerPart = {
    val part = new TimerPart
    part.bind(ic, 1, 1)
    part.load(tag)
    part
  }

  // ticks until the output goes high; -1 if it never does
  def ticksUntilPulse(part: TimerPart, limit: Int): Int = {
    for (i <- 0 until limit) {
      part.update()
      if (part.outputHigh) return i
      world.time += 1
    }
    -1
  }
}

private class ClockWorld
    extends World(
      null: ISaveHandler,
      null: String,
      null: WorldProvider,
      null: WorldSettings,
      null: Profiler
    ) {
  var time = 0L
  override def getTotalWorldTime = time
  override protected def createChunkProvider() = null
  override protected def func_152379_p() = 0
  override def getEntityByID(id: Int): Entity = null
}

private object ClockWorld {
  private val unsafe = {
    val f = classOf[sun.misc.Unsafe].getDeclaredField("theUnsafe")
    f.setAccessible(true)
    f.get(null).asInstanceOf[sun.misc.Unsafe]
  }

  def apply(time: Long): ClockWorld = {
    // World's constructor needs a real save handler and provider; skip it
    val world =
      unsafe.allocateInstance(classOf[ClockWorld]).asInstanceOf[ClockWorld]
    world.time = time
    world
  }
}
