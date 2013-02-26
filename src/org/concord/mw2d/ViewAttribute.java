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

package org.concord.mw2d;

import java.awt.BasicStroke;
import java.awt.Font;
import java.text.DecimalFormat;

public final class ViewAttribute {

	public final static Font LITTLE_FONT = new Font(null, Font.PLAIN, 9);

	public final static Font SMALL_FONT = new Font(null, Font.PLAIN, 10);

	public final static Font FONT_BOLD_15 = new Font(null, Font.BOLD, 15);

	public final static Font FONT_BOLD_18 = new Font(null, Font.BOLD, 18);

	final static DecimalFormat SCIENTIFIC_FORMAT = new DecimalFormat();

	final static DecimalFormat ANGSTROM_FORMAT = new DecimalFormat("#.#");

	public final static BasicStroke THICK = new BasicStroke(5.0f);

	public final static BasicStroke MODERATE = new BasicStroke(2.0f);

	public final static BasicStroke THIN = new BasicStroke(1.0f);

	public final static BasicStroke THIN_DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 2.0f }, 0.0f);

	public final static BasicStroke THIN_DOTTED = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.5f }, 0.0f);

	public final static BasicStroke DASHED = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 2.0f }, 0.0f);

	public final static BasicStroke THICKER_DASHED = new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 2.0f }, 0.0f);

	private ViewAttribute() {
	}

}