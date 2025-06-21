package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object AND extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = ~shape << 1 & 0xe

  override def deadSides = 3

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input == inputMask(gate.shape)) 1 else 0
}

class RenderAND extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("AND", 4)
  val torches = Seq(
    new RedstoneTorchModel(4, 8),
    new RedstoneTorchModel(12, 8),
    new RedstoneTorchModel(8, 8),
    new RedstoneTorchModel(8, 2)
  )

  override val coreModels =
    Seq(new BaseComponentModel("AND")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(3).disabled = (configuration & 1) != 0
    wires(1).disabled = (configuration & 2) != 0
    wires(2).disabled = (configuration & 4) != 0
    torches(2).on = !wires(1).on && !wires(1).disabled
    torches(0).on = !wires(2).on && !wires(2).disabled
    torches(1).on = !wires(3).on && !wires(3).disabled
    torches(3).on = false
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x11) == 0
    wires(3).on = (gate.state & 2) != 0
    wires(1).on = (gate.state & 4) != 0
    wires(2).on = (gate.state & 8) != 0
    wires(3).disabled = (gate.shape & 1) != 0
    wires(1).disabled = (gate.shape & 2) != 0
    wires(2).disabled = (gate.shape & 4) != 0
    torches(2).on = !wires(1).on && !wires(1).disabled
    torches(0).on = !wires(2).on && !wires(2).disabled
    torches(1).on = !wires(3).on && !wires(3).disabled
    torches(3).on = !wires(0).on
  }
}
