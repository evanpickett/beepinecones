package com.mstn.pinecones.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mstn.pinecones.component.FlowerData;
import com.mstn.pinecones.component.TreeOriginData;
import com.mstn.pinecones.data.BeeCarryData;
import com.mstn.pinecones.entity.PineconeEntity;
import com.mstn.pinecones.init.ModItems;
import com.mstn.pinecones.init.ModTags;
import com.mstn.pinecones.util.BiomeUtils;
import com.mstn.pinecones.util.TreeUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PineconesCommand {

    // Temporary override value for testing
    public static Double replantChanceOverride = null;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pinecones")
                .requires(source -> source.hasPermission(2))

                // Give pinecone with tree data
                .then(Commands.literal("give")
                        .then(Commands.literal("pinecone")
                                .executes(ctx -> givePinecone(ctx.getSource()))
                                .then(Commands.argument("woodType", StringArgumentType.string())
                                        .executes(ctx -> givePineconeWithType(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "woodType")))))
                        .then(Commands.literal("pollen")
                                .executes(ctx -> givePollen(ctx.getSource()))
                                .then(Commands.argument("flowerType", StringArgumentType.string())
                                        .executes(ctx -> givePollenWithType(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "flowerType"))))))

                // Spawn pinecone entity
                .then(Commands.literal("spawn")
                        .executes(ctx -> spawnPinecone(ctx.getSource())))

                // Check tree death at looked-at block
                .then(Commands.literal("check_tree")
                        .executes(ctx -> checkTree(ctx.getSource())))

                // Kill tree at looked-at block
                .then(Commands.literal("kill_tree")
                        .executes(ctx -> killTree(ctx.getSource())))

                // Override replant chance for testing
                .then(Commands.literal("set_chance")
                        .then(Commands.literal("replant")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(ctx -> setReplantChance(ctx.getSource(),
                                                DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetChances(ctx.getSource()))))

                // Make nearest bee carry item
                .then(Commands.literal("bee_carry")
                        .then(Commands.literal("pinecone")
                                .executes(ctx -> beeCarryPinecone(ctx.getSource())))
                        .then(Commands.literal("pollen")
                                .executes(ctx -> beeCarryPollen(ctx.getSource())))
                        .then(Commands.literal("clear")
                                .executes(ctx -> beeClearCarry(ctx.getSource()))))

                // Debug info
                .then(Commands.literal("debug")
                        .executes(ctx -> showDebugInfo(ctx.getSource())))
        );
    }

    private static int givePinecone(CommandSourceStack source) {
        return givePineconeWithType(source, "minecraft:oak_log");
    }

    private static int givePineconeWithType(CommandSourceStack source, String woodType) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ResourceLocation biome = BiomeUtils.getBiomeAt(player.level(), player.blockPosition());
        ResourceLocation wood = new ResourceLocation(woodType.contains(":") ? woodType : "minecraft:" + woodType);

        ItemStack pinecone = new ItemStack(ModItems.PINECONE.get());
        TreeOriginData.saveToStack(pinecone, new TreeOriginData(wood, biome));

        player.getInventory().add(pinecone);
        source.sendSuccess(() -> Component.literal("Gave pinecone (wood: " + wood + ", biome: " + biome + ")"), true);
        return 1;
    }

    private static int givePollen(CommandSourceStack source) {
        return givePollenWithType(source, "minecraft:poppy");
    }

    private static int givePollenWithType(CommandSourceStack source, String flowerType) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ResourceLocation flower = new ResourceLocation(flowerType.contains(":") ? flowerType : "minecraft:" + flowerType);

        ItemStack pollen = new ItemStack(ModItems.POLLEN.get());
        FlowerData.saveToStack(pollen, new FlowerData(flower));

        player.getInventory().add(pollen);
        source.sendSuccess(() -> Component.literal("Gave pollen (flower: " + flower + ")"), true);
        return 1;
    }

    private static int spawnPinecone(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        Vec3 pos = player.position().add(0, 1, 0);

        ResourceLocation biome = BiomeUtils.getBiomeAt(level, player.blockPosition());
        ItemStack pineconeItem = new ItemStack(ModItems.PINECONE.get());
        TreeOriginData.saveToStack(pineconeItem,
                new TreeOriginData(new ResourceLocation("minecraft:oak_log"), biome));

        PineconeEntity pinecone = new PineconeEntity(level, pos.x, pos.y, pos.z, pineconeItem);
        level.addFreshEntity(pinecone);

        source.sendSuccess(() -> Component.literal("Spawned pinecone entity at your position"), true);
        return 1;
    }

    private static int checkTree(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        BlockPos targetPos = getTargetBlock(player);
        if (targetPos == null) {
            source.sendFailure(Component.literal("No block in range"));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(targetPos);

        BlockPos logPos = null;
        if (state.is(ModTags.Blocks.TREE_LOGS)) {
            logPos = targetPos;
        } else if (state.is(ModTags.Blocks.TREE_LEAVES)) {
            logPos = TreeUtils.findConnectedLog(level, targetPos);
            if (logPos == null) {
                source.sendSuccess(() -> Component.literal("Leaf at " + targetPos + " - No connected log found (orphan leaf)"), true);
                return 1;
            }
        } else {
            source.sendFailure(Component.literal("Target block (" + state.getBlock() + ") is not a log or leaf"));
            return 0;
        }

        TreeUtils.TreeInfo info = TreeUtils.getTreeInfo(level, logPos);
        if (info == null) {
            source.sendFailure(Component.literal("Could not analyze tree"));
            return 0;
        }

        final BlockPos finalLogPos = logPos;
        source.sendSuccess(() -> Component.literal("=== Tree Info ==="), false);
        source.sendSuccess(() -> Component.literal("Position: " + finalLogPos), false);
        source.sendSuccess(() -> Component.literal("Type: " + info.logType()), false);
        source.sendSuccess(() -> Component.literal("Logs: " + info.logCount() + ", Leaves: " + info.leafCount()), false);
        source.sendSuccess(() -> Component.literal("Will decay: " + info.shouldDie() + " (needs 1 leaf remaining)"), false);

        return 1;
    }

    private static int killTree(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        BlockPos targetPos = getTargetBlock(player);
        if (targetPos == null) {
            source.sendFailure(Component.literal("No block in range"));
            return 0;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(targetPos);

        BlockPos logPos = null;
        if (state.is(ModTags.Blocks.TREE_LOGS)) {
            logPos = targetPos;
        } else if (state.is(ModTags.Blocks.TREE_LEAVES)) {
            logPos = TreeUtils.findConnectedLog(level, targetPos);
        }

        if (logPos != null) {
            final BlockPos finalLogPos = logPos;
            TreeUtils.destroyTree(level, logPos);
            source.sendSuccess(() -> Component.literal("Destroyed tree at " + finalLogPos), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("No tree found at target"));
            return 0;
        }
    }

    private static int setReplantChance(CommandSourceStack source, double value) {
        replantChanceOverride = value;
        source.sendSuccess(() -> Component.literal("Set pinecone replant chance to " + (value * 100) + "% (override active)"), true);
        return 1;
    }

    private static int resetChances(CommandSourceStack source) {
        replantChanceOverride = null;
        source.sendSuccess(() -> Component.literal("Reset replant chance override to config value"), true);
        return 1;
    }

    private static int beeCarryPinecone(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        Bee bee = findNearestBee(player);
        if (bee == null) {
            source.sendFailure(Component.literal("No bee found within 16 blocks"));
            return 0;
        }

        ResourceLocation biome = BiomeUtils.getBiomeAt(player.level(), player.blockPosition());
        ItemStack pinecone = new ItemStack(ModItems.PINECONE.get());
        TreeOriginData.saveToStack(pinecone,
                new TreeOriginData(new ResourceLocation("minecraft:oak_log"), biome));

        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        BeeCarryData.saveToEntity(bee, data.withCarriedItem(pinecone, bee.blockPosition()));

        source.sendSuccess(() -> Component.literal("Made nearest bee carry a pinecone"), true);
        return 1;
    }

    private static int beeCarryPollen(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        Bee bee = findNearestBee(player);
        if (bee == null) {
            source.sendFailure(Component.literal("No bee found within 16 blocks"));
            return 0;
        }

        ItemStack pollen = new ItemStack(ModItems.POLLEN.get());
        FlowerData.saveToStack(pollen, new FlowerData(new ResourceLocation("minecraft:poppy")));

        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        BeeCarryData.saveToEntity(bee, data.withCarriedItem(pollen, bee.blockPosition()));

        source.sendSuccess(() -> Component.literal("Made nearest bee carry pollen"), true);
        return 1;
    }

    private static int beeClearCarry(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player"));
            return 0;
        }

        Bee bee = findNearestBee(player);
        if (bee == null) {
            source.sendFailure(Component.literal("No bee found within 16 blocks"));
            return 0;
        }

        BeeCarryData data = BeeCarryData.loadFromEntity(bee);
        BeeCarryData.saveToEntity(bee, data.clearCarriedItem());

        source.sendSuccess(() -> Component.literal("Cleared bee's carried item"), true);
        return 1;
    }

    private static int showDebugInfo(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Pinecones Debug Info ==="), false);
        source.sendSuccess(() -> Component.literal("Replant chance override: " +
                (replantChanceOverride != null ? (replantChanceOverride * 100) + "%" : "none (using config)")), false);

        if (source.getEntity() instanceof ServerPlayer player) {
            Bee bee = findNearestBee(player);
            if (bee != null) {
                BeeCarryData data = BeeCarryData.loadFromEntity(bee);
                source.sendSuccess(() -> Component.literal("Nearest bee carrying: " +
                        (data.isCarryingItem() ? data.carriedItem().getHoverName().getString() : "nothing")), false);
                source.sendSuccess(() -> Component.literal("Bee just left nest: " + data.justLeftNest()), false);
            }
        }

        return 1;
    }

    private static BlockPos getTargetBlock(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(32));

        BlockHitResult result = player.level().clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getBlockPos();
        }
        return null;
    }

    private static Bee findNearestBee(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(16);
        List<Bee> bees = player.level().getEntitiesOfClass(Bee.class, searchBox);

        if (bees.isEmpty()) return null;

        Bee nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Bee bee : bees) {
            double dist = player.distanceToSqr(bee);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = bee;
            }
        }
        return nearest;
    }
}
