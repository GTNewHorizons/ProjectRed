package mrtjp.projectred.fabrication.circuitparts

import mrtjp.core.vec.Point
import IWireICPart.FORCED
import mrtjp.projectred.fabrication.ICPropagator

trait TPropagatingICPart
    extends CircuitPart
    with TConnectableICPart
    with IWireICPart {
  var propagationMask = 0xf

  def propagate(prev: CircuitPart, mode: Int) {
    if (mode != FORCED) ICPropagator.addPartChange(this)
    for (r <- 0 until 4)
      if ((propagationMask & 1 << r) != 0)
        if (maskConnects(r))
          propagateExternal(getStraight(r), posOfStraight(r), prev, mode)

    propagateOther(mode)
  }

  def propagateOther(mode: Int) {}

  def propagateExternal(
      to: CircuitPart,
      at: Point,
      from: CircuitPart,
      mode: Int
  ) {
    if (to != null) {
      if (to == from) return
      if (propagateTo(to, mode)) return
    }
    ICPropagator.addNeighborChange(at)
  }

  def propagateTo(part: CircuitPart, mode: Int) = part match {
    case w: IWireICPart =>
      ICPropagator.propagateTo(w, this, mode)
      true
    case _ => false
  }
}
