/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>, Ambre Bertucci <ambre@akarys.me>
 *
 * This file is part of Illuminated.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.illuminated.client;

import com.mojang.math.Axis;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.joml.Matrix3d;
import org.joml.Vector3d;

import java.util.ArrayList;

// To have an easier time with computation, we use two euclidean spaces:
// - World space: the usual Minecraft world space
// - Entity space: space around the entity, where the eye direction is along the -y axis, and the light cone is centered around (0, 0)
// We are able to convert between the two with a simple translation and rotation matrix (no scaling is required).

@Environment(EnvType.CLIENT)
public class FlashlightLightBehavior implements DynamicLightBehavior {
	private final Entity entity;
	private final float RADIUS = 8F;
	private final float DEPTH = 20F;
	private final float DISTANCE_DELTA = 3F;

	private double prevX;
	private double prevY;
	private double prevZ;
	private float prevYaw;
	private float prevPitch;
	private Matrix3d rotationMatrix;
	private Matrix3d inverseRotationMatrix;

	public FlashlightLightBehavior(Entity entity) {
		this.entity = entity;
		this.computeMatrices();
	}

	@Override
	public @Range(from = 0L, to = 15L) double lightAtPos(BlockPos pos, double falloffRatio) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		Vector3d coord = this.worldToEntitySpace(new Vector3d(x, y, z));

		// Signed distance field function for a vertical cone centered at (0, 0)
		double sdf = Math.min(RADIUS * (0.5F - coord.y() / DEPTH) - Math.sqrt(coord.x() * coord.x() + coord.z() * coord.z()), DEPTH * 0.5F - Math.abs(coord.y()));

		double distance = DEPTH / 2.F - coord.y() - DISTANCE_DELTA;
		double intensity = DEPTH / Math.pow(distance, 1.5);
		double light = intensity * 15.F;

		return Math.clamp(MathHelper.smoothstep(sdf), 0.F, 1.F) * light;
	}

	@Override
	public @NotNull BoundingBox getBoundingBox() {
		// To calculate the bounding box, we create a cuboid in entity space encapsulating the entire source, then transform it to world space.
		// We then calculate the larger xyz aligned cuboid encapsulating the first one by taking the minimum and maximum of each x and y coordinate.
		var horizontalValues = new double[]{-RADIUS, RADIUS};
		var yValues = new double[]{-Math.ceil(DEPTH / 2), Math.floor(DEPTH / 2)};
		var vectors = new ArrayList<Vector3d>();

		for (double x : horizontalValues) {
			for (double y : yValues) {
				for (double z : horizontalValues) {
					vectors.add(this.entityToWorldSpace(new Vector3d(x, y, z)));
				}
			}
		}

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;

		for (var vector : vectors) {
			if (vector.x() < minX) {
				minX = vector.x();
			}
			if (vector.y() < minY) {
				minY = vector.y();
			}
			if (vector.z() < minZ) {
				minZ = vector.z();
			}
			if (vector.x() > maxX) {
				maxX = vector.x();
			}
			if (vector.y() > maxY) {
				maxY = vector.y();
			}
			if (vector.z() > maxZ) {
				maxZ = vector.z();
			}
		}

		return new BoundingBox(
				MathHelper.floor(minX),
				MathHelper.floor(minY),
				MathHelper.floor(minZ),
				MathHelper.ceil(maxX),
				MathHelper.ceil(maxY),
				MathHelper.ceil(maxZ)
		);
	}

	@Override
	public boolean hasChanged() {
		if (
				Math.abs(entity.getX() - this.prevX) >= 0.1
						|| Math.abs(entity.getY() - this.prevY) >= 0.1
						|| Math.abs(entity.getZ() - this.prevZ) >= 0.1
						|| Math.abs(entity.getXRot() - this.prevYaw) >= 0.1
						|| Math.abs(entity.getYRot() - this.prevPitch) >= 0.1
		) {
			this.prevX = entity.getX();
			this.prevY = entity.getY();
			this.prevZ = entity.getZ();
			this.prevYaw = entity.getXRot();
			this.prevPitch = entity.getYRot();

			this.computeMatrices();

			return true;
		}

		return false;
	}

	private void computeMatrices() {
		var matrix = new Matrix3d();
		matrix.rotate(Axis.ZP.rotationDegrees(entity.getXRot()));  // rotat
		matrix.rotate(Axis.ZN.rotation(MathHelper.HALF_PI));       // rotat but again
		matrix.rotate(Axis.YP.rotationDegrees(entity.getYRot()));  // rotat a third time
		matrix.rotate(Axis.YP.rotation(MathHelper.HALF_PI));       // THE ULTIMATE ROTAT
		this.rotationMatrix = matrix;
		this.inverseRotationMatrix = matrix.invert(new Matrix3d());
	}

	// ! mutates !
	private Vector3d worldToEntitySpace(Vector3d in) {
		in.sub(entity.getX(), entity.getEyeY(), entity.getZ());
		in.mul(this.rotationMatrix);
		in.y += DEPTH / 2 - DISTANCE_DELTA;

		return in;
	}

	// ! mutates !
	private Vector3d entityToWorldSpace(Vector3d in) {
		in.y -= DEPTH / 2 - DISTANCE_DELTA;
		in.mul(this.inverseRotationMatrix);
		in.add(entity.getX(), entity.getEyeY(), entity.getZ());

		return in;
	}
}
