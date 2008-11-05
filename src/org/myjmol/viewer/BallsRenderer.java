/*
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.myjmol.g3d.Font3D;
import org.myjmol.g3d.Graphics3D;

// XIE: rewritten class

class BallsRenderer extends ShapeRenderer {

	private static Font3D font3d;
	private static Font3D chargeFont3d;
	private static final float ZERO = 0.0000000001f;

	static boolean drawVectorComponents;
	int minX, maxX, minY, maxY, minZ, maxZ;
	private boolean showHydrogens = true;
	private short colixSelection;
	private short width;
	private Point3i screen1, screen2, screen3;
	private Point3f tempPoint;
	private Vector3f tempVector, tempVector2;
	private boolean drawBall = true;

	BallsRenderer() {
		screen1 = new Point3i();
		screen2 = new Point3i();
		screen3 = new Point3i();
		tempPoint = new Point3f();
		tempVector = new Vector3f();
		tempVector2 = new Vector3f();
	}

	void render() {
		if (viewer.getMw2dFlag()) {
			render2d();
		}
		else {
			render3d();
		}
	}

	private void render2d() {
		Atom[] atoms = frame.atoms;
		Atom at;
		for (int i = frame.atomCount; --i >= 0;) {
			at = atoms[i];
			at.transform(viewer);
			render(at);
		}
	}

	private void render3d() {
		if (font3d == null)
			font3d = g3d.getFont3D(JmolConstants.LABEL_DEFAULT_FONTSIZE);
		if (chargeFont3d == null)
			chargeFont3d = g3d.getFont3D("Courier New", "Bold", JmolConstants.LABEL_DEFAULT_FONTSIZE + 2);
		minX = rectClip.x;
		maxX = minX + rectClip.width;
		minY = rectClip.y;
		maxY = minY + rectClip.height;
		boolean slabbing = viewer.getSlabEnabled();
		if (slabbing) {
			minZ = g3d.getSlab();
			maxZ = g3d.getDepth();
		}
		colixSelection = viewer.colorManager.getColixSelection();
		showHydrogens = viewer.getShowHydrogens();
		Atom[] atoms = frame.atoms;
		for (int i = frame.groupCount; --i >= 0;)
			frame.groups[i].minZ = Integer.MAX_VALUE;
		Atom at;
		for (int i = frame.atomCount; --i >= 0;) {
			at = atoms[i];
			if ((at.shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) == 0) {
				continue;
			}
			at.transform(viewer);
			if (slabbing) {
				if (g3d.isClippedZ(at.screenZ)) {
					at.clickabilityFlags = 0;
					int r = at.screenDiameter >> 1;
					if (at.screenZ < minZ - r || at.screenZ > maxZ + r)
						continue;
					if (!g3d.isInDisplayRange(at.screenX, at.screenY))
						continue;
				}
			}
			// note: above transform is required for all other renderings
			if (at.group != null) {
				int z = at.screenZ - (at.screenDiameter >> 1) - 2;
				if (z < at.group.minZ)
					at.group.minZ = Math.max(1, z);
			}
			if ((at.shapeVisibilityFlags & myVisibilityFlag) != 0) {
				render(at);
			}
		}
	}

	private void drawAtomVector(short colix, Atom a, Vector3f v, float s, int diameter) {
		if (v.lengthSquared() < 10000 * Float.MIN_VALUE)
			return;
		tempPoint.scale(s, v);
		tempPoint.add(a);
		screen1.set(viewer.transformPoint(tempPoint));
		tempVector.set(v);
		tempVector.normalize();
		tempPoint.scale(s, v);
		tempPoint.sub(tempVector);
		tempPoint.add(a);
		screen2.set(viewer.transformPoint(tempPoint));
		width = viewer.scaleToScreen((screen2.z + screen1.z) >> 1, diameter << 1);
		g3d.fillCone(colix, Graphics3D.ENDCAPS_SPHERICAL, width, screen2, screen1);
		width = viewer.scaleToScreen((a.screenZ + screen2.z) >> 1, diameter);
		g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL, width, a.screenX, a.screenY, a.screenZ, screen2.x,
				screen2.y, screen2.z);
	}

	private void renderVelocity(Atom atom, Vector3f velocity) {
		if (!atom.visible)
			return;
		ExtendedViewer viewer2 = (ExtendedViewer) viewer;
		if (velocity.lengthSquared() > ZERO)
			drawAtomVector(atom.colixAtom, atom, velocity, viewer2.getVelocityVectorScalingFactor(), 400);
	}

	private void drawSelectedVelocityVector(Atom atom, Vector3f velocity) {
		ExtendedViewer viewer2 = (ExtendedViewer) viewer;
		if (viewer.isSelected(atom.atomIndex)) {
			drawBall = false;
			viewer2.setVectorBox(atom, velocity, viewer2.getVelocityVectorScalingFactor() * 0.5f);
			if (drawVectorComponents) {
				if (Math.abs(velocity.x) > ZERO) {
					tempVector2.set(velocity.x, 0, 0);
					drawAtomVector(Graphics3D.RED, atom, tempVector2, viewer2.getVelocityVectorScalingFactor(), 200);
				}
				if (Math.abs(velocity.y) > ZERO) {
					tempVector2.set(0, velocity.y, 0);
					drawAtomVector(Graphics3D.GREEN, atom, tempVector2, viewer2.getVelocityVectorScalingFactor(), 200);
				}
				if (Math.abs(velocity.z) > ZERO) {
					tempVector2.set(0, 0, velocity.z);
					drawAtomVector(Graphics3D.BLUE, atom, tempVector2, viewer2.getVelocityVectorScalingFactor(), 200);
				}
			}
		}
	}

	private void render(Atom atom) {

		if (atom.screenZ <= 1)
			return;
		if (atom.getElementNumber() <= 0 || (!showHydrogens && atom.getElementNumber() == 1))
			return;

		if (atom.sigma != -1) { // if size is customized
			atom.screenDiameter = viewer.scaleToScreen(atom.screenZ, atom.sigma);
		}
		int diameter = atom.screenDiameter;
		if (viewer instanceof ExtendedViewer) {
			if (atom.isGenericAtom()) {
				if (atom.colixAtom != atom.colixCustom) {
					atom.colixAtom = atom.colixCustom;
				}
			}
			drawBall = true;
			ExtendedViewer viewer2 = (ExtendedViewer) viewer;
			Frame frame = viewer.modelManager.frame;
			if (frame != null) {
				if (frame.vibrationVectors != null && viewer2.velocityBitSet != null) {
					if (viewer2.velocityBitSet.get(atom.atomIndex)) {
						Vector3f v = frame.vibrationVectors[atom.atomIndex];
						if (v != null) {
							renderVelocity(atom, v);
						}
					}
					if (atom.atomIndex == viewer2.iAtomOfSelectedVelocity) {
						Vector3f v = frame.vibrationVectors[atom.atomIndex];
						drawSelectedVelocityVector(atom, v);
					}
				}
				if (viewer2.getShowCharge()) {
					if (frame.partialCharges != null) {
						float charge = frame.partialCharges[atom.atomIndex];
						if (charge > 0) {
							g3d.drawString("+", chargeFont3d, Graphics3D.BLUE, atom.screenX - 4, atom.screenY + 4,
									atom.screenZ - diameter);
						}
						else if (charge < 0) {
							g3d.drawString("-", chargeFont3d, Graphics3D.RED, atom.screenX - 4, atom.screenY + 4,
									atom.screenZ - diameter);
						}
					}
				}
			}
			if (viewer2.getShowAtomIndex()) {
				g3d.drawString(atom.atomIndex + "", font3d, atom.colixAtom, atom.screenX - (font3d.fontSize >> 1),
						atom.screenY + (font3d.fontSize >> 1), atom.screenZ);
			}
			else {
				if (atom.visible) {
					if (drawBall) {
						if (atom.screenZ * 2 < atom.screenDiameter) {
							short colix = Graphics3D.getTranslucentColix(atom.colixAtom);
							g3d
									.fillSphereCentered(colix, atom.screenDiameter, atom.screenX, atom.screenY,
											atom.screenZ);
						}
						else {
							if (viewer2.hidenBitSet == null || !viewer2.hidenBitSet.get(atom.atomIndex)) {
								if (viewer2.translucentBitSet == null) {
									g3d.fillSphereCentered(atom.colixAtom, atom.screenDiameter, atom.screenX,
											atom.screenY, atom.screenZ);
								}
								else {
									if (viewer2.translucentBitSet.get(atom.atomIndex)) {
										short colix = Graphics3D.getTranslucentColix(atom.colixAtom);
										g3d.fillSphereCentered(colix, atom.screenDiameter, atom.screenX, atom.screenY,
												atom.screenZ);
									}
									else {
										g3d.fillSphereCentered(atom.colixAtom, atom.screenDiameter, atom.screenX,
												atom.screenY, atom.screenZ);
									}
								}
							}
						}
					}
					else {
						short colix = Graphics3D.getTranslucentColix(atom.colixAtom);
						g3d.fillSphereCentered(colix, atom.screenDiameter, atom.screenX, atom.screenY, atom.screenZ);
					}
				}
			}
		}
		else {
			if (atom.screenZ * 2 < atom.screenDiameter) {
				short colix = Graphics3D.getTranslucentColix(atom.colixAtom);
				g3d.fillSphereCentered(colix, atom.screenDiameter, atom.screenX, atom.screenY, atom.screenZ);
			}
			else {
				g3d.fillSphereCentered(atom.colixAtom, atom.screenDiameter, atom.screenX, atom.screenY, atom.screenZ);
			}
		}

		if (atom.annotationKey) {
			drawPin(atom);
		}
		if (atom.interactionKey && viewer.interactionCentersVisible) {
			drawInteractionCenter(atom);
		}
		if (viewer.getSelectionHaloEnabled() && viewer.isSelected(atom.atomIndex)) {
			fillScreenedCircle(atom, colixSelection);
		}
		if (atom.atomIndex == viewer.clickedAtom) {
			fillScreenedClickedSign(atom, Graphics3D.CYAN);
		}

	}

	private void drawArrow(Atom atom) {
		g3d.fillTriangle(atom.interactionKeyColix, screen1, screen2, screen3);
		g3d.drawDottedLine(atom.interactionKeyColix, screen1.x, screen1.y, screen1.z, atom.screenX, atom.screenY,
				atom.screenZ);
	}

	private void drawInteractionCenter(Atom atom) {
		int a = atom.screenDiameter;
		int r = a / 6;
		screen1.set(atom.screenX, atom.screenY + a, atom.screenZ);
		screen2.set(atom.screenX - r / 2, atom.screenY + 5 * r, atom.screenZ);
		screen3.set(atom.screenX + r / 2, atom.screenY + 5 * r, atom.screenZ);
		drawArrow(atom);
		screen1.y = atom.screenY - a;
		screen2.y = atom.screenY - 5 * r;
		screen3.y = atom.screenY - 5 * r;
		drawArrow(atom);
		screen1.x = atom.screenX + a;
		screen1.y = atom.screenY;
		screen2.x = atom.screenX + 5 * r;
		screen2.y = atom.screenY - r / 2;
		screen3.x = atom.screenX + 5 * r;
		screen3.y = atom.screenY + r / 2;
		drawArrow(atom);
		screen1.x = atom.screenX - a;
		screen1.y = atom.screenY;
		screen2.x = atom.screenX - 5 * r;
		screen2.y = atom.screenY - r / 2;
		screen3.x = atom.screenX - 5 * r;
		screen3.y = atom.screenY + r / 2;
		drawArrow(atom);
	}

	private void drawPin(Atom atom) {

		screen1.x = screen2.x = atom.screenX;
		screen1.z = screen2.z = atom.screenZ;
		if (atom.pin == null)
			atom.pin = new AtomPin(atom);
		atom.pin.setSize();

		screen1.y = atom.pin.handleYmin();
		screen2.y = atom.pin.handleYmax() + 1;
		g3d.fillCylinder(atom.annotationKeyColix, Graphics3D.ENDCAPS_FLAT, atom.screenDiameter, screen1, screen2);

		if (viewer.activeAtomAnnotation == atom.atomIndex) {
			int x = atom.screenX - (atom.screenDiameter >> 1) - 4;
			int y = screen1.y - 4;
			int w = atom.screenDiameter + 8;
			int h = screen2.y - screen1.y + 8;
			g3d.fillScreenedRect(Graphics3D.YELLOW, x, y, atom.screenZ, w, h);
		}

		screen1.y = atom.pin.handleYmax();
		screen2.y = atom.screenY - atom.pin.r;
		g3d.fillCylinder(atom.annotationKeyColix, Graphics3D.ENDCAPS_FLAT, atom.pin.diameter, screen1, screen2);

		screen1.y = atom.screenY - atom.pin.r;
		screen2.y = atom.screenY + 2 * atom.pin.r;
		g3d.fillCylinder(atom.annotationKeyColix, Graphics3D.ENDCAPS_FLAT, atom.pin.width >> 1, screen1, screen2);

	}

	private void fillScreenedCircle(Atom atom, short colix) {
		int halowidth = atom.screenDiameter >> 2;
		if (halowidth < 4)
			halowidth = 4;
		else if (halowidth > 10)
			halowidth = 10;
		int haloDiameter = atom.screenDiameter + 2 * halowidth;
		g3d.fillScreenedCircleCentered(colix, haloDiameter, atom.screenX, atom.screenY, atom.screenZ);
	}

	private void fillScreenedClickedSign(Atom atom, short colix) {
		int a = atom.screenDiameter >> 2;
		if (a < 4)
			a = 4;
		else if (a > 10)
			a = 10;
		a = atom.screenDiameter + 2 * a;
		g3d.fillScreenedCircleCentered(colix, a, atom.screenX, atom.screenY, atom.screenZ);
		int r = a / 3;
		screen1.set(atom.screenX, atom.screenY + a, atom.screenZ);
		screen2.set(atom.screenX - r, atom.screenY + 2 * r, atom.screenZ);
		screen3.set(atom.screenX + r, atom.screenY + 2 * r, atom.screenZ);
		g3d.fillScreenedTriangle(colix, screen1, screen2, screen3);
		screen1.y = atom.screenY - a;
		screen2.y = atom.screenY - 2 * r;
		screen3.y = atom.screenY - 2 * r;
		g3d.fillScreenedTriangle(colix, screen1, screen2, screen3);
		screen1.x = atom.screenX + a;
		screen1.y = atom.screenY;
		screen2.x = atom.screenX + 2 * r;
		screen2.y = atom.screenY - r;
		screen3.x = atom.screenX + 2 * r;
		screen3.y = atom.screenY + r;
		g3d.fillScreenedTriangle(colix, screen1, screen2, screen3);
		screen1.x = atom.screenX - a;
		screen1.y = atom.screenY;
		screen2.x = atom.screenX - 2 * r;
		screen2.y = atom.screenY - r;
		screen3.x = atom.screenX - 2 * r;
		screen3.y = atom.screenY + r;
		g3d.fillScreenedTriangle(colix, screen1, screen2, screen3);
	}

}