package mrtjp.projectred.transportation

import codechicken.lib.lighting.LightModel
import codechicken.lib.raytracer.ExtendedMOP
import codechicken.lib.render.CCRenderState.IVertexOperation
import codechicken.lib.render._
import codechicken.lib.render.uv.{IconTransformation, UV, UVScale, UVTransformation}
import codechicken.lib.vec._
import codechicken.microblock.MicroMaterialRegistry.IMicroHighlightRenderer
import codechicken.microblock.{BlockMicroMaterial, CommonMicroClass, MicroMaterialRegistry}
import mrtjp.core.color.Colors
import mrtjp.projectred.core.libmc.PRLib
import net.minecraft.client.renderer.entity.{RenderItem, RenderManager}
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.{IIcon, MovingObjectPosition}
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRenderType, ItemRendererHelper}
import org.lwjgl.opengl.GL11

object RenderPipe {
  private final val dummyEntityItem = new EntityItem(null)
  private final val customRenderItem = new RenderItem {
    override def shouldBob = false
    override def shouldSpreadItems = false
  }
  customRenderItem.setRenderManager(RenderManager.instance)

  var sideModels: Array[CCModel] = null
  var centerModels: Array[CCModel] = null

  var sideModelsRS: Array[CCModel] = null
  var centerModelsRS: Array[CCModel] = null

  var sideModelsWool: Array[CCModel] = null
  var centerModelsWool: Array[CCModel] = null

  {
    val gen = new PipeModelGenerator
    sideModels = gen.sideModels
    centerModels = gen.centerModels

    val rsgen = new PipeModelGenerator
    rsgen.applyScale(0.90)
    sideModelsRS = rsgen.sideModels
    centerModelsRS = rsgen.centerModels

    val woolgen = new PipeModelGenerator
    woolgen.applyScale(0.80)
    sideModelsWool = woolgen.sideModels
    centerModelsWool = woolgen.centerModels
  }

  def renderPipe(p: SubcorePipePart, pos: Vector3) {
    val t = pos.translation()
    var uvt = new IconTransformation(p.getPipeType.sprites(0))
    val connMap = p.connMap & 0x3f

    if (connMap == 0x3 || connMap == 0xc || connMap == 0x30)
      for (a <- 0 until 3) {
        if ((connMap >> a * 2) == 3) centerModels(a).render(t, uvt)
      }
    else centerModels(3).render(t, uvt)

    for (s <- 0 until 6) if ((connMap & 1 << s) != 0) {
      uvt = new IconTransformation(p.getIcon(s))
      sideModels(s).render(t, uvt)
    }

    val gen = new PipeModelGenerator
    sideModels = gen.sideModels
    centerModels = gen.centerModels

    val rsgen = new PipeModelGenerator
    rsgen.applyScale(0.90)
    sideModelsRS = rsgen.sideModels
    centerModelsRS = rsgen.centerModels

    val woolgen = new PipeModelGenerator
    woolgen.applyScale(0.85)
    sideModelsWool = woolgen.sideModels
    centerModelsWool = woolgen.centerModels

  }

  def renderRSWiring(p: TRedstonePipe, pos: Vector3, signal: Byte) {
    val t = pos.translation()
    val colour =
      ColourMultiplier.instance((signal & 0xff) / 2 + 60 << 24 | 0xff)
    val uvt2 = new IconTransformation(PipeDefs.BASIC.sprites(1))
    val connMap = p.connMap & 0x3f

    if (connMap == 0x3 || connMap == 0xc || connMap == 0x30)
      for (a <- 0 until 3) {
        if ((connMap >> a * 2) == 3) centerModelsRS(a).render(t, uvt2, colour)
      }
    else centerModelsRS(3).render(t, uvt2, colour)

    for (s <- 0 until 6)
      if ((connMap & 1 << s) != 0)
        sideModelsRS(s).render(t, uvt2, colour)
  }

