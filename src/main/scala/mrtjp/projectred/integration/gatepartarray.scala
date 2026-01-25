/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.integration

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec._
import codechicken.multipart.TMultiPart
import mrtjp.projectred.api.IConnectable
import mrtjp.projectred.core.libmc.PRLib
import mrtjp.projectred.core.{Configurator, TFaceOrient}
import mrtjp.projectred.transmission._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

trait TArrayGatePart
    extends RedstoneGatePart
    with IRedwirePart
    with TFaceRSPropagation {
  def getLogicArray = getLogic[TArrayGateLogic[TArrayGatePart]]

  override def getSignal =
    getLogicArray.getSignal(toInternalMask(propagationMask))
  override def setSignal(signal: Int) =
    getLogicArray.setSignal(toInternalMask(propagationMask), signal)

  abstract override def updateAndPropagate(prev: TMultiPart, mode: Int) {
    val rd = sideDiff(prev)
    var uMask = 0
    for (r <- 0 until 4) if ((rd & 1 << r) != 0) {
      val pMask = getLogicArray.propogationMask(toInternal(r))
      if (pMask > 0 && (pMask & uMask) != pMask) {
        propagationMask = toAbsoluteMask(pMask)
        super.updateAndPropagate(prev, mode)
        uMask |= pMask
      }
    }
    if (uMask == 0) WirePropagator.addNeighborChange(new BlockCoord(tile))
    propagationMask = 0xf
  }

  override def propagateOther(mode: Int) {
    val nonConn = ~(connMap | connMap >> 4 | connMap >> 8) & 0xf
    notifyExternals(nonConn & propagationMask)
  }

  def sideDiff(p: TMultiPart): Int = {
    if (!p.isInstanceOf[TFaceOrient] || p.tile == null) return 0xf
    val part = p.asInstanceOf[TFaceOrient]
    val here = new BlockCoord(tile)
    val there = new BlockCoord(part.tile)

    if (here == there && (side & 6) != (part.side & 6))
      return 1 << Rotation.rotationTo(side, part.side)

    if (side != part.side)
      there.offset(side ^ 1) // bring corner up to same plane
    here.sub(there)

    import codechicken.lib.vec.Rotation._
    (here.x, here.y, here.z) match {
      case (0, 1, 0)  => 1 << rotationTo(side, 0)
      case (0, -1, 0) => 1 << rotationTo(side, 1)
      case (0, 0, 1)  => 1 << rotationTo(side, 2)
      case (0, 0, -1) => 1 << rotationTo(side, 3)
      case (1, 0, 0)  => 1 << rotationTo(side, 4)
      case (-1, 0, 0) => 1 << rotationTo(side, 5)
      case _ =>
        throw new RuntimeException(
          "Propagating to distant part from " + here + " to " + there + "!?"
        )
    }
  }

  override def calculateSignal: Int = {
    val ipmask = toInternalMask(propagationMask)
    if (getLogicArray.overrideSignal(ipmask))
      return getLogicArray.calculateSignal(ipmask)

    WirePropagator.setDustProvidePower(false)
    WirePropagator.redwiresProvidePower = false
    var s = 0
    def raise(sig: Int) { if (sig > s) s = sig }

    for (r <- 0 until 4)
      if ((propagationMask & 1 << r) != 0)
        if (maskConnectsCorner(r)) raise(calcCornerSignal(r))
        else if (maskConnectsStraight(r)) raise(calcStraightSignal(r))
        else if (maskConnectsInside(r)) raise(calcInternalSignal(r))
        else raise(calcMaxSignal(r, false, true))

    WirePropagator.setDustProvidePower(true)
    WirePropagator.redwiresProvidePower = true
    s
  }

  abstract override def onChange() {
    super.onChange()
    WirePropagator.propagateTo(this, IWirePart.RISING)
  }

  override def onSignalUpdate() {
    tile.markDirty()
    super.onChange()
    getLogicArray.onSignalUpdate()
  }

  abstract override def resolveSignal(part: Any, r: Int) = part match {
    case re: IRedwirePart if re.diminishOnSide(r) => re.getRedwireSignal(r) - 1
    case _ => super.resolveSignal(part, r)
  }

  override def getRedwireSignal(r: Int) = {
    val ir = toInternal(r)
    val pmask = getLogicArray.propogationMask(ir)
    if (pmask != 0) getLogicArray.getSignal(pmask)
    else getLogicRS.getOutput(this, ir) * 17
  }

  abstract override def canConnectRedstone(side: Int): Boolean = {
    if (super.canConnectRedstone(side)) return true
    if ((side & 6) == (this.side & 6)) return false
    getLogicArray.canConnectRedwire(this, toInternal(absoluteRot(side)))
  }

  def rsLevel(i: Int): Int =
    if (WirePropagator.redwiresProvidePower) (i + 16) / 17
    else 0

  abstract override def weakPowerLevel(side: Int): Int = {
    if ((side & 6) == (this.side & 6)) return 0
    val ir = toInternal(absoluteRot(side))
    if ((getLogicArray.redwireMask(shape) & 1 << ir) != 0)
      return rsLevel(getLogicArray.getSignal(getLogicArray.propogationMask(ir)))
    super.weakPowerLevel(side)
  }

  override def diminishOnSide(r: Int) =
    (getLogicArray.redwireMask(shape) & 1 << toInternal(r)) != 0

  abstract override def rotate() {
    val r = rotation
    setRotation((r + 1) % 4)
    val b = tile.canReplacePart(this, this)
    setRotation(r)
    if (b) super.rotate()
  }

  abstract override def preparePlacement(
      player: EntityPlayer,
      pos: BlockCoord,
      side: Int,
      meta: Int
  ) {
    super.preparePlacement(player, pos, side, meta)
    if (getLogicArray.canCross) {
      val npart = PRLib.getMultiPart(player.worldObj, pos, this.side ^ 1)
      npart match {
        case apart: TArrayGatePart =>
          if (apart.subID == subID && (apart.rotation & 1) == (rotation & 1))
            setRotation((rotation + 1) % 4)
        case _ =>
      }
    }
  }

  abstract override def occlusionTest(npart: TMultiPart) = npart match {
    case apart: TArrayGatePart if apart.getLogicArray.canCross =>
      if (
        apart.subID == subID && apart.side == (side ^ 1) && (apart.rotation & 1) != (rotation & 1)
      ) true
      else super.occlusionTest(npart)
    case _ => super.occlusionTest(npart)
  }
}

