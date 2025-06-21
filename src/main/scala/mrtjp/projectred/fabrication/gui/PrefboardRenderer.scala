package mrtjp.projectred.fabrication.gui

import codechicken.lib.math.MathHelper
import codechicken.lib.render.CCRenderState
import codechicken.lib.render.uv.UVScale
import codechicken.lib.vec.{Rotation, Scale, TransformationList, Translation}
import mrtjp.core.vec.{Size, Vec2}
import mrtjp.projectred.fabrication.ICComponentStore.faceModels
import mrtjp.projectred.fabrication.RenderCircuit
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

object PrefboardRenderer {

  def renderOrtho(size: Size, scale: Double, gridTranslation: Vec2) {
    val offset =
      gridTranslation - Vec2(gridTranslation.dx.toInt, gridTranslation.dy.toInt)

    val uv = new UVScale(
      size.width / (RenderCircuit.BASE_SCALE * scale) + 2,
      size.height / (RenderCircuit.BASE_SCALE * scale) + 2
    )
    val boardModel = faceModels.map(_.copy().apply(uv))

    val state = CCRenderState.instance
    state.resetInstance()
    state.pullLightmapInstance()
    state.setDynamicInstance()

    val t = new TransformationList(
      new Scale(
        size.width + 2 * RenderCircuit.BASE_SCALE * scale,
        1,
        -(size.height + 2 * RenderCircuit.BASE_SCALE * scale)
      ),
      new Translation(
        -(offset.dx + 1) * RenderCircuit.BASE_SCALE * scale,
        0,
        (offset.dy + 1) * RenderCircuit.BASE_SCALE * scale
      ),
      new Rotation(0.5 * MathHelper.pi, 1, 0, 0)
    )

    val r = new ResourceLocation(
      "projectred",
      "textures/blocks/fabrication/prefboard.png"
    )
    Minecraft.getMinecraft.getTextureManager.bindTexture(r)

    state.startDrawingInstance()
    boardModel(1).render(t)
    state.drawInstance()
  }
}
