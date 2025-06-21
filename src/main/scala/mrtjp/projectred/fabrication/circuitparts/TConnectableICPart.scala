package mrtjp.projectred.fabrication.circuitparts

trait TConnectableICPart extends CircuitPart with TICAcquisitions {
  var connMap: Byte = 0

  def maskConnects(r: Int) = (connMap & 1 << r) != 0

  def discover(r: Int) = getStraight(r) match {
    case c: TConnectableICPart =>
      canConnectPart(c, r) && c.connect(this, rotFromStraight(r))
    case c => discoverOverride(r, c)
  }

  def discoverOverride(r: Int, part: CircuitPart) = false

  def connect(part: CircuitPart, r: Int) = {
    if (canConnectPart(part, r)) {
      val oldConn = connMap
      connMap = (connMap | 1 << r).toByte
      if (oldConn != connMap) onMaskChanged()
      true
    } else false
  }

  def updateConns() = {
    var newConn = 0
    for (r <- 0 until 4) if (discover(r)) newConn |= 1 << r
    if (newConn != connMap) {
      connMap = newConn.toByte
      onMaskChanged()
      true
    } else false
  }

  def canConnectPart(part: CircuitPart, r: Int): Boolean

  def onMaskChanged() {}
}
