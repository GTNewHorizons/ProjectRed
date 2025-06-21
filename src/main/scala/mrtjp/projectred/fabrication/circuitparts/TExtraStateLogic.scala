package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import net.minecraft.nbt.NBTTagCompound

trait TExtraStateLogic extends SequentialICGateLogic {
  private var lState2: Byte = 0

  def state2 = lState2 & 0xff

  def setState2(state: Int) {
    lState2 = state.toByte
  }

  def clientState2 = false

  abstract override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("state2", lState2)
  }

  abstract override def load(tag: NBTTagCompound) {
    super.load(tag)
    lState2 = tag.getByte("state2")
  }

  abstract override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    if (clientState2) packet.writeByte(lState2)
  }

  abstract override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    if (clientState2) lState2 = packet.readByte()
  }

  abstract override def read(packet: MCDataInput, key: Int) = key match {
    case 11 => lState2 = packet.readByte()
    case _  => super.read(packet, key)
  }

  def sendState2Update() {
    gate.writeStreamOf(11).writeByte(lState2)
  }
}
