/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.circuitparts.wire

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.render.uv.IconTransformation
import codechicken.lib.vec.Transformation
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.ICComponentStore._
import mrtjp.projectred.fabrication._
import mrtjp.projectred.fabrication.circuitparts.{
  CircuitPart,
  CircuitPartDefs,
  TPoweredCircuitPart,
  TClientNetCircuitPart,
  TICAcquisitions
}
import mrtjp.projectred.fabrication.operations.CircuitOpDefs
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumChatFormatting

class ButtonICPart
    extends CircuitPart
    with TICAcquisitions
    with TPoweredCircuitPart
    with TClientNetCircuitPart {
  var on = false
  var sched = -1L

  override def save(tag: NBTTagCompound) {
    super.save(tag)
    tag.setBoolean("on", on)
    tag.setLong("sched", sched)
  }

  override def load(tag: NBTTagCompound) {
    super.load(tag)
    on = tag.getBoolean("on")
    sched = tag.getLong("sched")
  }

  override def writeDesc(out: MCDataOutput) {
    super.writeDesc(out)
    out.writeBoolean(on)
  }

  override def readDesc(in: MCDataInput) {
    super.readDesc(in)
    on = in.readBoolean()
  }

  override def read(in: MCDataInput, key: Int) = key match {
    case 1 => on = in.readBoolean()
    case _ => super.read(in, key)
  }

  override def readClientPacket(in: MCDataInput) {
    press()
  }

  def press() {
    if (!on) {
      on = true
      notify(0xf)
      sendStateUpdate()

      sched = world.network.getWorld.getTotalWorldTime + 20 // schedule depress
    }
  }

  def depress() {
    if (on) {
      on = false
      notify(0xf)
      sendStateUpdate()

      sched = -1 // clear depress schedule
    }
  }

  override def update() {
    if (sched != -1 && world.network.getWorld.getTotalWorldTime >= sched)
      depress()
  }

  def sendStateUpdate() {
    writeStreamOf(1).writeBoolean(on)
  }

  override def getPartType = CircuitPartDefs.Button

  override def onAdded() {
    if (!world.network.isRemote) notify(0xf)
  }

  override def onRemoved() {
    if (!world.network.isRemote) notify(0xf)
  }

  override def rsOutputLevel(r: Int) = if (on) 255 else 0
  override def canConnectRS(r: Int) = true

  @SideOnly(Side.CLIENT)
  override def onClicked() {
    sendClientPacket() // data not necessary, only 1 reason to send this.
  }

  @SideOnly(Side.CLIENT)
  override def getPartName = "Button"

  @SideOnly(Side.CLIENT)
  override def getPickOp = CircuitOpDefs.Button.getOp

  @SideOnly(Side.CLIENT)
  override def getRolloverData(detailLevel: Int) = {
    val b = Seq.newBuilder[String]
    if (detailLevel > 1)
      b += EnumChatFormatting.GRAY + "state: " + (if (on) "on" else "off")
    super.getRolloverData(detailLevel) ++ b.result()
  }

  @SideOnly(Side.CLIENT)
  override def renderDynamic(
      t: Transformation,
      ortho: Boolean,
      frame: Float
  ) = {
    RenderICButton.prepairDynamic(this)
    RenderICButton.render(t, ortho)
  }
}

object RenderICButton {
  var on = false

  def prepairInv() {
    on = false
  }

  def prepairDynamic(part: ButtonICPart) {
    on = part.on
  }

  def render(t: Transformation, ortho: Boolean) {
    prepairRender()
    faceModels(dynamicIdx(0, ortho)).render(
      t,
      new IconTransformation(if (on) buttonOnIcon else buttonOffIcon)
    )
    finishRender()
  }
}
