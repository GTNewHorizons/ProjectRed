package mrtjp.projectred.gametests;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import mrtjp.core.vec.Point;
import mrtjp.core.vec.Size;
import mrtjp.projectred.ProjectRedFabrication;
import mrtjp.projectred.fabrication.AlloyWireICPart;
import mrtjp.projectred.fabrication.ICGateDefinition;
import mrtjp.projectred.fabrication.IntegratedCircuit;
import mrtjp.projectred.fabrication.ItemICBlueprint;
import mrtjp.projectred.fabrication.LeverICPart;
import mrtjp.projectred.fabrication.SequentialGateICPart;
import mrtjp.projectred.fabrication.TileICWorkbench;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.integration.GatePart;
import mrtjp.projectred.transmission.RedAlloyWirePart;

public final class GateFixture {

    private GateFixture() {}

    public static NBTTagCompound tag(GateDefinition.GateDef definition) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("subID", (byte) definition.ordinal());
        tag.setBoolean("nolegacy", true);
        tag.setInteger("max", 4);
        tag.setInteger("inc", 1);
        tag.setInteger("dec", 1);
        tag.setInteger("pmax", 38);
        tag.setLong("pelapsed", -1);
        return tag;
    }

    public static GatePart world(GameTestHelper helper, GateDefinition.GateDef definition) {
        return world(helper, definition, tag(definition));
    }

    public static GatePart world(GameTestHelper helper, GateDefinition.GateDef definition, NBTTagCompound tag) {
        GatePart gate = (GatePart) MultiPartRegistry.createPart(definition.partname(), false);
        gate.load(tag);
        TestPos origin = helper.absolute("gate");
        TestPos output = helper.absolute("port0");
        for (int rotation = 0; rotation < 4; rotation++) {
            gate.setRotation(rotation);
            BlockCoord adjacent = new BlockCoord(origin.x(), origin.y(), origin.z())
                    .offset(gate.absoluteDir(gate.toAbsolute(0)));
            if (adjacent.x == output.x() && adjacent.y == output.y() && adjacent.z == output.z()) {
                add(helper, "gate", gate);
                return gate;
            }
        }
        helper.fail("gate output does not face the port0 fixture label");
        return gate;
    }

    public static void add(GameTestHelper helper, String label, TMultiPart part) {
        TestPos p = helper.absolute(label);
        TileMultipart.addPart(helper.getWorld(), new BlockCoord(p.x(), p.y(), p.z()), part);
    }

    public static RedAlloyWirePart output(GameTestHelper helper) {
        RedAlloyWirePart wire = new RedAlloyWirePart();
        wire.preparePlacement(1, 0);
        add(helper, "port0", wire);
        return wire;
    }

    public static IntegratedCircuit workbench(GameTestHelper helper) {
        helper.setBlock("workbench", ProjectRedFabrication.icBlock(), 0);
        TileICWorkbench bench = helper.assertTileEntityPresent(TileICWorkbench.class, "workbench");
        IntegratedCircuit design = new IntegratedCircuit();
        design.size_$eq(new Size(16, 16));
        ItemStack blueprint = new ItemStack(ProjectRedFabrication.itemICBlueprint());
        ItemICBlueprint.saveIC(design, blueprint);
        TestPos pos = helper.absolute("workbench");
        FakePlayer player = helper.spawnFakePlayer("PRBP" + pos.x() + "_" + pos.z());
        player.setSneaking(false);
        helper.afterTest(() -> player.inventory.mainInventory[player.inventory.currentItem] = null);
        helper.assertTrue(helper.simulateRightClick("workbench", player, blueprint), "blueprint insertion was handled");
        helper.assertTrue(bench.hasBP(), "workbench must contain a blueprint");
        helper.assertEquals(0, blueprint.stackSize, "workbench consumed the inserted blueprint");
        helper.assertEquals(design.size(), bench.getIC().size(), "workbench loaded the blueprint's board dimensions");
        return bench.getIC();
    }

    public static SequentialGateICPart ic(GameTestHelper helper, ICGateDefinition.ICGateDef definition) {
        IntegratedCircuit ic = workbench(helper);
        SequentialGateICPart gate = new SequentialGateICPart();
        gate.preparePlacement(0, definition.ordinal());
        ic.setPart(2, 2, gate);
        return gate;
    }

    public static AlloyWireICPart output(SequentialGateICPart gate) {
        AlloyWireICPart wire = new AlloyWireICPart();
        Point p = new Point(gate.x(), gate.y()).offset(gate.toAbsolute(0));
        gate.world().setPart(p, wire);
        return wire;
    }

    public static LeverICPart input(SequentialGateICPart gate, int port) {
        LeverICPart lever = new LeverICPart();
        gate.world().setPart(new Point(gate.x(), gate.y()).offset(gate.toAbsolute(port)), lever);
        return lever;
    }

    public static void assertSaveRequested(GameTestHelper helper, String owner, Runnable change) {
        TestPos p = helper.absolute(owner);
        Chunk chunk = helper.getWorld().getChunkFromBlockCoords(p.x(), p.z());
        boolean dirty = chunk.isModified;
        chunk.isModified = false;
        try {
            change.run();
            helper.assertTrue(chunk.isModified, owner + " was not notified of a persistent state change");
        } finally {
            chunk.isModified |= dirty;
        }
    }
}
