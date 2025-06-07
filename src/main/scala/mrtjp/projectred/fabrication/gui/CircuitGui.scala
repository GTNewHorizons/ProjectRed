/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui

import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui.{GuiLib, MCButtonNode, TNode}
import mrtjp.core.vec.{Point, Rect, Size}
import mrtjp.projectred.fabrication.circuitparts.TClientNetCircuitPart
import net.minecraft.client.gui.Gui
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

trait IGuiCircuitPart extends TClientNetCircuitPart {
  @SideOnly(Side.CLIENT)
  def createGui: CircuitGui
}

class CircuitGui(val part: IGuiCircuitPart) extends Gui with TNode {
  var size = Size.zeroSize

  override def frame = Rect(position, size)

  var lineColor = Colors.LIME.argb(0xaa)
  var linePointerCalc = { () => Point.zeroPoint }

  private def moverFrame = Rect(position + Point(4, 9), Size(4, 6))

  private var mouseDown = false
  private var mouseInit = Point.zeroPoint

  {
    val close = new MCButtonNode
    close.position = Point(4, 4)
    close.size = Size(5, 5)
    close.clickDelegate = { () => removeFromParent() }
    addChild(close)
  }

  override def frameUpdate_Impl(mouse: Point, rframe: Float) {
    if (mouseDown) {
      position += mouse - mouseInit
      mouseInit = mouse
    }

    if (part.world == null) removeFromParent()
  }

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    GuiLib.drawGuiBox(position.x, position.y, size.width, size.height, 0)
    GuiDraw.drawRect(
      moverFrame.x,
      moverFrame.y,
      moverFrame.width,
      moverFrame.height,
      Colors.LIGHT_GREY.argb
    )
  }

  override def drawFront_Impl(mouse: Point, rframe: Float) {
    val from = linePointerCalc()
    val to = from.clamp(frame)
    GL11.glColor4d(1, 1, 1, 1)
    GuiLib.drawLine(from.x, from.y, to.x, to.y, lineColor)
    GuiDraw.drawRect(to.x - 3, to.y - 3, 6, 6, lineColor)
  }

  override def mouseClicked_Impl(
      p: Point,
      button: Int,
      consumed: Boolean
  ): Boolean = {
    if (parent == null)
      false // we cant check for consume here, so manually check if closed
    else
      hitTest(p).find(_.isInstanceOf[CircuitGui]) match {
        case Some(gui) if gui == this =>
          val guis = parent.childrenByZ.collect { case g: CircuitGui => g }
          val otherGuis = guis.filter(_ != this)
          for (i <- otherGuis.indices)
            otherGuis(i).pushZTo(0.1 * i)
          pushZTo(0.1 * otherGuis.size)

          if (moverFrame.contains(p)) {
            mouseDown = true
            mouseInit = p
          }
          true
        case _ => false
      }
  }

  override def mouseReleased_Impl(p: Point, button: Int, consumed: Boolean) = {
    mouseDown = false
    false
  }

  override def keyPressed_Impl(c: Char, keycode: Int, consumed: Boolean) =
    if (!consumed && keycode == Keyboard.KEY_ESCAPE) {
      removeFromParent()
      true
    } else false
}
