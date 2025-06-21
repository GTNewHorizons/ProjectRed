package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object OR extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = ~shape << 1 & 0xe

  override def deadSides = 3

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input != 0) 1 else 0
}

class RenderOR extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("OR", 4)
  val torches =
    Seq(new RedstoneTorchModel(8, 9), new RedstoneTorchModel(8, 2.5))

  override val coreModels =
    Seq(new BaseComponentModel("OR")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(1).disabled = (configuration & 1) != 0
    wires(2).disabled = (configuration & 2) != 0
    wires(3).disabled = (configuration & 4) != 0
    torches(0).on = true
    torches(1).on = false
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x10) == 0
    wires(1).on = (gate.state & 2) != 0
    wires(2).on = (gate.state & 4) != 0
    wires(3).on = (gate.state & 8) != 0
    wires(1).disabled = (gate.shape & 1) != 0
    wires(2).disabled = (gate.shape & 2) != 0
    wires(3).disabled = (gate.shape & 4) != 0
    torches(0).on = (gate.state & 0xe) == 0
    torches(1).on = !wires(0).on
  }
}
