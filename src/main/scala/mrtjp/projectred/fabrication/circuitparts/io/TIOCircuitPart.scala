package mrtjp.projectred.fabrication.circuitparts.io

object TIOCircuitPart {
  val Closed = 0
  val Input = 1
  val Output = 2
  val InOut = 3

  val NoConn = 0
  val Simple = 1
  val Analog = 2
  val Bundled = 3
}

trait TIOCircuitPart {
  def onExtInputChanged(r: Int)

  def onExtOutputChanged(r: Int)

  def getIOSide: Int

  def getIOMode: Int

  def getConnMode: Int
}
