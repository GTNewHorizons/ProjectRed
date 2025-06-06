/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.IntegratedCircuit


trait CircuitOp {
  var id = -1

  def checkOp(circuit: IntegratedCircuit, start: Point, end: Point): Boolean

  def writeOp(
               circuit: IntegratedCircuit,
               start: Point,
               end: Point,
               rotation: Int,
               configuration: Int,
               out: MCDataOutput
             )

  def readOp(circuit: IntegratedCircuit, in: MCDataInput)

  @SideOnly(Side.CLIENT)
  def getOpName: String

  @SideOnly(Side.CLIENT)
  def renderHover(
                   circuit: IntegratedCircuit,
                   point: Point,
                   rot: Int,
                   configuration: Int,
                   x: Double,
                   y: Double,
                   xSize: Double,
                   ySize: Double
                 )

  @SideOnly(Side.CLIENT)
  def renderDrag(
                  circuit: IntegratedCircuit,
                  start: Point,
                  end: Point,
                  x: Double,
                  y: Double,
                  xSize: Double,
                  ySize: Double
                )

  @SideOnly(Side.CLIENT)
  def renderImage(x: Double, y: Double, width: Double, height: Double)
}

object CircuitOp {
  def getOperation(id: Int) = CircuitOpDefs(id).getOp

  def renderHolo(
                  x: Double,
                  y: Double,
                  xSize: Double,
                  ySize: Double,
                  csize: Size,
                  point: Point,
                  colour: Int
                ) {
    val x1 = (x + xSize / csize.width * point.x).toInt
    val y1 = (y + ySize / csize.height * point.y).toInt
    val x2 = (x + xSize / csize.width * (point.x + 1)).toInt
    val y2 = (y + ySize / csize.height * (point.y + 1)).toInt

    GuiDraw.drawRect(x1, y1, x2 - x1, y2 - y1, colour)
  }

  def isOnBorder(cSize: Size, point: Point) =
    point.x == 0 || point.y == 0 || point.x == cSize.width - 1 || point.y == cSize.height - 1

  def isOnEdge(cSize: Size, point: Point) =
    point == Point(0, 0) || point == Point(
      0,
      cSize.height - 1
    ) || point == Point(cSize.width - 1, 0) || point == Point(
      cSize.width - 1,
      cSize.height - 1
    )
}