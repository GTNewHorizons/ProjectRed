package mrtjp.projectred.fabrication.circuitparts.wire

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{
  CircuitPart,
  TPoweredCircuitPart,
  TICRSAcquisitions,
  TRSPropagatingICPart
}
import net.minecraft.nbt.NBTTagCompound

abstract class RedwireICPart
    extends WireICPart
    with TICRSAcquisitions
    with TRSPropagatingICPart
    with IRedwireICPart {
  var signal: Byte = 0

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("signal", signal)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    signal = tag.getByte("signal")
  }

  override def writeDesc(out: MCDataOutput) {
    super.writeDesc(out)
    out.writeByte(signal)
  }

  override def readDesc(in: MCDataInput) {
    super.readDesc(in)
    signal = in.readByte()
  }

  override def read(in: MCDataInput, key: Int) = key match {
    case 10 => signal = in.readByte()
    case _  => super.read(in, key)
  }

  override def onSignalUpdate() {
    super.onSignalUpdate()
    writeStreamOf(10).writeByte(signal)
  }

  override def discoverOverride(r: Int, part: CircuitPart) = part match {
    case pow: TPoweredCircuitPart => pow.canConnectRS(rotFromStraight(r))
    case _                        => super.discoverOverride(r, part)
  }

  override def canConnectRS(r: Int) = ICPropagator.redwiresConnectable

  override def getRedwireSignal(r: Int) = getSignal

  override def getSignal = signal & 0xff

  override def setSignal(sig: Int) {
    signal = sig.toByte
  }

  override def rsOutputLevel(r: Int) =
    if (ICPropagator.redwiresProvidePower && maskConnects(r))
      (signal & 0xff) + 16
    else 0

  override def canConnectPart(part: CircuitPart, r: Int) = part match {
    case re: IICRedwireEmitter   => true
    case pc: TPoweredCircuitPart => true
    case _                       => false
  }

  override def resolveSignal(part: Any, r: Int) = part match {
    case t: IRedwireICPart if t.diminishOnSide(r) => t.getRedwireSignal(r) - 1
    case t: IICRedwireEmitter                     => t.getRedwireSignal(r)
    case t: TPoweredCircuitPart                   => t.rsOutputLevel(r)
    case _                                        => 0
  }

  override def calculateSignal = {
    var s = 0
    ICPropagator.redwiresProvidePower = false

    def raise(sig: Int) {
      if (sig > s) s = sig
    }

    for (r <- 0 until 4) if (maskConnects(r)) raise(calcSignal(r))
    ICPropagator.redwiresProvidePower = true
    s
  }

  @SideOnly(Side.CLIENT)
  override def getRolloverData(detailLevel: Int) = {
    val data = Seq.newBuilder[String]

    import net.minecraft.util.EnumChatFormatting._
    if (detailLevel >= 3)
      data += GRAY + "signal: 0x" + Integer.toHexString(signal & 0xff)
    else if (detailLevel >= 2)
      data += GRAY + "state: " + (if (signal != 0) "high" else "low")

    super.getRolloverData(detailLevel) ++ data.result()
  }
}
