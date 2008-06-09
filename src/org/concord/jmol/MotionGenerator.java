/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */
package org.concord.jmol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.InteractionCenter;
import org.myjmol.api.JmolViewer;

/**
 * The interaction between the Rover and the Interaction Centers are given by the following potential energy function:
 * 
 * v(r) = (core / r)^12 + q * Q / r + (p dot r) * Q / r^3
 * 
 * The first term models the Pauli repulsion. The second term is the Coulombic interaction. The third term is the
 * charge-dipole interaction.
 * 
 * @author Charles Xie
 * 
 */
class MotionGenerator {

	private final static float ZERO = 0.000001f;
	private final static float ANGULAR_FRICTION_FACTOR = 50;
	private final static float CUTOFF_RATIO_SQUARED = 10000;

	private List<InteractionCenter> centers;
	private Point3f position;
	private Vector3f velocity;
	private Vector3f force;
	private Vector3f omega;
	private Vector3f torque;
	private float x0, y0, z0, xi, yi, zi, rsq, rsq6, fi, fx, fy, fz;
	private float core12;
	private float interactionStrength;
	private int count;
	private JmolContainer container;
	private JmolViewer viewer;
	private float steeringStrength;
	private boolean collisionDetectionForAllAtoms;
	private Point3f tmpPoint;
	private Matrix3f tmpMatrix;
	private Vector3f tmpVector;
	private Point3f rotateXyz;
	private AxisAngle4f tmpAxisAngle;

	MotionGenerator(JmolContainer jmolContainer) {
		container = jmolContainer;
		viewer = container.jmol.viewer;
		centers = Collections.synchronizedList(new ArrayList<InteractionCenter>());
		position = new Point3f();
		velocity = new Vector3f();
		force = new Vector3f();
		omega = new Vector3f();
		torque = new Vector3f();
		tmpPoint = new Point3f();
		tmpMatrix = new Matrix3f();
		tmpVector = new Vector3f();
		rotateXyz = new Point3f();
		tmpAxisAngle = new AxisAngle4f();
	}

	void setCollisionDetectionForAllAtoms(boolean b) {
		collisionDetectionForAllAtoms = b;
	}

	boolean getCollisionDetectionForAllAtoms() {
		return collisionDetectionForAllAtoms;
	}

	int getCount() {
		return count;
	}

	void setPosition(Point3f p) {
		position.set(p);
	}

	Point3f getPosition() {
		return position;
	}

	void setSteeringStrength(float strength) {
		steeringStrength = strength;
	}

	void changeSteeringStrength(float change) {
		steeringStrength += change;
		if (steeringStrength < 0.1f)
			steeringStrength = 0.1f;
		else if (steeringStrength > 5.0f)
			steeringStrength = 5.0f;
	}

	float getSteeringStrength() {
		return steeringStrength;
	}

	void reset() {
		clearCenters();
		steeringStrength = 0;
		count = 0;
		collisionDetectionForAllAtoms = false;
		resetSpeed();
	}

	void resetSpeed() {
		velocity.set(0, 0, 0);
		omega.set(0, 0, 0);
	}

	void clearCenters() {
		centers.clear();
	}

	void addInteractionCenter(InteractionCenter ic) {
		synchronized (centers) {
			if (centers.contains(ic))
				return;
			centers.add(ic);
		}
	}

	void removeInteractionCenter(InteractionCenter c) {
		centers.remove(c);
	}

	boolean readyToPaint() {
		return count % 10 == 0;
	}

