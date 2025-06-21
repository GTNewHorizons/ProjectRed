package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.MCDataInput
import mrtjp.projectred.ProjectRedCore.log
import mrtjp.projectred.fabrication.circuitparts.latches.{SRLatch, ToggleLatch}
import mrtjp.projectred.fabrication.circuitparts.misc.{
  ICounterGuiLogic,
  Synchronizer
}
import mrtjp.projectred.fabrication.circuitparts.timing.ITimerGuiLogic
import mrtjp.projectred.fabrication.gui.nodes.configuration.{
  ConfigurationCounter,
  ConfigurationRotation,
  ConfigurationRotationConfig,
  ConfigurationTimer
}
import mrtjp.projectred.fabrication.gui.nodes.{ConfigurationNode, TConfigurable}

class SequentialGateICPart
    extends RedstoneGateICPart
    with TComplexGateICPart
    with TConfigurable {
  var logic: SequentialICGateLogic = null

  override def assertLogic() {
    if (logic == null) logic = SequentialICGateLogic.create(this, subID)
  }

  override def getLogic[T]: T = logic.asInstanceOf[T]

  override def getPartType = CircuitPartDefs.ComplexGate

  override def readClientPacket(in: MCDataInput, key: Int) = key match {
    case 3 =>
      getLogicPrimitive match {
        case t: ITimerGuiLogic =>
          t.setTimerMax(this, in.readShort())
        case _ =>
          log.error(
            "Server IC stream received client packet for incorrect gate type"
          )
      }
    case 4 =>
      getLogicPrimitive match {
        case t: ICounterGuiLogic =>
          val actionID = in.readByte()
          actionID match {
            case 0 => t.setCounterMax(this, t.getCounterMax + in.readShort())
            case 1 => t.setCounterIncr(this, t.getCounterIncr + in.readShort())
            case 2 => t.setCounterDecr(this, t.getCounterDecr + in.readShort())
            case _ =>
              log.error(
                "Server IC stream received client packet for incorrect gate type"
              )
          }
        case _ =>
          log.error(
            "Server IC stream received client packet for incorrect gate type"
          )
      }
    case _ => super.readClientPacket(in, key)
  }

  override def createConfigurationNode: ConfigurationNode = {
    getLogicPrimitive match {
      case _: ICounterGuiLogic => new ConfigurationCounter(this)
      case _: ITimerGuiLogic   => new ConfigurationTimer(this)
      case _ =>
        new ConfigurationRotationConfig(this)
    }
  }
}
