package mrtjp.projectred.fabrication.circuitparts

trait TErrorCircuitPart extends CircuitPart {
  def postErrors: (String, Int) // (message, colour)
}
