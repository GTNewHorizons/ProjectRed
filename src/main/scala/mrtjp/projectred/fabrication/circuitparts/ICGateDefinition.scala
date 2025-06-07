/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

import mrtjp.core.util.Enum
import mrtjp.projectred.integration.GateDefinition.GateDef

object ICGateDefinition extends Enum {
  type EnumVal = ICGateDef

  import mrtjp.projectred.integration.{GateDefinition => gd}

  val IOSimple = ICGateDef("Simple IO", CircuitPartDefs.IOGate.id)
  val IOAnalog = ICGateDef("Analog IO", CircuitPartDefs.IOGate.id)
  val IOBundled = ICGateDef("Bundled IO", CircuitPartDefs.IOGate.id)

  val OR = ICGateDef("OR gate", CircuitPartDefs.SimpleGate.id, gd.OR)
  val NOR = ICGateDef("NOR gate", CircuitPartDefs.SimpleGate.id, gd.NOR)
  val NOT = ICGateDef("NOT gate", CircuitPartDefs.SimpleGate.id, gd.NOT)
  val AND = ICGateDef("AND gate", CircuitPartDefs.SimpleGate.id, gd.AND)
  val NAND = ICGateDef("NAND gate", CircuitPartDefs.SimpleGate.id, gd.NAND)
  val XOR = ICGateDef("XOR gate", CircuitPartDefs.SimpleGate.id, gd.XOR)
  val XNOR = ICGateDef("XNOR gate", CircuitPartDefs.SimpleGate.id, gd.XNOR)
  val Buffer =
    ICGateDef("Buffer gate", CircuitPartDefs.SimpleGate.id, gd.Buffer)
  val Multiplexer =
    ICGateDef("Multiplexer", CircuitPartDefs.SimpleGate.id, gd.Multiplexer)
  val Pulse = ICGateDef("Pulse Former", CircuitPartDefs.SimpleGate.id, gd.Pulse)
  val Repeater =
    ICGateDef("Repeater", CircuitPartDefs.SimpleGate.id, gd.Repeater)
  val Randomizer =
    ICGateDef("Randomizer", CircuitPartDefs.SimpleGate.id, gd.Randomizer)
  val SRLatch =
    ICGateDef("SR Latch", CircuitPartDefs.ComplexGate.id, gd.SRLatch)
  val ToggleLatch =
    ICGateDef("Toggle Latch", CircuitPartDefs.ComplexGate.id, gd.ToggleLatch)
  val TransparentLatch = ICGateDef(
    "Transparent Latch",
    CircuitPartDefs.SimpleGate.id,
    gd.TransparentLatch
  )
  val Timer = ICGateDef("Timer", CircuitPartDefs.ComplexGate.id, gd.Timer)
  val Sequencer =
    ICGateDef("Sequencer", CircuitPartDefs.ComplexGate.id, gd.Sequencer)
  val Counter = ICGateDef("Counter", CircuitPartDefs.ComplexGate.id, gd.Counter)
  val StateCell =
    ICGateDef("State Cell", CircuitPartDefs.ComplexGate.id, gd.StateCell)
  val Synchronizer =
    ICGateDef("Synchronizer", CircuitPartDefs.ComplexGate.id, gd.Synchronizer)
  val DecRandomizer =
    ICGateDef("Dec Randomizer", CircuitPartDefs.SimpleGate.id, gd.DecRandomizer)
  val NullCell =
    ICGateDef("Null Cell", CircuitPartDefs.ArrayGate.id, gd.NullCell)
  val InvertCell =
    ICGateDef("Invert Cell", CircuitPartDefs.ArrayGate.id, gd.InvertCell)
  val BufferCell =
    ICGateDef("Buffer Cell", CircuitPartDefs.ArrayGate.id, gd.BufferCell)

  case class ICGateDef(unlocal: String, gateType: Int, intDef: GateDef = null)
      extends Value {
    override def name = unlocal
  }
}
