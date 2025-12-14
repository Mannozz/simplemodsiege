package net.manno.simplemobsiege.block.entity;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.entity.ai.SiegeAttackGoal;
import net.manno.simplemobsiege.registry.ModBlockEntities;
import net.manno.simplemobsiege.registry.ModBlocks;
import net.manno.simplemobsiege.registry.ModDataComponents;
import net.manno.simplemobsiege.registry.ModItems;
import net.manno.simplemobsiege.world.inventory.SiegeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SiegeBlockEntity extends BlockEntity implements MenuProvider {
    public enum State {
        IDLE,
        ACTIVE,
        VICTORY,
        FAILED
    }

    private State state = State.IDLE;
    private final Set<BlockPos> spawnPoints = new HashSet<>();
    // 0-8: Wave (Mob Cards)
    // 9-17: Challenge Cards
    private final ItemStackHandler itemHandler = new ItemStackHandler(18) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("event.simplemobsiege.siege"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    ).setDarkenScreen(false);

    // Game Logic Variables
    private int currentWave = 0;
    private float durability = 100.0f;
    private int tickCounter = 0;
    private int mobsAlive = 0;
    private boolean waveSpawned = false;
    private int victoryPulseTicks = 0;

    public SiegeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SIEGE_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    
    // Add Getter
    public boolean isVictory() {
        return state == State.VICTORY;
    }
    
    public boolean isProvidingSignal() {
        return victoryPulseTicks > 0;
    }

    // Add Getter
    public Set<BlockPos> getSpawnPoints() {
        return spawnPoints;
    }

    public void addSpawnPoint(BlockPos pos) {
        this.spawnPoints.add(pos);
        setChanged();
        SimpleMobSiege.LOGGER.info("Added spawn point at {}", pos);
    }

    public void startSiege(@Nullable Player player) {
        if (state == State.IDLE || state == State.VICTORY || state == State.FAILED) {
            // Reset state
            state = State.IDLE;
            
            // Check if we have wave configured
            boolean hasWaves = false;
            for(int i=0; i<9; i++) {
                if(!itemHandler.getStackInSlot(i).isEmpty()) {
                    hasWaves = true;
                    break;
                }
            }
            if(!hasWaves) {
                if(player != null) player.displayClientMessage(Component.translatable("message.simplemobsiege.no_waves"), true);
                return;
            }

            if (spawnPoints.isEmpty()) {
                // Try to find nearby spawn points and auto-link them if they point to us
                AABB searchArea = new AABB(worldPosition).inflate(64);
                // We can't iterate blocks efficiently with AABB.
                // Iterate a reasonable range instead
                BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
                int range = 32;
                for(int x = -range; x <= range; x++) {
                    for(int y = -10; y <= 10; y++) {
                        for(int z = -range; z <= range; z++) {
                            mPos.set(worldPosition.getX() + x, worldPosition.getY() + y, worldPosition.getZ() + z);
                            if(level.getBlockEntity(mPos) instanceof net.manno.simplemobsiege.block.entity.SpawnPointBlockEntity spawnBe) {
                                if(this.worldPosition.equals(spawnBe.getSiegeBlockPos())) {
                                    addSpawnPoint(mPos.immutable());
                                }
                            }
                        }
                    }
                }
                
                if (spawnPoints.isEmpty()) {
                    if(player != null) player.displayClientMessage(Component.translatable("message.simplemobsiege.no_spawn_points"), true);
                    return;
                }
            }
            
            SimpleMobSiege.LOGGER.info("Starting siege at {}", worldPosition);
            state = State.ACTIVE;
            currentWave = 0;
            durability = 100.0f;
            tickCounter = 0;
            waveSpawned = false;
            mobsAlive = 0;
            setChanged();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SiegeBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick();
    }

    private void serverTick() {
        // Handle Redstone Pulse
        if (victoryPulseTicks > 0) {
            victoryPulseTicks--;
            if (victoryPulseTicks == 0) {
                level.updateNeighborsAt(worldPosition, this.getBlockState().getBlock());
            }
        }

        if (state != State.ACTIVE) {
            bossEvent.setVisible(false);
            bossEvent.removeAllPlayers();
            return;
        }
        
        // Update Boss Bar
        bossEvent.setVisible(true);
        bossEvent.setProgress(durability / 100.0f);
        bossEvent.setName(Component.translatable("event.simplemobsiege.siege.wave", currentWave + 1, mobsAlive));

        if (tickCounter % 20 == 0) {
            // Update players
            AABB range = new AABB(worldPosition).inflate(64);
            List<ServerPlayer> players = ((ServerLevel) level).getEntitiesOfClass(ServerPlayer.class, range);
            for (ServerPlayer player : players) {
                bossEvent.addPlayer(player);
            }
            // Remove far players
             for (ServerPlayer player : new ArrayList<>(bossEvent.getPlayers())) {
                if (!range.contains(player.position())) {
                    bossEvent.removePlayer(player);
                }
            }
        }

        tickCounter++;

        // Passive Durability Drain & Victory Check
        if (tickCounter % 20 == 0) {
            // Check mobs
            // Use configured radius
            AABB checkArea = new AABB(worldPosition).inflate(256); 
            List<Mob> invaders = ((ServerLevel)level).getEntitiesOfClass(Mob.class, checkArea, 
                e -> e.getTags().contains("simplemobsiege.invader"));
            
            mobsAlive = invaders.size();
            
            // Drain logic: For each invader close to core
            for (Mob mob : invaders) {
                if (mob.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 100) { // 10 blocks
                    durability -= 0.5f;
                }
            }

            if (durability <= 0) {
                failSiege();
                return;
            }

            // Wave Logic
            if (!waveSpawned) {
                if (spawnWave()) {
                    waveSpawned = true;
                    SimpleMobSiege.LOGGER.info("Wave spawned successfully");
                } else {
                     // Check if wave is empty (no card?)
                     boolean hasCards = false;
                     for(int i=0; i<9; i++) {
                         if(!itemHandler.getStackInSlot(i).isEmpty()) {
                             hasCards = true;
                             break;
                         }
                     }
                     if (!hasCards) {
                         // Should not happen if started correctly
                         winSiege();
                     } else {
                         SimpleMobSiege.LOGGER.warn("Wave failed to spawn. SpawnPoints: {}", spawnPoints.size());
                         // Fail or retry? For now, do nothing, just wait tick
                     }
                }
            } else {
                if (mobsAlive == 0) {
                    // Only win if we are sure we spawned something and now they are dead.
                    // But wait, what if mobs despawned or teleported away?
                    // We increased check radius to 128.
                    // We could also track spawned UUIDs but that is more complex.
                    // For now, large radius should suffice for "spawn point too far".
                    
                    winSiege();
                }
            }
        }
        
    }

    private boolean spawnWave() {
        if (currentWave >= 18) return false;
        
        // Collect all valid mob cards in wave slots (0-8)
        List<ItemStack> waveCards = new ArrayList<>();
        for(int i=0; i<9; i++) {
            ItemStack s = itemHandler.getStackInSlot(i);
            if(!s.isEmpty() && s.has(ModDataComponents.MOB_TYPE)) {
                waveCards.add(s);
            }
        }
        
        if (waveCards.isEmpty()) return false;

        // Validate and filter linked spawn points
        List<BlockPos> validActivePoints = new ArrayList<>();
        List<BlockPos> toRemove = new ArrayList<>();

        for (BlockPos pos : spawnPoints) {
            if (!level.isLoaded(pos)) continue; // Skip unloaded, don't remove
            
            if (!level.getBlockState(pos).is(ModBlocks.SPAWN_POINT_BLOCK.get())) {
                toRemove.add(pos);
                continue;
            }
            
            if (level.getBlockEntity(pos) instanceof SpawnPointBlockEntity spawnBe) {
                // Auto-fix legacy or missing links
                if (spawnBe.getSiegeBlockPos() == null) {
                    spawnBe.setSiegeBlockPos(this.worldPosition);
                    validActivePoints.add(pos);
                } else if (this.worldPosition.equals(spawnBe.getSiegeBlockPos())) {
                    validActivePoints.add(pos);
                } else {
                    // Linked to someone else
                    toRemove.add(pos);
                }
            } else {
                toRemove.add(pos);
            }
        }
        
        spawnPoints.removeAll(toRemove);
        if (!toRemove.isEmpty()) setChanged();
        
        if (validActivePoints.isEmpty()) {
             SimpleMobSiege.LOGGER.warn("No valid spawn points linked!");
             return false;
        }
        
        // Collect Challenge Effects
        List<String> effects = new ArrayList<>();
        for(int i=9; i<18; i++) {
            ItemStack challenge = itemHandler.getStackInSlot(i);
            if(!challenge.isEmpty() && challenge.getItem() == ModItems.CHALLENGE_CARD.get()) {
                effects.add("buff");
            }
        }

        boolean spawnedAny = false;
        int pointIndex = 0;
        
        for (ItemStack card : waveCards) {
            String mobTypeStr = card.get(ModDataComponents.MOB_TYPE);
            EntityType<?> type = EntityType.byString(mobTypeStr).orElse(null);
            if (type == null) {
                SimpleMobSiege.LOGGER.error("Invalid entity type: {}", mobTypeStr);
                continue;
            }

            int countPerPoint = card.getCount(); 
            int totalToSpawn = countPerPoint;

            for(int i=0; i<totalToSpawn; i++) {
                BlockPos spawnPos = validActivePoints.get(pointIndex % validActivePoints.size());
                pointIndex++;
                
                if (level.isLoaded(spawnPos)) {
                    Entity entity = type.spawn((ServerLevel) level, spawnPos.above(), MobSpawnType.EVENT);
                    if (entity instanceof Mob mob) {
                        mob.addTag("simplemobsiege.invader");
                        mob.goalSelector.addGoal(1, new SiegeAttackGoal(mob, worldPosition));
                        
                        // Apply Challenge Effects
                        for(String effect : effects) {
                            if(effect.equals("buff")) {
                                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9999, 1));
                                mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9999, 0));
                            }
                        }
                        
                        spawnedAny = true;
                    }
                }
            }
        }
        
        return spawnedAny;
    }

    private void failSiege() {
        state = State.FAILED;
        SimpleMobSiege.LOGGER.info("Siege failed!");
        bossEvent.setName(Component.translatable("event.simplemobsiege.siege.failed"));
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        setChanged();
        // Maybe explode or something?
    }

    private void winSiege() {
        if (state == State.VICTORY) return; // Prevent multiple triggers
        state = State.VICTORY;
        SimpleMobSiege.LOGGER.info("Siege won!");
        bossEvent.setName(Component.translatable("event.simplemobsiege.siege.victory"));
        bossEvent.setColor(BossEvent.BossBarColor.GREEN);
        setChanged();
        
        // Output Redstone Pulse (20 ticks = 1 second)
        victoryPulseTicks = 20;
        level.updateNeighborsAt(worldPosition, this.getBlockState().getBlock());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putString("State", state.name());
        
        tag.putLongArray("SpawnPoints", spawnPoints.stream().mapToLong(BlockPos::asLong).toArray());
        
        tag.putInt("CurrentWave", currentWave);
        tag.putFloat("Durability", durability);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        if (tag.contains("State")) {
            try {
                state = State.valueOf(tag.getString("State"));
            } catch (IllegalArgumentException e) {
                state = State.IDLE;
            }
        }
        
        if (tag.contains("SpawnPoints")) {
            spawnPoints.clear();
            Tag t = tag.get("SpawnPoints");
            if (t instanceof net.minecraft.nbt.LongArrayTag) {
                long[] points = ((net.minecraft.nbt.LongArrayTag)t).getAsLongArray();
                for (long p : points) {
                    spawnPoints.add(BlockPos.of(p));
                }
            } else if (t instanceof ListTag list && list.getElementType() == Tag.TAG_LONG) {
                for (Tag item : list) {
                    spawnPoints.add(BlockPos.of(((LongTag) item).getAsLong()));
                }
            }
        }

        currentWave = tag.getInt("CurrentWave");
        durability = tag.getFloat("Durability");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.simplemobsiege.siege_block");
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SiegeMenu(containerId, playerInventory, this, ContainerLevelAccess.create(level, worldPosition));
    }
}
