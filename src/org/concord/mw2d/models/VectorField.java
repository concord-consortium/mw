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

import java.awt.Shape;

/**
 * <p>
 * This interface defines a directional field, such as gravitational field, electric field, and so on.
 * 
 * <p>
 * By default, a vector field takes positive value if it points eastwards, southwards and inwards, and negative if
 * pointing to opposite direction.
 * </p>
 * 
 * <p>
 * It has to be pointed out that it is tricky to implement a vector field under periodic boundary conditions. For
 * example, consider a free-falling object. There is a major problem: The object will get accelerated all the time such
 * that its velocity becomes larger and larger and finally exceeds the limit computer algorithms, say the integrator,
 * can handle, or human eyes would comfortably look at.
 * </p>
 * 
 * <p>
 * Leaving alone the fact that everlasting acceleration will sooner or later result in the object'r running faster than
 * the speed of light, something impossible according to the theory of relativity, such a phenomenon is not our interest
 * of simulation. The more likely situation in which you have to use periodic boundary conditions in conjunction with a
 * vector field is about directional flow, which you will often need to add some sort of energy dissipation mechanism to
 * keep the system's speed from infinitely increasing in order to establish steady flow.
 * </p>
 * 
 * @see org.concord.mw2d.models.Boundary
 */

public interface VectorField {

	public final static short NORTH = 3001;
	public final static short EAST = 3002;
	public final static short SOUTH = 3003;
	public final static short WEST = 3004;
	public final static short INWARD = 3005;
	public final static short OUTWARD = 3006;

	public double getIntensity();

	public void setIntensity(double intensity);

	public void setLocal(boolean b);

	public boolean isLocal();

	public void setBounds(Shape shape);

}