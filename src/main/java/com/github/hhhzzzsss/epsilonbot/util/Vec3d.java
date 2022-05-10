package com.github.hhhzzzsss.epsilonbot.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Vec3d {
	public static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);
	@Getter public final double x;
	@Getter public final double y;
	@Getter public final double z;

	public Vec3d reverseSubtract(Vec3d vec) {
		return new Vec3d(vec.x - this.x, vec.y - this.y, vec.z - this.z);
	}

	public Vec3d normalize() {
		double d = (double) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
		return d < 1.0E-4D ? ZERO : new Vec3d(this.x / d, this.y / d, this.z / d);
	}

	public double dotProduct(Vec3d vec) {
		return this.x * vec.x + this.y * vec.y + this.z * vec.z;
	}

	public Vec3d crossProduct(Vec3d vec) {
		return new Vec3d(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z,
				this.x * vec.y - this.y * vec.x);
	}

	public Vec3d subtract(Vec3d vec) {
		return this.subtract(vec.x, vec.y, vec.z);
	}

	public Vec3d subtract(double x, double y, double z) {
		return this.add(-x, -y, -z);
	}

	public Vec3d add(Vec3d vec) {
		return this.add(vec.x, vec.y, vec.z);
	}

	public Vec3d add(double x, double y, double z) {
		return new Vec3d(this.x + x, this.y + y, this.z + z);
	}

	public double distanceTo(Vec3d vec) {
		double d = vec.x - this.x;
		double e = vec.y - this.y;
		double f = vec.z - this.z;
		return (double) Math.sqrt(d * d + e * e + f * f);
	}

	public double squaredDistanceTo(Vec3d vec) {
		double d = vec.x - this.x;
		double e = vec.y - this.y;
		double f = vec.z - this.z;
		return d * d + e * e + f * f;
	}

	public double squaredDistanceTo(double x, double y, double z) {
		double d = x - this.x;
		double e = y - this.y;
		double f = z - this.z;
		return d * d + e * e + f * f;
	}

	public Vec3d multiply(double mult) {
		return this.multiply(mult, mult, mult);
	}

	public Vec3d negate() {
		return this.multiply(-1.0D);
	}

	public Vec3d multiply(Vec3d mult) {
		return this.multiply(mult.x, mult.y, mult.z);
	}

	public Vec3d multiply(double multX, double multY, double multZ) {
		return new Vec3d(this.x * multX, this.y * multY, this.z * multZ);
	}

	public double length() {
		return (double) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
	}

	public double lengthSquared() {
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof Vec3d)) {
			return false;
		} else {
			Vec3d vec3d = (Vec3d) o;
			if (Double.compare(vec3d.x, this.x) != 0) {
				return false;
			} else if (Double.compare(vec3d.y, this.y) != 0) {
				return false;
			} else {
				return Double.compare(vec3d.z, this.z) == 0;
			}
		}
	}

	public int hashCode() {
		long l = Double.doubleToLongBits(this.x);
		int i = (int) (l ^ l >>> 32);
		l = Double.doubleToLongBits(this.y);
		i = 31 * i + (int) (l ^ l >>> 32);
		l = Double.doubleToLongBits(this.z);
		i = 31 * i + (int) (l ^ l >>> 32);
		return i;
	}

	public String toString() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ")";
	}

	public Vec3d rotateX(double angle) {
		double f = Math.cos(angle);
		double g = Math.sin(angle);
		double d = this.x;
		double e = this.y * f + this.z * g;
		double h = this.z * f - this.y * g;
		return new Vec3d(d, e, h);
	}

	public Vec3d rotateY(float angle) {
		double f = Math.cos(angle);
		double g = Math.sin(angle);
		double d = this.x * f + this.z * g;
		double e = this.y;
		double h = this.z * f - this.x * g;
		return new Vec3d(d, e, h);
	}

	public Vec3d rotateZ(double f) {
		double g = Math.cos(f);
		double h = Math.sin(f);
		double d = this.x * g + this.y * h;
		double e = this.y * g - this.x * h;
		double i = this.z;
		return new Vec3d(d, e, i);
	}

	/*public static Vec3d fromPolar(Vec2f polar) {
		return fromPolar(polar.x, polar.y);
	}*/

	public static Vec3d fromPolar(double pitch, double yaw) {
		double f = Math.cos(-yaw * 0.017453292F - 3.1415927F);
		double g = Math.sin(-yaw * 0.017453292F - 3.1415927F);
		double h = -Math.cos(-pitch * 0.017453292F);
		double i = Math.sin(-pitch * 0.017453292F);
		return new Vec3d(g * h, i, f * h);
	}
}
