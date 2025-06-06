/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts


class ArrayGateICPart
  extends RedstoneGateICPart
    with TComplexGateICPart
    with TArrayGateICPart {
  private var logic: ArrayGateICLogic = null

  override def getLogic[T] = logic.asInstanceOf[T]

  override def assertLogic() {
    if (logic == null) logic = ArrayGateICLogic.create(this, subID)
  }

  override def getPartType = CircuitPartDefs.ArrayGate
}
