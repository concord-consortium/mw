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

/**
 * @author Charles Xie
 * 
 */
public final class StyleConstant {

	public final static byte RESTRAINT_CROSS_STYLE = 0x15;
	public final static byte RESTRAINT_HEAVY_SPRING_STYLE = 0x16;
	public final static byte RESTRAINT_LIGHT_SPRING_STYLE = 0x17;
	public final static byte RESTRAINT_GHOST_STYLE = 0x18;

	public final static byte TRAJECTORY_LINE_STYLE = 0x31;
	public final static byte TRAJECTORY_DOTTEDLINE_STYLE = 0x32;
	public final static byte TRAJECTORY_CIRCLES_STYLE = 0x33;

	final static byte SPACE_FILLING = 0x01;
	final static byte BALL_AND_STICK = 0x02;
	final static byte WIRE_FRAME = 0x03;
	final static byte STICK = 0x04;
	final static byte DELAUNAY = 0x21;
	final static byte VORONOI = 0x22;
	final static byte DELAUNAY_AND_VORONOI = 0x23;

	final static byte VDW_DOTTED_CIRCLE = 0x51;
	final static byte VDW_RADIAL_COLOR_GRADIENT = 0x52;

	private StyleConstant() {
	}

}
