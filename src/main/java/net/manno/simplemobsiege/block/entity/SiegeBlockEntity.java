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

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
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
    
    // Shared Siege Logic
    private static final java.util.Map<String, SiegeGroup> SIEGE_GROUPS = new java.util.HashMap<>();

    public static void clearGroups() {
        SIEGE_GROUPS.clear();
    }
    
    private static class SiegeGroup {
        private final String name;
        private final ServerBossEvent bossEvent;
        private final Set<SiegeBlockEntity> members = new HashSet<>();
        private final java.util.Map<SiegeBlockEntity, Set<java.util.UUID>> mobObservations = new java.util.HashMap<>();
        private float durability;
        private int maxWave = 0;

        private final java.util.Map<SiegeBlockEntity, java.util.Collection<ServerPlayer>> nearbyPlayersMap = new java.util.HashMap<>();

        public SiegeGroup(String name, float initialDurability) {
            this.name = name;
            this.durability = initialDurability;
            this.bossEvent = (ServerBossEvent) new ServerBossEvent(
                    Component.translatable("event.simplemobsiege.siege"),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            ).setDarkenScreen(false);
            this.bossEvent.setVisible(true);
        }

        public void addMember(SiegeBlockEntity be) {
            members.add(be);
            // Sync durability: Take the lower (more damaged) value to prevent healing exploits on reload
            if (be.durability < this.durability) {
                this.durability = be.durability;
            }
            be.durability = this.durability;
        }

        public void removeMember(SiegeBlockEntity be) {
            members.remove(be);
            mobObservations.remove(be);
            nearbyPlayersMap.remove(be);
            if (members.isEmpty()) {
                bossEvent.removeAllPlayers();
                bossEvent.setVisible(false);
                SIEGE_GROUPS.remove(name);
            }
        }

        public void update(SiegeBlockEntity be, Set<java.util.UUID> mobs, float damage, int wave, java.util.Collection<ServerPlayer> players) {
            mobObservations.put(be, mobs);
            nearbyPlayersMap.put(be, players);
            
            if (damage > 0) {
                this.durability -= damage;
                if (this.durability < 0) this.durability = 0;
            }
            // Sync back to member
            be.durability = this.durability;

            if (wave > maxWave) maxWave = wave;
            
            updateBossBar();
        }
        
        public int getTotalMobs() {
            Set<java.util.UUID> uniqueMobs = new HashSet<>();
            for (Set<java.util.UUID> mobs : mobObservations.values()) {
                uniqueMobs.addAll(mobs);
            }
            return uniqueMobs.size();
        }
        
        public void setVictory() {
             bossEvent.setName(Component.translatable("event.simplemobsiege.siege.victory"));
             bossEvent.setColor(BossEvent.BossBarColor.GREEN);
        }
        
        public void setFailed() {
             bossEvent.setName(Component.translatable("event.simplemobsiege.siege.failed"));
             bossEvent.setColor(BossEvent.BossBarColor.RED);
        }

        private void updateBossBar() {
            int totalMobs = getTotalMobs();
            bossEvent.setProgress(durability / 100.0f);

            Component waveInfo = Component.translatable("event.simplemobsiege.siege.wave", maxWave + 1, totalMobs);
            
            String displayName = name.startsWith("#") ? "" : name;
            
            if (!displayName.isEmpty()) {
                bossEvent.setName(Component.literal(displayName + " (").append(waveInfo).append(")"));
            } else {
                bossEvent.setName(waveInfo);
            }
            
            // Sync Players
            Set<ServerPlayer> allPlayers = new HashSet<>();
            for (java.util.Collection<ServerPlayer> pList : nearbyPlayersMap.values()) {
                allPlayers.addAll(pList);
            }
            
            // Add missing
            for (ServerPlayer p : allPlayers) {
                bossEvent.addPlayer(p);
            }
            
            // Remove extra
            // Getting existing players returns an immutable view or copy?
            // getPlayers() returns unmodifiable collection usually.
            // We need to copy it to iterate and remove.
            List<ServerPlayer> current = new ArrayList<>(bossEvent.getPlayers());
            for (ServerPlayer p : current) {
                if (!allPlayers.contains(p)) {
                    bossEvent.removePlayer(p);
                }
            }
        }

        public void addPlayer(ServerPlayer player) {
            bossEvent.addPlayer(player);
        }


        public void removePlayer(ServerPlayer player) {
            bossEvent.removePlayer(player);
        }
        
        public java.util.Collection<ServerPlayer> getPlayers() {
            return bossEvent.getPlayers();
        }
    }

    private SiegeGroup siegeGroup;

    // Game Logic Variables
    private int currentWave = 0;
    private float durability = 100.0f;
    private int tickCounter = 0;
    private int mobsAlive = 0;
    private boolean waveSpawned = false;
    private int victoryPulseTicks = 0;
    private String waveName = "";

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
    
    @Override
    public void setRemoved() {
        if (siegeGroup != null) {
            siegeGroup.removeMember(this);
            siegeGroup = null;
        }
        super.setRemoved();
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        // If we loaded in ACTIVE state, we should rejoin group?
        // But we might not have waveName initialized yet if loadAdditional hasn't run?
        // onLoad runs after loadAdditional.
        if (state == State.ACTIVE) {
            joinSiegeGroup();
        }
    }

    private void joinSiegeGroup() {
        String key = (waveName != null && !waveName.isEmpty()) ? waveName : ("#" + worldPosition.asLong());
        siegeGroup = SIEGE_GROUPS.computeIfAbsent(key, k -> new SiegeGroup(k, durability));
        siegeGroup.addMember(this);
    }
    
    private void leaveSiegeGroup() {
        if (siegeGroup != null) {
            siegeGroup.removeMember(this);
            siegeGroup = null;
        }
    }

    // Add Getter
    public boolean isVictory() {
        return state == State.VICTORY;
    }
    
    public boolean isProvidingSignal() {
        return victoryPulseTicks > 0;
    }
    
    public String getWaveName() {
        return waveName;
    }

    public void setWaveName(String name) {
        if (state == State.ACTIVE && !name.equals(this.waveName)) {
            leaveSiegeGroup();
            this.waveName = name;
            joinSiegeGroup();
        } else {
            this.waveName = name;
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
            
            joinSiegeGroup();
            
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
            if (siegeGroup != null) {
                 leaveSiegeGroup();
            }
            return;
        }
        
        if (siegeGroup == null) {
            joinSiegeGroup();
        }
        
        // Player tracking via Group
        if (tickCounter % 20 == 0) {
            // Update players
            AABB range = new AABB(worldPosition).inflate(64);
            List<ServerPlayer> players = ((ServerLevel) level).getEntitiesOfClass(ServerPlayer.class, range);
            for (ServerPlayer player : players) {
                siegeGroup.addPlayer(player);
            }
            // Remove far players
            // We need to be careful not to remove players that are near OTHER members of the group.
            // But SiegeGroup manages one bossbar. 
            // If we remove a player here, they might be re-added by another member?
            // Actually ServerBossEvent handles add/remove idempotently.
            // But if I call removePlayer, it removes them.
            // So each member should only remove players that are NOT near ITSELF?
            // No, if Player is near Block A but far from Block B.
            // Block B says "Remove". Block A says "Add".
            // If they run in same tick... order matters.
            
            // Better logic: Don't remove here. 
            // Only ADD here.
            // Let the Group handle removal?
            // Or: Group checks all members for players to keep?
            // That's expensive to do every tick.
            
            // Alternative:
            // Just add.
            // How to remove?
            // Maybe iterate Group's players and check if they are near ANY member?
            // Only the Group knows all members.
            
            // Let's implement cleanup in Group.update or a separate periodic check.
            // For now, let's just ADD here. 
            // And maybe have a leader do the cleanup?
            
            // Temporary fix: Iterate players in bossEvent, check distance to THIS block.
            // If far, we WANT to remove, but only if they are not near others.
            // Since we can't easily check others efficiently without iterating them...
            
            // Let's rely on a lazy cleanup in SiegeGroup?
            // Or: Each member adds players near it.
            // We clear the player list and rebuild it? 
            // bossEvent.removeAllPlayers() causes packet spam? Maybe.
            
            // Let's check vanilla/standard practices.
            // Usually you check `players` list and sync.
            
            // Let's add a "cleanupPlayers" method to SiegeGroup that runs periodically.
            // And here we just ADD.
        }
        
        // We'll handle player removal in SiegeGroup.update logic if possible or let it slide for now.
        // Or implement a robust check.
        
        // Actually, if we just never remove, players keep the bar forever. Bad.
        // Let's make SiegeGroup.updateBossBar() handle player cleanup periodically.
        // We need access to Level for that. 
        // Members are in different chunks/locations.
        
        // Revised Plan for Players:
        // Each member calculates "Players near me".
        // We need the union of "Players near any member".
        // SiegeGroup can maintain a Set<UUID> of players valid for this tick?
        // Reset valid players set at start of tick? Hard to sync.
        
        // Alternative: 
        // Players have a "time to live" in the boss bar? No.
        
        // Simple heuristic:
        // Every 20 ticks (1 sec), each member updates the group with "Players near me".
        // The group collects these sets.
        // Then updates the BossEvent.
        
        // To do this sync:
        // SiegeGroup maintains Map<SiegeBlockEntity, Set<ServerPlayer>> playerMap.
        // In update(), we update this map.
        // Then flatten values to get all valid players.
        // Sync BossEvent to this set.
        
        // Let's add 'List<ServerPlayer> nearbyPlayers' to update().
        
        tickCounter++;

        // Passive Durability Drain & Victory Check
        if (tickCounter % 20 == 0) {
            // Check mobs
            // Use configured radius
            AABB checkArea = new AABB(worldPosition).inflate(256); 
            List<Mob> invaders = ((ServerLevel)level).getEntitiesOfClass(Mob.class, checkArea, 
                e -> e.getTags().contains("simplemobsiege.invader"));
            
            mobsAlive = invaders.size();
            Set<java.util.UUID> observedIds = new HashSet<>();
            for (Mob m : invaders) observedIds.add(m.getUUID());
            
            float damageTaken = 0;
            // Drain logic: For each invader close to core
            for (Mob mob : invaders) {
                if (mob.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 100) { // 10 blocks
                    damageTaken += 0.5f;
                }
            }

            // Collect players for group sync
            AABB range = new AABB(worldPosition).inflate(64);
            List<ServerPlayer> nearbyPlayers = ((ServerLevel) level).getEntitiesOfClass(ServerPlayer.class, range);

            siegeGroup.update(this, observedIds, damageTaken, currentWave, nearbyPlayers);

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
                 // Check aggregated mobs?
                 // Wait, mobsAlive is local.
                 // We need GROUP total mobs to decide win?
                 // Yes.
                 // Access group.getTotalMobs()?
                 // We can get it from siegeGroup.
                 
                 if (siegeGroup.getTotalMobs() == 0) {
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
            if (s.isEmpty()) continue;

            if (s.getItem() == ModItems.MOB_CARD_GROUP.get()) {
                CustomData customData = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                if (customData.contains("Inventory")) {
                    CompoundTag invTag = customData.copyTag().getCompound("Inventory");
                    ItemStackHandler tempHandler = new ItemStackHandler(27);
                    tempHandler.deserializeNBT(level.registryAccess(), invTag);
                    for(int j=0; j<27; j++) {
                        ItemStack card = tempHandler.getStackInSlot(j);
                        if (!card.isEmpty() && card.has(ModDataComponents.MOB_TYPE)) {
                            waveCards.add(card.copy());
                        }
                    }
                }
            } else if (s.has(ModDataComponents.MOB_TYPE)) {
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
        if (siegeGroup != null) {
            siegeGroup.setFailed();
            // All members fail?
            // If durability shared, all will fail eventually.
        }
        setChanged();
        // Maybe explode or something?
    }

    private void winSiege() {
        if (state == State.VICTORY) return; // Prevent multiple triggers
        state = State.VICTORY;
        SimpleMobSiege.LOGGER.info("Siege won!");
        if (siegeGroup != null) {
            siegeGroup.setVictory();
        }
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
        tag.putString("WaveName", waveName);
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
        if(tag.contains("WaveName")) {
            waveName = tag.getString("WaveName");
        }
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
