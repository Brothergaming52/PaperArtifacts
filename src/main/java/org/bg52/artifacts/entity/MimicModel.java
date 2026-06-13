package org.bg52.artifacts.entity;

/**
 * All 6 visual model states for the mimic, mapping to both
 * CustomModelData values (1.14-1.21.2) and item model keys (1.21.3+).
 */
public enum MimicModel {

    ATTACKED_DORMANT(201001, "mimic_attacked_dormant"),
    ATTACKED_HURT(201002, "mimic_attacked_hurt"),
    ATTACKED_JUMPING(201003, "mimic_attacked_jumping"),
    DORMANT(201004, "mimic_dormant"),
    HURT(201005, "mimic_hurt"),
    JUMPING(201006, "mimic_jumping");

    private final int customModelData;
    private final String itemModelKey;

    MimicModel(int customModelData, String itemModelKey) {
        this.customModelData = customModelData;
        this.itemModelKey = itemModelKey;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * The key portion of the item model NamespacedKey (namespace is always "artifacts").
     */
    public String getItemModelKey() {
        return itemModelKey;
    }

    /**
     * Get the attacked variant of a base model.
     * DORMANT → ATTACKED_DORMANT, JUMPING → ATTACKED_JUMPING, HURT → ATTACKED_DORMANT
     */
    public static MimicModel getAttackedVariant(MimicModel base) {
        if (base == null) return ATTACKED_DORMANT;
        switch (base) {
            case JUMPING:
            case ATTACKED_JUMPING:
                return ATTACKED_JUMPING;
            case DORMANT:
            case ATTACKED_DORMANT:
                return ATTACKED_DORMANT;
            default:
                // HURT and any other state map to attacked dormant
                return ATTACKED_DORMANT;
        }
    }
}
