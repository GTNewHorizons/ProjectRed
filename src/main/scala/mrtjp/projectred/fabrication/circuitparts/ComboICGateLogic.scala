package mrtjp.projectred.fabrication.circuitparts

import mrtjp.projectred.fabrication.circuitparts.latches.TransparentLatch
import mrtjp.projectred.fabrication.circuitparts.misc.{
  DecRandomizer,
  Randomizer
}
import mrtjp.projectred.fabrication.circuitparts.primitives._
import mrtjp.projectred.fabrication.circuitparts.timing.Repeater

abstract class ComboICGateLogic
    extends RedstoneICGateLogic[ComboICGatePart]
    with TSimpleRSICGateLogic[ComboICGatePart] {
  override def cycleShape(gate: ComboICGatePart) = {
    val oldShape = gate.shape
    val newShape = cycleShape(oldShape)
    if (newShape != oldShape) {
      gate.setShape(newShape)
      true
    } else false
  }

  def cycleShape(shape: Int): Int = {
    if (deadSides == 0) return shape

    var shape1 = shape
    import java.lang.Integer.{bitCount, numberOfLeadingZeros => lead}
    do shape1 = ComboICGateLogic.advanceDead(shape1) while (bitCount(
      shape1
    ) > maxDeadSides || 32 - lead(shape1) > deadSides)
    shape1
  }

  def deadSides = 0

  def maxDeadSides = deadSides - 1
}

object ComboICGateLogic {
  val advanceDead = Seq(1, 2, 4, 0, 5, 6, 3)

  val instances = new Array[ComboICGateLogic](ICGateDefinition.values.length)
  initialize()

  def initialize() {
    instances(ICGateDefinition.OR.ordinal) = OR
    instances(ICGateDefinition.NOR.ordinal) = NOR
    instances(ICGateDefinition.NOT.ordinal) = NOT
    instances(ICGateDefinition.AND.ordinal) = AND
    instances(ICGateDefinition.NAND.ordinal) = NAND
    instances(ICGateDefinition.XOR.ordinal) = XOR
    instances(ICGateDefinition.XNOR.ordinal) = XNOR
    instances(ICGateDefinition.Buffer.ordinal) = Buffer
    instances(ICGateDefinition.Multiplexer.ordinal) = Multiplexer
    instances(ICGateDefinition.Pulse.ordinal) = Pulse
    instances(ICGateDefinition.Repeater.ordinal) = Repeater
    instances(ICGateDefinition.Randomizer.ordinal) = Randomizer
    instances(ICGateDefinition.TransparentLatch.ordinal) = TransparentLatch
    instances(ICGateDefinition.DecRandomizer.ordinal) = DecRandomizer
  }
}
