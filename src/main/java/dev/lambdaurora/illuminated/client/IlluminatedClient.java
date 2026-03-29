/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.client;

import dev.lambdaurora.illuminated.Illuminated;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.yumi.mc.core.api.ModContainer;
import dev.yumi.mc.core.api.entrypoint.client.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class IlluminatedClient implements ClientModInitializer, DynamicLightsInitializer {
	public static final IlluminatedClient INSTANCE = new IlluminatedClient();

	@Override
	public void onInitializeClient(ModContainer mod) {
		ConditionalItemModelProperties.ID_MAPPER.put(Illuminated.id("on"), FlashlightOnConditionalItemModelProperty.MAP_CODEC);
	}

	@Override
	public void onInitializeDynamicLights(DynamicLightsContext context) {
		ClientTickEvents.START_LEVEL_TICK.register(level -> {
			for (var entity : level.entitiesForRendering()) {
				if (entity instanceof LivingEntity living) {
					var holder = (FlashlightHolder) living;

					if (Illuminated.isHoldingPoweredFlashlight(living)) {
						// Flashlight!
						if (holder.getFlashlightLightSource() == null) {
							holder.setFlashlightBehavior(new FlashlightLightBehavior(living));
							context.dynamicLightBehaviorManager().add(holder.getFlashlightLightSource());
						}
					} else {
						// Ahw...
						if (holder.getFlashlightLightSource() != null) {
							context.dynamicLightBehaviorManager().remove(holder.getFlashlightLightSource());
							holder.setFlashlightBehavior(null);
						}
					}
				}
			}
		});
	}
}
