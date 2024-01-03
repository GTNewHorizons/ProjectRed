package mrtjp.projectred.exploration

import codechicken.lib.lighting.LightModel
import codechicken.lib.math.MathHelper
import codechicken.lib.render._
import codechicken.lib.render.uv.{IconTransformation, UVTranslation}
import codechicken.lib.vec._
import mrtjp.core.block.TInstancedBlockRender
import mrtjp.core.color.Colors
import mrtjp.core.world.WorldLib
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.item.Item.ToolMaterial
import net.minecraft.item.ItemStack
import net.minecraft.util.{IIcon, ResourceLocation}
import net.minecraft.world.IBlockAccess
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRenderType, ItemRendererHelper}
import org.lwjgl.opengl.GL11

import scala.collection.JavaConversions._

object GemSawRenderer extends IItemRenderer {
  private val models = CCModel.parseObjModels(
    new ResourceLocation("microblock", "models/saw.obj"),
    7,
    new SwapYZ
  )
  private val handle = models.get("Handle")
  private val holder = models.get("BladeSupport")
  private val blade = models.get("Blade")

  override def handleRenderType(item: ItemStack, t: ItemRenderType) = true

  override def shouldUseRenderHelper(
      t: ItemRenderType,
      item: ItemStack,
      helper: ItemRendererHelper
  ) = true

  import mrtjp.core.color.Colors_old._
  import mrtjp.projectred.ProjectRedExploration.{toolMaterialPeridot, toolMaterialRuby, toolMaterialSapphire}
  private def colour(stack: ItemStack) =
    stack.getItem.asInstanceOf[ItemGemSaw].toolDef.mat match {
      case ToolMaterial.WOOD              => BROWN.rgba
      case ToolMaterial.STONE             => LIGHT_GREY.rgba
      case ToolMaterial.IRON              => WHITE.rgba
      case ToolMaterial.GOLD              => YELLOW.rgba
      case t if t == toolMaterialRuby     => RED.rgba
      case t if t == toolMaterialSapphire => BLUE.rgba
      case t if t == toolMaterialPeridot  => GREEN.rgba
      case ToolMaterial.EMERALD           => CYAN.rgba
      case _                              => BLACK.rgba
    }

  override def renderItem(
      rtype: ItemRenderType,
      item: ItemStack,
      data: AnyRef*
  ) {
    import codechicken.lib.vec.{TransformationList => TList}
    import net.minecraftforge.client.IItemRenderer.ItemRenderType._
    val t = rtype match {
      case INVENTORY =>
        new TList(
          new Scale(1.8),
          new Translation(0, 0, -0.6),
          new Rotation(-MathHelper.pi / 4, 1, 0, 0),
          new Rotation(MathHelper.pi * 3 / 4, 0, 1, 0)
        )
      case ENTITY =>
        new TList(
          new Scale(1),
          new Translation(0, 0, -0.25),
          new Rotation(-MathHelper.pi / 4, 1, 0, 0)
        )
      case EQUIPPED_FIRST_PERSON =>
        new TList(
          new Scale(1.5),
          new Rotation(-MathHelper.pi / 3, 1, 0, 0),
          new Rotation(MathHelper.pi * 3 / 4, 0, 1, 0),
          new Translation(0.5, 0.5, 0.5)
        )
      case EQUIPPED =>
        new TList(
          new Scale(1.5),
          new Rotation(-MathHelper.pi / 5, 1, 0, 0),
          new Rotation(-MathHelper.pi * 3 / 4, 0, 1, 0),
          new Translation(0.75, 0.5, 0.75)
        )
      case _ => return
    }

    val ccrsi = CCRenderState.instance
    ccrsi.reset()
    ccrsi.setDynamic()
    ccrsi.pullLightmap()
    CCRenderState.changeTexture("microblock:textures/items/saw.png")
    ccrsi.baseColour = 0xffffffff

    ccrsi.startDrawing()
    handle.render(t)
    holder.render(t)
    ccrsi.draw()

    if (rtype != ItemRenderType.EQUIPPED_FIRST_PERSON)
      GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glDisable(GL11.GL_CULL_FACE)

    ccrsi.startDrawing()
    ccrsi.baseColour = colour(item)
    blade.render(t, new UVTranslation(0, 4 / 64d))
    ccrsi.baseColour = 0xffffffff
    ccrsi.draw()

    GL11.glEnable(GL11.GL_CULL_FACE)
    if (rtype != EQUIPPED_FIRST_PERSON) GL11.glEnable(GL11.GL_LIGHTING)
  }
}

object RenderLily extends TInstancedBlockRender {
  val model = {
    val m = CCModel.quadModel(8)
    m.generateBlock(0, new Cuboid6(0, 0, 0.5, 1, 1, 0.5), ~0xc)
    val m2 = m.copy.apply(Rotation.quarterRotations(1).at(Vector3.center))
    val m3 = CCModel.combine(Seq(m, m2))
    m3.apply(new Rotation(45 * MathHelper.torad, 0, 1, 0).at(Vector3.center))
    m3.computeNormals()
    m3.shrinkUVs(0.0001)
    m3.computeLighting(LightModel.standardLightModel)
  }

  val icons = new Array[IIcon](8)
  var iconC: IIcon = null

  override def renderWorldBlock(
      r: RenderBlocks,
      w: IBlockAccess,
      x: Int,
      y: Int,
      z: Int,
      meta: Int
  ) = {
    val te = WorldLib.getTileEntity(w, x, y, z, classOf[TileLily])
    if (te != null) {
      val ccrsi = CCRenderState.instance
      ccrsi.reset()
      ccrsi.lightMatrix.locate(w, x, y, z)
      ccrsi.setBrightness(w, x, y, z)
      model.render(
        new Translation(x, y, z),
        new IconTransformation(icons(te.growth))
      )
      if (te.growth == 7)
        model.render(
          new Translation(x, y, z),
          new IconTransformation(iconC),
          new ColourMultiplier(Colors(te.meta).rgba)
        )
    }
  }

  override def getIcon(side: Int, meta: Int) = icons(7)

  override def renderInvBlock(r: RenderBlocks, meta: Int) {
    val ccrsi = CCRenderState.instance
    ccrsi.reset()
    ccrsi.setDynamic()
    ccrsi.pullLightmap()
    ccrsi.setNormal(0, -1, 0)
    ccrsi.startDrawing()
    r.drawCrossedSquares(icons(7), -0.5d, -0.95d, -0.5d, 1.65f)
    ccrsi.draw()
  }

  override def registerIcons(reg: IIconRegister) {
    for (i <- 0 until 8)
      icons(i) = reg.registerIcon("projectred:world/lily/lily_" + i)
    iconC = reg.registerIcon("projectred:world/lily/lily_colour")
  }
}
