/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.expansion

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import codechicken.lib.render.uv.{MultiIconTransformation, UVTransformation}
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui.{
  GuiLib,
  IconButtonNode,
  ItemDisplayNode,
  NodeContainer,
  NodeGui,
  TGuiBuilder,
  TSlot3,
  Slot3
}
import mrtjp.core.inventory.TInventory
import mrtjp.core.item.{ItemEquality, ItemKey}
import mrtjp.core.render.TCubeMapRender
import mrtjp.core.vec.{Point, Size}
import mrtjp.core.world.WorldLib
import mrtjp.projectred.ProjectRedExpansion
import mrtjp.projectred.core.libmc.PRResources
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister
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
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.oredict.{ShapedOreRecipe, ShapelessOreRecipe}

import java.util.{List => JList}
import scala.collection.JavaConversions._

class TileProjectBench
    extends TileMachine
    with TInventory
    with ISidedInventory
    with TGuiMachine {

  val invCrafting = new InventoryCrafting(new NodeContainer, 3, 3)
  val invCraftingResult = new InventoryCraftResult

  var isPlanRecipe = false
  var tRecipe: IRecipe = _

  var tInputs = new Array[ItemStack](9)
  private var tOutputs: Null = _

  private var recipeNeedsUpdate = true

  /* if the function searchFor used to check the recipe validation (maybe), so it will not give to
   * ForgeHooks.onPlayerTossEvent in eatRecipe to drop the container if the inventory of the bench is full */
  var isSearch = false

  override def save(tag: NBTTagCompound): Unit = {
    super.save(tag)
    saveInv(tag)
  }

  override def load(tag: NBTTagCompound): Unit = {
    super.load(tag)
    loadInv(tag)
  }

  override def read(in: MCDataInput, key: Int): Unit = key match {
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

  override def getBlock: BlockMachine = ProjectRedExpansion.machine2

  override def doesRotate = false
  override def doesOrient = false

  override def size = 28 // 0-8 crafting, 9-26 ingredients, 27 plan, 28 result
  override def name = "project_bench"

  override def canExtractItem(slot: Int, item: ItemStack, side: Int): Boolean =
    9 until 27 contains slot
  override def canInsertItem(slot: Int, item: ItemStack, side: Int): Boolean =
    9 until 27 contains slot
  override def getAccessibleSlotsFromSide(side: Int): Array[Int] =
    (9 until 27).toArray

  override def update(): Unit = { updateRecipeIfNeeded() }
  override def updateClient(): Unit = { updateRecipeIfNeeded() }

  private def updateRecipeIfNeeded(): Unit = {
    if (!recipeNeedsUpdate) return
    recipeNeedsUpdate = false
    updateRecipe()
  }

  def updateRecipe(): Unit = {
    isPlanRecipe = false
    tRecipe = null
    tInputs.transform(_ => null)
    tOutputs = null
    invCraftingResult.setInventorySlotContents(0, null)

    if ((0 until 9).exists(getStackInSlot(_) != null)) {
      for (i <- 0 until 9)
        invCrafting.setInventorySlotContents(i, getStackInSlot(i))
      matchAndSetRecipe()
    } else {
      val plan = getStackInSlot(27)
      if (plan != null && ItemPlan.hasRecipeInside(plan)) {
        val inputs = ItemPlan.loadPlanInputs(plan)
        for (i <- 0 until 9) invCrafting.setInventorySlotContents(i, inputs(i))
        matchAndSetRecipe()
        if (tRecipe != null) isPlanRecipe = true
      }
    }

    def matchAndSetRecipe(): Unit = {
      val recipes =
        CraftingManager.getInstance().getRecipeList.asInstanceOf[JList[IRecipe]]
      tRecipe = recipes.find(_.matches(invCrafting, world)).orNull
      if (tRecipe != null) {
        invCraftingResult.setInventorySlotContents(
          0,
          tRecipe.getCraftingResult(invCrafting)
        )
        for (i <- 0 until 9)
          tInputs(i) = {
            val s = invCrafting.getStackInSlot(i)
            if (s != null) s.copy else null
          }
      }
    }
  }

  private def writePlan(): Unit = {
    if (tRecipe != null && !isPlanRecipe) {
      val out = invCraftingResult.getStackInSlot(0)
      if (out != null) {
        val stack = getStackInSlot(27)
        if (stack != null)
          ItemPlan.savePlan(stack, (0 until 9).map(getStackInSlot).toArray, out)
      }
    }
  }

  private def clearGrid(id: Int): Unit = {
    world.getEntityByID(id) match {
      case p: EntityPlayer =>
        p.openContainer match {
          case c: ContainerProjectBench =>
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
) extends SlotCrafting(
      player,
      tile.invCrafting,
      tile.invCraftingResult,
      idx,
      x,
      y
    )
    with TSlot3 {
  override def canTakeStack(player: EntityPlayer): Boolean = {
    if (tile.isPlanRecipe) {
      val storage = (9 until 27).map { i =>
        val s = tile.getStackInSlot(i)
        if (s != null) s.copy else null
      }.toArray

      return searchFor(
        player.worldObj,
        tile.tRecipe,
        tile.tInputs,
        storage
      )
    }

    // copied from super for obfuscation bug
    canRemoveDelegate()
  }

  override def onPickupFromSlot(
      player: EntityPlayer,
      stack: ItemStack
  ): Unit = {
    onCrafting(stack)

    val storage = ((9 until 27) ++ (0 until 9)).map { i =>
      val s = tile.getStackInSlot(i)
      if (s != null) s.copy else null
    }.toArray

    tile.isSearch = true

    if (
      searchFor(
        player.worldObj,
        tile.tRecipe,
        tile.tInputs,
        storage
      )
    ) {
      val orderedStorage = storage.drop(18) ++ storage.take(18)
      for (i <- orderedStorage.indices) {
        val stack = orderedStorage(i)
        if (stack == null || stack.stackSize <= 0)
          tile.setInventorySlotContents(i, null)
        else tile.setInventorySlotContents(i, stack)
      }
    }

    val invCrafting = new InventoryCrafting(new NodeContainer, 3, 3)
    for (i <- 0 until 9)
      invCrafting.setInventorySlotContents(i, tile.tInputs(i))
    FMLCommonHandler
      .instance()
      .firePlayerCraftingEvent(player, stack, invCrafting)

    tile.updateRecipe()
  }

  private var start = 0
  private var end = 0

  private def searchFor(
      world: World,
      recipe: IRecipe,
      inputs: Array[ItemStack],
      storage: Array[ItemStack]
  ): Boolean = {

    if (tile.isPlanRecipe) { // search in resources inventory
      start = 0
      end = 18
    } else { // search in crafting grid inventory
      start = 18
      end = 27
    }

    val invCrafting = new InventoryCrafting(new NodeContainer, 3, 3)
    for (i <- 0 until 9) {
      val item = inputs(i)
      if (item != null) {
        if (!eatResource(recipe, item, storage)) return false
        invCrafting.setInventorySlotContents(i, item)
      }
    }
    recipe.matches(invCrafting, world)
  }

  private def getValidStorageIndex(
      storage: Array[ItemStack],
      cStack: ItemStack
  ): Int = {
    var emptyIndex = -1
    for (i <- 0 until 18) {
      if (storage(i) == null && emptyIndex == -1) {
        emptyIndex = i
      } else if (storage(i) != null && storage(i).isItemEqual(cStack)) {
        if (!storage(i).isStackable)
          return i
        else if (storage(i).stackSize < storage(i).getMaxStackSize)
          return i
      }
    }
    emptyIndex
  }

  private def getValidGridIndex(
      storage: Array[ItemStack],
      cStack: ItemStack
  ): Int = {
    for (i <- 18 until 27) {
      if (
        storage(i) != null && storage(i)
          .isItemEqual(cStack) && !storage(i).isStackable
      ) {
        return i
      }
    }
    -1
  }

  // Eat resource from inventory
  private def eatResource(
      recipe: IRecipe,
      stackIn: ItemStack,
      storage: Array[ItemStack]
  ): Boolean = {
    for (i <- start until end) {
      if (!tile.isPlanRecipe) {
        start += 1
      }

      if (storage(i) != null && ingredientMatch(recipe, stackIn, storage(i))) {
        if (storage(i).getItem.hasContainerItem(storage(i))) {
          val cStack = storage(i).getItem.getContainerItem(storage(i))
          var j = getValidStorageIndex(storage, cStack)
          if (j == -1) {
            if (!tile.isSearch && cStack.isStackable) {
              ForgeHooks.onPlayerTossEvent(player, cStack, false)
            } else if (!cStack.isStackable) {
              j = getValidGridIndex(storage, cStack)
              if (j != -1) {
                storage(j) = cStack
                return true
              } else {
                return false
              }
            }
          } else if (storage(i).isStackable) {
            if (storage(j) == null)
              storage(j) = cStack
            else
              storage(j).stackSize += 1
          } else { // if not stackable
            storage(i) = cStack
            return true
          }
        }

        // "eat" a peace of stack-able item and then check if to send to oblivion :>
        storage(i).stackSize -= 1
        if (storage(i).stackSize == 0)
          storage(i) = null

        return true
      }
    }
    false
  }

  /** Match one ingredient from storage, and match it with recipe input
    * @param stackIn
    *   : one of the recipe inputs
    * @param stackSt
    *   : one of the storage inputs
    */
  private def ingredientMatch(
      recipe: IRecipe,
      stackIn: ItemStack,
      stackSt: ItemStack
  ) = {
    val eq = new ItemEquality
    eq.matchMeta = !stackIn.isItemStackDamageable
    eq.matchNBT = false
    eq.matchOre = recipe.isInstanceOf[ShapedOreRecipe] || recipe
      .isInstanceOf[ShapelessOreRecipe]
    eq.matches(ItemKey.get(stackIn), ItemKey.get(stackSt))
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

class ContainerProjectBench(player: EntityPlayer, tile: TileProjectBench)
    extends NodeContainer {
  {
    for (((x, y), i) <- GuiLib.createSlotGrid(48, 18, 3, 3, 0, 0).zipWithIndex)
      addSlotToContainer(new Slot3(tile, i, x, y))

    for (((x, y), i) <- GuiLib.createSlotGrid(8, 76, 9, 2, 0, 0).zipWithIndex)
      addSlotToContainer(new Slot3(tile, i + 9, x, y))

    val plan = new Slot3(tile, 27, 17, 36)
    plan.canPlaceDelegate = { _.getItem.isInstanceOf[ItemPlan] }
    plan.slotLimitCalculator = { () => 1 }
    addSlotToContainer(plan)

    val output = new SlotProjectCrafting(player, tile, 28, 143, 36)
    output.canPlaceDelegate = { _ => false }
    addSlotToContainer(output)

    addPlayerInv(player, 8, 126)
  }

  def transferAllFromGrid(): Unit = {
    for (i <- 0 until 9)
      if (getSlot(i).getHasStack)
        transferStackInSlot(player, i)
    detectAndSendChanges()
  }

  override def slotClick(
      id: Int,
      mouse: Int,
      shift: Int,
      player: EntityPlayer
  ): ItemStack = {
    var mode = shift
    if (id == 28 && mode == 6) mode = 0
    super.slotClick(id, mouse, mode, player)
  }

  override def doMerge(stack: ItemStack, from: Int): Boolean = {
    if (0 until 9 contains from) // crafting grid
      {
        if (tryMergeItemStack(stack, 9, 27, reverse = false))
          return true // merge to storage
        if (tryMergeItemStack(stack, 29, 65, reverse = false))
          return true // merge to inventory
      } else if (9 until 27 contains from) // storage
      {
        if (stack.getItem.isInstanceOf[ItemPlan]) {
          if (
            getSlot(27).getStack != null && ItemKey.get(
              getSlot(27).getStack
            ) != ItemKey.get(stack)
          )
            transferStackInSlot(player, 27) // transfer existing stack

          if (tryMergeItemStack(stack, 27, 28, reverse = false))
            return true // merge to plan
        }
        if (tryMergeItemStack(stack, 29, 65, reverse = false))
          return true // merge to inventory
      } else if (from == 27) // plan slot
      {
        if (tryMergeItemStack(stack, 9, 27, reverse = true))
          return true // merge to storage
        if (tryMergeItemStack(stack, 29, 65, reverse = false))
          return true // merge to inventory
      } else if (from == 28) // output slot
      {
        if (tryMergeItemStack(stack, 29, 65, reverse = true))
          return true // merge to inventory
        if (tryMergeItemStack(stack, 9, 27, reverse = true))
          return true // merge to storage
      } else if (29 until 65 contains from) // player inventory
      {
        if (stack.getItem.isInstanceOf[ItemPlan]) {
          if (
            getSlot(27).getStack != null && ItemKey.get(
              getSlot(27).getStack
            ) != ItemKey.get(stack)
          )
            transferStackInSlot(player, 27) // transfer existing stack

          if (tryMergeItemStack(stack, 27, 28, reverse = false))
            return true // merge to plan
        }
        if (tryMergeItemStack(stack, 9, 27, reverse = false))
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

  override def drawBack_Impl(mouse: Point, rFrame: Float): Unit = {
    PRResources.guiProjectbench.bind()
    GuiDraw.drawTexturedModalRect(0, 0, 0, 0, size.width, size.height)

    if (tile.isPlanRecipe)
      for (
        ((x, y), i) <- GuiLib.createSlotGrid(48, 18, 3, 3, 0, 0).zipWithIndex
      ) {
        val stack = tile.tInputs(i)
        if (stack != null) {
          GuiDraw.drawRect(x, y, 16, 16, Colors.GREY.argb)
          ItemDisplayNode.renderItem(
            Point(x, y),
            Size(16, 16),
            zPosition,
            drawNumber = false,
            stack
          )
        }
      }

    GuiDraw.drawString("Project Bench", 8, 6, Colors.GREY.argb, false)
    GuiDraw.drawString("Inventory", 8, 116, Colors.GREY.argb, false)
  }
}

object GuiProjectBench extends TGuiBuilder {
  override def getID: Int = ExpansionProxy.projectbenchGui

  @SideOnly(Side.CLIENT)
  override def buildGui(
      player: EntityPlayer,
      data: MCDataInput
  ): GuiProjectBench = {
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

  override def getData(
      w: IBlockAccess,
      x: Int,
      y: Int,
      z: Int
  ): (Int, Int, UVTransformation) = (0, 0, iconT)
  override def getInvData: (Int, Int, UVTransformation) = (0, 0, iconT)

  override def getIcon(side: Int, meta: Int): IIcon = side match {
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
