/*
 * Copyright © 2025 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.lambdaurora.illuminated.Illuminated;
import dev.lambdaurora.illuminated.client.IlluminatedClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelIdentifier;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	@WrapOperation(
			method = "getModel",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/ItemModelShaper;getItemModel(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/BakedModel;"
			)
	)
	private BakedModel illuminated$getFlashlightInHandModel(
			ItemModelShaper instance, ItemStack stack, Operation<BakedModel> original
	) {
		if (stack.is(Illuminated.FLASHLIGHT)) {
			return instance.getModelManager().getModel(IlluminatedClient.FLASHLIGHT_IN_HAND_MODEL);
		}

		return original.call(instance, stack);
	}

	@WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z",
					ordinal = 1
			)
	)
	private boolean illuminated$getFlashlightGuiModel$isCheck(ItemStack instance, Item item, Operation<Boolean> original) {
		return instance.is(Illuminated.FLASHLIGHT) || original.call(instance, item);
	}

	@WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/resources/model/ModelManager;getModel(Lnet/minecraft/client/resources/model/ModelIdentifier;)Lnet/minecraft/client/resources/model/BakedModel;",
					ordinal = 1
			)
	)
	private BakedModel illuminated$getFlashlightGuiModel$getModel(
			ModelManager instance, ModelIdentifier modelId, Operation<BakedModel> original,
			@Local(argsOnly = true) ItemStack stack
	) {
		if (stack.is(Illuminated.FLASHLIGHT)) {
			var bakedModel = original.call(instance, IlluminatedClient.FLASHLIGHT_MODEL);
			ClientLevel clientLevel = Minecraft.getInstance().level;
			return bakedModel.getOverrides().resolve(bakedModel, stack, clientLevel, null, 0);
		}

		return original.call(instance, modelId);
	}
}
