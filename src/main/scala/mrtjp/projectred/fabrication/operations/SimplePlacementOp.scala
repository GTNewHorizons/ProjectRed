/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{Rotation, Transformation, Translation}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.Point
import mrtjp.projectred.fabrication.ICComponentStore.{orthoGridT, orthoPartT}
import mrtjp.projectred.fabrication.operations.CircuitOp.{isOnBorder, renderHolo}
import mrtjp.projectred.fabrication.IntegratedCircuit
import mrtjp.projectred.fabrication.circuitparts.CircuitPart


abstract class SimplePlacementOp extends CircuitOp {
  override def checkOp(circuit: IntegratedCircuit, start: Point, end: Point) =
    circuit.getPart(end.x, end.y) == null

  override def getRotation(): Int = 0

  override def getConfiguration(): Int = 0

  override def writeOp(
                        circuit: IntegratedCircuit,
                        start: Point,
                        end: Point,
                        out: MCDataOutput
                      ) {
    out.writeByte(end.x).writeByte(end.y)
  }

  override def readOp(circuit: IntegratedCircuit, in: MCDataInput) {
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
                            circuit: IntegratedCircuit,
                            point: Point,
                            x: Double,
                            y: Double,
                            xSize: Double,
                            ySize: Double
                          ) {
    if (circuit.getPart(point) != null) return

    renderHolo(
      x,
      y,
      xSize,
      ySize,
      circuit.size,
      point,
      if (!isOnBorder(circuit.size, point)) 0x33ffffff else 0x33ff0000
    )

    val t = orthoPartT(x, y, xSize, ySize, circuit.size, point.x, point.y)
    doPartRender(t)

  }

  @SideOnly(Side.CLIENT)
  override def renderDrag(
                           circuit: IntegratedCircuit,
                           start: Point,
                           end: Point,
                           x: Double,
                           y: Double,
                           xSize: Double,
                           ySize: Double
                         ) {
    if (circuit.getPart(end) != null) return

    renderHolo(
      x,
      y,
      xSize,
      ySize,
      circuit.size,
      end,
      if (!isOnBorder(circuit.size, end)) 0x44ffffff else 0x44ff0000
    )

    val t = orthoPartT(x, y, xSize, ySize, circuit.size, end.x, end.y)
    doPartRender(t)
  }

  def doPartRender(t: Transformation)

  def createPart: CircuitPart
}
