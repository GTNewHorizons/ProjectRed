package mrtjp.projectred.integration

import java.io.{ByteArrayOutputStream, DataOutputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import javax.imageio.ImageIO

import codechicken.lib.colour.{Colour, ColourARGB}
import codechicken.lib.render.CCModel
import codechicken.lib.vec.Rectangle4i
import org.junit.Assert.{assertEquals, assertNotNull}
import org.junit.Test

import scala.io.Source

class GateWireGoldenTest {

  @Test
  def wireGeometryMatchesGoldenOutput(): Unit = {
    val actual = GateWireGoldenTest.maskNames.map(characterize)
    val actualText = (GateWireGoldenTest.header +: actual).mkString("\n") + "\n"
    val actualDigest = sha256(actualText.getBytes(StandardCharsets.UTF_8))
    val expectedDigest = readResource(GateWireGoldenTest.goldenPath).trim

    if (actualDigest != expectedDigest) {
      val report = new File("build/reports/wire-golden/actual-v1.txt")
      report.getParentFile.mkdirs()
      Files.write(report.toPath, actualText.getBytes(StandardCharsets.UTF_8))
    }

    assertEquals(
      "Wire geometry changed; see build/reports/wire-golden/actual-v1.txt",
      expectedDigest,
      actualDigest
    )
  }

  private def characterize(name: String): String = {
    val data = loadMask(name)
    val rectangles = TWireModel.rectangulate(data)
    val model = WireModel3D.generateModel(data)

    Seq(
      name,
      rectangles.length.toString,
      model.verts.length.toString,
      digestRectangles(rectangles),
      digestModel(model)
    ).mkString(" ")
  }

  private def loadMask(name: String): Array[Colour] = {
    val path = GateWireGoldenTest.maskPath + name + ".png"
    val stream = getClass.getResourceAsStream(path)
    assertNotNull("Missing wire mask " + path, stream)

    val image =
      try ImageIO.read(stream)
      finally stream.close()
    assertNotNull("Invalid wire mask " + path, image)
    assertEquals("Unexpected width for " + name, 32, image.getWidth)
    assertEquals("Unexpected height for " + name, 32, image.getHeight)

    image
      .getRGB(0, 0, image.getWidth, image.getHeight, null, 0, image.getWidth)
      .map(new ColourARGB(_): Colour)
  }

  private def digestRectangles(rectangles: Seq[Rectangle4i]): String = digest {
    out =>
      out.writeInt(rectangles.length)
      rectangles.foreach { rectangle =>
        out.writeInt(rectangle.x)
        out.writeInt(rectangle.y)
        out.writeInt(rectangle.w)
        out.writeInt(rectangle.h)
      }
  }

  private def digestModel(model: CCModel): String = digest { out =>
    val normals = model.normals()
    out.writeInt(model.vertexMode)
    out.writeInt(model.vp)
    out.writeInt(model.verts.length)
    model.verts.indices.foreach { i =>
      val vertex = model.verts(i)
      val normal = normals(i)
      writeDouble(out, vertex.vec.x)
      writeDouble(out, vertex.vec.y)
      writeDouble(out, vertex.vec.z)
      writeDouble(out, vertex.uv.u)
      writeDouble(out, vertex.uv.v)
      out.writeInt(vertex.uv.tex)
      writeDouble(out, normal.x)
      writeDouble(out, normal.y)
      writeDouble(out, normal.z)
    }
  }

  private def digest(write: DataOutputStream => Unit): String = {
    val bytes = new ByteArrayOutputStream
    val out = new DataOutputStream(bytes)
    write(out)
    out.close()
    sha256(bytes.toByteArray)
  }

  private def sha256(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  private def writeDouble(out: DataOutputStream, value: Double): Unit = {
    require(!value.isNaN && !value.isInfinity, "Non-finite model value")
    out.writeLong(Math.round((if (value == 0) 0 else value) * 1000000000L))
  }

  private def readResource(path: String): String = {
    val stream = getClass.getResourceAsStream(path)
    assertNotNull("Missing golden output " + path, stream)
    val source = Source.fromInputStream(stream, "UTF-8")
    try source.mkString
    finally source.close()
  }
}

object GateWireGoldenTest {
  val goldenPath = "/mrtjp/projectred/integration/gate-wire-golden-v1.txt"
  val maskPath = "/assets/projectred/textures/blocks/integration/surface/"
  val header = "# gate-wire-golden-v1 scale=1e-9 masks=120"

  val maskNames = Seq(
    "OR" -> 4,
    "NOR" -> 4,
    "NOT" -> 4,
    "AND" -> 4,
    "NAND" -> 4,
    "XOR" -> 4,
    "XNOR" -> 5,
    "BUFFER" -> 4,
    "MULTIPLEXER" -> 6,
    "PULSE" -> 3,
    "REPEATER" -> 2,
    "RAND" -> 7,
    "RSLATCH" -> 2,
    "RSLATCH2" -> 4,
    "TOGLATCH" -> 2,
    "TRANSLATCH" -> 5,
    "LIGHTSENSOR" -> 1,
    "RAINSENSOR" -> 1,
    "TIME" -> 3,
    "COUNT" -> 2,
    "STATECELL" -> 5,
    "SYNC" -> 6,
    "BUSXCVR" -> 2,
    "COMPARATOR" -> 4,
    "BUSRAND1" -> 2,
    "BUSRAND2" -> 2,
    "BUSCONV" -> 3,
    "BUSINPUT" -> 1,
    "INVCELL" -> 1,
    "BUFFCELL" -> 2,
    "ANDCELL" -> 2,
    "STACKLATCH" -> 5,
    "DECRAND" -> 6,
    "IC1" -> 4,
    "IC2" -> 4
  ).flatMap { case (name, count) => (0 until count).map(name + "-" + _) }

  require(maskNames.length == 120)
}
