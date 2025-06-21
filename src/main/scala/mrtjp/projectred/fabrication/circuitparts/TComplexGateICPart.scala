package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import net.minecraft.nbt.NBTTagCompound

trait TComplexGateICPart extends GateICPart {
  def getLogicComplex = getLogic[TComplexICGateLogic[TComplexGateICPart]]

  def assertLogic()

  abstract override def save(tag: NBTTagCompound) {
    super.save(tag)
    getLogicComplex.save(tag)
  }

  abstract override def load(tag: NBTTagCompound) {
    super.load(tag)
    assertLogic()
    getLogicComplex.load(tag)
  }

  abstract override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    getLogicComplex.writeDesc(packet)
  }

  abstract override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    assertLogic()
    getLogicComplex.readDesc(packet)
  }

  abstract override def read(packet: MCDataInput, key: Int) = key match {
    case k if k > 10 =>
      assertLogic() // this may be a net dump part
      getLogicComplex.read(packet, k)
    case _ => super.read(packet, key)
  }

  abstract override def preparePlacement(
      rotation: Int,
      configuration: Int,
      meta: Int
  ) {
    super.preparePlacement(rotation, configuration, meta)
    assertLogic()
  }
}
