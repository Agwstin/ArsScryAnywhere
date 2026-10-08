package io.github.agwstin.arsscryanywhere.mixin;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.items.ScryerScroll;
import com.hollingsworth.arsnouveau.common.items.data.ScryCasterData;
import com.hollingsworth.arsnouveau.common.items.data.ScryPosData;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// The Enchanter's Eye stores the Scry Crystal as a GlobalPos (position + dimension),
/// but Ars Nouveau looks the position up in the caster's current dimension.
/// If the bound crystal lives in another dimension, re-run the cast in that dimension's level.
@Mixin(ScryCasterData.class)
public abstract class ScryCasterDataMixin {
    @Inject(
            method = "castSpell(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/network/chat/Component;Lcom/hollingsworth/arsnouveau/api/spell/Spell;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void arsscryanywhere$castInCrystalDimension(Level worldIn, LivingEntity entity, InteractionHand handIn, @Nullable Component invalidMessage, Spell spell,
                                                       CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(worldIn instanceof ServerLevel serverLevel)) return;
        GlobalPos target = arsscryanywhere$getBoundPos(entity, handIn);
        if (target == null || target.dimension().equals(worldIn.dimension())) return;
        ServerLevel targetLevel = serverLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;
        // Same dimension as the crystal now, so this call runs Ars Nouveau's original logic.
        cir.setReturnValue(((ScryCasterData) (Object) this).castSpell(targetLevel, entity, handIn, invalidMessage, spell));
    }

    /// Mirrors Ars Nouveau: a Scryer's Scroll in the offhand overrides the position bound to the eye.
    @Nullable
    private static GlobalPos arsscryanywhere$getBoundPos(LivingEntity entity, InteractionHand handIn) {
        ItemStack offhand = entity.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack source = offhand.getItem() instanceof ScryerScroll ? offhand : entity.getItemInHand(handIn);
        ScryPosData data = source.get(DataComponentRegistry.SCRY_DATA);
        return data == null ? null : data.pos().orElse(null);
    }
}
