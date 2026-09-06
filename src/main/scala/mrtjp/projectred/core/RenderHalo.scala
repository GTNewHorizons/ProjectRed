package mrtjp.projectred.core

import codechicken.lib.render.BlockRenderer.BlockFace
import codechicken.lib.vec._
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import mrtjp.core.color.Colors
import mrtjp.projectred.illumination.{BlockLamp, Int4Consumer}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.culling.Frustrum
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import java.nio.{BufferOverflowException, ByteBuffer}
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15

object RenderHalo {
  private var lightArray = new Array[LightCache](64)
  private var lightCount = 0
  private val pool = new java.util.ArrayDeque[LightCache]()
  private val translation = new Translation(0, 0, 0)
  private val haloColours = Array.tabulate(16)(i => Colors(i).rgba)
  private val haloAlpha = 128 / 255.0f
  private val frustum = new Frustrum
  private val cullBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0)
  private val lampHaloBox = Cuboid6.full.copy.expand(0.05d)

  private val haloFaceVerts = {
    val face = new BlockFace()
    val buf = new Array[Float](72)
    var s = 0
    var o = 0
    while (s < 6) {
      face.loadCuboidFace(lampHaloBox, s)
      var i = 0
      while (i < 4) {
        val v = face.verts(i).vec
        buf(o) = v.x.toFloat
        buf(o + 1) = v.y.toFloat
        buf(o + 2) = v.z.toFloat
        o += 3
        i += 1
      }
      s += 1
    }
    buf
  }

  private var batchVBO = 0
  private var batchWorld: World = null
  private var batchVersion = -1L
  private var batchVerts = 0
  private var batchBuf: ByteBuffer = null
  private var anchorX = 0.0d
  private var anchorY = 0.0d
  private var anchorZ = 0.0d

  private class VBOEntry(
      val minX: Double,
      val minY: Double,
      val minZ: Double,
      val maxX: Double,
      val maxY: Double,
      val maxZ: Double,
      val vbo: Int
  )

  private val haloVBOs = new java.util.ArrayList[VBOEntry]()

  private class LightCache {
    var x = 0
    var y = 0
    var z = 0
    var color = 0
    var cube: Cuboid6 = _

    def set(x: Int, y: Int, z: Int, color: Int, cube: Cuboid6) {
      this.x = x
      this.y = y
      this.z = z
      this.color = color
      this.cube = cube
    }
  }

  def addLight(x: Int, y: Int, z: Int, color: Int, box: Cuboid6) {
    if (lightCount == lightArray.length)
      lightArray = java.util.Arrays.copyOf(lightArray, lightCount * 2)
    var lc = pool.poll()
    if (lc == null) lc = new LightCache()
    lc.set(x, y, z, color, box)
    lightArray(lightCount) = lc
    lightCount += 1
  }

  @SubscribeEvent
  def onWorldUnload(event: WorldEvent.Unload) {
    batchWorld = null
  }

  @SubscribeEvent
  def onRenderWorldLast(event: RenderWorldLastEvent) {
    val entity = Minecraft.getMinecraft.renderViewEntity
    frustum.setPosition(
      entity.posX - (entity.posX - entity.lastTickPosX) * event.partialTicks,
      entity.posY - (entity.posY - entity.lastTickPosY) * event.partialTicks,
      entity.posZ - (entity.posZ - entity.lastTickPosZ) * event.partialTicks
    )
    val visible = compactVisible()
    val world = Minecraft.getMinecraft.theWorld

    glPushMatrix()

    // Adjust translation for camera movement between frames (using camra coordinates for numeric stability).
    // Note: When porting to MC 1.8, might want to use GlStateManager.translate() here instead.
    glTranslated(
      entity.posX - (entity.posX - entity.lastTickPosX) * event.partialTicks - entity.lastTickPosX,
      entity.posY - (entity.posY - entity.lastTickPosY) * event.partialTicks - entity.lastTickPosY,
      entity.posZ - (entity.posZ - entity.lastTickPosZ) * event.partialTicks - entity.lastTickPosZ
    )

    prepareRenderState()

    var i = 0
    while (i < lightCount) {
      if (i < visible) renderHalo(lightArray(i))
      pool.add(lightArray(i))
      i += 1
    }
    lightCount = 0

    if (world != null) {
      val entity = Minecraft.getMinecraft.renderViewEntity
      val ver = BlockLamp.cacheVersion(world)
      val dx = entity.posX - anchorX
      val dy = entity.posY - anchorY
      val dz = entity.posZ - anchorZ
      if (
        (world ne batchWorld) || ver != batchVersion ||
        dx * dx + dy * dy + dz * dz > 1073741824.0d
      ) {
        rebuildBatch(world)
        batchWorld = world
        batchVersion = ver
      }
      if (batchVerts > 0) {
        glPushMatrix()
        glTranslated(
          anchorX - entity.posX,
          anchorY - entity.posY,
          anchorZ - entity.posZ
        )
        drawBatch()
        glPopMatrix()
      }
    }

    restoreRenderState()
    glPopMatrix()
  }

  def prepareRenderState() {
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE)
    glDisable(GL_TEXTURE_2D)
    glDisable(GL_LIGHTING)
    glDisable(GL_CULL_FACE)
    glDepthMask(false)
  }

  def restoreRenderState() {
    glDepthMask(true)
    glColor4d(1, 1, 1, 1)
    glEnable(GL_CULL_FACE)
    glEnable(GL_LIGHTING)
    glEnable(GL_TEXTURE_2D)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    glDisable(GL_BLEND)
  }

  private def renderHalo(cc: LightCache) {
    renderHaloAt(cc.x, cc.y, cc.z, cc.color, cc.cube)
  }

  private def renderHaloAt(x: Int, y: Int, z: Int, color: Int, box: Cuboid6) {
    // Make sure to use camera coordinates for the halo transformation.
    val entity = Minecraft.getMinecraft.renderViewEntity
    translation.vec.set(x - entity.posX, y - entity.posY, z - entity.posZ)
    glPushMatrix()
    renderHalo(box, color, translation)
    glPopMatrix()
  }

  private def inFrustum(cc: LightCache): Boolean =
    inFrustum(cc.x, cc.y, cc.z, cc.cube)

  private def inFrustum(x: Int, y: Int, z: Int, cube: Cuboid6): Boolean = {
    cullBox.minX = x + cube.min.x
    cullBox.minY = y + cube.min.y
    cullBox.minZ = z + cube.min.z
    cullBox.maxX = x + cube.max.x
    cullBox.maxY = y + cube.max.y
    cullBox.maxZ = z + cube.max.z
    frustum.isBoundingBoxInFrustum(cullBox)
  }

  private def compactVisible(): Int = {
    var w = 0
    var i = 0
    while (i < lightCount) {
      val cc = lightArray(i)
      if (inFrustum(cc)) {
        if (w != i) {
          lightArray(i) = lightArray(w)
          lightArray(w) = cc
        }
        w += 1
      }
      i += 1
    }
    w
  }

  def renderHalo(cuboid: Cuboid6, colour: Int, t: Transformation) {
    val rgba = haloColours(colour)
    glColor4f(
      (rgba >>> 24 & 255) / 255.0f,
      (rgba >>> 16 & 255) / 255.0f,
      (rgba >>> 8 & 255) / 255.0f,
      haloAlpha
    )
    t.glApply()
    val vbo = getHaloVBO(cuboid)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
    glVertexPointer(3, GL_FLOAT, 0, 0L)
    glEnableClientState(GL_VERTEX_ARRAY)
    glDrawArrays(GL_QUADS, 0, 24)
    glDisableClientState(GL_VERTEX_ARRAY)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
  }

  private def getHaloVBO(cuboid: Cuboid6): Int = {
    var i = 0
    while (i < haloVBOs.size) {
      val e = haloVBOs.get(i)
      if (
        e.minX == cuboid.min.x && e.minY == cuboid.min.y && e.minZ == cuboid.min.z
        && e.maxX == cuboid.max.x && e.maxY == cuboid.max.y && e.maxZ == cuboid.max.z
      )
        return e.vbo
      i += 1
    }
    val vbo = buildHaloVBO(cuboid)
    haloVBOs.add(
      new VBOEntry(
        cuboid.min.x,
        cuboid.min.y,
        cuboid.min.z,
        cuboid.max.x,
        cuboid.max.y,
        cuboid.max.z,
        vbo
      )
    )
    vbo
  }

  private def buildHaloVBO(cuboid: Cuboid6): Int = {
    val buf = BufferUtils.createFloatBuffer(72)
    val face = new BlockFace()
    var s = 0
    while (s < 6) {
      face.loadCuboidFace(cuboid, s)
      var i = 0
      while (i < 4) {
        val v = face.verts(i).vec
        buf.put(v.x.toFloat).put(v.y.toFloat).put(v.z.toFloat)
        i += 1
      }
      s += 1
    }
    buf.flip()
    val vbo = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    vbo
  }

  private def rebuildBatch(world: World) {
    val entity = Minecraft.getMinecraft.renderViewEntity
    anchorX = entity.posX
    anchorY = entity.posY
    anchorZ = entity.posZ
    var count = 0
    val colorCounts = new Array[Int](16)
    val counter = new Int4Consumer {
      override def apply(x: Int, y: Int, z: Int, color: Int) {
        count += 1
        colorCounts(color & 15) += 1
      }
    }
    BlockLamp.foreachLitHalo(world)(counter)
    if (count == 0) {
      batchVerts = 0
      return
    }
    val ranges = new Array[Int](17)
    var c = 0
    while (c < 16) {
      ranges(c + 1) = ranges(c) + colorCounts(c) * 24 * 12
      c += 1
    }
    val bytes = ranges(16) + 2048
    if (batchBuf == null || batchBuf.capacity < bytes)
      batchBuf = BufferUtils.createByteBuffer(bytes)
    batchBuf.clear()
    val b = batchBuf
    val cursors = ranges.clone()
    val filler = new Int4Consumer {
      override def apply(x: Int, y: Int, z: Int, color: Int) {
        val cc = color & 15
        b.position(cursors(cc))
        var i = 0
        while (i < 72) {
          b.putFloat((x.toDouble - anchorX + haloFaceVerts(i)).toFloat)
          b.putFloat((y.toDouble - anchorY + haloFaceVerts(i + 1)).toFloat)
          b.putFloat((z.toDouble - anchorZ + haloFaceVerts(i + 2)).toFloat)
          i += 3
        }
        cursors(cc) += 24 * 12
      }
    }
    try {
      BlockLamp.foreachLitHalo(world)(filler)
    } catch {
      case _: BufferOverflowException => return
    }
    val batchBytes = ranges(16)
    batchVerts = batchBytes / 12
    batchRanges = ranges
    b.position(batchBytes)
    b.flip()
    if (batchVBO == 0) batchVBO = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, batchVBO)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, b, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
  }

  private def drawBatch() {
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, batchVBO)
    glEnableClientState(GL_VERTEX_ARRAY)
    glVertexPointer(3, GL_FLOAT, 12, 0L)
    var c = 0
    while (c < 16) {
      val count = (batchVertsOf(c + 1) - batchVertsOf(c)) / 24
      if (count > 0) {
        val rgba = haloColours(c)
        glColor4f(
          (rgba >>> 24 & 255) / 255.0f,
          (rgba >>> 16 & 255) / 255.0f,
          (rgba >>> 8 & 255) / 255.0f,
          haloAlpha
        )
        glDrawArrays(GL_QUADS, batchVertsOf(c), count * 24)
      }
      c += 1
    }
    glDisableClientState(GL_VERTEX_ARRAY)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
  }

  private def batchVertsOf(i: Int) = batchRanges(i) / 12

  private var batchRanges = new Array[Int](17)
}
