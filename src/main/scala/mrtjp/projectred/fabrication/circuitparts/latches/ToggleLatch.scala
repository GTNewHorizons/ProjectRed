package mrtjp.projectred.fabrication.circuitparts.latches

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ICGateRenderer,
  SequentialGateICPart,
  SequentialICGateLogic,
  TExtraStateLogic
}
import mrtjp.projectred.fabrication.{
  BaseComponentModel,
  LeverModel,
  RedstoneTorchModel
}

class ToggleLatch(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with TExtraStateLogic {
  override def outputMask(shape: Int) = 5
  override def inputMask(shape: Int) = 0xa

  override def clientState2 = true

  override def setup(gate: SequentialGateICPart) {
    gate.setState(0x10)
    gate.sendStateUpdate()
  }

  override def onChange(gate: SequentialGateICPart) {
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, 0xa)
    val high = newInput & ~oldInput

    if (high == 2 || high == 8) toggle(gate)

    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
    }
  }

  override def scheduledTick(gate: SequentialGateICPart) {
    val oldOutput = gate.state >> 4
    val newOutput = if (state2 == 0) 1 else 4
    if (oldOutput != newOutput) {
      gate.setState(newOutput << 4 | gate.state & 0xf)
      gate.onOutputChange(5)
    }
    onChange(gate)
  }

  override def activate(gate: SequentialGateICPart) {
    toggle(gate)
  }

  def toggle(gate: SequentialGateICPart) {
    setState2(state2 ^ 1)
    gate.scheduleTick(0)
  }
}

class RenderToggleLatch extends ICGateRenderer[SequentialGateICPart] {
  val wires = generateWireModels("TOGLATCH", 2)
  val torches = Seq(new RedstoneTorchModel(4, 4), new RedstoneTorchModel(4, 12))
  val lever = new LeverModel(11, 8)

  override val coreModels =
    Seq(new BaseComponentModel("TOGLATCH")) ++ wires ++ torches :+ lever

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(1).on = false
    torches(0).on = true
    torches(1).on = false
    lever.on = true
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    wires(0).on = (gate.state & 8) != 0
    wires(1).on = (gate.state & 2) != 0
    torches(0).on = (gate.state & 0x10) != 0
    torches(1).on = (gate.state & 0x40) != 0
    lever.on = (gate.state & 0x10) != 0
  }
}
