package mrtjp.projectred.fabrication.circuitparts.timing

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.math.MathHelper
import codechicken.multipart.handler.MultipartSaveLoad
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.core.Configurator
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

trait ITimerGuiLogic {
  def getTimerMax: Int
  def setTimerMax(gate: GateICPart, t: Int)
}

class Timer(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with TTimerGateLogic {
  override def outputMask(shape: Int) = 0xb
  override def inputMask(shape: Int) = 0xe

  override def setup(gate: SequentialGateICPart) { startPointer() }

  override def scheduledTick(gate: SequentialGateICPart) {
    gate.setState(gate.state & 0xf)
    gate.onOutputChange(0xb)
    onChange(gate)
  }

  override def onChange(gate: SequentialGateICPart) {
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, 0xe)

    if (newInput != oldInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
    }

    if (gate.schedTime < 0)
      if (newInput > 0) resetPointer() else startPointer()
  }

  override def pointerTick() {
    resetPointer()
    if (!gate.world.network.isRemote) {
      gate.scheduleTick(2)
      gate.setState(0xb0 | gate.state & 0xf)
      gate.onOutputChange(0xb)
    }
  }
}

trait TTimerGateLogic extends SequentialICGateLogic with ITimerGuiLogic {
  var pointer_max = 38
  var pointer_start = -1L
  var saveTime = -1L // used for blueprint in-hand rendering

  abstract override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setInteger("pmax", pointer_max)
    tag.setLong(
      "pelapsed",
      if (pointer_start < 0) pointer_start else getTotalTime - pointer_start
    )
    tag.setLong("tsave", getTotalTime)
  }

  abstract override def load(tag: NBTTagCompound) {
    super.load(tag)
    pointer_max = tag.getInteger("pmax")
    pointer_start = tag.getLong("pelapsed")
    saveTime = tag.getLong("tsave")
    if (pointer_start >= 0) pointer_start = getTotalTime - pointer_start
  }

  abstract override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    packet.writeInt(pointer_max)
    packet.writeLong(pointer_start)
  }

  abstract override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    pointer_max = packet.readInt()
    pointer_start = packet.readLong()
  }

  abstract override def read(packet: MCDataInput, key: Int) = key match {
    case 12 => pointer_max = packet.readInt()
    case 13 =>
      pointer_start = packet.readInt()
      if (pointer_start >= 0) pointer_start = getTotalTime - pointer_start
    case _ => super.read(packet, key)
  }

  def getTotalTime = // client-side safe version of getTotalWorldTime (workaround for no client world)
    {
      if (gate.world.network == null)
        saveTime // ic was loaded directly from stack, possibly for in-hand render
      else if (gate.world.network.getWorld == null)
        MultipartSaveLoad.loadingWorld.getTotalWorldTime // ic is being loaded with a workbench tile or gate
      else
        gate.world.network.getWorld.getTotalWorldTime // normal access during operation
    }

  def pointerValue =
    if (pointer_start < 0) 0 else (getTotalTime - pointer_start).toInt

  def sendPointerMaxUpdate() { gate.writeStreamOf(12).writeInt(pointer_max) }
  def sendPointerUpdate() {
    gate.writeStreamOf(13).writeInt(if (pointer_start < 0) -1 else pointerValue)
  }

  override def getTimerMax = pointer_max + 2
  override def setTimerMax(gate: GateICPart, time: Int) {
    var t = time
    val minTime = math.max(4, Configurator.minTimerTicks)
    if (t < minTime) t = minTime
    if (t != pointer_max) {
      pointer_max = t - 2
      sendPointerMaxUpdate()
    }
  }

  override def onTick(gate: SequentialGateICPart) {
    if (pointer_start >= 0)
      if (getTotalTime >= pointer_start + pointer_max) pointerTick()
      else if (pointer_start > getTotalTime)
        pointer_start = getTotalTime
  }

  def pointerTick()

  def resetPointer() {
    if (pointer_start >= 0) {
      pointer_start = -1
      gate.world.network.markSave()
      if (!gate.world.network.isRemote) sendPointerUpdate()
    }
  }

  def startPointer() {
    if (pointer_start < 0) {
      pointer_start = getTotalTime
      gate.world.network.markSave()
      if (!gate.world.network.isRemote) sendPointerUpdate()
    }
  }

  def interpPointer(f: Float) =
    if (pointer_start < 0) 0f else (pointerValue + f) / pointer_max

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: SequentialGateICPart, detailLevel: Int) = {
    val data = Seq.newBuilder[String]
    if (detailLevel > 1)
      data += "interval: " + "%.2f".format(getTimerMax * 0.05) + "s"
    super.getRolloverData(gate, detailLevel) ++ data.result()
  }
}

class RenderTimer extends ICGateRenderer[SequentialGateICPart] {
  val wires = generateWireModels("TIME", 3)
  val torches = Seq(new RedstoneTorchModel(8, 3), new RedstoneTorchModel(8, 8))
  val pointer = new PointerModel(8, 8)

  override val coreModels =
    Seq(new BaseComponentModel("TIME")) ++ wires ++ Seq(pointer) ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(1).on = false
    wires(2).on = false
    torches(0).on = false
    pointer.angle = 0
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    torches(0).on = (gate.state & 0x10) != 0
    wires(0).on = (gate.state & 0x88) != 0
    wires(1).on = (gate.state & 0x22) != 0
    wires(2).on = (gate.state & 4) != 0
    val ang =
      gate.getLogic[TTimerGateLogic].interpPointer(frame) * MathHelper.pi * 2
    pointer.angle = ang
  }
}
