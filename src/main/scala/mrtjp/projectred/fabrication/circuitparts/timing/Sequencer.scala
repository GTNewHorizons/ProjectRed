package mrtjp.projectred.fabrication.circuitparts.timing

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.math.MathHelper
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.core.Configurator
import mrtjp.projectred.core.TFaceOrient.flipMaskZ
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

object Sequencer {
  def cycleShape(shape: Int): Int = {
    shape ^ 1
  }
}

class Sequencer(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with ITimerGuiLogic {
  var pointer_max = 40
  var saveTime = -1L

  override def outputMask(shape: Int) = 0xf

  override def onChange(gate: SequentialGateICPart) {}
  override def scheduledTick(gate: SequentialGateICPart) {}

  override def getTimerMax = pointer_max
  override def setTimerMax(gate: GateICPart, time: Int) {
    var t = time
    val minTime = math.max(4, Configurator.minTimerTicks)
    if (t < minTime) t = minTime
    if (t != pointer_max) {
      pointer_max = t
      sendPointerMaxUpdate()
    }
  }

  override def save(tag: NBTTagCompound) {
    tag.setInteger("pmax", pointer_max)
    tag.setLong("tsave", getWorldTime)
  }
  override def load(tag: NBTTagCompound) {
    pointer_max = tag.getInteger("pmax")
    saveTime = tag.getLong("tsave")
  }

  override def writeDesc(packet: MCDataOutput) { packet.writeInt(pointer_max) }
  override def readDesc(packet: MCDataInput) { pointer_max = packet.readInt() }

  override def read(packet: MCDataInput, key: Int) = key match {
    case 12 => pointer_max = packet.readInt()
    case _  =>
  }

  def sendPointerMaxUpdate() { gate.writeStreamOf(12).writeInt(pointer_max) }

  def getWorldTime =
    if (gate.world.network != null) gate.world.network.getWorld.getWorldTime
    else saveTime

  override def onTick(gate: SequentialGateICPart) {
    if (!gate.world.network.isRemote) {
      val oldOut = gate.state >> 4
      var out = 1 << getWorldTime % (pointer_max * 4) / pointer_max
      if (gate.shape == 1) out = flipMaskZ(out)
      if (oldOut != out) {
        gate.setState(out << 4)
        gate.onOutputChange(0xf)
      }
    }
  }

  override def cycleShape(gate: SequentialGateICPart) = {
    gate.setShape(Sequencer.cycleShape(gate.shape))
    true
  }

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: SequentialGateICPart, detailLevel: Int) = {
    val data = Seq.newBuilder[String]
    if (detailLevel > 1)
      data += "interval: " + "%.2f".format(getTimerMax * 0.05) + "s"
    super.getRolloverData(gate, detailLevel) ++ data.result()
  }
}

class RenderSequencer extends ICGateRenderer[SequentialGateICPart] {
  val torches = Seq(
    new RedstoneTorchModel(8, 8),
    new RedstoneTorchModel(8, 3),
    new RedstoneTorchModel(13, 8),
    new RedstoneTorchModel(8, 13),
    new RedstoneTorchModel(3, 8)
  )
  val pointer = new PointerModel(8, 8)

  torches(0).on = true

  override val coreModels =
    Seq(new BaseComponentModel("SEQUENCER"), pointer) ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    torches(1).on = true
    torches(2).on = false
    torches(3).on = false
    torches(4).on = false

    pointer.angle = 0
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    torches(1).on = (gate.state & 0x10) != 0
    torches(2).on = (gate.state & 0x20) != 0
    torches(3).on = (gate.state & 0x40) != 0
    torches(4).on = (gate.state & 0x80) != 0

    val max = gate.getLogic[Sequencer].pointer_max * 4
    pointer.angle = (gate
      .getLogic[Sequencer]
      .getWorldTime % max + frame) / max * 2 * MathHelper.pi
    if (gate.shape == 1) pointer.angle *= -1
  }
}
