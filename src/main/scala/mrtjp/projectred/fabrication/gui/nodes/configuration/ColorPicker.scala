/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes.configuration

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.TNode
import mrtjp.core.vec.{Point, Rect, Size}

class ColorPicker(onPickColor: Int => Unit) extends TNode {

  var size = Size.zeroSize

  override def frame = Rect(position, size)

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    val edge = size.width / 4

    for (i <- 0 to 15) {
      GuiDraw.drawRect(
        position.x + (i % 4) * edge,
        position.y + (i / 4) * edge,
        edge,
        edge,
        Colors(i).argb
      )
    }
  }

  override def mouseClicked_Impl(
      p: Point,
      button: Int,
      consumed: Boolean
  ): Boolean = {
    if (!consumed && rayTest(p)) {
      val relPos = p.subtract(position)
      val index =
        (relPos.y / (size.height / 4)) * 4 + relPos.x / (size.width / 4)
      onPickColor(index)
      true
    } else
      false
  }
}
