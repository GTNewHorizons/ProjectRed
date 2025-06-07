/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{Transformation, Translation}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.wire._
import mrtjp.projectred.fabrication.circuitparts.{CircuitPart, CircuitPartDefs}
import mrtjp.projectred.fabrication.operations.CircuitOp.renderHolo

abstract class OpWire extends CircuitOp {
  override def checkOp(circuit: IntegratedCircuit, start: Point, end: Point) =
    circuit.getPart(start.x, start.y) == null

  override def getRotation(): Int = 0

  override def getConfiguration(): Int = 0

  override def writeOp(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ) {
    out.writeInt(start.x).writeInt(start.y)
    out.writeInt(end.x).writeInt(end.y)
  }

  override def readOp(circuit: IntegratedCircuit, in: MCDataInput) {
    val start = Point(in.readInt(), in.readInt())
    val end = Point(in.readInt(), in.readInt())
    val corner = start + Point((end - start).vectorize.axialProject)

    for (x <- math.min(start.x, corner.x) to math.max(start.x, corner.x)) {
      for (y <- math.min(start.y, corner.y) to math.max(start.y, corner.y)) {
        if (circuit.getPart(x, y) == null) {
          circuit.setPart(x, y, createPart)
        }
      }
    }
    for (x <- math.min(corner.x, end.x) to math.max(corner.x, end.x)) {
      for (y <- math.min(corner.y, end.y) to math.max(corner.y, end.y)) {
        if (circuit.getPart(x, y) == null) {
          circuit.setPart(x, y, createPart)
        }
      }
    }
  }

  def createPart: CircuitPart

  @SideOnly(Side.CLIENT)
  override def renderHover(
      position: Vec2,
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    val t = orthoPartT(position.subtract(prefboardOffset), scale)
    doRender(t, 0)
    renderHolo(position.subtract(prefboardOffset), scale, 0x33ffffff)
  }

  @SideOnly(Side.CLIENT)
  override def renderDrag(
      start: Vec2,
      end: Vec2,
      positionsWithParts: Seq[Vec2],
      scale: Double,
      prefboardOffset: Vec2
  ): Unit = {
    val corner = start + (end - start).axialProject

    for (
      x <- math.min(start.dx.toInt, corner.dx.toInt) to math.max(
        start.dx.toInt,
        corner.dx.toInt
      )
    ) {
      for (
        y <- math.min(start.dy.toInt, corner.dy.toInt) to math.max(
          start.dy.toInt,
          corner.dy.toInt
        )
      ) {
        if (!positionsWithParts.contains(Point(x, y))) {
          val t = orthoPartT(Vec2(x, y) - prefboardOffset, scale)
          doRender(t, 0)
        }
        renderHolo(
          Vec2(x, y) - prefboardOffset,
          corner.subtract(prefboardOffset),
          scale,
          0x44ffffff
        )
      }
    }
    for (
      x <- math.min(corner.dx.toInt, end.dx.toInt) to math.max(
        corner.dx.toInt,
        end.dx.toInt
      )
    ) {
      for (
        y <- math.min(corner.dy.toInt, end.dy.toInt) to math.max(
          corner.dy.toInt,
          end.dy.toInt
        )
      ) {
        if (!positionsWithParts.contains(Point(x, y))) {
          val t = orthoPartT(Vec2(x, y) - prefboardOffset, scale)
          doRender(t, 0)
        }
        renderHolo(
          Vec2(x, y) - prefboardOffset,
          corner.subtract(prefboardOffset),
          scale,
          0x44ffffff
        )
      }
    }

    // TODO Connections
//    if (circuit.getPart(px, py) == null) {
//      val t = orthoPartT(x, y, xSize, ySize, circuit.size, px, py)
//      var m = 0
//      if (px > start.x) { m |= 8; if (px != end2.x) m |= 2 }
//      if (px < start.x) { m |= 2; if (px != end2.x) m |= 8 }
//      if (py > start.y) { m |= 1; if (py != end2.y) m |= 4 }
//      if (py < start.y) { m |= 4; if (py != end2.y) m |= 1 }
//      if (px == start.x && end2.x > start.x) m |= 2
//      if (px == start.x && end2.x < start.x) m |= 8
//      if (py == start.y && end2.y > start.y) m |= 4
//      if (py == start.y && end2.y < start.y) m |= 1
//      doRender(t, m)
//    }
  }

  @SideOnly(Side.CLIENT)
  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ) {
    val t = orthoGridT(width, height) `with` new Translation(x, y, 0)
    doInvRender(t)
  }

  @SideOnly(Side.CLIENT)
  def doRender(t: Transformation, conn: Int)
  @SideOnly(Side.CLIENT)
  def doInvRender(t: Transformation)
}

class OpAlloyWire extends OpWire {
  override def createPart = CircuitPartDefs.AlloyWire.createPart

  @SideOnly(Side.CLIENT)
  override def doRender(t: Transformation, conn: Int) {
    val r = RenderICAlloyWire
    r.connMap = conn.toByte
    r.signal = 255.toByte
    r.render(t, true)
  }

  @SideOnly(Side.CLIENT)
  override def doInvRender(t: Transformation) {
    RenderICAlloyWire.prepairInv()
    RenderICAlloyWire.render(t, true)
  }

  @SideOnly(Side.CLIENT)
  override def getOpName = createPart.getPartName
}

class OpInsulatedWire(colour: Int) extends OpWire {
  override def createPart = {
    val part =
      CircuitPartDefs.InsulatedWire.createPart.asInstanceOf[InsulatedWireICPart]
    part.colour = colour.toByte
    part
  }

  @SideOnly(Side.CLIENT)
  override def doRender(t: Transformation, conn: Int) {
    val r = RenderICInsulatedWire
    r.connMap = conn.toByte
    r.signal = 255.toByte
    r.colour = colour.toByte
    r.render(t, true)
  }

  @SideOnly(Side.CLIENT)
  override def doInvRender(t: Transformation) {
    RenderICInsulatedWire.prepairInv(colour)
    RenderICInsulatedWire.render(t, true)
  }

  @SideOnly(Side.CLIENT)
  override def getOpName = Colors(colour & 0xff).name + " Insulated wire"
}

class OpBundledCable(colour: Int) extends OpWire {
  override def createPart = {
    val part =
      CircuitPartDefs.BundledCable.createPart.asInstanceOf[BundledCableICPart]
    part.colour = colour.toByte
    part
  }

  @SideOnly(Side.CLIENT)
  override def doRender(t: Transformation, conn: Int) {
    val r = RenderICBundledCable
    r.connMap = conn.toByte
    r.colour = colour.toByte
    r.render(t, true)
  }

  @SideOnly(Side.CLIENT)
  override def doInvRender(t: Transformation) {
    RenderICBundledCable.prepairInv(colour)
    RenderICBundledCable.render(t, true)
  }

  @SideOnly(Side.CLIENT)
  override def getOpName = (if (colour != -1) Colors(colour & 0xff).name + " "
                            else "") + "Bundled cable"
}
