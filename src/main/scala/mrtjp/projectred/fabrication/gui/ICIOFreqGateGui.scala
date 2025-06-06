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
import mrtjp.projectred.fabrication.circuitparts.io.{IOGateICPart, TFreqIOICGateLogic}


class ICIOFreqGateGui(override val gate: IOGateICPart)
    extends CircuitGui(gate)
    with TGateGui {
  {
    size = Size(138, 55)

    val conf = new MCButtonNode
    conf.position = Point(52, 7)
    conf.size = Size(46, 15)
    conf.text = "io mode"
    conf.clickDelegate = { () => gate.sendClientPacket(_.writeByte(1)) }
    addChild(conf)

    val minus = new MCButtonNode
    minus.position = Point(52, 33)
    minus.size = Size(14, 14)
    minus.text = "-"
    minus.clickDelegate = { () => gate.sendClientPacket(_.writeByte(6)) }
    addChild(minus)

    val plus = new MCButtonNode
    plus.position = Point(117, 33)
    plus.size = Size(14, 14)
    plus.text = "+"
    plus.clickDelegate = { () => gate.sendClientPacket(_.writeByte(5)) }
    addChild(plus)
  }

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    super.drawBack_Impl(mouse, rframe)

    GuiDraw.drawStringC(
      gate.shape match {
        case 0 => "input"
        case 1 => "output"
        case 2 => "inout"
      },
      position.x + 117,
      position.y + 11,
      Colors.GREY.argb,
      false
    )

    GuiDraw.drawStringC(
      "freq",
      position.x + 66,
      position.y + 22,
      50,
      14,
      Colors.GREY.argb,
      false
    )
    GuiDraw.drawStringC(
      gate.getLogic[TFreqIOICGateLogic].getFreqName,
      position.x + 66,
      position.y + 33,
      50,
      14,
      Colors.GREY.argb,
      false
    )
  }
}
