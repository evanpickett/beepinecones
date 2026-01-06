package com.mstn.pinecones;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue REPLACE_SAPLING_DROPS = BUILDER
            .comment("If true, sapling drops from leaves are replaced with pinecones. If false, pinecones drop alongside saplings.")
            .define("replaceSaplingDrops", true);

    public static final ForgeConfigSpec.DoubleValue PINECONE_REPLANT_CHANCE = BUILDER
            .comment("Chance per second for a dropped pinecone to replant itself before despawning (0.0 - 1.0)")
            .defineInRange("pineconeReplantChance", 0.25, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue PINECONE_TREE_SPACING = BUILDER
            .comment("Minimum distance (in blocks) from existing trees or saplings for a pinecone to plant itself")
            .defineInRange("pineconeTreeSpacing", 4, 0, 16);

    public static final ForgeConfigSpec.IntValue PLANT_DELAY_TICKS = BUILDER
            .comment("Minimum ticks a pinecone/pollen must be on the ground before it can plant (1200 = 1 minute)")
            .defineInRange("plantDelayTicks", 1200, 0, 12000);

    public static final ForgeConfigSpec.IntValue BEE_NEST_RADIUS = BUILDER
            .comment("Maximum radius from bee nest where bees will perform pinecone/pollen behaviors")
            .defineInRange("beeNestRadius", 32, 1, 128);

    public static final ForgeConfigSpec.IntValue BEE_PINECONE_DROP_TIMEOUT_TICKS = BUILDER
            .comment("Ticks before a bee drops a pinecone if it can't find a suitable location")
            .defineInRange("beePineconeDropTimeout", 200, 20, 1200);

    public static final ForgeConfigSpec.IntValue BEE_MIN_PINECONE_DROP_DISTANCE = BUILDER
            .comment("Minimum distance (in blocks) a bee must drop a pinecone from the pickup location and nearest log")
            .defineInRange("beePineconeDropDistance", 4, 1, 16);

    public static final ForgeConfigSpec.IntValue BEE_PINECONE_SEARCH_RADIUS = BUILDER
            .comment("Radius in which bees search for pinecones on the ground")
            .defineInRange("beePineconeSearchRadius", 10, 1, 32);

    public static final ForgeConfigSpec.IntValue BEE_POLLEN_SEARCH_RADIUS = BUILDER
            .comment("Radius in which bees search for crops or plantable blocks when carrying pollen")
            .defineInRange("beePollenSearchRadius", 10, 1, 32);

    public static final ForgeConfigSpec.BooleanValue COLONY_EXPANSION_ENABLED = BUILDER
            .comment("Enable colony expansion blocks that spawn below bee nests when enough flowers are nearby")
            .define("colonyExpansionEnabled", true);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_FLOWER_COUNT = BUILDER
            .comment("Number of flowers required near a bee nest for a colony expansion to form")
            .defineInRange("colonyExpansionFlowerCount", 10, 1, 50);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_FLOWER_RADIUS = BUILDER
            .comment("Radius to search for flowers around the bee nest")
            .defineInRange("colonyExpansionFlowerRadius", 8, 1, 32);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_HOSTILE_CHECK_RADIUS = BUILDER
            .comment("Radius around colony expansion to check for hostile mobs")
            .defineInRange("colonyExpansionHostileCheckRadius", 16, 1, 64);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_SPAWN_COUNT = BUILDER
            .comment("Number of defender bees to spawn when hostiles are detected or a bee is hurt")
            .defineInRange("colonyExpansionSpawnCount", 3, 1, 10);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_MAX_SPAWNED_BEES = BUILDER
            .comment("Maximum number of defender bees that can be spawned from one expansion at a time")
            .defineInRange("colonyExpansionMaxSpawnedBees", 20, 1, 50);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_HOSTILE_CHECK_INTERVAL = BUILDER
            .comment("Ticks between hostile mob checks (20 = 1 second)")
            .defineInRange("colonyExpansionHostileCheckInterval", 40, 10, 200);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_AGGRO_DURATION = BUILDER
            .comment("Ticks that a colony expansion stays aggro'd after being attacked (1200 = 1 minute)")
            .defineInRange("colonyExpansionAggroDuration", 1200, 200, 6000);

    public static final ForgeConfigSpec.IntValue COLONY_EXPANSION_AGGRO_RADIUS = BUILDER
            .comment("Radius around colony expansion to check for aggro'd player")
            .defineInRange("colonyExpansionAggroRadius", 16, 4, 32);

    public static final ForgeConfigSpec.IntValue BEE_MAX_POPULATION = BUILDER
            .comment("Maximum total number of regular bees allowed in an area (hard limit regardless of nest count)")
            .defineInRange("beeMaxPopulation", 9, 1, 50);

    public static final ForgeConfigSpec.IntValue BEES_PER_NEST = BUILDER
            .comment("Number of bees allowed per nest for love mode calculations")
            .defineInRange("beesPerNest", 3, 1, 10);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
