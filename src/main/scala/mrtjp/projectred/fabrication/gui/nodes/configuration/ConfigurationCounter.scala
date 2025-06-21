package mrtjp.projectred.fabrication.gui.nodes.configuration

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.SimpleTextboxNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.SequentialGateICPart
import mrtjp.projectred.fabrication.circuitparts.misc.ICounterGuiLogic

import scala.util.Try

class ConfigurationCounter(gate: SequentialGateICPart)
    extends ConfigurationRotationConfig(gate) {
  private val counterLogic =
    gate.getLogicPrimitive.asInstanceOf[ICounterGuiLogic]

  val max = new SimpleTextboxNode()
  max.position = Point(30, 118)
  max.size = Size(30, 12)
  max.text = counterLogic.getCounterMax.toString
  max.allowedcharacters = "0123456789"
  max.textChangedDelegate = { () =>
    {
      val num = Try(max.text.toInt)
      gate.sendClientPacket(
        _.writeByte(4).writeByte(0).writeShort(num.getOrElse(1))
      )
    }
  }
  addChild(max)

  val inc = new SimpleTextboxNode()
  inc.position = Point(30, 138)
  inc.size = Size(30, 12)
  inc.text = counterLogic.getCounterIncr.toString
  inc.allowedcharacters = "0123456789"
  inc.textChangedDelegate = { () =>
    {
      val num = Try(inc.text.toInt)
      gate.sendClientPacket(
        _.writeByte(4).writeByte(1).writeShort(num.getOrElse(1))
      )
    }
  }
  addChild(inc)

  val dec = new SimpleTextboxNode()
  dec.position = Point(30, 158)
  dec.size = Size(30, 12)
  dec.text = counterLogic.getCounterDecr.toString
  dec.allowedcharacters = "0123456789"
  dec.textChangedDelegate = { () =>
    {
      val num = Try(dec.text.toInt)
      gate.sendClientPacket(
        _.writeByte(4).writeByte(2).writeShort(num.getOrElse(1))
      )
    }
  }
  addChild(dec)

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    super.drawBack_Impl(mouse, rframe)

    val pos_state = position.add(5, 102)
    GuiDraw.drawString(
      "State: " + counterLogic.getCounterValue.toString,
      pos_state.x,
      pos_state.y,
      Colors.GREY.argb,
      false
    )

    val pos_max = position.add(5, 120)
    GuiDraw.drawString("Max", pos_max.x, pos_max.y, Colors.GREY.argb, false)

    val pos_inc = position.add(5, 140)
    GuiDraw.drawString("Inc", pos_inc.x, pos_inc.y, Colors.GREY.argb, false)

    val pos_dec = position.add(5, 160)
    GuiDraw.drawString("Dec", pos_dec.x, pos_dec.y, Colors.GREY.argb, false)
  }
}
