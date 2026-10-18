package mrtjp.projectred.gametests.lever;

import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.projectred.fabrication.IntegratedCircuit;
import mrtjp.projectred.fabrication.LeverICPart;
import mrtjp.projectred.gametests.GateFixture;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Fabrication" })
public class LeverRegressionTests {

    public static List<GameTestArguments> initialStates() {
        return Arrays.asList(GameTestArguments.named("switch-on", false), GameTestArguments.named("switch-off", true));
    }

    @GameTest(template = "gate", timeoutTicks = 20)
    @MethodSource("initialStates")
    public static void isolatedToggleRequestsPersistence(GameTestHelper helper, boolean initiallyOn) {
        IntegratedCircuit ic = GateFixture.workbench(helper);
        LeverICPart lever = new LeverICPart();
        NBTTagCompound initial = new NBTTagCompound();
        initial.setBoolean("on", initiallyOn);
        lever.load(initial);
        ic.setPart(1, 1, lever);
        helper.assertEquals(1, ic.parts().size(), "no neighboring component may mask the lever's save request");

        GateFixture.assertSaveRequested(helper, "workbench", () -> lever.readClientPacket(null));
        helper.assertEquals(!initiallyOn, lever.on(), "lever toggled");

        NBTTagCompound saved = new NBTTagCompound();
        ic.save(saved);
        IntegratedCircuit restored = new IntegratedCircuit();
        restored.load(saved);
        helper.assertEquals(!initiallyOn, ((LeverICPart) restored.getPart(1, 1)).on(), "saved circuit retains toggle");
        helper.succeed();
    }
}
