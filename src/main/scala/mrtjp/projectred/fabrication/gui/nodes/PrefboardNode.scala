package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.gui.{ClipNode, TNode}
import mrtjp.core.vec.{Point, Rect, Vec2}
import mrtjp.projectred.fabrication.circuitparts.ICGateDefinition
import mrtjp.projectred.fabrication.circuitparts.io.IOICGateLogic
import mrtjp.projectred.fabrication.circuitparts.latches.{
  SRLatch,
  TransparentLatch
}
import mrtjp.projectred.fabrication.circuitparts.misc.{
  Counter,
  DecRandomizer,
  Randomizer
}
import mrtjp.projectred.fabrication.circuitparts.primitives._
import mrtjp.projectred.fabrication.circuitparts.timing.{
  Repeater,
  Sequencer,
  StateCell
}
import mrtjp.projectred.fabrication.operations._
import mrtjp.projectred.fabrication.{IntegratedCircuit, RenderCircuit}
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.input.{Keyboard, Mouse}

import scala.collection.JavaConversions._
import scala.collection.convert.WrapAsJava

class PrefboardNode(
    circuit: IntegratedCircuit,
    hasBlueprint: Boolean,
    previewUpdateDelegate: CircuitOp => Unit,
    setConfigNode: TNode => Unit
) extends TNode {
  var currentOp: CircuitOp = null

  /** 0 - off 1 - name only 2 - minor details 3 - all details
    */
  var detailLevel = 1
  var scale = 1.0

  var offset: Vec2 = Vec2(0, 0)

  def updatePreview(): Unit = {
    previewUpdateDelegate(currentOp)
  }

  override def frame = Rect(
    position,
    parent.frame.size
  )

  var opPickDelegate = { _: CircuitOp => () }

  private var leftMouseDown = false
  private var rightMouseDown = false
  private var mouseStart = Point(0, 0)

  /** Converts coordinates in the gui to coordinates in the circuit (with
    * rounding)
    */
  private def toGridPoint(p: Vec2): Point = {
    val circuitCoord = p / (RenderCircuit.BASE_SCALE * scale) + offset
    Point(
      math.floor(circuitCoord.dx).round.toInt,
      math.floor(circuitCoord.dy).round.toInt
    )
  }

  private def toGridPoint(p: Point): Point = {
    val circuitCoord = p.vectorize / (RenderCircuit.BASE_SCALE * scale) + offset
    Point(
      math.floor(circuitCoord.dx).round.toInt,
      math.floor(circuitCoord.dy).round.toInt
    )
  }

  private def toCenteredGuiPoint(gridP: Point) = {
    val dp = frame.size.vectorize / 16
    Point(gridP.vectorize * dp + dp / 2)
  }

  override def update_Impl() {
    if (mcInst.theWorld.getTotalWorldTime % 20 == 0)
      circuit.refreshErrors()
  }

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    if (hasBlueprint) {
      val f = frame
      RenderCircuit.renderOrtho(
        circuit,
        parent.frame.size,
        scale,
        offset
      )

      if (currentOp != null) {
        if (rayTest(mouse) && !leftMouseDown) {
          // Render: Erase always, other stuff only if there are no already placed parts under the cursor
          currentOp match {
            case _: OpGate | _: OpWire | _: OpSimplePlacement =>
              if (circuit.getPart(toGridPoint(mouse.vectorize)) == null)
                currentOp.renderHover(
                  toGridPoint(mouse).vectorize,
                  scale,
                  offset
                )
            case _: OpAreaBase =>
              currentOp.renderHover(toGridPoint(mouse).vectorize, scale, offset)
          }
        } else if (leftMouseDown) {
          currentOp.renderDrag(
            mouseStart.vectorize,
            toGridPoint(mouse).vectorize,
            CircuitOp.partsBetweenPoints(
              mouseStart.vectorize,
              toGridPoint(mouse).vectorize,
              circuit
            ),
            scale,
            offset
          )
        }
      }
      RenderCircuit.renderErrors(circuit, scale, offset)
    }
  }

  override def drawFront_Impl(mouse: Point, rframe: Float) {
    if (!leftMouseDown && rayTest(mouse)) {
      val point = toGridPoint(mouse.vectorize)
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

  private var mousePosition = Point(0, 0)
  override def mouseDragged_Impl(
      p: Point,
      button: Int,
      time: Long,
      consumed: Boolean
  ): Boolean = {
    if (!consumed && rayTest(p) && button == 0 && currentOp == null) {
      if (time > 20) {
        offset =
          offset - (p - mousePosition).vectorize / (RenderCircuit.BASE_SCALE * scale)
      }
      mousePosition = p
      true
    } else {
      mousePosition = p
      false
    }
  }

  override def mouseClicked_Impl(
      p: Point,
      button: Int,
      consumed: Boolean
  ): Boolean = {
    if (!consumed && rayTest(p)) button match {
      case 0 =>
        leftMouseDown = true
        mouseStart = toGridPoint(p.vectorize)
        return true
      case 1 =>
        rightMouseDown = true
        val gridP = toGridPoint(p.vectorize)
        circuit.getPart(gridP) match {
          case gp: TConfigurable =>
            setConfigNode(gp.createConfigurationNode)
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
      val mouseEnd = toGridPoint(p.vectorize)
      val opUsed = currentOp != null && circuit.sendOpUse(
        currentOp,
        mouseStart,
        mouseEnd
      )
      if (!opUsed && mouseStart == mouseEnd) {
        val part = circuit.getPart(mouseEnd)
        if (part != null) part.onClicked()
      }
    }
    if (rightMouseDown) {
      rightMouseDown = false
      val mouseEnd = toGridPoint(p.vectorize)
      if (mouseEnd == mouseStart) {
        val part = circuit.getPart(mouseEnd)
        if (part != null) part.onActivated()
      }
    }
    false
  }

  override def mouseScrolled_Impl(p: Point, dir: Int, consumed: Boolean) = {
    if (!consumed && rayTest(p)) {
      if (dir > 0) rescaleAt(p, math.min(scale * 1.25, 3.0))
      else if (dir < 0) rescaleAt(p, math.max(scale * 0.8, 0.5))
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
      case _ => false
    }
    else false
  }

  def doRotate(): Unit = {
    currentOp match {
      case op: OpGate =>
        op.rotation += 1
        if (op.rotation == 4) op.rotation = 0
      case op: OpAreaBase =>
        op.rotateClipboard()
      case _ =>
    }
  }

  def doConfigure(): Unit = {
    currentOp match {
      case op: OpGate =>
        op.getID match {
          case ICGateDefinition.OR.ordinal =>
            op.configuration = OR.cycleShape(op.configuration)
          case ICGateDefinition.NOR.ordinal =>
            op.configuration = NOR.cycleShape(op.configuration)
          case ICGateDefinition.NOT.ordinal =>
            op.configuration = NOT.cycleShape(op.configuration)
          case ICGateDefinition.AND.ordinal =>
            op.configuration = AND.cycleShape(op.configuration)
          case ICGateDefinition.NAND.ordinal =>
            op.configuration = NAND.cycleShape(op.configuration)
          case ICGateDefinition.XOR.ordinal =>
            op.configuration = XOR.cycleShape(op.configuration)
          case ICGateDefinition.XNOR.ordinal =>
            op.configuration = XNOR.cycleShape(op.configuration)
          case ICGateDefinition.Buffer.ordinal =>
            op.configuration = Buffer.cycleShape(op.configuration)
          case ICGateDefinition.Multiplexer.ordinal =>
            op.configuration = Multiplexer.cycleShape(op.configuration)
          case ICGateDefinition.Pulse.ordinal =>
            op.configuration = Pulse.cycleShape(op.configuration)
          case ICGateDefinition.Repeater.ordinal =>
            op.configuration = Repeater.cycleShape(op.configuration)
          case ICGateDefinition.Randomizer.ordinal =>
            op.configuration = Randomizer.cycleShape(op.configuration)
          case ICGateDefinition.TransparentLatch.ordinal =>
            op.configuration = TransparentLatch.cycleShape(op.configuration)
          case ICGateDefinition.DecRandomizer.ordinal =>
            op.configuration = DecRandomizer.cycleShape(op.configuration)
          case ICGateDefinition.IOAnalog.ordinal |
              ICGateDefinition.IOSimple.ordinal |
              ICGateDefinition.IOBundled.ordinal =>
            op.configuration = IOICGateLogic.cycleShape(op.configuration)
          case ICGateDefinition.Sequencer.ordinal =>
            op.configuration = Sequencer.cycleShape(op.configuration)
          case ICGateDefinition.StateCell.ordinal =>
            op.configuration = StateCell.cycleShape(op.configuration)
          case ICGateDefinition.SRLatch.ordinal =>
            op.configuration = SRLatch.cycleShape(op.configuration)
          case ICGateDefinition.Counter.ordinal =>
            op.configuration = Counter.cycleShape(op.configuration)
          case _ =>
            op.configuration = 0
        }
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
      val part = circuit.getPart(toGridPoint(pos.vectorize))
      opPickDelegate(if (part != null) {
        setConfigNode(null)
        part.getCircuitOperation
      } else null)
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
    rescaleAt(parent.frame.midPoint, math.min(scale + 0.2, 3.0))
  }

  def decScale() {
    rescaleAt(parent.frame.midPoint, math.max(scale - 0.2, 0.5))
  }

  private def rescaleAt(point: Point, newScale: Double): Unit = {
    val newP = (point.vectorize / scale) * newScale
    val dp = (newP - point.vectorize) / (RenderCircuit.BASE_SCALE * scale)
    offset = offset + dp
    scale = newScale
  }

  /** Scales the prefboard to the circuit with some border
    */
  def scaleGuiToCircuit() = {
    val circuitBounds = circuit.getPartsBoundingBox()
    val circuitBoundsWithPadding = circuitBounds.union(
      Rect(circuitBounds.origin.subtract(1, 1), circuitBounds.size.add(3))
    )
    // Required scaling to fit whole circuit on the screen
    val requiredScale = math.min(
      (parent.frame.size.width / RenderCircuit.BASE_SCALE.toDouble) / circuitBoundsWithPadding.size.width,
      (parent.frame.size.height / RenderCircuit.BASE_SCALE.toDouble) / circuitBoundsWithPadding.size.height
    )
    offset = circuitBoundsWithPadding.origin.vectorize
    scale = requiredScale
  }
}
