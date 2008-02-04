/* $RCSfile: SticksRenderer.java,v $
 * $Author: qxie $
 * $Date: 2007-09-28 20:33:28 $
 * $Revision: 1.36 $

 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.viewer;

import org.jmol.g3d.*;

import java.awt.Rectangle;

import javax.vecmath.Point3i;

class SticksRenderer extends ShapeRenderer {

	boolean showMultipleBonds;
	byte modeMultipleBond;
	boolean showHydrogens;
	byte endcaps;

	boolean ssbondsBackbone;
	boolean hbondsBackbone;
	boolean bondsBackbone;
	boolean hbondsSolid;
	boolean asBits;

	Atom atomA, atomB;
	int xA, yA, zA;
	int xB, yB, zB;
	int dx, dy;
	int mag2d, mag2d2;
	short colixA, colixB;
	int width;
	int bondOrder;
	short madBond;

	private Point3i p1, p2, p3, p4;

	void render() {
		asBits = viewer.getTestFlag2();

		endcaps = Graphics3D.ENDCAPS_SPHERICAL;
		showMultipleBonds = viewer.getShowMultipleBonds();
		modeMultipleBond = viewer.getModeMultipleBond();

		ssbondsBackbone = viewer.getSsbondsBackbone();
		hbondsBackbone = viewer.getHbondsBackbone();
		bondsBackbone = hbondsBackbone | ssbondsBackbone;
		hbondsSolid = viewer.getHbondsSolid();

		Bond[] bonds = frame.bonds;
		for (int i = frame.bondCount; --i >= 0;) {
			Bond bond = bonds[i];
			if (bond != null && (bond.shapeVisibilityFlags & myVisibilityFlag) != 0) {
				render(bond, i);
			}
		}

	}

	// XIE
	void render(Bond bond, int index) {
		if (viewer.getMw2dFlag()) {
			render2d(bond, index);
		}
		else {
			render3d(bond, index);
		}
	}

	private void render3d(Bond bond, int index) {
		madBond = bond.mad;
		int order = bond.order;
		Atom atomA = bond.atom1;
		Atom atomB = bond.atom2;
		// XIE: begin
		if (g3d != null) {
			boolean displayA = g3d.isInDisplayRange(atomA.screenX, atomA.screenY);
			boolean displayB = g3d.isInDisplayRange(atomB.screenX, atomB.screenY);
			if (!displayA && !displayB)
				return;
		}
		if (!bond.atom1.isModelVisible() || !bond.atom2.isModelVisible() || frame.bsHidden.get(atomA.atomIndex)
				|| frame.bsHidden.get(atomB.atomIndex))
			return;
		// XIE: end

		colixA = Graphics3D.getColixInherited(bond.colix, atomA.colixAtom);
		colixB = Graphics3D.getColixInherited(bond.colix, atomB.colixAtom);
		if (bondsBackbone) {
			if (ssbondsBackbone && (order & JmolConstants.BOND_SULFUR_MASK) != 0) {
				// for ssbonds, always render the sidechain, then render the backbone version
				/*
				 * mth 2004 04 26 No, we are not going to do this any more render(bond, atomA, atomB);
				 */
				atomA = getBackboneAtom(atomA);
				atomB = getBackboneAtom(atomB);
			}
			else if (hbondsBackbone && (order & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
				atomA = getBackboneAtom(atomA);
				atomB = getBackboneAtom(atomB);
			}
		}
		render(bond, atomA, atomB);
		// XIE: begin
		if (bond.annotationKey) {
			drawPin(bond, index);
		}
		if (bond.interactionKey) {
			drawInteractionCenter(bond);
		}
		if (index == viewer.clickedBond) {
			fillScreenedClickedSign(bond, Graphics3D.CYAN);
		}
		// XIE: end
	}

	// XIE
	private void drawArrow(Bond bond, int x, int y, int z) {
		g3d.fillTriangle(bond.interactionKeyColix, p1, p2, p3);
		g3d.drawDottedLine(bond.interactionKeyColix, p1.x, p1.y, p1.z, x, y, z);
	}

	// XIE:
	private void drawInteractionCenter(Bond bond) {
		int a = (bond.atom1.screenDiameter + bond.atom2.screenDiameter) >> 1;
		int r = a / 6;
		initPs();
		int x = (bond.atom1.screenX + bond.atom2.screenX) >> 1;
		int y = (bond.atom1.screenY + bond.atom2.screenY) >> 1;
		int z = (bond.atom1.screenZ + bond.atom2.screenZ) >> 1;
		p1.set(x, y + a, z);
		p2.set(x - r / 2, y + 5 * r, z);
		p3.set(x + r / 2, y + 5 * r, z);
		drawArrow(bond, x, y, z);
		p1.y = y - a;
		p2.y = y - 5 * r;
		p3.y = y - 5 * r;
		drawArrow(bond, x, y, z);
		p1.x = x + a;
		p1.y = y;
		p2.x = x + 5 * r;
		p2.y = y - r / 2;
		p3.x = x + 5 * r;
		p3.y = y + r / 2;
		drawArrow(bond, x, y, z);
		p1.x = x - a;
		p1.y = y;
		p2.x = x - 5 * r;
		p2.y = y - r / 2;
		p3.x = x - 5 * r;
		p3.y = y + r / 2;
		drawArrow(bond, x, y, z);
	}

	// XIE: render selection halos for bonds
	private void fillScreenedClickedSign(Bond bond, short colix) {
		int dx = bond.atom2.screenX - bond.atom1.screenX;
		int dy = bond.atom2.screenY - bond.atom1.screenY;
		float inv = width / (float) Math.sqrt(dx * dx + dy * dy);
		float cost = dx * inv;
		float sint = dy * inv;
		initPs();
		p1.set((int) (bond.atom1.screenX - sint), (int) (bond.atom1.screenY + cost), bond.atom1.screenZ);
		p2.set((int) (bond.atom1.screenX + sint), (int) (bond.atom1.screenY - cost), bond.atom1.screenZ);
		p3.set((int) (bond.atom2.screenX - sint), (int) (bond.atom2.screenY + cost), bond.atom2.screenZ);
		p4.set((int) (bond.atom2.screenX + sint), (int) (bond.atom2.screenY - cost), bond.atom2.screenZ);
		g3d.fillScreenedTriangle(colix, p1, p2, p3);
		g3d.fillScreenedTriangle(colix, p2, p3, p4);
	}

	private void initPs() {
		if (p1 == null)
			p1 = new Point3i();
		if (p2 == null)
			p2 = new Point3i();
		if (p3 == null)
			p3 = new Point3i();
		if (p4 == null)
			p4 = new Point3i();
	}

	private void drawPin(Bond bond, int index) {
		if (bond.pin == null)
			bond.pin = new BondPin(bond);
		initPs();
		bond.pin.update(width);
		p1.z = p2.z = bond.pin.center.z;

		int headSize = 2 * width;
		p1.x = (int) (bond.pin.center.x - bond.pin.sint * width * 4);
		p2.x = (int) (bond.pin.center.x - bond.pin.sint * width);
		p1.y = (int) (bond.pin.center.y + bond.pin.cost * width * 4);
		p2.y = (int) (bond.pin.center.y + bond.pin.cost * width);
		g3d.fillCylinder(bond.annotationKeyColix, Graphics3D.ENDCAPS_FLAT, headSize, p1, p2);

		if (viewer.activeBondAnnotation == index) {
			p1.x = (int) (bond.pin.center.x - bond.pin.sint * (width * 4 + 4));
			p2.x = (int) (bond.pin.center.x - bond.pin.sint * (width + 4));
			p1.y = (int) (bond.pin.center.y + bond.pin.cost * (width * 4 + 4));
			p2.y = (int) (bond.pin.center.y + bond.pin.cost * (width + 4));
			p3.z = p4.z = bond.pin.center.z;
			p3.x = p1.x;
			p3.y = p1.y;
			p4.x = p2.x;
			p4.y = p2.y;
			headSize = width + 4;
			p1.x -= headSize * bond.pin.cost;
			p1.y -= headSize * bond.pin.sint;
			p3.x += headSize * bond.pin.cost;
			p3.y += headSize * bond.pin.sint;
			p2.x -= headSize * bond.pin.cost;
			p2.y -= headSize * bond.pin.sint;
			p4.x += headSize * bond.pin.cost;
			p4.y += headSize * bond.pin.sint;
			g3d.fillScreenedTriangle(Graphics3D.YELLOW, p1, p3, p4);
			g3d.fillScreenedTriangle(Graphics3D.YELLOW, p1, p4, p2);
		}

		p1.x = (int) (bond.pin.center.x - bond.pin.sint * (2 + width));
		p2.x = (int) (bond.pin.center.x - bond.pin.sint * width * 0.6f);
		p1.y = (int) (bond.pin.center.y + bond.pin.cost * (2 + width));
		p2.y = (int) (bond.pin.center.y + bond.pin.cost * width * 0.6f);
		g3d.fillCylinder(bond.annotationKeyColix, Graphics3D.ENDCAPS_FLAT, width * 3, p1, p2);

		p1.x = (int) (bond.pin.center.x - bond.pin.sint * width * 0.6f);
		p2.x = (int) (bond.pin.center.x + bond.pin.sint * width * 2);
		p1.y = (int) (bond.pin.center.y + bond.pin.cost * width * 0.6f);
		p2.y = (int) (bond.pin.center.y - bond.pin.cost * width * 2);
		g3d.fillCylinder(bond.annotationKeyColix, Graphics3D.ENDCAPS_FLAT, bond.pin.width >> 1, p1, p2);

	}

	// XIE
	private void render2d(Bond bond, int index) {
		madBond = bond.mad;
		Atom atomA = bond.atom1;
		Atom atomB = bond.atom2;
		colixA = Graphics3D.getColixInherited(bond.colix, atomA.colixAtom);
		colixB = Graphics3D.getColixInherited(bond.colix, atomB.colixAtom);
		render(bond, atomA, atomB);
	}

	void render(Bond bond, Atom atomA, Atom atomB) {
		this.atomA = atomA;
		xA = atomA.screenX;
		yA = atomA.screenY;
		zA = atomA.screenZ;
		this.atomB = atomB;
		xB = atomB.screenX;
		yB = atomB.screenY;
		zB = atomB.screenZ;
		dx = xB - xA;
		dy = yB - yA;

		// XIE: in the navigation mode, width is scaled according to the position of the furthest atom
		// It guarantees that the bond width is never larger than the diameter of the atom
		width = viewer.scaleToScreen(viewer.getNavigationMode() ? Math.max(zA, zB) : (zA + zB) >> 1, bond.mad);
		bondOrder = getRenderBondOrder(bond.order);
		switch (bondOrder) {
		case 1:
		case 2:
		case 3:
			renderCylinder(0);
			break;
		case JmolConstants.BOND_PARTIAL01:
			bondOrder = 1;
			renderCylinder(1);
			break;
		case JmolConstants.BOND_PARTIAL12:
		case JmolConstants.BOND_AROMATIC:
			bondOrder = 2;
			renderCylinder(getAromaticDottedBondMask(bond));
			break;
		case JmolConstants.BOND_STEREO_NEAR:
		case JmolConstants.BOND_STEREO_FAR:
			renderTriangle(bond);
			break;
		default:
			if ((bondOrder & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
				if (hbondsSolid) {
					bondOrder = 1;
					renderCylinder(0);
				}
				else {
					renderHbondDashed();
				}
				break;
			}
		}
	}

	Atom getBackboneAtom(Atom atom) {
		if (atom.group instanceof Monomer)
			return ((Monomer) atom.group).getLeadAtom();
		return atom;
	}

	int getRenderBondOrder(int order) {
		if ((order & JmolConstants.BOND_SULFUR_MASK) != 0)
			order &= ~JmolConstants.BOND_SULFUR_MASK;
		if ((order & JmolConstants.BOND_PARTIAL_MASK) != 0)
			return order;
		if ((order & JmolConstants.BOND_COVALENT_MASK) != 0) {
			if (order == 1
					|| !showMultipleBonds
					|| modeMultipleBond == JmolConstants.MULTIBOND_NEVER
					|| (modeMultipleBond == JmolConstants.MULTIBOND_NOTSMALL && madBond > JmolConstants.madMultipleBondSmallMaximum)) {
				return 1;
			}
		}
		return order;
	}

	private void renderCylinder(int dottedMask) {
		setColixNearClip(); // XIE
		boolean lineBond = (width <= 1);
		if (dx == 0 && dy == 0) {
			// end-on view
			if (!lineBond) {
				int space = width / 8 + 3;
				int step = width + space;
				int y = yA - ((bondOrder == 1) ? 0 : (bondOrder == 2) ? step / 2 : step);
				do {
					g3d.fillCylinder(colixA, colixA, endcaps, width, xA, y, zA, xA, y, zA);
					y += step;
				} while (--bondOrder > 0);
			}
			return;
		}
		if (bondOrder == 1) {
			if ((dottedMask & 1) != 0) {
				drawDashed(lineBond, xA, yA, zA, xB, yB, zB);
			}
			else {
				if (lineBond) {
					g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
				}
				else if (asBits) {// time test shows bitset method to be slower
					g3d.fillCylinderBits(colixA, colixB, endcaps, width, xA, yA, zA, xB, yB, zB);
				}
				else {
					g3d.fillCylinder(colixA, colixB, endcaps, width, xA, yA, zA, xB, yB, zB);
				}
			}
			return;
		}
		int dxB = dx * dx;
		int dyB = dy * dy;
		int mag2d2 = dxB + dyB;
		mag2d = (int) (Math.sqrt(mag2d2) + 0.5);
		resetAxisCoordinates(lineBond);
		while (true) {
			if ((dottedMask & 1) != 0) {
				drawDashed(lineBond, xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
			}
			else {
				if (lineBond)
					g3d.drawLine(colixA, colixB, xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
				else g3d.fillCylinder(colixA, colixB, endcaps, width, xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
			}
			dottedMask >>= 1;
			if (--bondOrder == 0)
				break;
			stepAxisCoordinates();
		}
	}

	int cylinderNumber;
	int xAxis1, yAxis1, zAxis1, xAxis2, yAxis2, zAxis2, dxStep, dyStep;

	void resetAxisCoordinates(boolean lineBond) {
		cylinderNumber = 0;
		int space = mag2d >> 3;
		int step = width + space;
		dxStep = step * dy / mag2d;
		dyStep = step * -dx / mag2d;

		xAxis1 = xA;
		yAxis1 = yA;
		zAxis1 = zA;
		xAxis2 = xB;
		yAxis2 = yB;
		zAxis2 = zB;

		if (bondOrder == 2) {
			xAxis1 -= dxStep / 2;
			yAxis1 -= dyStep / 2;
			xAxis2 -= dxStep / 2;
			yAxis2 -= dyStep / 2;
		}
		else if (bondOrder == 3) {
			xAxis1 -= dxStep;
			yAxis1 -= dyStep;
			xAxis2 -= dxStep;
			yAxis2 -= dyStep;
		}
	}

	void stepAxisCoordinates() {
		xAxis1 += dxStep;
		yAxis1 += dyStep;
		xAxis2 += dxStep;
		yAxis2 += dyStep;
	}

	Rectangle rectTemp = new Rectangle();

	private static float wideWidthAngstroms = 0.4f;

	private void renderTriangle(Bond bond) {
		// for now, always solid
		int mag2d = (int) Math.sqrt(dx * dx + dy * dy);
		int wideWidthPixels = (int) viewer.scaleToScreen(zB, wideWidthAngstroms);
		int dxWide, dyWide;
		if (mag2d == 0) {
			dxWide = 0;
			dyWide = wideWidthPixels;
		}
		else {
			dxWide = wideWidthPixels * -dy / mag2d;
			dyWide = wideWidthPixels * dx / mag2d;
		}
		int xWideUp = xB + dxWide / 2;
		int xWideDn = xWideUp - dxWide;
		int yWideUp = yB + dyWide / 2;
		int yWideDn = yWideUp - dyWide;
		if (colixA == colixB) {
			g3d.drawfillTriangle(colixA, xA, yA, zA, xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
		}
		else {
			int xMidUp = (xA + xWideUp) / 2;
			int yMidUp = (yA + yWideUp) / 2;
			int zMid = (zA + zB) / 2;
			int xMidDn = (xA + xWideDn) / 2;
			int yMidDn = (yA + yWideDn) / 2;
			g3d.drawfillTriangle(colixA, xA, yA, zA, xMidUp, yMidUp, zMid, xMidDn, yMidDn, zMid);
			g3d.drawfillTriangle(colixB, xMidUp, yMidUp, zMid, xMidDn, yMidDn, zMid, xWideDn, yWideDn, zB);
			g3d.drawfillTriangle(colixB, xMidUp, yMidUp, zMid, xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
		}
	}

	void drawDottedCylinder(short colixA, short colixB, int width, int x1, int y1, int z1, int x2, int y2, int z2) {
		int dx = x2 - x1;
		int dy = y2 - y1;
		int dz = z2 - z1;
		for (int i = 8; --i >= 0;) {
			int x = x1 + (dx * i) / 7;
			int y = y1 + (dy * i) / 7;
			int z = z1 + (dz * i) / 7;
			g3d.fillSphereCentered(i > 3 ? colixB : colixA, width, x, y, z);
		}
	}

	private int getAromaticDottedBondMask(Bond bond) {
		Atom atomC = findAromaticNeighbor(bond);
		if (atomC == null)
			return 1;
		int dxAC = atomC.screenX - xA;
		int dyAC = atomC.screenY - yA;
		return (dx * dyAC - dy * dxAC) >= 0 ? 2 : 1;
	}

	private Atom findAromaticNeighbor(Bond bond) {
		Bond[] bonds = atomB.bonds;
		for (int i = bonds.length; --i >= 0;) {
			Bond bondT = bonds[i];
			if ((bondT.order & JmolConstants.BOND_AROMATIC) == 0)
				continue;
			if (bondT == bond)
				continue;
			if (bondT.atom1 == atomB)
				return bondT.atom2;
			if (bondT.atom2 == atomB)
				return bondT.atom1;
		}
		return null;
	}

	void drawDashed(boolean lineBond, int xA, int yA, int zA, int xB, int yB, int zB) {
		setColixNearClip(); // XIE
		int dx = xB - xA;
		int dy = yB - yA;
		int dz = zB - zA;
		int i = 2;
		while (i <= 9) {
			int xS = xA + (dx * i) / 12;
			int yS = yA + (dy * i) / 12;
			int zS = zA + (dz * i) / 12;
			i += 3;
			int xE = xA + (dx * i) / 12;
			int yE = yA + (dy * i) / 12;
			int zE = zA + (dz * i) / 12;
			i += 2;
			if (lineBond)
				g3d.drawLine(colixA, colixB, xS, yS, zS, xE, yE, zE);
			else g3d.fillCylinder(colixA, colixB, Graphics3D.ENDCAPS_FLAT, width, xS, yS, zS, xE, yE, zE);
		}
	}

	void renderHbondDashed() {
		boolean lineBond = (width <= 1);
		int dx = xB - xA;
		int dy = yB - yA;
		int dz = zB - zA;
		int i = 1;
		while (i < 10) {
			int xS = xA + (dx * i) / 10;
			int yS = yA + (dy * i) / 10;
			int zS = zA + (dz * i) / 10;
			short colixS = i < 5 ? colixA : colixB;
			i += 2;
			int xE = xA + (dx * i) / 10;
			int yE = yA + (dy * i) / 10;
			int zE = zA + (dz * i) / 10;
			short colixE = i < 5 ? colixA : colixB;
			++i;
			if (lineBond)
				g3d.drawLine(colixS, colixE, xS, yS, zS, xE, yE, zE);
			else g3d.fillCylinder(colixS, colixE, Graphics3D.ENDCAPS_FLAT, width, xS, yS, zS, xE, yE, zE);
		}
	}

	// XIE
	private void setColixNearClip() {
		if (zA * 2 < atomA.screenDiameter) {
			colixA = Graphics3D.getTranslucentColix(colixA);
		}
		if (zB * 2 < atomB.screenDiameter) {
			colixB = Graphics3D.getTranslucentColix(colixB);
		}
	}

}