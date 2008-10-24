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

public class DefaultTextContainer extends TextContainer {

	public DefaultTextContainer() {
		super("Your text.");
	}

	public DefaultTextContainer(String text, float x, float y) {
		super(text, x, y);
	}

	public DefaultTextContainer(TextContainer c) {
		super(c);
	}

	public DefaultTextContainer(TextContainerState s) {
		super(s);
	}

	/** This is a stub method (no implementation at this class level) */
	protected void attachToHost() {
	}

	protected void setVisible(boolean b) {
	}

	protected boolean isVisible() {
		return true;
	}

}