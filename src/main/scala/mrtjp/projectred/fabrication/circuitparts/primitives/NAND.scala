package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object NAND extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = ~shape << 1 & 0xe

  override def deadSides = 3

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input == inputMask(gate.shape)) 0 else 1
}

class RenderNAND extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("NAND", 4)
  val torches = Seq(
    new RedstoneTorchModel(4, 8),
    new RedstoneTorchModel(12, 8),
    new RedstoneTorchModel(8, 8)
  )

  override val coreModels =
    Seq(new BaseComponentModel("NAND")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(3).disabled = (configuration & 1) != 0
    wires(1).disabled = (configuration & 2) != 0
    wires(2).disabled = (configuration & 4) != 0
    torches(0).on = !wires(2).on && !wires(2).disabled
    torches(1).on = !wires(3).on && !wires(3).disabled
    torches(2).on = !wires(1).on && !wires(1).disabled
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x11) != 0
    wires(3).on = (gate.state & 2) != 0
    wires(1).on = (gate.state & 4) != 0
    wires(2).on = (gate.state & 8) != 0
    wires(3).disabled = (gate.shape & 1) != 0
    wires(1).disabled = (gate.shape & 2) != 0
    wires(2).disabled = (gate.shape & 4) != 0
    torches(0).on = !wires(2).on && !wires(2).disabled
    torches(1).on = !wires(3).on && !wires(3).disabled
    torches(2).on = !wires(1).on && !wires(1).disabled
  }
}
