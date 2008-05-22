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

import javax.swing.JPopupMenu;

import org.concord.modeler.text.Page;

/**
 * This interface defines a type of objects that can be embedded into a <text>Page</text>.
 * 
 * @see org.concord.modeler.text.Page
 */

public interface Embeddable {

	public void setChangable(boolean b);

	public boolean isChangable();

	public void setMarked(boolean b);

	public boolean isMarked();

	/**
	 * An index of an embeddable component is used to differentiate the instances of the same type of component of a
	 * page, for the purpose of keeping track of these components. This method should be used to set the index.
	 */
	public void setIndex(int i);

	/**
	 * An index of an embeddable component is used to differentiate the instances of the same type of component of a
	 * page, for the purpose of keeping track of these components.
	 * 
	 * @return the index of this embeddable component of the page
	 */
	public int getIndex();

	/**
	 * destroy to allow garbage collection. This is usually called when the user leaves the current page, which,
	 * however, causes the cut/copy/paste of an embeddable component to fail (because of the loss of content, e.g. the
	 * ActionListeners and so on).
	 */
	public void destroy();

	public void createPopupMenu();

	public JPopupMenu getPopupMenu();

	public void setPage(Page page);

	public Page getPage();

	/** set a unique ID for this embedded object */
	public void setId(String id);

	/** get the unique ID for this embedded object */
	public String getId();

}