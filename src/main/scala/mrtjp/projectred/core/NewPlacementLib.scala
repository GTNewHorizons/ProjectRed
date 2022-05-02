package mrtjp.projectred.core

import net.minecraft.init.Blocks
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object NewPlacementLib {
  private val wireWhitelist = Seq(Blocks.glowstone, Blocks.piston, Blocks.sticky_piston, Blocks.piston_extension)

  def canPlaceWireOnSide(w: World, x: Int, y: Int, z: Int, side: Int): Boolean = {
    // Always allow on unloaded chunks, because we can't know what is there
    if (!w.blockExists(x, y, z)) return true
    val b = w.getBlock(x, y, z)
    if (b == null) return false
    wireWhitelist.contains(b)
    b.isSideSolid(w, x, y, z, ForgeDirection.getOrientation(side))
  }

  private val gateWhiteList = Seq(Blocks.glass)

  def canPlaceGateOnSide(w: World, x: Int, y: Int, z: Int, side: Int): Boolean = {
    if (!w.blockExists(x, y, z)) return true
    if (canPlaceWireOnSide(w, x, y, z, side)) return true

    val b = w.getBlock(x, y, z)
    if (b == null) return false
    if (gateWhiteList.contains(b)) return true

    false
  }

  def canPlaceTorchOnBlock(w: World, x: Int, y: Int, z: Int): Boolean = {
    if (!w.blockExists(x, y, z)) return true
    val b = w.getBlock(x, y, z)
    if (b == null) return false
    b.canPlaceTorchOnTop(w, x, y, z)
  }

  def canPlaceLight(w: World, x: Int, y: Int, z: Int, side: Int): Boolean = {
    if (canPlaceWireOnSide(w, x, y, z, side)) return true
    if (side == 1 && canPlaceTorchOnBlock(w, x, y, z)) return true

    false
  }
}
