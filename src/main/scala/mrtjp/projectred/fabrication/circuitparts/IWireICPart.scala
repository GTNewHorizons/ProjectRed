package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.IntegratedCircuit

object IWireICPart {

  /** Standard operation procedure, no special propogation rules. The propogator
    * signal may not have increased.
    */
  final val RISING = 0

  /** Used when the propogator signal dropped (to 0). Propagation should
    * continue until a rising or constant change is encountered at which point a
    * RISING should be propogated back to this wire.
    */
  final val DROPPING = 1

  /** Used when a wire's connection state has changed. Even if the signal
    * remains the same, new connections still need to be recalculated
    */
  final val FORCE = 2

  /** Used when the propogator did not change signal, but a new connection may
    * have been established and signal needs recalculating
    */
  final val FORCED = 3
}

trait IWireICPart {

  /** Recalculates the signal of this wire and calls the appropriate propogation
    * methods in WirePropagator. DO NOT CALL THIS YOURSELF. Use
    * WirePropagator.propagateTo
    *
    * @param prev
    *   The part which called this propogation (should be connected) may be
    *   null.
    * @param mode
    *   One of RISING, DROPPING, FORCE and FORCED specified above
    */
  def updateAndPropagate(prev: CircuitPart, mode: Int)

  /** Called at the end of a propogation run for partChanged events. Marks the
    * end of a state change for this part.
    */
  def onSignalUpdate()

  /** @param r
    *   The rotation of this part to test for wire connection.
    * @return
    *   true if the specified side of this block is connected to, for example, a
    *   'wire' where signal should decrease by one.
    */
  def diminishOnSide(r: Int): Boolean

  /** The world in which this part resides
    *
    * @return
    */
  def world: IntegratedCircuit
}
