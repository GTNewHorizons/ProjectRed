package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.nbt.NBTTagCompound

abstract class ArrayGateICLogicCrossing(gate: ArrayGateICPart)
    extends ArrayGateICLogic(gate) {
  var signal1: Byte = 0
  var signal2: Byte = 0

  override def redwireMask(shape: Int) = 0xf

  override def propogationMask(r: Int) = if (r % 2 == 0) 0x5 else 0xa

  override def inputMask(shape: Int) = 0xf

  override def outputMask(shape: Int) = 0xf

  override def getSignal(mask: Int) =
    (if (mask == 0x5) signal1 else signal2) & 0xff

  override def setSignal(mask: Int, signal: Int) {
    if (mask == 0x5) signal1 = signal.toByte else signal2 = signal.toByte
  }

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("s1", signal1)
    tag.setByte("s2", signal2)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    signal1 = tag.getByte("s1")
    signal2 = tag.getByte("s2")
  }

  override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    packet.writeByte(signal1)
    packet.writeByte(signal2)
  }

  override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    signal1 = packet.readByte()
    signal2 = packet.readByte()
  }

  override def read(packet: MCDataInput, key: Int) = key match {
    case 11 =>
      signal1 = packet.readByte()
      signal2 = packet.readByte()
    case _ =>
  }

  def sendSignalUpdate() {
    gate.writeStreamOf(11).writeByte(signal1).writeByte(signal2)
  }

  override def onChange(gate: ArrayGateICPart) {
    val oldSignal = (gate.state & 1) != 0
    val newSignal = signal1 != 0

    if (oldSignal != newSignal) {
      gate.setState(gate.state & 2 | (if (newSignal) 1 else 0))
      gate.onInputChange()
      gate.scheduleTick(0)
    }
  }

  override def scheduledTick(gate: ArrayGateICPart) {
    val input = (gate.state & 1) != 0
    val oldOutput = (gate.state & 2) != 0
    val newOutput = !input

    if (oldOutput != newOutput) {
      gate.setState(gate.state & 1 | (if (newOutput) 2 else 0))
      gate.onOutputChange(0)
      gate.onChange()
    }
  }

  override def onSignalUpdate() {
    sendSignalUpdate()
  }

  override def overrideSignal(mask: Int) = if (mask == 0xa) powerUp else false

  override def calculateSignal(mask: Int) = 255

  def powerUp: Boolean

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: ArrayGateICPart, detailLevel: Int) = {
    val data = Seq.newBuilder[String]

    if (detailLevel >= 3) {
      data += "lower: 0x" + Integer.toHexString(signal1 & 0xff)
      data += "upper: 0x" + Integer.toHexString(signal2 & 0xff)
    } else if (detailLevel >= 2) {
      data += "lower: " + (if (signal1 != 0) "high" else "low")
      data += "upper: " + (if (signal2 != 0) "high" else "low")
    }

    super.getRolloverData(gate, detailLevel) ++ data.result()
  }
}
