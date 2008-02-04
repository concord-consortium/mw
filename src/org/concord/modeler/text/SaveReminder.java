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

package org.concord.modeler.text;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.concord.modeler.Modeler;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.event.PageComponentListener;

public class SaveReminder implements PageComponentListener {

	private boolean changed, enabled = true;
	private boolean changeCausedByRun;

	public synchronized void setChanged(boolean b) {
		changed = b;
	}

	public synchronized boolean isChanged() {
		return changed;
	}

	public synchronized void setEnabled(boolean b) {
		enabled = b;
	}

	public synchronized boolean isEnabled() {
		return enabled;
	}

	public int showConfirmDialog(final Component parent, final String filename) {
		if (!changed || !enabled)
			return JOptionPane.NO_OPTION;
		String s2 = Modeler.getInternationalText("SaveConfirmation");
		if (changeCausedByRun) {
			String s = Modeler.getInternationalText("DoYouWantToSaveModelState");
			return JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(parent), s != null ? s
					: "Do you want to save the current state as the next initial state?", s2 != null ? s2
					: "Save Confirmation", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		}
		String s = Modeler.getInternationalText("DoYouWantToSaveChanges");
		return JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(parent), s != null ? s
				: "Do you want to save changes made to " + filename + "?", s2 != null ? s2 : "Save Confirmation",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
	}

	public void pageComponentChanged(PageComponentEvent e) {
		byte id = e.getID();
		// changeCausedByRun = id == PageComponentEvent.COMPONENT_RUN;
		if (id != PageComponentEvent.SNAPSHOT_TAKEN && id != PageComponentEvent.COMPONENT_RUN)
			setChanged(true);
	}

}