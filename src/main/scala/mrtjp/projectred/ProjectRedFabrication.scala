/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{
  FMLInitializationEvent,
  FMLPostInitializationEvent,
  FMLPreInitializationEvent
}
import mrtjp.projectred.core.Configurator
import mrtjp.projectred.fabrication.{
  BlockICMachine,
  FabricationProxy,
  ItemICBlueprint,
  ItemICChip
}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack

@Mod(
  modid = "ProjRed|Fabrication",
  dependencies = "required-after:ProjRed|Core;" +
    "required-after:ProjRed|Integration;" +
    "required-after:ProjRed|Transmission",
  modLanguage = "scala",
  acceptedMinecraftVersions = "[1.7.10]",
  name = "ProjectRed Fabrication",
  version = ProjectRedCore.VERSION
)
object ProjectRedFabrication {

  def isEnabled = Configurator.module_Fabrication

  /** Blocks * */
  var icBlock: BlockICMachine = null

  /** Items * */
  var itemICBlueprint: ItemICBlueprint = null
  var itemICChip: ItemICChip = null

  var tabFabrication = new CreativeTabs("fab") {
    override def getIconItemStack = new ItemStack(itemICChip)
    override def getTabIconItem = getIconItemStack.getItem
  }

  @Mod.EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    FabricationProxy.versionCheck()
    FabricationProxy.preinit()
  }

  @Mod.EventHandler
  def init(event: FMLInitializationEvent) {
    if (isEnabled)
      FabricationProxy.init()
  }

  @Mod.EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    if (isEnabled)
      FabricationProxy.postinit()
  }
}
