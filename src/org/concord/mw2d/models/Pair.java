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

package org.concord.mw2d.models;

import java.io.Serializable;

/**
 * This class represents a pair of elements. It is used by the <tt>Affinity</tt> class. Do NOT use this class.
 * 
 * @author Qian Xie
 */

public class Pair implements Serializable {

	private Element e1, e2;

	public Pair() {
	}

	public Pair(Element e1, Element e2) {
		this();
		this.e1 = e1;
		this.e2 = e2;
	}

	public void setElement1(Element e) {
		e1 = e;
	}

	public Element getElement1() {
		return e1;
	}

	public void setElement2(Element e) {
		e2 = e;
	}

	public Element getElement2() {
		return e2;
	}

	public int hashCode() {
		return e1.hashCode() ^ e2.hashCode();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Pair))
			return false;
		return (((Pair) obj).e1.equals(e1) && ((Pair) obj).e2.equals(e2))
				|| (((Pair) obj).e1.equals(e2) && ((Pair) obj).e2.equals(e1));
	}

	public String toString() {
		return "[" + e1.getID() + "," + e2.getID() + "]";
	}

}