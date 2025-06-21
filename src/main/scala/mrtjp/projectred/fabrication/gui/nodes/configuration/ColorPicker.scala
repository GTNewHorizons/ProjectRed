package mrtjp.projectred.fabrication.gui.nodes.configuration

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.TNode
import mrtjp.core.vec.{Point, Rect, Size}

class ColorPicker(var color: Int, onPickColor: Int => Unit) extends TNode {

  var size = Size.zeroSize

  override def frame = Rect(position, size)

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    val edge = size.width / 4

    for (i <- 0 to 15) {
      // Top Left Corner
      val x = position.x + (i % 4) * edge
      val y = position.y + (i / 4) * edge

      if (i != color) {
        GuiDraw.drawRect(
          x,
          y,
          edge,
          edge,
          Colors(i).argb
        )
      } else {
        // Border
        GuiDraw.drawRect(
          x,
          y,
          edge,
          edge,
          0xffff7777
        )
        // Color
        GuiDraw.drawRect(
          x + 2,
          y + 2,
          edge - 4,
          edge - 4,
          Colors(i).argb
        )
      }
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
      color = index
      onPickColor(color)
      true
    } else
      false
  }
}
