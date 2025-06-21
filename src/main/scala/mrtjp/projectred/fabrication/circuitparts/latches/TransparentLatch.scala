package mrtjp.projectred.fabrication.circuitparts.latches

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object TransparentLatch extends ComboICGateLogic {
  override def outputMask(shape: Int) = if (shape == 0) 3 else 9
  override def inputMask(shape: Int) = if (shape == 0) 0xc else 6

  override def cycleShape(shape: Int) = shape ^ 1

  override def calcOutput(gate: ComboICGatePart, input: Int) = {
    if ((input & 4) == 0) gate.state >> 4
    else if ((input & 0xa) == 0) 0
    else 0xf
  }
}

class RenderTransparentLatch extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("TRANSLATCH", 5)
  val torches = Seq(
    new RedstoneTorchModel(4, 12.5),
    new RedstoneTorchModel(4, 8),
    new RedstoneTorchModel(8, 8),
    new RedstoneTorchModel(8, 2),
    new RedstoneTorchModel(14, 8)
  )

  override val coreModels =
    Seq(new BaseComponentModel("TRANSLATCH")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    reflect = configuration == 1
    wires(0).on = true
    wires(1).on = false
    wires(2).on = true
    wires(3).on = false
    wires(4).on = false
    torches(0).on = true
    torches(1).on = false
    torches(2).on = true
    torches(3).on = false
    torches(4).on = false
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    reflect = gate.shape == 1
    val on = (gate.state & 0x10) != 0
    wires(0).on = !on
    wires(1).on = (gate.state & 4) != 0
    wires(2).on = (gate.state & 4) == 0
    wires(3).on = on
    wires(4).on = (gate.state & 0xa) != 0
    torches(0).on = wires(2).on
    torches(1).on = !wires(2).on && !wires(4).on
    torches(2).on = !wires(1).on && !wires(3).on
    torches(3).on = on
    torches(4).on = on
  }
}
