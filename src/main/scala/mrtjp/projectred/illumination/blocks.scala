package mrtjp.projectred.illumination

import java.util.{List => JList, Random}

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.{BlockCoord, Vector3}
import codechicken.multipart.IRedstoneConnectorBlock
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.block.{BlockCore, InstancedBlock, InstancedBlockTile}
import mrtjp.core.color.{Colors, Colors_old}
import mrtjp.core.world.WorldLib
import mrtjp.projectred.ProjectRedIllumination
import mrtjp.projectred.core.libmc.PRLib
import mrtjp.projectred.core.libmc.fx._
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EnumCreatureType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}

class BlockLamp
    extends InstancedBlock(
      "projectred.illumination.lamp",
      new Material(Material.circuits.getMaterialMapColor)
    )
    with IRedstoneConnectorBlock {
  setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)
  setHardness(0.5f)
  setCreativeTab(ProjectRedIllumination.tabLighting)

  override def renderAsNormalBlock = true

  override def getRenderType = 0

  override def isOpaqueCube = true

  override def isBlockNormalCube = true

  override def getSubBlocks(b: Item, tab: CreativeTabs, list: JList[_]) = {
    for (i <- 0 until 32)
      list
        .asInstanceOf[JList[ItemStack]]
        .add(new ItemStack(ProjectRedIllumination.blockLamp, 1, i))
  }

  override def canCreatureSpawn(
      t: EnumCreatureType,
      w: IBlockAccess,
      x: Int,
      y: Int,
      z: Int
  ) = false

  override def canConnectRedstone(
      w: IBlockAccess,
      x: Int,
      y: Int,
      z: Int,
      s: Int
  ) = true

  override def canProvidePower = true

  override def registerBlockIcons(reg: IIconRegister) {
    val onIcons = Vector.newBuilder[IIcon]
    val offIcons = Vector.newBuilder[IIcon]
    for (i <- 0 until 16) {
      onIcons += reg.registerIcon("projectred:lighting/lampon/" + i)
      offIcons += reg.registerIcon("projectred:lighting/lampoff/" + i)
    }
    BlockLamp.on = onIcons.result()
    BlockLamp.off = offIcons.result()
  }

  override def getIcon(w: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = {
    val t = WorldLib.getTileEntity(w, x, y, z, classOf[TileLamp])
    if (t != null)
      if (t.isOn) BlockLamp.on(t.getColor) else BlockLamp.off(t.getColor)
    else super.getIcon(w, x, y, z, side)
  }

  override def getIcon(side: Int, meta: Int) =
    if (meta > 15) BlockLamp.on(meta % 16) else BlockLamp.off(meta)

  override def getLightValue(w: IBlockAccess, x: Int, y: Int, z: Int) =
    w match {
      case world: World =>
        BlockLamp.getLightValue(world, x, y, z)
      case _ => super.getLightValue(w, x, y, z)
    }

  def getConnectionMask(
      world: IBlockAccess,
      x: Int,
      y: Int,
      z: Int,
      side: Int
  ) = 0x1f
  def weakPowerLevel(
      world: IBlockAccess,
      x: Int,
      y: Int,
      z: Int,
      side: Int,
      mask: Int
  ) = 0
}

object BlockLamp {
  var on: Seq[IIcon] = null
  var off: Seq[IIcon] = null

  private val cache = new LampLightCache

  def getLightValue(world: World, x: Int, y: Int, z: Int) =
    cache.get(world, x, y, z) >> 4
  def setLightValue(world: World, x: Int, y: Int, z: Int, light: Int, color: Int) =
    cache.put(world, x, y, z, (light << 4) | color)
  def clearLightValue(world: World, x: Int, y: Int, z: Int) =
    cache.remove(world, x, y, z)
  def foreachLitHalo(world: World)(f: Int4Consumer) =
    cache.foreachLit(world)(f)
  def cacheVersion(world: World): Long =
    cache.version(world)
}

