package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.IntegratedCircuit
import mrtjp.projectred.fabrication.operations.CircuitOp
import net.minecraft.nbt.NBTTagCompound

object CircuitPart {
  def createPart(id: Int) = CircuitPartDefs(id).createPart
}

/** Base Class for all Gates, that are displayed in the IC Workbench
  */
abstract class CircuitPart {
  var world: IntegratedCircuit = null
  var loc: (Int, Int) = null

  def bind(ic: IntegratedCircuit, x: Int, y: Int) {
    world = ic
    loc = (x, y)
  }

  def unbind() {
    world = null
    loc = null
  }

  def x = loc._1

  def y = loc._2

  def id = getPartType.id

  def getPartType: CircuitPartDefs.CircuitPartDef

  def save(tag: NBTTagCompound) {}

  def load(tag: NBTTagCompound) {}

  def writeDesc(out: MCDataOutput) {}

  def readDesc(in: MCDataInput) {}

  def writeStreamOf(key: Int): MCDataOutput =
    world.network.getPartStream(x, y).writeByte(key)

  def read(in: MCDataInput) {
    read(in, in.readUByte())
  }

  def read(in: MCDataInput, key: Int) = key match {
    case 0 => readDesc(in)
    case _ =>
  }

  def sendDescUpdate() {
    writeDesc(writeStreamOf(0))
  }

  def update() {}

  def scheduledTick() {}

  def scheduleTick(ticks: Int) {
    world.scheduleTick(x, y, ticks)
  }

  def onAdded() {}

  def onRemoved() {}

  def onNeighborChanged() {}

  @SideOnly(Side.CLIENT)
  def onClicked() {}

  @SideOnly(Side.CLIENT)
  def onActivated() {}

  @SideOnly(Side.CLIENT)
  def getPartName: String

  /** Returns Circuit Operation, that would create this CircuitPart
    */
  @SideOnly(Side.CLIENT)
  def getCircuitOperation: CircuitOp = null

  @SideOnly(Side.CLIENT)
  def getRolloverData(detailLevel: Int): Seq[String] =
    if (detailLevel > 0) Seq(getPartName) else Seq.empty

  @SideOnly(Side.CLIENT)
  def renderDynamic(t: Transformation, ortho: Boolean, frame: Float) {}
}
