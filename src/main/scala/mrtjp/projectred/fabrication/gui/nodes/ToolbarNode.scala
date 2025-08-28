/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui.nodes

import codechicken.lib.gui.GuiDraw
import mrtjp.core.gui.{IconButtonNode, TNode}
import mrtjp.core.vec.{Point, Size}
import mrtjp.projectred.fabrication.IntegratedCircuit
import mrtjp.projectred.fabrication.operations.{CircuitOp, CircuitOpDefs}
import mrtjp.projectred.fabrication.operations.CircuitOpDefs.{
  ANDGate,
  AlloyWire,
  AnalogIO,
  BlackBundledCable,
  BlackInsulatedWire,
  BufferCellGate,
  BufferGate,
  BundledIO,
  Button,
  Copy,
  CounterGate,
  Cut,
  DecRandomizerGate,
  Erase,
  InvertCellGate,
  Lever,
  MultiplexerGate,
  NANDGate,
  NORGate,
  NOTGate,
  NeutralBundledCable,
  NullCellGate,
  ORGate,
  OpDef,
  Paste,
  PulseFormerGate,
  RandomizerGate,
  RepeaterGate,
  SRLatchGate,
  SequencerGate,
  SimpleIO,
  StateCellGate,
  SynchronizerGate,
  TimerGate,
  ToggleLatchGate,
  Torch,
  TransparentLatchGate,
  WhiteInsulatedWire,
  XNORGate,
  XORGate
}
import net.minecraft.util.EnumChatFormatting.{AQUA, ITALIC}
import net.minecraft.util.StatCollector

import scala.collection.JavaConversions._

class ToolbarNode(circuit: IntegratedCircuit, onPick: CircuitOp => Unit)
    extends TNode {

  private def translate(str: String): String = {
    StatCollector.translateToLocal(str)
  }

  def buildToolbar(): Unit = {
    addToolset("", Seq(Erase))
    addToolset("", Seq(Cut))
    addToolset("", Seq(Copy))
    addToolset("", Seq(Paste))
    addToolset(
      translate("gui.projectred.fabrication.debug"),
      Seq(Torch, Lever, Button)
    )
    addToolset("", Seq(AlloyWire))
    addToolsetRange(
      translate("gui.projectred.fabrication.insulated_wires"),
      WhiteInsulatedWire,
      BlackInsulatedWire
    )
    addToolsetRange(
      translate("gui.projectred.fabrication.bundled_cables"),
      NeutralBundledCable,
      BlackBundledCable
    )
    addToolset(
      translate("gui.projectred.fabrication.ios"),
      Seq(SimpleIO, BundledIO, AnalogIO)
    )
    addToolset(
      translate("gui.projectred.fabrication.primitives"),
      Seq(
        ORGate,
        NORGate,
        NOTGate,
        ANDGate,
        NANDGate,
        XORGate,
        XNORGate,
        BufferGate,
        MultiplexerGate
      )
    )
    addToolset(
      translate("gui.projectred.fabrication.timing_and_clocks"),
      Seq(
        PulseFormerGate,
        RepeaterGate,
        TimerGate,
        SequencerGate,
        StateCellGate
      )
    )
    addToolset(
      translate("gui.projectred.fabrication.latches"),
      Seq(SRLatchGate, ToggleLatchGate, TransparentLatchGate)
    )
    addToolset("Cells", Seq(NullCellGate, InvertCellGate, BufferCellGate))
    addToolset(
      translate("gui.projectred.fabrication.misc"),
      Seq(RandomizerGate, CounterGate, SynchronizerGate, DecRandomizerGate)
    )
    addImExport()
  }

  def selectOp(op: CircuitOp): Unit = {
    if (op == null) {
      children
        .collect { case b: ICToolsetNode => b }
        .foreach(node => {
          node.setUnfocused()
        })
    } else {
      children
        .collect { case b: ICToolsetNode => b }
        .foreach(node => {
          node.select(op)
        })
    }
  }

  private def addToolsetRange(name: String, from: OpDef, to: OpDef): Unit = {
    addToolset(name, (from.getID to to.getID).map(CircuitOpDefs(_)))
  }

  private def addToolset(name: String, opset: Seq[OpDef]): Unit = {
    val toolset = new ICToolsetNode(name, { op => onPick(op) })
    toolset.position = Point(17, 0) * children.size
    toolset.opSet = opset.map(_.getOp)
    toolset.setup()
    addChild(toolset)
  }

  private def addImExport(): Unit = {
    var button: IconButtonNode = null
    button = new IconButtonNode {
      override def drawFront_Impl(mouse: Point, rframe: Float): Unit = {
        val p = parent.children
          .filter { p => p.isInstanceOf[IconButtonNode] }
          .head
          .position
        if (
          p.x < mouse.x && mouse.x < p.x + 16 && p.y < mouse.y && mouse.y < p.y + 16
        ) {
          translateToScreen()
          val Point(mx, my) = parent.convertPointToScreen(mouse)
          GuiDraw.drawMultilineTip(
            mx + 12,
            my - 16,
            Seq(
              ITALIC.toString + StatCollector.translateToLocal(
                "gui.projectred.fabrication.im_ex_port"
              )
            )
          )
          translateFromScreen()
        }
      }
    }
    button.size = Size(16, 16)
    button.position = Point(17, 0) * children.size
    button.clickDelegate = { () =>
      var exportGui: StringExport = null
      exportGui = new StringExport(
        circuit,
        () => {
          exportGui.removeFromParent()
          selectOp(Paste.op)
          onPick(Paste.op)
        }
      )
      addChild(exportGui)
    }
    addChild(button)
  }
}
