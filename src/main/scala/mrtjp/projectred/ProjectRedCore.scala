package mrtjp.projectred

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{
  FMLInitializationEvent,
  FMLPostInitializationEvent,
  FMLPreInitializationEvent,
  FMLServerStartingEvent
}
import mrtjp.projectred.core._
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import org.apache.logging.log4j.LogManager

@Mod(
  modid = "ProjRed|Core",
  version = ProjectRedCore.VERSION,
  dependencies = "required-after:Forge;" +
    "required-after:ForgeMultipart;" +
    "required-after:MrTJPCoreMod",
  modLanguage = "scala",
  guiFactory = "mrtjp.projectred.core.GuiConfigFactory",
  acceptedMinecraftVersions = "[1.7.10]",
  name = "ProjectRed Core"
)
object ProjectRedCore {
  val log = LogManager.getFormatterLogger("ProjRed|Core")
  final val VERSION = Tags.VERSION

  /** Items * */
  var itemPart: ItemPart = null
  var itemDrawPlate: ItemDrawPlate = null
  var itemScrewdriver: ItemScrewdriver = null
  var itemWireDebugger: ItemWireDebugger = null
  var itemDataCard: ItemDataCard = null

  var tabCore = new CreativeTabs("core") {
    override def getIconItemStack = new ItemStack(
      ProjectRedCore.itemScrewdriver
    )
    override def getTabIconItem = getIconItemStack.getItem
  }

  @Mod.EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    Configurator.loadConfig()
    CoreProxy.versionCheck()
    CoreProxy.preinit()
  }

  @Mod.EventHandler
  def init(event: FMLInitializationEvent) {
    CoreProxy.init()
  }

  @Mod.EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    CoreProxy.postinit()
  }

  @Mod.EventHandler
  def onServerStarting(event: FMLServerStartingEvent) {}
}
