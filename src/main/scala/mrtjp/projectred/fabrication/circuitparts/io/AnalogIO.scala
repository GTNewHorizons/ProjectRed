package mrtjp.projectred.fabrication.circuitparts.io

import mrtjp.projectred.fabrication.ICComponentStore.signalColour

class AnalogIOICGateLogic(gate: IOGateICPart)
    extends IOICGateLogic(gate)
    with TFreqIOICGateLogic
    with TRSIOICGateLogic {
  override def getConnMode(gate: IOGateICPart) = TIOCircuitPart.Analog

  override def getFreqName = "0x" + Integer.toHexString(freq)

  override def toggleWorldInput() {
    val newInput = (gate.world.iostate(gate.rotation) & 1 << freq) ^ 1 << freq
    gate.world.setInput(gate.rotation, if (newInput == 0) 1 else newInput)
  }
}

class RenderAnalogIO extends RenderIO {
  override def invColour = signalColour(0.toByte)
  override def dynColour(gate: IOGateICPart) = signalColour(
    (gate.getLogic[AnalogIOICGateLogic].freq * 17).toByte
  )
}
