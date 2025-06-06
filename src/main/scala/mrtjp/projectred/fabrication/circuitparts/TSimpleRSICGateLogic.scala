/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

import cpw.mods.fml.relauncher.{Side, SideOnly}


trait TSimpleRSICGateLogic[T <: RedstoneGateICPart]
    extends RedstoneICGateLogic[T] {
  def getDelay(shape: Int) = 0

  def feedbackMask(shape: Int) = 0

  def calcOutput(gate: T, input: Int) = 0

  override def onChange(gate: T) {
    val iMask = inputMask(gate.shape)
    val oMask = outputMask(gate.shape)
    val fMask = feedbackMask(gate.shape)
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, iMask | fMask)
    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
    }

    val newOutput = calcOutput(gate, gate.state & iMask) & oMask
    if (newOutput != (gate.state >> 4)) gate.scheduleTick(getDelay(gate.shape))
  }

  override def scheduledTick(gate: T) {
    val iMask = inputMask(gate.shape)
    val oMask = outputMask(gate.shape)
    val oldOutput = gate.state >> 4
    val newOutput = calcOutput(gate, gate.state & iMask) & oMask
    if (oldOutput != newOutput) {
      gate.setState(gate.state & 0xf | newOutput << 4)
      gate.onOutputChange(oMask)
    }
    onChange(gate)
  }

  override def setup(gate: T) {
    val iMask = inputMask(gate.shape)
    val oMask = outputMask(gate.shape)
    val output = calcOutput(gate, getInput(gate, iMask)) & oMask
    if (output != 0) {
      gate.setState(output << 4)
      gate.onOutputChange(
        output
      ) // use output for change mask because nothing is going low
    }
  }

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: T, detailLevel: Int) = {
    val s = Seq.newBuilder[String]
    if (detailLevel > 2)
      s += "I: " + rolloverInput(gate) += "O: " + rolloverOutput(gate)
    super.getRolloverData(gate, detailLevel) ++ s.result()
  }
  def rolloverInput(gate: T) = "0x" + Integer.toHexString(gate.state & 0xf)
  def rolloverOutput(gate: T) = "0x" + Integer.toHexString(gate.state >> 4)
}
