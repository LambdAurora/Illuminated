/*
 * Copyright © 2025 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.mixin.client;

import dev.lambdaurora.illuminated.client.IlluminatedClient;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelIdentifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
	@Shadow
	protected abstract void loadSpecialItemModelAndDependencies(ModelIdentifier modelId);

	@Inject(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/resources/model/ModelBakery;loadSpecialItemModelAndDependencies(Lnet/minecraft/client/resources/model/ModelIdentifier;)V",
					ordinal = 1
			)
	)
	private void illuminated$injectFlashlightInHandModels(CallbackInfo ci) {
		this.loadSpecialItemModelAndDependencies(IlluminatedClient.FLASHLIGHT_IN_HAND_MODEL);
	}
}
