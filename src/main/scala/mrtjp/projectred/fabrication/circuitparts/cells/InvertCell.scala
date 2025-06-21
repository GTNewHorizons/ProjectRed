package mrtjp.projectred.fabrication.circuitparts.cells

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{
  ArrayGateICLogicCrossing,
  ArrayGateICPart,
  ICGateRenderer
}

class InvertCell(gate: ArrayGateICPart) extends ArrayGateICLogicCrossing(gate) {
  override def powerUp = (gate.state & 2) != 0
}

class RenderInvertCell extends ICGateRenderer[ArrayGateICPart] {
  val wires = generateWireModels("INVCELL", 1)
  val torch = new RedstoneTorchModel(8, 8)
  val top = new CellTopWireModel
  val bottom = new InvertCellBottomWireModel

  override val coreModels = Seq(
    new BaseComponentModel("INVCELL")
  ) ++ wires ++ Seq(bottom, torch, new CellStandModel, top)

  override def prepareStatic(configuration: Int): Unit = {
    bottom.signal = 0
    top.signal = 255.toByte
    wires(0).on = false
    torch.on = true
  }

  override def prepareDynamic(gate: ArrayGateICPart, frame: Float) {
    val logic = gate.getLogic[InvertCell]
    bottom.signal = logic.signal1
    top.signal = logic.signal2
    wires(0).on = logic.signal1 != 0
    torch.on = logic.signal1 == 0
  }
}
