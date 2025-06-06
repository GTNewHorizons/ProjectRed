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
import mrtjp.projectred.fabrication.circuitparts.timing.ITimerGuiLogic

class ICTimerGateGui(override val gate: SequentialGateICPart)
  extends CircuitGui(gate)
    with TGateGui {
  {
    size = Size(160, 80)

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

    def createButton(x: Int, y: Int, w: Int, h: Int, text: String, delta: Int) {
      val b = new MCButtonNode
      b.position = Point(x, y)
      b.size = Size(w, h)
      b.text = text
      b.clickDelegate = { () =>
        gate.sendClientPacket(_.writeByte(3).writeShort(delta))
      }
      addChild(b)
    }

    val bw = 32
    val bh = 12
    val r1x = 69
    val r2x = r1x + 35
    val by = 34
    val bdy = 14

    createButton(r1x, by + (0 * bdy), bw, bh, "-50ms", -1)
    createButton(r1x, by + (1 * bdy), bw, bh, "-1s", -20)
    createButton(r1x, by + (2 * bdy), bw, bh, "-10s", -200)

    createButton(r2x, by + (0 * bdy), bw, bh, "+50ms", 1)
    createButton(r2x, by + (1 * bdy), bw, bh, "+1s", 20)
    createButton(r2x, by + (2 * bdy), bw, bh, "+10s", 200)
  }

  def getLogic = gate.getLogic[ITimerGuiLogic]

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    super.drawBack_Impl(mouse, rframe)
    val s = "Interval: " + "%.2f".format(getLogic.getTimerMax * 0.05) + "s"
    GuiDraw.drawStringC(
      s,
      position.x + 102,
      position.y + 24,
      Colors.GREY.argb,
      false
    )
  }
}
