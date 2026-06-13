package org.bg52.artifacts.entity;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.manager.MimicManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Controls the mimic's AI behavior each tick.
 * Handles target acquisition, jumping, landing, and damage dealing.
 *
 * The slime's AI is kept ON at all times because slimes require it for
 * their physics/movement system to work. We manually control velocity
 * every tick to suppress the slime's built-in random hopping.
 *
 * We cancel the slime's natural targeting via EntityTargetEvent in
 * MimicDamageListener.
 */
public class MimicAI extends BukkitRunnable {

    private enum AIState {
        IDLE,
        PRE_JUMP,
        AIRBORNE,
        POST_JUMP
    }

    @SuppressWarnings("unused")
    private final Artifacts plugin;
    private final MimicEntity mimic;
    private final MimicAnimator animator;
    private final MimicManager mimicManager;

    private AIState state = AIState.IDLE;
    private Player target = null;
    private int jumpCooldown = 20;
    private int stateTicks = 0;
    private int noTargetTicks = 0; // Tracks how long the mimic has no target

    // Jump direction tracking — re-enforced each tick during AIRBORNE
    // to prevent the slime's random hop AI from hijacking the trajectory
    private double jumpVelX = 0;
    private double jumpVelZ = 0;

    // Knockback window — when > 0, don't suppress horizontal velocity
    private int knockbackTicks = 0;

    // Tuning constants — lunges forward, not high
    private static final int PRE_JUMP_TICKS = 3;
    private static final int POST_JUMP_TICKS = 3;
    private static final int JUMP_COOLDOWN = 25;
    private static final double DETECTION_RANGE = 8.0;
    private static final double DETECTION_RANGE_SQ = DETECTION_RANGE * DETECTION_RANGE;
    private static final double ATTACK_RANGE = 1.5;
    private static final double JUMP_HORIZONTAL_SPEED = 0.45; // Controlled forward lunge
    private static final double JUMP_VERTICAL = 0.25; // Low hop, stays grounded
    private static final int MIN_AIR_TICKS = 4; // Short jumps land faster
    private static final int NO_TARGET_REVERT_TICKS = 100; // 5 seconds with no target → revert to chest
    private static final int KNOCKBACK_WINDOW_TICKS = 8; // Ticks to let knockback velocity play out

    public MimicAI(Artifacts plugin, MimicEntity mimic, MimicAnimator animator, MimicManager mimicManager) {
        this.plugin = plugin;
        this.mimic = mimic;
        this.animator = animator;
        this.mimicManager = mimicManager;
    }

    @Override
    public void run() {
        if (!mimic.isAlive()) {
            cancel();
            return;
        }

        Slime slime = mimic.getSlime();
        if (slime == null || !slime.isValid()) {
            cancel();
            return;
        }

        // Slime AI MUST stay on — slimes need it for their physics system.
        // We control all movement by overriding velocity each tick.
        slime.setAI(true);

        // Tick down knockback window
        if (knockbackTicks > 0)
            knockbackTicks--;

        updateTarget();

        MimicModel normalModel;

        switch (state) {
            case IDLE:
                normalModel = MimicModel.DORMANT;

                // Suppress slime's random hops — zero horizontal velocity
                // (unless in knockback window, let that play out)
                if (knockbackTicks <= 0) {
                    suppressHorizontalVelocity(slime);
                }

                if (target != null) {
                    noTargetTicks = 0; // Reset revert timer
                    jumpCooldown--;
                    if (jumpCooldown <= 0) {
                        state = AIState.PRE_JUMP;
                        stateTicks = PRE_JUMP_TICKS;
                    }
                } else {
                    // Track how long we've had no target
                    noTargetTicks++;
                    if (noTargetTicks >= NO_TARGET_REVERT_TICKS) {
                        // Revert to chest — no players around for too long
                        Slime revertSlime = mimic.getSlime();
                        if (revertSlime != null) {
                            mimicManager.revertMimic(revertSlime.getUniqueId());
                        }
                        return; // revertMimic already cancels the task
                    }
                }
                break;

            case PRE_JUMP:
                normalModel = MimicModel.HURT;
                noTargetTicks = 0; // Active, reset revert timer

                // Suppress slime's random hops during squat animation
                suppressHorizontalVelocity(slime);

                stateTicks--;
                if (stateTicks <= 0) {
                    applyJumpVelocity();
                    Location loc = slime.getLocation().add(0.5, 0, 0.5);
                    loc.getWorld().playSound(loc, "artifacts:entity.mimic.open", org.bukkit.SoundCategory.HOSTILE, 1.0f,
                            1.0f);
                    state = AIState.AIRBORNE;
                    stateTicks = 0;
                }
                break;

            case AIRBORNE:
                normalModel = MimicModel.JUMPING;
                stateTicks++;

                // Re-enforce our intended jump direction each tick.
                // This prevents the slime's built-in AI from hijacking the
                // trajectory with its own random hop velocity.
                Vector currentVel = slime.getVelocity();
                slime.setVelocity(new Vector(jumpVelX, currentVel.getY(), jumpVelZ));

                // Only check for landing after enough time in the air
                if (stateTicks > MIN_AIR_TICKS && slime.isOnGround()) {
                    state = AIState.POST_JUMP;
                    stateTicks = POST_JUMP_TICKS;
                    dealDamageToNearby();
                }
                // Safety: if airborne too long (4 seconds), force landing
                if (stateTicks > 80) {
                    state = AIState.POST_JUMP;
                    stateTicks = POST_JUMP_TICKS;
                }
                break;

            case POST_JUMP:
                normalModel = MimicModel.HURT;

                // Stop horizontal movement on landing
                suppressHorizontalVelocity(slime);

                stateTicks--;
                if (stateTicks <= 0) {
                    state = AIState.IDLE;
                    Location loc = slime.getLocation().add(0.5, 0, 0.5);
                    loc.getWorld().playSound(loc, "artifacts:entity.mimic.close", org.bukkit.SoundCategory.HOSTILE,
                            1.0f, 1.0f);
                    jumpCooldown = JUMP_COOLDOWN;
                }
                break;

            default:
                normalModel = MimicModel.DORMANT;
        }

        // Sync zombie AI and target with the slime's current state
        mimic.syncZombie(state != AIState.IDLE, target);

        // Ensure zombie remains mounted on the slime every tick
        mimic.ensureMounted();

        animator.tick(normalModel);
    }

