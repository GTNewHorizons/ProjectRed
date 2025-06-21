package mrtjp.projectred.fabrication.circuitparts.io

import mrtjp.core.color.Colors

class BundledIOICGateLogic(gate: IOGateICPart)
    extends IOICGateLogic(gate)
    with TFreqIOICGateLogic {
  override def getConnMode(gate: IOGateICPart) = TIOCircuitPart.Bundled

  override def getFreqName = Colors(freq).name.toLowerCase

  override def toggleWorldInput() {
    gate.world.setInput(
      gate.rotation,
      (gate.world.iostate(gate.rotation) & 0xffff) ^ 1 << freq
    )
  }
}

class RenderBundledIO extends RenderIO {
  override def invColour = Colors(0).rgba
  override def dynColour(gate: IOGateICPart) = Colors(
    gate.getLogic[AnalogIOICGateLogic].freq
  ).rgba
}
