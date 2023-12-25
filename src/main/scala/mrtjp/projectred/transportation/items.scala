package mrtjp.projectred.transportation

import java.util.{List => JList}

import codechicken.lib.vec.{BlockCoord, Vector3}
import codechicken.multipart.{MultiPartRegistry, TItemMultiPart}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.gui.{GuiLib, NodeContainer, Slot3}
import mrtjp.core.inventory.SimpleInventory
import mrtjp.core.item.{ItemCore, ItemDefinition, TItemGlassSound}
import mrtjp.projectred.ProjectRedTransportation
import mrtjp.projectred.core._
import mrtjp.projectred.transportation.ChipType.ChipType
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.{EnumChatFormatting, IIcon}
import net.minecraft.world.World
import org.lwjgl.input.Keyboard

import scala.collection.mutable.ListBuffer

class ItemPartPipe
    extends ItemCore("projectred.transportation.pipe")
    with TItemMultiPart
    with TItemGlassSound {
  setHasSubtypes(true)
  setCreativeTab(ProjectRedTransportation.tabTransportation)

  def newPart(
      item: ItemStack,
      player: EntityPlayer,
      world: World,
      pos: BlockCoord,
      side: Int,
      vhit: Vector3
  ) = {
    val pdef = PipeDefs.values(item.getItemDamage)
    val p = MultiPartRegistry
      .createPart(pdef.partname, false)
      .asInstanceOf[PayloadPipePart[_]]
    if (p != null) p.preparePlacement(side, item.getItemDamage)
    p
  }

  @SideOnly(Side.CLIENT)
  override def getSubItems(item: Item, tab: CreativeTabs, list: JList[ItemStack]) {
    val l2 = list
    for (t <- PipeDefs.values) l2.add(t.makeStack)
  }

  override def registerIcons(reg: IIconRegister) {
    for (p <- PipeDefs.values) p.registerIcon(reg)
  }

  @SideOnly(Side.CLIENT)
  override def getSpriteNumber = 0
}

object PipeDefs extends ItemDefinition {
  override type EnumVal = PipeVal
  override def getItem = ProjectRedTransportation.itemPartPipe

  val BASIC = new PipeVal("pr_pipe", "basic", "rs")
  val ROUTEDJUNCTION = new PipeVal(
    "pr_rbasic",
    "routedjunc",
    "routed",
    "unrouted",
    "routedconn",
    "unroutedconn"
  )
  val ROUTEDINTERFACE = new PipeVal("pr_rinterface", "routedint")
  val ROUTEDCRAFTING = new PipeVal("pr_rcrafting", "routedcrafting")

  /** deprecated * */
  val ROUTEDREQUEST = new PipeVal("pr_rrequest", "routedrequest")
  val ROUTEDEXTENSION = new PipeVal("pr_rextension", "routedextension")

  /** deprecated * */
  val ROUTEDFIREWALL = new PipeVal("pr_rfire", "routedfire")
  val PRESSURETUBE = new PipeVal(
    "pr_pt",
    Seq("pressuretube") ++ (0 to 15 map { "colour/colour_" + _ }): _*
  )
  val RESISTANCETUBE = new PipeVal("pr_rpt", "resistancetube")
  val NETWORKVALVE = new PipeVal(
    "pr_netvalve",
    "netvalve_blocked",
    "netvalve_in",
    "netvalve_out",
    "netvalve_inout"
  )
  val NETWORKLATENCY = new PipeVal("pr_netlatency", "netlatency")

  class PipeVal(val partname: String, val textures: String*) extends ItemDef {
    val sprites = new Array[IIcon](textures.length)

    def registerIcon(reg: IIconRegister) {
      if (textures.nonEmpty)
        for (i <- 0 until textures.length)
          sprites(i) =
            reg.registerIcon("projectred:mechanical/pipes/" + textures(i))
    }
  }
}

