/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
 * @author Charles Xie
 * 
 */
public interface FieldArea {

	public void setVectorField(VectorField vectorField);

	public VectorField getVectorField();

	public void setViscosity(float viscosity);

	public float getViscosity();

	public void setPhotonAbsorption(float photonAbsorption);

	public float getPhotonAbsorption();

	public void setElectronAbsorption(float electronAbsorption);

	public float getElectronAbsorption();

	public Shape getBounds();

	public void interact(Particle p);

	public boolean absorb(Photon p);

	public boolean absorb(Electron e);

	public void setReflection(boolean b);

	public boolean getReflection();

}
