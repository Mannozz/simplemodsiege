package net.manno.simplemobsiege.item;

import net.manno.simplemobsiege.world.inventory.MobCardItemGroupMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class MobCardItemGroupItem extends Item {
    public MobCardItemGroupItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new MobCardItemGroupMenu(id, inv, stack),
                    Component.translatable("item.simplemobsiege.mob_card_group")
            ), buffer -> {
                buffer.writeBoolean(hand == InteractionHand.MAIN_HAND);
            });
        }
        return InteractionResultHolder.success(stack);
    }
}

