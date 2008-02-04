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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import org.concord.modeler.ui.IconPool;

class StopAction extends AbstractAction {

	private MDModel model;

	StopAction(MDModel model) {
		this.model = model;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		putValue(NAME, "Stop");
		putValue(SHORT_DESCRIPTION, "Stop");
		putValue(SMALL_ICON, IconPool.getIcon("pause"));
	}

	/**
	 * NOTE: sometimes, when this event is dispatched and processed, the movieUpdater has not updated the current frame
	 * index yet. So we use movie.length()-2 instead of movie.length()-1 to prevent malfunctions. It is not likely that
	 * the user will be able to pause when just rewinding one step backward from the current frame.
	 */
	public void actionPerformed(ActionEvent e) {
		if (model.movie.getCurrentFrameIndex() >= model.movie.length() - 2)
			model.stop();
		model.movie.pause();
		model.movie.enableMovieActions(true);
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}