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

package org.concord.modeler;

/**
 * Engines are the components that other components depend upon. For example, a molecular dynamics model is an engine,
 * because it can have other components such as a temperature slider that depends on it. The slider, however, is not an
 * engine, because no component depends on it. In other words, an engine does not have to know the existence of a
 * temperature slider when it is created, but a temperature slider must know which model it will control when it is
 * created. The authoring system should not allow a slider without a controling object.
 * 
 * Engines MUST be reused.
 * 
 * @see org.concord.modeler.EnginePool
 * @see org.concord.modeler.ModelCanvas
 * @see org.concord.modeler.PageMolecularViewer
 * @see org.concord.modeler.PageMd3d
 */

public interface Engine {

	/** stop whatever job the engine is currently doing, typically with a thread. */
	public void stopImmediately();

	/** reset the engine to the default state. */
	public void reset();

	/** check if the view of this engine is showing */
	public boolean isShowing();

}