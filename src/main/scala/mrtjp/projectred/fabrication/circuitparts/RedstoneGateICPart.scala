package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import mrtjp.projectred.fabrication.circuitparts.wire.IICRedwireEmitter
import net.minecraft.nbt.NBTTagCompound

abstract class RedstoneGateICPart
    extends GateICPart
    with TICRSAcquisitions
    with TPoweredCircuitPart {

  /** Mapped inputs and outputs of the gate. OOOO IIII High nybble is output.
    * Low nybble is input
    */
  private var gateState: Byte = 0

  def state = gateState & 0xff

  def setState(s: Int) {
    gateState = s.toByte
  }

  def getLogicRS = getLogic[RedstoneICGateLogic[RedstoneGateICPart]]

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("state", gateState)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    gateState = tag.getByte("state")
  }

  override def writeDesc(out: MCDataOutput) {
    super.writeDesc(out)
    out.writeByte(gateState)
  }

  override def readDesc(in: MCDataInput) {
    super.readDesc(in)
    gateState = in.readByte()
  }

  override def read(in: MCDataInput, key: Int) = key match {
    case 5 => gateState = in.readByte()
    case _ => super.read(in, key)
  }

  def sendStateUpdate() {
    writeStreamOf(5).writeByte(gateState)
  }

  def onInputChange() {
    sendStateUpdate()
  }

  def onOutputChange(mask: Int) {
    world.network.markSave()
    sendStateUpdate()
    notify(toAbsoluteMask(mask))
  }

  override def rsOutputLevel(r: Int): Int = {
    val ir = toInternal(r)
    if ((getLogicRS.outputMask(shape) & 1 << ir) != 0)
      getLogicRS.getOutput(this, ir)
    else 0
  }

  override def canConnectRS(r: Int) = getLogicRS.canConnect(this, toInternal(r))

  def getRedstoneInput(r: Int) = calcSignal(toAbsolute(r))

  override def resolveSignal(part: Any, r: Int) = part match {
    case re: IICRedwireEmitter   => re.getRedwireSignal(r)
    case ip: TPoweredCircuitPart => ip.rsOutputLevel(r)
    case _                       => 0
  }
}
