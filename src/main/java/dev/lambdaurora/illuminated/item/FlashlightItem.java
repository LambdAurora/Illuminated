/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.item;

import dev.lambdaurora.illuminated.Illuminated;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FlashlightItem extends Item {
	public FlashlightItem(Properties properties) {
		super(properties);
	}

	@Override
	public @NotNull InteractionResult use(Level level, Player player, InteractionHand hand) {
		var stack = player.getItemInHand(hand);

		stack.set(Illuminated.ON, !stack.getOrDefault(Illuminated.ON, false));
		player.playSound(Illuminated.FLASHLIGHT_TOGGLE_SOUND, 1.f, 1.f);
		return InteractionResult.CONSUME;
	}
}
