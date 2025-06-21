package mrtjp.projectred.fabrication.circuitparts.timing

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

class RenderPulse extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("PULSE", 3)
  val torches = Seq(
    new RedstoneTorchModel(4, 9.5),
    new RedstoneTorchModel(11, 9.5),
    new RedstoneTorchModel(8, 3.5)
  )

  override val coreModels =
    Seq(new BaseComponentModel("PULSE")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    wires(2).on = false
    torches(0).on = true
    torches(1).on = false
    torches(2).on = false
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 4) == 0
    wires(1).on = (gate.state & 4) != 0
    wires(2).on = (gate.state & 0x14) == 4
    torches(0).on = wires(0).on
    torches(1).on = wires(1).on
    torches(2).on = (gate.state & 0x10) != 0
  }
}
