/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.expansion

import java.util.{List => JList}
import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import codechicken.lib.render.uv.{MultiIconTransformation, UVTransformation}
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui._
import mrtjp.core.inventory.TInventory
import mrtjp.core.item.{ItemEquality, ItemKey}
import mrtjp.core.render.TCubeMapRender
import mrtjp.core.vec.{Point, Size}
import mrtjp.core.world.WorldLib
import mrtjp.projectred.ProjectRedExpansion
import mrtjp.projectred.core.libmc.PRResources
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.client.resources.I18n
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{
  ISidedInventory,
  InventoryCraftResult,
  InventoryCrafting,
  SlotCrafting
}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.{CraftingManager, IRecipe}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.oredict.{ShapedOreRecipe, ShapelessOreRecipe}

import scala.collection.JavaConversions._

/** Slot layout for the project bench, in one place so the container, the tile
  * and the crafting slot cannot drift apart.
  *
  * Tile inventory (28 slots) and container slots 0..27 happen to use the same
  * indices; the container adds the output slot and the player inventory on top.
  */
object ProjectBenchSlots {

  /** Crafting grid. */
  final val GridStart = 0
  final val GridEnd = 9 // exclusive
  final val GridSize = GridEnd - GridStart

  /** Ingredient storage. */
  final val StorageStart = 9
  final val StorageEnd = 27 // exclusive
  final val StorageSize = StorageEnd - StorageStart

  /** Plan slot. */
  final val PlanSlot = 27

  /** Container-only slots; the tile inventory has no result slot of its own. */
  final val OutputSlot = 28
  final val PlayerInvStart = 29
  final val PlayerInvEnd = 65 // exclusive

  /** Order in which tile slots are copied into the "pool" array that
    * [[SlotProjectCrafting.searchFor]] consumes from: ingredient storage first,
    * then the crafting grid. Pool index k maps to tile slot PoolOrder(k), which
    * is what makes the write-back the exact inverse of the snapshot.
    *
    * Treat as immutable.
    */
  final val PoolOrder: Array[Int] =
    ((StorageStart until StorageEnd) ++ (GridStart until GridEnd)).toArray

  /** Index at which the crafting grid section of a pool array begins. */
  final val GridOffset = StorageSize
}

