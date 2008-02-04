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

public interface ModelComponent {

	public double getRx();

	public double getRy();

	public void setSelected(boolean b);

	public boolean isSelected();

	public void setMarked(boolean b);

	public boolean isMarked();

	public void setVisible(boolean b);

	public boolean isVisible();

	public void setBlinking(boolean b);

	public boolean isBlinking();

	/** blink this component to attract attention */
	public void blink();

	/** return true if this component contains the specified coordinates */
	public boolean contains(double x, double y);

	/**
	 * set the model this component is placed into. This is critically important, because a <code>ModelComponent</code>
	 * usually needs to know its <code>MDModel</code> environment in order to function upon requests or respond to
	 * changes.
	 */
	public void setModel(MDModel model);

	/**
	 * this method is not called <code>getModel()</code> to avoid auto-serialization in the classes that implement
	 * this interface
	 */
	public MDModel getHostModel();

	/**
	 * store the state of this component for uses like undoing. Implementer of this interface should store as much
	 * information as possible so that this component's state can be adequately retrieved from the stored information.
	 */
	public void storeCurrentState();

	/**
	 * restore this component's state from the information stored by calling <code>storeCurrentState()</code>.
	 */
	public void restoreState();

	/** destroy this object to allow garbage collection, e.g. removing dependencies. */
	public void destroy();

}