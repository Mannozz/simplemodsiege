package net.manno.simplemobsiege.event;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.manno.simplemobsiege.block.entity.SiegeBlockEntity;
import net.manno.simplemobsiege.registry.ModDataComponents;
import net.manno.simplemobsiege.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = SimpleMobSiege.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SiegeBlockEntity.clearGroups();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            if (isBlankCard(mainHand)) {
                convertCard(player, mainHand, event.getEntity().getType(), true);
            } else if (isBlankCard(offHand)) {
                convertCard(player, offHand, event.getEntity().getType(), false);
            }
        }
    }

    private static boolean isBlankCard(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.BLANK_CARD.get();
    }

    private static void convertCard(Player player, ItemStack stack, EntityType<?> type, boolean isMainHand) {
        ItemStack mobCard = new ItemStack(ModItems.MOB_CARD.get());
        String typeId = EntityType.getKey(type).toString();
        mobCard.set(ModDataComponents.MOB_TYPE, typeId);

        stack.shrink(1);
        if (stack.isEmpty()) {
            if (isMainHand) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mobCard);
            } else {
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, mobCard);
            }
        } else {
            if (!player.getInventory().add(mobCard)) {
                player.drop(mobCard, false);
            }
        }
    }
}