class TileProjectBench
    extends TileMachine
    with TInventory
    with ISidedInventory
    with TGuiMachine {
  import ProjectBenchSlots._

  val invCrafting = new InventoryCrafting(new NodeContainer, 3, 3)
  val invResult = new InventoryCraftResult

  var isPlanRecipe = false
  var currentRecipe: IRecipe = null
  var currentInputs = new Array[ItemStack](GridSize)

  private var recipeNeedsUpdate = true

  override def save(tag: NBTTagCompound): Unit = {
    super.save(tag)
    saveInv(tag)
  }

  override def load(tag: NBTTagCompound): Unit = {
    super.load(tag)
    loadInv(tag)
  }

  override def read(in: MCDataInput, key: Int) = key match {
    case 1 => writePlan()
    case 2 => clearGrid(in.readInt())
    case _ => super.read(in, key)
  }

  def sendWriteButtonAction(): Unit = {
    writeStream(1).sendToServer()
  }

  def sendClearGridAction(id: Int): Unit = {
    writeStream(2).writeInt(id).sendToServer()
  }

  override def getBlock = ProjectRedExpansion.machine2

  override def doesRotate = false
  override def doesOrient = false

  // 0-8 crafting grid, 9-26 ingredient storage, 27 plan.
  // The crafting result is not part of this inventory; it lives in invResult.
  override def size = 28
  override def name = "project_bench"

  override def canExtractItem(slot: Int, item: ItemStack, side: Int) =
    (StorageStart until StorageEnd).contains(slot)
  override def canInsertItem(slot: Int, item: ItemStack, side: Int) =
    (StorageStart until StorageEnd).contains(slot)
  override def getAccessibleSlotsFromSide(side: Int) =
    (StorageStart until StorageEnd).toArray

  override def update(): Unit = { updateRecipeIfNeeded() }
  override def updateClient(): Unit = { updateRecipeIfNeeded() }

  def updateRecipeIfNeeded(): Unit = {
    if (!recipeNeedsUpdate) return
    updateRecipeNow()
  }

  /** Recompute the recipe immediately and clear the pending flag, so callers
    * that have just rewritten the inventory do not leave a redundant refresh
    * queued for the next tick.
    */
  def updateRecipeNow(): Unit = {
    recipeNeedsUpdate = false
    updateRecipe()
  }

  def updateRecipe(): Unit = {
    isPlanRecipe = false
    currentRecipe = null
    currentInputs.transform(_ => null)
    invResult.setInventorySlotContents(0, null)

    if ((GridStart until GridEnd).exists(getStackInSlot(_) != null)) {
      for (i <- GridStart until GridEnd)
        invCrafting.setInventorySlotContents(i, getStackInSlot(i))
      matchAndSetRecipe()
    } else {
      val plan = getStackInSlot(PlanSlot)
      if (plan != null && ItemPlan.hasRecipeInside(plan)) {
        val inputs = ItemPlan.loadPlanInputs(plan)
        for (i <- GridStart until GridEnd)
          invCrafting.setInventorySlotContents(i, inputs(i))
        matchAndSetRecipe()
        if (currentRecipe != null) isPlanRecipe = true
      }
    }

    def matchAndSetRecipe(): Unit = {
      val recipes =
        CraftingManager.getInstance().getRecipeList.asInstanceOf[JList[IRecipe]]
      currentRecipe = recipes.find(_.matches(invCrafting, world)).orNull
      if (currentRecipe != null) {
        invResult.setInventorySlotContents(
          0,
          currentRecipe.getCraftingResult(invCrafting)
        )
        for (i <- GridStart until GridEnd)
          currentInputs(i) = {
            val s = invCrafting.getStackInSlot(i)
            if (s != null) s.copy else null
          }
      }
    }
  }

  def writePlan(): Unit = {
    if (currentRecipe != null && !isPlanRecipe) {
      val out = invResult.getStackInSlot(0)
      if (out != null) {
        val stack = getStackInSlot(PlanSlot)
        if (stack != null)
          ItemPlan.savePlan(
            stack,
            (GridStart until GridEnd).map(getStackInSlot).toArray,
            out
          )
      }
    }
  }

  def clearGrid(id: Int): Unit = {
    world.getEntityByID(id) match {
      case p: EntityPlayer =>
        p.openContainer match {
          // The entity id comes from the client, so verify the container the
          // sender actually has open belongs to this bench.
          case c: ContainerProjectBench if c.tile eq this =>
            c.transferAllFromGrid()
          case _ =>
        }
      case _ =>
    }
  }

  override def markDirty(): Unit = {
    super.markDirty()
    recipeNeedsUpdate = true
  }

  override def onBlockRemoval(): Unit = {
    super.onBlockRemoval()
    dropInvContents(world, x, y, z)
  }

  override def openGui(player: EntityPlayer): Unit = {
    GuiProjectBench.open(player, createContainer(player), _.writeCoord(x, y, z))
  }

  override def createContainer(player: EntityPlayer) =
    new ContainerProjectBench(player, this)
}

