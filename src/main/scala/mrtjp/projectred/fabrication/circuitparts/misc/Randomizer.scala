package mrtjp.projectred.fabrication.circuitparts.misc

import mrtjp.projectred.core.TFaceOrient
import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, YellowChipModel}

import java.util.Random

object Randomizer extends ComboICGateLogic {
  val rand = new Random

  override def outputMask(shape: Int) =
    ~((shape & 1) << 1 | (shape & 2) >> 1 | (shape & 4) << 1) & 0xb
  override def inputMask(shape: Int) = 4
  override def feedbackMask(shape: Int) = outputMask(shape)

  override def deadSides = 3

  override def getDelay(shape: Int) = 2

  override def calcOutput(gate: ComboICGatePart, input: Int) = {
    if (input == 0) gate.state >> 4
    else
      outputMask(gate.shape) & TFaceOrient.shiftMask(rand.nextInt(8), 3)
  }

  override def onChange(gate: ComboICGatePart) {
    super.onChange(gate)
    if ((gate.state & 4) != 0) gate.scheduleTick(2)
  }
}

class RenderRandomizer extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("RAND", 7)
  val chips = Seq(
    new YellowChipModel(8, 5.5),
    new YellowChipModel(11.5, 11.5),
    new YellowChipModel(4.5, 11.5)
  )

  override val coreModels =
    Seq(new BaseComponentModel("RAND")) ++ wires ++ chips

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(1).on = false
    wires(2).on = false
    wires(3).on = false
    wires(4).on = false
    wires(5).on = false
    wires(6).on = false
    wires(1).disabled = (configuration & 1) != 0
    wires(0).disabled = (configuration & 2) != 0
    wires(3).disabled = (configuration & 4) != 0
    wires(5).disabled = wires(1).disabled
    wires(4).disabled = wires(0).disabled
    wires(6).disabled = wires(3).disabled
    chips(0).on = false
    chips(1).on = false
    chips(2).on = false
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(2).on = (gate.state & 4) != 0
    wires(0).on = (gate.state & 0x11) != 0
    wires(1).on = (gate.state & 0x22) != 0
    wires(3).on = (gate.state & 0x88) != 0
    wires(4).on = wires(2).on
    wires(5).on = wires(2).on
    wires(6).on = wires(2).on
    wires(1).disabled = (gate.shape & 1) != 0
    wires(0).disabled = (gate.shape & 2) != 0
    wires(3).disabled = (gate.shape & 4) != 0
    wires(5).disabled = wires(1).disabled
    wires(4).disabled = wires(0).disabled
    wires(6).disabled = wires(3).disabled
    chips(0).on = (gate.state & 0x10) != 0
    chips(1).on = (gate.state & 0x20) != 0
    chips(2).on = (gate.state & 0x80) != 0
  }
}
