package mrtjp.projectred.illumination;

import codechicken.lib.vec.Vector3;
import codechicken.microblock.Microblock;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Registered by name before loading; FMP transforms this class into a generated trait interface. */
public abstract class LightMicroblock extends Microblock {

    public LightMicroblock() {
        super(0);
    }

    @Override
    public boolean shouldRenderDynamic() {
        return true;
    }

    @Override
    public int getLightValue() {
        return LightMicroblockLogic.lightValue(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderDynamic(Vector3 pos, float frame, int pass) {
        LightMicroblockLogic.renderHalo(this, pass);
    }
}
