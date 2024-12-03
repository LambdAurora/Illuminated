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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.world.entity.LivingEntity;

@Environment(EnvType.CLIENT)
public class IlluminatedClient implements ClientModInitializer, DynamicLightsInitializer {
	public static final IlluminatedClient INSTANCE = new IlluminatedClient();
	private DynamicLightsContext context;

	@Override
	public void onInitializeClient() {
		ConditionalItemModelProperties.ID_MAPPER.put(Illuminated.id("on"), FlashlightOnConditionalItemModelProperty.MAP_CODEC);

		ClientTickEvents.START_WORLD_TICK.register(level -> {
			for (var entity : level.entitiesForRendering()) {
				if (entity instanceof LivingEntity living) {
					var holder = (FlashlightHolder) living;

					if (Illuminated.isHoldingPoweredFlashlight(living)) {
						// Flashlight!
						if (holder.getFlashlightLightSource() == null) {
							holder.setFlashlightBehavior(new FlashlightLightBehavior(living));
							this.context.dynamicLightBehaviorManager().add(holder.getFlashlightLightSource());
						}
					} else {
						// Ahw...
						if (holder.getFlashlightLightSource() != null) {
							this.context.dynamicLightBehaviorManager().remove(holder.getFlashlightLightSource());
							holder.setFlashlightBehavior(null);
						}
					}
				}
			}
		});
	}

	@Override
	public void onInitializeDynamicLights(DynamicLightsContext context) {
		this.context = context;
	}
}
