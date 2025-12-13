package net.manno.simplemobsiege.block.entity;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.entity.ai.SiegeAttackGoal;
import net.manno.simplemobsiege.registry.ModBlockEntities;
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
    // 0-8: Waves (Mob Cards)
    // 9-17: Challenge Cards
    // 18-26: Rewards
    private final ItemStackHandler itemHandler = new ItemStackHandler(27) {
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

    public SiegeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.SIEGE_BLOCK_ENTITY.get(), pos, blockState);
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
            
            // Check if we have waves configured
            boolean hasWaves = false;
            for(int i=0; i<9; i++) {
                if(!itemHandler.getStackInSlot(i).isEmpty()) {
                    hasWaves = true;
                    break;
                }
            }
            if (!hasWaves) {
                if(player != null) player.displayClientMessage(Component.translatable("message.simplemobsiege.no_waves"), true);
                return;
            }

            if (spawnPoints.isEmpty()) {
                if(player != null) player.displayClientMessage(Component.translatable("message.simplemobsiege.no_spawn_points"), true);
                return;
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
            AABB checkArea = new AABB(worldPosition).inflate(32);
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
                // Find next valid wave
                ItemStack card = itemHandler.getStackInSlot(currentWave);
                while(card.isEmpty() && currentWave < 9) {
                     currentWave++;
                     if(currentWave < 9) card = itemHandler.getStackInSlot(currentWave);
                }
                
                if (currentWave >= 9) {
                    winSiege();
                    return;
                }

                if (spawnWave()) {
                    waveSpawned = true;
                    SimpleMobSiege.LOGGER.info("Wave {} spawned successfully", currentWave);
                } else {
                     SimpleMobSiege.LOGGER.warn("Wave {} failed to spawn. Card: {}, SpawnPoints: {}", currentWave, card, spawnPoints.size());
                     // Skip this wave if it fails
                     currentWave++;
                }
            } else {
                if (mobsAlive == 0) {
                    // Wave Cleared
                    currentWave++;
                    SimpleMobSiege.LOGGER.info("Wave cleared. Advancing to wave {}", currentWave);
                    
                    if (currentWave >= 9) {
                        winSiege();
                    } else {
                        waveSpawned = false; // Prepare for next wave
                    }
                }
            }
        }
    }

    private boolean spawnWave() {
        if (currentWave >= 9) return false;
        ItemStack card = itemHandler.getStackInSlot(currentWave);
        if (card.isEmpty() || !card.has(ModDataComponents.MOB_TYPE)) return false;

        String mobTypeStr = card.get(ModDataComponents.MOB_TYPE);
        EntityType<?> type = EntityType.byString(mobTypeStr).orElse(null);
        if (type == null) {
            SimpleMobSiege.LOGGER.error("Invalid entity type: {}", mobTypeStr);
            return false;
        }

        // Spawn at all linked spawn points
        List<BlockPos> points = new ArrayList<>(spawnPoints);
        if (points.isEmpty()) {
             SimpleMobSiege.LOGGER.warn("No spawn points linked!");
             return false;
        }

        // Get count from stack size
        int countPerPoint = card.getCount(); 
        // User said: "Stack quantity corresponds to refresh quantity".
        // But "Linked multiple points... decides this wave's quantity".
        // Let's assume Total Quantity = Stack Size * Points? Or Total Quantity = Stack Size distributed?
        // User: "Stack count corresponds to that kind of mob in one activity refresh quantity".
        // "If multiple cards of same type... accumulate".
        // Since we only check one slot per wave, the stack size is the total count for this wave type.
        // We should distribute them among spawn points.
        
        int totalToSpawn = countPerPoint;
        int spawnedCount = 0;
        
        // Collect Challenge Effects
        List<String> effects = new ArrayList<>();
        for(int i=9; i<18; i++) {
            ItemStack challenge = itemHandler.getStackInSlot(i);
            if(!challenge.isEmpty() && challenge.getItem() == ModItems.CHALLENGE_CARD.get()) {
                // Determine effect. For now, we can use item name or custom data.
                // Assuming simple hardcoded effects for prototype based on item name or similar?
                // Or just random buff for now if no Data Component?
                // We'll just give Speed/Strength for now as a placeholder.
                effects.add("buff");
            }
        }

        boolean spawnedAny = false;
        int pointIndex = 0;
        
        for(int i=0; i<totalToSpawn; i++) {
            BlockPos spawnPos = points.get(pointIndex % points.size());
            pointIndex++;
            
            if (level.isLoaded(spawnPos)) {
                Entity entity = type.spawn((ServerLevel) level, spawnPos.above(), MobSpawnType.EVENT);
                if (entity instanceof Mob mob) {
                    mob.addTag("simplemobsiege.invader");
                    mob.goalSelector.addGoal(1, new SiegeAttackGoal(mob, worldPosition));
                    
                    // Apply Challenge Effects
                    for(String effect : effects) {
                        // Example effects
                        if(effect.equals("buff")) {
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9999, 1));
                            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9999, 0));
                        }
                    }
                    
                    spawnedAny = true;
                    spawnedCount++;
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
        state = State.VICTORY;
        SimpleMobSiege.LOGGER.info("Siege won!");
        bossEvent.setName(Component.translatable("event.simplemobsiege.siege.victory"));
        bossEvent.setColor(BossEvent.BossBarColor.GREEN);
        
        // Distribute rewards
        for (int i = 0; i < 9; i++) {
            ItemStack reward = itemHandler.getStackInSlot(18 + i);
            if (!reward.isEmpty()) {
                Block.popResource(level, worldPosition.above(), reward.copy());
                itemHandler.setStackInSlot(18 + i, ItemStack.EMPTY);
            }
        }
        setChanged();
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
            long[] points = tag.getLongArray("SpawnPoints");
            for (long p : points) {
                spawnPoints.add(BlockPos.of(p));
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
