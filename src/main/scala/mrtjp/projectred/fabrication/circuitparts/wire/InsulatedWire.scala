package mrtjp.projectred.fabrication.circuitparts.wire

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.render.ColourMultiplier
import codechicken.lib.render.uv.IconTransformation
import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication.circuitparts.{CircuitPart, CircuitPartDefs}
import mrtjp.projectred.fabrication.operations.CircuitOpDefs
import net.minecraft.nbt.NBTTagCompound

trait IInsulatedRedwireICPart extends IRedwireICPart {
  def getInsulatedColour: Int
}

class InsulatedWireICPart extends RedwireICPart with IInsulatedRedwireICPart {
  var colour: Byte = 0

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("colour", colour)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    colour = tag.getByte("colour")
  }

  override def writeDesc(out: MCDataOutput) {
    super.writeDesc(out)
    out.writeByte(colour)
  }

  override def readDesc(in: MCDataInput) {
    super.readDesc(in)
    colour = in.readByte()
  }

  override def getPartType = CircuitPartDefs.InsulatedWire

  override def resolveSignal(part: Any, r: Int) = part match {
    case b: IBundledCableICPart => (b.getBundledSignal.apply(colour) & 0xff) - 1
    case _                      => super.resolveSignal(part, r)
  }

  override def canConnectPart(part: CircuitPart, r: Int) = part match {
    case b: IBundledCableICPart  => true
    case iw: InsulatedWireICPart => iw.colour == colour
    case _                       => super.canConnectPart(part, r)
  }

  override def getInsulatedColour = colour

  @SideOnly(Side.CLIENT)
  override def renderDynamic(t: Transformation, ortho: Boolean, frame: Float) {
    RenderICInsulatedWire.prepairDynamic(this)
    RenderICInsulatedWire.render(t, ortho)
  }

  @SideOnly(Side.CLIENT)
  override def getPartName = Colors(colour & 0xff).name + " Insulated wire"

  @SideOnly(Side.CLIENT)
  override def getCircuitOperation =
    CircuitOpDefs
      .values(CircuitOpDefs.WhiteInsulatedWire.ordinal + colour)
      .getOp
}

object RenderICInsulatedWire {
  var connMap: Byte = 0
  var signal: Byte = 0
  var colour: Byte = 0

  def prepairInv(c: Int) {
    connMap = 0xf
    signal = 255.toByte
    colour = c.toByte
  }

  def prepairDynamic(part: InsulatedWireICPart) {
    connMap = part.connMap
    signal = part.signal
    colour = part.colour
  }

  def render(t: Transformation, ortho: Boolean) {
    prepairRender()
    faceModels(dynamicIdx(0, ortho)).render(
      t,
      new IconTransformation(redwireIcons(connMap & 0xff)),
      ColourMultiplier.instance((signal & 0xff) / 2 + 60 << 24 | 0xff)
    )
    faceModels(dynamicIdx(0, ortho)).render(
      t,
      new IconTransformation(insulatedwireIcons(connMap & 0xff)),
      ColourMultiplier.instance(Colors(colour & 0xff).rgba)
    )
    finishRender()
  }
}
