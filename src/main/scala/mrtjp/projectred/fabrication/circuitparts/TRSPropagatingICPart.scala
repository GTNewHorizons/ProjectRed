package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.ICPropagator
import IWireICPart._

trait TRSPropagatingICPart extends TPropagatingICPart {
  def calculateSignal: Int

  def getSignal: Int

  def setSignal(signal: Int)

  override def updateAndPropagate(prev: CircuitPart, mode: Int) {
    if (mode == DROPPING && getSignal == 0) return
    val newSignal = calculateSignal
    if (newSignal < getSignal) {
      if (newSignal > 0) ICPropagator.propagateAnalogDrop(this)
      setSignal(0)
      propagate(prev, DROPPING)
    } else if (newSignal > getSignal) {
      setSignal(newSignal)
      if (mode == DROPPING) propagate(null, RISING)
      else propagate(prev, RISING)
    } else if (mode == DROPPING) propagateTo(prev, RISING)
    else if (mode == FORCE) propagate(prev, FORCED)
  }
}
