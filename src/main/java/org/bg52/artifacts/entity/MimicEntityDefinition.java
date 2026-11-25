package org.bg52.artifacts.entity;

import org.bg52.synthetix.entity.ai.EntityAISpec;
import org.bg52.synthetix.entity.ai.EntityAIType;
import org.bg52.synthetix.entity.animation.AnimationTrack;
import org.bg52.synthetix.entity.animation.Keyframe;
import org.bg52.synthetix.entity.model.BoneTransform;
import org.bg52.synthetix.entity.model.CustomEntityDefinition;
import org.bg52.synthetix.entity.model.EntityBoneSpec;
import org.bg52.synthetix.entity.model.EntityHitboxSpec;
import org.bg52.synthetix.entity.model.EntityLootSpec;
import org.bg52.synthetix.entity.model.EntitySoundSpec;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Super-modular CustomEntityDefinition for the Mimic Entity.
 *
 * Utilizes Synthetix's Tri-Layer Virtual Entity Architecture:
 * - Driver Layer: Base living mob proxy providing authoritative physics, navigation, and 1.20.6 attributes.
 * - Visual Skeletal Rig: Zero-ticking virtual ItemDisplay bones with client-side SLERP matrix interpolation.
 * - Spatial Interaction Matrix: Sub-voxel Interaction hitboxes (Body 1.0x, Mouth/Core Weakpoint 2.0x).
 * - Animation Timeline Tracks: Procedural keyframes for idle, walk/hop, attack, hurt, and death.
 */
public class MimicEntityDefinition {

    private static CustomEntityDefinition definition;

