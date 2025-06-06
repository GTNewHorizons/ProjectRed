/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.circuitparts.CircuitPartDefs
import mrtjp.projectred.fabrication.circuitparts.wire.{RenderICButton, RenderICLever, RenderICTorch}


class CircuitOpTorch extends SimplePlacementOp {
  override def doPartRender(t: Transformation) = RenderICTorch.render(t, true)

  override def createPart = CircuitPartDefs.Torch.createPart

  @SideOnly(Side.CLIENT)
  override def getOpName = "Torch"
}


class CircuitOpButton extends SimplePlacementOp {
  override def doPartRender(t: Transformation) {
    RenderICButton.prepairInv()
    RenderICButton.render(t, true)
  }

  override def createPart = CircuitPartDefs.Button.createPart

  @SideOnly(Side.CLIENT)
  override def getOpName = "Button"
}


class CircuitOpLever extends SimplePlacementOp {
  override def doPartRender(t: Transformation) {
    RenderICLever.prepairInv()
    RenderICLever.render(t, true)
  }

  override def createPart = CircuitPartDefs.Lever.createPart

  @SideOnly(Side.CLIENT)
  override def getOpName = "Lever"
}