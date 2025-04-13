package mrtjp.projectred.transmission

import net.minecraftforge.client.IItemRenderer
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer.{
  ItemRendererHelper,
  ItemRenderType
}
import codechicken.lib.render.{ColourMultiplier, CCRenderState, TextureUtils}
import codechicken.lib.vec.{Vector3, Transformation, Translation, Scale}
import net.minecraft.util.IIcon
import codechicken.lib.render.CCRenderState.IVertexOperation
import codechicken.lib.render.uv.IconTransformation

trait TWireItemRenderCommon extends IItemRenderer {
  def handleRenderType(item: ItemStack, rtype: ItemRenderType) = true

  def shouldUseRenderHelper(
      rtype: ItemRenderType,
      item: ItemStack,
      helper: ItemRendererHelper
  ) = true

  def renderWireInventory(
      meta: Int,
      x: Float,
      y: Float,
      z: Float,
      scale: Float
  ) {
    val wdef = WireDef.values(meta)
    if (wdef == null) return
    TextureUtils.bindAtlas(0)
    val state = CCRenderState.instance
    state.resetInstance()
    state.setDynamicInstance()
    state.pullLightmapInstance()
    state.startDrawingInstance()

    doRender(
      wdef.thickness,
      wdef.itemColour << 8 | 0xff,
      new Scale(scale).at(Vector3.center).`with`(new Translation(x, y, z)),
      new IconTransformation(wdef.wireSprites(0))
    )

    state.drawInstance()
  }

  def doRender(thickness: Int, renderHue: Int, ops: IVertexOperation*)
}

object WireItemRenderer extends TWireItemRenderCommon {
  override def renderItem(
      rtype: ItemRenderType,
      item: ItemStack,
      data: AnyRef*
  ) {
    val damage = item.getItemDamage
    import ItemRenderType._
    rtype match {
      case ENTITY   => renderWireInventory(damage, -0.5f, 0f, -0.5f, 0.6f)
      case EQUIPPED => renderWireInventory(damage, 0f, 0.45f, 0f, 1f)
      case EQUIPPED_FIRST_PERSON =>
        renderWireInventory(damage, 0.0f, 0.5f, 0.0f, 1f)
      case INVENTORY => renderWireInventory(damage, -0.5f, -0.2f, -0.5f, 1f)
      case _         =>
    }
  }

  override def doRender(
      thickness: Int,
      renderHue: Int,
      ops: IVertexOperation*
  ) {
    RenderWire.renderInventoryModel(thickness, renderHue, ops: _*)
  }
}

object FramedWireItemRenderer extends TWireItemRenderCommon {
  override def renderItem(
      rtype: ItemRenderType,
      item: ItemStack,
      data: AnyRef*
  ) {
    val damage = item.getItemDamage
    import ItemRenderType._
    rtype match {
      case ENTITY   => renderWireInventory(damage, -0.5f, 0f, -0.5f, 1f)
      case EQUIPPED => renderWireInventory(damage, 0f, 0f, 0f, 1f)
      case EQUIPPED_FIRST_PERSON => renderWireInventory(damage, 0f, 0f, 0f, 1f)
      case INVENTORY => renderWireInventory(damage, -0.5f, -0.5f, -0.5f, 1f)
      case _         =>
    }
  }

  override def doRender(
      thickness: Int,
      renderHue: Int,
      ops: IVertexOperation*
  ) {
    RenderFramedWire.renderInv(thickness, renderHue, ops: _*)
  }
}
