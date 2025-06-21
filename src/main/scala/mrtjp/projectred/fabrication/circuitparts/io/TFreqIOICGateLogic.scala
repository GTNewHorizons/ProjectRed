package mrtjp.projectred.fabrication.circuitparts.io

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import net.minecraft.nbt.NBTTagCompound

trait TFreqIOICGateLogic extends IOICGateLogic {
  var freq = 0

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("freq", freq.toByte)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    freq = tag.getByte("freq")
  }

  override def writeDesc(out: MCDataOutput) {
    super.writeDesc(out)
    out.writeByte(freq)
  }

  override def readDesc(in: MCDataInput) {
    super.readDesc(in)
    freq = in.readUByte()
  }

  override def read(in: MCDataInput, key: Int) = key match {
    case 12 => freq = in.readUByte()
    case _  => super.read(in, key)
  }

  def sendFreqUpdate() {
    gate.writeStreamOf(12).writeByte(freq)
  }

  override def resolveInputFromWorld =
    if ((gate.world.iostate(gate.rotation) & 1 << freq) != 0) 255
    else 0

  override def resolveOutputToWorld =
    if ((gate.world.iostate(gate.rotation) >>> 16 & 1 << freq) != 0) 255
    else 0

  override def setWorldOutput(state: Boolean) {
    val s =
      ((gate.world.iostate(gate.rotation) >>> 16) & ~(1 << freq)) | (if (state)
                                                                       1
                                                                     else
                                                                       0) << freq
    gate.world.setOutput(gate.rotation, s)
  }
}
