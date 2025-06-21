package mrtjp.projectred.fabrication.gui.nodes.configuration

import codechicken.lib.vec.{Rotation, Scale, TransformationList, Translation}
import mrtjp.core.gui.IconButtonNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.io.IOGateICPart
import mrtjp.projectred.fabrication.{ArrowModel, ICComponentStore}

class ConfigurationSimpleIO(gate: IOGateICPart)
    extends ConfigurationRotation(gate) {

  val arrowModel = new ArrowModel

  val in = new IconButtonNode {
    override def drawButton(mouseover: Boolean): Unit = {
      renderIcon(0, position)
    }
  }

  in.position = Point(5, 80)
  in.size = Size(20, 15)
  in.clickDelegate = { () =>
    gate.sendClientPacket(_.writeByte(6).writeByte(0))
  }
  addChild(in)

  val out = new IconButtonNode {
    override def drawButton(mouseover: Boolean): Unit =
      renderIcon(1, position)
  }
  out.position = Point(25, 80)
  out.size = Size(20, 15)
  out.clickDelegate = { () =>
    gate.sendClientPacket(_.writeByte(6).writeByte(1))
  }
  addChild(out)

  val inout = new IconButtonNode {
    override def drawButton(mouseover: Boolean): Unit =
      renderIcon(2, position)
  }
  inout.position = Point(45, 80)
  inout.size = Size(20, 15)
  inout.clickDelegate = { () =>
    gate.sendClientPacket(_.writeByte(6).writeByte(2))
  }
  addChild(inout)

  private def renderIcon(icon: Int, position: Point): Unit = {
    val t = new TransformationList(
      new Scale(40, 1, -40),
      new Rotation(0.5 * math.Pi, 1, 0, 0),
      new Translation(position.x - 9, position.y - 12, 0)
    )
    arrowModel.arrowDirection = icon
    ICComponentStore.prepairRender()
    arrowModel.renderModel(t, 0, true)
    ICComponentStore.finishRender()
  }
}