    public static synchronized CustomEntityDefinition getDefinition() {
        if (definition != null) {
            return definition;
        }

        CustomEntityDefinition def = new CustomEntityDefinition("mimic");
        def.setOwningPlugin("PaperArtifacts");
        def.setDisplayName("<red><bold>Mimic</bold></red>");
        def.setCustomNameVisible(false);
        // Slime base mob: Size 2 matches authentic chest hitbox (1.04x1.04x1.04) and hopping attack locomotion
        def.setBaseEntityType(EntityType.SLIME);
        def.setSlimeSize(2);

        // ── 1.20.6 Engine Attributes ──
        def.setMaxHealth(60.0);       // 30 hearts
        def.setAttackDamage(5.0);     // 2.5 hearts
        def.setMovementSpeed(0.30);
        def.setStepHeight(0.6);
        def.setScale(1.0);
        def.setFollowRange(16.0);
        def.setGravity(0.08);

        // ── Skeletal Bone Hierarchy ──
        // Root bone (Body & Lower Chest): Center offset Y = 0.5 places bottom of chest flush on the ground.
        // Hinge is at Z = -7/16 (-0.4375f), Latch is at Z = +7/16 (+0.4375f) facing forward toward target.
        EntityBoneSpec rootBone = def.getBone("root");
        if (rootBone == null) {
            rootBone = new EntityBoneSpec("root", null, new Vector3f(0.0f, 0.0f, 0.0f), Material.CARROT_ON_A_STICK, 201010);
            def.addBone(rootBone);
        } else {
            rootBone.setMaterial(Material.CARROT_ON_A_STICK);
            rootBone.setCustomModelData(201010);
            rootBone.setPivot(new Vector3f(0.0f, 0.0f, 0.0f));
        }
        rootBone.setItemModel("artifacts:mimic_bottom");
        rootBone.getInitialTransform().translation.set(0.0f, 0.5f, 0.0f);

        // Sub-voxel body hitbox on root bone (0.95w x 0.6h)
        EntityHitboxSpec bodyHitbox = new EntityHitboxSpec("body", "root", 0.95f, 0.6f, 1.0);
        bodyHitbox.setOffset(new Vector3f(0.0f, 0.3f, 0.0f));
        rootBone.setHitbox(bodyHitbox);

        // Lid bone: Pivots precisely at the back hinge (Y = 1/16 = 0.0625, Z = -7/16 = -0.4375 relative to chest center)
        EntityBoneSpec lidBone = new EntityBoneSpec("lid", "root", new Vector3f(0.0f, 0.0625f, -0.4375f), Material.CARROT_ON_A_STICK, 201011);
        lidBone.setItemModel("artifacts:mimic_lid");

        // Weakpoint mouth hitbox mounted to lid (positioned toward front latch at Z = +0.35)
        EntityHitboxSpec mouthHitbox = new EntityHitboxSpec("mouth", "lid", 0.70f, 0.45f, 2.0);
        mouthHitbox.setWeakpoint(true);
        mouthHitbox.setOffset(new Vector3f(0.0f, 0.15f, 0.35f));
        lidBone.setHitbox(mouthHitbox);
        def.addBone(lidBone);

        // ── Animation Tracks ──
        def.addAnimation(createIdleTrack());
        def.addAnimation(createWalkTrack());
        def.addAnimation(createAttackTrack());
        def.addAnimation(createHurtTrack());
        def.addAnimation(createDeathTrack());

        // ── AI Configuration ──
        EntityAISpec ai = def.getAi();
        ai.setAiType(EntityAIType.HOSTILE_LEAP);
        ai.setTargetPlayers(true);
        ai.setLookAtPlayer(true);
        ai.setWander(true);
        ai.setLeapCooldownTicks(20);
        ai.setLeapHorizontalSpeed(0.24);
        ai.setLeapVerticalSpeed(0.32);
        ai.setPreLeapTicks(8);
        ai.setPostLeapTicks(12);
        ai.setAttackRange(1.5);
        ai.setAttackCooldownTicks(20);

        // Prevent slime driver from splitting into child slimes on death
        def.setAllowSplitting(false);

        // ── Sound Audio Profile ──
        EntitySoundSpec sounds = def.getSounds();
        sounds.setAmbientSound("artifacts:entity.mimic.open");
        sounds.setHurtSound("artifacts:entity.mimic.hurt");
        sounds.setDeathSound("artifacts:entity.mimic.death");
        sounds.setStepSound("artifacts:entity.mimic.close");
        sounds.setVolume(1.0f);
        sounds.setPitch(1.0f);

        // ── Death Drops Profile ──
        EntityLootSpec loot = def.getLoot();
        loot.setExp(15);
        loot.getDrops().add(new EntityLootSpec.DropEntry("IRON_INGOT", 1, 3, 0.6));
        loot.getDrops().add(new EntityLootSpec.DropEntry("GOLD_INGOT", 1, 2, 0.4));
        loot.getDrops().add(new EntityLootSpec.DropEntry("EMERALD", 1, 1, 0.25));

        // ── Bedrock / Geyser Protocol Compatibility ──
        org.bg52.synthetix.bedrock.BedrockModelSpec bedrockSpec = new org.bg52.synthetix.bedrock.BedrockModelSpec(
                Material.CARROT_ON_A_STICK, 201004, null, "artifacts_mimic_dormant"
        );
        bedrockSpec.setSlot(org.bukkit.inventory.EquipmentSlot.HEAD);
        bedrockSpec.addStateSpec(new org.bg52.synthetix.bedrock.BedrockStateSpec(
                "idle", Material.CARROT_ON_A_STICK, 201004, null, "artifacts_mimic_dormant"
        ));
        bedrockSpec.addStateSpec(new org.bg52.synthetix.bedrock.BedrockStateSpec(
                "walk", Material.CARROT_ON_A_STICK, 201006, null, "artifacts_mimic_jumping"
        ));
        bedrockSpec.addStateSpec(new org.bg52.synthetix.bedrock.BedrockStateSpec(
                "attack", Material.CARROT_ON_A_STICK, 201003, null, "artifacts_mimic_attacked_jumping"
        ));
        bedrockSpec.addStateSpec(new org.bg52.synthetix.bedrock.BedrockStateSpec(
                "hurt", Material.CARROT_ON_A_STICK, 201005, null, "artifacts_mimic_hurt"
        ));
        bedrockSpec.addStateSpec(new org.bg52.synthetix.bedrock.BedrockStateSpec(
                "death", Material.CARROT_ON_A_STICK, 201007, null, "artifacts_mimic_death"
        ));
        def.setBedrockSpec(bedrockSpec);

        definition = def;
        return def;
    }

