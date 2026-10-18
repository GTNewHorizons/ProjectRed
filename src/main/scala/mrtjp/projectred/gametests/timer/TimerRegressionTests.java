package mrtjp.projectred.gametests.timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.gtnewhorizons.horizonqa.api.GameTestArguments;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.annotation.MethodSource;

import mrtjp.projectred.fabrication.ICGateDefinition;
import mrtjp.projectred.fabrication.LeverICPart;
import mrtjp.projectred.fabrication.SequentialGateICPart;
import mrtjp.projectred.gametests.GateFixture;
import mrtjp.projectred.integration.GateDefinition;
import mrtjp.projectred.integration.ITimerGuiLogic;
import mrtjp.projectred.integration.SequentialGatePart;

@GameTestHolder(value = "projectred", requiredMods = { "ProjRed|Integration", "ProjRed|Fabrication" })
public class TimerRegressionTests {

    public static List<GameTestArguments> implementations() {
        return Arrays.asList(GameTestArguments.named("world", false), GameTestArguments.named("ic", true));
    }

    public static List<GameTestArguments> timedComponents() {
        return Arrays.asList(
                GameTestArguments.named("world-timer", false, false),
                GameTestArguments.named("ic-timer", true, false),
                GameTestArguments.named("world-state-cell", false, true),
                GameTestArguments.named("ic-state-cell", true, true));
    }

    @GameTest(template = "gate", timeoutTicks = 140)
    @MethodSource("implementations")
    public static void twoTickReductionChangesActualTimerPeriod(GameTestHelper helper, boolean ic) {
        TimerAccess timer = timer(helper, ic, false);
        List<Long> starts = observePulses(helper, timer.signal);
        helper.startSequence()
                .thenWaitUntil("first timer pulse reaches wire", 50, () -> helper.assertTrue(starts.size() >= 1))
                .thenExecute("reduce public interval from forty to thirty-eight", () -> timer.interval.accept(38))
                .thenWaitUntil(
                        "observe two pulses at the new interval",
                        90,
                        () -> helper.assertTrue(starts.size() >= 3))
                .thenExecute("verify measured spacing, not just stored configuration", () -> {
                    helper.assertEquals(38L, starts.get(1) - starts.get(0), "first changed timer period");
                    helper.assertEquals(38L, starts.get(2) - starts.get(1), "subsequent timer period");
                }).thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 55)
    @MethodSource("implementations")
    public static void twoTickReductionAdvancesStateCellCompletion(GameTestHelper helper, boolean ic) {
        TimerAccess cell = timer(helper, ic, true);
        List<Long> starts = observePulses(helper, cell.signal);
        long[] released = { -1 };
        helper.startSequence().thenExecute("configure and trigger the state cell", () -> {
            cell.interval.accept(38);
            cell.trigger.run();
            released[0] = helper.getWorld().getTotalWorldTime();
        }).thenWaitUntil("completion pulse reaches wire", 45, () -> helper.assertEquals(1, starts.size())).thenExecute(
                "completion uses the shortened countdown",
                () -> helper
                        .assertEquals(36L, starts.get(0) - released[0], "interval includes the final two-tick pulse"))
                .thenSucceed();
    }

    @GameTest(template = "gate", timeoutTicks = 20)
    @MethodSource("timedComponents")
    public static void intervalEditRequestsPersistence(GameTestHelper helper, boolean ic, boolean stateCell) {
        TimerAccess timer = timer(helper, ic, stateCell);
        GateFixture.assertSaveRequested(helper, timer.owner, () -> timer.interval.accept(38));
        helper.succeed();
    }

    private static List<Long> observePulses(GameTestHelper helper, IntSupplier signal) {
        List<Long> starts = new ArrayList<>();
        boolean[] previous = { false };
        helper.onEachTick("record output pulse starts", () -> {
            boolean high = signal.getAsInt() > 0;
            if (high && !previous[0]) starts.add(helper.getWorld().getTotalWorldTime());
            previous[0] = high;
        });
        return starts;
    }

    private static TimerAccess timer(GameTestHelper helper, boolean ic, boolean stateCell) {
        TimerAccess access = new TimerAccess();
        if (ic) {
            SequentialGateICPart gate = GateFixture
                    .ic(helper, stateCell ? ICGateDefinition.StateCell() : ICGateDefinition.Timer());
            mrtjp.projectred.fabrication.ITimerGuiLogic logic = (mrtjp.projectred.fabrication.ITimerGuiLogic) gate
                    .getLogic();
            access.interval = n -> logic.setTimerMax(gate, n);
            access.signal = GateFixture.output(gate)::getSignal;
            access.owner = "workbench";
            if (stateCell) {
                LeverICPart trigger = GateFixture.input(gate, 2);
                access.trigger = () -> {
                    trigger.readClientPacket(null);
                    trigger.readClientPacket(null);
                };
            }
        } else {
            SequentialGatePart gate = (SequentialGatePart) GateFixture
                    .world(helper, stateCell ? GateDefinition.StateCell() : GateDefinition.Timer());
            ITimerGuiLogic logic = (ITimerGuiLogic) gate.getLogicSequential();
            access.interval = n -> logic.setTimerMax(gate, n);
            access.signal = GateFixture.output(helper)::getSignal;
            access.owner = "gate";
            access.trigger = () -> {
                helper.setRedstoneInput("port2", 15);
                helper.setRedstoneInput("port2", 0);
            };
        }
        return access;
    }

    private static class TimerAccess {

        IntConsumer interval;
        IntSupplier signal;
        Runnable trigger;
        String owner;
    }
}
