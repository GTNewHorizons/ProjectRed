package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object Multiplexer extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = 0xe

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if ((input & 1 << 2) != 0) (input >> 3) & 1 else (input >> 1) & 1
}

object Pulse extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = 4

  override def calcOutput(gate: ComboICGatePart, input: Int) = 0

  override def onChange(gate: ComboICGatePart) = {
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, 4)

    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
      if (newInput != 0 && (gate.state & 0xf0) == 0) {
        gate.setState(gate.state & 0xf | 0x10)
        gate.scheduleTick(2)
        gate.onOutputChange(1)
      }
    }
  }
}

class RenderMultiplexer extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("MULTIPLEXER", 6)
  val torches = Seq(
    new RedstoneTorchModel(8, 2),
    new RedstoneTorchModel(9, 10.5),
    new RedstoneTorchModel(4.5, 8),
    new RedstoneTorchModel(11.5, 8)
  )

  override val coreModels =
    Seq(new BaseComponentModel("MULTIPLEXER")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(1).on = true
    wires(2).on = true
    wires(3).on = false
    wires(4).on = false
    wires(5).on = false
    torches(0).on = false
    torches(1).on = true
    torches(2).on = false
    torches(3).on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(2).on = (gate.state & 4) == 0
    wires(3).on = (gate.state & 4) != 0
    wires(4).on = (gate.state & 8) != 0
    wires(5).on = (gate.state & 2) != 0
    torches(0).on = (gate.state & 0x10) != 0
    torches(1).on = !wires(3).on
    torches(2).on = (gate.state & 8) == 0 && wires(3).on
    torches(3).on = (gate.state & 4) == 0 && !wires(5).on
    wires(0).on = torches(2).on
    wires(1).on = torches(1).on
  }
}
