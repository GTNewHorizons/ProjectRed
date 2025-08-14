/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.gui.{ButtonNode, IconButtonNode, TNode}
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.operations.CircuitOp
import net.minecraft.util.StatCollector

import scala.collection.immutable.ListMap
import scala.collection.JavaConversions._

class ICToolsetNode(onSelect: CircuitOp => Unit) extends TNode {
  var opSet = Seq.empty[CircuitOp]
  var title = ""
  var buttonSize = Size(16, 16)
  var buttonGap = 1

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
      _ += StatCollector.translateToLocal(buttonOpMap(selectedButton).getOpName)
    }
    groupButton.clickDelegate = { () => selectedButton.clickDelegate() }
    addChild(groupButton)
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
      _ += StatCollector.translateToLocal(op.getOpName)
    }
    b.clickDelegate = { () => {
      onSelect(op)
      buttonClicked(op, b)
    } }
    b
  }

  private def buttonClicked(op: CircuitOp, button: ButtonNode) {
    setFocused()
    // Hide all other toolsets
    parent.children
      .collect {
        case t: ICToolsetNode if t != this => t
      }
      .foreach(toolsetNode => toolsetNode.setUnfocused())
    selectedButton.mouseoverLock = false
    button.mouseoverLock = true
    selectedButton = button
  }

  def select(op: CircuitOp): Unit = {
    for((button, operation) <- buttonOpMap) {
      if(op.id == operation.id) {
        buttonClicked(op, button)
        return
      }
    }
  }

  def setFocused() {
    if (!focused) {
      if (buttonOpMap.size > 1)
        for (b <- buttonOpMap.keys)
          b.hidden = false
    }
    focused = true
    groupButton.mouseoverLock = true
  }

  def setUnfocused() {
    if (focused) {
      if (buttonOpMap.size > 1)
        for (b <- buttonOpMap.keys)
          b.hidden = true
    }
    focused = false
    groupButton.mouseoverLock = false
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
