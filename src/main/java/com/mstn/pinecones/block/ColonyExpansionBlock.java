package com.mstn.pinecones.block;

import com.mstn.pinecones.Config;
import com.mstn.pinecones.data.DefenderBeeData;
import com.mstn.pinecones.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Colony expansion block that spawns below bee nests when enough flowers are nearby.
 * Provides defensive bee spawning when hostile mobs are detected.
 */
public class ColonyExpansionBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0, 10.67, 0, 16, 16, 16);

    private static final int DESTRUCTION_DEFENDER_COUNT = 6;

    public ColonyExpansionBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.getBlock() instanceof BeehiveBlock;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.hasProperty(BeehiveBlock.FACING)) {
            Direction nestFacing = aboveState.getValue(BeehiveBlock.FACING);
            level.setBlock(pos, state.setValue(FACING, nestFacing), Block.UPDATE_ALL);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (Config.COLONY_EXPANSION_ENABLED.get()) {
                spawnDefendersOnDestruction(serverLevel, pos, player);
            }
            int honeycombCount = 16 + level.random.nextInt(5);
            ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, honeycombCount);
            ItemEntity itemEntity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, honeycomb);
            level.addFreshEntity(itemEntity);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        super.attack(state, level, pos, player);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (Config.COLONY_EXPANSION_ENABLED.get()) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ColonyExpansionBlockEntity expansion) {
                    expansion.setAggro(player, level.getGameTime());
                    expansion.spawnDefenderBees(serverLevel, pos, player);
                }
            }
        }
    }

    /**
     * Spawns defender bees when the colony expansion is destroyed by a player.
     */
    private void spawnDefendersOnDestruction(ServerLevel level, BlockPos pos, Player player) {
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                DESTRUCTION_DEFENDER_COUNT * 2, 0.3, 0.3, 0.3, 0.0);

        for (int i = 0; i < DESTRUCTION_DEFENDER_COUNT; i++) {
            Bee bee = new Bee(net.minecraft.world.entity.EntityType.BEE, level);

            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5);
            double y = pos.getY() - 0.3;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5);
            bee.setPos(x, y, z);

            bee.setTarget(player);
            bee.setRemainingPersistentAngerTime(Integer.MAX_VALUE);
            bee.setPersistentAngerTarget(player.getUUID());

            // Store defender data in persistent NBT
            DefenderBeeData.saveToEntity(bee, DefenderBeeData.createHomeless());

            level.addFreshEntity(bee);

            level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.NEUTRAL, 1.0F, 1.2F);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColonyExpansionBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return createTickerHelper(blockEntityType, ModBlockEntities.COLONY_EXPANSION.get(),
                    ColonyExpansionBlockEntity::clientTick);
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.COLONY_EXPANSION.get(),
                ColonyExpansionBlockEntity::serverTick);
    }
}