trait Int4Consumer {
  def apply(x: Int, y: Int, z: Int, color: Int): Unit
}

private class LampLightCache {
  private val tables = new java.util.WeakHashMap[World, LampLightTable]()

  def get(world: World, x: Int, y: Int, z: Int): Int = {
    val t = find(world)
    if (t == null) 0 else t.get(pack(x, y, z))
  }

  def put(world: World, x: Int, y: Int, z: Int, v: Int): Unit =
    tableFor(world).put(pack(x, y, z), v)

  def remove(world: World, x: Int, y: Int, z: Int): Unit = {
    val t = find(world)
    if (t != null) t.remove(pack(x, y, z))
  }

  def foreachLit(world: World)(f: Int4Consumer): Unit = {
    val t = find(world)
    if (t != null) t.foreachLit(f)
  }

  def version(world: World): Long = {
    val t = find(world)
    if (t == null) -1 else t.version
  }

  private def find(world: World): LampLightTable =
    tables.synchronized(tables.get(world))

  private def tableFor(world: World): LampLightTable = tables.synchronized {
    var table = tables.get(world)
    if (table == null) {
      table = new LampLightTable
      tables.put(world, table)
    }
    table
  }

  private def pack(x: Int, y: Int, z: Int) =
    ((x.toLong & 0x3ffffffL) << 38) | ((z.toLong & 0x3ffffffL) << 12) |
      (y.toLong & 0xfffL)
}

private class LampLightTable {
  private val EMPTY = Long.MinValue
  private val TOMB = Long.MinValue + 1
  private var keys = Array.fill(8)(EMPTY)
  private var vals = new Array[Int](8)
  private var used = 0
  private var mask = 7
  private[illumination] var version: Long = 0

  def get(key: Long): Int = {
    var i = hash(key) & mask
    var n = 0
    while (n < keys.length) {
      val k = keys(i)
      if (k == key) return vals(i)
      if (k == EMPTY) return 0
      i = (i + 1) & mask
      n += 1
    }
    0
  }

  def put(key: Long, v: Int): Unit = {
    var i = hash(key) & mask
    var n = 0
    var tomb = -1
    while (n < keys.length) {
      val k = keys(i)
      if (k == key) {
        if (vals(i) != v) {
          vals(i) = v
          version += 1
        }
        return
      }
      if (k == TOMB && tomb < 0) tomb = i
      else if (k == EMPTY) {
        insert(if (tomb < 0) i else tomb, key, v)
        return
      }
      i = (i + 1) & mask
      n += 1
    }
    if (tomb >= 0) insert(tomb, key, v)
  }

  private def insert(i: Int, key: Long, v: Int): Unit = {
    if (keys(i) == EMPTY) used += 1
    keys(i) = key
    vals(i) = v
    if (used >= keys.length - keys.length / 3) grow()
    version += 1
  }

  def remove(key: Long): Unit = {
    var i = hash(key) & mask
    var n = 0
    while (n < keys.length) {
      val k = keys(i)
      if (k == key) {
        keys(i) = TOMB
        version += 1
        return
      }
      if (k == EMPTY) return
      i = (i + 1) & mask
      n += 1
    }
  }

  private def grow(): Unit = {
    val oldKeys = keys
    val oldVals = vals
    val size = oldKeys.length * 2
    keys = Array.fill(size)(EMPTY)
    vals = new Array[Int](size)
    mask = size - 1
    used = 0
    var i = 0
    while (i < oldKeys.length) {
      val oldKey = oldKeys(i)
      if (oldKey != EMPTY && oldKey != TOMB) {
        var j = hash(oldKey) & mask
        var n = 0
        while (n < size && keys(j) != EMPTY) {
          j = (j + 1) & mask
          n += 1
        }
        if (keys(j) == EMPTY) {
          keys(j) = oldKey
          vals(j) = oldVals(i)
          used += 1
        }
      }
      i += 1
    }
  }

