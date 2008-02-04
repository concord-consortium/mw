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

package org.concord.modeler.event;

/**
 * Caution!!! Do NOT use this interface. Reason: All ModelListeners added to a model will get removed when the page that
 * contains the model is (re)loaded. The ModelListeners that are saved in the content of the page will be added
 * according to the data contained in the file. If a ModelListener is not implemented by a serialized component, as the
 * embedded components such as PageButton, then it will not be added back to the listener list when a page is loaded.
 */

public interface ModelListener {

	public void modelUpdate(ModelEvent e);

}
