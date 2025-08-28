/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts.wire

import codechicken.lib.render.uv.IconTransformation
import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{
  CircuitPart,
  CircuitPartDefs,
  TPoweredCircuitPart,
  TICAcquisitions
}
import mrtjp.projectred.fabrication.operations.CircuitOpDefs

class TorchICPart
    extends CircuitPart
    with TICAcquisitions
    with TPoweredCircuitPart {
  override def getPartType = CircuitPartDefs.Torch

  override def onAdded() {
    if (!world.network.isRemote) notify(0xf)
  }

  override def onRemoved() {
    if (!world.network.isRemote) notify(0xf)
  }

  override def rsOutputLevel(r: Int) = 255
  override def canConnectRS(r: Int) = true

  @SideOnly(Side.CLIENT)
  override def getPartName = "Torch"

  @SideOnly(Side.CLIENT)
  override def getCircuitOperation = CircuitOpDefs.Torch.getOp

  @SideOnly(Side.CLIENT)
  override def renderDynamic(
      t: Transformation,
      ortho: Boolean,
      frame: Float
  ) = {
    RenderICTorch.render(t, ortho)
  }
}

object RenderICTorch {
  def render(t: Transformation, ortho: Boolean) {
    prepairRender()
    faceModels(dynamicIdx(0, ortho))
      .render(t, new IconTransformation(torchOnIcon))
    finishRender()
  }
}
