package mrtjp.projectred.fabrication

import codechicken.lib.packet.PacketCustom
import mrtjp.core.vec.Size
import org.junit.Assert._
import org.junit.Test

class ICRedrawTest {
  @Test
  def acceptsEveryGuiBoardSizeAndClearsTheOldCircuit(): Unit = {
    for (width <- 16 to 64 by 16; height <- 16 to 64 by 16) {
      val circuit = existingCircuit()
      val oldPart = circuit.getPart(1, 1)
      val redraw = new IntegratedCircuit
      redraw.name = "new circuit"
      redraw.size = Size(width, height)

      assertTrue(circuit.readRedraw(packet(redraw)))
      assertEquals(redraw.name, circuit.name)
      assertEquals(redraw.size, circuit.size)
      assertTrue(circuit.parts.isEmpty)
      assertTrue(circuit.iostate.forall(_ == 0))
      assertNull(oldPart.world)
    }
  }

  @Test
  def rejectsUploadedZeroPeriodSequencer(): Unit = {
    val upload = new IntegratedCircuit
    upload.size = Size(16, 16)
    val sequencer = new SequentialGateICPart
    sequencer.preparePlacement(0, ICGateDefinition.Sequencer.ordinal)
    sequencer.assertLogic()
    sequencer.getLogic[Sequencer].pointer_max = 0
    sequencer.bind(upload, 1, 1)
    upload.parts((1, 1)) = sequencer

    assertRejected(packet(upload))
  }

  @Test
  def rejectsSimulationState(): Unit = {
    for (side <- 0 until 4; state <- Seq(1, 0x10000, -1)) {
      val upload = new IntegratedCircuit
      upload.size = Size(16, 16)
      upload.iostate(side) = state
      assertRejected(packet(upload))
    }
  }

  @Test
  def rejectsUnsupportedBoardSizes(): Unit = {
    for (dimension <- Seq(0, 1, 15, 17, 63, 65, 80, 255)) {
      for (size <- Seq(Size(dimension, 16), Size(16, dimension))) {
        val upload = new IntegratedCircuit
        upload.size = size
        assertRejected(packet(upload))
      }
    }
  }

  @Test
  def rejectsMalformedNameLengthWithoutClearingTheCircuit(): Unit = {
    val out = new PacketCustom("ICRedrawTest", 1)
    out.writeByte(0x80).writeByte(0x80).writeByte(0)
    assertRejected(new PacketCustom(out.getByteBuf))
  }

  @Test
  def rejectsTruncatedPacketsWithoutClearingTheCircuit(): Unit = {
    val redraw = new IntegratedCircuit
    redraw.size = Size(16, 16)
    val length = packet(redraw).getByteBuf.writerIndex()
    for (end <- 1 until length) {
      val truncated = packet(redraw)
      truncated.getByteBuf.writerIndex(end)
      assertRejected(truncated)
    }
  }

  private def assertRejected(in: PacketCustom): Unit = {
    val circuit = existingCircuit()
    val oldPart = circuit.getPart(1, 1)
    assertFalse(circuit.readRedraw(in))
    assertEquals("existing", circuit.name)
    assertEquals(Size(32, 32), circuit.size)
    assertEquals(1, circuit.parts.size)
    assertSame(oldPart, circuit.getPart(1, 1))
    assertSame(circuit, oldPart.world)
    assertArrayEquals(Array(1, 2, 3, 4), circuit.iostate)
  }

  private def existingCircuit(): IntegratedCircuit = {
    val circuit = new IntegratedCircuit
    circuit.name = "existing"
    circuit.size = Size(32, 32)
    for (side <- 0 until 4) circuit.iostate(side) = side + 1
    val part = new LeverICPart
    part.bind(circuit, 1, 1)
    circuit.parts((1, 1)) = part
    circuit
  }

  private def packet(circuit: IntegratedCircuit): PacketCustom = {
    val out = new PacketCustom("ICRedrawTest", 1)
    circuit.writeDesc(out)
    new PacketCustom(out.getByteBuf)
  }
}
