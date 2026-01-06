/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.expansion

import java.util.UUID

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.render.uv.{MultiIconTransformation, UVTransformation}
import codechicken.lib.vec.{BlockCoord, Cuboid6, Vector3}
import codechicken.multipart.IRedstoneConnector
import com.mojang.authlib.GameProfile
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.block.TInstancedBlockRender
import mrtjp.core.color.Colors
import mrtjp.core.gui._
import mrtjp.core.inventory.TInventory
import mrtjp.core.render.TCubeMapRender
import mrtjp.core.vec.Point
import mrtjp.core.world.WorldLib
import mrtjp.projectred.ProjectRedExpansion
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.expansion.TileBlockPlacer._
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, WorldServer}
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent

import scala.ref.WeakReference

class TileBlockPlacer
    extends TileMachine
    with TActiveDevice
    with TInventory
    with IRedstoneConnector {
  override def save(tag: NBTTagCompound) {
    super.save(tag)
    saveInv(tag)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    loadInv(tag)
  }

  override def markDirty() {
    super.markDirty()
  }

  override def getBlock = ProjectRedExpansion.machine2
  override def doesRotate = false
  override def doesOrient = true

  override def size = 9
  override def name = "block_placer"

  def reloadPlayer() {
    if (fakePlayer == null)
      fake = WeakReference(
        FakePlayerFactory.get(
          world.asInstanceOf[WorldServer],
          new GameProfile(UUID.randomUUID(), "[PR_FAKE]")
        )
      )

    val pos = new Vector3(x, y, z).add(positions(side))

    val (pitch, yaw) = angles(side)
    fakePlayer.setLocationAndAngles(pos.x, pos.y, pos.z, yaw, pitch)
  }

  override def onBlockRemoval() {
    super.onBlockRemoval()
    dropInvContents(world, x, y, z)
  }

  override def onBlockActivated(player: EntityPlayer, actside: Int): Boolean = {
    if (super.onBlockActivated(player, actside)) return true

    if (!world.isRemote)
      GuiBlockPlacer.open(
        player,
        createContainer(player),
        _.writeCoord(x, y, z)
      )
    true
  }

  def createContainer(player: EntityPlayer) =
    new ContainerBlockPlacer(player, this)

  override def onActivate() {
    if (world.isRemote) return
    reloadPlayer()
    val upos = position.offset(side ^ 1)

    // tryUseItem will immediately trigger block updates, which can trigger other TileBlockPlacers' onActivate.
    // Because of this, we back up the inventory of the fake player before copying over this block's inventory,
    // and we restore it before we return from this function.

    // Otherwise, if TileBlockPlacer A's onActivated triggered TileBlockPlacer B's onActivated,
    // TileBlockPlacer B would overwrite the inventory that TileBlockPlacer A copied into the fake player,
    // and TileBlockPlacer A would copy back the inventory that TileBlockPlacer B stored in the fake player.
    val backupInv = new Array[ItemStack](9)
    for (i <- 0 until 9) backupInv(i) = fakePlayer.inventory.getStackInSlot(i)

    try {
      copyInvToPlayer()
      for (i <- 0 until 9) {
        val stack = getStackInSlot(i)
        if (stack != null && tryUseItem(stack, upos.x, upos.y, upos.z, i)) {
          if (fakePlayer.isUsingItem) fakePlayer.stopUsingItem()
          copyInvFromPlayer()
          val newStack = getStackInSlot(i)
          if (newStack != null && newStack.stackSize == 0)
            setInventorySlotContents(i, null)
          return
        }
      }
      copyInvFromPlayer()
    } finally {
      // Restore inventory as it was before
      for (i <- 0 until 9)
        fakePlayer.inventory.setInventorySlotContents(i, backupInv(i))
    }

  }

  def copyInvToPlayer() {
    for (i <- 0 until 9)
      fakePlayer.inventory.setInventorySlotContents(i, getStackInSlot(i))
  }

  def copyInvFromPlayer() {
    for (i <- 0 until 9)
      setInventorySlotContents(i, fakePlayer.inventory.getStackInSlot(i))
  }

  def tryUseItem(stack: ItemStack, x: Int, y: Int, z: Int, slot: Int) = {
    fakePlayer.inventory.currentItem = slot

    try {
      tryRightClick(stack, x, y, z, slot) ||
      tryEntityClick(stack, x, y, z)
    } catch {
      case e: Throwable => false
    }
  }

  def tryRightClick(
      stack: ItemStack,
      x: Int,
      y: Int,
      z: Int,
      slot: Int
  ): Boolean = {
    val event = ForgeEventFactory.onPlayerInteract(
      fakePlayer,
      PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
      x,
      y,
      z,
      1,
      world
    )
    if (event.isCanceled) return false

    if (
      stack.getItem.onItemUseFirst(
        stack,
        fakePlayer,
        world,
        x,
        y,
        z,
        1,
        0.5f,
        0.5f,
        0.5f
      )
    ) return true

    if (stack.getItem.isInstanceOf[ItemBlock]) {
      if (
        stack.tryPlaceItemIntoWorld(
          fakePlayer,
          world,
          x,
          y,
          z,
          1,
          0.5f,
          0.5f,
          0.5f
        )
      ) return true
    } else {
      if (
        stack.getItem.onItemUse(
          stack,
          fakePlayer,
          world,
          x,
          y,
          z,
          1,
          0.5f,
          0.5f,
          0.5f
        )
      ) return true
      if (
        stack.getItem.onItemUse(
          stack,
          fakePlayer,
          world,
          x,
          y - 1,
          z,
          1,
          0.5f,
          0.5f,
          0.5f
        )
      ) return true
    }

    val size = stack.stackSize
    val newStack = stack.useItemRightClick(world, fakePlayer)
    if (stack != newStack || (newStack != null && newStack.stackSize != size)) {
      fakePlayer.inventory.setInventorySlotContents(slot, newStack)
      return true
    }

    false
  }

  def tryEntityClick(stack: ItemStack, x: Int, y: Int, z: Int): Boolean = {
    val start = position.toVector3Centered
    val end = start.copy.add(
      new Vector3(BlockCoord.sideOffsets(side ^ 1)).multiply(2.5)
    )
    val e = traceEntityHits(start, end)
    e != null && useOnEntity(stack, e)
  }

  def useOnEntity(stack: ItemStack, e: Entity): Boolean = {
    if (e.interactFirst(fakePlayer)) return true

    if (e.isInstanceOf[EntityLivingBase]) {
      val oldS = stack.stackSize
      stack.interactWithEntity(fakePlayer, e.asInstanceOf[EntityLivingBase])
      if (stack.stackSize != oldS) return true
    }
    false
  }

  def traceEntityHits(start: Vector3, end: Vector3) = {
    val box = new Cuboid6(0, 1, 0, 1, 3.5, 1)
      .apply(rotationT)
      .add(new Vector3(position))
    val elist =
      world.getEntitiesWithinAABBExcludingEntity(fakePlayer, box.toAABB)

    var eHit: Entity = null
    var edis = Double.MaxValue

    for (i <- 0 until elist.size()) {
      val e = elist.get(i).asInstanceOf[Entity]
      if (e.canBeCollidedWith) {
        val a2 = new Cuboid6(e.boundingBox).expand(e.getCollisionBorderSize)
        if (
          a2.toAABB.isVecInside(start.toVec3D)
        ) // Why doesnt Cuboid6 have this method!?
          {
            eHit = e
            edis = 0.0d
          } else {
          val mop = RayTracer.instance.rayTraceCuboid(start, end, a2)
          if (mop != null) {
            val d = new Vector3(mop.hitVec).subtract(start).mag
            if (d < edis) {
              eHit = e
              edis = d
            }
          }
        }
      }
    }
    eHit
  }

  override def getConnectionMask(side: Int) =
    if ((side ^ 1) == this.side) 0 else 0x1f
  override def weakPowerLevel(side: Int, mask: Int) = 0
}

