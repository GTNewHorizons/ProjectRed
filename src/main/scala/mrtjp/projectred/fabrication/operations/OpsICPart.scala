/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.circuitparts.CircuitPartDefs
import mrtjp.projectred.fabrication.circuitparts.wire.{
  RenderICButton,
  RenderICLever,
  RenderICTorch
}

class OpTorch extends OpSimplePlacement {
  override def doPartRender(t: Transformation) = RenderICTorch.render(t, true)

  override def createPart = CircuitPartDefs.Torch.createPart

  @SideOnly(Side.CLIENT)
  override def getOpName = "tile.notGate.name"
}

class OpButton extends OpSimplePlacement {
  override def doPartRender(t: Transformation) {
    RenderICButton.prepairInv()
    RenderICButton.render(t, true)
  }

  override def createPart = CircuitPartDefs.Button.createPart

  @SideOnly(Side.CLIENT)
  override def getOpName = "tile.button.name"
}

class OpLever extends OpSimplePlacement {
  override def doPartRender(t: Transformation) {
    RenderICLever.prepairInv()
    RenderICLever.render(t, true)
  }

  override def createPart = CircuitPartDefs.Lever.createPart

  @SideOnly(Side.CLIENT)
  override def getOpName = "tile.lever.name"
}
