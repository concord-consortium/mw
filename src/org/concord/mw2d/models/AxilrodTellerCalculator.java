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

package org.concord.mw2d.models;

// Axilrod-Teller three-body potential

class AxilrodTellerCalculator {

	private float threeBodyForceConstant = 1000.0f;
	private float threeBodyCutOff = 50.0f;
	private float threeBodyCutOffSquare = 2500.0f;

	private double dijx, dijy, dij;
	private double dikx, diky, dik;
	private double djkx, djky, djk;
	private double dikdjk, dikdij, dijdjk;
	private double prefactor1, prefactor2;
	private double etemp, vsum;
	private Atom[] atom;
	private int numberOfAtoms;

	AxilrodTellerCalculator(AtomicModel model) {
	}

	void setThreeBodyCutOff(int cutoff) {
		if (cutoff <= 0)
			throw new IllegalArgumentException("cutoff<=0");
		threeBodyCutOff = cutoff;
		threeBodyCutOffSquare = threeBodyCutOff * threeBodyCutOff;
	}

	double compute() {

		vsum = 0;

		for (int i = 0; i < numberOfAtoms - 2; i++) {

			for (int j = i + 1; j < numberOfAtoms - 1; j++) {

				dijx = atom[i].rx - atom[j].rx;
				dijy = atom[i].ry - atom[j].ry;
				dij = dijx * dijx + dijy * dijy;

				if (dij < threeBodyCutOffSquare) {

					for (int k = j + 1; k < numberOfAtoms; k++) {

						dikx = atom[i].rx - atom[k].rx;
						diky = atom[i].ry - atom[k].ry;
						dik = dikx * dikx + diky * diky;

						if (dik < threeBodyCutOffSquare) {

							djkx = atom[j].rx - atom[k].rx;
							djky = atom[j].ry - atom[k].ry;
							djk = djkx * djkx + djky * djky;

							if (djk < threeBodyCutOffSquare) {

								dij = Math.sqrt(dij);
								dik = Math.sqrt(dik);
								djk = Math.sqrt(djk);

								dikdjk = dikx * djkx + diky * djky;
								dikdij = dikx * dijx + diky * dijy;
								dijdjk = dijx * djkx + dijy * djky;

								prefactor1 = threeBodyForceConstant
										* 2.0
										* MDModel.GF_CONVERSION_CONSTANT
										/ (dij * dij * dij * dij * dij * dik * dik * dik * dik * dik * djk * djk * djk
												* djk * djk);
								prefactor2 = 5.0 * (dij * dij * djk * djk * dik * dik - 3.0 * dikdjk * dikdij * dijdjk);

								atom[i].fx += prefactor1
										/ atom[i].mass
										* (prefactor2 * (dijx / (dij * dij) + dikx / (dik * dik)) + 3.0
												* (dijdjk * dikdij + dikdij * dikdjk) * djkx + 3.0 * dijdjk * dikdjk
												* (dijx + dikx) - 2.0 * (djk * djk * dik * dik * dijx + djk * djk * dij
												* dij * dikx));

								atom[i].fy += prefactor1
										/ atom[i].mass
										* (prefactor2 * (dijy / (dij * dij) + diky / (dik * dik)) + 3.0
												* (dijdjk * dikdij + dikdij * dikdjk) * djky + 3.0 * dijdjk * dikdjk
												* (dijy + diky) - 2.0 * (djk * djk * dik * dik * dijy + djk * djk * dij
												* dij * diky));

								atom[j].fx += prefactor1
										/ atom[j].mass
										* (prefactor2 * (djkx / (djk * djk) - dijx / (dij * dij)) + 3.0
												* (dijdjk * dikdij - dikdjk * dijdjk) * dikx + 3.0 * dikdjk * dikdij
												* (dijx - djkx) - 2.0 * (dij * dij * dik * dik * djkx - djk * djk * dik
												* dik * dijx));

								atom[j].fy += prefactor1
										/ atom[j].mass
										* (prefactor2 * (djky / (djk * djk) - dijy / (dij * dij)) + 3.0
												* (dijdjk * dikdij - dikdjk * dijdjk) * diky + 3.0 * dikdjk * dikdij
												* (dijy - djky) - 2.0 * (dij * dij * dik * dik * djky + djk * djk * dik
												* dik * dijy));

								atom[k].fx += prefactor1
										/ atom[k].mass
										* (prefactor2 * (-dikx / (dik * dik) - djkx / (djk * djk)) - 3.0
												* (dikdjk * dikdij + dikdjk * dijdjk) * dijx - 3.0 * dikdij * dijdjk
												* (djkx + dikx) + 2.0 * (dij * dij * dik * dik * djkx + dij * dij * djk
												* djk * dikx));

								atom[k].fy += prefactor1
										/ atom[k].mass
										* (prefactor2 * (-diky / (dik * dik) - djky / (djk * djk)) - 3.0
												* (dikdjk * dikdij + dikdjk * dijdjk) * dijy - 3.0 * dikdij * dijdjk
												* (djky + diky) + 2.0 * (dij * dij * dik * dik * djky + dij * dij * djk
												* djk * diky));

								etemp = 0.2 * prefactor1 * prefactor2 / MDModel.GF_CONVERSION_CONSTANT;
								vsum += etemp;

							}

						}

					}

				}

			}

		}

		return vsum;

	}

}