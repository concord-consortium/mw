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

import java.awt.Dimension;

/** Generates a self-avoid random walk on a lattice using a Markov Chain Monte Carlo method. */

class WalkGenerator {

	private Dimension dimension;

	public WalkGenerator() {
	}

	public WalkGenerator(Dimension dim) {
		setDimension(dim);
	}

	/** generate a self-avoid random walk with the given length */
	public Walk generate(short step) {

		Walk walk = new Walk(step);

		double ran;
		int count = 0;
		byte k = -1;
		boolean forwardTried = false, leftTried = false, rightTried = false;

		for (short i = 0; i < step; i++) {

			do {
				ran = Math.random();
				if (k == -1) {
					/* if no move is rejected at this node yet */
					if (ran < 0.333) {
						k = Walk.FORWARD;
					}
					else if (ran >= 0.333 && ran < 0.667) {
						k = Walk.LEFT;
					}
					else {
						k = Walk.RIGHT;
					}
					walk.setStep(i, k);
					count++;
				}
				else {
					/*
					 * if moving towards a certain direction has been rejected, we only try the other two directions
					 */
					switch (k) {
					case Walk.FORWARD:
						forwardTried = true;
						break;
					case Walk.LEFT:
						leftTried = true;
						break;
					case Walk.RIGHT:
						rightTried = true;
						break;
					}
					if (forwardTried && leftTried && rightTried)
						return null;
					if (!forwardTried) {
						k = Walk.FORWARD;
						walk.setStep(i, k);
					}
					else if (!leftTried) {
						k = Walk.LEFT;
						walk.setStep(i, k);
					}
					else if (!rightTried) {
						k = Walk.RIGHT;
						walk.setStep(i, k);
					}
					count++;
				}
			} while ((walk.isSelfIntersected(i) || walk.isOutOfBound(i, dimension)) && count < 10 * step);

			if (count == 10 * step)
				return null;

			k = -1;
			count = 0;
			forwardTried = leftTried = rightTried = false;

		}

		// if(dimension!=null) centeralize(walk);

		return walk;

	}

	/*
	 * private void centeralize(Walk walk){ int n=walk.getLength(); float xc=0.0f, yc=0.0f; short[] x=walk.getXArray();
	 * short[] y=walk.getYArray(); for(short i=0; i<n; i++){ xc+=x[i]; yc+=y[i]; } xc/=n; yc/=n; short
	 * a=(short)(xc-dimension.width/2); short b=(short)(yc-dimension.height/2); for(short i=0; i<n; i++){ x[i]-=a;
	 * y[i]-=b; } walk.setOrigin((short)(walk.getOriginX()-a), (short)(walk.getOriginY()-b));
	 * walk.setFirstNode((short)(walk.getFirstNodeX()-a), (short)(walk.getFirstNodeY()-b)); }
	 */

	public void setDimension(Dimension dim) {
		if (dimension == null) {
			dimension = new Dimension(dim);
		}
		else {
			dimension.width = dim.width;
			dimension.height = dim.height;
		}
	}

	public Dimension getDimension() {
		return dimension;
	}

}