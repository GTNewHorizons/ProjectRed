/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.expansion.TileProjectBench

import codechicken.lib.data.MCDataInput
import mrtjp.core.gui._
import mrtjp.core.inventory.TInventory
import mrtjp.core.item.ItemEquality
import mrtjp.projectred.ProjectRedExpansion
import mrtjp.projectred.expansion.{
  TileMachine,
  TGuiMachine,
  BlockMachine,
  ItemPlan
}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{
  ISidedInventory,
  InventoryCraftResult,
  InventoryCrafting
}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.{CraftingManager, IRecipe}
import net.minecraft.nbt.NBTTagCompound

import java.util.{List => JList}
import scala.collection.JavaConversions._

class TileProjectBench
    extends TileMachine
    with TInventory
    with ISidedInventory
    with TGuiMachine {

  var eq: ItemEquality = new ItemEquality
  val invCrafting = new InventoryCrafting(new NodeContainer, 3, 3)
  val invCraftingResult = new InventoryCraftResult

  var isPlanRecipe = false
  var tRecipe: IRecipe = _

  var tInputs = new Array[ItemStack](9)
  private var tOutputs: Null = _

  private var recipeNeedsUpdate = true

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

  /** The size of Project Bench with next indices : <br> [[0 - 8]] - crafting
    * grid <br> [[9 - 26]] - resource storage <br> [[27]] - recipe plan slot
    * <br> [[28]] - result slot
    */
  override def size = 28
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
    if (!recipeNeedsUpdate)
      return

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
        invCraftingResult
          .setInventorySlotContents(0, tRecipe.getCraftingResult(invCrafting))
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

  def getInventoryCopy: Array[ItemStack] = {
    (0 until 27).map { i =>
      val s = getStackInSlot(i)
      if (s != null) s.copy else null
    }.toArray
  }

  /** Check if the inventory is empty */
  def isStorageNotFull: Boolean = {
    9 until 27 foreach { i =>
      if (getStackInSlot(i) == null)
        return true
    }
    false
  }
}
