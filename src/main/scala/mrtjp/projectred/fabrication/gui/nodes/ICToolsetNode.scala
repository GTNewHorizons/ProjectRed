package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.gui.{ButtonNode, IconButtonNode, TNode}
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.operations.CircuitOp

import scala.collection.immutable.ListMap
import scala.collection.JavaConversions._

class ICToolsetNode extends TNode {
  var opSet = Seq.empty[CircuitOp]
  var title = ""
  var buttonSize = Size(16, 16)
  var buttonGap = 1

  var opSelectDelegate = { _: CircuitOp => () }

  private var focused = false
  private var buttonOpMap = ListMap.empty[ButtonNode, CircuitOp]

  private var selectedButton: ButtonNode = null
  private var groupButton: ButtonNode = null
  private var groupButtonOperation: CircuitOp = null

  def setup() {
    for (op <- opSet) {
      val b = createButtonFor(op)
      b.size = buttonSize
      b.hidden = true
      addChild(b)
      buttonOpMap += b -> op
    }

    val delta = opSet.size * (buttonSize.width + buttonGap)
    val firstPoint =
      Point(-delta / 2 + buttonSize.width / 2, -buttonSize.height - buttonGap)
    for ((b, i) <- buttonOpMap.keys.zipWithIndex)
      b.position = firstPoint.add(i * (buttonSize.width + buttonGap), 0)

    selectedButton = buttonOpMap.head._1
    groupButtonOperation = buttonOpMap.head._2

    groupButton = new IconButtonNode {
      override def drawButton(mouseover: Boolean) = {
        groupButtonOperation.renderImageStatic(
          position.x + 2,
          position.y + 2,
          size.width - 4,
          size.height - 4
        )
      }
    }
    groupButton.size = buttonSize
    groupButton.tooltipBuilder = {
      _ += buttonOpMap(selectedButton).getOpName
    }
    groupButton.clickDelegate = { () => selectedButton.clickDelegate() }
    addChild(groupButton)
  }

  private def buttonClicked(op: CircuitOp, button: ButtonNode) {
    setFocused()
    opSelectDelegate(op)
    parent.children
      .collect {
        case t: ICToolsetNode if t != this => t
      }
      .foreach(_.setUnfocused())
    selectedButton.mouseoverLock = false
    button.mouseoverLock = true
    selectedButton = button
  }

  def setUnfocused() {
    if (focused) hideSubTools()
    focused = false
    groupButton.mouseoverLock = false
  }

  def setFocused() {
    if (!focused) unhideSubTools()
    focused = true
    groupButton.mouseoverLock = true
  }

  private def unhideSubTools() {
    if (buttonOpMap.size > 1)
      for (b <- buttonOpMap.keys)
        b.hidden = false
  }

  private def hideSubTools() {
    if (buttonOpMap.size > 1)
      for (b <- buttonOpMap.keys)
        b.hidden = true
  }

  private def createButtonFor(op: CircuitOp) = {
    val b = new IconButtonNode {
      override def drawButton(mouseover: Boolean) {
        op.renderImageStatic(
          position.x + 2,
          position.y + 2,
          size.width - 4,
          size.height - 4
        )
      }
    }
    b.tooltipBuilder = {
      _ += op.getOpName
    }
    b.clickDelegate = { () => buttonClicked(op, b) }
    b
  }

  def pickOp(op: CircuitOp) {
    setUnfocused()
    buttonOpMap.find(_._2 == op) match {
      case Some((b, _)) => b.clickDelegate()
      case _            =>
    }
  }

  override def drawFront_Impl(mouse: Point, rframe: Float) {
    if (
      title.nonEmpty && groupButton.rayTest(parent.convertPointTo(mouse, this))
    ) {
      import net.minecraft.util.EnumChatFormatting._
      translateToScreen()
      val Point(mx, my) = parent.convertPointToScreen(mouse)
      GuiDraw.drawMultilineTip(
        mx + 12,
        my - 32,
        Seq(AQUA.toString + ITALIC.toString + title)
      )
      translateFromScreen()
    }
  }
}
