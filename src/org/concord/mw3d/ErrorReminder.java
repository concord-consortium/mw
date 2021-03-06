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
package org.concord.mw3d;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class ErrorReminder {

	public final static Byte EXCEED_CAPACITY = (byte) 0;
	public final static Byte OBJECT_OVERLAP = (byte) 1;

	private MolecularView parent;
	private Map<Byte, Boolean> show;
	private boolean suppress;

	private static boolean i18ned;
	private final static String[] DIALOG_OPTIONS = { "OK", "Don't show this message again." };

	ErrorReminder(MolecularView parent) {
		this.parent = parent;
		show = new HashMap<Byte, Boolean>();
		show.put(EXCEED_CAPACITY, Boolean.TRUE);
		show.put(OBJECT_OVERLAP, Boolean.TRUE);
	}

	private static void i18n() {
		String s = MolecularContainer.getInternationalText("OK");
		if (s != null)
			DIALOG_OPTIONS[0] = s;
		s = MolecularContainer.getInternationalText("DoNotShowThisMessageAgain");
		if (s != null)
			DIALOG_OPTIONS[1] = s;
		i18ned = true;
	}

	public void setSuppressed(boolean b) {
		suppress = b;
	}

	public boolean isSuppressed() {
		return suppress;
	}

	public void show(Byte type) {

		if (suppress)
			return;

		if (!i18ned)
			i18n();

		if (type == EXCEED_CAPACITY) {
			if (show.get(EXCEED_CAPACITY) == Boolean.TRUE) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						int n = MolecularModel.SIZE;
						if (JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(parent),
								"The number of atoms has reached the limit(" + n + "). You cannot insert more.",
								"Atom Exceeds Limit", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null,
								DIALOG_OPTIONS, DIALOG_OPTIONS[0]) == JOptionPane.NO_OPTION) {
							show.put(EXCEED_CAPACITY, Boolean.FALSE);
						}
					}
				});
			}
		}

		else if (type == OBJECT_OVERLAP) {
			if (show.get(OBJECT_OVERLAP) == Boolean.TRUE) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						String s1 = MolecularContainer.getInternationalText("OverlapNotAllowed");
						String s2 = MolecularContainer.getInternationalText("OverlapError");
						if (JOptionPane
								.showOptionDialog(
										JOptionPane.getFrameForComponent(parent),
										s1 != null ? s1
												: "<html>Overlap with other objects or the boundary of the glass simulation box<br>is not allowed. Action aborted.</html>",
										s2 != null ? s2 : "Overlap Error", JOptionPane.YES_NO_OPTION,
										JOptionPane.ERROR_MESSAGE, null, DIALOG_OPTIONS, DIALOG_OPTIONS[0]) == JOptionPane.NO_OPTION) {
							show.put(OBJECT_OVERLAP, Boolean.FALSE);
						}
					}
				});
			}
		}

	}

}