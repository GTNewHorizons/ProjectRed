/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.{GuiLib, MCButtonNode, SimpleTextboxNode, TNode}
import mrtjp.core.vec.{Point, Rect, Size}
import mrtjp.projectred.fabrication.gui.GuiICWorkbench
import org.lwjgl.input.Keyboard

class NewICNode extends TNode {
  val size = Size(100, 120)

  override def frame = Rect(position, size)

  var sizerRenderSize = Size(50, 50)
  var sizerRenderOffset = Point(0, -16)
  var sizerRenderGap = 2

  var maxBoardSize = Size(4, 4)
  var selectedBoardSize = Size(1, 1)

  var completionDelegate = { () => () }

  var outsideColour = Colors.LIGHT_GREY.argb
  var insideColour = Colors.CYAN.rgb | 0x88000000
  var hoverColour = Colors.BLUE.argb

  def getName = {
    val t = textbox.text
    if (t.isEmpty) "untitled" else t
  }

  private var textbox: SimpleTextboxNode = null
  private var sizerMap: Map[(Int, Int), Rect] = null

  private def sizerPos =
    position + Point(size / 2 - sizerRenderSize / 2) + sizerRenderOffset

  private def calcSizerRects = {
    val p = sizerPos
    val d = sizerRenderSize / maxBoardSize

    val rcol = GuiLib.createGrid(
      p.x,
      p.y,
      maxBoardSize.width,
      maxBoardSize.height,
      d.width,
      d.height
    )
    val icol =
      GuiLib.createGrid(0, 0, maxBoardSize.width, maxBoardSize.height, 1, 1)
    val zcol = rcol.zip(icol)

    var rects = Map[(Int, Int), Rect]()
    for (((px, py), (x, y)) <- zcol) {
      val rect =
        Rect(Point(px, py) + sizerRenderGap / 2, d - sizerRenderGap / 2)
      rects += (x, y) -> rect
    }
    rects
  }

  private def getMouseoverPos(mouse: Point) =
    sizerMap.find(_._2 contains mouse) match {
      case Some(((x, y), r)) => Point(x, y)
      case None => null
    }

  override def traceHit(absPoint: Point) = true

  override def onAddedToParent_Impl() {
    sizerMap = calcSizerRects

    val close = new MCButtonNode
    close.size = Size(8, 8)
    close.position = Point(4, 4)
    close.clickDelegate = { () => removeFromParent() }
    addChild(close)

    val fin = new MCButtonNode
    fin.size = Size(40, 15)
    fin.position = Point(
      size.width / 2 - fin.size.width / 2,
      size.height - fin.size.height - 4
    )
    fin.clickDelegate = { () =>
      removeFromParent()
      completionDelegate()
    }
    fin.text = "start"
    addChild(fin)

    textbox = new SimpleTextboxNode
    textbox.size = Size(80, 14)
    textbox.position = Point(size / 2 - textbox.size / 2) + Point(0, 24)
    textbox.phantom = "untitled"
    addChild(textbox)
  }

  override def frameUpdate_Impl(mouse: Point, rframe: Float) {
    if (!parent.asInstanceOf[GuiICWorkbench].tile.hasBP)
      removeFromParent()
  }

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    GuiDraw.drawGradientRect(
      0,
      0,
      parent.frame.width,
      parent.frame.height,
      -1072689136,
      -804253680
    )
    GuiLib.drawGuiBox(position.x, position.y, size.width, size.height, 0)

    val mousePos = getMouseoverPos(mouse)
    for (((x, y), rect) <- sizerMap) {
      GuiDraw.drawRect(rect.x, rect.y, rect.width, rect.height, outsideColour)

      if (x <= selectedBoardSize.width - 1 && y <= selectedBoardSize.height - 1)
        GuiDraw.drawRect(rect.x, rect.y, rect.width, rect.height, insideColour)

      if (mousePos != null && x == mousePos.x && y == mousePos.y)
        GuiDraw.drawRect(rect.midX - 2, rect.midY - 2, 4, 4, hoverColour)
    }
  }

  override def drawFront_Impl(mouse: Point, rframe: Float) {
    if (rayTest(mouse)) {
      val mousePos = getMouseoverPos(mouse)
      if (mousePos != null) {
        translateToScreen()
        val Point(mx, my) = parent.convertPointToScreen(mouse)
        import scala.collection.JavaConversions._
        GuiDraw.drawMultilineTip(
          mx + 12,
          my - 12,
          Seq((mousePos.x + 1) * 16 + " x " + (mousePos.y + 1) * 16)
        )
        translateFromScreen()
      }
    }
  }

  override def mouseClicked_Impl(
                                  p: Point,
                                  button: Int,
                                  consumed: Boolean
                                ): Boolean = {
    if (!consumed) {
      val mousePos = getMouseoverPos(p)
      if (mousePos != null) {
        selectedBoardSize = Size(mousePos + 1)
        return true
      }
    }
    false
  }

  override def keyPressed_Impl(c: Char, keycode: Int, consumed: Boolean) = {
    if (!consumed) keycode match {
      case Keyboard.KEY_ESCAPE =>
        removeFromParent()
        true
      case Keyboard.KEY_RETURN =>
        removeFromParent()
        completionDelegate()
        true
      case _ => false
    }
    else false
  }
}
