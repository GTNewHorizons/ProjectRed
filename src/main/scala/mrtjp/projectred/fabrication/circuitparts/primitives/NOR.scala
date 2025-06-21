package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object NOR extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = ~shape << 1 & 0xe
  override def feedbackMask(shape: Int) = 1

  override def deadSides = 3

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input == 0) 1 else 0
}

class RenderNOR extends ICGateRenderer[ComboICGatePart] {
  var wires = generateWireModels("NOR", 4)
  var torch = new RedstoneTorchModel(8, 9)

  override val coreModels = Seq(new BaseComponentModel("NOR")) ++ wires :+ torch

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(1).disabled = (configuration & 1) != 0
    wires(2).disabled = (configuration & 2) != 0
    wires(3).disabled = (configuration & 4) != 0
    torch.on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x11) != 0
    wires(1).on = (gate.state & 2) != 0
    wires(2).on = (gate.state & 4) != 0
    wires(3).on = (gate.state & 8) != 0
    wires(1).disabled = (gate.shape & 1) != 0
    wires(2).disabled = (gate.shape & 2) != 0
    wires(3).disabled = (gate.shape & 4) != 0
    torch.on = (gate.state & 0xe) == 0
  }
}
