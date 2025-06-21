package mrtjp.projectred.fabrication.circuitparts.io

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.ICGateRenderer
import mrtjp.projectred.fabrication.{ArrowModel, BaseComponentModel, IOSigModel}

abstract class RenderIO extends ICGateRenderer[IOGateICPart] {
  val wires = generateWireModels("IOSIMP", 1)
  val iosig = new IOSigModel
  val arrow = new ArrowModel

  override val coreModels =
    Seq(new BaseComponentModel("IOSIMP")) ++ wires :+ iosig :+ arrow

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    iosig.on = false
    iosig.colour = invColour
    arrow.arrowDirection = configuration
  }

  override def prepareDynamic(gate: IOGateICPart, frame: Float) {
    wires(0).on = (gate.state & 0x44) != 0
    iosig.on = wires(0).on
    iosig.colour = dynColour(gate)
    arrow.arrowDirection = gate.shape
  }

  def invColour: Int

  def dynColour(gate: IOGateICPart): Int
}
