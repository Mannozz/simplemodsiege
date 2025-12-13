package net.manno.simplemobsiege.item;

import net.manno.simplemobsiege.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MobCardItem extends Item {
    public MobCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.has(ModDataComponents.MOB_TYPE)) {
            String mobId = stack.get(ModDataComponents.MOB_TYPE);
            try {
                // Try to get translation of the entity type
                EntityType<?> type = EntityType.byString(mobId).orElse(null);
                if (type != null) {
                    return Component.translatable(this.getDescriptionId(stack)).append(" (").append(type.getDescription()).append(")");
                }
            } catch (Exception ignored) {}
            return Component.translatable(this.getDescriptionId(stack)).append(" (").append(Component.literal(mobId)).append(")");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (stack.has(ModDataComponents.MOB_TYPE)) {
            tooltipComponents.add(Component.translatable("tooltip.simplemobsiege.mob_card", stack.get(ModDataComponents.MOB_TYPE)));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.simplemobsiege.mob_card.empty"));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
