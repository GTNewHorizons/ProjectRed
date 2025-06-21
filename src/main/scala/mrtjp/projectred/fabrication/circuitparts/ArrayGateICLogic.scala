package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.circuitparts.cells.{
  BufferCell,
  InvertCell,
  NullCell
}

object ArrayGateICLogic {

  import ICGateDefinition._

  def create(gate: ArrayGateICPart, subID: Int) = subID match {
    case NullCell.ordinal   => new NullCell(gate)
    case InvertCell.ordinal => new InvertCell(gate)
    case BufferCell.ordinal => new BufferCell(gate)
    case _ => throw new IllegalArgumentException("Invalid gate subID: " + subID)
  }
}

abstract class ArrayGateICLogic(val gate: ArrayGateICPart)
    extends RedstoneICGateLogic[ArrayGateICPart]
    with TArrayICGateLogic[ArrayGateICPart]
    with TComplexICGateLogic[ArrayGateICPart]
