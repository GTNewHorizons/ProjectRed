/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts.primitives

import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}

object XOR extends ComboICGateLogic {
  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = 10

  override def calcOutput(gate: ComboICGatePart, input: Int) = {
    val side1 = (input & 1 << 1) != 0
    val side2 = (input & 1 << 3) != 0
    if (side1 != side2) 1 else 0
  }
}

class RenderXOR extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("XOR", 4)
  val torches = Seq(
    new RedstoneTorchModel(4.5, 8),
    new RedstoneTorchModel(11.5, 8),
    new RedstoneTorchModel(8, 12)
  )

  override val coreModels =
    Seq(new BaseComponentModel("XOR")) ++ wires ++ torches

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = false
    wires(3).on = false
    wires(2).on = false
    wires(1).on = true
    torches(0).on = false
    torches(1).on = false
    torches(2).on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x11) != 0
    wires(3).on = (gate.state & 2) != 0
    wires(2).on = (gate.state & 8) != 0
    wires(1).on = !wires(3).on && !wires(2).on
    torches(0).on = !wires(2).on && !wires(1).on
    torches(1).on = !wires(3).on && !wires(1).on
    torches(2).on = wires(1).on
  }
}
