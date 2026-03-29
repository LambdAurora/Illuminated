/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface FlashlightHolder {
	@Nullable FlashlightLightBehavior getFlashlightLightSource();

	void setFlashlightBehavior(@Nullable FlashlightLightBehavior flashlightBehavior);
}
