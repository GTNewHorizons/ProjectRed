package mrtjp.projectred.exploration.mixins;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import mrtjp.projectred.core.Configurator;

import javax.annotation.Nonnull;

public enum Mixins implements IMixins {

    MOSS_SPREADING(new MixinBuilder("Replace the vanilla moss cobble with a version that ticks & use the stonebricks random tick to spread moss.")
            .addCommonMixins("BlockMixin", "BlockStoneBrickMixin")
            .setApplyIf(Configurator::gen_SpreadingMoss)
            .setPhase(Phase.EARLY));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
