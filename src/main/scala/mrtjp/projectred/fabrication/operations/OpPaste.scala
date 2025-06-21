package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.IntegratedCircuit

class OpPaste extends OpAreaBase {

  override def checkOp(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point
  ): Boolean = {
    OpAreaBase.clipboard.forall(op => circuit.getPart(start + op._1) == null)
  }

  override def clientSendOperation(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ): Unit = {
    super.clientSendOperation(circuit, start, end, out)
    out.writeInt(OpAreaBase.clipboard.size)
    OpAreaBase.clipboard.foreach { op =>
      op._2.clientSendOperation(circuit, start + op._1, end + op._1, out)
    }
  }

  override protected def serverReceiveOperation(
      circuit: IntegratedCircuit,
      in: MCDataInput
  ): Unit = {
    for (_ <- 0 until in.readInt()) {
      CircuitOp.readOp(circuit, in)
    }
  }

  override def getOpName: String = "Paste"

  override def renderHover(
      position: Vec2,
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    for (part <- OpAreaBase.clipboard) {
      part._2.renderHover(position + part._1.vectorize, scale, prefboardOffset)
    }
  }

  override def renderDrag(
      start: Vec2,
      end: Vec2,
      positionsWithParts: Seq[Vec2],
      scale: Double,
      prefboardOffset: Vec2
  ): Unit =
    renderHover(start, scale, prefboardOffset)

  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ): Unit = {
    // TODO render paste icon
  }
}
