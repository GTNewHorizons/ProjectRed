package mrtjp.projectred.fabrication.circuitparts

import mrtjp.core.util.Enum
import mrtjp.projectred.fabrication.circuitparts.io.IOGateICPart
import mrtjp.projectred.fabrication.circuitparts.wire._

object CircuitPartDefs extends Enum {
  type EnumVal = CircuitPartDef

  val Torch = CircuitPartDef(() => new TorchICPart)
  val Lever = CircuitPartDef(() => new LeverICPart)
  val Button = CircuitPartDef(() => new ButtonICPart)

  val AlloyWire = CircuitPartDef(() => new AlloyWireICPart)
  val InsulatedWire = CircuitPartDef(() => new InsulatedWireICPart)
  val BundledCable = CircuitPartDef(() => new BundledCableICPart)

  val IOGate = CircuitPartDef(() => new IOGateICPart)
  val SimpleGate = CircuitPartDef(() => new ComboICGatePart)
  val ComplexGate = CircuitPartDef(() => new SequentialGateICPart)
  val ArrayGate = CircuitPartDef(() => new ArrayGateICPart)

  case class CircuitPartDef(factory: () => CircuitPart) extends Value {
    def id = ordinal

    override def name = s"$id"

    def createPart = factory.apply()
  }
}
