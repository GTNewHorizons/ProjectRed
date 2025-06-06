/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts.io

trait TRSIOICGateLogic extends IOICGateLogic {
  override def setup(gate: IOGateICPart) {
    if ((gate.world.iostate(gate.rotation) & 0xffff) == 0) {
      gate.world.setInput(gate.rotation, 1)
      gate.world.onInputChanged(1 << gate.rotation)
    }
  }

  override def extInputChange(gate: IOGateICPart) {
    if ((gate.world.iostate(gate.rotation) & 0xffff) == 0) {
      gate.world.setInput(gate.rotation, 1)
      gate.world.onInputChanged(1 << gate.rotation)
    }
    super.extInputChange(gate)
  }
}
