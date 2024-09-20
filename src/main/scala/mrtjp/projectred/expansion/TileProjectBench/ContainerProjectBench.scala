package mrtjp.projectred.expansion.TileProjectBench

import mrtjp.core.gui.{GuiLib, NodeContainer, Slot3}
import mrtjp.core.item.ItemKey
import mrtjp.projectred.expansion.ItemPlan
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText

class ContainerProjectBench(player: EntityPlayer, tile: TileProjectBench)
    extends NodeContainer {

  var output: SlotProjectCrafting = _
  var plan: Slot3 = _

  {
    for (((x, y), i) <- GuiLib.createSlotGrid(48, 18, 3, 3, 0, 0).zipWithIndex)
      addSlotToContainer(new Slot3(tile, i, x, y))

    for (((x, y), i) <- GuiLib.createSlotGrid(8, 76, 9, 2, 0, 0).zipWithIndex)
      addSlotToContainer(new Slot3(tile, i + 9, x, y))

    plan = new Slot3(tile, 27, 17, 36)
    plan.canPlaceDelegate = { _.getItem.isInstanceOf[ItemPlan] }
    plan.slotLimitCalculator = { () => 1 }
    addSlotToContainer(plan)

    output = new SlotProjectCrafting(player, tile, 28, 143, 36)
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

  /** Do thing when clicked to slot LB => mouse 0, shift 0 LB + lshift => mouse
    * 0, shift 1 LB + rshift => mouse 0, shift 1
    *
    * @param id
    *   is the slot id
    * @param mouse
    *   the mouse code, where 1 is the right mouse
    * @param shift
    *   left or right shift code, where 1 is the left shift
    * @param player
    *   the current player clicking button
    * @return
    *   [[ItemStack]] of the slot
    */
  override def slotClick(
      id: Int,
      mouse: Int,
      shift: Int,
      player: EntityPlayer
  ): ItemStack = {
    var mode = shift
    if (id == 28) {
      if (mode == 6) mode = 0
      else if (mode == 1 && mouse == 0) {
        if (tile.isStorageNotFull) {
          val stack = output.getStack
          while (output.canTakeStack(player))
            output.onShiftFromSlot(player, stack, isShift = true)
        } else
          player.addChatComponentMessage(
            new ChatComponentText("§cStorage is full!")
          )
        return null
      }
    }
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
