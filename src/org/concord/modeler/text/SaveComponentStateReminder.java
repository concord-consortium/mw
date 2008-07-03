/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.awt.EventQueue;

import javax.swing.JOptionPane;

import org.concord.modeler.Modeler;

class SaveComponentStateReminder {

	private static boolean suppress;
	private static boolean enabled = true;
	private static boolean i18ned;
	private final static String[] DIALOG_OPTIONS = { "No", "Yes", "Yes, and don't ask again." };

	private SaveComponentStateReminder() {
	}

	private static void i18n() {
		String s = Modeler.getInternationalText("No");
		if (s != null)
			DIALOG_OPTIONS[0] = s;
		s = Modeler.getInternationalText("Yes");
		if (s != null)
			DIALOG_OPTIONS[1] = s;
		s = Modeler.getInternationalText("YesAndDoNotAskAgain");
		if (s != null)
			DIALOG_OPTIONS[2] = s;
		i18ned = true;
	}

	static void setEnabled(boolean b) {
		enabled = b;
	}

	static void ask(final Page parent, final String name, final Runnable saveRun, final Runnable resetRun) {
		if (suppress) {
			saveRun.run();
			return;
		}
		if (!enabled) {
			saveRun.run();
			enabled = true;
			return;
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				dialog2(parent, name, saveRun, resetRun);
			}
		});
	}

	private static void dialog2(Page parent, String name, Runnable saveRun, Runnable resetRun) {
		if (!i18ned)
			i18n();
		String s = Modeler.getInternationalText("DoYouAlsoWantToSaveStateOfModel");
		String s2 = Modeler.getInternationalText("SaveModelState");
		int i = JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(parent), (s != null ? s
				: "Do you also want to save the state of this embedded model")
				+ ": " + name + "?", s2 != null ? s2 : "Save Model State", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, DIALOG_OPTIONS, DIALOG_OPTIONS[0]);
		switch (i) {
		case JOptionPane.YES_OPTION:
			resetRun.run();
			break;
		case JOptionPane.CANCEL_OPTION:
			suppress = true;
		case JOptionPane.NO_OPTION:
			parent.getProgressBar().setString("Saving model state: " + name + "......");
			Thread t = new Thread(saveRun, "Save Model State: " + name);
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			break;
		default:
			resetRun.run();
		}
	}

}