  def renderColourWool(p: TColourFilterPipe, pos: Vector3, colour: Byte) {
    val t = pos.translation()
    val uvt2 = new IconTransformation(PipeDefs.PRESSURETUBE.sprites(1 + colour))
    val connMap = p.connMap & 0x3f

    if (connMap == 0x3 || connMap == 0xc || connMap == 0x30)
      for (a <- 0 until 3) {
        if ((connMap >> a * 2) == 3) centerModelsWool(a).render(t, uvt2)
      }
    else centerModelsWool(3).render(t, uvt2)

    for (s <- 0 until 6)
      if ((connMap & 1 << s) != 0)
        sideModelsWool(s).render(t, uvt2)
  }

  def renderBreakingOverlay(icon: IIcon, pipe: SubcorePipePart) {
    CCRenderState.instance.setPipeline(
      new Translation(pipe.x, pipe.y, pipe.z),
      new IconTransformation(icon)
    )
    import scala.collection.JavaConversions._
    for (box <- pipe.getCollisionBoxes)
      BlockRenderer.renderCuboid(box, 0)
  }

  def renderInv(ops: IVertexOperation*) {
    centerModels(3).render(ops: _*)
    for (s <- 0 to 1) sideModels(s).render(ops: _*)
  }

  def renderItemFlow[T <: AbstractPipePayload](
      p: PayloadPipePart[T],
      pos: Vector3,
      frame: Float
  ) {
    GL11.glPushMatrix()
    GL11.glDisable(GL11.GL_LIGHTING)
    for (r <- p.itemFlow.delegate) if (!p.itemFlow.outputQueue.contains(r)) {
      val dir = if (r.isEntering) r.input else r.output
      val prog = r.progress + (r.speed * frame)

      var frameX = pos.x + 0.5d
      var frameY = pos.y + 0.25d
      var frameZ = pos.z + 0.5d
      dir match {
        case 0 => frameY = (pos.y - 0.25d) + (1.0d - prog)
        case 1 => frameY = (pos.y - 0.25d) + prog
        case 2 => frameZ = pos.z + (1.0d - prog)
        case 3 => frameZ = pos.z + prog
        case 4 => frameX = pos.x + (1.0d - prog)
        case 5 => frameX = pos.x + prog
        case _ =>
      }
      doRenderItem(r, frameX, frameY, frameZ)
      r match {
        case net: NetworkPayload =>
          renderPayloadColour(net.netPriority.color, frameX, frameY, frameZ)
        case pa: PressurePayload if pa.colour > -1 =>
          renderPayloadColour(pa.colour, frameX, frameY, frameZ)
        case _ =>
      }
    }
    GL11.glEnable(GL11.GL_LIGHTING)
    GL11.glPopMatrix()
  }

  private def doRenderItem(
      r: AbstractPipePayload,
      x: Double,
      y: Double,
      z: Double
  ) {
    if (r == null || r.getItemStack == null) return
    val renderScale = 0.7f
    val itemstack = r.getItemStack

    GL11.glPushMatrix()
    GL11.glTranslatef(
      x.asInstanceOf[Float],
      y.asInstanceOf[Float],
      z.asInstanceOf[Float]
    )
    GL11.glTranslatef(0, 0.125f, 0)
    GL11.glScalef(renderScale, renderScale, renderScale)

    dummyEntityItem.setEntityItemStack(itemstack)
    customRenderItem.doRender(dummyEntityItem, 0, 0, 0, 0, 0)

    GL11.glPopMatrix()
  }

  private def renderPayloadColour(
      colour: Int,
      x: Double,
      y: Double,
      z: Double
  ) {
    GL11.glPushMatrix()
    prepareRenderState()
    GL11.glEnable(GL11.GL_LIGHTING)

    val t = new Vector3(x, y, z).add(-4 / 16d, 0, -4 / 16d)

    val ccrsi = CCRenderState.instance
    ccrsi.setPipeline(new Translation(t))
    ccrsi.alphaOverride = 32
    ccrsi.baseColour = Colors(colour).rgba
    BlockRenderer.renderCuboid(
      new Cuboid6(1 / 16d, 1 / 16d, 1 / 16d, 7 / 16d, 7 / 16d, 7 / 16d),
      0
    )

    restoreRenderState()
    GL11.glPopMatrix()
  }

