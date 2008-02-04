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

/**
 * An obstacle represents a zone where no atom or molecule can enter. While the geometric properties of an obstacle are
 * defined through the <tt>Shape</tt> interface, its physical properties such as charge distribution, possible
 * mechanism of motion, and so on, should be defined through this interface.
 * 
 * @author Charles Xie
 */

public interface Obstacle extends ModelComponent {

	public double getMinX();

	public double getMinY();

	public double getMaxX();

	public double getMaxY();

	public double getDensity();

	public void setDensity(double m);

}