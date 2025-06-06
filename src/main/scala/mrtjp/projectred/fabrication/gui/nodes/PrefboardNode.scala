/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import codechicken.lib.render.ColourMultiplier
import codechicken.lib.render.uv.{UVScale, UVTranslation}
import mrtjp.core.color.Colors
import mrtjp.core.gui.{ClipNode, TNode}
import mrtjp.core.vec.{Point, Rect, Size}
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication.circuitparts.ICGateDefinition
import mrtjp.projectred.fabrication.gui.{CircuitGui, IGuiCircuitPart}
import mrtjp.projectred.fabrication.operations.{CircuitOp, CircuitOpDefs, OpGateCommons}
import mrtjp.projectred.fabrication.{IntegratedCircuit, RenderCircuit}
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.input.{Keyboard, Mouse}

import scala.collection.JavaConversions._
import scala.collection.convert.WrapAsJava

class PrefboardNode(circuit: IntegratedCircuit, previewUpdateDelegate: (CircuitOp) => Unit) extends TNode {
  var currentOp: CircuitOp = null

  /** 0 - off 1 - name only 2 - minor details 3 - all details
   */
  var detailLevel = 1
  var scale = 1.0
  var sizeMult = 8

  def size = circuit.size * sizeMult

  def updatePreview(): Unit = {
    previewUpdateDelegate(currentOp)
  }

  override def frame = Rect(
    position,
    Size((size.width * scale).toInt, (size.height * scale).toInt)
  )

  var opPickDelegate = { _: CircuitOp => () }

  private var leftMouseDown = false
  private var rightMouseDown = false
  private var mouseStart = Point(0, 0)

  private def isCircuitValid = circuit.nonEmpty

  private def toGridPoint(p: Point) = {
    val f = frame
    val rpos = p - position
    Point(
      (rpos.x * circuit.size.width * 1.0 / f.width).toInt
        .min(circuit.size.width - 1)
        .max(0),
      (rpos.y * circuit.size.height * 1.0 / f.height).toInt
        .min(circuit.size.height - 1)
        .max(0)
    )
  }

  private def toCenteredGuiPoint(gridP: Point) = {
    val dp = frame.size.vectorize / circuit.size.vectorize
    Point(gridP.vectorize * dp + dp / 2)
  }