  private def prepareRenderState() {
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glDisable(GL11.GL_LIGHTING)
    GL11.glDisable(GL11.GL_CULL_FACE)
    GL11.glDepthMask(false)
    val ccrsi = CCRenderState.instance
    ccrsi.startDrawing()
    ccrsi.pullLightmap()
    ccrsi.setDynamic()
  }

  private def restoreRenderState() {
    CCRenderState.instance.draw()
    GL11.glDepthMask(true)
    GL11.glColor3f(1, 1, 1)
    GL11.glEnable(GL11.GL_CULL_FACE)
    GL11.glEnable(GL11.GL_LIGHTING)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glDisable(GL11.GL_BLEND)
  }

  private def renderMicroHighlight(part: SubcorePipePart, tFunc: () => _) {
    val pos = new BlockCoord(part.tile)
    GL11.glPushMatrix()
    GL11.glTranslated(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    GL11.glScaled(1.002, 1.002, 1.002)
    GL11.glTranslated(-0.5, -0.5, -0.5)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glDepthMask(false)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    val ccrsi = CCRenderState.instance
    ccrsi.reset()
    TextureUtils.bindAtlas(0)
    ccrsi.setDynamic()
    ccrsi.setBrightness(part.world, pos.x, pos.y, pos.z)
    ccrsi.alphaOverride = 127
    ccrsi.startDrawing()

    tFunc()

    ccrsi.draw()
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glDepthMask(true)
    GL11.glPopMatrix()
  }

  def renderRSMicroHighlight(part: TRedstonePipe) {
    renderMicroHighlight(
      part,
      { () => renderRSWiring(part, Vector3.zero, 255.toByte) }
    )
  }

  def renderWoolMicroHighlight(part: TColourFilterPipe, colour: Byte) {
    renderMicroHighlight(
      part,
      { () => renderColourWool(part, Vector3.zero, colour) }
    )
  }
}

private class PipeModelGenerator(
    val w: Double = 2 / 8d,
    val d: Double = 1 / 16d - 0.002
) {
  var sideModels = new Array[CCModel](6)
  var centerModels = new Array[CCModel](4)
  generateModels()

  def generateModels() {
    generateCenterModel()
    generateCrossExclusiveModels()
    generateSideModels()
    finishModels()
  }

  def generateCenterModel() {
    val model = CCModel.quadModel(48)
    model.verts(0) = new Vertex5(0.5 - w, 0.5 - w, 0.5 - w, 4, 8)
    model.verts(1) = new Vertex5(0.5 + w, 0.5 - w, 0.5 - w, 12, 8)
    model.verts(2) = new Vertex5(0.5 + w, 0.5 - w, 0.5 + w, 12, 0)
    model.verts(3) = new Vertex5(0.5 - w, 0.5 - w, 0.5 + w, 4, 0)
    model.verts(4) = new Vertex5(0.5 - w, 0.5 - w + d, 0.5 + w, 4, 8)
    model.verts(5) = new Vertex5(0.5 + w, 0.5 - w + d, 0.5 + w, 12, 8)
    model.verts(6) = new Vertex5(0.5 + w, 0.5 - w + d, 0.5 - w, 12, 0)
    model.verts(7) = new Vertex5(0.5 - w, 0.5 - w + d, 0.5 - w, 4, 0)
    model.generateSidedParts(0, Vector3.center)
    centerModels(3) = model
  }

  def generateCrossExclusiveModels() {
    val model = CCModel.quadModel(32)
    model.verts(0) = new Vertex5(0.5 - w, 0.5 - w, 0.5 - w, 0, 16)
    model.verts(1) = new Vertex5(0.5 + w, 0.5 - w, 0.5 - w, 8, 16)
    model.verts(2) = new Vertex5(0.5 + w, 0.5 - w, 0.5 + w, 8, 8)
    model.verts(3) = new Vertex5(0.5 - w, 0.5 - w, 0.5 + w, 0, 8)
    model.verts(4) = new Vertex5(0.5 - w, 0.5 - w + d, 0.5 + w, 0, 16)
    model.verts(5) = new Vertex5(0.5 + w, 0.5 - w + d, 0.5 + w, 8, 16)
    model.verts(6) = new Vertex5(0.5 + w, 0.5 - w + d, 0.5 - w, 8, 8)
    model.verts(7) = new Vertex5(0.5 - w, 0.5 - w + d, 0.5 - w, 0, 8)

    for (s <- 1 until 4)
      model.generateSidedPart(0, s, Vector3.center, 0, 8 * s, 8)

    centerModels(0) =
      model.copy.apply(Rotation.sideOrientation(2, 1).at(Vector3.center))
    centerModels(1) =
      model.copy.apply(Rotation.sideOrientation(0, 1).at(Vector3.center))
    centerModels(2) = model
  }

  def generateSideModels() {
    val model = CCModel.quadModel(36)
    model.verts(0) = new Vertex5(0.5 - w, 0, 0.5 + w, 0, 0)
    model.verts(1) = new Vertex5(0.5 + w, 0, 0.5 + w, 0, 8)
    model.verts(2) = new Vertex5(0.5 + w, 0.5 - w, 0.5 + w, 4, 8)
    model.verts(3) = new Vertex5(0.5 - w, 0.5 - w, 0.5 + w, 4, 0)
    model.verts(4) = new Vertex5(0.5 + w, 0, 0.5 + w - d, 0, 0)
    model.verts(5) = new Vertex5(0.5 - w, 0, 0.5 + w - d, 0, 8)
    model.verts(6) = new Vertex5(0.5 - w, 0.5 - w, 0.5 + w - d, 4, 8)
    model.verts(7) = new Vertex5(0.5 + w, 0.5 - w, 0.5 + w - d, 4, 0)
    for (r <- 1 until 4)
      model.apply(Rotation.quarterRotations(r).at(Vector3.center), 0, r * 8, 8)

    model.verts(32) = new Vertex5(0.5 - w, 0, 0.5 - w, 8, 16)
    model.verts(33) = new Vertex5(0.5 + w, 0, 0.5 - w, 16, 16)
    model.verts(34) = new Vertex5(0.5 + w, 0, 0.5 + w, 16, 8)
    model.verts(35) = new Vertex5(0.5 - w, 0, 0.5 + w, 8, 8)
    sideModels(0) = model

    for (s <- 1 until 6) {
      sideModels(s) =
        model.copy.apply(Rotation.sideRotations(s).at(Vector3.center))
      if (s % 2 == 1) {
        val verts = sideModels(s).verts
        val t = new UVT(Rotation.quarterRotations(2).at(new Vector3(8, 0, 4)))
        for (i <- 0 until 32) verts(i).apply(t)
      }
    }
  }

  def finishModels() {
    for (m <- centerModels ++ sideModels) {
      m.apply(new UVScale(1 / 16d))
      m.shrinkUVs(0.0005)
      m.computeNormals
      m.computeLighting(LightModel.standardLightModel)
    }
  }

  def applyScale(scale: Double) {
    val nscale = 2.0 - scale - (1.0 - scale) * 0.001
    val tscale = (1 - scale) / 2d
    val trans = Seq(
      new Translation(0, tscale, 0),
      new Translation(0, -tscale, 0),
      new Translation(0, 0, tscale),
      new Translation(0, 0, -tscale),
      new Translation(tscale, 0, 0),
      new Translation(-tscale, 0, 0)
    )

    for (s <- Seq(0, 1))
      sideModels(s)
        .apply(new Scale(scale, nscale, scale).at(Vector3.center))
        .apply(trans(s))

    for (s <- Seq(2, 3))
      sideModels(s)
        .apply(new Scale(scale, scale, nscale).at(Vector3.center))
        .apply(trans(s))

    for (s <- Seq(4, 5))
      sideModels(s)
        .apply(new Scale(nscale, scale, scale).at(Vector3.center))
        .apply(trans(s))

    val cscale = scale
    for (f <- 0 until 4)
      centerModels(f).apply(
        new Scale(cscale, cscale, cscale).at(Vector3.center)
      )
  }

}

private class UVT(t: Transformation) extends UVTransformation {
  private val vec = new Vector3

