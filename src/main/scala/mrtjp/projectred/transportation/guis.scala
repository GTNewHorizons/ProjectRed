package mrtjp.projectred.transportation

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import codechicken.lib.packet.PacketCustom
import codechicken.lib.vec.BlockCoord
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui._
import mrtjp.core.item.{ItemKey, ItemKeyStack}
import mrtjp.core.resource.ResourceLib
import mrtjp.core.vec.{Point, Rect, Size, Vec2}
import mrtjp.projectred.core.libmc._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import org.lwjgl.input.Keyboard

class GuiInterfacePipe(container: Container, pipe: RoutedInterfacePipePart)
    extends NodeGui(container, 176, 200) {
  override def drawBack_Impl(mouse: Point, frame: Float) {
    PRResources.guiPipeInterface.bind()
    drawTexturedModalRect(0, 0, 0, 0, xSize, ySize)
    GuiLib.drawPlayerInvBackground(8, 118)
  }

  override def drawFront_Impl(mouse: Point, frame: Float) {
    PRResources.guiPipeInterface.bind()
    val oldZ = zLevel
    zLevel = 300

    for (i <- 0 until 4) {
      val x = 19
      val y = 10 + i * 26
      val u = 178
      val v = if (inventorySlots.getSlot(i).getStack == null) 107 else 85
      drawTexturedModalRect(x, y, u, v, 25, 20)
    }
    zLevel = oldZ
  }
}

object GuiInterfacePipe extends TGuiBuilder {
  override def getID = TransportationProxy.guiIDInterfacePipe

  @SideOnly(Side.CLIENT)
  override def buildGui(player: EntityPlayer, data: MCDataInput) = {
    val coord = data.readCoord()
    PRLib.getMultiPart(player.worldObj, coord, 6) match {
      case pipe: RoutedInterfacePipePart =>
        new GuiInterfacePipe(pipe.createContainer(player), pipe)
      case _ => null
    }
  }
}

class GuiRequester(pipe: IWorldRequester) extends NodeGui(256, 192) {
  private val MAX_COUNT = 999999999
  private val MAX_COUNT_LENGTH = 9

  var clip: ClipNode = null
  var pan: PanNode = null
  var list: ItemListNode = null
  var selectedItem: ItemKey = null

  var itemMap = Map.empty[ItemKey, Int]

  var textFilter: SimpleTextboxNode = null
  var textCount: SimpleTextboxNode = null

  var pull: CheckBoxNode = null
  var craft: CheckBoxNode = null
  var partials: CheckBoxNode = null

  sealed trait SortMode
  case object CountDesc extends SortMode
  case object CountAsc extends SortMode
  case object IDDesc extends SortMode
  case object IDAsc extends SortMode

  var sortMode: SortMode = RequestGuiState.sortMode match {
    case 0 => CountDesc
    case 1 => CountAsc
    case 2 => IDDesc
    case 3 => IDAsc
  }

