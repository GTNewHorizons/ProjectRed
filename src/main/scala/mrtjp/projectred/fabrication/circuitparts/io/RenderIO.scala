/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts.io

import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.ICGateRenderer
import mrtjp.projectred.fabrication.{BaseComponentModel, IOSigModel}

abstract class RenderIO extends ICGateRenderer[IOGateICPart] {
  val wires = generateWireModels("IOSIMP", 1)
  val iosig = new IOSigModel

  override val coreModels =
    Seq(new BaseComponentModel("IOSIMP")) ++ wires :+ iosig

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    iosig.on = false
    iosig.colour = invColour
  }

  override def prepareDynamic(gate: IOGateICPart, frame: Float) {
    wires(0).on = (gate.state & 0x44) != 0
    iosig.on = wires(0).on
    iosig.colour = dynColour(gate)
  }

  def invColour: Int

  def dynColour(gate: IOGateICPart): Int
}