  def transform(uv: UV) {
    vec.set(uv.u, 0, uv.v).apply(t)
    uv.set(vec.x, vec.z)
  }

  override def apply(uv: UV) = {
    vec.set(uv.u, 0, uv.v).apply(t)
    uv.set(vec.x, vec.z)
  }

  override def inverse() = new UVT(t.inverse())
}

object PipeRSHighlightRenderer extends IMicroHighlightRenderer {
  override def renderHighlight(
      player: EntityPlayer,
      hit: MovingObjectPosition,
      mcrClass: CommonMicroClass,
      size: Int,
      material: Int
  ): Boolean = {
    if (mcrClass.getClassId != 3 || size != 1 || player.isSneaking) return false
    val tile = PRLib.getMultipartTile(
      player.worldObj,
      hit.blockX,
      hit.blockY,
      hit.blockZ
    )
    if (tile == null) return false

    MicroMaterialRegistry.getMaterial(material) match {
      case b: BlockMicroMaterial if b.block == Blocks.redstone_block =>
        val hitData: (Integer, _) = ExtendedMOP.getData(hit)
        tile.partList(hitData._1) match {
          case p: TRedstonePipe if !p.material =>
            RenderPipe.renderRSMicroHighlight(p)
            true
          case _ => false
        }
      case _ => false
    }
  }
}

object PipeColourHighlightRenderer extends IMicroHighlightRenderer {
  override def renderHighlight(
      player: EntityPlayer,
      hit: MovingObjectPosition,
      mcrClass: CommonMicroClass,
      size: Int,
      material: Int
  ): Boolean = {
    if (mcrClass.getClassId != 3 || size != 1 || player.isSneaking) return false
    val tile = PRLib.getMultipartTile(
      player.worldObj,
      hit.blockX,
      hit.blockY,
      hit.blockZ
    )
    if (tile == null) return false

    MicroMaterialRegistry.getMaterial(material) match {
      case b: BlockMicroMaterial if b.block == Blocks.wool =>
        val hitData: (Integer, _) = ExtendedMOP.getData(hit)
        tile.partList(hitData._1) match {
          case p: TColourFilterPipe if p.colour != b.meta =>
            RenderPipe.renderWoolMicroHighlight(p, b.meta.toByte)
            true
          case _ => false
        }
      case _ => false
    }
  }
}

object PipeItemRenderer extends IItemRenderer {
  def handleRenderType(item: ItemStack, r: ItemRenderType) = true
  def shouldUseRenderHelper(
      r: ItemRenderType,
      item: ItemStack,
      helper: ItemRendererHelper
  ) = true

