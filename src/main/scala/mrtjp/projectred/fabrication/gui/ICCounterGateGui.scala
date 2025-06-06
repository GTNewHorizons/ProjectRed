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
import mrtjp.projectred.fabrication.circuitparts.SequentialGateICPart
import mrtjp.projectred.fabrication.circuitparts.misc.ICounterGuiLogic

class ICCounterGateGui(override val gate: SequentialGateICPart)
  extends CircuitGui(gate)
    with TGateGui {
  var valID = 0

  {
    size = Size(160, 94)

    val ax = 54
    val aw = 50
    val ah = 15

    val rotate = new MCButtonNode
    rotate.position = Point(ax, 5)
    rotate.size = Size(aw, ah)
    rotate.text = "rotate"
    rotate.clickDelegate = { () => gate.sendClientPacket(_.writeByte(0)) }
    addChild(rotate)

    val conf = new MCButtonNode
    conf.position = Point(ax + aw + 1, 5)
    conf.size = Size(aw, ah)
    conf.text = "configure"
    conf.clickDelegate = { () => gate.sendClientPacket(_.writeByte(1)) }
    addChild(conf)

    val sw = new MCButtonNode
    sw.position = Point(54, 28)
    sw.size = Size(20, 12)
    sw.text = "var"
    sw.clickDelegate = { () => valID = (valID + 1) % 3 }
    addChild(sw)

    def createButton(x: Int, y: Int, w: Int, h: Int, delta: Int) {
      val b = new MCButtonNode
      b.position = Point(x, y)
      b.size = Size(w, h)
      b.text = (if (delta < 0) "" else "+") + delta
      b.clickDelegate = { () =>
        gate.sendClientPacket(_.writeByte(4).writeByte(valID).writeShort(delta))
      }
      addChild(b)
    }

    val bw = 32
    val bh = 12
    val r1x = 69
    val r2x = r1x + 35
    val by = 48
    val bdy = 14

    createButton(r1x, by + (0 * bdy), bw, bh, -1)
    createButton(r1x, by + (1 * bdy), bw, bh, -5)
    createButton(r1x, by + (2 * bdy), bw, bh, -10)

    createButton(r2x, by + (0 * bdy), bw, bh, 1)
    createButton(r2x, by + (1 * bdy), bw, bh, 5)
    createButton(r2x, by + (2 * bdy), bw, bh, 10)
  }

  def getLogic = gate.getLogic[ICounterGuiLogic]

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    super.drawBack_Impl(mouse, rframe)
    val s = "State: " + getLogic.getCounterValue
    GuiDraw.drawStringC(
      s,
      position.x + 102,
      position.y + 24,
      Colors.GREY.argb,
      false
    )

    val m = valID match {
      case 0 => "Max: " + getLogic.getCounterMax
      case 1 => "Incr: " + getLogic.getCounterIncr
      case 2 => "Decr: " + getLogic.getCounterDecr
    }
    GuiDraw.drawStringC(
      m,
      position.x + 102,
      position.y + 36,
      Colors.GREY.argb,
      false
    )
  }
}
