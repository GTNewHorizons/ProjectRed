package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.circuitparts.wire.IRedwireICPart

trait TArrayICGateLogic[T <: TArrayGateICPart] extends RedstoneICGateLogic[T] {
  abstract override def canConnectTo(gate: T, part: CircuitPart, r: Int) =
    part match {
      case re: IRedwireICPart if canConnectRedwire(gate, r) => true
      case _ => super.canConnectTo(gate, part, r)
    }

  def canConnectRedwire(gate: T, r: Int): Boolean =
    canConnectRedwire(gate.shape, r)

  def canConnectRedwire(shape: Int, r: Int): Boolean =
    (redwireMask(shape) & 1 << r) != 0

  def redwireMask(shape: Int): Int

  def propogationMask(r: Int): Int

  def getSignal(mask: Int): Int

  def setSignal(mask: Int, signal: Int)

  def overrideSignal(mask: Int) = false

  def calculateSignal(mask: Int) = 0

  def canCross = false

  def onSignalUpdate()
}
