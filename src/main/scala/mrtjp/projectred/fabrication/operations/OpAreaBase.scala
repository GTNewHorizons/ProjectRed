package mrtjp.projectred.fabrication.operations

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.IntegratedCircuit
import mrtjp.projectred.fabrication.circuitparts.CircuitPart
import mrtjp.projectred.fabrication.operations.OpAreaBase.clipboard

object OpAreaBase {
  type List = Seq[(Point, CircuitOp)]

  /** clipboard for copy/past/cut operations
    */
  var clipboard: List = Seq()
}

abstract class OpAreaBase extends CircuitOp {

  override def checkOp(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point
  ): Boolean = true

  override def getConfiguration(): Int = 0

  override def getRotation(): Int = 0

  /** Draw partially transparent rectangle over area
    */
  @SideOnly(Side.CLIENT)
  override def renderDrag(
      start: Vec2,
      end: Vec2,
      positionsWithParts: Seq[Vec2],
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    var (topLeft, bottomRight) = (
      Vec2(
        math.min(start.dx, end.dx),
        math.min(start.dy, end.dy)
      ) - prefboardOffset,
      Vec2(
        math.max(start.dx, end.dx),
        math.max(start.dy, end.dy)
      ) - prefboardOffset
    )
    bottomRight = bottomRight.add(1, 1)
    CircuitOp.renderHolo(topLeft, bottomRight, scale, 0x44ffffff)
  }

  /** Saves parts to [[OpAreaBase.clipboard]]
    */
  protected def saveToClipboard(parts: Map[(Int, Int), CircuitPart]): Unit = {
    val (x, y) = if (parts.nonEmpty) {
      parts
        .reduce((a, b) =>
          ((math.min(a._1._1, b._1._1), math.min(a._1._2, b._1._2)), null)
        )
        ._1
    } else (0, 0)
    OpAreaBase.clipboard = parts
      .map(element => {
        (
          Point(element._1._1 - x, element._1._2 - y),
          element._2.getCircuitOperation
        )
      })
      .toList
  }

  def rotateClipboard(): Unit = {
    if (clipboard.size > 1) {
      val p2 = clipboard
        .reduce((a, b) =>
          (Point(math.max(a._1.x, b._1.x), math.max(a._1.y, b._1.y)), null)
        )
        ._1
      clipboard = clipboard.map { item =>
        (
          Point(-item._1.y + p2.y, item._1.x),
          item._2 match {
            case a: OpGate =>
              a.rotation = (a.rotation + 1) % 4
              a
            case a => a
          }
        )
      }.toList
    }
  }
}
