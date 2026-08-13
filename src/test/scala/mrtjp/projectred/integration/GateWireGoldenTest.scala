package mrtjp.projectred.integration

import java.io.{ByteArrayOutputStream, DataOutputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import javax.imageio.ImageIO

import codechicken.lib.colour.{Colour, ColourARGB}
import codechicken.lib.render.CCModel
import codechicken.lib.vec.Rectangle4i
import org.junit.Assert.{assertEquals, assertNotNull, assertNotSame}
import org.junit.Test

import scala.io.Source

class GateWireGoldenTest {

  @Test
  def wireGeometryMatchesGoldenOutput(): Unit = {
    val actual = GateWireGoldenTest.maskNames.map(characterize)
    assertGolden("geometry", GateWireGoldenTest.header, actual)
  }

  @Test
  def wireBakingMatchesGoldenOutput(): Unit = {
    val actual = GateWireGoldenTest.maskNames.map(characterizeBaking)
    assertGolden("baking", GateWireGoldenTest.bakingHeader, actual)
  }

  @Test
  def bakedModelsDoNotShareMutableGeometry(): Unit = {
    val base = WireModel3D.generateModel(loadMask("OR-0"))
    val pair = ComponentModelBakery.bakeDynamic(base)

    assertNotSame(pair(0), pair(1))
    assertNotSame(pair(0).verts(0), pair(1).verts(0))
    assertNotSame(pair(0).verts(0).vec, pair(1).verts(0).vec)
    assertNotSame(pair(0).verts(0).uv, pair(1).verts(0).uv)
    assertNotSame(pair(0).normals()(0), pair(1).normals()(0))

    val originalX = pair(0).verts(0).vec.x
    pair(1).verts(0).vec.x += 1
    assertEquals(originalX, pair(0).verts(0).vec.x, 0)
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

  private def characterizeBaking(name: String): String = {
    val modelPair = ComponentModelBakery.bakeDynamic(
      WireModel3D.generateModel(loadMask(name))
    )
    val orientedDigest = digest { out =>
      for (orient <- 0 until 48) {
        val model = modelPair(if (orient < 24) 0 else 1).copy
        model.apply(ComponentModelBakery.orientPrecomputed(orient))
        writeModel(out, model)
      }
    }
    name + " " + modelPair(0).verts.length + " " + orientedDigest
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

  private def digestModel(model: CCModel): String = digest(writeModel(_, model))

  private def writeModel(out: DataOutputStream, model: CCModel): Unit = {
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

  private def assertGolden(
      kind: String,
      header: String,
      actual: Seq[String]
  ): Unit = {
    val actualText = (header +: actual).mkString("\n") + "\n"
    val actualDigest = sha256(actualText.getBytes(StandardCharsets.UTF_8))
    val goldenPath =
      "/mrtjp/projectred/integration/gate-wire-" + kind + "-v1.txt"
    val expectedDigest = readResource(goldenPath).trim

    if (actualDigest != expectedDigest) {
      val report = new File(
        "build/reports/wire-golden/actual-" + kind + "-v1.txt"
      )
      report.getParentFile.mkdirs()
      Files.write(report.toPath, actualText.getBytes(StandardCharsets.UTF_8))
    }

    assertEquals(
      "Wire " + kind + " changed; see build/reports/wire-golden/actual-" + kind + "-v1.txt",
      expectedDigest,
      actualDigest
    )
  }
}

object GateWireGoldenTest {
  val maskPath = "/assets/projectred/textures/blocks/integration/surface/"
  val header = "# gate-wire-geometry-v1 scale=1e-9 masks=120"
  val bakingHeader =
    "# gate-wire-baking-v1 scale=1e-9 masks=120 orientations=48"

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