object TileBlockPlacer {
  var fake = WeakReference[EntityPlayer](null)
  def fakePlayer = fake.get.orNull

  val angles = Seq(
    (-90f, 0f),
    (90f, 0f),
    (0f, 0f),
    (0f, 180f),
    (0f, 270f),
    (0f, 90f)
  )

  val positions = Seq(
    new Vector3(0.5d, -1.12d + 0.60d, 0.5d),
    new Vector3(0.5d, -1.12d - 0.52d, 0.5d),
    new Vector3(0.5d, -1.12d, 0.5d + 0.52d),
    new Vector3(0.5d, -1.12d, 0.5d - 0.52d),
    new Vector3(0.5d + 0.52d, -1.12d, 0.5d),
    new Vector3(0.5d - 0.52d, -1.12d, 0.5d)
  )
}

class ContainerBlockPlacer(p: EntityPlayer, tile: TileBlockPlacer)
    extends NodeContainer {
  {
    var s = 0
    for ((x, y) <- GuiLib.createSlotGrid(62, 18, 3, 3, 0, 0)) {
      addSlotToContainer(new Slot3(tile, s, x, y))
      s += 1
    }
    addPlayerInv(p, 8, 86)
  }
}

class GuiBlockPlacer(c: ContainerBlockPlacer) extends NodeGui(c, 176, 168) {
  override def drawBack_Impl(mouse: Point, frame: Float) {
    PRResources.guiBlockPlacer.bind()
    GuiDraw.drawTexturedModalRect(0, 0, 0, 0, 176, 168)
    GuiDraw.drawString(
      I18n.format("gui.projectred.expansion.machine2|2.title"),
      8,
      6,
      Colors.GREY.argb,
      false
    )
    GuiDraw.drawString(
      I18n.format("container.inventory"),
      8,
      75,
      Colors.GREY.argb,
      false
    )
  }
}

