/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.operations

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{Rotation, Transformation, Translation, Vector3}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.vec.{Point, Vec2}
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{CircuitPart, GateICPart, ICGateDefinition, ICGateRenderer}
import mrtjp.projectred.fabrication.operations.CircuitOp.renderHolo


class OpGate(meta: Int) extends CircuitOp {

  var rotation: Int = 0
  var configuration: Int = 0

  def getID: Int = meta

  override def checkOp(circuit: IntegratedCircuit, start: Point, end: Point) =
    circuit.getPart(start) == null

  override def getRotation(): Int = rotation

  override def getConfiguration(): Int = configuration

  override def writeOp(
      circuit: IntegratedCircuit,
      start: Point,
      end: Point,
      out: MCDataOutput
  ) {
    out.writeByte(start.x).writeByte(start.y)
    out.writeByte(rotation)
    out.writeByte(configuration)
  }

  override def readOp(circuit: IntegratedCircuit, in: MCDataInput) {
    val point = Point(in.readByte(), in.readByte())
    rotation = in.readUByte()
    configuration = in.readUByte()

    if (circuit.getPart(point) == null) {
      val part = CircuitPart
        .createPart(ICGateDefinition(meta).gateType)
        .asInstanceOf[GateICPart]
      part.preparePlacement(rotation, configuration, meta)
      circuit.setPart(point, part)
    }
  }

  @SideOnly(Side.CLIENT)
  override def renderHover(position: Vec2, scale: Double, prefboardOffset: Vec2): Unit = {
    val t = orthoPartT(position.subtract(prefboardOffset), scale)
    doRender(t, rotation, configuration)
    renderHolo(position.subtract(prefboardOffset), scale, 0x33ffffff)
  }

  @SideOnly(Side.CLIENT)
  def renderDrag(start: Vec2, end: Vec2, positionsWithParts: Seq[Vec2], scale: Double, prefboardOffset: Vec2): Unit = {
    // Gates can't be dragged, so only the first will be rendered
    val t = orthoPartT(start - prefboardOffset, scale)
    doRender(t, rotation, configuration)
    renderHolo(start - prefboardOffset, scale, 0x44ffffff)
  }

  @SideOnly(Side.CLIENT)
  override def renderImage(
      x: Double,
      y: Double,
      width: Double,
      height: Double
  ) {
    val t = orthoGridT(width, height) `with` new Translation(x, y, 0)
    doRender(t, rotation, configuration)
  }

  override def renderImageStatic(x: Double, y: Double, width: Double, height: Double): Unit = {
    val t = orthoGridT(width, height) `with` new Translation(x, y, 0)
    doRender(t, 0, 0)
  }

  @SideOnly(Side.CLIENT)
  def doRender(t: Transformation, rot: Int, configuration: Int) {
    ICGateRenderer.renderWithConfiguration(configuration, Rotation.quarterRotations(rot).at(Vector3.center) `with` t, meta)
  }

  @SideOnly(Side.CLIENT)
  override def getOpName = ICGateDefinition(meta).unlocal
}