class ItemRoutingChip
    extends ItemCore("projectred.transportation.routingchip") {
  setHasSubtypes(true)
  setCreativeTab(ProjectRedTransportation.tabTransportation)

  override def getSubItems(i: Item, tab: CreativeTabs, list: JList[ItemStack]) {
    val list2 = list
    for (c <- RoutingChipDefs.values) list2.add(c.makeStack)
  }

  @SideOnly(Side.CLIENT)
  override def registerIcons(reg: IIconRegister) {
    for (c <- RoutingChipDefs.values) c.registerIcons(reg)
  }

  override def getIconFromDamage(meta: Int) = {
    val c = RoutingChipDefs.values(meta)
    if (c != null) c.icon
    else null
  }

  override def addInformation(
      stack: ItemStack,
      player: EntityPlayer,
      list: JList[String],
      par4: Boolean
  ) {
    val list2 = list
    if (
      Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(
        Keyboard.KEY_RSHIFT
      )
    ) if (ItemRoutingChip.hasChipInside(stack)) {
      import scala.collection.JavaConversions._
      val r = ItemRoutingChip.loadChipFromItemStack(stack)
      val s = new ListBuffer[String]
      r.infoCollection(s)
      list2.addAll(s)
    } else list2.add(EnumChatFormatting.GRAY + "not configured")
  }

  override def onItemRightClick(
      stack: ItemStack,
      w: World,
      player: EntityPlayer
  ): ItemStack = {
    if (!w.isRemote && ItemRoutingChip.isValidChip(stack)) {
      val r = ItemRoutingChip.loadChipFromItemStack(stack)
      r.openGui(player)
    }
    super.onItemRightClick(stack, w, player)
  }

  override def onItemUse(
      stack: ItemStack,
      player: EntityPlayer,
      w: World,
      par4: Int,
      par5: Int,
      par6: Int,
      par7: Int,
      par8: Float,
      par9: Float,
      par10: Float
  ): Boolean = {
    if (!w.isRemote && ItemRoutingChip.isValidChip(stack)) {
      val r = ItemRoutingChip.loadChipFromItemStack(stack)
      r.openGui(player)
    }
    true
  }

  override def doesSneakBypassUse(
      world: World,
      x: Int,
      y: Int,
      z: Int,
      player: EntityPlayer
  ) = true
}

object ItemRoutingChip {
  def assertStackTag(stack: ItemStack) {
    if (!stack.hasTagCompound) stack.setTagCompound(new NBTTagCompound)
  }

  def isValidChip(stack: ItemStack) = {
    stack != null && stack.getItem.isInstanceOf[ItemRoutingChip] &&
    RoutingChipDefs.getForStack(stack) != null
  }

  def hasChipInside(stack: ItemStack) = {
    isValidChip(stack) && stack.hasTagCompound && stack.getTagCompound.hasKey(
      "chipROM"
    )
  }

  def saveChipToItemStack(stack: ItemStack, chipset: RoutingChip) {
    assertStackTag(stack)
    val tag1 = stack.getTagCompound
    val tag2 = new NBTTagCompound
    chipset.save(tag2)
    tag1.setTag("chipROM", tag2)
  }

  def loadChipFromItemStack(stack: ItemStack) = {
    val e = RoutingChipDefs.getForStack(stack)
    val chip = e.createChipset
    if (stack.hasTagCompound && stack.getTagCompound.hasKey("chipROM"))
      chip.load(stack.getTagCompound.getCompoundTag("chipROM"))
    chip
  }
}

object RoutingChipDefs extends ItemDefinition {
  type EnumVal = ChipVal
  override def getItem = ProjectRedTransportation.itemRoutingChip

  val ITEMRESPONDER = new ChipVal("responder", new ChipItemResponder)
  val DYNAMICITEMRESPONDER =
    new ChipVal("dyn_responder", new ChipDynamicItemResponder)
  val ITEMOVERFLOWRESPONDER =
    new ChipVal("overflow", new ChipItemOverflowResponder)
  val ITEMTERMINATOR = new ChipVal("terminator", new ChipItemTerminator)
  val ITEMEXTRACTOR = new ChipVal("extractor", new ChipExtractor)
  val ITEMBROADCASTER = new ChipVal("broadcaster", new ChipBroadcaster)
  val ITEMSTOCKKEEPER = new ChipVal("stock_keeper", new ChipStockKeeper)
  val ITEMCRAFTING =
    new ChipVal("crafting", new ChipCrafting, ChipType.CRAFTING)
  val ITEMEXTENSION = new ChipVal("extension", new ChipCraftingExtension)

