/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Size, Vec2}
import mrtjp.projectred.fabrication.{IntegratedCircuit, RenderCircuit}


trait CircuitOp {
  var id = -1

  def checkOp(circuit: IntegratedCircuit, start: Point, end: Point): Boolean

  def getRotation(): Int

  def getConfiguration(): Int

  def writeOp(
               circuit: IntegratedCircuit,
               start: Point,
               end: Point,
               out: MCDataOutput
             )

  def readOp(circuit: IntegratedCircuit, in: MCDataInput)

  @SideOnly(Side.CLIENT)
  def getOpName: String

  /**
   * Render the selected Operation, when on the mouse cursor
   * @param position Position in Grid coordinates
   * @param scale Prefboard scaling
   */
  @SideOnly(Side.CLIENT)
  def renderHover(position: Vec2, scale: Double, prefboardOffset: Vec2): Unit

  /**
   * Render the selected Operation when dragging
   * @param start Start in Grid coordinates
   * @param end End in Grid coordinates
   * @param positionsWithParts Positions in Rect(start, end) with existing parts
   * @param scale Prefboard scaling
   * @param prefboardOffset Prefboard offset
   */
  @SideOnly(Side.CLIENT)
  def renderDrag(start: Vec2, end: Vec2, positionsWithParts: Seq[Vec2], scale: Double, prefboardOffset: Vec2): Unit

  /**
   * Render the part, that will be placed by this operation
   */
  @SideOnly(Side.CLIENT)
  def renderImage(x: Double, y: Double, width: Double, height: Double)

  /**
   * Same as renderImage, however it ignores configuration and rotation (e.g. toolbar)
   */
  @SideOnly(Side.CLIENT)
  def renderImageStatic(x: Double, y: Double, width: Double, height: Double): Unit =
    renderImage(x, y ,width, height)
}

object CircuitOp {
  def getOperation(id: Int) = CircuitOpDefs(id).getOp

  /**
   * Draw one Tile
   * @param position In Grid Coordinates
   */
  def renderHolo(position: Vec2, scale: Double, colour: Int): Unit = {
    val start = position * RenderCircuit.BASE_SCALE * scale
    val end = start + Vec2(RenderCircuit.BASE_SCALE * scale, RenderCircuit.BASE_SCALE * scale)
    GuiDraw.drawRect(start.dx.toInt, start.dy.toInt, (end - start).dx.toInt, (end - start).dy.toInt, colour)
  }

  /**
   * Draw Multiple tiles in a rect excluding end
   * @param start In Grid Coordinates
   * @param end In Grid coordinates
   */
  def renderHolo(start: Vec2, end: Vec2, scale: Double, colour: Int): Unit = {
    val s = start * RenderCircuit.BASE_SCALE * scale
    val e = end * RenderCircuit.BASE_SCALE * scale
    GuiDraw.drawRect(s.dx.toInt, s.dy.toInt, (e - s).dx.toInt, (e - s).dy.toInt, colour)
  }

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

  def partsBetweenPoints(start: Vec2, end: Vec2, circuit: IntegratedCircuit): Seq[Vec2] = {
    var partPositions: Seq[Vec2] = Seq.empty
    for(x <- math.min(start.dx.toInt, end.dx.toInt) to math.max(start.dx.toInt, end.dx.toInt)) {
      for(y <- math.min(start.dy.toInt, end.dy.toInt) to math.max(start.dy.toInt, end.dy.toInt)) {
        if(circuit.getPart(x, y) != null) {
          partPositions = partPositions :+ Vec2(x, y)
        }
      }
    }
    partPositions
  }
}
