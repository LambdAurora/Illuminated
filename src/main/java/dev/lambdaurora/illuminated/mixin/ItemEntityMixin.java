/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.mixin;

import dev.lambdaurora.illuminated.Illuminated;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
	@Inject(method = "setItem", at = @At("HEAD"))
	private void illuminated$onSetItem(ItemStack stack, CallbackInfo ci) {
		if (stack.is(Illuminated.FLASHLIGHT)) {
			var value = stack.get(Illuminated.ON);

			if (value != null && value) {
				stack.set(Illuminated.ON, false);
			}
		}
	}
}
