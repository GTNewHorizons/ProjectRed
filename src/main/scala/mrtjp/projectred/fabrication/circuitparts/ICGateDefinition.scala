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

  val IOSimple =
    ICGateDef("gui.projectred.fabrication.io_simple", CircuitPartDefs.IOGate.id)
  val IOAnalog =
    ICGateDef("gui.projectred.fabrication.io_analog", CircuitPartDefs.IOGate.id)
  val IOBundled = ICGateDef(
    "gui.projectred.fabrication.io_bundled",
    CircuitPartDefs.IOGate.id
  )

  val OR = ICGateDef(
    "item.projectred.integration.gate|0.name",
    CircuitPartDefs.SimpleGate.id,
    gd.OR
  )
  val NOR = ICGateDef(
    "item.projectred.integration.gate|1.name",
    CircuitPartDefs.SimpleGate.id,
    gd.NOR
  )
  val NOT = ICGateDef(
    "item.projectred.integration.gate|2.name",
    CircuitPartDefs.SimpleGate.id,
    gd.NOT
  )
  val AND = ICGateDef(
    "item.projectred.integration.gate|3.name",
    CircuitPartDefs.SimpleGate.id,
    gd.AND
  )
  val NAND = ICGateDef(
    "item.projectred.integration.gate|4.name",
    CircuitPartDefs.SimpleGate.id,
    gd.NAND
  )
  val XOR = ICGateDef(
    "item.projectred.integration.gate|5.name",
    CircuitPartDefs.SimpleGate.id,
    gd.XOR
  )
  val XNOR = ICGateDef(
    "item.projectred.integration.gate|6.name",
    CircuitPartDefs.SimpleGate.id,
    gd.XNOR
  )
  val Buffer =
    ICGateDef(
      "item.projectred.integration.gate|7.name",
      CircuitPartDefs.SimpleGate.id,
      gd.Buffer
    )
  val Multiplexer =
    ICGateDef(
      "item.projectred.integration.gate|8.name",
      CircuitPartDefs.SimpleGate.id,
      gd.Multiplexer
    )
  val Pulse = ICGateDef(
    "item.projectred.integration.gate|9.name",
    CircuitPartDefs.SimpleGate.id,
    gd.Pulse
  )
  val Repeater =
    ICGateDef(
      "item.projectred.integration.gate|10.name",
      CircuitPartDefs.SimpleGate.id,
      gd.Repeater
    )
  val Randomizer =
    ICGateDef(
      "item.projectred.integration.gate|11.name",
      CircuitPartDefs.SimpleGate.id,
      gd.Randomizer
    )
  val SRLatch =
    ICGateDef(
      "item.projectred.integration.gate|12.name",
      CircuitPartDefs.ComplexGate.id,
      gd.SRLatch
    )
  val ToggleLatch =
    ICGateDef(
      "item.projectred.integration.gate|13.name",
      CircuitPartDefs.ComplexGate.id,
      gd.ToggleLatch
    )
  val TransparentLatch = ICGateDef(
    "item.projectred.integration.gate|14.name",
    CircuitPartDefs.SimpleGate.id,
    gd.TransparentLatch
  )
  val Timer = ICGateDef(
    "item.projectred.integration.gate|17.name",
    CircuitPartDefs.ComplexGate.id,
    gd.Timer
  )
  val Sequencer =
    ICGateDef(
      "item.projectred.integration.gate|18.name",
      CircuitPartDefs.ComplexGate.id,
      gd.Sequencer
    )
  val Counter = ICGateDef(
    "item.projectred.integration.gate|19.name",
    CircuitPartDefs.ComplexGate.id,
    gd.Counter
  )
  val StateCell =
    ICGateDef(
      "item.projectred.integration.gate|20.name",
      CircuitPartDefs.ComplexGate.id,
      gd.StateCell
    )
  val Synchronizer =
    ICGateDef(
      "item.projectred.integration.gate|21.name",
      CircuitPartDefs.ComplexGate.id,
      gd.Synchronizer
    )
  val DecRandomizer =
    ICGateDef(
      "item.projectred.integration.gate|33.name",
      CircuitPartDefs.SimpleGate.id,
      gd.DecRandomizer
    )
  val NullCell =
    ICGateDef(
      "item.projectred.integration.gate|23.name",
      CircuitPartDefs.ArrayGate.id,
      gd.NullCell
    )
  val InvertCell =
    ICGateDef(
      "item.projectred.integration.gate|24.name",
      CircuitPartDefs.ArrayGate.id,
      gd.InvertCell
    )
  val BufferCell =
    ICGateDef(
      "item.projectred.integration.gate|25.name",
      CircuitPartDefs.ArrayGate.id,
      gd.BufferCell
    )

  case class ICGateDef(
      unlocalized: String,
      gateType: Int,
      intDef: GateDef = null
  ) extends Value {
    override def name = unlocalized
  }
}
