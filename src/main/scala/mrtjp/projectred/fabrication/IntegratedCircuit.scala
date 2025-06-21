package mrtjp.projectred.fabrication

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import mrtjp.core.vec.{Point, Rect, Size}
import mrtjp.projectred.ProjectRedCore.log
import mrtjp.projectred.fabrication.circuitparts.io.TIOCircuitPart
import mrtjp.projectred.fabrication.circuitparts.{
  CircuitPart,
  TClientNetCircuitPart,
  TErrorCircuitPart
}
import mrtjp.projectred.fabrication.operations.CircuitOp
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}

import scala.collection.mutable.{Map => MMap}

class IntegratedCircuit {
  var network: WorldCircuit = null

  var name = "untitled"

  var parts = MMap[(Int, Int), CircuitPart]()
  var errors = Map.empty[Point, (String, Int)]

  private var scheduledTicks = MMap[(Int, Int), Long]()

  /** Mapped inputs and outputs of this IC. Outputs go to the world, inputs come
    * in from the world. OOOO OOOO OOOO OOOO IIII IIII IIII IIII
    */
  val iostate = Array(0, 0, 0, 0)

  var outputChangedDelegate = { () => () }

  def setInput(r: Int, state: Int) {
    iostate(r) = iostate(r) & 0xffff0000 | state & 0xffff
  }

  def setOutput(r: Int, state: Int) {
    iostate(r) = iostate(r) & 0xffff | (state & 0xffff) << 16
  }

  def onInputChanged(mask: Int) {
    val ioparts = parts.values.collect { case io: TIOCircuitPart => io }
    for (r <- 0 until 4) if ((mask & 1 << r) != 0) {
      ioparts.foreach(_.onExtInputChanged(r))
      sendInputUpdate(r)
    }
  }

  def onOutputChanged(mask: Int) {
    val ioparts = parts.values.collect { case io: TIOCircuitPart => io }
    for (r <- 0 until 4) if ((mask & 1 << r) != 0) {
      ioparts.foreach(_.onExtOutputChanged(r))
      outputChangedDelegate()
      sendOutputUpdate(r)
    }
  }

  def save(tag: NBTTagCompound) {
    tag.setString("name", name)
    tag.setIntArray("iost", iostate)

    val tagList = new NBTTagList
    for (part <- parts.values) {
      val partTag = new NBTTagCompound
      partTag.setByte("id", part.id.toByte)
      partTag.setInteger("xpos", part.x)
      partTag.setInteger("ypos", part.y)
      part.save(partTag)
      tagList.appendTag(partTag)
    }
    tag.setTag("parts", tagList)

    // etc
  }

  def load(tag: NBTTagCompound) {
    clear()
    name = tag.getString("name")
    val ta = tag.getIntArray("iost")
    for (i <- 0 until 4) iostate(i) = ta(i)

    val partList = tag.getTagList("parts", 10)
    for (i <- 0 until partList.tagCount) {
      val partTag = partList.getCompoundTagAt(i)
      val part = CircuitPart.createPart(partTag.getByte("id") & 0xff)
      setPart_do(
        partTag.getInteger("xpos"),
        partTag.getInteger("ypos"),
        part
      )
      part.load(partTag)
    }

    // etc
  }

  def writeDesc(out: MCDataOutput) {
    out.writeString(name)
    for (i <- 0 until 4) out.writeInt(iostate(i))

    for (((x, y), part) <- parts) {
      out.writeByte(part.id)
      out.writeInt(x).writeInt(y)
      part.writeDesc(out)
    }
    out.writeByte(255)

    // etc
  }

  def readDesc(in: MCDataInput) {
    clear()
    name = in.readString()
    for (i <- 0 until 4) iostate(i) = in.readInt()

    var id = in.readUByte()
    while (id != 255) {
      val part = CircuitPart.createPart(id)
      setPart_do(in.readInt(), in.readInt(), part)
      part.readDesc(in)
      id = in.readUByte()
    }
    // etc
  }

  def read(in: MCDataInput, key: Int) = {
    if (!network.isRemote) {
      key match {
        case 3 => CircuitOp.readOp(this, in)
        case 4 =>
          getPart(in.readInt(), in.readInt()) match {
            case g: TClientNetCircuitPart => g.readClientPacket(in)
            case _ =>
              log.error("Server IC stream received invalid client packet")
          }
      }
    } else {
      key match {
        case 0 => readDesc(in)
        case 1 =>
          val part = CircuitPart.createPart(in.readUByte())
          setPart_do(in.readInt(), in.readInt(), part)
          part.readDesc(in)
        case 2 => removePart(in.readInt(), in.readInt())
        case 5 => iostate(in.readUByte()) = in.readInt()
        case 6 => setInput(in.readUByte(), in.readShort())
        case 7 => setOutput(in.readUByte(), in.readShort())
      }
    }
  }

