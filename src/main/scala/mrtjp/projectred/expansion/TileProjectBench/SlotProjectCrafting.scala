package mrtjp.projectred.expansion.TileProjectBench

import cpw.mods.fml.common.FMLCommonHandler
import mrtjp.core.gui.{NodeContainer, TSlot3}
import mrtjp.core.item.ItemKey
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{InventoryCrafting, SlotCrafting}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.world.World
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.oredict.{ShapedOreRecipe, ShapelessOreRecipe}

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

  private val INVALID_INDEX = -1
  private val previous_indices = Array[Int](
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX,
    INVALID_INDEX
  )

  /** Check if the stack1 can be stacked and is equal to stack2 */
  private def isStackableEqual(
      stack1: ItemStack,
      stack2: ItemStack
  ): Boolean = {
    if (stack1 != null && stack2 != null)
      stack1.isItemEqual(stack2) && stack1.stackSize < stack1.getMaxStackSize
    else false
  }

  /** Check if the stack has container */
  private def hasContainer(
      stack: ItemStack
  ): Boolean = {
    stack.getItem.hasContainerItem(stack)
  }

  /** Return the container of the stack */
  private def getContainer(
      stack: ItemStack
  ): ItemStack = {
    stack.getItem.getContainerItem(stack)
  }

  override def canTakeStack(player: EntityPlayer): Boolean = {
    if (tile.isPlanRecipe) {
      val storage = tile.getInventoryCopy
      return {
        val (search, _) =
          searchFor(player.worldObj, tile.tRecipe, tile.tInputs, storage)
        search
      }
    }
    // copied from super for obfuscation bug
    canRemoveDelegate()
  }

  override def onPickupFromSlot(
      player: EntityPlayer,
      stack: ItemStack
  ): Unit = {
    onShiftFromSlot(player, stack, isShift = false)
  }

  def onShiftFromSlot(
      player: EntityPlayer,
      stack: ItemStack,
      isShift: Boolean
  ): Unit = {
    onCrafting(stack)

    // search for crafting possibility
    val storage = tile.getInventoryCopy

    val (search, itemToDrops) = searchFor(
      player.worldObj,
      tile.tRecipe,
      tile.tInputs,
      storage
    )

    // do if the crafting is possible
    if (search) {
      // if the player do shift + click
      if (isShift) {
        if (stack.isStackable)
          setItemStackInStorage(
            storage,
            stack,
            INVALID_INDEX,
            getIndexOfStackableItemStackInResourceStorage
          )
        else
          setItemStackInStorage(
            storage,
            stack,
            INVALID_INDEX,
            getIndexOfEmptySlotInResourceStorage
          )
      }

      // set all slots to a new value and drop items if they exist */
      for (i <- 0 until 27) {
        // checks to stackSize <= 0 no need because it's already done in [[decreaseStackSize]]
        tile.setInventorySlotContents(i, storage(i))
        if (i < 9) // because itemToDrops.size is 9
          // drop items to ground
          ForgeHooks.onPlayerTossEvent(player, itemToDrops(i), false)
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

  /** Used in [[searchFor]] to set the start of the loop used in [[eatResource]]
    */
  private var start = 0

  /** Used in [[searchFor]] to set the end of the loop used [[eatResource]] */
  private var end = 0

  /** Returns the search interval depending on [[tile.isPlanRecipe]] value
    * @return
    *   if true, the interval for resource storage <br> if false, the interval
    *   for crafting grid
    */
  private def setSearchInterval(): Unit = {
    if (tile.isPlanRecipe) {
      start = 9
      end = 27
    } else {
      start = 0
      end = 9
    }
  }

  /** Search for the possibility to craft an item
    * @param world
    *   current world of Project Bench
    * @param recipe
    *   for checking
    * @param inputs
    *   recipe inputs
    * @param storage
    *   Project Bench inventory copy
    * @return
    *   [[Boolean]] result of the valid recipe crafting
    * @return
    *   [[Array]] array of items to drop to ground
    */
  private def searchFor(
      world: World,
      recipe: IRecipe,
      inputs: Array[ItemStack],
      storage: Array[ItemStack]
  ): (Boolean, Array[ItemStack]) = {
    val itemToDrops = new Array[ItemStack](9)
    itemToDrops.transform(_ => null)

    setSearchInterval()

    val invCrafting = new InventoryCrafting(new NodeContainer, 3, 3)
    for (i <- 0 until 9) {
      val stack = inputs(i)
      if (stack != null) {
        val (eaten, (itemToDrop, previous_index)) =
          eatResource(recipe, storage, stack, previous_indices(i))
        if (!eaten)
          return (false, itemToDrops)
        itemToDrops(i) = itemToDrop
        previous_indices(i) = previous_index
        invCrafting.setInventorySlotContents(i, stack)
      } else {
        previous_indices(i) = INVALID_INDEX
      }
    }
    (recipe.matches(invCrafting, world), itemToDrops)
  }

  /** Get the storage index for stackable item stack, if the stackable item
    * stack was not find returns the first empty slot index in this case if they
    * are not any valid index return [[INVALID_INDEX]]
    * @param previous_index
    *   is the previous index of the input
    */
  private def getIndexOfStackableItemStackInResourceStorage(
      storage: Array[ItemStack],
      stack: ItemStack,
      previous_index: Int
  ): Int = {
    if (
      previous_index != INVALID_INDEX && isStackableEqual(
        storage(previous_index),
        stack
      )
    ) previous_index
    else {
      var emptyIndex = INVALID_INDEX
      var i = 9;
      while (i < 27) {
        if (isStackableEqual(storage(i), stack)) return i
        else if (storage(i) == null && emptyIndex == INVALID_INDEX)
          emptyIndex = i
        i += 1
      }
      emptyIndex
    }
  }

  /** Get the index of the first empty slot, if they are not any, returns
    * [[INVALID_INDEX]], stack is unused here, but the function used in
    * [[setItemStackInStorage]]
    */
  private def getIndexOfEmptySlotInResourceStorage(
      storage: Array[ItemStack],
      stack: ItemStack,
      previous: Int
  ): Int = {
    var i = 9;
    while (i < 27) {
      if (storage(i) == null) return i
      i += 1
    }
    INVALID_INDEX
  }

  /** Decrease the stack size of item stack in the i slot of the storage, and
    * null if the decreased stack size <= 0.
    * @param storage
    *   the copy of the Project Bench storage
    * @param i
    *   the slot index of the storage
    */
  private def decreaseStackSize(storage: Array[ItemStack], i: Int): Unit = {
    storage(i).stackSize -= 1
    if (storage(i).stackSize <= 0) storage(i) = null
  }

  /** Set item stack in Project Bench storage copy with getStackStorageIndex
    * @param storage
    *   the copy of Project Bench storage
    * @param stack
    *   the stack to set in storage
    * @param getStackStorageIndex
    *   the function used to find a valid index for setting the stack in storage
    * @return
    *   stack, [[INVALID_INDEX]] if the index of getStackStorageIndex is
    *   [[INVALID_INDEX]] or null and index if the index is valid
    */
  private def setItemStackInStorage(
      storage: Array[ItemStack],
      stack: ItemStack,
      previous_index: Int,
      getStackStorageIndex: (Array[ItemStack], ItemStack, Int) => Int
  ): (ItemStack, Int) = {
    val j = getStackStorageIndex(storage, stack, previous_index)
    if (j == INVALID_INDEX) {
      return (stack, j)
    } else {
      if (storage(j) == null) storage(j) = stack
      else storage(j).stackSize += 1
    }
    (null, j)
  }

  /** Use an item from recipe and compare with the Project Bench copied storage
    * to "eat"
    * @param recipe
    *   for matching ingredients
    * @param storage
    *   the copy of Project Bench inventory
    * @param stack
    *   item from recipe to eat
    * @return
    *   [[Boolean]] can be item eaten or not <br> [[ItemStack]] item to drop in
    *   the world, because [[searchFor]] also used in [[canTakeStack]] function,
    *   to check something
    */
  private def eatResource(
      recipe: IRecipe,
      storage: Array[ItemStack],
      stack: ItemStack,
      previous_index: Int
  ): (Boolean, (ItemStack, Int)) = {
    for (i <- start until end) {
      if (!tile.isPlanRecipe) {
        start += 1
      }
      if (storage(i) != null && ingredientMatch(recipe, stack, storage(i))) {
        if (hasContainer(storage(i))) {
          val cStack = getContainer(storage(i))
          if (storage(i).isStackable) {
            if (cStack.isStackable) {
              decreaseStackSize(storage, i)
              return (
                true,
                setItemStackInStorage(
                  storage,
                  cStack,
                  previous_index,
                  getIndexOfStackableItemStackInResourceStorage
                )
              )
            } else { // cStack is not stackable
              decreaseStackSize(storage, i)
              return (
                true,
                setItemStackInStorage(
                  storage,
                  cStack,
                  previous_index,
                  getIndexOfEmptySlotInResourceStorage
                )
              )
            }
          } else { // storage(i) is not stackable
            if (cStack.isStackable) {
              decreaseStackSize(storage, i)
              return (
                true,
                setItemStackInStorage(
                  storage,
                  cStack,
                  previous_index,
                  getIndexOfStackableItemStackInResourceStorage
                )
              )
            } else { // cStack is not stackable
              storage(i) = cStack
              return (true, (null, i))
            }
          }
        } else {
          // "eat" a peace of stack-able item and then check if to send to oblivion :>
          decreaseStackSize(storage, i)
          return (true, (null, i))
        }
      }
    }
    (false, (null, INVALID_INDEX))
  }

  /** Match one ingredient from storage, and match it with recipe input
    * @param recipe
    *   : the recipe current input
    * @param stackIn
    *   : one of the recipe inputs
    * @param stackSt
    *   : one of the storage inputs
    * @return
    *   result of matching
    */
  private def ingredientMatch(
      recipe: IRecipe,
      stackIn: ItemStack,
      stackSt: ItemStack
  ) = {
    tile.eq.matchMeta = !stackIn.isItemStackDamageable
    tile.eq.matchNBT = false
    tile.eq.matchOre = recipe.isInstanceOf[ShapedOreRecipe] || recipe
      .isInstanceOf[ShapelessOreRecipe]
    tile.eq.matches(ItemKey.get(stackIn), ItemKey.get(stackSt))
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
