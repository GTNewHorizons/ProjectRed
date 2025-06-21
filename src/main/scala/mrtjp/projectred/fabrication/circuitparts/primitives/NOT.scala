package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object NOT extends ComboICGateLogic {
  override def outputMask(shape: Int) =
    ~((shape & 1) << 1 | (shape & 2) >> 1 | (shape & 4) << 1) & 0xb
  override def inputMask(shape: Int) = 4
  override def feedbackMask(shape: Int) = outputMask(shape)

  override def deadSides = 3

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input == 0) 0xb else 0
}

class RenderNOT extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("NOT", 4)
  val torch = new RedstoneTorchModel(8, 8)

  override val coreModels = Seq(new BaseComponentModel("NOT")) ++ wires :+ torch

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = true
    wires(2).on = false
    wires(3).on = true
    wires(0).disabled = (configuration & 2) != 0
    wires(1).disabled = (configuration & 1) != 0
    wires(3).disabled = (configuration & 4) != 0
    torch.on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x11) != 0
    wires(1).on = (gate.state & 0x22) != 0
    wires(2).on = (gate.state & 4) != 0
    wires(3).on = (gate.state & 0x88) != 0
    wires(0).disabled = (gate.shape & 2) != 0
    wires(1).disabled = (gate.shape & 1) != 0
    wires(3).disabled = (gate.shape & 4) != 0
    torch.on = (gate.state & 0xf0) != 0
  }
}
