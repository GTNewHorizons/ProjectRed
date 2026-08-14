package mrtjp.projectred.integration

import codechicken.lib.render.CCModel
import codechicken.lib.vec.Vector3
import org.junit.Assert.{assertEquals, assertNotSame, assertSame}
import org.junit.Test

class ComponentModelSharingTest {

  @Test
  def equivalentTorchesShareOnlyGeometry(): Unit = {
    val first = new RedstoneTorchModel(8, 8, 6)
    val second = new RedstoneTorchModel(8, 8, 6)
    val different = new RedstoneTorchModel(8, 8, 8)
    val expected = SingleComponentModel.bakeModelPair(
      RedstoneTorchModel.genModel(8, 8, 6),
      Vector3.zero
    )

    assertSame(first.modelPair, second.modelPair)
    assertNotSame(first.modelPair, different.modelPair)
    assertModelsEqual(expected, first.modelPair)
    first.on = true
    assertEquals(false, second.on)
  }

  private def assertModelsEqual(
      expected: Array[CCModel],
      actual: Array[CCModel]
  ) =
    for (
      pairIndex <- expected.indices;
      vertexIndex <- expected(pairIndex).verts.indices
    ) {
      val expectedVertex = expected(pairIndex).verts(vertexIndex)
      val actualVertex = actual(pairIndex).verts(vertexIndex)
      val expectedNormal = expected(pairIndex).normals()(vertexIndex)
      val actualNormal = actual(pairIndex).normals()(vertexIndex)
      assertEquals(expectedVertex.vec.x, actualVertex.vec.x, 0)
      assertEquals(expectedVertex.vec.y, actualVertex.vec.y, 0)
      assertEquals(expectedVertex.vec.z, actualVertex.vec.z, 0)
      assertEquals(expectedVertex.uv.u, actualVertex.uv.u, 0)
      assertEquals(expectedVertex.uv.v, actualVertex.uv.v, 0)
      assertEquals(expectedVertex.uv.tex, actualVertex.uv.tex)
      assertEquals(expectedNormal.x, actualNormal.x, 0)
      assertEquals(expectedNormal.y, actualNormal.y, 0)
      assertEquals(expectedNormal.z, actualNormal.z, 0)
    }
}
