package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.{IntegratedCircuit, RenderCircuit}

/** Circuit Operations describe the communication between server and client:
  *
  *   - 1: Client sends operation to the server with [[clientSendOperation]]
  *   - 2: Server receives operation with [[serverReceiveOperation]] and sends
  *     updates back to the client(s). The sending back is done in
  *     [[IntegratedCircuit]]
  *   - 3: Client(s) receive updated circuit. This step is handled in
  *     [[IntegratedCircuit]]
  */
trait CircuitOp {
  var id = -1

  /** Can this operation be executed
    */
  def checkOp(circuit: IntegratedCircuit, start: Point, end: Point): Boolean

  def getRotation(): Int

  def getConfiguration(): Int

  def clientSendOperation(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ): Unit = {
    out.writeByte(id)
  }

  protected def serverReceiveOperation(
      circuit: IntegratedCircuit,
      in: MCDataInput
  ): Unit

  @SideOnly(Side.CLIENT)
  def getOpName: String

  /** Render the selected Operation, when on the mouse cursor
    * @param position
    *   Position in Grid coordinates
    * @param scale
    *   Prefboard scaling
    */
  @SideOnly(Side.CLIENT)
  def renderHover(position: Vec2, scale: Double, prefboardOffset: Vec2): Unit

  /** Render the selected Operation when dragging
    * @param start
    *   Start in Grid coordinates
    * @param end
    *   End in Grid coordinates
    * @param positionsWithParts
    *   Positions in Rect(start, end) with existing parts
    * @param scale
    *   Prefboard scaling
    * @param prefboardOffset
    *   Prefboard offset
    */
  @SideOnly(Side.CLIENT)
  def renderDrag(
      start: Vec2,
      end: Vec2,
      positionsWithParts: Seq[Vec2],
      scale: Double,
      prefboardOffset: Vec2
  ): Unit

  /** Render the part, that will be placed by this operation
    */
  @SideOnly(Side.CLIENT)
  def renderImage(x: Double, y: Double, width: Double, height: Double)

  /** Same as renderImage, however it ignores configuration and rotation (e.g.
    * toolbar)
    */
  @SideOnly(Side.CLIENT)
  def renderImageStatic(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ): Unit =
    renderImage(x, y, width, height)
}

object CircuitOp {

  /** Read operation from stream and create Circuit Part in circuit
    * @param circuit
    *   The circuit in which the part is being created
    * @param in
    *   Stream
    */
  def readOp(circuit: IntegratedCircuit, in: MCDataInput): Unit = {
    getOperation(in.readUByte()).serverReceiveOperation(circuit, in)
  }

  /** IDs as in CircuitOpDefs -> CircuitOp
    */
  private def getOperation(id: Int): CircuitOp = CircuitOpDefs(id).getOp

  /** Draw a rectangle in the size of one tile
    * @param position
    *   In Grid Coordinates
    * @param colour
    *   in ARGB
    */
  def renderHolo(position: Vec2, scale: Double, colour: Int): Unit = {
    val start = position * RenderCircuit.BASE_SCALE * scale
    val end = start + Vec2(
      RenderCircuit.BASE_SCALE * scale,
      RenderCircuit.BASE_SCALE * scale
    )
    GuiDraw.drawRect(
      start.dx.toInt,
      start.dy.toInt,
      (end - start).dx.toInt,
      (end - start).dy.toInt,
      colour
    )
  }

  /** Same as other renderHolo, only for drawing a rectangle over multiple cells
    * of the grid
    * @param topLeft
    *   In Grid Coordinates
    * @param bottomRight
    *   In Grid coordinates, excluding that cell
    */
  def renderHolo(
      topLeft: Vec2,
      bottomRight: Vec2,
      scale: Double,
      colour: Int
  ): Unit = {
    val s = topLeft * RenderCircuit.BASE_SCALE * scale
    val e = bottomRight * RenderCircuit.BASE_SCALE * scale
    GuiDraw.drawRect(
      s.dx.toInt,
      s.dy.toInt,
      (e - s).dx.toInt,
      (e - s).dy.toInt,
      colour
    )
  }

  def partsBetweenPoints(
      start: Vec2,
      end: Vec2,
      circuit: IntegratedCircuit
  ): Seq[Vec2] = {
    var partPositions: Seq[Vec2] = Seq.empty
    for (
      x <- math.min(start.dx.toInt, end.dx.toInt) to math.max(
        start.dx.toInt,
        end.dx.toInt
      )
    ) {
      for (
        y <- math.min(start.dy.toInt, end.dy.toInt) to math.max(
          start.dy.toInt,
          end.dy.toInt
        )
      ) {
        if (circuit.getPart(x, y) != null) {
          partPositions = partPositions :+ Vec2(x, y)
        }
      }
    }
    partPositions
  }
}
