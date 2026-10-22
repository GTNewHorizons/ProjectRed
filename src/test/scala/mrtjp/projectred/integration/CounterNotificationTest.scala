package mrtjp.projectred.integration

import org.junit.Assert.assertEquals
import org.junit.Test

class CounterNotificationTest {
  @Test
  def worldCounterNotifiesOnlyWhenOutputChanges(): Unit = {
    var notifications = 0
    val gate = new SequentialGatePart {
      override def onOutputChange(mask: Int): Unit = { notifications += 1 }
    }
    val logic = new Counter(gate)
    logic.max = 4
    gate.setState(0x42)
    logic.scheduledTick(gate)
    assertEquals("zero output was unchanged", 0, notifications)
    assertEquals("input bits were preserved", 0x42, gate.state)
    logic.value = 4
    logic.scheduledTick(gate)
    assertEquals("maximum output changed once", 1, notifications)
    logic.scheduledTick(gate)
    assertEquals("maximum output was unchanged", 1, notifications)
  }

  @Test
  def icCounterNotifiesOnlyWhenOutputChanges(): Unit = {
    var notifications = 0
    val gate = new mrtjp.projectred.fabrication.SequentialGateICPart {
      override def onOutputChange(mask: Int): Unit = { notifications += 1 }
    }
    val logic = new mrtjp.projectred.fabrication.Counter(gate)
    logic.max = 4
    gate.setState(0x42)
    logic.scheduledTick(gate)
    assertEquals("zero output was unchanged", 0, notifications)
    assertEquals("input bits were preserved", 0x42, gate.state)
    logic.value = 4
    logic.scheduledTick(gate)
    assertEquals("maximum output changed once", 1, notifications)
    logic.scheduledTick(gate)
    assertEquals("maximum output was unchanged", 1, notifications)
  }
}
