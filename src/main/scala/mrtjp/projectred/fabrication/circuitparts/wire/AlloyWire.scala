package mrtjp.projectred.fabrication.circuitparts.wire

import codechicken.lib.render.ColourMultiplier
import codechicken.lib.render.uv.IconTransformation
import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication.circuitparts.CircuitPartDefs
import mrtjp.projectred.fabrication.operations.CircuitOpDefs

class AlloyWireICPart extends RedwireICPart {
  override def getPartType = CircuitPartDefs.AlloyWire

  @SideOnly(Side.CLIENT)
  override def renderDynamic(t: Transformation, ortho: Boolean, frame: Float) {
    RenderICAlloyWire.prepairDynamic(this)
    RenderICAlloyWire.render(t, ortho)
  }

  @SideOnly(Side.CLIENT)
  override def getPartName = "Alloy wire"

  @SideOnly(Side.CLIENT)
  override def getCircuitOperation = CircuitOpDefs.AlloyWire.getOp
}

object RenderICAlloyWire {
  var connMap: Byte = 0
  var signal: Byte = 0

  def prepairInv() {
    connMap = 0xf
    signal = 0xff.toByte
  }

  def prepairDynamic(part: AlloyWireICPart) {
    connMap = part.connMap
    signal = part.signal
  }

  def render(t: Transformation, ortho: Boolean) {
    prepairRender()
    faceModels(dynamicIdx(0, ortho)).render(
      t,
      new IconTransformation(redwireIcons(connMap & 0xff)),
      ColourMultiplier.instance((signal & 0xff) / 2 + 60 << 24 | 0xff)
    )
    finishRender()
  }
}
