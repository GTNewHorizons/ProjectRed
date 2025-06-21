package mrtjp.projectred.fabrication.gui.nodes.configuration

import mrtjp.core.gui.MCButtonNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.GateICPart
import mrtjp.projectred.fabrication.gui.nodes.ConfigurationNode

class ConfigurationRotation(gate: GateICPart) extends ConfigurationNode(gate) {
  val rotate = new MCButtonNode
  rotate.position = Point(8, 60)
  rotate.size = Size(50, 15)
  rotate.text = "rotate"
  rotate.clickDelegate = { () => gate.sendClientPacket(_.writeByte(0)) }
  addChild(rotate)
}
