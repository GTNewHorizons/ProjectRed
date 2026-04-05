package mrtjp.projectred

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event._
import mrtjp.projectred.api.ProjectRedAPI
import mrtjp.projectred.fabrication.ICPropagator
import mrtjp.projectred.transmission._
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack

@Mod(
  modid = "ProjRed|Transmission",
  dependencies = "required-after:ProjRed|Core",
  modLanguage = "scala",
  acceptedMinecraftVersions = "[1.7.10]",
  name = "ProjectRed Transmission",
  version = ProjectRedCore.VERSION
)
object ProjectRedTransmission {
  ProjectRedAPI.transmissionAPI = new APIImpl_Transmission

  /** Multipart items * */
  var itemPartWire: ItemPartWire = null
  var itemPartFramedWire: ItemPartFramedWire = null

  var tabTransmission = new CreativeTabs("trans") {
    override def getIconItemStack = new ItemStack(
      ProjectRedTransmission.itemPartWire
    )
    override def getTabIconItem = getIconItemStack.getItem
  }

  @Mod.EventHandler
  def preInit(event: FMLPreInitializationEvent) {
    TransmissionProxy.versionCheck()
    TransmissionProxy.preinit()
  }

  @Mod.EventHandler
  def init(event: FMLInitializationEvent) {
    TransmissionProxy.init()
  }

  @Mod.EventHandler
  def postInit(event: FMLPostInitializationEvent) {
    TransmissionProxy.postinit()
  }

  @Mod.EventHandler
  def serverStopping(event: FMLServerStoppedEvent) {
    WirePropagator.reset()
    ICPropagator.reset()
  }
}
