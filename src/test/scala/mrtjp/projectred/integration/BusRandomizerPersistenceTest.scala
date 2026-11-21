package mrtjp.projectred.integration

import mrtjp.projectred.transmission.BundledCommons
import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert.assertEquals
import org.junit.Test

class BusRandomizerPersistenceTest {
  @Test
  def restoredSelectionMaskIsUnsigned(): Unit = {
    for (mask <- Seq(0x8000, 0xffff)) {
      val logic = restored(mask, 0)
      assertEquals(s"selection mask $mask", mask, logic.mask)
      assertEquals(
        s"selectable channel count $mask",
        Integer.bitCount(mask),
        Integer.bitCount(logic.mask)
      )
    }
  }

  @Test
  def restoredOutputMaskIsUnsigned(): Unit = {
    for (mask <- Seq(0x8000, 0xffff)) {
      val logic = restored(0xffff, mask)
      assertEquals(s"output mask $mask", mask, logic.output)
      assertEquals(
        s"output channels $mask",
        mask,
        BundledCommons.packDigital(logic.unpackedOut)
      )
    }
  }

  private def restored(mask: Int, output: Int): BusRandomizer = {
    val tag = new NBTTagCompound
    tag.setByte("subID", GateDefinition.BusRandomizer.ordinal.toByte)
    tag.setBoolean("nolegacy", true)
    tag.setShort("in", mask.toShort)
    tag.setShort("out", output.toShort)
    val gate = new BundledGatePart
    gate.load(tag)
    val saved = new NBTTagCompound
    gate.save(saved)
    val copy = new BundledGatePart
    copy.load(saved)
    copy.getLogic[BusRandomizer]
  }
}
