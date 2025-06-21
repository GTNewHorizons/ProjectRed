package mrtjp.projectred.fabrication.circuitparts.misc

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.math.MathHelper
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.core.TFaceOrient.flipMaskZ
import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  GateICPart,
  ICGateRenderer,
  SequentialGateICPart,
  SequentialICGateLogic
}
import mrtjp.projectred.fabrication.{
  BaseComponentModel,
  PointerModel,
  RedstoneTorchModel
}
import net.minecraft.nbt.NBTTagCompound

trait ICounterGuiLogic {
  def getCounterMax: Int
  def setCounterMax(gate: GateICPart, i: Int)

  def getCounterIncr: Int
  def setCounterIncr(gate: GateICPart, i: Int)

  def getCounterDecr: Int
  def setCounterDecr(gate: GateICPart, i: Int)

  def getCounterValue: Int
  def setCounterValue(gate: GateICPart, i: Int)
}

object Counter {
  def cycleShape(shape: Int): Int = {
    if (shape == 1) 0 else 1
  }
}

class Counter(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with ICounterGuiLogic {
  var value = 0
  var max = 10
  var incr = 1
  var decr = 1

  override def outputMask(shape: Int) = 5
  override def inputMask(shape: Int) = 10

  override def save(tag: NBTTagCompound) {
    tag.setInteger("val", value)
    tag.setInteger("max", max)
    tag.setInteger("inc", incr)
    tag.setInteger("dec", decr)
  }

  override def load(tag: NBTTagCompound) {
    value = tag.getInteger("val")
    max = tag.getInteger("max")
    incr = tag.getInteger("inc")
    decr = tag.getInteger("dec")
  }

  override def writeDesc(packet: MCDataOutput) {
    packet.writeInt(value).writeInt(max).writeInt(incr).writeInt(decr)
  }

  override def readDesc(packet: MCDataInput) {
    value = packet.readInt()
    max = packet.readInt()
    incr = packet.readInt()
    decr = packet.readInt()
  }

  override def read(packet: MCDataInput, key: Int) = key match {
    case 11 => value = packet.readInt()
    case 12 => max = packet.readInt()
    case 13 => incr = packet.readInt()
    case 14 => decr = packet.readInt()
    case _  =>
  }

  def sendValueUpdate() { gate.writeStreamOf(11).writeInt(value) }
  def sendMaxUpdate() { gate.writeStreamOf(12).writeInt(max) }
  def sendIncrUpdate() { gate.writeStreamOf(13).writeInt(incr) }
  def sendDecrUpdate() { gate.writeStreamOf(14).writeInt(decr) }

  override def getCounterValue = value
  override def getCounterMax = max
  override def getCounterIncr = incr
  override def getCounterDecr = decr

  override def setCounterValue(gate: GateICPart, i: Int) {
    val oldVal = value
    value = Math.min(max, Math.max(0, i))
    if (value != oldVal)
      sendValueUpdate()
  }

  override def setCounterMax(gate: GateICPart, i: Int) {
    val oldMax = max
    max = Math.min(32767, Math.max(1, i))
    if (max != oldMax) {
      sendMaxUpdate()
      val oldVal = value
      value = Math.min(value, Math.max(0, i))
      if (value != oldVal) {
        sendValueUpdate()
        gate.scheduleTick(2)
      }
    }
  }

  override def setCounterIncr(gate: GateICPart, i: Int) {
    val oldIncr = incr
    incr = Math.min(max, Math.max(1, i))
    if (incr != oldIncr)
      sendIncrUpdate()
  }

  override def setCounterDecr(gate: GateICPart, i: Int) {
    val oldDecr = decr
    decr = Math.min(max, Math.max(1, i))
    if (decr != oldDecr)
      sendDecrUpdate()
  }

  def onChange(gate: SequentialGateICPart) {
    val oldInput = gate.state & 0xf
    var newInput = getInput(gate, 0xa)
    if (gate.shape == 1) newInput = flipMaskZ(newInput)
    val high = newInput & ~oldInput

    if ((high & 2) != 0) setCounterValue(gate, value + incr)
    if ((high & 8) != 0) setCounterValue(gate, value - decr)
    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
      gate.scheduleTick(2)
    }
  }

  override def cycleShape(gate: SequentialGateICPart) = {
    gate.setShape(Counter.cycleShape(gate.shape))
    true
  }

  def scheduledTick(gate: SequentialGateICPart) {
    val oldOutput = gate.state
    var newOutput = 0
    if (value == max) newOutput = 1
    else if (value == 0) newOutput = 4
    if (newOutput != oldOutput) gate.setState(gate.state & 0xf | newOutput << 4)
    if (newOutput != oldOutput) gate.onOutputChange(5)
  }

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: SequentialGateICPart, detailLevel: Int) = {
    val data = Seq.newBuilder[String]
    if (detailLevel > 1) {
      data += "state: " + getCounterValue
      if (detailLevel > 2) {
        data += "max: " + getCounterMax
        data += "incr: " + getCounterIncr
        data += "decr: " + getCounterDecr
      }
    }
    super.getRolloverData(gate, detailLevel) ++ data.result()
  }
}

class RenderCounter extends ICGateRenderer[SequentialGateICPart] {
  val wires = generateWireModels("COUNT", 2)
  val torches = Seq(
    new RedstoneTorchModel(11, 8),
    new RedstoneTorchModel(8, 3),
    new RedstoneTorchModel(8, 13)
  )
  val pointer = new PointerModel(11, 8, 1.2d)

  torches(0).on = true

  override val coreModels =
    Seq(new BaseComponentModel("COUNT")) ++ wires ++ Seq(pointer) ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    reflect = false
    wires(0).on = false
    wires(1).on = false
    torches(1).on = false
    torches(2).on = true
    pointer.angle = 220 * MathHelper.torad
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    reflect = gate.shape == 1
    wires(0).on = (gate.state & 8) != 0
    wires(1).on = (gate.state & 2) != 0
    torches(1).on = (gate.state & 0x10) != 0
    torches(2).on = (gate.state & 0x40) != 0

    val max = gate.getLogic[Counter].max
    val value = gate.getLogic[Counter].value
    pointer.angle =
      (value / max.toDouble * (340 - 220) + 210) * MathHelper.torad
    if (gate.shape == 1) reflect = true
  }
}
