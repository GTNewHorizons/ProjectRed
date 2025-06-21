package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.vec.{Rotation, Vector3}

trait TICOrient extends CircuitPart {
  var orientation: Byte = 0

  def rotation = orientation & 0x3

  def setRotation(r: Int) {
    orientation = (orientation & 0xfc | r).toByte
  }

  def rotationT =
    Rotation.quarterRotations(rotation).at(new Vector3(0.5, 0, 0.5))

  // internal r from absRot
  def toInternal(absRot: Int) = (absRot + 4 - rotation) % 4

  // absRot from internal r
  def toAbsolute(r: Int) = (r + rotation) % 4

  def toInternalMask(mask: Int) = TICOrient.shiftMask(mask, toInternal(0))

  def toAbsoluteMask(mask: Int) = TICOrient.shiftMask(mask, toAbsolute(0))
}

object TICOrient {
  def shiftMask(mask: Int, r: Int) =
    (mask & ~0xf) | (mask << r | mask >> 4 - r) & 0xf

  def flipMaskZ(mask: Int) = mask & 5 | mask << 2 & 8 | mask >> 2 & 2
}
