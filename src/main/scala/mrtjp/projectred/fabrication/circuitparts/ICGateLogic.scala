/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.gui.{CircuitGui, ICGateGui}

abstract class ICGateLogic[T <: GateICPart] {
  def canConnectTo(gate: T, part: CircuitPart, r: Int): Boolean

  def cycleShape(gate: T) = false

  def onChange(gate: T)

  def scheduledTick(gate: T)

  def onTick(gate: T) {}

  def setup(gate: T) {}

  def activate(gate: T) {}

  @SideOnly(Side.CLIENT)
  def getRolloverData(gate: T, detailLevel: Int): Seq[String] = Seq.empty

  @SideOnly(Side.CLIENT)
  def createGui(gate: T): CircuitGui = new ICGateGui(gate)
}
