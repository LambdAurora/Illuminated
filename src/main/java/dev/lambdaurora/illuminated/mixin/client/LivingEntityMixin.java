/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.mixin.client;

import dev.lambdaurora.illuminated.client.FlashlightHolder;
import dev.lambdaurora.illuminated.client.FlashlightLightBehavior;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements FlashlightHolder {
	@Unique
	private FlashlightLightBehavior lightSource;

	@Override
	public FlashlightLightBehavior getFlashlightLightSource() {
		return this.lightSource;
	}

	@Override
	public void setFlashlightBehavior(FlashlightLightBehavior lightSource) {
		this.lightSource = lightSource;
	}
}
