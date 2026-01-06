package com.mstn.pinecones.util;

import com.mstn.pinecones.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TreeUtils {
    private static final int MAX_TREE_SEARCH_BLOCKS = 512;
    private static final int MAX_LOG_SEARCH_DISTANCE = 6;

    /**
     * Finds a connected log block below or near the given leaf position.
     */
    @Nullable
    public static BlockPos findConnectedLog(Level level, BlockPos leafPos) {
        BlockState leafState = level.getBlockState(leafPos);
        if (!leafState.is(ModTags.Blocks.TREE_LEAVES)) {
            return null;
        }

        Queue<BlockPos> toCheck = new ArrayDeque<>();
        Set<BlockPos> checked = new HashSet<>();
        toCheck.add(leafPos);

        while (!toCheck.isEmpty() && checked.size() < MAX_TREE_SEARCH_BLOCKS) {
            BlockPos current = toCheck.poll();
            if (checked.contains(current)) continue;
            checked.add(current);

            if (current.distManhattan(leafPos) > MAX_LOG_SEARCH_DISTANCE) continue;

            BlockState state = level.getBlockState(current);

            if (state.is(ModTags.Blocks.TREE_LOGS)) {
                return current;
            }

            if (state.is(ModTags.Blocks.TREE_LEAVES)) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (!checked.contains(neighbor)) {
                        toCheck.add(neighbor);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if the only remaining leaf for a tree is directly above its topmost log.
     */
    public static boolean isOnlyLeafAboveLog(Level level, BlockPos logPos) {
        BlockState logState = level.getBlockState(logPos);
        if (!logState.is(ModTags.Blocks.TREE_LOGS)) {
            return false;
        }

        Block logBlock = logState.getBlock();

        BlockPos topLog = logPos;
        while (level.getBlockState(topLog.above()).is(logBlock)) {
            topLog = topLog.above();
        }

        BlockPos aboveLog = topLog.above();
        BlockState aboveState = level.getBlockState(aboveLog);
        if (!aboveState.is(ModTags.Blocks.TREE_LEAVES)) {
            return false;
        }

        if (aboveState.hasProperty(LeavesBlock.PERSISTENT) && aboveState.getValue(LeavesBlock.PERSISTENT)) {
            return false;
        }

        int leafCount = countConnectedLeaves(level, topLog, logBlock);

        return leafCount == 1;
    }

    private static int countConnectedLeaves(Level level, BlockPos startLogPos, Block logBlock) {
        Set<BlockPos> logs = findAllConnectedLogs(level, startLogPos, logBlock);
        return countConnectedLeaves(level, logs);
    }

    private static int countConnectedLeaves(Level level, Set<BlockPos> logs) {
        Set<BlockPos> countedLeaves = new HashSet<>();

        for (BlockPos logPos : logs) {
            for (Direction dir : Direction.values()) {
                countLeavesFrom(level, logPos.relative(dir), countedLeaves, logs);
            }
        }

        return countedLeaves.size();
    }

    /**
     * Gets debug information about a tree at the given log position.
     */
    public static TreeInfo getTreeInfo(Level level, BlockPos logPos) {
        BlockState logState = level.getBlockState(logPos);
        if (!logState.is(ModTags.Blocks.TREE_LOGS)) {
            return null;
        }

        Block logBlock = logState.getBlock();
        Set<BlockPos> logs = findAllConnectedLogs(level, logPos, logBlock);
        int leafCount = countConnectedLeaves(level, logs);
        boolean shouldDie = isOnlyLeafAboveLog(level, logPos);

        return new TreeInfo(logs.size(), leafCount, shouldDie, logBlock);
    }

    public record TreeInfo(int logCount, int leafCount, boolean shouldDie, Block logType) {}

    private static void countLeavesFrom(Level level, BlockPos start, Set<BlockPos> countedLeaves, Set<BlockPos> logs) {
        if (countedLeaves.contains(start) || logs.contains(start)) return;

        Queue<BlockPos> toCheck = new ArrayDeque<>();
        toCheck.add(start);

        while (!toCheck.isEmpty() && countedLeaves.size() < MAX_TREE_SEARCH_BLOCKS) {
            BlockPos current = toCheck.poll();
            if (countedLeaves.contains(current) || logs.contains(current)) continue;

            BlockState state = level.getBlockState(current);
            if (!state.is(ModTags.Blocks.TREE_LEAVES)) continue;

            if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
                continue;
            }

            countedLeaves.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!countedLeaves.contains(neighbor) && !logs.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }
    }

    /**
     * Finds all connected logs of the same type starting from a given position.
     */
    public static Set<BlockPos> findAllConnectedLogs(Level level, BlockPos startPos, Block logBlock) {
        Set<BlockPos> logs = new HashSet<>();
        Queue<BlockPos> toCheck = new ArrayDeque<>();
        toCheck.add(startPos);

        while (!toCheck.isEmpty() && logs.size() < MAX_TREE_SEARCH_BLOCKS) {
            BlockPos current = toCheck.poll();
            if (logs.contains(current)) continue;

            BlockState state = level.getBlockState(current);
            if (!state.is(logBlock)) continue;

            logs.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!logs.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        return logs;
    }

    /**
     * Destroys an entire tree (all connected logs of the same type).
     */
    public static void destroyTree(Level level, BlockPos logPos) {
        BlockState logState = level.getBlockState(logPos);
        if (!logState.is(ModTags.Blocks.TREE_LOGS)) return;

        Block logBlock = logState.getBlock();
        Set<BlockPos> logs = findAllConnectedLogs(level, logPos, logBlock);

        for (BlockPos pos : logs) {
            level.destroyBlock(pos, true);
        }
    }

    /**
     * Checks if a position is directly above a log block.
     */
    public static boolean isDirectlyAboveLog(Level level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(ModTags.Blocks.TREE_LOGS);
    }

    /**
     * Gets the wood type from a log or leaf block state.
     */
    public static Identifier getWoodType(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    /**
     * Finds the nearest log to a given position within a search radius.
     */
    @Nullable
    public static BlockPos findNearestLog(Level level, BlockPos center, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (level.getBlockState(checkPos).is(ModTags.Blocks.TREE_LOGS)) {
                        double dist = center.distSqr(checkPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }

        return nearest;
    }
}
