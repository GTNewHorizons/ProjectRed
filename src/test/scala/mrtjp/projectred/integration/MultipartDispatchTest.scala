package mrtjp.projectred.integration

import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test
import org.objectweb.asm.{ClassReader, ClassVisitor, MethodVisitor, Opcodes}

import scala.collection.mutable.ArrayBuffer

/** Inspect compiled dispatch without loading untransformed traits or
  * client/game classes.
  */
class MultipartDispatchTest {
  private val root = "mrtjp/projectred/"
  private val fmp = "codechicken/multipart/"

  private def calls(
      owner: String,
      method: String,
      descriptor: String = ""
  ): Seq[(Int, String, String, String)] = {
    val result = ArrayBuffer.empty[(Int, String, String, String)]
    val stream =
      getClass.getClassLoader.getResourceAsStream(root + owner + ".class")
    assertTrue("Missing compiled class " + owner, stream != null)
    try
      new ClassReader(stream).accept(
        new ClassVisitor(Opcodes.ASM5) {
          override def visitMethod(
              access: Int,
              name: String,
              desc: String,
              signature: String,
              exceptions: Array[String]
          ): MethodVisitor =
            if (name != method || (descriptor.nonEmpty && desc != descriptor))
              null
            else
              new MethodVisitor(Opcodes.ASM5) {
                override def visitMethodInsn(
                    opcode: Int,
                    target: String,
                    name: String,
                    desc: String,
                    isInterface: Boolean
                ): Unit =
                  result += ((opcode, target, name, desc))
              }
        },
        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
      )
    finally stream.close()
    result.toVector
  }

  private def routes(
      owner: String,
      method: String,
      target: String,
      name: String,
      descriptor: String = ""
  ): Unit =
    assertTrue(
      owner + "." + method + " must dispatch to " + target + "." + name,
      calls(owner, method, descriptor).exists(c =>
        c._2 == target && c._3 == name
      )
    )

  @Test
  def redstoneUsesTheRuntimeCapability(): Unit = {
    val open = calls("transmission/RedwirePart", "discoverOpen").filter(
      _._3 == "openConnections"
    )
    assertEquals(
      Seq(
        (
          Opcodes.INVOKEINTERFACE,
          fmp + "IRedstoneTile",
          "openConnections",
          "(I)I"
        )
      ),
      open
    )
  }

  @Test
  def placementRetainsTheGlassSoundWrapper(): Unit = {
    for (
      owner <- Seq(
        "illumination/ItemBaseLight",
        "illumination/ItemPartButtonCommons",
        "transmission/ItemWireCommon",
        "integration/ItemPartGate",
        "transportation/ItemPartPipe",
        "expansion/ItemSolarPanel"
      )
    ) {
      routes(
        owner,
        "onItemUse",
        "mrtjp/core/item/TItemSound$class",
        "onItemUse"
      )
      routes(
        owner,
        "mrtjp$core$item$TItemSound$$super$onItemUse",
        root + "core/TItemMultiPartPlacement$class",
        "onItemUse"
      )
    }
    routes(
      "core/TItemMultiPartPlacement$class",
      "onItemUse",
      fmp + "JItemMultiPart",
      "onItemUse"
    )
    routes(
      "illumination/ItemPartButtonCommons",
      "newPart",
      fmp + "minecraft/ButtonPart",
      "metaForSide"
    )
  }

  @Test
  def geometryAndEffectsHaveExplicitDispatch(): Unit = {
    for (owner <- Seq("illumination/BaseLightPart", "integration/GatePart")) {
      val stream =
        getClass.getClassLoader.getResourceAsStream(root + owner + ".class")
      try
        assertEquals(fmp + "JCuboidPart", new ClassReader(stream).getSuperName)
      finally stream.close()
    }

    for (
      owner <- Seq(
        "illumination/BaseLightPart",
        "integration/GatePart",
        "expansion/TFaceElectricalDevice$class",
        "transmission/TWireCommons$class",
        "transportation/SubcorePipePart"
      )
    )
      routes(owner, "occlusionTest", fmp + "NormalOcclusionTest", "apply")

    for (
      (owner, receiver) <- Seq(
        "integration/GatePart" -> "",
        "expansion/TFaceElectricalDevice$class" ->
          "Lmrtjp/projectred/expansion/TFaceElectricalDevice;"
      );
      method <- Seq("addHitEffects", "addDestroyEffects")
    )
      routes(
        owner,
        method,
        fmp + "IconHitEffects",
        method,
        "(" + receiver + "Lnet/minecraft/util/MovingObjectPosition;" +
          "Lnet/minecraft/client/particle/EffectRenderer;)V"
      )

    for (
      (method, target) <- Seq(
        "getSubParts" -> "subParts",
        "getCollisionBoxes" -> "collisionBoxes",
        "drawBreaking" -> "renderBreaking"
      )
    )
      routes(
        "expansion/TFaceElectricalDevice$class",
        method,
        fmp + "JCuboidPart",
        target
      )

    routes(
      "integration/ArrayGatePart",
      "mrtjp$projectred$integration$TArrayGatePart$$super$occlusionTest",
      root + "integration/GatePart",
      "occlusionTest"
    )
  }

  @Test
  def generatedLightTraitDelegatesThroughObject(): Unit = {
    assertEquals(
      Seq(
        (
          Opcodes.INVOKESTATIC,
          root + "illumination/LightMicroblockLogic",
          "lightValue",
          "(Ljava/lang/Object;)I"
        )
      ),
      calls("illumination/LightMicroblock", "getLightValue")
    )
    assertEquals(
      Seq(
        (
          Opcodes.INVOKESTATIC,
          root + "illumination/LightMicroblockLogic",
          "renderHalo",
          "(Ljava/lang/Object;I)V"
        )
      ),
      calls("illumination/LightMicroblock", "renderDynamic")
    )
  }

  @Test
  def packetDispatchStillUsesProjectRedsSwitch(): Unit = {
    for (
      owner <- Seq(
        "integration/GatePart",
        "transmission/WirePart",
        "transmission/FramedWirePart",
        "transportation/SubcorePipePart",
        "expansion/SolarPanelPart",
        "illumination/LightButtonPart"
      )
    )
      routes(owner, "read", root + "core/TSwitchPacket$class", "read")
  }
}
