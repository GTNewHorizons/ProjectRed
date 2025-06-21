package mrtjp.projectred.fabrication.circuitparts.io

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.circuitparts.{
  ICGateDefinition,
  RedstoneICGateLogic,
  TComplexICGateLogic,
  TICOrient
}

object IOICGateLogic {
  def create(gate: IOGateICPart, subID: Int) = subID match {
    case ICGateDefinition.IOSimple.ordinal  => new SimpleIOICGateLogic(gate)
    case ICGateDefinition.IOAnalog.ordinal  => new AnalogIOICGateLogic(gate)
    case ICGateDefinition.IOBundled.ordinal => new BundledIOICGateLogic(gate)
    case _ => throw new IllegalArgumentException("Invalid gate subID: " + subID)
  }

  def cycleShape(shape: Int): Int = {
    (shape + 1) % 3
  }
}

abstract class IOICGateLogic(val gate: IOGateICPart)
    extends RedstoneICGateLogic[IOGateICPart]
    with TComplexICGateLogic[IOGateICPart] {

  import TIOCircuitPart._

  override def inputMask(shape: Int) = shape match {
    case 0 => 1
    case 1 => 4
    case 2 => 5
  }

  override def outputMask(shape: Int) = shape match {
    case 0 => 4
    case 1 => 1
    case 2 => 5
  }

  override def cycleShape(gate: IOGateICPart): Boolean = {
    gate.setShape(IOICGateLogic.cycleShape(gate.shape))
    true
  }

  def extInputChange(gate: IOGateICPart) {
    gate.onChange()
  }

  def extOutputChange(gate: IOGateICPart) {}

  def getIOMode(gate: IOGateICPart): Int = gate.shape match {
    case 0 => Input
    case 1 => Output
    case 2 => InOut
  }

  def getConnMode(gate: IOGateICPart): Int

  def resolveInputFromWorld: Int

  def resolveOutputToWorld: Int

  def setWorldOutput(state: Boolean)

  def toggleWorldInput()

  override def onChange(gate: IOGateICPart) {
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, ~(gate.state >> 4) & inputMask(gate.shape))
    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
      gate.scheduleTick(0)
    }
  }

  override def scheduledTick(gate: IOGateICPart) {
    val oldOutput = gate.state >> 4
    val newOutput =
      TICOrient.shiftMask(gate.state & 0xf, 2) & outputMask(gate.shape)
    if (oldOutput != newOutput) {
      gate.setState(gate.state & 0xf | newOutput << 4)
      gate.onOutputChange(oldOutput ^ newOutput)
    }
    onChange(gate)
  }

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: IOGateICPart, detailLevel: Int) = {
    val s = Seq.newBuilder[String]
    if (detailLevel >= 2) {
      val f = getFreqName
      if (f.nonEmpty) s += "freq: " + f
      s += "mode: " + (gate.shape match {
        case 0 => "I"
        case 1 => "O"
        case 2 => "IO"
      })
    }
    if (detailLevel >= 3) {
      s += "I: " + (if (resolveInputFromWorld != 0) "high" else "low")
      s += "O: " + (if (resolveOutputToWorld != 0) "high" else "low")
    }
    super.getRolloverData(gate, detailLevel) ++ s.result()
  }

  def getFreqName = ""

  override def activate(gate: IOGateICPart) {
    toggleWorldInput()
    gate.world.onInputChanged(1 << gate.rotation)
  }
}
