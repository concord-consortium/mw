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

package org.concord.mw3d.models;

public class Crystal {

	public final static byte SIMPLE_CUBIC = 0;
	public final static byte BODY_CENTERED_CUBIC = 1;
	public final static byte FACE_CENTERED_CUBIC = 2;
	public final static byte HCP = 3;
	public final static byte DIAMOND = 4;
	public final static byte NACL = 11;
	public final static byte CSCL = 12;
	public final static byte L12 = 13;
	public final static byte ZNS = 14;
	public final static byte CAF2 = 15;
	public final static byte CATIO3 = 21;

	private Crystal() {
	}

	private static void createFromSeed(byte type, Atom[] seed, int nx, int ny, int nz, float a, float b, float c,
			float alpha, float beta, float gamma, MolecularModel model) {

		int nx2 = nx / 2;
		int ny2 = ny / 2;
		int nz2 = nz / 2;

		float cos = 1.0f;
		float sin = 0.0f;
		if (type == HCP) {
			cos = (float) Math.cos(Math.PI / 6.0);
			sin = (float) Math.sin(Math.PI / 6.0);
		}

		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				for (int k = 0; k < nz; k++) {
					if (seed.length > 1) {
						for (int iref = 0; iref < seed.length; iref++) {
							model.addAtom(seed[iref].getSymbol(), (seed[iref].rx + i - nx2) * a + b * j * sin,
									(seed[iref].ry + j - ny2) * b * cos, (seed[iref].rz + k - nz2) * c, 0, 0, 0,
									seed[iref].charge);
						}
					}
					else {
						model.addAtom(seed[0].getSymbol(), (i - nx2) * a, (j - ny2) * b, (k - nz2) * c, 0, 0, 0,
								seed[0].charge);
					}
				}
			}
		}

	}

	public static void create(byte type, String[] element, int nx, int ny, int nz, float a, float b, float c,
			float alpha, float beta, float gamma, MolecularModel model) {

		Atom[] seed = null;
		byte nSeed;

		/* build unit cell seed */

		switch (type) {

		case SIMPLE_CUBIC:
			nSeed = 1;
			seed = new Atom[nSeed];
			seed[0] = new Atom();
			seed[0].setSymbol(element[0]);
			break;

		case FACE_CENTERED_CUBIC:

			nSeed = 4;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = -0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = -0.5f;
			for (byte i = 0; i < nSeed; i++) {
				seed[i].setSymbol(element[0]);
			}
			break;

		case HCP:
			nSeed = 2;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = (float) Math.sqrt(3.0) / 6.0f;
			seed[1].rz = 0.5f;
			seed[0].setSymbol(element[0]);
			seed[1].setSymbol(element[0]);
			break;

		case BODY_CENTERED_CUBIC:

			nSeed = 2;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.5f;
			for (byte i = 0; i < nSeed; i++) {
				seed[i].setSymbol(element[0]);
			}
			break;

		case DIAMOND:

			nSeed = 8;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = -0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = -0.5f;
			float shift = 0.25f;
			for (byte i = 0; i < 4; i++) {
				seed[i + 4].rx = seed[i].rx + shift;
				seed[i + 4].ry = seed[i].ry + shift;
				seed[i + 4].rz = seed[i].rz + shift;
			}
			for (byte i = 0; i < nSeed; i++) {
				seed[i].setSymbol(element[0]);
			}
			break;

		case L12:

			nSeed = 4;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = -0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = -0.5f;
			seed[0].setSymbol(element[0]);
			seed[1].setSymbol(element[1]);
			seed[2].setSymbol(element[1]);
			seed[3].setSymbol(element[1]);
			break;

		case CSCL:

			nSeed = 2;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.5f;
			seed[0].setSymbol(element[0]);
			seed[1].setSymbol(element[1]);
			seed[0].charge = 1.0f;
			seed[1].charge = -1.0f;
			break;

		case NACL:

			nSeed = 8;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();

			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = 0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = 0.5f;

			seed[4].rx = 0.5f;
			seed[4].ry = 0.5f;
			seed[4].rz = 0.5f;
			seed[5].rx = 0.0f;
			seed[5].ry = 0.0f;
			seed[5].rz = 0.5f;
			seed[6].rx = 0.5f;
			seed[6].ry = 0.0f;
			seed[6].rz = 0.0f;
			seed[7].rx = 0.0f;
			seed[7].ry = 0.5f;
			seed[7].rz = 0.0f;

			for (byte i = 0; i < 4; i++) {
				seed[i].setSymbol(element[0]);
				seed[i].charge = 1.0f;
			}
			for (byte i = 4; i < 8; i++) {
				seed[i].setSymbol(element[1]);
				seed[i].charge = -1.0f;
			}
			break;

		case ZNS:

			nSeed = 8;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = -0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = -0.5f;
			seed[4].rx = 0.25f;
			seed[4].ry = 0.25f;
			seed[4].rz = -0.25f;
			seed[5].rx = 0.75f;
			seed[5].ry = 0.75f;
			seed[5].rz = -0.25f;
			seed[6].rx = 0.25f;
			seed[6].ry = 0.75f;
			seed[6].rz = -0.75f;
			seed[7].rx = 0.75f;
			seed[7].ry = 0.75f;
			seed[7].rz = -0.75f;
			for (byte i = 0; i < 4; i++) {
				seed[i].setSymbol(element[0]);
				seed[i + 4].setSymbol(element[1]);
			}
			break;

		case CAF2:

			nSeed = 12;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = -0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = -0.5f;
			seed[4].rx = 0.25f;
			seed[4].ry = 0.25f;
			seed[4].rz = -0.25f;
			seed[5].rx = 0.75f;
			seed[5].ry = 0.75f;
			seed[5].rz = -0.25f;
			seed[6].rx = 0.25f;
			seed[6].ry = 0.75f;
			seed[6].rz = -0.25f;
			seed[7].rx = 0.75f;
			seed[7].ry = 0.25f;
			seed[7].rz = -0.25f;
			seed[8].rx = 0.25f;
			seed[8].ry = 0.75f;
			seed[8].rz = -0.75f;
			seed[9].rx = 0.75f;
			seed[9].ry = 0.25f;
			seed[9].rz = -0.75f;
			seed[10].rx = 0.25f;
			seed[10].ry = 0.25f;
			seed[10].rz = -0.75f;
			seed[11].rx = 0.75f;
			seed[11].ry = 0.75f;
			seed[11].rz = -0.75f;
			for (byte i = 0; i < 4; i++) {
				seed[i].setSymbol(element[0]);
				seed[i + 4].setSymbol(element[1]);
				seed[i + 8].setSymbol(element[1]);
			}
			break;

		case CATIO3:

			nSeed = 5;
			seed = new Atom[nSeed];
			for (byte i = 0; i < nSeed; i++)
				seed[i] = new Atom();
			seed[0].rx = 0.0f;
			seed[0].ry = 0.0f;
			seed[0].rz = 0.0f;
			seed[1].rx = 0.5f;
			seed[1].ry = 0.5f;
			seed[1].rz = 0.0f;
			seed[2].rx = 0.0f;
			seed[2].ry = 0.5f;
			seed[2].rz = -0.5f;
			seed[3].rx = 0.5f;
			seed[3].ry = 0.0f;
			seed[3].rz = -0.5f;
			seed[4].rx = 0.5f;
			seed[4].ry = 0.5f;
			seed[4].rz = -0.5f;
			seed[0].setSymbol(element[0]);
			seed[1].setSymbol(element[1]);
			seed[2].setSymbol(element[1]);
			seed[3].setSymbol(element[1]);
			seed[4].setSymbol(element[2]);
			break;
		}

		if (seed != null)
			createFromSeed(type, seed, nx, ny, nz, a, b, c, alpha, beta, gamma, model);

	}

}