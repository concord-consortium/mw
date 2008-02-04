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

import java.awt.Shape;
import java.awt.geom.GeneralPath;

/** give general path a name to identify */

public class NamedGeneralPath {

	private String name;
	private GeneralPath path;

	public NamedGeneralPath(String name) {
		path = new GeneralPath();
		this.name = name;
	}

	public NamedGeneralPath(int rule, String name) {
		path = new GeneralPath(rule);
		this.name = name;
	}

	public NamedGeneralPath(int rule, int initialCapacity, String name) {
		path = new GeneralPath(rule, initialCapacity);
		this.name = name;
	}

	public NamedGeneralPath(Shape shape, String name) {
		path = new GeneralPath(shape);
		this.name = name;
	}

	public GeneralPath getGeneralPath() {
		return path;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
