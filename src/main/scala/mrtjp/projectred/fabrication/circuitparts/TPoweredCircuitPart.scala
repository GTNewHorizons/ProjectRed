package mrtjp.projectred.fabrication.circuitparts

trait TPoweredCircuitPart {
  def rsOutputLevel(r: Int): Int

  def canConnectRS(r: Int): Boolean
}
