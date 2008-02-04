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

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.concord.mw2d.MDView;

class ActionReminder {

	final static Byte INVOKE_MINIMIZER = (byte) 0;
	final static Byte RESET_TAPE = (byte) 1;
	final static Byte RESET_TO_SAVED_STATE = (byte) 2;

	private boolean suppress;
	private Component parent;
	private Map<Byte, Boolean> show;

	private static boolean i18ned;
	private final static String[] DIALOG_OPTIONS_YES_NO = { "OK", "Don't show this again." };
	private final static String[] DIALOG_OPTIONS_YES_NO_CANCEL = { "Yes", "No", "Yes, but don't show again." };

	public ActionReminder() {
		show = new HashMap<Byte, Boolean>();
		show.put(INVOKE_MINIMIZER, Boolean.TRUE);
		show.put(RESET_TAPE, Boolean.TRUE);
		show.put(RESET_TO_SAVED_STATE, Boolean.TRUE);
	}

	private static void i18n() {
		String s = MDView.getInternationalText("OK");
		if (s != null)
			DIALOG_OPTIONS_YES_NO[0] = s;
		s = MDView.getInternationalText("DoNotShowThisMessageAgain");
		if (s != null)
			DIALOG_OPTIONS_YES_NO[1] = s;
		s = MDView.getInternationalText("Yes");
		if (s != null)
			DIALOG_OPTIONS_YES_NO_CANCEL[0] = s;
		s = MDView.getInternationalText("No");
		if (s != null)
			DIALOG_OPTIONS_YES_NO_CANCEL[1] = s;
		s = MDView.getInternationalText("YesButDoNotShowThisMessageAgain");
		if (s != null)
			DIALOG_OPTIONS_YES_NO_CANCEL[2] = s;
		i18ned = true;
	}

	public void setParentComponent(Component parent) {
		this.parent = parent;
	}

	public void setSuppressed(boolean b) {
		suppress = b;
	}

	public boolean isSuppressed() {
		return suppress;
	}

	public int show(Byte type) {

		if (!i18ned)
			i18n();

		if (type == INVOKE_MINIMIZER) {
			if (suppress)
				return JOptionPane.CANCEL_OPTION;
			if (show.get(INVOKE_MINIMIZER) == Boolean.TRUE) {
				int i = JOptionPane
						.showOptionDialog(
								parent,
								"The engine has detected that some objects overlap too much.\nEnergy minimization will be performed now.",
								"Bad overlaps", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
								DIALOG_OPTIONS_YES_NO, DIALOG_OPTIONS_YES_NO[0]);
				if (i == JOptionPane.NO_OPTION)
					show.put(INVOKE_MINIMIZER, Boolean.FALSE);
				return i;
			}
		}

		else if (type == RESET_TAPE) {
			if (suppress)
				return JOptionPane.CANCEL_OPTION;
			if (show.get(RESET_TAPE) == Boolean.TRUE) {
				String s1 = MDView.getInternationalText("RecorderResetBeforeChangingModel");
				String s2 = MDView.getInternationalText("ResetRecorder");
				int i = JOptionPane.showOptionDialog(parent, s1 != null ? s1
						: "The recorder has to be reset before changing the model. Do you want to continue?",
						s2 != null ? s2 : "Reset recorder", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE, null, DIALOG_OPTIONS_YES_NO_CANCEL,
						DIALOG_OPTIONS_YES_NO_CANCEL[0]);
				if (i == JOptionPane.CANCEL_OPTION)
					show.put(RESET_TAPE, Boolean.FALSE);
				return i;
			}
			return JOptionPane.CANCEL_OPTION;
		}

		else if (type == RESET_TO_SAVED_STATE) {
			if (suppress)
				return JOptionPane.CANCEL_OPTION;
			if (show.get(RESET_TO_SAVED_STATE) == Boolean.TRUE) {
				String s1 = MDView.getInternationalText("DoYouReallyWantToReset");
				String s2 = MDView.getInternationalText("Reset");
				int i = JOptionPane.showOptionDialog(parent, s1 != null ? s1
						: "Do you really want to reset to the original state?", s2 != null ? s2 : "Reset",
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
						DIALOG_OPTIONS_YES_NO_CANCEL, DIALOG_OPTIONS_YES_NO_CANCEL[0]);
				if (i == JOptionPane.CANCEL_OPTION)
					show.put(RESET_TO_SAVED_STATE, Boolean.FALSE);
				return i;
			}
			return JOptionPane.CANCEL_OPTION;
		}

		return -1;

	}

}