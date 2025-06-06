/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.render.uv.{UVScale, UVTranslation}
import codechicken.lib.vec.Translation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication.IntegratedCircuit

class CircuitOpErase extends CircuitOp {
  override def checkOp(circuit: IntegratedCircuit, start: Point, end: Point) =
    true

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

    for (x <- math.min(start.x, end.x) to math.max(start.x, end.x))
      for (y <- math.min(start.y, end.y) to math.max(start.y, end.y))
        circuit.removePart(x, y)
  }

  @SideOnly(Side.CLIENT)
  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ) {
    val t = orthoGridT(width, height) `with` new Translation(x, y, 0)

    prepairRender()
    PRResources.guiPrototyper.bind()
    faceModels(dynamicIdx(0, true)).render(
      t,
      new UVScale(16) `with` new UVTranslation(330, 18) `with` new UVScale(
        1 / 512d
      )
    )
    finishRender()
  }

  @SideOnly(Side.CLIENT)
  override def renderHover(position: Vec2, scale: Double, prefboardOffset: Vec2): Unit = {
    CircuitOp.renderHolo(position - prefboardOffset, scale, 0x33ff0000)
  }

  @SideOnly(Side.CLIENT)
  override def renderDrag(start: Vec2, end: Vec2, positionsWithParts: Seq[Vec2], scale: Double, prefboardOffset: Vec2): Unit = {
    var (topLeft, bottomRight) = (
      Vec2(math.min(start.dx, end.dx), math.min(start.dy, end.dy)).subtract(prefboardOffset),
      Vec2(math.max(start.dx, end.dx), math.max(start.dy, end.dy)).subtract(prefboardOffset)
    )
    bottomRight = bottomRight.add(1, 1)
    CircuitOp.renderHolo(topLeft, bottomRight, scale, 0x44ffffff)
    for(posWithPart <- positionsWithParts) {
      CircuitOp.renderHolo(posWithPart - prefboardOffset, scale, 0x44ff0000)
    }
  }

  @SideOnly(Side.CLIENT)
  override def getOpName = "Erase"
}
