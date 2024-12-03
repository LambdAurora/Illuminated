/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated;

import com.mojang.serialization.Codec;
import dev.lambdaurora.illuminated.item.FlashlightItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Illuminated implements ModInitializer {
	public static final String NAMESPACE = "illuminated";

	public static final DataComponentType<Boolean> ON = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("on"),
			DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build()
	);

	public static final SoundEvent FLASHLIGHT_TOGGLE_SOUND = SoundEvent.createVariableRangeEvent(id("item.flashlight.toggle"));

	public static final Item FLASHLIGHT = Items.registerItem(
			ResourceKey.of(Registries.ITEM, id("flashlight")),
			FlashlightItem::new,
			new Item.Properties().component(ON, false)
	);

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.addAfter(Items.SPYGLASS, FLASHLIGHT);
		});
	}

	public static Identifier id(String path) {
		return Identifier.of(NAMESPACE, path);
	}

	public static boolean isHoldingPoweredFlashlight(Entity entity) {
		if (!(entity instanceof LivingEntity living)) return false;

		ItemStack item = living.getMainHandItem();

		if (!item.is(FLASHLIGHT)) {
			item = living.getOffhandItem();

			if (!item.is(FLASHLIGHT)) return false;
		}

		return item.getOrDefault(ON, false);
	}
}
