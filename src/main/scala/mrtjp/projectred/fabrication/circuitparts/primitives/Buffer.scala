package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object Buffer extends ComboICGateLogic {
  override def outputMask(shape: Int) =
    ~((shape & 1) << 1 | (shape & 2) << 2) & 0xb
  override def inputMask(shape: Int) = 4
  override def feedbackMask(shape: Int) = outputMask(shape)

  override def deadSides = 2
  override def maxDeadSides = 2

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input != 0) 0xb else 0
}

class RenderBuffer extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("BUFFER", 4)
  val torches =
    Seq(new RedstoneTorchModel(8, 3.5), new RedstoneTorchModel(8, 9))

  override val coreModels =
    Seq(new BaseComponentModel("BUFFER")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(1).disabled = (configuration & 1) != 0
    wires(3).disabled = (configuration & 2) != 0
    torches(0).on = false
    torches(1).on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 4) == 0
    wires(1).on = (gate.state & 0x22) != 0
    wires(2).on = (gate.state & 0x44) != 0
    wires(3).on = (gate.state & 0x88) != 0
    wires(1).disabled = (gate.shape & 1) != 0
    wires(3).disabled = (gate.shape & 2) != 0
    torches(0).on = (gate.state & 4) != 0
    torches(1).on = (gate.state & 4) == 0
  }
}
