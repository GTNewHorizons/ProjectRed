package mrtjp.projectred.integration

import java.io.{ByteArrayInputStream, InputStream}
import java.util.{ArrayList, Collections, List => JList, Set => JSet}

import codechicken.lib.vec.Rectangle4i
import net.minecraft.client.resources.data.IMetadataSection
import net.minecraft.client.resources.{IResource, IResourceManager}
import net.minecraft.util.ResourceLocation
import org.apache.commons.io.IOUtils
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test

class GateWireMasksTest {

  @Test
  def precomputedLayoutsMatchSourceMasks(): Unit = {
    GateWireGoldenTest.maskNames.foreach { name =>
      assertEquals(
        name,
        tuples(TWireModel.rectangulate(GateWireTestData.loadMask(name))),
        tuples(GateWireMasks.rectangles(name))
      )
    }
  }

  @Test
  def builtInMasksHaveExpectedUniqueLayouts(): Unit = {
    assertEquals(
      97,
      GateWireGoldenTest.maskNames
        .map(GateWireMasks.rectangles)
        .map(tuples)
        .distinct
        .size
    )
  }

  @Test
  def builtInLayoutClosesButDoesNotReadSingleResource(): Unit = {
    val stream =
      new TrackingInputStream(Array.emptyByteArray, failOnRead = true)
    val actual = ComponentStore.loadWireRectangles("OR-0", manager(stream))

    assertEquals(tuples(GateWireMasks.rectangles("OR-0")), tuples(actual))
    assertTrue(stream.closed)
  }

  @Test
  def resourcePackOverrideUsesHighestPriorityMask(): Unit = {
    val builtIn =
      new TrackingInputStream(Array.emptyByteArray, failOnRead = true)
    val overrideStream = new TrackingInputStream(resourceBytes("OR-1"))
    val actual = ComponentStore.loadWireRectangles(
      "OR-0",
      manager(builtIn, overrideStream)
    )

    assertEquals(tuples(GateWireMasks.rectangles("OR-1")), tuples(actual))
    assertTrue(builtIn.closed)
    assertTrue(overrideStream.closed)
  }

  @Test
  def unknownMaskFallsBackToPng(): Unit = {
    val stream = new TrackingInputStream(resourceBytes("OR-1"))
    val actual = ComponentStore.loadWireRectangles("ADDON", manager(stream))

    assertEquals(tuples(GateWireMasks.rectangles("OR-1")), tuples(actual))
    assertTrue(stream.closed)
  }

  private def tuples(rectangles: Seq[Rectangle4i]) =
    rectangles.map(r => (r.x, r.y, r.w, r.h))

  private def resourceBytes(name: String): Array[Byte] = {
    val stream =
      getClass.getResourceAsStream(GateWireGoldenTest.maskPath + name + ".png")
    try IOUtils.toByteArray(stream)
    finally stream.close()
  }

  private def manager(streams: TrackingInputStream*) = new IResourceManager {
    private val resources = new ArrayList[IResource]()
    streams.foreach(stream => resources.add(new TestResource(stream)))

    override def getResourceDomains: JSet[_] = Collections.emptySet[AnyRef]()
    override def getResource(location: ResourceLocation): IResource =
      resources.get(resources.size() - 1)
    override def getAllResources(location: ResourceLocation): JList[_] =
      resources
  }

  private class TestResource(stream: InputStream) extends IResource {
    override def getInputStream = stream
    override def hasMetadata = false
    override def getMetadata(name: String): IMetadataSection = null
  }

  private class TrackingInputStream(
      bytes: Array[Byte],
      failOnRead: Boolean = false
  ) extends ByteArrayInputStream(bytes) {
    var closed = false

    override def read(): Int = {
      if (failOnRead) throw new AssertionError("Stream was read")
      super.read()
    }

    override def read(buffer: Array[Byte], offset: Int, length: Int): Int = {
      if (failOnRead) throw new AssertionError("Stream was read")
      super.read(buffer, offset, length)
    }

    override def close(): Unit = {
      closed = true
      super.close()
    }
  }
}