object IGateWireRenderConnect {
  def getConnsAtHeight(gate: GatePart, h: Double) = {
    var conn = 0
    for (r <- 0 until 4) if (getConnHeight(gate, r) == h) conn |= 1 << r
    gate.toInternalMask(conn)
  }

  def getConnHeight(gate: GatePart, r: Int) = gate.getStraight(r) match {
    case g: GatePart =>
      g.getLogic[Any] match {
        case logic: IGateWireRenderConnect =>
          val ir = g.toInternal(gate.rotFromStraight(r))
          if ((logic.renderConnectMask & 1 << ir) != 0) logic.getHeight(ir)
          else -1.0d
        case _ => -1.0d
      }
    case _ => -1.0d
  }
}

trait IGateWireRenderConnect {
  def renderConnectMask: Int
  def getHeight(r: Int): Double
}

trait TArrayGateLogic[T <: TArrayGatePart] extends RedstoneGateLogic[T] {
  abstract override def canConnectTo(gate: T, part: IConnectable, r: Int) =
    part match {
      case re: IRedwirePart if canConnectRedwire(gate, r) => true
      case _ => super.canConnectTo(gate, part, r)
    }

  def canConnectRedwire(gate: T, r: Int): Boolean =
    canConnectRedwire(gate.shape, r)
  def canConnectRedwire(shape: Int, r: Int): Boolean =
    (redwireMask(shape) & 1 << r) != 0

  def redwireMask(shape: Int): Int

  def propogationMask(r: Int): Int

  def getSignal(mask: Int): Int
  def setSignal(mask: Int, signal: Int)

  def overrideSignal(mask: Int) = false
  def calculateSignal(mask: Int) = 0

  def canCross = false

  def onSignalUpdate()
}

class ArrayGatePart
    extends RedstoneGatePart
    with TComplexGatePart
    with TArrayGatePart {
  private var logic: ArrayGateLogic = null

  override def getLogic[T] = logic.asInstanceOf[T]

  override def assertLogic() {
    if (logic == null) logic = ArrayGateLogic.create(this, subID)
  }

  override def getType = "pr_agate"
}