class SlotProjectCrafting(
    player: EntityPlayer,
    tile: TileProjectBench,
    idx: Int,
    x: Int,
    y: Int
) extends SlotCrafting(player, tile.invCrafting, tile.invResult, idx, x, y)
    with TSlot3 {
  import ProjectBenchSlots._

  /** Scratch grid used to test a craft without touching tile.invCrafting.
    * canTakeStack runs on both sides and on every slot click, so it must not
    * mutate state the rest of the machine reads.
    */
  private val testGrid = new InventoryCrafting(new NodeContainer, 3, 3)

  /** Copy of every slot searchFor is allowed to consume from, in PoolOrder. The
    * copies mean a failed search costs nothing.
    */
  private def snapshotPool(): Array[ItemStack] = PoolOrder.map { i =>
    val s = tile.getStackInSlot(i)
    if (s != null) s.copy else null
  }

  override def canTakeStack(player: EntityPlayer): Boolean = {
    if (tile.currentRecipe == null) return false
    // canRemoveDelegate is copied from super because of an obfuscation bug
    if (!tile.isPlanRecipe && !canRemoveDelegate()) return false

    // searchFor ends with recipe.matches against the grid it just filled, so
    // this is the whole predicate. Deliberately the same pool and the same
    // call onPickupFromSlot makes, so the two can never disagree.
    searchFor(
      testGrid,
      player.worldObj,
      tile.currentRecipe,
      tile.currentInputs,
      snapshotPool()
    )
  }

  override def onPickupFromSlot(
      player: EntityPlayer,
      stack: ItemStack
  ): Unit = {
    if (tile.currentRecipe == null) return

    val pool = snapshotPool()

    // canTakeStack runs the identical check, so this should never fail;
    // bail out rather than hand out remainders for a craft that did not happen.
    if (
      !searchFor(
        tile.invCrafting,
        player.worldObj,
        tile.currentRecipe,
        tile.currentInputs,
        pool
      )
    ) return

    // Commit the consumption. PoolOrder(k) is the tile slot pool(k) came from.
    for (k <- pool.indices) {
      val s = pool(k)
      tile.setInventorySlotContents(
        PoolOrder(k),
        if (s == null || s.stackSize <= 0) null else s
      )
    }

    FMLCommonHandler
      .instance()
      .firePlayerCraftingEvent(
        player,
        stack,
        tile.invCrafting
      )
    onCrafting(stack)

    for (i <- GridStart until GridEnd) {
      // invCrafting holds what was actually consumed, so this is the container
      // item of the eaten ingredient (empty bucket, bottle, tool, ...).
      val remainder = getRemaining(i, tile.invCrafting)

      if (remainder != null) {
        // Grid recipes put the container item back where the ingredient was,
        // but only if consuming the ingredient emptied that slot.
        if (!tile.isPlanRecipe && tile.getStackInSlot(i) == null)
          tile.setInventorySlotContents(i, remainder)
        // Otherwise into storage, then the player, then the floor.
        else if (
          !tryAddToStorageSlots(remainder) &&
          !player.inventory.addItemStackToInventory(remainder)
        )
          player.dropPlayerItemWithRandomChoice(remainder, false)
      }
    }

    tile.updateRecipeNow()
  }

  def tryAddToStorageSlots(stack: ItemStack): Boolean = {
    // Merge into partially filled storage slots first.
    var j = StorageStart
    while (j < StorageEnd) {
      val slotStack = tile.getStackInSlot(j)
      if (
        slotStack != null && slotStack.isItemEqual(stack) &&
        ItemStack.areItemStackTagsEqual(slotStack, stack) &&
        slotStack.stackSize < slotStack.getMaxStackSize
      ) {
        val toAdd =
          Math.min(
            slotStack.getMaxStackSize - slotStack.stackSize,
            stack.stackSize
          )
        slotStack.stackSize += toAdd
        stack.stackSize -= toAdd
        // stackSize changed in place, so nothing else marks the tile dirty.
        tile.markDirty()
        if (stack.stackSize <= 0) return true
      }
      j += 1
    }

    // Then any empty slot.
    j = StorageStart
    while (j < StorageEnd) {
      if (tile.getStackInSlot(j) == null) {
        tile.setInventorySlotContents(j, stack)
        return true
      }
      j += 1
    }

    false
  }

  def getRemaining(i: Int, inv: InventoryCrafting): ItemStack = {
    val stack = inv.getStackInSlot(i)
    if (stack == null) return null

    val item = stack.getItem
    if (item != null && item.hasContainerItem(stack))
      item.getContainerItem(stack)
    else null
  }

  /** Tries to satisfy every ingredient of `recipe` out of `pool`, writing what
    * it took into `target` and decrementing `pool` in place.
    *
    * `pool` is decremented but `target` is only meaningful if this returns
    * true. Pass a scratch inventory as `target` to test a craft; pass
    * tile.invCrafting to actually perform one.
    */
  def searchFor(
      target: InventoryCrafting,
      world: World,
      recipe: IRecipe,
      inputs: Array[ItemStack],
      pool: Array[ItemStack]
  ): Boolean = {
    // Plan recipes start at the front of storage and leave the cursor where it
    // is between ingredients, so several items can come off the same stack.
    //
    // Grid recipes start just before the grid section and step forward before
    // every ingredient, so input i is taken from grid slot i where possible and
    // wraps into storage when that slot cannot supply it.
    var cursor = if (tile.isPlanRecipe) 0 else GridOffset - 1

    def advance(): Int = { cursor = (cursor + 1) % pool.length; cursor }

    // Recipe-level, not per-ingredient: a recipe counts as an ore recipe as
    // soon as one of its nine inputs is an ore-dictionary entry, so this only
    // gates the fallback below, never the exact match.
    val oreRecipe =
      recipe.isInstanceOf[ShapedOreRecipe] || recipe
        .isInstanceOf[ShapelessOreRecipe]

    // Walk the ring once from `start`. On a hit the cursor stays on the slot
    // that supplied the item; on a miss it ends back on `start`, so a second
    // scan can pick up where this one began.
    def scan(want: ItemStack, start: Int, allowOre: Boolean): ItemStack = {
      cursor = start
      do {
        val candidate = pool(cursor)
        if (candidate != null && ingredientMatch(want, candidate, allowOre)) {
          candidate.stackSize -= 1
          if (candidate.stackSize <= 0) pool(cursor) = null

          val taken = candidate.copy()
          taken.stackSize = 1
          return taken
        }
      } while (advance() != start)
      null
    }

    def eat(want: ItemStack): ItemStack = {
      if (!tile.isPlanRecipe) advance()
      val start = cursor

      // Exact items always win. An ore-dictionary substitute is only
      // considered once nothing exact is left, so the plain ItemStack
      // ingredients of an ore recipe still get the item they asked for.
      // Anything this fallback grabs wrongly is caught by the matches() call
      // at the end, which is what actually authorises the craft.
      val exact = scan(want, start, allowOre = false)
      if (exact != null) exact
      else if (oreRecipe) scan(want, start, allowOre = true)
      else null
    }

    var i = GridStart
    while (i < GridEnd) {
      val want = inputs(i)
      if (want == null) {
        // Clear, so a stale entry from an earlier search cannot satisfy matches.
        target.setInventorySlotContents(i, null)
      } else {
        val taken = eat(want)
        if (taken == null) return false
        target.setInventorySlotContents(i, taken)
      }
      i += 1
    }

    recipe.matches(target, world)
  }

  private def ingredientMatch(
      want: ItemStack,
      candidate: ItemStack,
      allowOre: Boolean
  ): Boolean = {
    val eq = new ItemEquality
    eq.matchMeta = !want.isItemStackDamageable
    eq.matchNBT = true
    eq.matchOre = allowOre
    eq.matches(ItemKey.get(want), ItemKey.get(candidate))
  }

  // Following 3 methods copy-pasted from TSlot3 for obfuscation issues
  override def getSlotStackLimit: Int = slotLimitCalculator()
  override def isItemValid(stack: ItemStack): Boolean = canPlaceDelegate(stack)
  override def onSlotChanged(): Unit = {
    super.onSlotChanged()
    slotChangeDelegate()
    slotChangeDelegate2()
  }
}

