/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

trait TICBundledAcquisitions extends TICAcquisitions {
  def calcArray(r: Int): Array[Byte] =
    resolveArray(getStraight(r), rotFromStraight(r))

  def resolveArray(part: Any, r: Int): Array[Byte]
}
