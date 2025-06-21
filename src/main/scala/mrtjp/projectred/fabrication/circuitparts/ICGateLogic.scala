package mrtjp.projectred.fabrication.circuitparts

import cpw.mods.fml.relauncher.{Side, SideOnly}

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
}