    /**
     * Find or update the current target player.
     */
    private void updateTarget() {
        Slime slime = mimic.getSlime();
        Location loc = slime.getLocation();

        // Validate current target
        if (target != null) {
            if (!target.isOnline() || target.isDead()
                    || target.getGameMode() == GameMode.SPECTATOR
                    || target.getGameMode() == GameMode.CREATIVE
                    || !target.getWorld().equals(loc.getWorld())
                    || target.getLocation().distanceSquared(loc) > DETECTION_RANGE_SQ) {
                target = null;
            }
        }

        // Find new target
        if (target == null) {
            double closestDist = DETECTION_RANGE_SQ;
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.isDead())
                    continue;
                if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE)
                    continue;
                double dist = p.getLocation().distanceSquared(loc);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = p;
                }
            }
        }
    }

    /**
     * Apply jump velocity toward the target — leaps further and higher than a
     * normal slime.
     */
    private void applyJumpVelocity() {
        if (target == null) {
            state = AIState.IDLE;
            jumpCooldown = JUMP_COOLDOWN;
            return;
        }

        Slime slime = mimic.getSlime();
        Location slimeLoc = slime.getLocation();
        Location targetLoc = target.getLocation();

        Vector direction = targetLoc.toVector().subtract(slimeLoc.toVector());
        direction.setY(0);
        double distance = direction.length();

        if (distance > 0.1) {
            direction.normalize();
        } else {
            // Very close, small random direction
            direction = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize();
        }

        // Scale horizontal speed — further targets get more speed, capped
        // Minimum speed of 0.5 ensures the mimic always leaps noticeably
        double horizontalSpeed = Math.max(0.25, Math.min(distance * 0.12, JUMP_HORIZONTAL_SPEED));
        direction.multiply(horizontalSpeed);
        direction.setY(JUMP_VERTICAL);

        // Save intended horizontal velocity for re-enforcement during AIRBORNE
        jumpVelX = direction.getX();
        jumpVelZ = direction.getZ();

        slime.setVelocity(direction);
    }

    /**
     * Deal damage to nearby players when landing.
     */
    private void dealDamageToNearby() {
        Slime slime = mimic.getSlime();
        for (Entity entity : slime.getNearbyEntities(ATTACK_RANGE, ATTACK_RANGE, ATTACK_RANGE)) {
            if (entity instanceof Player) {
                Player p = (Player) entity;
                if (p.getGameMode() != GameMode.SPECTATOR && p.getGameMode() != GameMode.CREATIVE) {
                    p.damage(MimicEntity.ATTACK_DAMAGE, slime);
                }
            }
        }
    }

    /**
     * Suppress horizontal velocity to prevent slime random hopping.
     * Preserves Y velocity (gravity).
     */
    private void suppressHorizontalVelocity(Slime slime) {
        Vector vel = slime.getVelocity();
        if (Math.abs(vel.getX()) > 0.001 || Math.abs(vel.getZ()) > 0.001) {
            slime.setVelocity(new Vector(0, vel.getY(), 0));
        }
    }

    /**
     * Called when the mimic takes damage from a player.
     */
    public void onDamaged(Player attacker) {
        animator.onAttacked();
        if (target == null) {
            target = attacker;
            jumpCooldown = 10; // Quick retaliation
        }

        // Open knockback window so velocity isn't immediately suppressed
        knockbackTicks = KNOCKBACK_WINDOW_TICKS;

        // Apply knockback manually
        Slime slime = mimic.getSlime();
        if (slime != null && slime.isValid() && attacker != null) {
            Location slimeLoc = slime.getLocation();
            Location attackerLoc = attacker.getLocation();
            Vector knockback = slimeLoc.toVector().subtract(attackerLoc.toVector());
            knockback.setY(0);
            if (knockback.lengthSquared() > 0.01) {
                knockback.normalize().multiply(0.35);
                knockback.setY(0.15);
            } else {
                knockback = new Vector(0, 0.15, 0);
            }
            slime.setVelocity(slime.getVelocity().add(knockback));
        }
    }

    /**
     * Called when the mimic dies.
     */
    public void onDeath() {
        cancel();
    }

    public MimicEntity getMimicEntity() {
        return mimic;
    }

    public MimicAnimator getAnimator() {
        return animator;
    }

    public Player getTarget() {
        return target;
    }
}
