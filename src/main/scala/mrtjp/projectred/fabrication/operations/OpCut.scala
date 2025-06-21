package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.MCDataOutput
import mrtjp.core.vec.Point
import mrtjp.projectred.fabrication.IntegratedCircuit

class OpCut extends OpErase {
  override def clientSendOperation(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ): Unit = {
    // Save Parts in clipboard (only client
    val topLeft = Point(math.min(start.x, end.x), math.min(start.y, end.y))
    val bottomRight = Point(math.max(start.x, end.x), math.max(start.y, end.y))
    val parts = circuit.getParts(topLeft, bottomRight + Point(1, 1))
    saveToClipboard(parts)
    // Delete parts
    super.clientSendOperation(circuit, start, end, out)
  }

  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ): Unit = {}

  override def getOpName: String = "Cut"
}
