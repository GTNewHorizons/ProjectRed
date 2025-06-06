/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication

import codechicken.lib.vec._
import mrtjp.projectred.fabrication.gui.PrefboardRenderer
import net.minecraft.client.renderer.texture.IIconRegister
import org.lwjgl.opengl.GL11._


object RenderCircuit {
  def registerIcons(reg: IIconRegister) {
    ICComponentStore.registerIcons(reg)
  }

  def renderOrtho(
      circuit: IntegratedCircuit,
      x: Double,
      y: Double,
      xSize: Double,
      ySize: Double,
      frame: Float
  ) {
    val t =
      ICComponentStore.orthoGridT(xSize, ySize) `with` new Translation(x, y, 0)
    renderBoard(circuit, t, true)
    renderCircuit(circuit, t, true, frame)
  }

  def renderDynamic(
      circuit: IntegratedCircuit,
      t: Transformation,
      frame: Float
  ) {
    glDisable(GL_DEPTH_TEST)
    renderBoard(circuit, t, true)
    renderCircuit(circuit, t, true, frame)
    glEnable(GL_DEPTH_TEST)
  }

  def renderBoard(
      circuit: IntegratedCircuit,
      t: Transformation,
      ortho: Boolean
  ) {
    PrefboardRenderer.render(circuit, t, ortho)
  }

  def renderCircuit(
      circuit: IntegratedCircuit,
      t: Transformation,
      ortho: Boolean,
      frame: Float
  ) {
    for (((x, y), part) <- circuit.parts) {
      val tlist = new TransformationList(
        new Scale(1.0 / circuit.size.width, 1, 1.0 / circuit.size.height),
        new Translation(
          x * 1.0 / circuit.size.width,
          0,
          y * 1.0 / circuit.size.height
        ),
        t
      )
      part.renderDynamic(tlist, ortho, frame)
    }
  }
}