  {
    clip = new ClipNode
    clip.position = Point(18, 18)
    clip.size = Size(220, 117)
    addChild(clip)

    pan = new PanNode
    pan.size = Size(220, 117)
    pan.scrollBarThickness = 16
    pan.scrollModifier = Vec2(0, 1)
    pan.scrollBarHorizontal = false
    pan.panDelegate = { () => refreshList() }
    clip.addChild(pan)

    list = new ItemListNode
    list.zPosition = -0.01
    list.itemSize = Size(16, 16)
    list.gridWidth = 12
    list.displayNodeFactory = { stack =>
      val d = new ItemDisplayNode
      d.zPosition = -0.01
      d.backgroundColour =
        if (stack.key == selectedItem)
          Colors.LIME.argb(0x44)
        else 0
      d.clickDelegate = { () =>
        selectedItem = stack.key
        refreshList()
      }
      d
    }
    pan.addChild(list)

    textFilter = new SimpleTextboxNode
    textFilter.position = Point(69, 139)
    textFilter.size = Size(118, 16)
    textFilter.phantom = "Search..."
    textFilter.textChangedDelegate = { () => refreshList() }
    addChild(textFilter)

    textCount = new SimpleTextboxNode {
      override def mouseScrolled_Impl(p: Point, dir: Int, consumed: Boolean) = {
        if (!consumed && rayTest(p)) {
          if (dir > 0) countUp()
          else if (dir < 0) countDown()
          true
        } else false
      }
    }
    textCount.position = Point(87, 158)
    textCount.size = Size(50, 14)
    textCount.text = "1"
    textCount.phantom = "1"
    textCount.allowedcharacters = "0123456789"
    textCount.textChangedDelegate = { () =>
      if (textCount.text.length > MAX_COUNT_LENGTH)
        textCount.text = textCount.text.substring(0, MAX_COUNT_LENGTH)
    }
    textCount.focusChangeDelegate = { () =>
      if (!textCount.focused)
        if (textCount.text.isEmpty || Integer.parseInt(textCount.text) < 1)
          textCount.text = "1"
    }
    addChild(textCount)

    pull = CheckBoxNode.centered(203, 150)
    pull.state = RequestGuiState.pull
    pull.clickDelegate = { () =>
      RequestGuiState.pull = pull.state
      askForListRefresh()
    }
    addChild(pull)

    craft = CheckBoxNode.centered(203, 165)
    craft.state = RequestGuiState.craft
    craft.clickDelegate = { () =>
      RequestGuiState.craft = craft.state
      askForListRefresh()
    }
    addChild(craft)

    partials = CheckBoxNode.centered(203, 180)
    partials.state = RequestGuiState.partials
    partials.clickDelegate = { () =>
      RequestGuiState.partials = partials.state
      askForListRefresh()
    }
    addChild(partials)

    val sortButton = new MCButtonNode
    sortButton.position = Point(10, 143)
    sortButton.size = Size(50, 14)

    def sortLabel = sortMode match {
      case CountDesc => "Count ▼"
      case CountAsc  => "Count ▲"
      case IDDesc    => "ID ▼"
      case IDAsc     => "ID ▲"
    }

    sortButton.text = sortLabel

    sortButton.clickDelegate = { () =>
      sortMode = sortMode match {
        case CountDesc => CountAsc
        case CountAsc  => IDDesc
        case IDDesc    => IDAsc
        case IDAsc     => CountDesc
      }

      RequestGuiState.sortMode = sortMode match {
        case CountDesc => 0
        case CountAsc  => 1
        case IDDesc    => 2
        case IDAsc     => 3
      }

      sortButton.text = sortLabel
      askForListRefresh()
    }

    addChild(sortButton)

    val ref = new MCButtonNode
    ref.position = Point(10, 158)
    ref.size = Size(50, 14)
    ref.text = "Refresh"
    ref.clickDelegate = { () => askForListRefresh() }
    addChild(ref)

    val req = new MCButtonNode
    req.position = Point(10, 173)
    req.size = Size(50, 14)
    req.text = "Submit"
    req.clickDelegate = { () => sendItemRequest() }
    addChild(req)

    val down = new MCButtonNode
    down.position = Point(69, 158)
    down.size = Size(14, 14)
    down.text = "-"
    down.clickDelegate = { () => countDown() }
    addChild(down)

    val up = new MCButtonNode
    up.position = Point(141, 158)
    up.size = Size(14, 14)
    up.text = "+"
    up.clickDelegate = { () => countUp() }
    addChild(up)

    val all = new MCButtonNode
    all.position = Point(160, 158)
    all.size = Size(27, 14)
    all.text = "All"
    all.clickDelegate = { () =>
      if (selectedItem != null)
        textCount.text = String.valueOf(Math.max(1, itemMap(selectedItem)))
    }
    addChild(all)
  }

