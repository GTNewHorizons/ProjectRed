/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.projectred.fabrication.gui

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui._
import mrtjp.core.vec.{Point, Size}
import mrtjp.core.world.WorldLib
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.fabrication.gui.nodes.{ICToolsetNode, InfoNode, NewICNode, OpPreviewNode, PrefboardNode}
import mrtjp.projectred.fabrication.operations.{CircuitOpDefs, OpGate}
import mrtjp.projectred.fabrication.{FabricationProxy, IntegratedCircuit, TileICWorkbench}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

import java.math.MathContext


class GuiICWorkbench(val tile: TileICWorkbench) extends NodeGui(330, 256) {
  var pref: PrefboardNode = null
  var toolSets = Seq[ICToolsetNode]()

  override def onAddedToParent_Impl() {
    val clip = new ClipNode
    clip.position = Point(7, 18)
    clip.size = Size(252, 197)
    addChild(clip)

    val pan = new PanNode
    pan.size = Size(252, 197)
    pan.clampSlack = 35
    pan.dragTestFunction = { () => Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) }
    clip.addChild(pan)

    val opPreview = new OpPreviewNode()
    opPreview.position = Point(269, 30)
    addChild(opPreview)

    pref = new PrefboardNode(tile.circuit, op => {opPreview.updatePreview(op)})
    pref.position = Point(pan.size / 2 - pref.size / 2)
    pref.zPosition = -0.01 // Must be below pan/clip nodes
    pref.opPickDelegate = { op =>
      if (op == null) {
        // Reset rotation and configuration of selected Gate
        pref.currentOp match {
          case op: OpGate =>
            op.rotation = 0
            op.configuration = 0
          case _ =>
        }
        pref.currentOp = null
        pref.updatePreview()
      }
      toolSets.foreach(_.pickOp(op))
    }
    pan.addChild(pref)

    val toolbar = new TNode {}

    {
      import CircuitOpDefs._
      def addToolsetRange(name: String, from: OpDef, to: OpDef) {
        addToolset(name, (from.getID to to.getID).map(CircuitOpDefs(_)))
      }

      def addToolset(name: String, opset: Seq[OpDef]) {
        val toolset = new ICToolsetNode
        toolset.position = Point(17, 0) * toolbar.children.size
        toolset.title = name
        toolset.opSet = opset.map(_.getOp)
        toolset.setup()
        toolset.opSelectDelegate = { op =>
          pref.currentOp = op
          pref.updatePreview()
        }
        toolbar.addChild(toolset)
        toolSets :+= toolset
      }

      addToolset("", Seq(Erase))
      addToolset("Debug", Seq(Torch, Lever, Button))
      addToolset("", Seq(AlloyWire))
      addToolsetRange("Insulated wires", WhiteInsulatedWire, BlackInsulatedWire)
      addToolsetRange("Bundled cables", NeutralBundledCable, BlackBundledCable)
      addToolset("IOs", Seq(SimpleIO, BundledIO, AnalogIO))
      addToolset(
        "Primatives",
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
        "Timing and Clocks",
        Seq(
          PulseFormerGate,
          RepeaterGate,
          TimerGate,
          SequencerGate,
          StateCellGate
        )
      )
      addToolset(
        "Latches",
        Seq(SRLatchGate, ToggleLatchGate, TransparentLatchGate)
      )
      addToolset("Cells", Seq(NullCellGate, InvertCellGate, BufferCellGate))
      addToolset(
        "Misc",
        Seq(RandomizerGate, CounterGate, SynchronizerGate, DecRandomizerGate)
      )
    }

    addChild(toolbar)
    toolbar.position =
      Point(size.width / 2 - toolbar.calculateAccumulatedFrame.width / 2, 235)

    val dminus = new MCButtonNode
    dminus.position = Point(269, 175)
    dminus.size = Size(10, 10)
    dminus.text = "-"
    dminus.clickDelegate = { () => pref.decDetail() }
    addChild(dminus)

    val dplus = new MCButtonNode
    dplus.position = Point(309, 175)
    dplus.size = Size(10, 10)
    dplus.text = "+"
    dplus.clickDelegate = { () => pref.incDetail() }
    addChild(dplus)

    val sminus = new MCButtonNode
    sminus.position = Point(269, 207)
    sminus.size = Size(10, 10)
    sminus.text = "-"
    sminus.clickDelegate = { () => pref.decScale() }
    addChild(sminus)

    val splus = new MCButtonNode
    splus.position = Point(309, 207)
    splus.size = Size(10, 10)
    splus.text = "+"
    splus.clickDelegate = { () => pref.incScale() }
    addChild(splus)

    val reqNew = new MCButtonNode
    reqNew.position = Point(272, 133)
    reqNew.size = Size(44, 12)
    reqNew.text = "redraw"
    reqNew.clickDelegate = { () =>
      if (tile.hasBP) {
        val nic = new NewICNode
        nic.position = Point(size / 2) - Point(nic.size / 2)
        nic.completionDelegate = { () =>
          val ic = new IntegratedCircuit
          ic.name = nic.getName
          ic.size = nic.selectedBoardSize * 16
          tile.sendNewICToServer(ic)
        }
        addChild(nic)
        nic.pushZTo(5)
      }
    }
    addChild(reqNew)

    val info = new InfoNode
    info.position = Point(241, 18)
    info.zPosition = 1
    addChild(info)
  }

  override def drawBack_Impl(mouse: Point, frame: Float) {
    GL11.glColor4f(1, 1, 1, 1)
    PRResources.guiPrototyper.bind()
    Gui.func_146110_a(0, 0, 0, 0, size.width, size.height, 512, 512)

    GuiDraw.drawString("IC Workbench", 8, 6, Colors.GREY.argb, false)

    GuiDraw.drawStringC("detail", 273, 162, 42, 14, Colors.GREY.argb, false)
    GuiDraw.drawStringC(
      pref.detailLevel + "",
      279,
      175,
      30,
      10,
      Colors.GREY.argb,
      false
    )

    GuiDraw.drawStringC("scale", 273, 193, 42, 14, Colors.GREY.argb, false)
    GuiDraw.drawStringC(
      BigDecimal(pref.scale, new MathContext(2)) + "",
      279,
      207,
      30,
      10,
      Colors.GREY.argb,
      false
    )
  }
}

object GuiICWorkbench extends TGuiBuilder {
  override def getID = FabricationProxy.icWorkbenchGui

  @SideOnly(Side.CLIENT)
  override def buildGui(player: EntityPlayer, data: MCDataInput) = {
    WorldLib.getTileEntity(
      Minecraft.getMinecraft.theWorld,
      data.readCoord()
    ) match {
      case t: TileICWorkbench =>
        t.circuit.readDesc(data)
        new GuiICWorkbench(t)
      case _ => null
    }
  }
}