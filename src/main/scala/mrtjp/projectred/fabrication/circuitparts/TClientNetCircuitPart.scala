/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import cpw.mods.fml.relauncher.{Side, SideOnly}

trait TClientNetCircuitPart extends CircuitPart {
  def readClientPacket(in: MCDataInput)

  @SideOnly(Side.CLIENT)
  def sendClientPacket(writer: MCDataOutput => Unit = { _ => }) {
    world.sendClientPacket(this, writer)
  }
}
