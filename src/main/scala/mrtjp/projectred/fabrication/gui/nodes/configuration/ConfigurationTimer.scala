package mrtjp.projectred.fabrication.gui.nodes.configuration

import codechicken.lib.gui.GuiDraw
import codechicken.lib.vec.Translation
import mrtjp.core.color.Colors
import mrtjp.core.gui.SimpleTextboxNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.ICComponentStore
import mrtjp.projectred.fabrication.circuitparts.timing.ITimerGuiLogic
import mrtjp.projectred.fabrication.circuitparts.{
  ICGateRenderer,
  SequentialGateICPart
}

import scala.util.Try

class ConfigurationTimer(gate: SequentialGateICPart)
    extends ConfigurationRotation(gate) {
  private val timerLogic = gate.getLogicPrimitive.asInstanceOf[ITimerGuiLogic]

  val text = new SimpleTextboxNode
  text.position = Point(20, 95)
  text.size = Size(30, 10)
  text.allowedcharacters = "0123456789.,"
  text.text = "%.2f".format(timerLogic.getTimerMax * 0.05)
  text.textChangedDelegate = () => {
    val time = Try(text.text.replaceAll(",", ".").toDouble)
    gate.sendClientPacket(
      _.writeByte(3).writeShort((time.getOrElse(1d) * 20).toInt)
    )
  }
  addChild(text)

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    super.drawBack_Impl(mouse, rframe)

    val pos_state = position.add(13, 82)
    GuiDraw.drawString(
      "Interval:",
      pos_state.x,
      pos_state.y,
      Colors.GREY.argb,
      false
    )
  }
}
