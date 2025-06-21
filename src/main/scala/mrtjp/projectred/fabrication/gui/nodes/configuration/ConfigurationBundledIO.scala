package mrtjp.projectred.fabrication.gui.nodes.configuration

import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.io.IOGateICPart

class ConfigurationBundledIO(gate: IOGateICPart)
    extends ConfigurationSimpleIO(gate) {
  val colorPicker = new ColorPicker(
    gate.getIOMode,
    onPickColor = color => {
      gate.sendFrequency(color)
    }
  )
  colorPicker.position = Point(5, 100)
  colorPicker.size = Size(60, 60)
  addChild(colorPicker)
}