class ContainerProjectBench(player: EntityPlayer, val tile: TileProjectBench)
    extends NodeContainer {
  import ProjectBenchSlots._

  {
    for (((x, y), i) <- GuiLib.createSlotGrid(48, 18, 3, 3, 0, 0).zipWithIndex)
      addSlotToContainer(new Slot3(tile, i + GridStart, x, y))

    for (((x, y), i) <- GuiLib.createSlotGrid(8, 76, 9, 2, 0, 0).zipWithIndex)
      addSlotToContainer(new Slot3(tile, i + StorageStart, x, y))

    val plan = new Slot3(tile, PlanSlot, 17, 36)
    plan.canPlaceDelegate = { _.getItem.isInstanceOf[ItemPlan] }
    plan.slotLimitCalculator = { () => 1 }
    addSlotToContainer(plan)

    val output = new SlotProjectCrafting(player, tile, OutputSlot, 143, 36)
    output.canPlaceDelegate = { _ => false }
    addSlotToContainer(output)

    addPlayerInv(player, 8, 126)
  }

  def transferAllFromGrid(): Unit = {
    for (i <- GridStart until GridEnd)
      if (getSlot(i).getHasStack)
        transferStackInSlot(player, i)
    detectAndSendChanges()
  }

  override def transferStackInSlot(player: EntityPlayer, i: Int): ItemStack = {
    if (i == OutputSlot && !getSlot(OutputSlot).canTakeStack(player))
      null
    else
      super.transferStackInSlot(player, i)
  }

  override def doMerge(stack: ItemStack, from: Int): Boolean = {
    if ((GridStart until GridEnd).contains(from)) // crafting grid
      {
        if (tryMergeItemStack(stack, StorageStart, StorageEnd, false))
          return true // merge to storage
        if (tryMergeItemStack(stack, PlayerInvStart, PlayerInvEnd, false))
          return true // merge to inventory
      } else if ((StorageStart until StorageEnd).contains(from)) // storage
      {
        if (stack.getItem.isInstanceOf[ItemPlan]) {
          if (
            getSlot(PlanSlot).getStack != null && ItemKey.get(
              getSlot(PlanSlot).getStack
            ) != ItemKey.get(stack)
          )
            transferStackInSlot(player, PlanSlot) // transfer existing stack

          if (tryMergeItemStack(stack, PlanSlot, OutputSlot, false))
            return true // merge to plan
        }
        if (tryMergeItemStack(stack, PlayerInvStart, PlayerInvEnd, false))
          return true // merge to inventory
      } else if (from == PlanSlot) // plan slot
      {
        if (tryMergeItemStack(stack, StorageStart, StorageEnd, true))
          return true // merge to storage
        if (tryMergeItemStack(stack, PlayerInvStart, PlayerInvEnd, false))
          return true // merge to inventory
      } else if (from == OutputSlot) // output slot
      {
        if (tryMergeItemStack(stack, PlayerInvStart, PlayerInvEnd, true))
          return true // merge to inventory
        if (tryMergeItemStack(stack, StorageStart, StorageEnd, true))
          return true // merge to storage
      } else if (
      (PlayerInvStart until PlayerInvEnd).contains(from)
    ) // player inventory
      {
        if (stack.getItem.isInstanceOf[ItemPlan]) {
          if (
            getSlot(PlanSlot).getStack != null && ItemKey.get(
              getSlot(PlanSlot).getStack
            ) != ItemKey.get(stack)
          )
            transferStackInSlot(player, PlanSlot) // transfer existing stack

          if (tryMergeItemStack(stack, PlanSlot, OutputSlot, false))
            return true // merge to plan
        }
        if (tryMergeItemStack(stack, StorageStart, StorageEnd, false))
          return true // merge to storage
      }

    false
  }
}