  def getForStack(stack: ItemStack) = {
    if (
      stack != null && stack.getItem.isInstanceOf[ItemRoutingChip] &&
      values.isDefinedAt(stack.getItemDamage)
    ) values(stack.getItemDamage)
    else null
  }

  class ChipVal(iconPath: String, f: => RoutingChip, cType: ChipType)
      extends ItemDef {
    def this(icon: String, f: => RoutingChip) =
      this(icon, f, ChipType.INTERFACE)

    var icon: IIcon = null
    def registerIcons(reg: IIconRegister) {
      icon = reg.registerIcon("projectred:mechanical/" + iconPath)
    }

    def isInterfaceChip = cType == ChipType.INTERFACE
    def isCraftingChip = cType == ChipType.CRAFTING

    def createChipset = f
  }

}

object ChipType extends Enumeration {
  type ChipType = Value
  val INTERFACE, CRAFTING = Value
}

class ItemRouterUtility
    extends ItemCore("projectred.transportation.routerutil") {
  setMaxStackSize(1)
  setTextureName("projectred:mechanical/router_util")
  setCreativeTab(ProjectRedTransportation.tabTransportation)

  override def onItemRightClick(
      stack: ItemStack,
      w: World,
      player: EntityPlayer
  ) = {
    super.onItemRightClick(stack, w, player)
  }

  override def onItemUse(
      stack: ItemStack,
      player: EntityPlayer,
      w: World,
      par4: Int,
      par5: Int,
      par6: Int,
      par7: Int,
      par8: Float,
      par9: Float,
      par10: Float
  ) = {
    true
  }
}

class ChipUpgradeContainer(player: EntityPlayer) extends NodeContainer {
  val upgradeInv = new SimpleInventory(7, "upBus", 1) {
    override def isItemValidForSlot(i: Int, stack: ItemStack) = {
      if (i == 6)
        stack != null &&
        stack.getItem.isInstanceOf[ItemRoutingChip] &&
        stack.hasTagCompound && stack.getTagCompound.hasKey("chipROM")
      else if (stack.getItem.isInstanceOf[ItemPart]) {
        val slotForMeta = stack.getItemDamage - PartDefs.CHIPUPGRADE_LX.meta
        slotForMeta == i
      } else false
    }

    override def markDirty() {
      super.markDirty()
      refreshChips()
    }
  }

  val slot = player.inventory.currentItem

  {
    var s = 0
    def next = { s += 1; s - 1 }

    for ((x, y) <- GuiLib.createSlotGrid(8, 18, 1, 3, 2, 2))
      addSlotToContainer(new Slot3(upgradeInv, next, x, y))
    for ((x, y) <- GuiLib.createSlotGrid(152, 18, 1, 3, 2, 2))
      addSlotToContainer(new Slot3(upgradeInv, next, x, y))

    addSlotToContainer(new Slot3(upgradeInv, next, 80, 38))

    addPlayerInv(player, 8, 86)
  }

  override def onContainerClosed(p: EntityPlayer) {
    super.onContainerClosed(p)
    for (i <- 0 until upgradeInv.getSizeInventory)
      if (upgradeInv.getStackInSlot(i) != null) {
        p.dropPlayerItemWithRandomChoice(upgradeInv.getStackInSlot(i), false)
        upgradeInv.setInventorySlotContents(i, null)
      }
    upgradeInv.markDirty()
  }

  override def addSlotToContainer(slot: Slot) = {
    super.addSlotToContainer(slot)
    if (slot.getSlotIndex == this.slot && slot.inventory == player.inventory)
      slot.asInstanceOf[Slot3].canRemoveDelegate = { () => false }
    slot
  }

  // TODO better way to handle this cached chip (currently only used for gui's rendering to avoid creating one every frame)
  var cachedChip: RoutingChip = null
  private def refreshChips() {
    val stack = upgradeInv.getStackInSlot(6)
    cachedChip =
      if (stack != null && ItemRoutingChip.isValidChip(stack))
        ItemRoutingChip.loadChipFromItemStack(stack)
      else null
  }

  def containsChipStack() = upgradeInv.getStackInSlot(6) != null

  def install() {
    detectAndSendChanges()
  }
}