  def refreshList() {

    def filterAllows(stack: ItemKeyStack): Boolean = {
      val searchText = textFilter.text
      if (searchText.isEmpty) return true

      def fallbackStringMatch(name: String, filter: String): Boolean = {
        for (s <- filter.split(" ")) if (!name.contains(s)) return false
        true
      }

      if (!NEISearchFieldHelper.existsSearchField())
        return fallbackStringMatch(stack.key.getName.toLowerCase, searchText)

      Option(NEISearchFieldHelper.getFilter(searchText)) match {
        case Some(filter) =>
          try {
            filter.test(stack.key.makeStack(1))
          } catch {
            case _: Throwable =>
              fallbackStringMatch(stack.key.getName.toLowerCase, searchText)
          }
        case None =>
          fallbackStringMatch(stack.key.getName.toLowerCase, searchText)
      }
    }

    val unsortedItems = itemMap
      .map { case (key, count) => ItemKeyStack.get(key, count) }
      .toSeq
      .filter(filterAllows)

    val itemsSorted = sortMode match {

      case CountDesc =>
        unsortedItems.sortWith { (a, b) =>
          if (a.stackSize == b.stackSize)
            a < b
          else
            a.stackSize > b.stackSize
        }

      case CountAsc =>
        unsortedItems.sortWith { (a, b) =>
          if (a.stackSize == b.stackSize)
            a < b
          else
            a.stackSize < b.stackSize
        }

      case IDDesc =>
        unsortedItems.sorted.reverse

      case IDAsc =>
        unsortedItems.sorted
    }

    list.items = itemsSorted

    list.reset()

    if (!list.items.exists(_.key == selectedItem))
      selectedItem = null
  }

  override def drawBack_Impl(mouse: Point, frame: Float) {
    PRResources.guiPipeRequest.bind()
    GuiDraw.drawTexturedModalRect(0, 0, 0, 0, size.width, size.height)
  }

  override def drawFront_Impl(mouse: Point, frame: Float) {
    GuiDraw.drawString("Pull", 212, 147, Colors.GREY.rgb, false)
    GuiDraw.drawString("Craft", 212, 162, Colors.GREY.rgb, false)
    GuiDraw.drawString("Partial", 212, 177, Colors.GREY.rgb, false)
    drawCountBreakdownHint()
  }

  private def drawCountBreakdownHint() {
    val countHint =
      try {
        val count = textCount.text.toInt
        if (count <= 0) return ""

        val stacks = count / 64
        val remainder = count % 64

        if (stacks > 0 && remainder > 0) {
          s"$stacks × 64 + $remainder"
        } else if (stacks > 0) {
          s"$stacks × 64"
        } else {
          "" // Don't show anything for counts less than 64
        }
      } catch {
        case _: NumberFormatException => ""
      }

    if (countHint.nonEmpty) {
      val textWidth = GuiDraw.getStringWidth(countHint)
      val x =
        87 + (50 - textWidth) / 2 // Center below textCount (87 is textCount x position, 50 is width)
      val y =
        173 + 4 // Below textCount (173 is Submit y position, offset of 3.5 pixels to center it vertically)
      GuiDraw.drawString(countHint, x, y, Colors.GREY.rgb, false)
    }
  }

  override def onAddedToParent_Impl() {
    askForListRefresh()
    list.cullFrame = convertRectToScreen(Rect(Point(18, 18), Size(220, 117)))
  }

  private def sendItemRequest() {
    val count = textCount.text
    if (count.isEmpty) return

    val amount = Integer.parseInt(count)
    if (amount <= 0) return

    val request = selectedItem
    if (request != null) {
      val packet = new PacketCustom(
        TransportationSPH.channel,
        TransportationSPH.gui_Request_submit
      )
      packet.writeCoord(new BlockCoord(pipe.getContainer.tile))
      packet.writeBoolean(pull.state)
      packet.writeBoolean(craft.state)
      packet.writeBoolean(partials.state)
      packet.writeItemStack(request.makeStack(amount), true)
      packet.sendToServer()
    }
  }

  private def askForListRefresh() {
    val packet = new PacketCustom(
      TransportationSPH.channel,
      TransportationSPH.gui_Request_listRefresh
    )
    packet.writeCoord(new BlockCoord(pipe.getContainer.tile))
    packet.writeBoolean(pull.state)
    packet.writeBoolean(craft.state)
    packet.sendToServer()
  }

  private def getModifierIncrement: Int = {
    if (
      Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(
        Keyboard.KEY_RCONTROL
      )
    )
      64
    else if (
      Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(
        Keyboard.KEY_RSHIFT
      )
    )
      10
    else
      1
  }

