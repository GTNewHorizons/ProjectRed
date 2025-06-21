package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.circuitparts.latches._
import mrtjp.projectred.fabrication.circuitparts.misc.{Counter, Synchronizer}
import mrtjp.projectred.fabrication.circuitparts.timing._

abstract class SequentialICGateLogic(val gate: SequentialGateICPart)
    extends RedstoneICGateLogic[SequentialGateICPart]
    with TComplexICGateLogic[SequentialGateICPart]

object SequentialICGateLogic {

  def create(gate: SequentialGateICPart, subID: Int) = subID match {
    case ICGateDefinition.SRLatch.ordinal      => new SRLatch(gate)
    case ICGateDefinition.ToggleLatch.ordinal  => new ToggleLatch(gate)
    case ICGateDefinition.Timer.ordinal        => new Timer(gate)
    case ICGateDefinition.Sequencer.ordinal    => new Sequencer(gate)
    case ICGateDefinition.Counter.ordinal      => new Counter(gate)
    case ICGateDefinition.StateCell.ordinal    => new StateCell(gate)
    case ICGateDefinition.Synchronizer.ordinal => new Synchronizer(gate)
    case _ => throw new IllegalArgumentException("Invalid gate subID: " + subID)
  }
}
