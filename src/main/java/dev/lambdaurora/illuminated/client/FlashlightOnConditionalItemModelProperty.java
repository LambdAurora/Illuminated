/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.client;

import com.mojang.serialization.MapCodec;
import dev.lambdaurora.illuminated.Illuminated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record FlashlightOnConditionalItemModelProperty() implements ConditionalItemModelProperty {
	public static final MapCodec<FlashlightOnConditionalItemModelProperty> MAP_CODEC = MapCodec.unit(new FlashlightOnConditionalItemModelProperty());

	@Override
	public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int i, ItemDisplayContext context) {
		return stack.getOrDefault(Illuminated.ON, false);
	}

	@Override
	public MapCodec<FlashlightOnConditionalItemModelProperty> type() {
		return MAP_CODEC;
	}
}