object GuiBlockPlacer extends TGuiBuilder {
  override def getID = ExpansionProxy.blockPlacerGui

  @SideOnly(Side.CLIENT)
  override def buildGui(player: EntityPlayer, data: MCDataInput) = {
    val t = WorldLib.getTileEntity(
      player.worldObj,
      data.readCoord(),
      classOf[TileBlockPlacer]
    )
    if (t != null) new GuiBlockPlacer(t.createContainer(player))
    else null
  }
}

object RenderBlockPlacer extends TInstancedBlockRender with TCubeMapRender {
  var bottom: IIcon = _
  var side1A: IIcon = _
  var side2A: IIcon = _
  var topA: IIcon = _
  var side1B: IIcon = _
  var side2B: IIcon = _
  var topB: IIcon = _

  var iconT1: UVTransformation = _
  var iconT2: UVTransformation = _

  override def getData(w: IBlockAccess, x: Int, y: Int, z: Int) = {
    val te = WorldLib.getTileEntity(w, x, y, z, classOf[TActiveDevice])
    if (te != null)
      (te.side, te.rotation, if (te.active || te.powered) iconT2 else iconT1)
    else (0, 0, iconT1)
  }

  override def getInvData = (0, 0, iconT1)

  override def getIcon(s: Int, meta: Int) = s match {
    case 0 => bottom
    case 1 => topA
    case 2 => side1A
    case 3 => side1A
    case 4 => side2A
    case 5 => side2A
    case _ => bottom
  }

  override def registerIcons(reg: IIconRegister) {
    def register(s: String) =
      reg.registerIcon(s"projectred:mechanical/placer/$s")
    bottom = register("bottom")

    side1A = register("side1a")
    side2A = register("side2a")
    topA = register("topa")

    side1B = register("side1b")
    side2B = register("side2b")
    topB = register("topb")

    iconT1 =
      new MultiIconTransformation(bottom, topA, side1A, side1A, side2A, side2A)
    iconT2 =
      new MultiIconTransformation(bottom, topB, side1B, side1B, side2B, side2B)
  }
}
