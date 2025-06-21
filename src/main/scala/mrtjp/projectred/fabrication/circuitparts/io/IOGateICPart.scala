package mrtjp.projectred.fabrication.circuitparts.io

import codechicken.lib.data.MCDataInput
import mrtjp.projectred.fabrication.circuitparts.{
  CircuitPartDefs,
  RedstoneGateICPart,
  TComplexGateICPart
}
import mrtjp.projectred.fabrication.gui.nodes.configuration.{
  ConfigurationAnalogIO,
  ConfigurationBundledIO,
  ConfigurationSimpleIO
}
import mrtjp.projectred.fabrication.gui.nodes.{ConfigurationNode, TConfigurable}

class IOGateICPart
    extends RedstoneGateICPart
    with TIOCircuitPart
    with TComplexGateICPart
    with TConfigurable {
  private var logic: IOICGateLogic = null

  override def getLogic[T] = logic.asInstanceOf[T]

  def getLogicIO = getLogic[IOICGateLogic]

  override def assertLogic() {
    if (logic == null) logic = IOICGateLogic.create(this, subID)
  }

  override def readClientPacket(in: MCDataInput, key: Int) = key match {
    case 5 =>
      getLogicIO match {
        case f: TFreqIOICGateLogic =>
          f.freq = in.readByte()
          f.sendFreqUpdate()
          f.gate.onChange()
        case _ =>
      }
    case 6 =>
      getLogicIO match {
        case f: IOICGateLogic =>
          f.gate.setShape(in.readByte())
          this.sendShapeUpdate()
          f.gate.onChange()
        case _ =>
      }
    case _ => super.readClientPacket(in, key)
  }

  def sendFrequency(color: Int): Unit = {
    sendClientPacket(_.writeByte(5).writeByte(color))
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

  override def createConfigurationNode: ConfigurationNode = {
    getLogicIO match {
      case _: BundledIOICGateLogic =>
        new ConfigurationBundledIO(this)
      case _: SimpleIOICGateLogic =>
        new ConfigurationSimpleIO(this)
      case _: AnalogIOICGateLogic =>
        new ConfigurationAnalogIO(this)
    }
  }
}
