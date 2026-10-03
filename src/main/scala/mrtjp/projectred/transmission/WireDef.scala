package mrtjp.projectred.transmission

import cpw.mods.fml.relauncher.{SideOnly, Side}
import mrtjp.core.item.ItemDefinition
import mrtjp.projectred.ProjectRedTransmission
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon
import net.minecraftforge.oredict.OreDictionary

object WireDef extends ItemDefinition {
  override type EnumVal = WireDef
  override def getItem = ProjectRedTransmission.itemPartWire

  val RED_ALLOY = WireDef("pr_redwire", "pr_fredwire", 0, 0xc80000, "redalloy")

  val INSULATED_0 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/whiteoff",
    "insulated/whiteon"
  )
  val INSULATED_1 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/orangeoff",
    "insulated/orangeon"
  )
  val INSULATED_2 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/magentaoff",
    "insulated/magentaon"
  )
  val INSULATED_3 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/lightblueoff",
    "insulated/lightblueon"
  )
  val INSULATED_4 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/yellowoff",
    "insulated/yellowon"
  )
  val INSULATED_5 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/limeoff",
    "insulated/limeon"
  )
  val INSULATED_6 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/pinkoff",
    "insulated/pinkon"
  )
  val INSULATED_7 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/greyoff",
    "insulated/greyon"
  )
  val INSULATED_8 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/lightgreyoff",
    "insulated/lightgreyon"
  )
  val INSULATED_9 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/cyanoff",
    "insulated/cyanon"
  )
  val INSULATED_10 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/purpleoff",
    "insulated/purpleon"
  )
  val INSULATED_11 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/blueoff",
    "insulated/blueon"
  )
  val INSULATED_12 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/brownoff",
    "insulated/brownon"
  )
  val INSULATED_13 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/greenoff",
    "insulated/greenon"
  )
  val INSULATED_14 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/redoff",
    "insulated/redon"
  )
  val INSULATED_15 = WireDef(
    "pr_insulated",
    "pr_finsulated",
    1,
    0xffffff,
    "insulated/blackoff",
    "insulated/blackon"
  )

  val BUNDLED_N =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/neutral")
  val BUNDLED_0 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/white")
  val BUNDLED_1 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/orange")
  val BUNDLED_2 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/magenta")
  val BUNDLED_3 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/lightblue")
  val BUNDLED_4 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/yellow")
  val BUNDLED_5 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/lime")
  val BUNDLED_6 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/pink")
  val BUNDLED_7 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/grey")
  val BUNDLED_8 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/lightgrey")
  val BUNDLED_9 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/cyan")
  val BUNDLED_10 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/purple")
  val BUNDLED_11 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/blue")
  val BUNDLED_12 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/brown")
  val BUNDLED_13 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/green")
  val BUNDLED_14 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/red")
  val BUNDLED_15 =
    WireDef("pr_bundled", "pr_fbundled", 2, 0xffffff, "bundled/black")

  val POWER_LOWLOAD =
    WireDef("pr_pwrlow", "pr_fpwrlow", 1, 0xffffff, "power/lowload")

  // Groups
  val INSULATED_WIRES = INSULATED_0 to INSULATED_15 toArray
  val BUNDLED_WIRES = BUNDLED_N to BUNDLED_15 toArray

  val oreDictDefinitionInsulated = "projredInsulatedWire"
  val oreDictDefinitionInsFramed = "projredInsFramedWire"
  val oreDictDefinitionBundled = "projredBundledCable"

  def initOreDict() {
    for (w <- INSULATED_WIRES) {
      if (w.hasFramedForm)
        OreDictionary.registerOre(oreDictDefinitionInsFramed, w.makeFramedStack)
      OreDictionary.registerOre(oreDictDefinitionInsulated, w.makeStack)
    }
    for (w <- BUNDLED_WIRES)
      OreDictionary.registerOre(oreDictDefinitionBundled, w.makeStack)
  }

  def apply(
      wireType: String,
      framedType: String,
      thickness: Int,
      itemColour: Int,
      textures: String*
  ) =
    new WireDef(wireType, framedType, thickness, itemColour, textures)

  class WireDef(
      val wireType: String,
      val framedType: String,
      val thickness: Int,
      val itemColour: Int,
      textures: Seq[String]
  ) extends ItemDef {
    def hasWireForm = wireType != null
    def hasFramedForm = framedType != null

    val wireSprites = new Array[IIcon](textures.length)

    @SideOnly(Side.CLIENT)
    def loadTextures(reg: IIconRegister) {
      for (i <- textures.indices)
        wireSprites(i) =
          reg.registerIcon("projectred:integration/" + textures(i))
    }

    def makeFramedStack: ItemStack = makeFramedStack(1)
    def makeFramedStack(i: Int) =
      if (hasFramedForm)
        new ItemStack(ProjectRedTransmission.itemPartFramedWire, i, meta)
      else null
  }
}