class GuiProjectBench(tile: TileProjectBench, c: ContainerProjectBench)
    extends NodeGui(c, 176, 208) {
  {
    val write = new IconButtonNode {
      override def drawButton(mouseover: Boolean): Unit = {
        PRResources.guiProjectbench.bind()
        GuiDraw.drawTexturedModalRect(position.x, position.y, 176, 0, 14, 14)
      }
    }
    write.position = Point(18, 56)
    write.size = Size(14, 14)
    write.clickDelegate = { () => tile.sendWriteButtonAction() }
    addChild(write)

    val clear = new IconButtonNode {
      override def drawButton(mouseover: Boolean): Unit = {
        PRResources.guiProjectbench.bind()
        GuiDraw.drawTexturedModalRect(position.x, position.y, 176, 15, 8, 8)
      }
    }
    clear.position = Point(37, 17)
    clear.size = Size(8, 8)
    clear.clickDelegate = { () =>
      tile.sendClearGridAction(Minecraft.getMinecraft.thePlayer.getEntityId)
    }
    addChild(clear)
  }

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    PRResources.guiProjectbench.bind()
    GuiDraw.drawTexturedModalRect(0, 0, 0, 0, size.width, size.height)

    if (tile.isPlanRecipe)
      for (
        ((x, y), i) <- GuiLib.createSlotGrid(48, 18, 3, 3, 0, 0).zipWithIndex
      ) {
        val stack = tile.currentInputs(i)
        if (stack != null) {
          GuiDraw.drawRect(x, y, 16, 16, Colors.GREY.argb)
          ItemDisplayNode.renderItem(
            Point(x, y),
            Size(16, 16),
            zPosition,
            false,
            stack
          )
        }
      }

    GuiDraw.drawString(
      I18n.format("gui.projectred.expansion.machine2|10.title"),
      8,
      6,
      Colors.GREY.argb,
      false
    )
    GuiDraw.drawString(
      I18n.format("container.inventory"),
      8,
      116,
      Colors.GREY.argb,
      false
    )
  }
}

object GuiProjectBench extends TGuiBuilder {
  override def getID = ExpansionProxy.projectbenchGui

  @SideOnly(Side.CLIENT)
  override def buildGui(player: EntityPlayer, data: MCDataInput) = {
    WorldLib.getTileEntity(player.worldObj, data.readCoord()) match {
      case t: TileProjectBench =>
        new GuiProjectBench(t, t.createContainer(player))
      case _ => null
    }
  }
}

object RenderProjectBench extends TCubeMapRender {
  var bottom: IIcon = _
  var top: IIcon = _
  var side1: IIcon = _
  var side2: IIcon = _

  var iconT: UVTransformation = _

  override def getData(w: IBlockAccess, x: Int, y: Int, z: Int) = (0, 0, iconT)
  override def getInvData = (0, 0, iconT)

  override def getIcon(side: Int, meta: Int) = side match {
    case 0 => bottom
    case 1 => top
    case _ => side1
  }

  override def registerIcons(reg: IIconRegister): Unit = {
    bottom = reg.registerIcon("projectred:mechanical/projectbench/bottom")
    top = reg.registerIcon("projectred:mechanical/projectbench/top")
    side1 = reg.registerIcon("projectred:mechanical/projectbench/side1")
    side2 = reg.registerIcon("projectred:mechanical/projectbench/side2")

    iconT = new MultiIconTransformation(bottom, top, side1, side1, side2, side2)
  }
}
