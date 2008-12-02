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

final class UndoAction {

	private UndoAction() {
	}

	final static short INSERT_A_PARTICLE = 4001;
	final static short INSERT_A_MOLECULE = 4002;
	final static short REMOVE_A_PARTICLE = 4003;
	final static short REMOVE_A_MOLECULE = 4004;
	final static short BLOCK_REMOVE = 4005;
	final static short REMOVE_RADIAL_BOND = 4006;
	final static short REMOVE_ANGULAR_BOND = 4007;
	final static short REMOVE_OBSTACLE = 4008;
	final static short FILL_AREA_WITH_PARTICLES = 4010;

	final static short INSERT_LAYERED_COMPONENT = 4010;
	final static short REMOVE_LAYERED_COMPONENT = 4011;
	final static short SEND_BACK_LAYERED_COMPONENT = 4012;
	final static short BRING_FORWARD_LAYERED_COMPONENT = 4013;
	final static short FRONT_LAYERED_COMPONENT = 4014;
	final static short BACK_LAYERED_COMPONENT = 4015;
	final static short ATTACH_LAYERED_COMPONENT = 4016;
	final static short DETACH_LAYERED_COMPONENT = 4017;

	final static short TRANSLATE_MODEL = 4018;
	final static short ROTATE_MODEL = 4019;

	final static short RESIZE_LINE = 4020;
	final static short RESIZE_RECTANGLE = 4021;
	final static short RESIZE_ELLIPSE = 4022;
	final static short RESIZE_TRIANGLE = 4023;

}