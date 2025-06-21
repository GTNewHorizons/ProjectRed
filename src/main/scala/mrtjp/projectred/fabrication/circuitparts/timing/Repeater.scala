package mrtjp.projectred.fabrication.circuitparts.timing

import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.projectred.fabrication.ICComponentStore.generateWireModels
import mrtjp.projectred.fabrication.circuitparts.{
  ComboICGateLogic,
  ComboICGatePart,
  ICGateRenderer
}
import mrtjp.projectred.fabrication.{BaseComponentModel, RedstoneTorchModel}

object Repeater extends ComboICGateLogic {
  val delays = Array(2, 4, 6, 8, 16, 32, 64, 128, 256)

  override def outputMask(shape: Int) = 1
  override def inputMask(shape: Int) = 4

  override def getDelay(shape: Int) = delays(shape)

  override def cycleShape(shape: Int) = (shape + 1) % delays.length

  override def calcOutput(gate: ComboICGatePart, input: Int) =
    if (input == 0) 0 else 1

  override def onChange(gate: ComboICGatePart) {
    if (gate.schedTime < 0) super.onChange(gate)
  }

  override def activate(gate: ComboICGatePart) {
    gate.configure()
  }

  @SideOnly(Side.CLIENT)
  override def getRolloverData(gate: ComboICGatePart, detailLevel: Int) = {
    val data = Seq.newBuilder[String]
    if (detailLevel > 1) data += "delay: " + delays(gate.shape)
    super.getRolloverData(gate, detailLevel) ++ data.result()
  }
}

class RenderRepeater extends ICGateRenderer[ComboICGatePart] {
  val wires = generateWireModels("REPEATER", 2)
  val endTorch = new RedstoneTorchModel(8, 2)
  val varTorches = Seq(
    new RedstoneTorchModel(12.5, 12),
    new RedstoneTorchModel(12.5, 11),
    new RedstoneTorchModel(12.5, 10),
    new RedstoneTorchModel(12.5, 9),
    new RedstoneTorchModel(12.5, 8),
    new RedstoneTorchModel(12.5, 7),
    new RedstoneTorchModel(12.5, 6),
    new RedstoneTorchModel(12.5, 5),
    new RedstoneTorchModel(12.5, 4)
  )

  var shape = 0

  override val coreModels =
    Seq(new BaseComponentModel("REPEATER")) ++ wires :+ endTorch

  override def switchModels = Seq(varTorches(shape))

  override def prepareStatic(configuration: Int): Unit = {
    wires(0).on = true
    wires(1).on = false
    endTorch.on = false
    shape = configuration
    varTorches(configuration).on = true
  }

  override def prepareDynamic(gate: ComboICGatePart, frame: Float) {
    wires(0).on = (gate.state & 0x10) == 0
    wires(1).on = (gate.state & 4) != 0
    endTorch.on = (gate.state & 0x10) != 0
    shape = gate.shape % 8
    varTorches(shape).on = (gate.state & 4) == 0
  }
}
