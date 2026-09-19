package mrtjp.projectred.core.libmc

import mrtjp.core.gui.PanNode
import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.gui.Gui
import org.lwjgl.opengl.GL11

/** A PanNode whose scroll bars are drawn from textures/gui/scrollbar.png
  * instead of flat colored rectangles.
  *
  * The texture is 16x8 and holds two 8x8 tiles: the track at x = 0 and the
  * handle at x = 8. Each tile is drawn as a "nine-slice": the 1 pixel border
  * keeps its size and only the inside stretches, so any scrollBarThickness
  * works.
  */
class TexturedPanNode extends PanNode {
  private val TextureWidth = 16
  private val TextureHeight = 8
  private val TileSize = 8
  private val Border = 1

  private val TrackU = 0
  private val HandleU = 8

  override def drawBack_Impl(mouse: Point, rframe: Float) {
    GL11.glColor4f(1, 1, 1, 1)
    PRResources.guiScrollBar.bind()

    if (scrollBarVertical) {
      drawNineSlice(
        Rect(
          Point(position.x + size.width - scrollBarThickness, position.y),
          Size(scrollBarThickness, size.height)
        ),
        TrackU
      )
      drawNineSlice(getScrollBarRight, HandleU)
    }
    if (scrollBarHorizontal) {
      drawNineSlice(
        Rect(
          Point(position.x, position.y + size.height - scrollBarThickness),
          Size(size.width, scrollBarThickness)
        ),
        TrackU
      )
      drawNineSlice(getScrollBarBelow, HandleU)
    }
  }

  private def drawNineSlice(area: Rect, tileU: Int) {
    if (area.width <= 0 || area.height <= 0) return

    // Too small to keep a border, so just stretch the whole tile
    if (area.width < 2 * Border + 1 || area.height < 2 * Border + 1) {
      drawPiece(
        tileU,
        0,
        TileSize,
        TileSize,
        area.x,
        area.y,
        area.width,
        area.height
      )
      return
    }

    val innerTile = TileSize - 2 * Border
    val innerWidth = area.width - 2 * Border
    val innerHeight = area.height - 2 * Border

    val xs = Seq(area.x, area.x + Border, area.x + Border + innerWidth)
    val widths = Seq(Border, innerWidth, Border)
    val ys = Seq(area.y, area.y + Border, area.y + Border + innerHeight)
    val heights = Seq(Border, innerHeight, Border)

    val tileUs = Seq(tileU, tileU + Border, tileU + Border + innerTile)
    val tileWidths = Seq(Border, innerTile, Border)
    val tileVs = Seq(0, Border, Border + innerTile)
    val tileHeights = Seq(Border, innerTile, Border)

    for (row <- 0 until 3; column <- 0 until 3) {
      drawPiece(
        tileUs(column),
        tileVs(row),
        tileWidths(column),
        tileHeights(row),
        xs(column),
        ys(row),
        widths(column),
        heights(row)
      )
    }
  }

  private def drawPiece(
      u: Int,
      v: Int,
      textureWidth: Int,
      textureHeight: Int,
      x: Int,
      y: Int,
      width: Int,
      height: Int
  ) {
    Gui.func_152125_a(
      x,
      y,
      u,
      v,
      textureWidth,
      textureHeight,
      width,
      height,
      TextureWidth,
      TextureHeight
    )
  }
}