	void move(float delta) {

		x0 = position.x;
		y0 = position.y;
		z0 = position.z;
		fx = 0;
		fy = 0;
		fz = 0;
		torque.set(0, 0, 0);

		if (collisionDetectionForAllAtoms) {
			int n = viewer.getAtomCount();
			Point3f p;
			float r;
			for (int i = 0; i < n; i++) {
				p = viewer.getAtomPoint3f(i);
				r = viewer.getVdwRadius(i);
				xi = p.x - x0;
				yi = p.y - y0;
				zi = p.z - z0;
				rsq = xi * xi + yi * yi + zi * zi;
				if (rsq > CUTOFF_RATIO_SQUARED * r * r)
					continue;
				rsq6 = rsq * rsq * rsq;
				rsq6 *= rsq6;
				core12 = r * container.rover.getDiameter();
				core12 = core12 * core12 * core12;
				core12 *= core12;
				fi = -12 * core12 / (rsq6 * rsq);
				fx += fi * xi;
				fy += fi * yi;
				fz += fi * zi;
			}
		}

		synchronized (centers) {

			if (!centers.isEmpty()) {

				for (InteractionCenter c : centers) {

					xi = c.getX() - x0;
					yi = c.getY() - y0;
					zi = c.getZ() - z0;
					rsq = xi * xi + yi * yi + zi * zi;

					if (rsq > CUTOFF_RATIO_SQUARED * c.getRadius() * c.getRadius())
						continue;

					// repulsion (just in case pauliRepulsionForAllAtoms fails)
					rsq6 = rsq * rsq * rsq;
					rsq6 *= rsq6;
					core12 = c.getRadius() * container.rover.getDiameter();
					core12 = core12 * core12 * core12;
					core12 *= core12;
					fi = -12 * core12 / (rsq6 * rsq);
					fx += fi * xi;
					fy += fi * yi;
					fz += fi * zi;

					if (Math.abs(c.getCharge()) > ZERO) {

						float rsqrt = (float) Math.sqrt(rsq);

						// charge-charge
						if (Math.abs(container.rover.getCharge()) > ZERO) {
							fi = -c.getCharge() * container.rover.getCharge() / (rsqrt * rsq);
							fx += fi * xi;
							fy += fi * yi;
							fz += fi * zi;
						}

						// charge-dipole
						if (Math.abs(container.rover.getDipole()) > ZERO) {
							fi = -3 * c.getCharge() * container.rover.getDipole() * zi / (rsq * rsq * rsqrt);
							fx += fi * xi;
							fy += fi * yi;
							fz += fi * zi + c.getCharge() * container.rover.getDipole() / (rsq * rsqrt);
						}

					}

				}

			}
		}

		// handle translation
		force.set(fx, fy, fz);
		interactionStrength = force.lengthSquared();
		Vector3f steeringDirection = container.jmol.navigator.getSteeringDirection();
		if (steeringDirection.length() > ZERO) {
			force.x -= steeringDirection.x * steeringStrength;
			force.y -= steeringDirection.y * steeringStrength;
			force.z -= steeringDirection.z * steeringStrength;
		}
		force.x -= velocity.x * container.rover.getFriction();
		force.y -= velocity.y * container.rover.getFriction();
		force.z -= velocity.z * container.rover.getFriction();
		float mass = container.rover.getMass();
		if (mass < 0.1f)
			mass = 0.1f;
		force.scale(1.0f / mass);

		tmpPoint.scale(delta, force);
		velocity.add(tmpPoint);

		tmpPoint.scale(delta, velocity);
		position.add(tmpPoint);
		tmpPoint.scale(0.5f * delta * delta, force);
		position.add(tmpPoint);

		// handle rotation
		Vector3f rotateDirection = container.jmol.navigator.getRotationDirection();
		if (rotateDirection.length() > ZERO) {
			torque.x += rotateDirection.x * steeringStrength;
			torque.y += rotateDirection.y * steeringStrength;
			torque.z += rotateDirection.z * steeringStrength;
		}
		torque.x -= omega.x * container.rover.getFriction() * ANGULAR_FRICTION_FACTOR;
		torque.y -= omega.y * container.rover.getFriction() * ANGULAR_FRICTION_FACTOR;
		torque.z -= omega.z * container.rover.getFriction() * ANGULAR_FRICTION_FACTOR;
		float moi = container.rover.getMomentOfInertia();
		if (moi < 1)
			moi = 1;
		torque.scale(1.0f / moi);

		tmpPoint.scale(delta, torque);
		omega.add(tmpPoint);

		tmpPoint.scale(delta, omega);
		torque.scale(0.5f * delta * delta);
		tmpPoint.add(torque);

		if (interactionStrength > 0.0001f) {
			float angle = 0;
			tmpMatrix.invert(viewer.getRotationMatrix());
			tmpVector.set(0, 0, 1);
			tmpMatrix.transform(tmpVector);
			switch (container.rover.getTurningOption()) {
			case Rover.TURN_TO_VELOCITY: // turn the rover to the direction of the velocity
				boolean sameDirection = tmpVector.dot(velocity) >= 0;
				angle = velocity.angle(tmpVector);
				if (!sameDirection) {
					angle = (float) Math.PI - angle;
				}
				tmpVector.cross(tmpVector, velocity);
				break;
			case Rover.TURN_TO_FORCE: // turn the rover to the direction of the force
				sameDirection = tmpVector.dot(force) >= 0;
				angle = force.angle(tmpVector);
				if (!sameDirection) {
					angle = (float) Math.PI - angle;
				}
				tmpVector.cross(tmpVector, force);
				break;
			}
			viewer.getRotationMatrix().transform(tmpVector); // rotate this vector in the view space
			tmpAxisAngle.set(tmpVector, angle * (interactionStrength > 0.001f ? 0.001f : interactionStrength));
			tmpMatrix.set(tmpAxisAngle);
			computeRotateXyz(tmpMatrix);
			tmpPoint.add(rotateXyz);
		}

		viewer.rotateXBy(tmpPoint.x);
		viewer.rotateYBy(tmpPoint.y);
		viewer.rotateZBy(tmpPoint.z);

		count++;

	}

	void computeRotateXyz(Matrix3f matrixRotate) {
		float m20 = matrixRotate.m20;
		float rY = -(float) Math.asin(m20);
		float rX, rZ;
		if (Math.abs(m20 - 1) < ZERO) {
			rX = -(float) Math.atan2(matrixRotate.m12, matrixRotate.m11);
			rZ = 0;
		}
		else {
			rX = (float) Math.atan2(matrixRotate.m21, matrixRotate.m22);
			rZ = (float) Math.atan2(matrixRotate.m10, matrixRotate.m00);
		}
		if (Float.isNaN(rX) || Float.isNaN(rY) || Float.isNaN(rZ)) {
			rotateXyz.set(0, 0, 0);
		}
		else {
			rotateXyz.set(rX, rY, rZ);
		}
	}

}
