package org.bg52.artifacts.entity;

/**
 * Handles mimic animation transitions.
 * Each tick, MimicAI provides the "normal" model based on physics state,
 * and the animator either displays that or overlays the attacked reaction.
 */
public class MimicAnimator {

    private final MimicEntity mimic;

    // Attacked overlay state
    private boolean isAttacked = false;
    private int attackedPhase = 0;         // 0=none, 1=attacked_base, 2=attacked_hurt
    private int attackedTicksRemaining = 0;
    private MimicModel attackedBaseModel = MimicModel.ATTACKED_DORMANT;

    private static final int ATTACKED_BASE_TICKS = 8;
    private static final int ATTACKED_HURT_TICKS = 4;

    public MimicAnimator(MimicEntity mimic) {
        this.mimic = mimic;
    }

    /**
     * Called every tick by MimicAI.
     * @param normalModel the model the AI wants to display based on current physics state
     */
    public void tick(MimicModel normalModel) {
        if (isAttacked) {
            if (attackedPhase == 1) {
                mimic.setModel(attackedBaseModel);
                attackedTicksRemaining--;
                if (attackedTicksRemaining <= 0) {
                    attackedPhase = 2;
                    attackedTicksRemaining = ATTACKED_HURT_TICKS;
                }
            } else if (attackedPhase == 2) {
                mimic.setModel(MimicModel.ATTACKED_HURT);
                attackedTicksRemaining--;
                if (attackedTicksRemaining <= 0) {
                    isAttacked = false;
                    attackedPhase = 0;
                    // Transition back to normal model
                    mimic.setModel(normalModel);
                }
            }
        } else {
            mimic.setModel(normalModel);
        }
    }

    /**
     * Trigger the attacked reaction animation.
     * Captures the current visual category and overlays the attacked variant.
     */
    public void onAttacked() {
        attackedBaseModel = MimicModel.getAttackedVariant(mimic.getCurrentModel());
        isAttacked = true;
        attackedPhase = 1;
        attackedTicksRemaining = ATTACKED_BASE_TICKS;
    }

    public boolean isInAttackedAnimation() {
        return isAttacked;
    }
}
