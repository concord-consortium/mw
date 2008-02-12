/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
package org.concord.molbio.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * @author Charles Xie
 * 
 */

class CustomBorderLayout extends BorderLayout {

	private Component northComponent;
	private Component southComponent;
	private Component westComponent;
	private Component eastComponent;
	private Component centerComponent;
	private String overlapSide;

	public CustomBorderLayout(int hgap, int vgap) {
		super(hgap, vgap);
	}

	public void setNorthComponent(Component c) {
		northComponent = c;
	}

	public void setSouthComponent(Component c) {
		southComponent = c;
	}

	public void setWestComponent(Component c) {
		westComponent = c;
	}

	public void setEastComponent(Component c) {
		eastComponent = c;
	}

	public void setCenterComponent(Component c) {
		centerComponent = c;
	}

	public void setOverlapableSide(String side) {
		overlapSide = side;
	}

	public void layoutContainer(Container target) {

		synchronized (target.getTreeLock()) {

			Insets insets = target.getInsets();
			int top = insets.top;
			int bottom = target.getHeight() - insets.bottom;
			int left = insets.left;
			int right = target.getWidth() - insets.right;

			if (northComponent != null) {
				northComponent.setSize(right - left, northComponent.getHeight());
				Dimension d = northComponent.getPreferredSize();
				northComponent.setBounds(left, top, right - left, d.height);
				if (overlapSide != NORTH) {
					top += d.height + (getVgap() < 0 ? 0 : getVgap());
				}
				else {
					top += d.height + getVgap();
				}
			}

			if (southComponent != null) {
				southComponent.setSize(right - left, southComponent.getHeight());
				Dimension d = southComponent.getPreferredSize();
				southComponent.setBounds(left, bottom - d.height, right - left, d.height);
				if (overlapSide != SOUTH) {
					bottom -= d.height + (getVgap() < 0 ? 0 : getVgap());
				}
				else {
					bottom -= d.height + getVgap();
				}
			}

			if (eastComponent != null) {
				eastComponent.setSize(eastComponent.getWidth(), bottom - top);
				Dimension d = eastComponent.getPreferredSize();
				eastComponent.setBounds(right - d.width, top, d.width, bottom - top);
				if (overlapSide != EAST) {
					right -= d.width + (getHgap() < 0 ? 0 : getHgap());
				}
				else {
					right -= d.width + getHgap();
				}
			}

			if (westComponent != null) {
				westComponent.setSize(westComponent.getWidth(), bottom - top);
				Dimension d = westComponent.getPreferredSize();
				westComponent.setBounds(left, top, d.width, bottom - top);
				if (overlapSide != WEST) {
					left += d.width + (getHgap() < 0 ? 0 : getHgap());
				}
				else {
					left += d.width + getHgap();
				}
			}

			if (centerComponent != null) {
				centerComponent.setBounds(left, top, right - left, bottom - top);
			}
		}

	}

	public Dimension preferredLayoutSize(Container target) {

		synchronized (target.getTreeLock()) {

			Dimension dim = new Dimension(0, 0);

			if (eastComponent != null) {
				Dimension d = eastComponent.getPreferredSize();
				dim.width += d.width + getHgap();
				dim.height = Math.max(d.height, dim.height);
			}
			if (westComponent != null) {
				Dimension d = westComponent.getPreferredSize();
				dim.width += d.width + getHgap();
				dim.height = Math.max(d.height, dim.height);
			}
			if (centerComponent != null) {
				Dimension d = centerComponent.getPreferredSize();
				dim.width += d.width;
				dim.height = Math.max(d.height, dim.height);
			}
			if (northComponent != null) {
				Dimension d = northComponent.getPreferredSize();
				dim.width = Math.max(d.width, dim.width);
				if (overlapSide != NORTH) {
					dim.height += d.height + (getVgap() < 0 ? 0 : getVgap());
				}
				else {
					dim.height += d.height + getVgap();
				}
			}
			if (southComponent != null) {
				Dimension d = southComponent.getPreferredSize();
				dim.width = Math.max(d.width, dim.width);
				if (overlapSide != SOUTH) {
					dim.height += d.height + (getVgap() < 0 ? 0 : getVgap());
				}
				else {
					dim.height += d.height + getVgap();
				}
			}

			Insets insets = target.getInsets();
			dim.width += insets.left + insets.right;
			dim.height += insets.top + insets.bottom;

			return dim;
		}

	}

	public Dimension minimumLayoutSize(Container target) {

		synchronized (target.getTreeLock()) {

			Dimension dim = new Dimension(0, 0);

			if (eastComponent != null) {
				Dimension d = eastComponent.getMinimumSize();
				dim.width += d.width + getHgap();
				dim.height = Math.max(d.height, dim.height);
			}
			if (westComponent != null) {
				Dimension d = westComponent.getMinimumSize();
				dim.width += d.width + getHgap();
				dim.height = Math.max(d.height, dim.height);
			}
			if (centerComponent != null) {
				Dimension d = centerComponent.getMinimumSize();
				dim.width += d.width;
				dim.height = Math.max(d.height, dim.height);
			}
			if (northComponent != null) {
				Dimension d = northComponent.getMinimumSize();
				dim.width = Math.max(d.width, dim.width);
				if (overlapSide != NORTH) {
					dim.height += d.height + (getVgap() < 0 ? 0 : getVgap());
				}
				else {
					dim.height += d.height + getVgap();
				}
			}
			if (southComponent != null) {
				Dimension d = southComponent.getMinimumSize();
				dim.width = Math.max(d.width, dim.width);
				if (overlapSide != SOUTH) {
					dim.height += d.height + (getVgap() < 0 ? 0 : getVgap());
				}
				else {
					dim.height += d.height + getVgap();
				}
			}

			Insets insets = target.getInsets();
			dim.width += insets.left + insets.right;
			dim.height += insets.top + insets.bottom;

			return dim;

		}

	}

}
