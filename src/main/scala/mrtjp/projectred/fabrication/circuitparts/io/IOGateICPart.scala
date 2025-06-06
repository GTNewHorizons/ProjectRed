/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts.io

import codechicken.lib.data.MCDataInput
import mrtjp.projectred.fabrication.circuitparts.{CircuitPartDefs, RedstoneGateICPart, TComplexGateICPart}

class IOGateICPart
  extends RedstoneGateICPart
    with TIOCircuitPart
    with TComplexGateICPart {
  private var logic: IOICGateLogic = null

  override def getLogic[T] = logic.asInstanceOf[T]

  def getLogicIO = getLogic[IOICGateLogic]

  override def assertLogic() {
    if (logic == null) logic = IOICGateLogic.create(this, subID)
  }

  override def readClientPacket(in: MCDataInput, key: Int) = key match {
    case 5 =>
      getLogicIO match {
        case f: TFreqIOICGateLogic => f.freqUp()
        case _ =>
      }
    case 6 =>
      getLogicIO match {
        case f: TFreqIOICGateLogic => f.freqDown()
        case _ =>
      }
    case _ => super.readClientPacket(in, key)
  }

  override def getPartType = CircuitPartDefs.IOGate

  override def onExtInputChanged(r: Int) {
    if (r == rotation) getLogicIO.extInputChange(this)
  }

  override def onExtOutputChanged(r: Int) {
    if (r == rotation) getLogicIO.extOutputChange(this)
  }

  override def getIOSide = rotation

  override def getIOMode = getLogicIO.getIOMode(this)

  override def getConnMode = getLogicIO.getConnMode(this)

  override def getRedstoneInput(r: Int): Int = {
    if (r == 0) getLogicIO.resolveInputFromWorld // r is to outside world
    else super.getRedstoneInput(r)
  }

  override def onOutputChange(mask: Int) {
    super.onOutputChange(mask)
    if ((mask & 1) != 0) {
      val oldOutput = world.iostate(rotation) >>> 16
      getLogicIO.setWorldOutput((state & 0x10) != 0)
      val newOutput = world.iostate(rotation) >>> 16
      if (oldOutput != newOutput) world.onOutputChanged(1 << rotation)
    }
  }
}
