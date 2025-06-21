package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import net.minecraft.nbt.NBTTagCompound

trait TComplexICGateLogic[T <: TComplexGateICPart] extends ICGateLogic[T] {
  def save(tag: NBTTagCompound) {}

  def load(tag: NBTTagCompound) {}

  def readDesc(packet: MCDataInput) {}

  def writeDesc(packet: MCDataOutput) {}

  /** Allocated keys > 10
    */
  def read(packet: MCDataInput, key: Int) {}
}
