package mrtjp.projectred.fabrication.circuitparts.io

import mrtjp.projectred.fabrication.ICComponentStore.signalColour

class SimpleIOICGateLogic(gate: IOGateICPart)
    extends IOICGateLogic(gate)
    with TRSIOICGateLogic {
  override def getConnMode(gate: IOGateICPart) = TIOCircuitPart.Simple

  override def resolveInputFromWorld =
    if ((gate.world.iostate(gate.rotation) & 0xfffe) != 0) 255
    else 0

  override def resolveOutputToWorld =
    if (((gate.world.iostate(gate.rotation) >> 16) & 0xfffe) != 0) 255 else 0

  override def setWorldOutput(state: Boolean) {
    gate.world.setOutput(gate.rotation, if (state) 0x8000 else 1)
  }

  override def toggleWorldInput() {
    gate.world.setInput(
      gate.rotation,
      if ((gate.world.iostate(gate.rotation) & 0x8000) != 0) 1 else 0x8000
    )
  }
}

class RenderSimpleIO extends RenderIO {
  override def invColour = signalColour(0.toByte)
  override def dynColour(gate: IOGateICPart) = signalColour(
    (if (iosig.on) 255 else 0).toByte
  )
}
