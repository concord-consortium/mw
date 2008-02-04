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

/**
 * @author Charles Xie
 * 
 */
public class DrawingElementStateFactory {

	private DrawingElementStateFactory() {
	}

	public static Object createState(DrawingElement e) {
		if (e instanceof AbstractRectangle) {
			return new RectangleState((AbstractRectangle) e);
		}
		if (e instanceof AbstractEllipse) {
			return new EllipseState((AbstractEllipse) e);
		}
		if (e instanceof AbstractLine) {
			return new LineState((AbstractLine) e);
		}
		if (e instanceof TextContainer) {
			return new TextContainerState((TextContainer) e);
		}
		return null;
	}

	public static DrawingElement createElement(Object state) {
		if (state instanceof RectangleState) {
			return new DefaultRectangle((RectangleState) state);
		}
		if (state instanceof EllipseState) {
			return new DefaultEllipse((EllipseState) state);
		}
		if (state instanceof LineState) {
			return new DefaultLine((LineState) state);
		}
		if (state instanceof TextContainerState) {
			return new DefaultTextContainer((TextContainerState) state);
		}
		return null;
	}

}