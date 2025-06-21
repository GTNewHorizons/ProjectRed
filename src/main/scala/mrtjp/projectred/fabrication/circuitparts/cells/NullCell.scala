package mrtjp.projectred.fabrication.circuitparts.cells

import mrtjp.projectred.fabrication.circuitparts.{
  ArrayGateICLogicCrossing,
  ArrayGateICPart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{
  BaseComponentModel,
  CellStandModel,
  CellTopWireModel,
  NullCellBottomWireModel
}

class NullCell(gate: ArrayGateICPart) extends ArrayGateICLogicCrossing(gate) {
  override def powerUp = false
}

class RenderNullCell extends ICGateRenderer[ArrayGateICPart] {
  val top = new CellTopWireModel
  val bottom = new NullCellBottomWireModel

  override val coreModels =
    Seq(new BaseComponentModel("NULLCELL"), bottom, new CellStandModel, top)

  override def prepareStatic(configuration: Int): Unit = {
    bottom.signal = 0
    top.signal = 0
  }

  override def prepareDynamic(gate: ArrayGateICPart, frame: Float) {
    bottom.signal = gate.getLogic[NullCell].signal1
    top.signal = gate.getLogic[NullCell].signal2
  }
}
