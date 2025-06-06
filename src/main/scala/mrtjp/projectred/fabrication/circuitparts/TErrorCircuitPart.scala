/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

trait TErrorCircuitPart extends CircuitPart {
  def postErrors: (String, Int) // (message, colour)
}