    /**
     * Idle Animation Track: Completely closed chest resting flush on the ground.
     */
    private static AnimationTrack createIdleTrack() {
        AnimationTrack track = new AnimationTrack("idle", 20.0f, true);

        // Rest: completely closed chest, base height Y = 0.5
        track.addKeyframe("root", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Subtle swell / breathing
        track.addKeyframe("root", new Keyframe(10.0f,
                new BoneTransform(new Vector3f(0.0f, 0.515f, 0.0f), new Quaternionf(), new Vector3f(1.01f, 0.99f, 1.01f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(10.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        track.addKeyframe("root", new Keyframe(20.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(20.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        return track;
    }

    /**
     * Walk Track: Chest hopping locomotion matching mimic.animation.json.
     * Hops mid-air with lid opening to authentic 35 degrees, settling back to 0.
     */
    private static AnimationTrack createWalkTrack() {
        AnimationTrack track = new AnimationTrack("walk", 14.0f, true);

        // Tick 0: Ground contact
        track.addKeyframe("root", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0.0f, 0.48f, 0.0f), new Quaternionf(), new Vector3f(1.04f, 0.94f, 1.04f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 3: Launch into air - mouth begins to open (25 deg)
        Quaternionf takeoffRot = new Quaternionf().rotateX((float) Math.toRadians(-15.0));
        Quaternionf lidTakeoff = new Quaternionf().rotateX((float) Math.toRadians(-25.0));
        track.addKeyframe("root", new Keyframe(3.0f,
                new BoneTransform(new Vector3f(0.0f, 0.65f, 0.04f), takeoffRot, new Vector3f(0.96f, 1.04f, 0.96f)),
                Keyframe.InterpolationType.LINEAR));
        track.addKeyframe("lid", new Keyframe(3.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidTakeoff, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));

        // Tick 7: Apex of hop - maximum mid-air extension (lid opens to 35 deg, not over-extended)
        Quaternionf apexRot = new Quaternionf();
        Quaternionf lidApex = new Quaternionf().rotateX((float) Math.toRadians(-35.0));
        track.addKeyframe("root", new Keyframe(7.0f,
                new BoneTransform(new Vector3f(0.0f, 0.82f, 0.06f), apexRot, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(7.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidApex, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 10: Descent towards ground - closing mouth (15 deg)
        Quaternionf descentRot = new Quaternionf().rotateX((float) Math.toRadians(10.0));
        Quaternionf lidDescent = new Quaternionf().rotateX((float) Math.toRadians(-15.0));
        track.addKeyframe("root", new Keyframe(10.0f,
                new BoneTransform(new Vector3f(0.0f, 0.60f, 0.04f), descentRot, new Vector3f(1.0f, 1.02f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));
        track.addKeyframe("lid", new Keyframe(10.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidDescent, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));

        // Tick 12: Touchdown - impact squash and clack shut
        track.addKeyframe("root", new Keyframe(12.0f,
                new BoneTransform(new Vector3f(0.0f, 0.48f, 0.0f), new Quaternionf(), new Vector3f(1.05f, 0.94f, 1.05f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(12.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 14: Elastic recovery to rest
        track.addKeyframe("root", new Keyframe(14.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(14.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        return track;
    }

    /**
     * Attack Track: Snapping lunge bite matching mimic.animation.json.
     * Rears back opening maw to designed 65 degrees, then clamps shut on target.
     */
    private static AnimationTrack createAttackTrack() {
        AnimationTrack track = new AnimationTrack("attack", 14.0f, false);

        // Tick 0: Rest
        track.addKeyframe("root", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 4: Wind-up - rear back and open lid to designed 65 degrees (not over 90)
        Quaternionf rearBack = new Quaternionf().rotateX((float) Math.toRadians(-20.0));
        Quaternionf lidMaw = new Quaternionf().rotateX((float) Math.toRadians(-65.0));
        track.addKeyframe("root", new Keyframe(4.0f,
                new BoneTransform(new Vector3f(0.0f, 0.55f, -0.08f), rearBack, new Vector3f(1.02f, 1.05f, 1.02f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(4.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidMaw, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 8: Lunge snap bite - thrust forward and clamp lid tightly shut (slight over-clamp to -5 deg)
        Quaternionf snapForward = new Quaternionf().rotateX((float) Math.toRadians(5.0));
        Quaternionf lidClamp = new Quaternionf().rotateX((float) Math.toRadians(5.0));
        track.addKeyframe("root", new Keyframe(8.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.12f), snapForward, new Vector3f(1.06f, 0.95f, 1.06f)),
                Keyframe.InterpolationType.LINEAR));
        track.addKeyframe("lid", new Keyframe(8.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidClamp, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));

        // Tick 14: Recover to rest
        track.addKeyframe("root", new Keyframe(14.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(14.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        return track;
    }

    /**
     * Hurt Track: Rapid shudder and recoil matching mimic.animation.json.
     * Lid briefly jolts to 25 degrees max without over-opening.
     */
    private static AnimationTrack createHurtTrack() {
        AnimationTrack track = new AnimationTrack("hurt", 6.0f, false);

        // Tick 0: Start
        track.addKeyframe("root", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));
        track.addKeyframe("lid", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));

        // Tick 2: Recoil back and jolt lid open (25 deg max) with subtle left-right shudder
        Quaternionf flinch = new Quaternionf().rotateX((float) Math.toRadians(-8.0)).rotateZ((float) Math.toRadians(-5.0));
        Quaternionf lidJolt = new Quaternionf().rotateX((float) Math.toRadians(-25.0));
        track.addKeyframe("root", new Keyframe(2.0f,
                new BoneTransform(new Vector3f(0.0f, 0.52f, -0.06f), flinch, new Vector3f(0.97f, 1.04f, 0.97f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(2.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidJolt, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 6: Settle to rest
        track.addKeyframe("root", new Keyframe(6.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(6.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        return track;
    }

    /**
     * Death Track: Topples backwards 90 degrees with lid opening to 60 degrees.
     */
    private static AnimationTrack createDeathTrack() {
        AnimationTrack track = new AnimationTrack("death", 20.0f, false);

        // Tick 0: Rest
        track.addKeyframe("root", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0.0f, 0.50f, 0.0f), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));
        track.addKeyframe("lid", new Keyframe(0.0f,
                new BoneTransform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.LINEAR));

        // Tick 6: Stumble backward with lid gaping to 45 deg
        Quaternionf stumble = new Quaternionf().rotateX((float) Math.toRadians(45.0));
        Quaternionf lidDeath = new Quaternionf().rotateX((float) Math.toRadians(-45.0));
        track.addKeyframe("root", new Keyframe(6.0f,
                new BoneTransform(new Vector3f(0.0f, 0.38f, 0.18f), stumble, new Vector3f(1.03f, 1.03f, 1.03f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(6.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidDeath, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        // Tick 16-20: Collapse backward onto ground (90 deg) and lid rests open at 60 deg
        Quaternionf fallen = new Quaternionf().rotateX((float) Math.toRadians(90.0));
        Quaternionf lidFlat = new Quaternionf().rotateX((float) Math.toRadians(-60.0));
        track.addKeyframe("root", new Keyframe(16.0f,
                new BoneTransform(new Vector3f(0.0f, 0.20f, 0.40f), fallen, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(16.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidFlat, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        track.addKeyframe("root", new Keyframe(20.0f,
                new BoneTransform(new Vector3f(0.0f, 0.20f, 0.40f), fallen, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));
        track.addKeyframe("lid", new Keyframe(20.0f,
                new BoneTransform(new Vector3f(0, 0, 0), lidFlat, new Vector3f(1.0f, 1.0f, 1.0f)),
                Keyframe.InterpolationType.EASE_IN_OUT));

        return track;
    }
}
