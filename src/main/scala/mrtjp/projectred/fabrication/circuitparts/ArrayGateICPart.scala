package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.gui.nodes.configuration.ConfigurationRotation
import mrtjp.projectred.fabrication.gui.nodes.{ConfigurationNode, TConfigurable}

class ArrayGateICPart
    extends RedstoneGateICPart
    with TComplexGateICPart
    with TArrayGateICPart
    with TConfigurable {
  private var logic: ArrayGateICLogic = null

  override def getLogic[T] = logic.asInstanceOf[T]

  override def assertLogic() {
    if (logic == null) logic = ArrayGateICLogic.create(this, subID)
  }

  override def getPartType = CircuitPartDefs.ArrayGate

  override def createConfigurationNode: ConfigurationNode =
    new ConfigurationRotation(this)
}
