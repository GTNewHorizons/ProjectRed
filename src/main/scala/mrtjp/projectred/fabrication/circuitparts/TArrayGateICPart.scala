package mrtjp.projectred.fabrication.circuitparts

import mrtjp.core.vec.Point
import mrtjp.projectred.fabrication.ICPropagator
import mrtjp.projectred.fabrication.circuitparts.wire.IRedwireICPart
import mrtjp.projectred.transmission.IWirePart

trait TArrayGateICPart
    extends RedstoneGateICPart
    with IRedwireICPart
    with TRSPropagatingICPart {
  def getLogicArray = getLogic[TArrayICGateLogic[TArrayGateICPart]]

  override def getSignal =
    getLogicArray.getSignal(toInternalMask(propagationMask))

  override def setSignal(signal: Int) =
    getLogicArray.setSignal(toInternalMask(propagationMask), signal)

  abstract override def updateAndPropagate(prev: CircuitPart, mode: Int) {
    val rd = sideDiff(prev)
    var uMask = 0
    for (r <- 0 until 4) if ((rd & 1 << r) != 0) {
      val pMask = getLogicArray.propogationMask(toInternal(r))
      if (pMask > 0 && (pMask & uMask) != pMask) {
        propagationMask = toAbsoluteMask(pMask)
        super.updateAndPropagate(prev, mode)
        uMask |= pMask
      }
    }
    if (uMask == 0) ICPropagator.addNeighborChange(Point(x, y))
    propagationMask = 0xf
  }

  override def propagateOther(mode: Int) {
    val nonConn = ~(connMap | connMap >> 4 | connMap >> 8) & 0xf
    notify(nonConn & propagationMask)
  }

  def sideDiff(part: CircuitPart): Int = {
    if (part.world == null) return 0xf
    val here = Point(x, y)
    val there = Point(part.x, part.y)
    there - here match {
      case Point(0, -1) => 1 << 0
      case Point(1, 0)  => 1 << 1
      case Point(0, 1)  => 1 << 2
      case Point(-1, 0) => 1 << 3
      case _ =>
        throw new RuntimeException(
          s"Circuit array gate tried to propagate from $here to #$there"
        )
    }
  }

  override def calculateSignal: Int = {
    val ipmask = toInternalMask(propagationMask)
    if (getLogicArray.overrideSignal(ipmask))
      return getLogicArray.calculateSignal(ipmask)

    var s = 0
    ICPropagator.redwiresProvidePower = false

    def raise(sig: Int) {
      if (sig > s) s = sig
    }

    for (r <- 0 until 4)
      if ((propagationMask & 1 << r) != 0) raise(calcSignal(r))
    ICPropagator.redwiresProvidePower = true
    s
  }

  abstract override def onChange() {
    super.onChange()
    ICPropagator.propagateTo(this, IWirePart.RISING)
  }

  override def onSignalUpdate() {
    world.network.markSave()
    super.onChange()
    getLogicArray.onSignalUpdate()
  }

  override def resolveSignal(part: Any, r: Int) = part match {
    case re: IRedwireICPart if re.diminishOnSide(r) =>
      re.getRedwireSignal(r) - 1
    case _ => super.resolveSignal(part, r)
  }

  override def getRedwireSignal(r: Int) = {
    val ir = toInternal(r)
    val pmask = getLogicArray.propogationMask(ir)
    if (pmask != 0) getLogicArray.getSignal(pmask)
    else getLogicRS.getOutput(this, ir)
  }

  override def canConnectRS(r: Int): Boolean = {
    if (super.canConnectRS(r)) return true
    getLogicArray.canConnectRedwire(this, toInternal(r))
  }

  override def rsOutputLevel(r: Int): Int = {
    val ir = toInternal(r)
    if ((getLogicArray.redwireMask(shape) & 1 << ir) != 0)
      return if (ICPropagator.redwiresProvidePower)
        getLogicArray.getSignal(getLogicArray.propogationMask(ir))
      else 0
    super.rsOutputLevel(r)
  }

  override def diminishOnSide(r: Int) =
    (getLogicArray.redwireMask(shape) & 1 << toInternal(r)) != 0
}