  private def hash(key: Long): Int = {
    val h = key * 0x9e3779b97f4a7c15L
    (h ^ (h >>> 32)).toInt
  }

  def foreachLit(f: Int4Consumer): Unit = {
    var i = 0
    while (i < keys.length) {
      val k = keys(i)
      if (k != EMPTY && k != TOMB && (vals(i) >>> 4) > 0) {
        val x = ((k >>> 38).toInt << 6) >> 6
        val y = (k & 0xfffL).toInt
        val z = (((k >>> 12).toInt & 0x3ffffff) << 6) >> 6
        f(x, y, z, vals(i) & 0xf)
      }
      i += 1
    }
  }
}

class TileLamp extends InstancedBlockTile with ILight {
  var inverted = false
  var powered = false
  private var lightCache = 0
  private var lightDirty = true

  override def getBlock = ProjectRedIllumination.blockLamp
  override def getMetaData = getColor + (if (inverted) 16 else 0)

  override def onBlockPlaced(
      side: Int,
      meta: Int,
      player: EntityPlayer,
      stack: ItemStack,
      hit: Vector3
  ) {
    inverted = meta > 15
    lightDirty = true
    scheduleTick(2)
  }
  override def getLightValue = {
    if (lightDirty) recomputeLight()
    lightCache
  }

  private def recomputeLight() {
    lightCache =
      if (inverted != powered)
        IlluminationProxy.getLightValue(getColor, 15)
      else 0
    lightDirty = false
    BlockLamp.setLightValue(
      world,
      x,
      y,
      z,
      lightCache,
      getColor
    )
  }

  override def onNeighborChange(b: Block) {
    if (!world.isRemote) scheduleTick(2)
  }

  override def onBlockRemoval() {
    super.onBlockRemoval()
    BlockLamp.clearLightValue(world, x, y, z)
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    BlockLamp.clearLightValue(world, x, y, z)
  }

  override def invalidate() {
    super.invalidate()
    BlockLamp.clearLightValue(world, x, y, z)
  }

  def checkPower = {
    world.isBlockIndirectlyGettingPowered(x, y, z) ||
    world.getBlockPowerInput(x, y, z) != 0
  }

  override def onScheduledTick() {
    val oldInv = inverted
    val oldPow = powered
    inverted = getBlockMetadata > 15
    powered = checkPower
    if (oldInv != inverted || oldPow != powered) {
      recomputeLight()
      markDescUpdate()
      markLight()
    }
  }

  override def update() {
    super.update()
    if (lightDirty) {
      recomputeLight()
      markLight()
    }
  }

  override def updateClient() {
    super.updateClient()
    if (lightDirty) {
      recomputeLight()
      markLight()
    }
  }

  override def readDesc(in: MCDataInput) {
    inverted = in.readBoolean()
    powered = in.readBoolean()
    recomputeLight()
    markRender()
    markLight()
  }

  override def writeDesc(out: MCDataOutput) {
    out.writeBoolean(inverted).writeBoolean(powered)
  }

  override def load(tag: NBTTagCompound) {
    inverted = tag.getBoolean("inv")
    powered = tag.getBoolean("pow")
    lightDirty = true
  }

  override def validate() {
    super.validate()
    scheduleTick(2)
  }

  override def save(tag: NBTTagCompound) {
    tag.setBoolean("inv", inverted)
    tag.setBoolean("pow", powered)
  }

  override def getColor = getBlockMetadata % 16
  override def isOn = inverted != powered
}

