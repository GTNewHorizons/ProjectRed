package mrtjp.projectred.fabrication.gui.nodes.configuration

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.SimpleTextboxNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.io.{
  AnalogIOICGateLogic,
  IOGateICPart
}

import scala.util.Try

class ConfigurationAnalogIO(gate: IOGateICPart)
    extends ConfigurationSimpleIO(gate) {
  private val logic = gate.getLogicPrimitive.asInstanceOf[AnalogIOICGateLogic]

  val freq = new SimpleTextboxNode
  freq.position = Point(20, 110)
  freq.size = Size(30, 12)
  freq.text = "0x%x".format(logic.freq)
  freq.textChangedDelegate = { () =>
    {
      val num = Try(Integer.parseInt(freq.text.substring(2), 16))
      gate.sendFrequency(num.getOrElse(0))
    }
  }
  addChild(freq)

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    super.drawBack_Impl(mouse, rframe)

    val pos_text = position.add(5, 100)
    GuiDraw.drawString(
      "Frequency:",
      pos_text.x,
      pos_text.y,
      Colors.GREY.argb,
      false
    )
  }
}
