package mrtjp.projectred.fabrication.circuitparts

import mrtjp.core.vec.Point

trait TICAcquisitions extends CircuitPart {
  def getStraight(r: Int) = world.getPart(posOfStraight(r))

  def posOfStraight(r: Int) = Point(x, y).offset(r)

  def rotFromStraight(r: Int) = (r + 2) % 4

  def notifyToDir(r: Int) {
    world.notifyNeighbor(posOfStraight(r))
  }

  def notify(mask: Int) {
    world.notifyNeighbors(x, y, mask)
  }
}
