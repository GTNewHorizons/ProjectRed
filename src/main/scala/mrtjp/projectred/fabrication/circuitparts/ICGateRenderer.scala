package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.vec.Transformation
import mrtjp.projectred.fabrication.ICComponentModel
import mrtjp.projectred.fabrication.ICComponentStore.{
  finishRender,
  prepairRender
}
import mrtjp.projectred.fabrication.circuitparts.cells.{
  RenderBufferCell,
  RenderInvertCell,
  RenderNullCell
}
import mrtjp.projectred.fabrication.circuitparts.io.{
  RenderAnalogIO,
  RenderBundledIO,
  RenderSimpleIO
}
import mrtjp.projectred.fabrication.circuitparts.latches.{
  RenderSRLatch,
  RenderToggleLatch,
  RenderTransparentLatch
}
import mrtjp.projectred.fabrication.circuitparts.misc.{
  RenderCounter,
  RenderDecRandomizer,
  RenderRandomizer,
  RenderSynchronizer
}
import mrtjp.projectred.fabrication.circuitparts.primitives.{
  RenderAND,
  RenderBuffer,
  RenderMultiplexer,
  RenderNAND,
  RenderNOR,
  RenderNOT,
  RenderOR,
  RenderXNOR,
  RenderXOR
}
import mrtjp.projectred.fabrication.circuitparts.timing.{
  RenderPulse,
  RenderRepeater,
  RenderSequencer,
  RenderStateCell,
  RenderTimer
}
import net.minecraft.client.renderer.texture.IIconRegister

object ICGateRenderer {
  var renderers = buildRenders()

  def buildRenders() = Seq[ICGateRenderer[_]](
    new RenderSimpleIO,
    new RenderAnalogIO,
    new RenderBundledIO,
    new RenderOR,
    new RenderNOR,
    new RenderNOT,
    new RenderAND,
    new RenderNAND,
    new RenderXOR,
    new RenderXNOR,
    new RenderBuffer,
    new RenderMultiplexer,
    new RenderPulse,
    new RenderRepeater,
    new RenderRandomizer,
    new RenderSRLatch,
    new RenderToggleLatch,
    new RenderTransparentLatch,
    new RenderTimer,
    new RenderSequencer,
    new RenderCounter,
    new RenderStateCell,
    new RenderSynchronizer,
    new RenderDecRandomizer,
    new RenderNullCell,
    new RenderInvertCell,
    new RenderBufferCell
  )

  def registerIcons(reg: IIconRegister) {}

  def renderDynamic(
      gate: GateICPart,
      t: Transformation,
      ortho: Boolean,
      frame: Float
  ) {
    val r = renderers(gate.subID).asInstanceOf[ICGateRenderer[GateICPart]]
    r.prepareDynamic(gate, frame)
    r.renderDynamic(gate.rotationT `with` t, ortho)
  }

  def renderWithConfiguration(
      configuration: Int,
      t: Transformation,
      id: Int
  ): Unit = {
    val r = renderers(id)
    r.prepareStatic(configuration)
    r.renderDynamic(t, true)
  }

  def renderInv(t: Transformation, id: Int) {
    val r = renderers(id)
    r.prepareStatic(0)
    r.renderDynamic(t, true)
  }
}

abstract class ICGateRenderer[T <: GateICPart] {
  var reflect = false

  def coreModels: Seq[ICComponentModel]
  def switchModels = Seq[ICComponentModel]()

  def prepareStatic(configuration: Int)
  def prepareDynamic(gate: T, frame: Float) {}

  def renderDynamic(t: Transformation, ortho: Boolean) {
    renderModels(t, if (reflect) 1 else 0, ortho)
  }

  def renderModels(t: Transformation, orient: Int, ortho: Boolean) {
    prepairRender()
    for (m <- coreModels ++ switchModels) m.renderModel(t, orient, ortho)
    finishRender()
  }
}
