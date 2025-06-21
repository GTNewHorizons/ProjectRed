package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object XNOR extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = 10

  override def calcOutput(gate: ComboICGatePart, input: Int) = {
    val side1 = (input & 1 << 1) != 0
    val side2 = (input & 1 << 3) != 0
    if (side1 == side2) 1 else 0
  }
}

class RenderXNOR extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("XNOR", 5)
  val torches = Seq(
    new RedstoneTorchModel(8, 2),
    new RedstoneTorchModel(4.5, 8),
    new RedstoneTorchModel(11.5, 8),
    new RedstoneTorchModel(8, 12)
  )

  override val coreModels =
    Seq(new BaseComponentModel("XNOR")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(3).on = false
    wires(2).on = false
    wires(1).on = false
    torches(0).on = true
    torches(1).on = false
    torches(2).on = false
    torches(3).on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 2) != 0 && (gate.state & 8) == 0
    wires(1).on = (gate.state & 8) != 0 && (gate.state & 2) == 0
    wires(2).on = (gate.state & 8) != 0
    wires(3).on = (gate.state & 2) != 0
    wires(4).on = !wires(3).on && !wires(2).on
    torches(0).on = (gate.state & 0x11) != 0
    torches(1).on = !wires(4).on && (gate.state & 8) == 0
    torches(2).on = !wires(4).on && (gate.state & 2) == 0
    torches(3).on = (gate.state & 2) == 0 && (gate.state & 8) == 0
  }
}