  def sendPartAdded(part: CircuitPart) {
    val out = network.getICStreamOf(1)
    out.writeByte(part.id)
    out.writeInt(part.x).writeInt(part.y)
    part.writeDesc(out)
  }

  def sendRemovePart(x: Int, y: Int) {
    network.getICStreamOf(2).writeInt(x).writeInt(y)
  }

  def sendOpUse(op: CircuitOp, start: Point, end: Point) = {
    if (op.checkOp(this, start, end)) {
      op.clientSendOperation(this, start, end, network.getICStreamOf(3))
      true
    } else false
  }

  def sendClientPacket(
      part: TClientNetCircuitPart,
      writer: MCDataOutput => Unit
  ) {
    val s = network.getICStreamOf(4).writeInt(part.x).writeInt(part.y)
    writer(s)
  }

  def sendInputUpdate(r: Int) {
    network.getICStreamOf(6).writeByte(r).writeShort(iostate(r) & 0xffff)
  }

  def sendOutputUpdate(r: Int) {
    network.getICStreamOf(7).writeByte(r).writeShort(iostate(r) >> 16)
  }

  def clear() {
    parts.values.foreach {
      _.unbind()
    } // remove references
    parts = MMap()
    scheduledTicks = MMap()
    name = "untitled"
    for (i <- 0 until 4) iostate(i) = 0
  }

  def tick() {
    val t = network.getWorld.getTotalWorldTime
    var rem = Seq[(Int, Int)]()
    for ((k, v) <- scheduledTicks) if (v >= t) {
      getPart(k._1, k._2).scheduledTick()
      rem :+= k
    }
    rem.foreach(scheduledTicks.remove)

    for (part <- parts.values) part.update()
  }

  def refreshErrors() {
    val eparts = parts.values.collect { case p: TErrorCircuitPart => p }
    val elist = Map.newBuilder[Point, (String, Int)]

    for (part <- eparts) {
      val error = part.postErrors
      if (error != null)
        elist += Point(part.x, part.y) -> error
    }

    errors = elist.result()
  }

  def getPartsBoundingBox(): Rect = {
    val keys = parts.keys
    val ((x2, y2), (x1, y1)) = if (keys.nonEmpty) {
      (
        keys.reduce((p1, p2) =>
          (math.max(p1._1, p2._1), math.max(p1._2, p2._2))
        ),
        keys.reduce((p1, p2) =>
          (math.min(p1._1, p2._1), math.min(p1._2, p2._2))
        )
      )
    } else {
      ((1, 1), (0, 0))
    }
    Rect(Point(x1, y1), Size(x2 - x1, y2 - y1))
  }

  def setPart(x: Int, y: Int, part: CircuitPart) {
    setPart_do(x, y, part)
    part.onAdded()
    if (!network.isRemote) sendPartAdded(part)
  }

  private def setPart_do(x: Int, y: Int, part: CircuitPart) {
    part.bind(this, x, y)
    parts += (x, y) -> part
  }

  def getPart(x: Int, y: Int): CircuitPart = parts.getOrElse((x, y), null)

  def getParts(
      topLeft: Point,
      bottomRight: Point
  ): Map[(Int, Int), CircuitPart] = {
    parts
      .filter(element =>
        element._1._1 >= topLeft.x &&
          element._1._1 < bottomRight.x &&
          element._1._2 >= topLeft.y &&
          element._1._2 < bottomRight.y
      )
      .toMap
  }

  def removePart(x: Int, y: Int) {
    val part = getPart(x, y)
    if (part != null) {
      if (!network.isRemote) sendRemovePart(x, y)
      parts.remove((x, y))
      part.onRemoved()
      part.unbind()
    }
  }

  def notifyNeighbor(x: Int, y: Int) {
    val part = getPart(x, y)
    if (part != null) part.onNeighborChanged()
  }

  def notifyNeighbors(x: Int, y: Int, mask: Int) {
    for (r <- 0 until 4) if ((mask & 1 << r) != 0) {
      val point = Point(x, y).offset(r)
      val part = getPart(point.x, point.y)
      if (part != null) part.onNeighborChanged()
    }
  }

  def scheduleTick(x: Int, y: Int, ticks: Int) {
    scheduledTicks += (x, y) -> (network.getWorld.getTotalWorldTime + ticks)
  }

  // Convinience functions
  def setPart(p: Point, part: CircuitPart) {
    setPart(p.x, p.y, part)
  }

  def getPart(p: Point): CircuitPart = getPart(p.x, p.y)

  def removePart(p: Point) {
    removePart(p.x, p.y)
  }

  def notifyNeighbor(p: Point) {
    notifyNeighbor(p.x, p.y)
  }

  def notifyNeighbors(p: Point, mask: Int) {
    notifyNeighbors(p.x, p.y, mask)
  }

  def scheduleTick(p: Point, ticks: Int) {
    scheduleTick(p.x, p.y, ticks)
  }
}
