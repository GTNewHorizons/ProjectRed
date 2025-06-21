package mrtjp.projectred.fabrication.gui.nodes.configuration

import mrtjp.core.gui.MCButtonNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.GateICPart

class ConfigurationRotationConfig(gate: GateICPart)
    extends ConfigurationRotation(gate) {

  val conf = new MCButtonNode
  conf.position = Point(8, 80)
  conf.size = Size(50, 15)
  conf.text = "configure"
  conf.clickDelegate = { () => gate.sendClientPacket(_.writeByte(1)) }
  addChild(conf)
}
