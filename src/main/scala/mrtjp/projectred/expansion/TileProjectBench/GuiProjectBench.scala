package mrtjp.projectred.expansion.TileProjectBench

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui.{
  GuiLib,
  IconButtonNode,
  ItemDisplayNode,
  NodeGui,
  TGuiBuilder
}
import mrtjp.core.vec.{Point, Size}
import mrtjp.core.world.WorldLib
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.expansion.ExpansionProxy
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer

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
