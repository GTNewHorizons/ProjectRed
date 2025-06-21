package mrtjp.projectred.fabrication.gui

import codechicken.lib.data.MCDataInput
import codechicken.lib.gui.GuiDraw
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.color.Colors
import mrtjp.core.gui._
import mrtjp.core.vec.{Point, Size, Vec2}
import mrtjp.core.world.WorldLib
import mrtjp.projectred.core.libmc.PRResources
import mrtjp.projectred.fabrication.gui.nodes._
import mrtjp.projectred.fabrication.operations.{CircuitOpDefs, OpGate}
import mrtjp.projectred.fabrication.{FabricationProxy, TileICWorkbench}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

import java.math.MathContext

class GuiICWorkbench(val tile: TileICWorkbench) extends NodeGui(330, 256) {
  var pref: PrefboardNode = null
  var toolSets = Seq[ICToolsetNode]()
  var configurationNode: TNode = null

  override def onAddedToParent_Impl() {
    val clip = new ClipNode
    clip.position = Point(7, 18)
    clip.size = Size(252, 197)
    addChild(clip)

    val opPreview = new OpPreviewNode()
    opPreview.position = Point(269, 18)
    addChild(opPreview)

    pref = new PrefboardNode(
      tile.circuit,
      tile.hasBP,
      op => { opPreview.updatePreview(op) },
      node => {
        if (configurationNode != null) {
          configurationNode.removeFromParent()
          configurationNode = null
        }
        configurationNode = node
        if (configurationNode != null) {
          configurationNode.position = Point(260, 17)
          addChild(configurationNode)
        }
      }
    )
    pref.zPosition = -0.01 // Must be below clip nodes
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
    clip.addChild(pref)
    if (tile.circuit.parts.nonEmpty) {
      pref.scaleGuiToCircuit()
    } else {
      pref.offset = Vec2(0, 0)
      pref.scale = 1.0d
    }

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
          if (configurationNode != null) {
            configurationNode.removeFromParent()
            configurationNode = null
          }
        }
        toolbar.addChild(toolset)
        toolSets :+= toolset
      }

      addToolset("", Seq(Erase))
      addToolset("", Seq(Cut))
      addToolset("", Seq(Copy))
      addToolset("", Seq(Paste))
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

    val textboxICName = new SimpleTextboxNode()
    textboxICName.position = Point(80, 4)
    textboxICName.size = Size(115, 11)
    textboxICName.text = tile.circuit.name
    textboxICName.textChangedDelegate = { () =>
      tile.sendICNameToServer(textboxICName.text)
    }
    addChild(textboxICName)

    val dminus = new MCButtonNode
    dminus.position = Point(269, 200)
    dminus.size = Size(10, 10)
    dminus.text = "-"
    dminus.clickDelegate = { () => pref.decDetail() }
    addChild(dminus)

    val dplus = new MCButtonNode
    dplus.position = Point(309, 200)
    dplus.size = Size(10, 10)
    dplus.text = "+"
    dplus.clickDelegate = { () => pref.incDetail() }
    addChild(dplus)

    val sminus = new MCButtonNode
    sminus.position = Point(268, 4)
    sminus.size = Size(10, 10)
    sminus.text = "-"
    sminus.clickDelegate = { () => pref.decScale() }
    addChild(sminus)

    val splus = new MCButtonNode
    splus.position = Point(308, 4)
    splus.size = Size(10, 10)
    splus.text = "+"
    splus.clickDelegate = { () => pref.incScale() }
    addChild(splus)

    val resetView = new MCButtonNode
    resetView.position = Point(200, 3)
    resetView.size = Size(60, 12)
    resetView.text = "Reset View"
    resetView.clickDelegate = { () =>
      if (tile.hasBP && tile.circuit.parts.nonEmpty) {
        pref.scaleGuiToCircuit()
      }
    }
    addChild(resetView)

    val info = new InfoNode
    info.position = Point(241, 18)
    info.zPosition = 1
    addChild(info)
  }

  override def keyPressed_Impl(
      c: Char,
      keycode: Int,
      consumed: Boolean
  ): Boolean = {
    import Keyboard._
    if (!consumed) keycode match {
      case KEY_W =>
        pref.offset += Vec2(0, -2 / pref.scale)
        true
      case KEY_A =>
        pref.offset += Vec2(-2 / pref.scale, 0)
        true
      case KEY_S =>
        pref.offset += Vec2(0, 2 / pref.scale)
        true
      case KEY_D =>
        pref.offset += Vec2(2 / pref.scale, 0)
        true
      case _ =>
        false
    }
    else false
  }

  override def drawBack_Impl(mouse: Point, frame: Float) {
    GL11.glColor4f(1, 1, 1, 1)
    PRResources.guiPrototyper.bind()
    Gui.func_146110_a(0, 0, 0, 0, size.width, size.height, 512, 512)

    GuiDraw.drawString("IC Workbench", 8, 6, Colors.GREY.argb, false)

    GuiDraw.drawStringC("detail", 273, 190, 42, 14, Colors.GREY.argb, false)
    GuiDraw.drawStringC(
      pref.detailLevel + "",
      279,
      200,
      30,
      10,
      Colors.GREY.argb,
      false
    )

    GuiDraw.drawStringC(
      BigDecimal(pref.scale, new MathContext(2)) + "",
      278,
      4,
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
