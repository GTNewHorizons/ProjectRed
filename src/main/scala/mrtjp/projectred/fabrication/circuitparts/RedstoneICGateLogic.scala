/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.circuitparts.wire.IICRedwireEmitter


abstract class RedstoneICGateLogic[T <: RedstoneGateICPart]
    extends ICGateLogic[T] {
  override def canConnectTo(gate: T, part: CircuitPart, r: Int) = part match {
    case re: IICRedwireEmitter => canConnect(gate, r)
    case _                     => false
  }

  def canConnect(gate: T, r: Int): Boolean = canConnect(gate.shape, r)
  def canConnect(shape: Int, r: Int): Boolean =
    ((inputMask(shape) | outputMask(shape)) & 1 << r) != 0

  def outputMask(shape: Int) = 0
  def inputMask(shape: Int) = 0

  def getOutput(gate: T, r: Int) = if ((gate.state & 0x10 << r) != 0) 255 else 0
  def getInput(gate: T, mask: Int) = {
    var input = 0
    for (r <- 0 until 4)
      if ((mask & 1 << r) != 0 && gate.getRedstoneInput(r) > 0) input |= 1 << r
    input
  }
}
