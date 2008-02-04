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

package org.concord.modeler;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import static org.concord.modeler.ui.BorderRectangle.*;

class BorderManager {

	final static String[] BORDER_TYPE = new String[] { EMPTY_BORDER, RAISED_BEVEL_BORDER, LOWERED_BEVEL_BORDER,
			RAISED_ETCHED_BORDER, LOWERED_ETCHED_BORDER, LINE_BORDER, MATTE_BORDER };

	private BorderManager() {
	}

	static void setBorder(JComponent c, String type, Color bgColor) {

		if (c.getBorder() instanceof CompoundBorder) {

			Border border = ((CompoundBorder) c.getBorder()).getInsideBorder();
			if (type != null) {
				if (type.equals(RAISED_BEVEL_BORDER)) {
					c.setBorder(new CompoundBorder(BorderFactory.createRaisedBevelBorder(), border));
				}
				else if (type.equals(LOWERED_BEVEL_BORDER)) {
					c.setBorder(new CompoundBorder(BorderFactory.createLoweredBevelBorder(), border));
				}
				else if (type.equals(RAISED_ETCHED_BORDER)) {
					c.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), border));
				}
				else if (type.equals(LOWERED_ETCHED_BORDER)) {
					c.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), border));
				}
				else if (type.equals(LINE_BORDER)) {
					c.setBorder(new CompoundBorder(BorderFactory
							.createLineBorder(new Color(0xffffff ^ bgColor.getRGB())), border));
				}
				else if (type.equals(MATTE_BORDER)) {
					c.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, TILE_ICON), border));
				}
				else {
					c.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(), border));
				}
			}
			else {
				c.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(), border));
			}

		}

		else {
			if (type != null) {
				if (type.equals(RAISED_BEVEL_BORDER)) {
					c.setBorder(BorderFactory.createRaisedBevelBorder());
				}
				else if (type.equals(LOWERED_BEVEL_BORDER)) {
					c.setBorder(BorderFactory.createLoweredBevelBorder());
				}
				else if (type.equals(RAISED_ETCHED_BORDER)) {
					c.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				}
				else if (type.equals(LOWERED_ETCHED_BORDER)) {
					c.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				}
				else if (type.equals(LINE_BORDER)) {
					c.setBorder(BorderFactory.createLineBorder(new Color(0xffffff ^ bgColor.getRGB())));
				}
				else if (type.equals(MATTE_BORDER)) {
					c.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, TILE_ICON));
				}
				else {
					c.setBorder(BorderFactory.createEmptyBorder());
				}
			}
			else {
				c.setBorder(BorderFactory.createEmptyBorder());
			}
		}

	}

	static String getBorder(JComponent c) {
		return getBorder(c.getBorder());
	}

	private static String getBorder(Border border) {
		if (border instanceof TitledBorder) {
			return getBorder(((TitledBorder) border).getBorder());
		}
		if (border instanceof BevelBorder) {
			if (((BevelBorder) border).getBevelType() == BevelBorder.RAISED)
				return RAISED_BEVEL_BORDER;
			return LOWERED_BEVEL_BORDER;
		}
		if (border instanceof EtchedBorder) {
			if (((EtchedBorder) border).getEtchType() == EtchedBorder.RAISED)
				return RAISED_ETCHED_BORDER;
			return LOWERED_ETCHED_BORDER;
		}
		if (border instanceof LineBorder)
			return LINE_BORDER;
		if (border instanceof MatteBorder)
			return MATTE_BORDER;
		if (border instanceof CompoundBorder) {
			Border outside = ((CompoundBorder) border).getOutsideBorder();
			if (outside instanceof BevelBorder) {
				if (((BevelBorder) outside).getBevelType() == BevelBorder.RAISED)
					return RAISED_BEVEL_BORDER;
				return LOWERED_BEVEL_BORDER;
			}
			if (outside instanceof EtchedBorder) {
				if (((EtchedBorder) outside).getEtchType() == EtchedBorder.RAISED)
					return RAISED_ETCHED_BORDER;
				return LOWERED_ETCHED_BORDER;
			}
			if (outside instanceof LineBorder)
				return LINE_BORDER;
			if (outside instanceof MatteBorder)
				return MATTE_BORDER;
		}
		return EMPTY_BORDER;
	}

}