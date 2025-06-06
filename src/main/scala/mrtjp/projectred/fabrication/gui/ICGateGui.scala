/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui

import mrtjp.core.gui.MCButtonNode
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.circuitparts.GateICPart

class ICGateGui(override val gate: GateICPart)
  extends CircuitGui(gate)
    with TGateGui {
  {
    size = Size(120, 55)

    val rotate = new MCButtonNode
    rotate.position = Point(58, 12)
    rotate.size = Size(50, 15)
    rotate.text = "rotate"
    rotate.clickDelegate = { () => gate.sendClientPacket(_.writeByte(0)) }
    addChild(rotate)

    val conf = new MCButtonNode
    conf.position = Point(58, 28)
    conf.size = Size(50, 15)
    conf.text = "configure"
    conf.clickDelegate = { () => gate.sendClientPacket(_.writeByte(1)) }
    addChild(conf)
  }
}
