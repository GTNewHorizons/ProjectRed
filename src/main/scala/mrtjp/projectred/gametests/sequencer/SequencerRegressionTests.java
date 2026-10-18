package mrtjp.projectred.gametests.sequencer;

import java.util.Arrays;
import java.util.List;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.projectred.fabrication.ICGateDefinition;
import mrtjp.projectred.fabrication.SequentialGateICPart;
import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.integration.Sequencer;
import mrtjp.projectred.integration.SequentialGatePart;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Integration", "ProjRed|Fabrication" })
public class SequencerRegressionTests {

    public static List<GameTestArguments> implementations() {
        return Arrays.asList(GameTestArguments.named("world", false), GameTestArguments.named("ic", true));
    }

    @GameTest(template = "gate", timeoutTicks = 20)
    @MethodSource("implementations")
    public static void intervalEditRequestsPersistence(GameTestHelper helper, boolean ic) {
        if (ic) {
            SequentialGateICPart gate = GateFixture.ic(helper, ICGateDefinition.Sequencer());
            mrtjp.projectred.fabrication.Sequencer logic = (mrtjp.projectred.fabrication.Sequencer) gate.getLogic();
            GateFixture.assertSaveRequested(helper, "workbench", () -> logic.setTimerMax(gate, 41));
            helper.assertEquals(41, logic.getTimerMax(), "sequencer interval edit was not applied");
        } else {
            SequentialGatePart gate = (SequentialGatePart) GateFixture.world(helper, GateDefinition.Sequencer());
            Sequencer logic = (Sequencer) gate.getLogicSequential();
            GateFixture.assertSaveRequested(helper, "gate", () -> logic.setTimerMax(gate, 41));
            helper.assertEquals(41, logic.getTimerMax(), "sequencer interval edit was not applied");
        }
        helper.succeed();
    }
}
