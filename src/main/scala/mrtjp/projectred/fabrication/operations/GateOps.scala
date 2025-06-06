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
import mrtjp.projectred.fabrication.operations.CircuitOp.{isOnBorder, isOnEdge, renderHolo}


abstract class OpGateCommons(meta: Int) extends CircuitOp {

  var rotation: Int = 0
  var configuration: Int = 0

  def getID: Int = meta

  def canPlace(circuit: IntegratedCircuit, point: Point): Boolean
  def findRot(circuit: IntegratedCircuit, start: Point, end: Point): Int

  override def checkOp(circuit: IntegratedCircuit, start: Point, end: Point) =
    canPlace(circuit, start) && circuit.getPart(start) == null

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

    if (circuit.getPart(point) == null && canPlace(circuit, point)) {
      val part = CircuitPart
        .createPart(ICGateDefinition(meta).gateType)
        .asInstanceOf[GateICPart]
      part.preparePlacement(rotation, configuration, meta)
      circuit.setPart(point, part)
    }
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

    val t = orthoPartT(x, y, xSize, ySize, circuit.size, point.x, point.y)
    doRender(t, rotation, configuration)

    renderHolo(
      x,
      y,
      xSize,
      ySize,
      circuit.size,
      point,
      if (canPlace(circuit, point)) 0x33ffffff else 0x33ff0000
    )
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
    if (circuit.getPart(start) != null) return

    val t = orthoPartT(x, y, xSize, ySize, circuit.size, start.x, start.y)
    doRender(t, rotation, configuration)

    renderHolo(
      x,
      y,
      xSize,
      ySize,
      circuit.size,
      start,
      if (canPlace(circuit, start)) 0x44ffffff else 0x44ff0000
    )
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

class OpGate(meta: Int) extends OpGateCommons(meta) {
  override def findRot(circuit: IntegratedCircuit, start: Point, end: Point) = {
    (end - start).vectorize.axialProject.normalize match {
      case Vec2(0, -1) => 0
      case Vec2(1, 0)  => 1
      case Vec2(0, 1)  => 2
      case Vec2(-1, 0) => 3
      case _           => 0
    }
  }

  override def canPlace(circuit: IntegratedCircuit, point: Point) =
    !isOnBorder(circuit.size, point)
}

class OpIOGate(meta: Int) extends OpGateCommons(meta) {
  override def canPlace(circuit: IntegratedCircuit, point: Point) =
    isOnBorder(circuit.size, point) && !isOnEdge(circuit.size, point)

  override def findRot(circuit: IntegratedCircuit, start: Point, end: Point) = {
    val wm = circuit.size.width - 1
    val hm = circuit.size.height - 1
    start match {
      case Point(_, 0)    => 0
      case Point(`wm`, _) => 1
      case Point(_, `hm`) => 2
      case Point(0, _)    => 3
      case _              => 0
    }
  }
}
