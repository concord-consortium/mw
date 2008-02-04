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

package org.concord.modeler.util;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.awt.print.Paper;

public class PrinterUtilities extends Object {

	private PrinterUtilities() {
	}

	/**
	 * @param orientation
	 *            the orientation of the paper
	 * @param paper
	 *            a piece of paper
	 * @param d
	 *            the actual dimension of the document to be printed
	 * @return the rescaled dimension that fits the imageable size of the paper but keeps the ratio of width to length
	 *         of the input dimension object
	 */
	public static Dimension scaleToPaper(int orientation, Paper paper, Dimension d) {

		float wp = (float) paper.getImageableWidth();
		float hp = (float) paper.getImageableHeight();
		float w = d.width;
		float h = d.height;

		int w1 = (int) w, h1 = (int) h;

		float rw = 1.0f;
		float rh = 1.0f;

		if (orientation == PageFormat.PORTRAIT) {
			rw = w / wp;
			rh = h / hp;
		}
		else if (orientation == PageFormat.LANDSCAPE || orientation == PageFormat.REVERSE_LANDSCAPE) {
			rw = w / hp;
			rh = h / wp;
		}

		boolean out = false;

		if (rw > 1.0f && rh > 1.0f) {
			out = true;
		}
		else if (rw > 1.0f && rh < 1.0f) {
			out = true;
		}
		else if (rw < 1.0f && rh > 1.0f) {
			out = true;
		}

		if (out) {
			float rmax = 1.0f / Math.max(rw, rh);
			w1 = (int) (w * rmax);
			h1 = (int) (h * rmax);
		}

		return new Dimension(w1, h1);

	}

}