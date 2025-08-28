package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.color.Colors
import mrtjp.core.gui.{GuiLib, MCButtonNode, TNode}
import mrtjp.core.vec.{Point, Rect, Size}
import mrtjp.projectred.fabrication.IntegratedCircuit
import mrtjp.projectred.fabrication.circuitparts.CircuitPart
import mrtjp.projectred.fabrication.operations.OpAreaBase
import net.minecraft.client.gui.Gui
import net.minecraft.nbt.{JsonToNBT, NBTTagCompound}
import net.minecraft.util.StatCollector

import java.awt.Toolkit
import java.awt.datatransfer.{DataFlavor, StringSelection}
import java.io.ByteArrayOutputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Base64
import java.util.zip.{DataFormatException, Deflater, Inflater}

/** Export/Import of Circuits: Circuit as nbt -> Deflate -> Base64
  */
class StringExport(circuit: IntegratedCircuit, onImport: () => Unit)
    extends Gui
    with TNode {

  position = Point(20, -150)

  override def frame: Rect = Rect(position, Size(180, 80))

  var statusText = ""
  var statusColor = Colors.RED

  {
    val close = new MCButtonNode
    close.position = Point(4, 4)
    close.size = Size(5, 5)
    close.clickDelegate = { () => removeFromParent() }
    addChild(close)

    val buttonImport = new MCButtonNode
    buttonImport.position = Point(10, 10)
    buttonImport.size = Size(160, 20)
    buttonImport.text =
      StatCollector.translateToLocal("gui.projectred.fabrication.import_string")
    buttonImport.clickDelegate = { () =>
      try {
        importString()
        onImport()
      } catch {
        case _: IllegalArgumentException | _: DataFormatException =>
          statusText = "gui.projectred.fabrication.invalid_string"
          statusColor = Colors.RED
        case _: IllegalStateException =>
          statusText = "gui.projectred.fabrication.clipboard_unavailable"
          statusColor = Colors.RED
      }
    }
    addChild(buttonImport)

    val buttonExport = new MCButtonNode
    buttonExport.position = Point(10, 35)
    buttonExport.size = Size(160, 20)
    buttonExport.text =
      StatCollector.translateToLocal("gui.projectred.fabrication.export_string")
    buttonExport.clickDelegate = { () =>
      try {
        exportString(circuit)
        statusText = "gui.projectred.fabrication.export_success"
        statusColor = Colors.GREEN
      } catch {
        case _: IllegalStateException =>
          statusText = "gui.projectred.fabrication.clipboard_unavailable"
          statusColor = Colors.RED
      }
    }
    addChild(buttonExport)
  }

  override def drawBack_Impl(mouse: Point, rframe: Float): Unit = {
    GuiLib.drawGuiBox(
      position.x,
      position.y,
      frame.size.width,
      frame.size.height,
      0
    )
    GuiDraw.drawString(
      StatCollector.translateToLocal(statusText),
      position.x + 5,
      position.y + 60,
      statusColor.argb,
      false
    )
  }

  private def exportString(circuit: IntegratedCircuit): Unit = {
    val nbtTagCompound = new NBTTagCompound
    circuit.save(nbtTagCompound)
    val data = nbtTagCompound.toString.getBytes(Charset.forName("UTF-8"))
    val compressed = compress(data)
    val base64EncodedString = new StringSelection(
      Base64.getEncoder.encodeToString(compressed)
    )
    Toolkit.getDefaultToolkit.getSystemClipboard
      .setContents(base64EncodedString, null)
  }

  private def compress(input: Array[Byte]): Array[Byte] = {

    val bos = new ByteArrayOutputStream()

    val compressor = new Deflater()
    compressor.setInput(input)
    compressor.finish()

    val buffer = new Array[Byte](1024)
    while (!compressor.finished()) {
      val len = compressor.deflate(buffer)
      bos.write(buffer, 0, len)
    }
    compressor.end()

    bos.toByteArray
  }

  @throws[Exception]
  private def importString(): Unit = {
    val clipboard = Toolkit.getDefaultToolkit.getSystemClipboard
    val clipboardString =
      clipboard.getData(DataFlavor.stringFlavor).asInstanceOf[String]
    val decoded = Base64.getDecoder.decode(clipboardString)
    val decompressed = decompress(decoded)

    val nbt =
      JsonToNBT.func_150315_a(new String(decompressed, StandardCharsets.UTF_8))
    val compound = nbt.asInstanceOf[NBTTagCompound]
    val x = compound.getTagList("parts", 10)
    val parts = (0 until x.tagCount())
      .map { i =>
        x.getCompoundTagAt(i)
      }
      .map { partTag =>
        val part = CircuitPart.createPart(partTag.getByte("id"))
        part.load(partTag)
        part.loc = (partTag.getInteger("xpos"), partTag.getInteger("ypos"))
        (part.x, part.y) -> part
      }
      .toMap
    OpAreaBase.saveToClipboard(parts)
  }

  private def decompress(data: Array[Byte]): Array[Byte] = {
    val inflater = new Inflater()
    inflater.setInput(data)

    val output = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)

    while (!inflater.finished()) {
      val count = inflater.inflate(buffer)
      output.write(buffer, 0, count)
    }
    output.toByteArray
  }
}
