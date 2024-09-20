package mrtjp.projectred.expansion.TileProjectBench

import codechicken.lib.render.uv.{MultiIconTransformation, UVTransformation}
import mrtjp.core.render.TCubeMapRender
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess

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
