package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.IntegratedCircuit

class OpCopy extends OpAreaBase {

  override def clientSendOperation(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ): Unit = {
    // Save Parts in clipboard (only client)
    val topLeft = Point(math.min(start.x, end.x), math.min(start.y, end.y))
    val bottomRight = Point(math.max(start.x, end.x), math.max(start.y, end.y))
    val parts = circuit.getParts(topLeft, bottomRight + Point(1, 1))
    saveToClipboard(parts)
    super.clientSendOperation(circuit, start, end, out)
  }

  // Nothing to process
  override protected def serverReceiveOperation(
      circuit: IntegratedCircuit,
      in: MCDataInput
  ): Unit = {}

  override def getOpName: String = "Copy"

  override def renderHover(
      position: Vec2,
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    // TODO
  }

  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ): Unit = {}
}
