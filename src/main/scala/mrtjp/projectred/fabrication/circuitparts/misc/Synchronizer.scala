package mrtjp.projectred.fabrication.circuitparts.misc

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ICGateRenderer,
  SequentialGateICPart,
  SequentialICGateLogic,
  TExtraStateLogic
}
import mrtjp.projectred.fabrication.{
  BaseComponentModel,
  RedChipModel,
  RedstoneTorchModel
}

class Synchronizer(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with TExtraStateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = 14

  override def onChange(gate: SequentialGateICPart) {
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, 14)
    val high = newInput & ~oldInput
    if (oldInput != newInput) {
      val oldValue = state2

      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
      if ((newInput & 4) != 0) setState2(0)
      else {
        if ((high & 2) != 0) setState2(state2 | 1) // right
        if ((high & 8) != 0) setState2(state2 | 2) // left
      }
      if (right && left) gate.scheduleTick(0)

      if (state2 != oldValue) sendState2Update()
    }
  }

  override def scheduledTick(gate: SequentialGateICPart) {
    val oldValue = state2
    if (!pulsing && right && left) {
      gate.setState(gate.state | 1 << 4)
      gate.onOutputChange(1)
      setState2(state2 | 4) // pulsing
      gate.scheduleTick(2)
    } else if (pulsing) {
      gate.setState(gate.state & ~0x10)
      gate.onOutputChange(1)
      setState2(0) // off
    }
    if (state2 != oldValue) sendState2Update()
  }

  def right = (state2 & 1) != 0
  def left = (state2 & 2) != 0
  def pulsing = (state2 & 4) != 0

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: SequentialGateICPart, detailLevel: Int) = {
    val data = Seq.newBuilder[String]
    if (detailLevel > 1) {
      data += "0: " + (if (right) "high" else "low")
      data += "1: " + (if (left) "high" else "low")
    }
    super.getRolloverData(gate, detailLevel) ++ data.result()
  }
}

class RenderSynchronizer extends ICGateRenderer[SequentialGateICPart] {
  val wires = generateWireModels("SYNC", 6)
  val torch = new RedstoneTorchModel(8, 3)
  val chips = Seq(new RedChipModel(4.5, 9), new RedChipModel(11.5, 9))

  override val coreModels =
    Seq(new BaseComponentModel("SYNC")) ++ wires ++ chips ++ Seq(torch)

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = true
    wires(2).on = false
    wires(3).on = false
    wires(4).on = false
    wires(5).on = false
    chips(0).on = false
    chips(1).on = false
    torch.on = false
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    val logic = gate.getLogic[Synchronizer]
    wires(0).on = !logic.left
    wires(1).on = !logic.right
    wires(2).on = (gate.state & 4) != 0
    wires(3).on = logic.left && logic.right
    wires(4).on = (gate.state & 8) != 0
    wires(5).on = (gate.state & 2) != 0
    chips(0).on = logic.left
    chips(1).on = logic.right
    torch.on = (gate.state & 0x10) != 0
  }
}
