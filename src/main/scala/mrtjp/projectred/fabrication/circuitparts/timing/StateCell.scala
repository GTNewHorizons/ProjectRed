package mrtjp.projectred.fabrication.circuitparts.timing

import codechicken.lib.math.MathHelper
import mrtjp.projectred.core.TFaceOrient.flipMaskZ
import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ICGateRenderer,
  SequentialGateICPart,
  SequentialICGateLogic,
  TExtraStateLogic
}
import mrtjp.projectred.fabrication.{
  BaseComponentModel,
  PointerModel,
  RedChipModel,
  RedstoneTorchModel
}

object StateCell {
  def cycleShape(shape: Int): Int = {
    (shape + 1) % 2
  }
}

class StateCell(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with TTimerGateLogic
    with TExtraStateLogic {
  override def outputMask(shape: Int) = {
    var output = 9
    if (shape == 1) output = flipMaskZ(output)
    output
  }

  override def inputMask(shape: Int) = {
    var input = 6
    if (shape == 1) input = flipMaskZ(input)
    input
  }

  override def cycleShape(gate: SequentialGateICPart) = {
    gate.setShape(StateCell.cycleShape(gate.shape))
    true
  }

  override def onChange(gate: SequentialGateICPart) {
    val oldInput = gate.state & 0xf
    var newInput = getInput(gate, 0xe)
    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()

      if (gate.shape == 1) newInput = flipMaskZ(newInput)
      if ((newInput & 4) != 0 && state2 == 0) {
        setState2(1)
        sendState2Update()
        gate.scheduleTick(0)
      }

      if (state2 != 0)
        if ((newInput & 6) != 0) resetPointer()
        else startPointer()
    }
  }

  override def pointerTick() {
    resetPointer()
    if (!gate.world.network.isRemote) {
      setState2(0)
      sendState2Update()
      gate.setState(0x10 | gate.state & 0xf)
      gate.onOutputChange(outputMask(gate.shape))
      gate.scheduleTick(2)
    }
  }

  override def scheduledTick(gate: SequentialGateICPart) {
    var output = 0
    if (state2 != 0) output = 8
    if (gate.shape == 1) output = flipMaskZ(output)

    gate.setState(output << 4 | gate.state & 0xf)
    gate.onOutputChange(outputMask(gate.shape))
  }
}

class RenderStateCell extends ICGateRenderer[SequentialGateICPart] {
  val wires = generateWireModels("STATECELL", 5)
  val torches =
    Seq(new RedstoneTorchModel(10, 3.5), new RedstoneTorchModel(13, 8))
  val chip = new RedChipModel(6.5, 10)
  val pointer = new PointerModel(13, 8)

  override val coreModels = Seq(
    new BaseComponentModel("STATECELL")
  ) ++ wires ++ Seq(chip, pointer) ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    reflect = false
    wires(0).on = false
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(4).on = false
    torches(0).on = false
    torches(1).on = true
    chip.on = false
    pointer.angle = -MathHelper.pi / 2
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    reflect = gate.shape == 1
    val logic = gate.getLogic[StateCell]
    var state = gate.state
    if (reflect) state = flipMaskZ(state >> 4) << 4 | flipMaskZ(state)

    wires(0).on = (state & 0x10) != 0
    wires(1).on = (state & 4) != 0
    wires(2).on = logic.state2 == 0 || (state & 4) != 0
    wires(3).on = (state & 0x88) != 0
    wires(4).on = (state & 2) != 0
    torches(0).on = (state & 0x10) != 0
    torches(1).on = logic.pointer_start >= 0
    chip.on = logic.state2 != 0

    reflect = gate.shape == 1
    pointer.angle =
      gate.getLogic[StateCell].interpPointer(frame) - MathHelper.pi / 2
  }
}
