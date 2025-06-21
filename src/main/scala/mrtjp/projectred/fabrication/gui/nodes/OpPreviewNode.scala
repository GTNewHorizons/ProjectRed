package mrtjp.projectred.fabrication.gui.nodes

import mrtjp.core.gui.TNode
import mrtjp.core.vec.Point
import mrtjp.projectred.fabrication.operations.CircuitOp

class OpPreviewNode extends TNode {

  var currentOp: CircuitOp = null

  def updatePreview(op: CircuitOp): Unit = {
    currentOp = op
  }

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    if (currentOp != null) {
      currentOp.renderImage(
        position.x,
        position.y,
        50,
        50
      )
    }
  }
}
