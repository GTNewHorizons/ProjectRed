package mrtjp.projectred.fabrication.circuitparts.misc

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{
  BaseComponentModel,
  RedChipModel,
  RedstoneTorchModel,
  YellowChipModel
}

import java.util.Random

object DecRandomizer extends ComboICGateLogic {
  val rand = new Random

  override def cycleShape(shape: Int) = shape ^ 1

  override def outputMask(shape: Int) = if (shape == 0) 11 else 9
  override def inputMask(shape: Int) = 4
  override def feedbackMask(shape: Int) = 2

  override def getDelay(shape: Int) = 2

  override def calcOutput(gate: ComboICGatePart, input: Int) = {
    if (input == 0) if ((gate.state >> 4) == 0) 1 else gate.state >> 4
    else Seq(1, 8, 2)(rand.nextInt((~gate.shape | 2) & 3))
  }

  override def onChange(gate: ComboICGatePart) {
    super.onChange(gate)
    if ((gate.state & 4) != 0) gate.scheduleTick(2)
  }
}

class RenderDecRandomizer extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("DECRAND", 6)
  val chips = Seq(
    new YellowChipModel(5, 13),
    new YellowChipModel(11, 13),
    new RedChipModel(5.5, 8)
  )
  val torches = Seq(
    new RedstoneTorchModel(8, 2.5),
    new RedstoneTorchModel(14, 8),
    new RedstoneTorchModel(2, 8),
    new RedstoneTorchModel(9, 8)
  )

  override val coreModels =
    Seq(new BaseComponentModel("DECRAND")) ++ wires ++ chips ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(4).on = true
    wires(5).on = true
    wires(0).disabled = configuration != 0
    wires(3).disabled = configuration != 0
    torches(0).on = true
    torches(1).on = false
    torches(2).on = false
    torches(3).on = false
    chips(0).on = false
    chips(1).on = true
    chips(2).on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    val state = gate.state
    wires(0).on = (state >> 4) == 2
    wires(1).on = (state >> 4) == 8
    wires(2).on = (state & 4) != 0
    wires(3).on = (state & 4) != 0
    wires(4).on = (state >> 4) == 1 || (state >> 4) == 2
    wires(5).on = (state >> 4) == 1
    wires(0).disabled = gate.shape != 0
    wires(3).disabled = gate.shape != 0
    torches(0).on = (state >> 4) == 1
    torches(1).on = (state >> 4) == 2
    torches(2).on = (state >> 4) == 8
    torches(3).on = !wires(4).on
    chips(0).on = (state >> 4) == 2
    chips(1).on = (state >> 4) == 1 || (state >> 4) == 2
    chips(2).on = true
  }
}
