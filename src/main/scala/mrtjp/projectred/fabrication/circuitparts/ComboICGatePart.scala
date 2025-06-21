package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.gui.nodes.configuration.ConfigurationRotationConfig
import mrtjp.projectred.fabrication.gui.nodes.{ConfigurationNode, TConfigurable}

class ComboICGatePart extends RedstoneGateICPart with TConfigurable {
  override def getLogic[T] = ComboICGateLogic.instances(subID).asInstanceOf[T]

  def getLogicCombo = getLogic[ComboICGateLogic]

  override def getPartType = CircuitPartDefs.SimpleGate

  override def createConfigurationNode: ConfigurationNode =
    new ConfigurationRotationConfig(this)
}
