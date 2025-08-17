/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

trait TICRSAcquisitions extends TICAcquisitions with TPoweredCircuitPart {
  def calcSignal(r: Int): Int =
    resolveSignal(getStraight(r), rotFromStraight(r))

  def resolveSignal(part: Any, r: Int): Int
}
