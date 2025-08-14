/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.vec.Translation
import mrtjp.core.gui.TNode
import mrtjp.core.vec.Point
import mrtjp.projectred.fabrication.ICComponentStore
import mrtjp.projectred.fabrication.circuitparts.{
  GateICPart,
  ICGateRenderer,
  TClientNetCircuitPart
}
import net.minecraft.util.StatCollector

trait TConfigurable extends TClientNetCircuitPart {
  def createConfigurationNode: ConfigurationNode
}

abstract class ConfigurationNode(gate: GateICPart) extends TNode {
  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    val pos = position.add(8, 0)
    ICGateRenderer.renderDynamic(
      gate,
      ICComponentStore.orthoGridT(50, 50) `with`
        new Translation(
          pos.x,
          pos.y,
          0
        ),
      true,
      rframe
    )
  }

  protected def translate(unlocalizedName: String): String = {
    StatCollector.translateToLocal(unlocalizedName)
  }
}