object ArrayGatePart {
  val oBoxes = Array.ofDim[Cuboid6](6, 2)
  val cBoxes = new Array[Cuboid6](6)

  oBoxes(0)(0) = new Cuboid6(1 / 8d, 0, 0, 7 / 8d, 6 / 8d, 1)
  oBoxes(0)(1) = new Cuboid6(0, 0, 1 / 8d, 1, 6 / 8d, 7 / 8d)
  cBoxes(0) = new Cuboid6(0, 0, 0, 1, 6 / 8d, 1)
  for (s <- 1 until 6) {
    val t = Rotation.sideRotations(s).at(Vector3.center)
    oBoxes(s)(0) = oBoxes(0)(0).copy.apply(t)
    oBoxes(s)(1) = oBoxes(0)(1).copy.apply(t)
    cBoxes(s) = cBoxes(0).copy.apply(t)
  }
}

object ArrayGateLogic {
  import mrtjp.projectred.integration.GateDefinition._
  def create(gate: ArrayGatePart, subID: Int) = subID match {
    case NullCell.ordinal      => new NullCell(gate)
    case InvertCell.ordinal    => new InvertCell(gate)
    case BufferCell.ordinal    => new BufferCell(gate)
    case ANDCell.ordinal       => new ANDCell(gate)
    case StackingLatch.ordinal => new StackingLatch(gate)
    case _ => throw new IllegalArgumentException("Invalid gate subID: " + subID)
  }
}

abstract class ArrayGateLogic(val gate: ArrayGatePart)
    extends RedstoneGateLogic[ArrayGatePart]
    with TArrayGateLogic[ArrayGatePart]
    with TComplexGateLogic[ArrayGatePart]

abstract class ArrayGateLogicCrossing(gate: ArrayGatePart)
    extends ArrayGateLogic(gate)
    with IGateWireRenderConnect {
  var signal1: Byte = 0
  var signal2: Byte = 0

  override def redwireMask(shape: Int) = 0xf
  override def propogationMask(r: Int) = if (r % 2 == 0) 0x5 else 0xa
  override def inputMask(shape: Int) = 0xf
  override def outputMask(shape: Int) = 0xf

  override def renderConnectMask = 0xa
  override def getHeight(r: Int) = 10.0d

  override def getSignal(mask: Int) =
    (if (mask == 0x5) signal1 else signal2) & 0xff
  override def setSignal(mask: Int, signal: Int) {
    if (mask == 0x5) signal1 = signal.toByte else signal2 = signal.toByte
  }

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("s1", signal1)
    tag.setByte("s2", signal2)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    signal1 = tag.getByte("s1")
    signal2 = tag.getByte("s2")
  }

  override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    packet.writeByte(signal1)
    packet.writeByte(signal2)
  }

  override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    signal1 = packet.readByte()
    signal2 = packet.readByte()
  }

  override def read(packet: MCDataInput, key: Int) = key match {
    case 11 =>
      signal1 = packet.readByte()
      signal2 = packet.readByte()
      if (Configurator.staticGates) gate.tile.markRender()
    case _ =>
  }

  def sendSignalUpdate() {
    gate.getWriteStreamOf(11).writeByte(signal1).writeByte(signal2)
  }

  override def onChange(gate: ArrayGatePart) {
    val oldSignal = (gate.state & 1) != 0
    val newSignal = signal1 != 0

    if (oldSignal != newSignal) {
      gate.setState(gate.state & 2 | (if (newSignal) 1 else 0))
      gate.onInputChange()
      gate.scheduleTick(2)
    }
  }

  override def scheduledTick(gate: ArrayGatePart) {
    val input = (gate.state & 1) != 0
    val oldOutput = (gate.state & 2) != 0
    val newOutput = !input

    if (oldOutput != newOutput) {
      gate.setState(gate.state & 1 | (if (newOutput) 2 else 0))
      gate.onOutputChange(0)
      gate.onChange()
    }
  }

  override def getOcclusions(gate: ArrayGatePart) =
    ArrayGatePart.oBoxes(gate.side)
  override def getBounds(gate: ArrayGatePart) = ArrayGatePart.cBoxes(gate.side)

  override def onSignalUpdate() { sendSignalUpdate() }

  override def overrideSignal(mask: Int) = if (mask == 0xa) powerUp else false

  override def calculateSignal(mask: Int) = 255

  def powerUp: Boolean
}

