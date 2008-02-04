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

package org.concord.modeler.draw;

import java.awt.BasicStroke;

public class StrokeFactory extends BasicStroke {

	private static float dashPhase = 0.0f;
	private static float meterLimit = 10.0f;
	private static int endCapStyle = CAP_BUTT;
	private static int joinStyle = JOIN_MITER;

	private StrokeFactory() {
	}

	private static float[] storedDashArray;

	public static BasicStroke createStroke(float width, float[] dash) {
		return new BasicStroke(width, endCapStyle, joinStyle, meterLimit, dash, dashPhase);
	}

	public static BasicStroke changeThickness(BasicStroke stroke, float thickness) {
		BasicStroke newStroke = null;
		if (Math.abs(thickness) < 0.01 && stroke != null) {
			storedDashArray = stroke.getDashArray();
			return null;
		}
		if (Math.abs(thickness) > 0.01) {
			if (stroke == null) {
				newStroke = new BasicStroke(thickness, endCapStyle, joinStyle, meterLimit, storedDashArray, dashPhase);
			}
			else {
				newStroke = new BasicStroke(thickness, endCapStyle, joinStyle, meterLimit, stroke.getDashArray(),
						dashPhase);
			}
		}
		return newStroke;
	}

	public static BasicStroke changeStyle(BasicStroke stroke, float[] dash) {
		BasicStroke newStroke = null;
		if (stroke == null) {
			storedDashArray = dash;
			return null;
		}
		newStroke = new BasicStroke(stroke.getLineWidth(), stroke.getEndCap(), stroke.getLineJoin(), stroke
				.getMiterLimit(), dash, stroke.getDashPhase());
		return newStroke;
	}

}
