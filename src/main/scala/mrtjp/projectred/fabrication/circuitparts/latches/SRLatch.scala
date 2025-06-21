package mrtjp.projectred.fabrication.circuitparts.latches

import mrtjp.projectred.core.TFaceOrient.{flipMaskZ, shiftMask}
import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ICGateRenderer,
  SequentialGateICPart,
  SequentialICGateLogic,
  TExtraStateLogic
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object SRLatch {
  def cycleShape(shape: Int): Int = {
    (shape + 1) % 4
  }
}

class SRLatch(gate: SequentialGateICPart)
    extends SequentialICGateLogic(gate)
    with TExtraStateLogic {
  override def outputMask(shape: Int) = if ((shape >> 1) == 0) 0xf else 5
  override def inputMask(shape: Int) = 0xa

  override def cycleShape(gate: SequentialGateICPart) = {
    gate.setShape(SRLatch.cycleShape(gate.shape))
    setState2(flipMaskZ(state2))
    gate.setState(flipMaskZ(gate.state))
    gate.onOutputChange(0xf)
    gate.scheduleTick(0)
    true
  }

  override def setup(gate: SequentialGateICPart) {
    setState2(2)
    gate.setState(0x30)
  }

  override def onChange(gate: SequentialGateICPart) {
    val stateInput = state2

    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, 0xa)
    val oldOutput = gate.state >> 4

    if (newInput != oldInput)
      if (
        stateInput != 0xa && newInput != 0 && newInput != stateInput
      ) // state needs changing
        {
          gate.setState(newInput)
          setState2(newInput)
          gate.onOutputChange(oldOutput) // always going low
          gate.scheduleTick(0)
        } else {
        gate.setState(oldOutput << 4 | newInput)
        gate.onInputChange()
      }
  }

  override def scheduledTick(gate: SequentialGateICPart) {
    val oldOutput = gate.state >> 4
    val newOutput = calcOutput(gate)

    if (oldOutput != newOutput) {
      gate.setState(gate.state & 0xf | newOutput << 4)
      gate.onOutputChange(outputMask(gate.shape))
    }
    onChange(gate)
  }

  def calcOutput(gate: SequentialGateICPart): Int = {
    var input = gate.state & 0xf
    var stateInput = state2

    if ((gate.shape & 1) != 0) // reverse
      {
        input = flipMaskZ(input)
        stateInput = flipMaskZ(stateInput)
      }

    if (stateInput == 0xa) // disabled
      {
        if (input == 0xa) {
          gate.scheduleTick(0)
          return 0
        }

        stateInput =
          if (input == 0)
            if (gate.world.network.getWorld.rand.nextBoolean()) 2 else 8
          else input

        setState2(
          if ((gate.shape & 1) != 0) flipMaskZ(stateInput) else stateInput
        )
      }

    var output = shiftMask(stateInput, 1)
    if ((gate.shape & 2) == 0) output |= stateInput
    if ((gate.shape & 1) != 0) output = flipMaskZ(output) // reverse
    output
  }
}

class RenderSRLatch extends ICGateRenderer[SequentialGateICPart] {
  val wires1 = generateWireModels("RSLATCH", 2)
  val wires2 = generateWireModels("RSLATCH2", 4)
  val torches1 =
    Seq(new RedstoneTorchModel(8, 3), new RedstoneTorchModel(8, 13))
  val torches2 =
    Seq(new RedstoneTorchModel(9.5, 3), new RedstoneTorchModel(6.5, 13))
  val base1 = new BaseComponentModel("RSLATCH")
  val base2 = new BaseComponentModel("RSLATCH2")

  val m1 = Seq(base1) ++ wires1 ++ torches1
  val m2 = Seq(base2) ++ wires2 ++ torches2

  var shape = 0

  override val coreModels = Seq()

  override def switchModels = if (shape == 0) m1 else m2

  override def prepareStatic(configuration: Int): Unit = {
    reflect = (configuration & 1) != 0
    shape = configuration >> 1
    var state = Seq(96, 48, 64, 16)(configuration)
    if (reflect) state = flipMaskZ(state >> 4) << 4 | flipMaskZ(state)
    if (shape == 0) {
      wires1(0).on = (state & 0x88) != 0
      wires1(1).on = (state & 0x22) != 0
      torches1(0).on = (state & 0x10) != 0
      torches1(1).on = (state & 0x40) != 0
    } else {
      wires2(1).on = (state & 2) != 0
      wires2(3).on = (state & 8) != 0
      torches2(0).on = (state & 0x10) != 0
      torches2(1).on = (state & 0x40) != 0
      wires2(0).on = torches2(1).on
      wires2(2).on = torches2(0).on
    }
  }

  override def prepareDynamic(gate: SequentialGateICPart, frame: Float) {
    reflect = (gate.shape & 1) != 0
    shape = gate.shape >> 1
    var state = gate.state
    if (reflect) state = flipMaskZ(state >> 4) << 4 | flipMaskZ(state)
    if (shape == 0) {
      wires1(0).on = (state & 0x88) != 0
      wires1(1).on = (state & 0x22) != 0
      torches1(0).on = (state & 0x10) != 0
      torches1(1).on = (state & 0x40) != 0
    } else {
      wires2(1).on = (state & 2) != 0
      wires2(3).on = (state & 8) != 0
      torches2(0).on = (state & 0x10) != 0
      torches2(1).on = (state & 0x40) != 0
      wires2(0).on = torches2(1).on
      wires2(2).on = torches2(0).on
    }
  }
}
