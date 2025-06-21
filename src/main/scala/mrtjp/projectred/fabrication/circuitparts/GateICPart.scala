package mrtjp.projectred.fabrication.circuitparts

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.operations.{
  CircuitOp,
  CircuitOpDefs,
  OpGate
}
import net.minecraft.nbt.NBTTagCompound

abstract class GateICPart
    extends CircuitPart
    with TConnectableICPart
    with TICOrient
    with TClientNetCircuitPart {
  private var gateSubID: Byte = 0
  private var gateShape: Byte = 0

  var schedTime = 0L
  var schedDigital = false

  def getLogic[T]: T

  def getLogicPrimitive = getLogic[ICGateLogic[GateICPart]]

  def subID = gateSubID & 0xff

  def shape = gateShape & 0xff

  def setShape(s: Int) {
    gateShape = s.toByte
  }

  def preparePlacement(rot: Int, configuration: Int, meta: Int) {
    gateSubID = meta.toByte
    setRotation(rot)
    gateShape = configuration.toByte
  }

  override def save(tag: NBTTagCompound) {
    tag.setByte("orient", orientation)
    tag.setByte("subID", gateSubID)
    tag.setByte("shape", gateShape)
    tag.setByte("connMap", connMap)
    tag.setLong("schedTime", schedTime)
  }

  override def load(tag: NBTTagCompound) {
    orientation = tag.getByte("orient")
    gateSubID = tag.getByte("subID")
    gateShape = tag.getByte("shape")
    connMap = tag.getByte("connMap")
    schedTime = tag.getLong("schedTime")
  }

  override def writeDesc(out: MCDataOutput) {
    out.writeByte(orientation)
    out.writeByte(gateSubID)
    out.writeByte(gateShape)
  }

  override def readDesc(in: MCDataInput) {
    orientation = in.readByte()
    gateSubID = in.readByte()
    gateShape = in.readByte()
  }

  override def read(in: MCDataInput, key: Int) = key match {
    case 1 => orientation = in.readByte()
    case 2 => gateShape = in.readByte()
    case _ => super.read(in, key)
  }

  override def readClientPacket(in: MCDataInput) {
    readClientPacket(in, in.readUByte())
  }

  def readClientPacket(in: MCDataInput, key: Int) = key match {
    case 0 => rotate()
    case 1 => configure()
    case 2 => getLogicPrimitive.activate(this)
    case _ =>
  }

  override def canConnectPart(part: CircuitPart, r: Int) =
    getLogicPrimitive.canConnectTo(this, part, toInternal(r))

  override def scheduledTick() {
    getLogicPrimitive.scheduledTick(this)
  }

  override def scheduleTick(ticks: Int) {
    if (ticks == 0) scheduleDigitalTick()
    else if (schedTime < 0)
      schedTime = world.network.getWorld.getTotalWorldTime + ticks
  }

  def processScheduled() {
    if (
      schedTime >= 0 && world.network.getWorld.getTotalWorldTime >= schedTime
    ) {
      schedTime = -1
      scheduledTick()
    }
  }

  def scheduleDigitalTick() {
    schedDigital = true
  }

  var iter = 0

  def processScheduledDigital() {
    while (schedDigital && iter < 3) // recursion control
      {
        schedDigital = false
        iter += 1
        scheduledTick()
      }
  }

  def onChange() {
    processScheduled()
    getLogicPrimitive.onChange(this)
    processScheduledDigital()
  }

  override def update() {
    if (!world.network.isRemote) {
      processScheduled()
      iter = 0
      processScheduledDigital()
    }
    getLogicPrimitive.onTick(this)
  }

  override def onNeighborChanged() {
    if (!world.network.isRemote) {
      updateConns()
      onChange()
    }
  }

  override def onAdded() {
    super.onAdded()
    if (!world.network.isRemote) {
      getLogicPrimitive.setup(this)
      updateConns()
      onChange()
    }
  }

  override def onRemoved() {
    super.onRemoved()
    if (!world.network.isRemote) notify(0xf)
  }

  def configure() {
    if (getLogicPrimitive.cycleShape(this)) {
      updateConns()
      world.network.markSave()
      sendShapeUpdate()
      notify(0xf)
      onChange()
    }
  }

  def rotate() {
    setRotation((rotation + 1) % 4)
    updateConns()
    world.network.markSave()
    sendOrientUpdate()
    notify(0xf)
    onChange()
  }

  def sendShapeUpdate() {
    writeStreamOf(2).writeByte(gateShape)
  }

  def sendOrientUpdate() {
    writeStreamOf(1).writeByte(orientation)
  }

  @SideOnly(Side.CLIENT)
  override def renderDynamic(t: Transformation, ortho: Boolean, frame: Float) {
    ICGateRenderer.renderDynamic(this, t, ortho, frame)
  }

  @SideOnly(Side.CLIENT)
  override def getPartName = ICGateDefinition(subID).name

  @SideOnly(Side.CLIENT)
  override def getRolloverData(detailLevel: Int) = {
    val s = Seq.newBuilder[String]
    import net.minecraft.util.EnumChatFormatting._
    s ++= getLogicPrimitive.getRolloverData(this, detailLevel)
    super.getRolloverData(detailLevel) ++ s.result().map(GRAY + _)
  }

  @SideOnly(Side.CLIENT)
  override def onClicked() {
    sendClientPacket(_.writeByte(2))
  }

  @SideOnly(Side.CLIENT)
  override def getCircuitOperation: CircuitOp = {
    val op = new OpGate(subID)
    op.id = CircuitOpDefs.SimpleIO.ordinal + subID
    op.rotation = orientation
    op.configuration = gateShape
    op
  }
}