  def renderItem(rtype: ItemRenderType, item: ItemStack, data: AnyRef*) {
    val damage = item.getItemDamage
    import net.minecraftforge.client.IItemRenderer.ItemRenderType._
    rtype match {
      case ENTITY   => renderWireInventory(damage, -.5f, 0f, -.5f, 1f)
      case EQUIPPED => renderWireInventory(damage, 0f, .0f, 0f, 1f)
      case EQUIPPED_FIRST_PERSON =>
        renderWireInventory(damage, 1f, -.6f, -.4f, 2f)
      case INVENTORY => renderWireInventory(damage, 0f, -.1f, 0f, 1f)
      case _         =>
    }
  }

  def renderWireInventory(
      meta: Int,
      x: Float,
      y: Float,
      z: Float,
      scale: Float
  ) {
    val pdef = PipeDefs.values(meta)
    if (pdef == null) return
    TextureUtils.bindAtlas(0)
    val ccrsi = CCRenderState.instance
    ccrsi.reset()
    ccrsi.setDynamic()
    ccrsi.pullLightmap()
    ccrsi.startDrawing()

    RenderPipe.renderInv(
      new Scale(scale).`with`(new Translation(x, y, z)),
      new IconTransformation(pdef.sprites(0))
    )

    ccrsi.draw()
  }
}
