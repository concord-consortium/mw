/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
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

package org.concord.mw3d;

import java.awt.Cursor;
import java.awt.Point;

import javax.swing.ImageIcon;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;

public final class UserAction {

	public final static byte DEFA_ID = 0x00;
	public final static byte SLRT_ID = 0x01;
	public final static byte SLOV_ID = 0x02;
	public final static byte SLAT_ID = 0x03;
	public final static byte PANN_ID = 0x04;
	public final static byte RBND_ID = 0x05;
	public final static byte ABND_ID = 0x06;
	public final static byte TBND_ID = 0x07;
	public final static byte PCHG_ID = 0x08;
	public final static byte NCHG_ID = 0x09;
	public final static byte ROTA_ID = 0x0a;
	public final static byte TRAN_ID = 0x0b;
	public final static byte DUPL_ID = 0x0c;

	public final static byte XADD_ID = 0x10;
	public final static byte YADD_ID = 0x11;
	public final static byte ZADD_ID = 0x12;
	public final static byte XMOL_ID = 0x13;
	public final static byte YMOL_ID = 0x14;
	public final static byte ZMOL_ID = 0x15;
	public final static byte XFIL_ID = 0x16;
	public final static byte YFIL_ID = 0x17;
	public final static byte ZFIL_ID = 0x18;
	public final static byte XREC_ID = 0x19;
	public final static byte YREC_ID = 0x1a;
	public final static byte ZREC_ID = 0x1b;
	public final static byte XOVL_ID = 0x1c;
	public final static byte YOVL_ID = 0x1d;
	public final static byte ZOVL_ID = 0x1e;

	public final static byte DELR_ID = 0x30;
	public final static byte DELC_ID = 0x31;
	public final static byte FIXR_ID = 0x32;
	public final static byte FIXC_ID = 0x33;
	public final static byte XIFR_ID = 0x34;
	public final static byte XIFC_ID = 0x35;
	public final static byte TSLC_ID = 0x36;
	public final static byte CLST_ID = 0x37;
	public final static byte HIDE_ID = 0x38;
	public final static byte EDIH_ID = 0x39;

	public final static byte SBOX_ID = 0x40;
	public final static byte VVEL_ID = 0x41;
	public final static byte EXOB_ID = 0x42;

	public final static byte DRAW_ID = 0x50;

	private static Cursor extrusionCursor, radialBondCursor, angularBondCursor, torsionBondCursor,
			rotateMoleculeCursor, translateMoleculeCursor, duplicateMoleculeCursor, chargePositiveCursor,
			chargeNegativeCursor;

	static Cursor getExtrusionCursor() {
		if (extrusionCursor == null)
			extrusionCursor = ModelerUtilities.createCursor(
					UserAction.class.getResource("resources/ExtrudeCursor.gif"), new Point(16, 16), "extrusion");
		return extrusionCursor;
	}

	private static Cursor getRadialBondCursor() {
		if (radialBondCursor == null)
			radialBondCursor = ModelerUtilities.createCursor((ImageIcon) IconPool.getIcon("radial bond cursor"),
					new Point(), "radial bond");
		return radialBondCursor;
	}

	private static Cursor getAngularBondCursor() {
		if (angularBondCursor == null)
			angularBondCursor = ModelerUtilities.createCursor((ImageIcon) IconPool.getIcon("angular bond cursor"),
					new Point(), "angular bond");
		return angularBondCursor;
	}

	private static Cursor getTorsionBondCursor() {
		if (torsionBondCursor == null)
			torsionBondCursor = ModelerUtilities.createCursor(new ImageIcon(MolecularContainer.class
					.getResource("resources/torsionbondcursor.gif")), new Point(), "torsion bond");
		return torsionBondCursor;
	}

	private static Cursor getRotateMoleculeCursor() {
		if (rotateMoleculeCursor == null)
			rotateMoleculeCursor = ModelerUtilities.createCursor(new ImageIcon(MolecularContainer.class
					.getResource("resources/RotateMoleculeCursor.gif")), new Point(20, 11), "rotate molecule");
		return rotateMoleculeCursor;
	}

	private static Cursor getTranslateMoleculeCursor() {
		if (translateMoleculeCursor == null)
			translateMoleculeCursor = ModelerUtilities.createCursor(new ImageIcon(MolecularContainer.class
					.getResource("resources/TranslateMoleculeCursor.gif")), new Point(20, 11), "translate molecule");
		return translateMoleculeCursor;
	}

	private static Cursor getDuplicateMoleculeCursor() {
		if (duplicateMoleculeCursor == null)
			duplicateMoleculeCursor = ModelerUtilities.createCursor(new ImageIcon(MolecularContainer.class
					.getResource("resources/DuplicateMoleculeCursor.gif")), new Point(20, 11), "duplicate molecule");
		return duplicateMoleculeCursor;
	}

	private static Cursor getChargePositiveCursor() {
		if (chargePositiveCursor == null)
			chargePositiveCursor = ModelerUtilities.createCursor(new ImageIcon(MolecularContainer.class
					.getResource("resources/ChargePositiveCursor.gif")), new Point(), "charge positively");
		return chargePositiveCursor;
	}

	private static Cursor getChargeNegativeCursor() {
		if (chargeNegativeCursor == null)
			chargeNegativeCursor = ModelerUtilities.createCursor(new ImageIcon(MolecularContainer.class
					.getResource("resources/ChargeNegativeCursor.gif")), new Point(), "charge negatively");
		return chargeNegativeCursor;
	}

	static Cursor getCursor(byte id) {
		switch (id) {
		case DELR_ID:
		case DELC_ID:
		case FIXR_ID:
		case FIXC_ID:
		case XIFR_ID:
		case XIFC_ID:
		case TSLC_ID:
		case CLST_ID:
		case HIDE_ID:
		case EDIH_ID:
			return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
		case PANN_ID:
			return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		case SBOX_ID:
		case EXOB_ID:
			return getExtrusionCursor();
		case RBND_ID:
			return getRadialBondCursor();
		case ABND_ID:
			return getAngularBondCursor();
		case TBND_ID:
			return getTorsionBondCursor();
		case ROTA_ID:
			return getRotateMoleculeCursor();
		case TRAN_ID:
			return getTranslateMoleculeCursor();
		case DUPL_ID:
			return getDuplicateMoleculeCursor();
		case PCHG_ID:
			return getChargePositiveCursor();
		case NCHG_ID:
			return getChargeNegativeCursor();
		default:
			return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		}

	}

}