/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication

import codechicken.lib.render.ColourMultiplier
import codechicken.lib.render.uv.{UVScale, UVTranslation}
import codechicken.lib.vec._
import mrtjp.core.color.Colors
import mrtjp.core.vec.{Point, Size, Vec2}
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.fabrication.ICComponentStore.{
  dynamicIdx,
  faceModels,
  finishRender,
  orthoPartT,
  prepairRender
}
import mrtjp.projectred.fabrication.gui.PrefboardRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister

object RenderCircuit {
  val BASE_SCALE = 16

  def registerIcons(reg: IIconRegister) {
    ICComponentStore.registerIcons(reg)
  }

  def renderOrtho(
      circuit: IntegratedCircuit,
      boardSize: Size,
      gridScale: Double,
      gridTranslation: Vec2
  ) {
    PrefboardRenderer.renderOrtho(boardSize, gridScale, gridTranslation)
    renderCircuitOrtho(circuit, gridScale, gridTranslation)
  }

  def renderErrors(
      circuit: IntegratedCircuit,
      gridScale: Double,
      gridTranslation: Vec2
  ): Unit = {
    if (
      Minecraft.getMinecraft.theWorld.getTotalWorldTime % 100 > 5 && circuit.errors.nonEmpty
    ) {
      prepairRender()
      PRResources.guiPrototyper.bind()
      for ((Point(x, y), (_, c)) <- circuit.errors) {
        val t = orthoPartT(Vec2(x, y) - gridTranslation, gridScale)
        faceModels(dynamicIdx(0, true)).render(
          t,
          new UVScale(64) `with` new UVTranslation(
            330,
            37
          ) `with` new UVScale(1 / 512d),
          ColourMultiplier.instance(Colors(c).rgba)
        )
      }
      finishRender()
    }
  }

  def renderCircuitOrtho(
      circuit: IntegratedCircuit,
      scale: Double,
      gridTranslation: Vec2
  ): Unit = {
    val t = ICComponentStore.orthoGridT(BASE_SCALE, BASE_SCALE)
    for (((x, y), part) <- circuit.parts) {
      val tlist = new TransformationList(
        new Scale(scale, 1, scale),
        new Translation(
          (x - gridTranslation.dx) * scale,
          0,
          (y - gridTranslation.dy) * scale
        ),
        t
      )
      part.renderDynamic(tlist, true, 1f)
    }
  }
}
