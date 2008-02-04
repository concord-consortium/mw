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
 * <tt>Boundary</tt> is an interface defining the area where and how particels move. <tt>Boundary</tt> is <i>not</i>
 * the scope of <tt>View</tt>.
 * </p>
 * 
 * <p>
 * In terms of changing the direction of velocities, boundaries can be classified into two types: reflecting and
 * periodic. The former bounces off particles, the latter lets particles go through.
 * </p>
 * 
 * <p>
 * Reflecting boundaries can be elastic, inelastic or superelastic, which mean losing no energy, losing some energy, or
 * gaining some energy, respectively. Reflecting boundaries can have a Langevin layer in which an atom will feel a
 * friction force which is propotional to its speed. Reflecting boundaries can be given a potential (like mirror image
 * potential in electrostatics) through which they could (selectively) interact with certain type atoms. In such a case,
 * a boundary becomes a physical substrate. All these physical properties are encapsulated in a field object called
 * <tt>Walls</tt>.
 * </p>
 * 
 * <p>
 * Periodic boundaries rarely need to behave like a physical body. Periodic boundaries do not allow elasticity nor
 * inelasticity. They are useful in doing molecular-dynamics-based fluid dynamics.
 * </p>
 * 
 * <p>
 * The interaction of a boundary with a Gay-Berne particle depends on how the particle moves. If the particle is driven
 * by the engine, its motion may contain translational and rotational components. If the particle is controlled by the
 * user's finger, the motion can be either purely rotational or purely translational. In the second case, you have to
 * specify the type of motion when calling the corresponding method.
 * </p>
 * 
 * @author Charles Xie
 * @see org.concord.mw2d.MDView
 * @see org.concord.mw2d.models.Wall
 * @see org.concord.mw2d.models.GayBerneParticle
 */

public interface Boundary extends ModelComponent {

	/** DBC stands for default boundary conditions */
	public final static short DBC_ID = 5561;

	/** RBC stands for reflecting boundary conditions */
	public final static short RBC_ID = 5562;

	/** PBC stands for periodic boundary conditions */
	public final static short PBC_ID = 5563;

	/** the type of motion of particle when hitting the boundary is translation. */
	public final static short TRANSLATION = 111;

	/** the type of motion of particle when hitting the boundary is rotation. */
	public final static short ROTATION = 112;

	/** set the shape of view of the model to which this boundary is attached. */
	public void setView(Shape s);

	/** get the shape of view of the model to which this boundary is attached. */
	public Shape getView();

}