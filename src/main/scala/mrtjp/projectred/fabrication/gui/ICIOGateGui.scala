/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.MCButtonNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.io.IOGateICPart

class ICIOGateGui(override val gate: IOGateICPart)
  extends CircuitGui(gate)
    with TGateGui {
  {
    size = Size(124, 55)

    val conf = new MCButtonNode
    conf.position = Point(62, 33)
    conf.size = Size(46, 15)
    conf.text = "io mode"
    conf.clickDelegate = { () => gate.sendClientPacket(_.writeByte(1)) }
    addChild(conf)
  }

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    super.drawBack_Impl(mouse, rframe)

    GuiDraw.drawStringC(
      gate.shape match {
        case 0 => "input"
        case 1 => "output"
        case 2 => "inout"
      },
      position.x + 85,
      position.y + 16,
      Colors.GREY.argb,
      false
    )
  }
}