class BlockAirousLight
    extends BlockCore("projectred.illumination.airousLight", Material.air) {
  override def isAir(world: IBlockAccess, x: Int, y: Int, z: Int) = true
  override def getRenderType = -1
  override def getCollisionBoundingBoxFromPool(
      w: World,
      x: Int,
      y: Int,
      z: Int
  ) = null
  override def isOpaqueCube = false
  override def canCollideCheck(meta: Int, click: Boolean) = false
  override def dropBlockAsItemWithChance(
      w: World,
      x: Int,
      y: Int,
      z: Int,
      a: Int,
      b: Float,
      c: Int
  ) {}

  override def createTileEntity(w: World, meta: Int) = new TileAirousLight
  override def hasTileEntity(meta: Int) = true

  @SideOnly(Side.CLIENT)
  override def randomDisplayTick(
      world: World,
      x: Int,
      y: Int,
      z: Int,
      rand: Random
  ) {
    if (rand.nextInt(10) > 0) return
    val color = world.getBlockMetadata(x, y, z) % 16

    val dist = 3
    val dx = x + rand.nextInt(dist) - rand.nextInt(dist)
    val dy = y + rand.nextInt(dist) - rand.nextInt(dist)
    val dz = z + rand.nextInt(dist) - rand.nextInt(dist)
    val ex = dx + rand.nextInt(dist) - rand.nextInt(dist)
    val ey = dy + rand.nextInt(dist) - rand.nextInt(dist)
    val ez = dz + rand.nextInt(dist) - rand.nextInt(dist)

    val c = ParticleManagement.instance.spawn(world, "ember", dx, dy, dz)
    if (c != null) {
      val orbit = new ParticleLogicOrbitPoint(new Vector3(ex, ey, ez))
      orbit.setOrbitSpeed(0.5f * rand.nextDouble).setTargetDistance(0.3d)
      orbit.setShrinkingOrbit(0.01, 0.01).setPriority(2)
      val scale = new ParticleLogicScale
      scale.setRate(-0.001f, -0.0001f * rand.nextFloat)
      scale.setTerminate(true)

      val iconshift = ParticleLogicIconShift.fluttering
      val approach =
        new ParticleLogicApproachPoint(new Vector3(ex, ey, ez), 0.03f, 0.5f)
      approach.setFinal(true)

      c.setIgnoreMaxAge(true)
      c.setScale(0.05f + 0.02f * rand.nextFloat)
      c.setPRColor(Colors.apply(color))
      c += orbit
      c += scale
      c += iconshift
      c += approach
    }
  }

  override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    val te = WorldLib.getTileEntity(world, x, y, z, classOf[TileAirousLight])
    if (te != null) te.lightVal else 0
  }
}

class TileAirousLight extends TileEntity {
  private val source = new BlockCoord(-1, -1, -1)
  private var sourcePartID = -1
  private var color = -1
  private var delay = 100

  override def updateEntity() {
    if (!worldObj.isRemote) {
      if ({ delay -= 1; delay } > 0) return
      delay = worldObj.rand.nextInt(100)

      val light = getLight
      if (light == null || !light.isOn || light.getColor != color)
        worldObj.setBlockToAir(xCoord, yCoord, zCoord)
    }
  }

  private def getLight: ILight = {
    if (sourcePartID > -1) {
      PRLib.getMultiPart(worldObj, source, sourcePartID) match {
        case light: ILight => return light
        case _             =>
      }
    }
    WorldLib.getTileEntity(worldObj, source, classOf[ILight])
  }

  def setSource(x: Int, y: Int, z: Int, color: Int, partID: Int) = {
    source.set(x, y, z)
    this.color = color
    sourcePartID = partID
  }

  override def readFromNBT(tag: NBTTagCompound) {
    super.readFromNBT(tag)
    val x = tag.getInteger("sX")
    val y = tag.getInteger("sY")
    val z = tag.getInteger("sZ")
    source.set(x, y, z)
    sourcePartID = tag.getByte("spID")
    color = tag.getByte("col")
  }

  override def writeToNBT(tag: NBTTagCompound) {
    super.writeToNBT(tag)
    tag.setInteger("sX", source.x)
    tag.setInteger("sY", source.y)
    tag.setInteger("sX", source.z)
    tag.setByte("spID", sourcePartID.asInstanceOf[Byte])
    tag.setByte("col", color.asInstanceOf[Byte])
  }

  def lightVal = IlluminationProxy.getLightValue(color, 15)
}
