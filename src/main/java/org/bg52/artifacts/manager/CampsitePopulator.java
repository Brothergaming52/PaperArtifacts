package org.bg52.artifacts.manager;

import org.bg52.artifacts.Artifacts;
import org.bg52.artifacts.util.VersionUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bed;
import org.bukkit.generator.BlockPopulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CampsitePopulator extends BlockPopulator {

    private final Artifacts plugin;
    private CampsiteConfig cfg;

    // ─── Weighted block tables (mirrors the JSON configs) ───────────────────────

    private static final WeightedList<String> BEDS = new WeightedList<String>()
            .add("RED_BED", 1).add("YELLOW_BED", 1).add("CYAN_BED", 1)
            .add("GRAY_BED", 1).add("MAGENTA_BED", 1).add("GREEN_BED", 1);

    private static final WeightedList<String> CRAFTING_STATIONS = new WeightedList<String>()
            .add("CRAFTING_TABLE", 5).add("SMITHING_TABLE", 5)
            .add("FLETCHING_TABLE", 5).add("CARTOGRAPHY_TABLE", 5)
            .add("ANVIL", 2).add("CHIPPED_ANVIL", 2)
            .add("DAMAGED_ANVIL", 1);

    private static final WeightedList<String> DECORATIONS = new WeightedList<String>()
            .add("POTTED_DEAD_BUSH", 2).add("POTTED_BAMBOO", 2)
            .add("POTTED_RED_TULIP", 2).add("BREWING_STAND", 1)
            .add("CANDLE_CAKE", 1); // falls back to CAKE on older versions

    private static final WeightedList<String> FURNACES = new WeightedList<String>()
            .add("FURNACE", 2).add("BLAST_FURNACE", 1).add("SMOKER", 1);

    private static final WeightedList<String> FURNACE_CHIMNEYS = new WeightedList<String>()
            .add("COBBLESTONE_WALL", 2).add("COBBLED_DEEPSLATE_WALL", 2)
            .add("STONE_BRICK_WALL", 1).add("DEEPSLATE_BRICK_WALL", 1);

    private static final WeightedList<String> LIT_CAMPFIRES = new WeightedList<String>()
            .add("CAMPFIRE", 9).add("SOUL_CAMPFIRE", 1);

    private static final WeightedList<String> LIGHT_SOURCES = new WeightedList<String>()
            .add("LANTERN", 4).add("SOUL_LANTERN", 1)
            .add("CANDLE", 4); // candles count as one entry for simplicity

    private static final String UNLIT_CAMPFIRE = "CAMPFIRE";
    private static final String FLOOR_BLOCK = "OAK_PLANKS";

    // ─── Loot Generation Classes and Pools ─────────────────────────────────────

    static class LootEntry {
        String material;
        int min;
        int max;
        boolean isEnchantedBook;

        LootEntry(String material) {
            this(material, 1, 1);
        }

        LootEntry(String material, int min, int max) {
            this.material = material;
            this.min = min;
            this.max = max;
            this.isEnchantedBook = false;
        }

        LootEntry enchantedBook() {
            this.isEnchantedBook = true;
            return this;
        }

        public org.bukkit.inventory.ItemStack generate(Random random) {
            Material mat = VersionUtil.safeOf(material);
            if (mat == null) return null;
            int count = min;
            if (max > min) count += random.nextInt((max - min) + 1);
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat, count);
            if (isEnchantedBook && mat.name().contains("ENCHANTED_BOOK")) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                    org.bukkit.inventory.meta.EnchantmentStorageMeta emeta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
                    org.bukkit.enchantments.Enchantment[] enchants = org.bukkit.enchantments.Enchantment.values();
                    if (enchants.length > 0) {
                        org.bukkit.enchantments.Enchantment enc = enchants[random.nextInt(enchants.length)];
                        int level = 1;
                        if (enc.getMaxLevel() > 1) {
                            level = 1 + random.nextInt(enc.getMaxLevel());
                        }
                        emeta.addStoredEnchant(enc, level, true);
                        item.setItemMeta(emeta);
                    }
                }
            }
            return item;
        }
    }

    private static final WeightedList<LootEntry> CHEST_TOOLS = new WeightedList<LootEntry>()
            .add(new LootEntry("DIAMOND_PICKAXE"), 2).add(new LootEntry("DIAMOND_AXE"), 1)
            .add(new LootEntry("DIAMOND_SHOVEL"), 1).add(new LootEntry("GOLDEN_PICKAXE"), 4)
            .add(new LootEntry("GOLDEN_AXE"), 2).add(new LootEntry("GOLDEN_SHOVEL"), 2)
            .add(new LootEntry("IRON_PICKAXE"), 6).add(new LootEntry("IRON_AXE"), 3)
            .add(new LootEntry("IRON_SHOVEL"), 3).add(new LootEntry("IRON_HELMET"), 2)
            .add(new LootEntry("IRON_CHESTPLATE"), 2).add(new LootEntry("IRON_LEGGINGS"), 2)
            .add(new LootEntry("IRON_BOOTS"), 2).add(new LootEntry("CHAINMAIL_HELMET"), 1)
            .add(new LootEntry("CHAINMAIL_CHESTPLATE"), 1).add(new LootEntry("CHAINMAIL_LEGGINGS"), 1)
            .add(new LootEntry("CHAINMAIL_BOOTS"), 1);

    private static final WeightedList<LootEntry> CHEST_JUNK = new WeightedList<LootEntry>()
            .add(new LootEntry("GUNPOWDER", 2, 8), 5).add(new LootEntry("ROTTEN_FLESH", 2, 8), 5)
            .add(new LootEntry("SPIDER_EYE", 2, 8), 5).add(new LootEntry("STRING", 2, 8), 5)
            .add(new LootEntry("PAPER", 2, 8), 5).add(new LootEntry("BONE", 2, 8), 5)
            .add(new LootEntry("STICK", 2, 8), 3).add(new LootEntry("GLASS_BOTTLE", 2, 8), 3)
            .add(new LootEntry("LEATHER", 2, 8), 3).add(new LootEntry("FLINT", 2, 8), 3)
            .add(new LootEntry("FEATHER", 2, 8), 3);

    private static final WeightedList<LootEntry> CHEST_ORES = new WeightedList<LootEntry>()
            .add(new LootEntry("RAW_COPPER", 2, 8), 3).add(new LootEntry("RAW_IRON", 2, 8), 3)
            .add(new LootEntry("RAW_GOLD", 2, 8), 3).add(new LootEntry("COAL", 4, 8), 6)
            .add(new LootEntry("DIAMOND", 1, 4), 1);

    private static final WeightedList<LootEntry> CHEST_TREASURE = new WeightedList<LootEntry>()
            .add(new LootEntry("ENCHANTED_BOOK").enchantedBook(), 8)
            .add(new LootEntry("GOLDEN_APPLE"), 4)
            .add(new LootEntry("ENCHANTED_GOLDEN_APPLE"), 1);

    private static final WeightedList<LootEntry> BARREL_POOL = new WeightedList<LootEntry>()
            .add(new LootEntry("COD", 4, 16), 1).add(new LootEntry("SALMON", 4, 16), 1)
            .add(new LootEntry("ROTTEN_FLESH", 4, 16), 1).add(new LootEntry("BONE", 2, 10), 1)
            .add(new LootEntry("PAPER", 4, 16), 1).add(new LootEntry("SUGAR_CANE", 4, 16), 1)
            .add(new LootEntry("WHEAT", 4, 16), 1).add(new LootEntry("BOOK", 4, 16), 1)
            .add(new LootEntry("SUGAR", 4, 16), 1).add(new LootEntry("COAL", 4, 16), 1)
            .add(new LootEntry("TNT", 1, 4), 1).add(new LootEntry("COBWEB", 1, 4), 1)
            .add(new LootEntry("IRON_INGOT", 1, 8), 1).add(new LootEntry("GOLD_INGOT", 1, 8), 1)
            .add(new LootEntry("COPPER_INGOT", 1, 8), 1).add(new LootEntry("DIAMOND", 1, 4), 1)
            .add(new LootEntry("EMERALD", 1, 4), 1).add(new LootEntry("CARROT", 4, 12), 1)
            .add(new LootEntry("POTATO", 4, 12), 1).add(new LootEntry("APPLE", 4, 12), 1)
            .add(new LootEntry("COBBLESTONE", 16, 64), 1).add(new LootEntry("RAIL", 8, 32), 1)
            .add(new LootEntry("MINECART", 1, 1), 1);

    // Horizontal faces
    private static final BlockFace[] H_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public CampsitePopulator(Artifacts plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getCampsiteConfig();
    }

    public void reloadConfig(CampsiteConfig cfg) {
        this.cfg = cfg;
    }

    // ─── Main populate entry point ───────────────────────────────────────────────

    @Override
    public void populate(World world, Random random, Chunk chunk) {
        if (cfg.getCount() <= 0)
            return;
        
        // Check if campsites are enabled for this world
        if (!cfg.isWorldEnabled(world.getName()))
            return;

        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;

        // ~1 in 10 rarity filter (mirrors "rarity_filter chance: 10")
        if (random.nextInt(10) != 0)
            return;

        // Count-gated: try cfg.getCount() times but only 1 per chunk on average at
        // default
        // (The original uses a custom placement modifier that limits globally,
        // here we do one attempt per chunk that passes the rarity filter)
        for (int attempt = 0; attempt < 1; attempt++) {
            int lx = random.nextInt(16);
            int lz = random.nextInt(16);
            int x = baseX + lx;
            int z = baseZ + lz;

            int y = findSuitableY(world, x, z, random);
            if (y == Integer.MIN_VALUE)
                continue;

            if (cfg.isMinimalistCampsites()) {
                placeMinimalistCampsite(world, x, y, z, random);
            } else {
                placeCampsite(world, x, y, z, random);
            }
        }
    }

    // ─── Y-position search (mirrors environment_scan + campsite_height_range) ───

    private int findSuitableY(World world, int x, int z, Random random) {
        int minY = clampedMinY(world);
        int maxY = Math.min(cfg.getMaxY(), world.getMaxHeight() - 10);

        if (minY >= maxY)
            return Integer.MIN_VALUE;

        // Pick a random Y in range, then scan downward for a solid floor with air above
        int startY = minY + random.nextInt(maxY - minY);

        for (int y = startY; y > minY; y--) {
            Block floor = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);

            if (!floor.getType().isAir() && floor.getType().isSolid()
                    && above.getType().isAir()) {
                // Check ceiling clearance
                if (ceilingClearance(world, x, y + 1, z) >= cfg.getMaxCeilingHeight()) {
                    // Check 5x5 area flatness (±2 blocks)
                    if (isSufficientlyFlat(world, x, y, z)) {
                        return y + 1; // stand on the floor
                    }
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private int clampedMinY(World world) {
        int worldMin;
        try {
            java.lang.reflect.Method method = world.getClass().getMethod("getMinHeight");
            worldMin = (Integer) method.invoke(world); // 1.18+
        } catch (Exception e) {
            worldMin = 0; // pre-1.18
        }
        return Math.max(cfg.getMinY(), worldMin + 1);
    }

    private int ceilingClearance(World world, int x, int y, int z) {
        int count = 0;
        int maxH = world.getMaxHeight();
        for (int dy = 0; dy < 16 && (y + dy) < maxH; dy++) {
            if (!world.getBlockAt(x, y + dy, z).getType().isAir())
                break;
            count++;
        }
        return count;
    }

    private boolean isSufficientlyFlat(World world, int cx, int floorY, int cz) {
        // Mirror surface_flatness_filter: floor blocks within ±2 radius should be solid
        int solidCount = 0;
        int total = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) < 2 || Math.abs(dz) < 2) { // ring-shaped check
                    Block b = world.getBlockAt(cx + dx, floorY - 1, cz + dz);
                    total++;
                    if (!b.getType().isAir())
                        solidCount++;
                }
            }
        }
        return solidCount >= total * 0.6; // at least 60% solid
    }

    // ─── Minimalist campsite (single chest / mimic) ───────────────────────────

    private void placeMinimalistCampsite(World world, int x, int y, int z, Random random) {
        BlockFace facing = randomHorizontalFace(random);
        placeChest(world, x, y, z, facing.getOppositeFace(), random);
    }

    // ─── Full campsite ────────────────────────────────────────────────────────

    /**
     * Direct port of CampsiteFeature.place().
     *
     * Layout (viewed from above, origin = O):
     * Clear a ring 5×5 (radius 2), then:
     * - Place floor (oak planks where missing)
     * - Place campfire at origin
     * - Pick a random direction D, go 2 blocks out:
     * 33% chance: barrels in that wall
     * 67% chance: bed + barrel + optional light source
     * - Rotate 90°, go 2 blocks out: crafting table + furnace + chest
     */
    private void placeCampsite(World world, int x, int y, int z, Random random) {
        // ── Clear air ring ──────────────────────────────────────────────────────
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) < 2 || Math.abs(dz) < 2) {
                        Block b = world.getBlockAt(x + dx, y + dy, z + dz);
                        if (!b.getType().isAir()) {
                            safeSetType(b, VersionUtil.caveAir());
                        }
                    }
                }
            }
        }

        // ── Floor ───────────────────────────────────────────────────────────────
        placeFloor(world, x, y, z, random);

        // ── Campfire at origin ──────────────────────────────────────────────────
        placeCampfire(world, x, y, z, random);

        // ── Side A: bed or barrels ──────────────────────────────────────────────
        BlockFace dirA = randomHorizontalFace(random);
        int ax = x + dirA.getModX() * 2;
        int az = z + dirA.getModZ() * 2;

        if (random.nextInt(3) == 0) {
            // Barrels along the wall
            for (BlockFace side : new BlockFace[] { clockwise(dirA), counterClockwise(dirA) }) {
                int bx = ax + side.getModX();
                int bz = az + side.getModZ();
                placeBarrel(world, bx, y, bz, random);
                if (random.nextInt(3) == 0) {
                    placeBarrel(world, bx, y + 1, bz, random);
                }
            }
        } else {
            // Bed
            BlockFace bedDir = random.nextBoolean() ? clockwise(dirA) : counterClockwise(dirA);
            placeBed(world, ax, y, az, bedDir, random);
            // Barrel beside the bed
            int barrelX = ax + bedDir.getModX();
            int barrelZ = az + bedDir.getModZ();
            placeBarrel(world, barrelX, y, barrelZ, random);
            // Optional light source above barrel
            if (random.nextFloat() < 0.5f) {
                placeLightSource(world, barrelX, y + 1, barrelZ, random);
            }
        }

        // ── Side B: crafting + furnace + chest ──────────────────────────────────
        BlockFace dirB = random.nextBoolean() ? clockwise(dirA) : counterClockwise(dirA);
        int bx = x + dirB.getModX() * 2;
        int bz = z + dirB.getModZ() * 2;

        // Three positions along side B
        List<int[]> positions = new ArrayList<>();
        for (BlockFace side : new BlockFace[] { clockwise(dirB), BlockFace.SELF, counterClockwise(dirB) }) {
            positions.add(new int[] { bx + side.getModX(), bz + side.getModZ() });
        }
        Collections.shuffle(positions, random);

        BlockFace inward = opposite(dirB);
        placeCraftingStation(world, positions.get(0)[0], y, positions.get(0)[1], inward, random);
        placeFurnace(world, positions.get(1)[0], y, positions.get(1)[1], inward, random);
        placeChest(world, positions.get(2)[0], y, positions.get(2)[1], inward, random);
    }

    // ─── Floor placement ─────────────────────────────────────────────────────

    private void placeFloor(World world, int x, int y, int z, Random random) {
        Material floorMat = VersionUtil.safeOf(FLOOR_BLOCK);
        Material cobblestone = VersionUtil.safeOf("COBBLESTONE");
        Material cobbledDeepslate = VersionUtil.safeOf("COBBLED_DEEPSLATE");

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) < 2 || Math.abs(dz) < 2) {
                    Block b = world.getBlockAt(x + dx, y - 1, z + dz);
                    Material current = b.getType();
                    if (current.isAir() || !b.getType().isSolid()) {
                        safeSetType(b, floorMat);
                    } else if (random.nextBoolean()) {
                        // Replace deepslate/stone with cobble variants
                        String name = current.name();
                        if (name.equals("DEEPSLATE")) {
                            safeSetType(b, cobbledDeepslate != null ? cobbledDeepslate : cobblestone);
                        } else if (name.equals("STONE")) {
                            safeSetType(b, cobblestone);
                        }
                    }
                }
            }
        }
    }

    // ─── Campfire ─────────────────────────────────────────────────────────────

    private void placeCampfire(World world, int x, int y, int z, Random random) {
        boolean lit = cfg.isAllowLightSources() && random.nextFloat() < 0.10f;
        String name = lit ? LIT_CAMPFIRES.pick(random) : UNLIT_CAMPFIRE;
        Material mat = VersionUtil.of(name);
        if (mat == null)
            mat = VersionUtil.safeOf("TORCH"); // fallback on old servers
        Block b = world.getBlockAt(x, y, z);
        safeSetType(b, mat);
    }

    // ─── Bed ─────────────────────────────────────────────────────────────────

    private void placeBed(World world, int x, int y, int z, BlockFace facing, Random random) {
        String bedName = BEDS.pick(random);
        Material bedMat = VersionUtil.of(bedName);
        if (bedMat == null)
            bedMat = VersionUtil.safeOf("RED_BED");
        if (bedMat == null)
            return; // beds not available (pre-1.12?)

        Block head = world.getBlockAt(x, y, z);
        Block foot = world.getBlockAt(x - facing.getModX(), y, z - facing.getModZ());

        safeSetBed(head, bedMat, facing, true);
        safeSetBed(foot, bedMat, facing, false);
    }

    private void safeSetBed(Block block, Material bedMat, BlockFace facing, boolean isHead) {
        try {
            block.setType(bedMat, false);
            BlockData data = block.getBlockData();
            if (data instanceof Bed) {
                Bed bed = (Bed) data;
                bed.setFacing(facing);
                bed.setPart(isHead ? Bed.Part.HEAD : Bed.Part.FOOT);
                block.setBlockData(bed, false);
            }
        } catch (Exception ignored) {
            // Old API — just set the block without state
            block.setType(bedMat, false);
        }
    }

    // ─── Barrel ──────────────────────────────────────────────────────────────

    private void placeBarrel(World world, int x, int y, int z, Random random) {
        Material mat = VersionUtil.of("BARREL");
        if (mat == null) {
            // Fallback: plain chest
            mat = VersionUtil.safeOf("CHEST");
        }
        Block b = world.getBlockAt(x, y, z);
        safeSetType(b, mat);
        // Barrels on 1.14+ can be directional
        try {
            BlockData data = b.getBlockData();
            if (data instanceof Directional) {
                Directional directional = (Directional) data;
                if (random.nextBoolean()) {
                    directional.setFacing(BlockFace.UP);
                } else {
                    directional.setFacing(randomHorizontalFace(random));
                }
                b.setBlockData(directional, false);
            }
        } catch (Exception ignored) {
        }
        
        populateBarrelLoot(b, random);
    }

    // ─── Light source ─────────────────────────────────────────────────────────

    private void placeLightSource(World world, int x, int y, int z, Random random) {
        if (!cfg.isAllowLightSources())
            return;
        boolean lit = random.nextFloat() < 0.30f;
        String name;
        if (lit) {
            name = LIGHT_SOURCES.pick(random);
        } else {
            // unlit = candle (fallback: torch)
            name = "CANDLE";
        }
        Material mat = VersionUtil.of(name);
        if (mat == null)
            mat = VersionUtil.safeOf("TORCH");
        if (mat != null)
            safeSetType(world.getBlockAt(x, y, z), mat);
    }

    // ─── Crafting station ────────────────────────────────────────────────────

    private void placeCraftingStation(World world, int x, int y, int z, BlockFace facing, Random random) {
        String name = CRAFTING_STATIONS.pick(random);
        Material mat = VersionUtil.of(name);
        if (mat == null)
            mat = VersionUtil.safeOf("CRAFTING_TABLE");

        Block b = world.getBlockAt(x, y, z);
        safeSetType(b, mat);
        safeSetFacing(b, facing);

        // Optional decoration on top
        if (random.nextInt(3) == 0) {
            String deco = DECORATIONS.pick(random);
            Material decoMat = VersionUtil.of(deco);
            if (decoMat != null)
                safeSetType(world.getBlockAt(x, y + 1, z), decoMat);
        }
    }

    // ─── Furnace ─────────────────────────────────────────────────────────────

    private void placeFurnace(World world, int x, int y, int z, BlockFace facing, Random random) {
        String name = FURNACES.pick(random);
        Material mat = VersionUtil.of(name);
        if (mat == null)
            mat = VersionUtil.safeOf("FURNACE");

        Block b = world.getBlockAt(x, y, z);
        safeSetType(b, mat);
        safeSetFacing(b, facing);

        // Optional chimney above
        if (random.nextBoolean()) {
            String chimney = FURNACE_CHIMNEYS.pick(random);
            Material chimneyMat = VersionUtil.of(chimney);
            if (chimneyMat == null)
                chimneyMat = VersionUtil.safeOf("COBBLESTONE_WALL");
            if (chimneyMat != null)
                safeSetType(world.getBlockAt(x, y + 1, z), chimneyMat);
        }
    }

    // ─── Chest or Mimic ──────────────────────────────────────────────────────

    private void placeChest(World world, int x, int y, int z, BlockFace facing, Random random) {
        Block block = world.getBlockAt(x, y, z);

        if (random.nextFloat() < cfg.getMimicChance()) {
            Material chest = VersionUtil.safeOf("CHEST");
            safeSetType(block, chest);
            safeSetFacing(block, facing);
            plugin.getMimicManager().addMimicChest(block.getLocation());
            return;
        }

        boolean trapped = random.nextFloat() < cfg.getTrappedChestChance();
        if (trapped) {
            // Place TNT below, trapped chest above
            Material tnt = VersionUtil.safeOf("TNT");
            safeSetType(world.getBlockAt(x, y - 1, z), tnt);
            Material trappedChest = VersionUtil.safeOf("TRAPPED_CHEST");
            safeSetType(block, trappedChest);
            safeSetFacing(block, BlockFace.values()[random.nextInt(4)]); // random H face
            populateChestLoot(block, random);
        } else {
            Material chest = VersionUtil.safeOf("CHEST");
            safeSetType(block, chest);
            safeSetFacing(block, facing);
            populateChestLoot(block, random);
        }
    }

    // ─── Block state helpers ─────────────────────────────────────────────────

    private void safeSetType(Block block, Material mat) {
        if (mat == null || mat == Material.AIR) {
            // Don't clear blocks unnecessarily; cave_air is fine but skip null
            return;
        }
        try {
            block.setType(mat, false);
        } catch (Exception ignored) {
        }
    }

    private void safeSetFacing(Block block, BlockFace facing) {
        try {
            BlockData data = block.getBlockData();
            if (data instanceof Directional) {
                ((Directional) data).setFacing(facing);
                block.setBlockData(data, false);
            }
        } catch (Exception ignored) {
        }
    }

    // ─── Direction helpers ───────────────────────────────────────────────────

    private BlockFace randomHorizontalFace(Random random) {
        return H_FACES[random.nextInt(H_FACES.length)];
    }

    private BlockFace clockwise(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            default:
                return BlockFace.EAST;
        }
    }

    private BlockFace counterClockwise(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.NORTH;
            default:
                return BlockFace.WEST;
        }
    }

    private BlockFace opposite(BlockFace face) {
        return face.getOppositeFace();
    }

    // ─── Loot Population ──────────────────────────────────────────────────────

    private void populateChestLoot(Block block, Random random) {
        if (random.nextDouble() > cfg.getChestLootChance()) return;
        org.bukkit.block.BlockState state = block.getState();
        if (state instanceof org.bukkit.block.Container) {
            org.bukkit.inventory.Inventory inv = ((org.bukkit.block.Container) state).getInventory();
            inv.clear();

            int tools = 1 + random.nextInt(3);
            for (int i = 0; i < tools; i++) addItemSafe(inv, CHEST_TOOLS.pick(random).generate(random));

            int junk = 1 + random.nextInt(4);
            for (int i = 0; i < junk; i++) addItemSafe(inv, CHEST_JUNK.pick(random).generate(random));

            int ores = 1 + random.nextInt(4);
            for (int i = 0; i < ores; i++) addItemSafe(inv, CHEST_ORES.pick(random).generate(random));

            if (random.nextDouble() < 0.3) {
                addItemSafe(inv, CHEST_TREASURE.pick(random).generate(random));
            }

            if (random.nextDouble() < 0.15) {
                org.bukkit.inventory.ItemStack artifact = generateRandomArtifact(random);
                if (artifact != null) addItemSafe(inv, artifact);
            }

            shuffleInventory(inv, random);
        }
    }

    private void populateBarrelLoot(Block block, Random random) {
        if (random.nextDouble() > cfg.getBarrelLootChance()) return;
        org.bukkit.block.BlockState state = block.getState();
        if (state instanceof org.bukkit.block.Container) {
            org.bukkit.inventory.Inventory inv = ((org.bukkit.block.Container) state).getInventory();
            inv.clear();

            addItemSafe(inv, BARREL_POOL.pick(random).generate(random));
            shuffleInventory(inv, random);
        }
    }

    private void addItemSafe(org.bukkit.inventory.Inventory inv, org.bukkit.inventory.ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            inv.addItem(item);
        }
    }

    private void shuffleInventory(org.bukkit.inventory.Inventory inv, Random random) {
        org.bukkit.inventory.ItemStack[] contents = inv.getContents();
        for (int i = contents.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            org.bukkit.inventory.ItemStack temp = contents[index];
            contents[index] = contents[i];
            contents[i] = temp;
        }
        inv.setContents(contents);
    }

    private org.bukkit.inventory.ItemStack generateRandomArtifact(Random random) {
        List<org.bg52.artifacts.item.ArtifactItem> available = new ArrayList<>();
        for (org.bg52.artifacts.item.ArtifactItem item : org.bg52.artifacts.item.ArtifactItem.values()) {
            if (plugin.getArtifactConfig().isItemEnabled(item.getId())) {
                available.add(item);
            }
        }
        if (!available.isEmpty()) {
            org.bg52.artifacts.item.ArtifactItem chosen = available.get(random.nextInt(available.size()));
            return plugin.getItemRegistry().createItemStack(chosen);
        }
        return null;
    }

    // ─── Weighted random list ────────────────────────────────────────────────

    static class WeightedList<T> {
        private final List<T> items = new ArrayList<>();
        private final List<Integer> weights = new ArrayList<>();
        private int totalWeight = 0;

        WeightedList<T> add(T item, int weight) {
            items.add(item);
            weights.add(weight);
            totalWeight += weight;
            return this;
        }

        T pick(Random random) {
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            for (int i = 0; i < items.size(); i++) {
                cumulative += weights.get(i);
                if (roll < cumulative)
                    return items.get(i);
            }
            return items.get(items.size() - 1);
        }
    }
}