/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui

import codechicken.lib.vec.Translation
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.ICComponentStore
import mrtjp.projectred.fabrication.circuitparts.{GateICPart, ICGateRenderer}

trait TGateGui extends CircuitGui {
  var gateRenderSize = Size(40, 40)
  var gateRenderX = 10

  def gate: GateICPart

  abstract override def drawBack_Impl(mouse: Point, rframe: Float) {
    super.drawBack_Impl(mouse, rframe)

    ICGateRenderer.renderDynamic(
      gate,
      ICComponentStore
        .orthoGridT(gateRenderSize.width, gateRenderSize.height) `with`
        new Translation(
          position.x + gateRenderX,
          position.y + (size / 2 - gateRenderSize / 2).height,
          0
        ),
      true,
      rframe
    )
  }
}
