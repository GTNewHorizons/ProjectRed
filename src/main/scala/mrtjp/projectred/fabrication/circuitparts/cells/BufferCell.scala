package mrtjp.projectred.fabrication.circuitparts.cells

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{
  ArrayGateICLogicCrossing,
  ArrayGateICPart,
  ICGateRenderer
}

class BufferCell(gate: ArrayGateICPart) extends ArrayGateICLogicCrossing(gate) {
  override def powerUp = (gate.state & 2) == 0
}

class RenderBufferCell extends ICGateRenderer[ArrayGateICPart] {
  val wires = generateWireModels("BUFFCELL", 2)
  val torches =
    Seq(new RedstoneTorchModel(11, 13), new RedstoneTorchModel(8, 8))
  val top = new CellTopWireModel
  val bottom = new InvertCellBottomWireModel

  override val coreModels =
    Seq(new BaseComponentModel("BUFFCELL")) ++ wires ++ Seq(
      bottom
    ) ++ torches ++ Seq(new CellStandModel, top)

  override def prepareStatic(configuration: Int): Unit = {
    bottom.signal = 0
    top.signal = 0
    wires(0).on = false
    wires(1).on = true
    torches(0).on = true
    torches(1).on = false
  }

  override def prepareDynamic(gate: ArrayGateICPart, frame: Float) {
    val logic = gate.getLogic[BufferCell]
    bottom.signal = logic.signal1
    top.signal = logic.signal2
    torches(0).on = logic.signal1 == 0
    torches(1).on = logic.signal1 != 0
    wires(0).on = logic.signal1 != 0
    wires(1).on = logic.signal1 == 0
  }
}
