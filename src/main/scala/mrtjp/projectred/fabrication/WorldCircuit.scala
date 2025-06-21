package mrtjp.projectred.fabrication

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.packet.PacketCustom
import codechicken.lib.vec.BlockCoord
import mrtjp.core.vec.Point
import mrtjp.projectred.ProjectRedCore.log
import mrtjp.projectred.fabrication.circuitparts.CircuitPart
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.fluids.FluidStack

trait WorldCircuit {
  def getIC: IntegratedCircuit

  def getWorld: World

  def getICStreamOf(key: Int): MCDataOutput

  def getPartStream(x: Int, y: Int): MCDataOutput

  def isRemote: Boolean

  def markSave()
}

object DummyMCIO extends MCDataOutput {
  override def writeVarInt(i: Int) = this
  override def writeCoord(x: Int, y: Int, z: Int) = this
  override def writeCoord(coord: BlockCoord) = this
  override def writeString(s: String) = this
  override def writeFloat(f: Float) = this
  override def writeDouble(d: Double) = this
  override def writeShort(s: Int) = this
  override def writeVarShort(s: Int) = this
  override def writeInt(i: Int) = this
  override def writeFluidStack(liquid: FluidStack) = this
  override def writeByteArray(array: Array[Byte]) = this
  override def writeBoolean(b: Boolean) = this
  override def writeItemStack(stack: ItemStack) = this
  override def writeNBTTagCompound(tag: NBTTagCompound) = this
  override def writeChar(c: Char) = this
  override def writeLong(l: Long) = this
  override def writeByte(b: Int) = this
}

trait SimulatedWorldCircuit extends WorldCircuit {
  override def getICStreamOf(key: Int) = DummyMCIO
  override def getPartStream(x: Int, y: Int) = DummyMCIO
  override def isRemote = false
}

trait NetWorldCircuit extends WorldCircuit {
  private var icStream: PacketCustom = null
  private var partStream: PacketCustom = null

  def createPartStream(): PacketCustom
  def sendPartStream(out: PacketCustom)
  override def getPartStream(x: Int, y: Int): MCDataOutput = {
    if (partStream == null) partStream = createPartStream()

    val part = getIC.getPart(x, y)
    partStream.writeByte(part.id)
    partStream.writeInt(x).writeInt(y)

    partStream
  }
  def flushPartStream() {
    if (partStream != null) {
      partStream.writeByte(255) // terminator
      sendPartStream(partStream.compress())
      partStream = null
    }
  }
  def readPartStream(in: MCDataInput) {
    try {
      var id = in.readUByte()
      while (id != 255) {
        val (x, y) = (in.readInt(), in.readInt())
        var part = getIC.getPart(x, y)
        if (part == null || part.id != id) {
          log.error("client part stream couldnt find part " + Point(x, y))
          part = CircuitPart.createPart(id)
        }
        part.read(in)
        id = in.readUByte()
      }
    } catch {
      case ex: IndexOutOfBoundsException =>
        log.error("Circuit part stream failed to be read.")
        ex.printStackTrace()
    }
  }

  def createICStream(): PacketCustom
  def sendICStream(out: PacketCustom)
  override def getICStreamOf(key: Int): MCDataOutput = {
    if (icStream == null) icStream = createICStream()
    icStream.writeByte(key)
    icStream
  }
  def flushICStream() {
    if (icStream != null) {
      icStream.writeByte(255) // terminator
      sendICStream(icStream.compress())
      icStream = null
    }
  }
  def readICStream(in: MCDataInput) {
    try {
      var id = in.readUByte()
      while (id != 255) {
        getIC.read(in, id)
        id = in.readUByte()
      }
    } catch {
      case ex: IndexOutOfBoundsException =>
        log.error("Circuit IC stream failed to be read")
    }
  }
}
