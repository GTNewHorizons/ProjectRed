package mrtjp.projectred.illumination

import codechicken.lib.render._
import codechicken.lib.render.uv.IconTransformation
import codechicken.lib.vec._
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import mrtjp.core.color.{Colors, Colors_old}
import mrtjp.projectred.core.{RenderHalo, Configurator}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{
  ItemRenderType,
  ItemRendererHelper
}
import net.minecraftforge.client.event.RenderWorldLastEvent
import org.lwjgl.opengl.GL11

object LampTESR extends TileEntitySpecialRenderer with IItemRenderer {
  override def handleRenderType(item: ItemStack, `type`: ItemRenderType) = true
  override def shouldUseRenderHelper(
      `type`: ItemRenderType,
      item: ItemStack,
      helper: ItemRendererHelper
  ) = true

  override def renderItem(t: ItemRenderType, item: ItemStack, data: AnyRef*) {
    import net.minecraftforge.client.IItemRenderer.ItemRenderType._
    t match {
      case ENTITY                => render(-0.15, 0, -0.15, 1)
      case EQUIPPED              => render(0, 0, 0, 1)
      case EQUIPPED_FIRST_PERSON => render(0, 0, 0, 1)
      case INVENTORY             => render(0, -0.05, 0, 0.95)
      case _                     =>
    }

    def render(x: Double, y: Double, z: Double, s: Double) {
      val meta = item.getItemDamage
      val icon = new IconTransformation(
        if (meta > 15) BlockLamp.on(meta % 16) else BlockLamp.off(meta)
      )

      GL11.glPushMatrix()
      GL11.glTranslated(x, y, z)
      GL11.glScaled(s, s, s)
      TextureUtils.bindAtlas(0)
      val state = CCRenderState.instance
      state.resetInstance()
      state.setDynamicInstance()
      state.pullLightmapInstance()
      state.startDrawingInstance()

      val t = new Translation(x, y, z)
      state.setPipelineInstance(t, icon)
      BlockRenderer.renderCuboid(Cuboid6.full, 0)
      state.drawInstance()

      if (meta > 15) {
        RenderHalo.prepareRenderState()
        RenderHalo.renderHalo(lBounds, meta % 16, t)
        RenderHalo.restoreRenderState()
      }

      GL11.glPopMatrix()
    }
  }

  private val lBounds = Cuboid6.full.copy.expand(0.05d)
  override def renderTileEntityAt(
      te: TileEntity,
      x: Double,
      y: Double,
      z: Double,
      partials: Float
  ) {
    te match {
      case light: ILight if light.isOn =>
        val meta =
          te.getWorldObj.getBlockMetadata(te.xCoord, te.yCoord, te.zCoord) % 16
        RenderHalo.addLight(te.xCoord, te.yCoord, te.zCoord, meta, lBounds)
      case _ =>
    }
  }
}

trait ButtonRenderCommons extends IItemRenderer {
  val invRenderBox =
    new Cuboid6(0.0, 0.375, 0.5 - 0.1875, 0.25, 0.625, 0.5 + 0.1875)
  val invLightBox = invRenderBox.copy.expand(0.025d)

  override def handleRenderType(item: ItemStack, t: ItemRenderType) = true
  override def shouldUseRenderHelper(
      t: ItemRenderType,
      item: ItemStack,
      helper: ItemRendererHelper
  ) = true

  override def renderItem(t: ItemRenderType, item: ItemStack, data: AnyRef*) {
    import net.minecraftforge.client.IItemRenderer.ItemRenderType._
    t match {
      case ENTITY                => render(-0.05, 0, -0.1, 0.5)
      case EQUIPPED              => render(0.2, 0, 0, 1)
      case EQUIPPED_FIRST_PERSON => render(0.2, 0, 0, 1)
      case INVENTORY             => render(0.25, 0, 0, 1)
      case _                     =>
    }

    def render(x: Double, y: Double, z: Double, s: Double) {
      val color = item.getItemDamage
      if (0 until 16 contains color) {
        val icon = new IconTransformation(ItemPartButton.icon)
        val t = new Scale(s) `with` new Translation(x, y, z)

        GL11.glPushMatrix()

        TextureUtils.bindAtlas(0)
        val state = CCRenderState.instance
        state.resetInstance()
        state.setDynamicInstance()
        state.pullLightmapInstance()
        state.startDrawingInstance()

        state.setPipelineInstance(
          t,
          icon,
          new ColourMultiplier(Colors(color).rgba)
        )
        BlockRenderer.renderCuboid(invRenderBox, 0)
        drawExtras(t)

        state.drawInstance()
        RenderHalo.prepareRenderState()
        RenderHalo.renderHalo(invLightBox, color, t)
        RenderHalo.restoreRenderState()
        GL11.glPopMatrix()
      }
    }
  }

  def drawExtras(t: Transformation) {}
}

object RenderButton extends ButtonRenderCommons

object RenderFButton extends ButtonRenderCommons {
  val model = genModel(16, 2, 8)
  override def drawExtras(t: Transformation) {
    model.render(t, new IconTransformation(Blocks.redstone_torch.getIcon(0, 0)))
  }

  private def genModel(height: Int, x: Double, z: Double): CCModel = {
    val m = CCModel.quadModel(20)
    m.verts(0) = new Vertex5(7 / 16d, 10 / 16d, 9 / 16d, 7 / 16d, 8 / 16d)
    m.verts(1) = new Vertex5(9 / 16d, 10 / 16d, 9 / 16d, 9 / 16d, 8 / 16d)
    m.verts(2) = new Vertex5(9 / 16d, 10 / 16d, 7 / 16d, 9 / 16d, 6 / 16d)
    m.verts(3) = new Vertex5(7 / 16d, 10 / 16d, 7 / 16d, 7 / 16d, 6 / 16d)
    m.generateBlock(
      4,
      6 / 16d,
      (10 - height) / 16d,
      7 / 16d,
      10 / 16d,
      11 / 16d,
      9 / 16d,
      0x33
    )
    m.generateBlock(
      12,
      7 / 16d,
      (10 - height) / 16d,
      6 / 16d,
      9 / 16d,
      11 / 16d,
      10 / 16d,
      0xf
    )
    m.apply(new Translation(-0.5 + x / 16, (height - 10) / 16d, -0.5 + z / 16))
    m.computeNormals
    m.shrinkUVs(0.0005)
    m.apply(new Scale(1.0005))
    m
  }
}
