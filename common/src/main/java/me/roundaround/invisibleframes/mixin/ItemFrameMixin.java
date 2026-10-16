package me.roundaround.invisibleframes.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
  @Inject(
      method = "interact",
      at = @At(value = "INVOKE", target =
          "Lnet/minecraft/world/entity/decoration/ItemFrame;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"),
      cancellable = true
  )
  private void invisibleframes$interact(
      Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir
  ) {
    // This call is reached only on the server, for a non-fixed frame holding an item.
    if (player.isShiftKeyDown()) {
      ItemFrame frame = (ItemFrame) (Object) this;
      frame.setInvisible(!frame.isInvisible());
      frame.gameEvent(GameEvent.BLOCK_CHANGE, player);
      // Consume the interaction just like vanilla, preventing an offhand retry.
      cir.setReturnValue(InteractionResult.CONSUME);
    }
  }

  @Inject(
      method = "onSyncedDataUpdated",
      at = @At(value = "INVOKE", target =
          "Lnet/minecraft/world/entity/decoration/ItemFrame;onItemChanged(Lnet/minecraft/world/item/ItemStack;)V")
  )
  private void invisibleframes$onItemChange(EntityDataAccessor<?> accessor, CallbackInfo ci) {
    ItemFrame frame = (ItemFrame) (Object) this;
    if (frame.getItem().isEmpty()) {
      frame.setInvisible(false);
    }
  }
}