  private def countUp() {
    var current = 0
    val s = textCount.text
    if (s != null && !s.isEmpty) current = Integer.parseInt(s)

    val newCount = current + getModifierIncrement

    if (newCount <= MAX_COUNT) textCount.text = "" + newCount
  }

  private def countDown() {
    val s = textCount.text
    val current = if (s.nonEmpty) Integer.parseInt(s) else 1

    val newCount = (current - getModifierIncrement) max 1

    textCount.text = "" + newCount
  }

  def receiveContentList(content: Map[ItemKey, Int]) {
    itemMap = content
    refreshList()
  }

  override def keyPressed_Impl(c: Char, keycode: Int, consumed: Boolean) = {
    if (!consumed && keycode == Keyboard.KEY_RETURN) {
      textFilter.setFocused(true)
      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
        textFilter.setText("")
      true
    } else false
  }

  override def mouseScrolled_Impl(p: Point, dir: Int, consumed: Boolean) = {
    if (!consumed && clip.frame.contains(convertPointFromScreen(p))) {
      if (dir > 0) pan.panChildren(Vec2.down * 3)
      else if (dir < 0) pan.panChildren(Vec2.up * 3)
      true
    } else false
  }
}

class GuiFirewallPipe(pipe: RoutedFirewallPipe, c: Container)
    extends NodeGui(c, 176, 184) {
  {
    val excl = new IconButtonNode {
      override def drawButton(mouseover: Boolean) {
        ResourceLib.guiExtras.bind()
        GuiDraw.drawTexturedModalRect(
          position.x,
          position.y,
          if (pipe.filtExclude) 1 else 17,
          102,
          14,
          14
        )
      }
    }
    excl.position = Point(113, 45)
    excl.size = Size(14, 14)
    excl.tooltipBuilder = {
      _ += ("Items are " +
        (if (pipe.filtExclude) "blacklisted" else "whitelisted"))
    }
    excl.clickDelegate = { () => sendMessage(0) }
    addChild(excl)

    def makeButton(x: Int, y: Int, f: => Boolean, desc: String, id: Int) {
      val b = new IconButtonNode {
        override def drawButton(mouseover: Boolean) {
          ResourceLib.guiExtras.bind()
          GuiDraw.drawTexturedModalRect(x, y, if (f) 33 else 49, 134, 14, 14)
        }
      }
      b.position = Point(x, y)
      b.size = Size(14, 14)
      b.tooltipBuilder = { _ += desc }
      b.clickDelegate = { () => sendMessage(id) }
      addChild(b)
    }

    makeButton(150, 28, pipe.allowRoute, "Push routing", 1)
    makeButton(150, 45, pipe.allowBroadcast, "Pulling", 2)
    makeButton(150, 62, pipe.allowCrafting, "Crafting", 3)
  }

  def sendMessage(id: Int) {
    new PacketCustom(
      TransportationCPH.channel,
      TransportationCPH.gui_FirewallPipe_action
    )
      .writeCoord(new BlockCoord(pipe.tile))
      .writeByte(id)
      .sendToServer()
  }

  override def drawBack_Impl(mouse: Point, frame: Float) {
    PRResources.guiPipeFirewall.bind()
    GuiDraw.drawTexturedModalRect(0, 0, 0, 0, size.width, size.height)
    GuiDraw.drawString("Firewall Pipe", 8, 6, Colors.GREY.argb, false)
  }
}

object GuiFirewallPipe extends TGuiBuilder {
  override def getID = TransportationProxy.guiIDFirewallPipe

  @SideOnly(Side.CLIENT)
  override def buildGui(player: EntityPlayer, data: MCDataInput) = {
    PRLib.getMultiPart(player.worldObj, data.readCoord(), 6) match {
      case pipe: RoutedFirewallPipe =>
        pipe.filtExclude = data.readBoolean()
        pipe.allowRoute = data.readBoolean()
        pipe.allowBroadcast = data.readBoolean()
        pipe.allowCrafting = data.readBoolean()
        new GuiFirewallPipe(pipe, pipe.createContainer(player))
      case _ =>
        for (i <- 0 until 4) data.readBoolean()
        null
    }
  }
}

object RequestGuiState {
  var pull = true
  var craft = true
  var partials = false

  var sortMode = 0
}
