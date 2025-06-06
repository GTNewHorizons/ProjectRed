/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui

import codechicken.lib.render.uv.UVScale
import codechicken.lib.render.{CCModel, CCRenderState}
import codechicken.lib.vec.{Scale, Transformation, TransformationList, Translation}
import mrtjp.projectred.fabrication.ICComponentStore.faceModels
import mrtjp.projectred.fabrication.IntegratedCircuit
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

import scala.collection.JavaConversions._


object PrefboardRenderer {
  private var boardModels = Map[(Int, Int), Seq[CCModel]]()
  private var cornerModels = Map[(Int, Int), Seq[CCModel]]()
  private var edgeModels = Map[(Int, Int), Seq[CCModel]]()

  private def createBoardModel(w: Int, h: Int): Seq[CCModel] =
    faceModels.map(_.copy.apply(new UVScale(w, h)))

  private def createCornerModel(w: Int, h: Int): Seq[CCModel] = {
    val corners = Seq((0, 0), (0, h - 1), (w - 1, h - 1), (w - 1, 0)).map {
      pair =>
        new TransformationList(
          new Scale(1.0 / w, 1, 1.0 / h),
          new Translation(pair._1 * 1.0 / w, 0, pair._2 * 1.0 / h)
        )
    }

    faceModels.map { m =>
      var models = Seq[CCModel]()
      for (t <- corners)
        models :+= m.copy.apply(t)
      CCModel.combine(models)
    }
  }

  private def createEdgeModel(w: Int, h: Int): Seq[CCModel] = {
    val edges =
      Seq((0, 0, 1, h), (0, 0, w, 1), (w - 1, 0, 1, h), (0, h - 1, w, 1)).map {
        pair =>
          (
            new TransformationList(
              new Scale(1.0 / w, 1, 1.0 / h),
              new Scale(pair._3, 1, pair._4),
              new Translation(pair._1 * 1.0 / w, 0, pair._2 * 1.0 / h)
            ),
            new UVScale(pair._3, pair._4)
          )
      }

    faceModels.map { m =>
      var models = Seq[CCModel]()
      for ((t, uvt) <- edges)
        models :+= m.copy.apply(t).apply(uvt)
      CCModel.combine(models)
    }
  }

  private def getBoardModel(w: Int, h: Int) = {
    if (!boardModels.contains((w, h)))
      boardModels += (w, h) -> createBoardModel(w, h)
    boardModels((w, h))
  }

  private def getCornerModel(w: Int, h: Int) = {
    if (!cornerModels.contains((w, h)))
      cornerModels += (w, h) -> createCornerModel(w, h)
    cornerModels((w, h))
  }

  private def getEdgeModel(w: Int, h: Int) = {
    if (!edgeModels.contains((w, h)))
      edgeModels += (w, h) -> createEdgeModel(w, h)
    edgeModels((w, h))
  }

  def render(circuit: IntegratedCircuit, t: Transformation, ortho: Boolean) {
    val w = circuit.size.width
    val h = circuit.size.height

    def bind(s: String) {
      val r = new ResourceLocation(
        "projectred",
        "textures/blocks/fabrication/" + s + ".png"
      )
      Minecraft.getMinecraft.getTextureManager.bindTexture(r)
    }

    val state = CCRenderState.instance
    state.resetInstance()
    state.pullLightmapInstance()
    state.setDynamicInstance()

    for (
      (tex, models) <- Seq(
        ("prefboard", getBoardModel(w, h)),
        ("prefboard_edge", getEdgeModel(w, h)),
        ("prefboard_corner", getCornerModel(w, h))
      )
    ) {
      bind(tex)
      state.startDrawingInstance()
      models(if (ortho) 1 else 0).render(t)
      state.drawInstance()
    }
  }
}
