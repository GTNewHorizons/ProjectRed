package mrtjp.projectred.integration

import mrtjp.projectred.transmission.BundledCommons
import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert.assertEquals
import org.junit.Test

class BusConverterPersistenceTest {
  @Test
  def fullMasksSurviveSaveAndLoad(): Unit = {
    for (mask <- Seq(0x100, 0x8000, 0xffff)) {
      val tag = initialTag()
      tag.setShort("in0", mask.toShort)
      tag.setShort("out0", mask.toShort)
      val original = loaded(tag)
      val saved = new NBTTagCompound
      original.save(saved)
      val restored = loaded(saved).getLogic[BusConverter]
      assertEquals(s"saved input mask $mask", mask, restored.bIn)
      assertEquals(s"saved output mask $mask", mask, restored.bOut)
      assertEquals(
        s"restored channels $mask",
        mask,
        BundledCommons.packDigital(restored.bOutUnpacked)
      )
    }
  }

  @Test
  def legacyByteMasksStayUnsigned(): Unit = {
    for (mask <- Seq(0x80, 0xff)) {
      val tag = initialTag()
      tag.setByte("in0", mask.toByte)
      tag.setByte("out0", mask.toByte)
      val restored = loaded(tag).getLogic[BusConverter]
      assertEquals(s"legacy input mask $mask", mask, restored.bIn)
      assertEquals(s"legacy output mask $mask", mask, restored.bOut)
      assertEquals(
        s"legacy channels $mask",
        mask,
        BundledCommons.packDigital(restored.bOutUnpacked)
      )
    }
  }

  private def initialTag(): NBTTagCompound = {
    val tag = new NBTTagCompound
    tag.setByte("subID", GateDefinition.BusConverter.ordinal.toByte)
    tag.setBoolean("nolegacy", true)
    tag
  }

  private def loaded(tag: NBTTagCompound): BundledGatePart = {
    val gate = new BundledGatePart
    gate.load(tag)
    gate
  }
}
