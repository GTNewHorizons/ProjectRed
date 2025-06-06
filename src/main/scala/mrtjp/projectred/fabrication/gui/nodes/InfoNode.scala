/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.gui._
import mrtjp.core.vec.{Point, Rect, Size}
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.fabrication.gui.GuiICWorkbench
import net.minecraft.client.gui.Gui


class InfoNode extends TNode {
  val size = Size(18, 18)
  override def frame = Rect(position, size)

  private def getTile = parent.asInstanceOf[GuiICWorkbench].tile

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    PRResources.guiPrototyper.bind()
    if (!getTile.hasBP)
      Gui.func_146110_a(
        position.x,
        position.y,
        330,
        0,
        size.width,
        size.height,
        512,
        512
      )
  }

  override def drawFront_Impl(mouse: Point, rframe: Float) {
    val text =
      if (!getTile.hasBP)
        "Lay down a blueprint on the workbench."
      else ""
    if (text.nonEmpty && rayTest(mouse)) {
      translateToScreen()
      val Point(mx, my) = parent.convertPointToScreen(mouse)
      import scala.collection.JavaConversions._
      GuiDraw.drawMultilineTip(mx + 12, my - 12, Seq(text))
      translateFromScreen()
    }
  }
}