class NullCell(gate: ArrayGatePart) extends ArrayGateLogicCrossing(gate) {
  override def canCross = true

  override def powerUp = false

  override def lightLevel = 0
}

class InvertCell(gate: ArrayGatePart) extends ArrayGateLogicCrossing(gate) {
  override def powerUp = (gate.state & 2) != 0
}

class BufferCell(gate: ArrayGatePart) extends ArrayGateLogicCrossing(gate) {
  override def powerUp = (gate.state & 2) == 0
}

trait TArrayCellTopOnly extends ArrayGateLogic {
  var signal: Byte = 0

  override def redwireMask(shape: Int) = 0xa
  override def propogationMask(r: Int) = if (r % 2 == 1) 0xa else 0

  override def getSignal(mask: Int) = if (mask == 0xa) signal & 0xff else 0
  override def setSignal(mask: Int, sig: Int) {
    if (mask == 0xa) signal = sig.toByte
  }

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setByte("signal", signal)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    signal = tag.getByte("signal")
  }

  override def writeDesc(packet: MCDataOutput) {
    super.writeDesc(packet)
    packet.writeByte(signal)
  }

  override def readDesc(packet: MCDataInput) {
    super.readDesc(packet)
    signal = packet.readByte()
  }

  override def read(packet: MCDataInput, key: Int) = key match {
    case 11 =>
      signal = packet.readByte()
      if (Configurator.staticGates) gate.tile.markRender()
    case _ =>
  }

  def sendSignalUpdate() { gate.getWriteStreamOf(11).writeByte(signal) }

  override def onSignalUpdate() { sendSignalUpdate() }

  override def onChange(gate: ArrayGatePart) {
    val iMask = inputMask(gate.shape)
    val rMask = redwireMask(gate.shape)
    val oMask = outputMask(gate.shape)
    val fMask = feedbackMask(gate.shape)
    val oldInput = gate.state & 0xf
    val newInput = getInput(gate, iMask | fMask) | getRedwireInput(rMask);
    if (oldInput != newInput) {
      gate.setState(gate.state & 0xf0 | newInput)
      gate.onInputChange()
    }

    val newOutput = calcOutput(gate, gate.state & (iMask | rMask)) & oMask
    if (newOutput != (gate.state >> 4)) gate.scheduleTick(getDelay(gate.shape))
  }

  override def scheduledTick(gate: ArrayGatePart) {
    val iMask = inputMask(gate.shape)
    val rMask = redwireMask(gate.shape)
    val oMask = outputMask(gate.shape)
    val oldOutput = gate.state >> 4
    val newOutput = calcOutput(gate, gate.state & (iMask | rMask)) & oMask
    if (oldOutput != newOutput) {
      gate.setState(gate.state & 0xf | newOutput << 4)
      gate.onOutputChange(oMask)
    }
    onChange(gate)
  }

  def getDelay(shape: Int) = 2

  def feedbackMask(shape: Int) = 0

  def getRedwireInput(mask: Int) = if (getSignal(mask) != 0) mask else 0

  def calcOutput(gate: ArrayGatePart, input: Int) = 0
}

class ANDCell(gate: ArrayGatePart)
    extends ArrayGateLogic(gate)
    with TArrayCellTopOnly
    with IGateWireRenderConnect {
  override def inputMask(shape: Int) = 4
  override def outputMask(shape: Int) = 1

  override def renderConnectMask = 0xa
  override def getHeight(r: Int) = 10.0d

  override def calcOutput(gate: ArrayGatePart, input: Int) =
    if (((input & 4) != 0) && ((input & 0xa) != 0)) 1 else 0

  override def getOcclusions(gate: ArrayGatePart) =
    ArrayGatePart.oBoxes(gate.side)
  override def getBounds(gate: ArrayGatePart) = ArrayGatePart.cBoxes(gate.side)
}

class StackingLatch(gate: ArrayGatePart)
    extends ArrayGateLogic(gate)
    with TArrayCellTopOnly {
  override def inputMask(shape: Int) = 4
  override def outputMask(shape: Int) = 1

  override def calcOutput(gate: ArrayGatePart, input: Int) =
    if ((input & 0xa) == 0) gate.state >> 4
    else if ((input & 4) == 0) 0
    else 1
}