  override def update_Impl() {
    if (mcInst.theWorld.getTotalWorldTime % 20 == 0)
      circuit.refreshErrors()
  }

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    if (isCircuitValid) {
      val f = frame
      RenderCircuit.renderOrtho(
        circuit,
        f.x,
        f.y,
        size.width * scale,
        size.height * scale,
        rframe
      )

      if (currentOp != null) {
        if (frame.contains(mouse) && rayTest(mouse) && !leftMouseDown) {
          currentOp.renderHover(
            circuit,
            toGridPoint(mouse),
            f.x,
            f.y,
            size.width * scale,
            size.height * scale
          )
        } else if (leftMouseDown)
          currentOp.renderDrag(
            circuit,
            mouseStart,
            toGridPoint(mouse),
            f.x,
            f.y,
            size.width * scale,
            size.height * scale
          )
      }

      if (
        mcInst.theWorld.getTotalWorldTime % 100 > 5 && circuit.errors.nonEmpty
      ) {
        prepairRender()
        PRResources.guiPrototyper.bind()
        for ((Point(x, y), (_, c)) <- circuit.errors) {
          val t = orthoPartT(
            f.x,
            f.y,
            size.width * scale,
            size.height * scale,
            circuit.size,
            x,
            y
          )
          faceModels(dynamicIdx(0, true)).render(
            t,
            new UVScale(64) `with` new UVTranslation(
              330,
              37
            ) `with` new UVScale(1 / 512d),
            ColourMultiplier.instance(Colors(c).rgba)
          )
        }
        finishRender()
      }
    }
  }

  override def drawFront_Impl(mouse: Point, rframe: Float) {
    if (
      isCircuitValid && !leftMouseDown && frame.contains(mouse) && rayTest(
        mouse
      )
    ) {
      val point = toGridPoint(mouse)
      val part = circuit.getPart(point)
      if (part != null) {
        val data = part.getRolloverData(detailLevel)
        if (data.nonEmpty) {
          ClipNode.tempDisableScissoring()
          translateToScreen()
          val Point(mx, my) = parent.convertPointToScreen(mouse)
          GuiDraw.drawMultilineTip(
            mx + 12,
            my - 12,
            WrapAsJava.seqAsJavaList(data)
          )
          if (circuit.errors.contains(point))
            GuiDraw.drawMultilineTip(
              mx + 12,
              my - 32,
              Seq(EnumChatFormatting.RED.toString + circuit.errors(point)._1)
            )
          translateFromScreen()
          ClipNode.tempEnableScissoring()
        }
      }
    }
  }

  override def mouseClicked_Impl(
                                  p: Point,
                                  button: Int,
                                  consumed: Boolean
                                ): Boolean = {
    if (isCircuitValid && !consumed && rayTest(p)) button match {
      case 0 =>
        leftMouseDown = true
        mouseStart = toGridPoint(p)
        return true
      case 1 =>
        rightMouseDown = true
        val gridP = toGridPoint(p)
        circuit.getPart(gridP) match {
          case gp: IGuiCircuitPart =>
            val currentlyOpen = children.collect { case cg: CircuitGui => cg }
            if (!currentlyOpen.exists(_.part == gp)) {
              val gui = gp.createGui
              gui.position =
                convertPointFrom(Point(4, 4) * (currentlyOpen.size + 1), parent)
              gui.linePointerCalc = () => toCenteredGuiPoint(gridP)
              addChild(gui)
              gui.pushZTo(currentlyOpen.size * 0.1)
            }
          case _ =>
        }
        return true
      case _ if button == mcInst.gameSettings.keyBindPickBlock.getKeyCode =>
        doPickOp()
        return true
      case _ =>
    }
    false
  }

  override def mouseReleased_Impl(p: Point, button: Int, consumed: Boolean) = {
    if (leftMouseDown) {
      leftMouseDown = false
      val mouseEnd = toGridPoint(p)
      val opUsed = currentOp != null && circuit.sendOpUse(currentOp, mouseStart, mouseEnd)
      if (!opUsed && mouseEnd == mouseStart) {
        val part = circuit.getPart(mouseEnd)
        if (part != null) part.onClicked()
      }
    }
    if (rightMouseDown) {
      rightMouseDown = false
      val mouseEnd = toGridPoint(p)
      if (mouseEnd == mouseStart) {
        val part = circuit.getPart(mouseEnd)
        if (part != null) part.onActivated()
      }
    }
    false
  }

  override def mouseScrolled_Impl(p: Point, dir: Int, consumed: Boolean) = {
    if (!consumed && rayTest(p)) {
      if (dir > 0) rescaleAt(p, math.min(scale + 0.1, 3.0))
      else if (dir < 0) rescaleAt(p, math.max(scale - 0.1, 0.5))
      true
    } else false
  }

  override def keyPressed_Impl(c: Char, keycode: Int, consumed: Boolean) = {
    updatePreview()
    import Keyboard._
    if (!consumed) keycode match {
      case KEY_ESCAPE if leftMouseDown =>
        leftMouseDown = false
        true
      case KEY_ESCAPE if currentOp != null =>
        opPickDelegate(null)
        true
      case KEY_Q =>
        doPickOp()
        true
      case KEY_R if currentOp != null =>
        doRotate()
        true
      case KEY_C if currentOp != null =>
        doConfigure()
        true
      case _ if keycode == mcInst.gameSettings.keyBindInventory.getKeyCode =>
        opPickDelegate(CircuitOpDefs.Erase.getOp)
        true
      case _ => false
    }
    else false
  }

  def doRotate(): Unit = {
    currentOp match {
      case op: OpGateCommons =>
        op.rotation += 1
        if (op.rotation == 4) op.rotation = 0
      case _ =>
    }
  }

  def doConfigure(): Unit = {
    currentOp match {
      case op: OpGateCommons =>
        op.configuration += 1
      case _ =>
    }
  }

  def doPickOp() {
    val root = getRoot
    val i = Mouse.getX * root.width / root.mc.displayWidth
    val j = root.height - Mouse.getY * root.height / root.mc.displayHeight - 1
    val absPos = Point(i, j)

    val pos = parent.convertPointFromScreen(absPos)
    if (rayTest(pos)) {
      val part = circuit.getPart(toGridPoint(pos))
      opPickDelegate(if (part != null) part.getPickOp else null)
      updatePreview()
    }
  }

  def incDetail() {
    detailLevel = math.min(detailLevel + 1, 3)
  }

  def decDetail() {
    detailLevel = math.max(detailLevel - 1, 0)
  }

  def incScale() {
    rescaleAt(frame.midPoint, math.min(scale + 0.2, 3.0))
  }

  def decScale() {
    rescaleAt(frame.midPoint, math.max(scale - 0.2, 0.5))
  }

  def rescaleAt(point: Point, newScale: Double) {
    val p = parent.convertPointTo(point, this).vectorize
    val newP = (p / scale) * newScale
    val dp = newP - p
    scale = newScale
    position -= Point(dp)
  }
}
