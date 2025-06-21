package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{Transformation, Translation}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.ICComponentStore.{orthoGridT, orthoPartT}
import mrtjp.projectred.fabrication.IntegratedCircuit
import mrtjp.projectred.fabrication.circuitparts.CircuitPart
import mrtjp.projectred.fabrication.operations.CircuitOp.renderHolo

abstract class OpSimplePlacement extends CircuitOp {
  override def checkOp(circuit: IntegratedCircuit, start: Point, end: Point) =
    circuit.getPart(end.x, end.y) == null

  override def getRotation(): Int = 0

  override def getConfiguration(): Int = 0

  override def clientSendOperation(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ) {
    super.clientSendOperation(circuit, start, end, out)
    out.writeByte(end.x).writeByte(end.y)
  }

  override def serverReceiveOperation(
      circuit: IntegratedCircuit,
      in: MCDataInput
  ) {
    val point = Point(in.readUByte(), in.readUByte())
    if (circuit.getPart(point.x, point.y) == null)
      circuit.setPart(point.x, point.y, createPart)
  }

  @SideOnly(Side.CLIENT)
  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ): Unit = {
    val t = orthoGridT(width, height) `with` new Translation(x, y, 0)
    doPartRender(t)
  }

  @SideOnly(Side.CLIENT)
  override def renderHover(
      position: Vec2,
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    val t = orthoPartT(position - prefboardOffset, scale)
    doPartRender(t)
    renderHolo(position.subtract(prefboardOffset), scale, 0x33ff0000)
  }

  @SideOnly(Side.CLIENT)
  override def renderDrag(
      start: Vec2,
      end: Vec2,
      positionsWithParts: Seq[Vec2],
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    val t = orthoPartT(end - prefboardOffset, scale)
    doPartRender(t)
    renderHolo(end - prefboardOffset, scale, 0x44ffffff)
  }

  def doPartRender(t: Transformation)

  def createPart: CircuitPart
}
