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

package org.concord.modeler;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.concord.modeler.text.Page;
import org.concord.modeler.text.PageNameGroup;

abstract class AbstractCreateReportAction extends AbstractAction {

	Modeler modeler;
	boolean justPrint;

	AbstractCreateReportAction(Modeler modeler) {
		super();
		this.modeler = modeler;
		putValue(SMALL_ICON, new ImageIcon(getClass().getResource("images/Report.gif")));
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

	void openReportPage(Map map, PageNameGroup png) {
		final Modeler m = modeler.openNewWindowWithoutBars(false, true);
		if (png == null) {
			m.editor.getPage().createReport(map);
		}
		else {
			m.editor.getPage().createReportForPageGroup(map, png);
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				m.editor.setViewPosition(0, 0);
				m.editor.setEditable(false);
				m.editor.getPage().saveReport();
			}
		});
	}

	// shall we continue?
	boolean question() {
		justPrint = false;
		if (Modeler.user.isEmpty())
			return handleUserDataNotFound();
		return handleUserDataFound();
	}

	private boolean handleUserDataFound() {
		String name = Modeler.user.getFullName();
		String s = Modeler.getInternationalText("AreYou");
		int i = JOptionPane.showConfirmDialog(modeler, s == null ? name + " (ID: " + Modeler.user.getUserID()
				+ ") is currently logged in.\nAre you " + name + "?" : s + " " + name + "?");
		if (i == JOptionPane.YES_OPTION)
			return true;
		if (i == JOptionPane.NO_OPTION) {
			Page p = new Page();
			p.setPreferredSize(new Dimension(500, 360));
			p.visit(Modeler.getContextRoot() + "login.jsp?client=mw&action=logout&onreport=yes");
			s = Modeler.getInternationalText("Login");
			i = JOptionPane.showConfirmDialog(modeler, p, s != null ? s : "Login", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null);
			return i == JOptionPane.OK_OPTION || i == JOptionPane.CLOSED_OPTION;
		}
		return false;
	}

	private boolean handleUserDataNotFound() {
		String s = Modeler.getInternationalText("LoginToSubmitReport");
		String s2 = Modeler.getInternationalText("PrintReportWithoutLogin");
		Object[] options = new String[] { s != null ? s : "Log in to submit report.",
				s2 != null ? s2 : "Print without login." };
		s = Modeler.getInternationalText("NeedToLogin");
		s2 = Modeler.getInternationalText("YouAreNotCurrentlyLoggedIn");
		int i = JOptionPane.showOptionDialog(modeler, s2 != null ? s2
				: "You are not currently logged in. What would you like to do?", s != null ? s : "Need to login",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if (i == JOptionPane.YES_OPTION) {
			s = Modeler.getInternationalText("HaveYouRegisteredWithMolecularWorkbench");
			s2 = Modeler.getInternationalText("Question");
			int j = JOptionPane.showConfirmDialog(modeler, s != null ? s
					: "Have you registered with the Molecular Workbench?", s2 != null ? s2 : "Question",
					JOptionPane.YES_NO_OPTION);
			Page p = new Page();
			if (j == JOptionPane.YES_OPTION) {
				p.setPreferredSize(new Dimension(500, 360));
				p.visit(Modeler.getContextRoot() + "login.jsp?client=mw&onreport=yes");
				s = Modeler.getInternationalText("Login");
				i = JOptionPane.showConfirmDialog(modeler, p, s != null ? s : "Login", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE, null);
			}
			else {
				p.setPreferredSize(new Dimension(800, 600));
				p.visit(Modeler.getContextRoot() + "register.jsp?client=mw&onreport=yes");
				s = Modeler.getInternationalText("DoneWithRegistration");
				s2 = Modeler.getInternationalText("CancelButton");
				String s3 = Modeler.getInternationalText("Registration");
				String[] ops = new String[] { s != null ? s : "Done with registration.", s2 != null ? s2 : "Cancel" };
				i = JOptionPane.showOptionDialog(modeler, p, s3 != null ? s3 : "Registration",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, ops, ops[0]);
			}
			return i == JOptionPane.OK_OPTION || i == JOptionPane.CLOSED_OPTION;
		}
		else {
			UserInfoProvider provider = new UserInfoProvider(modeler);
			provider.setLocationRelativeTo(modeler);
			provider.setVisible(true);
			if (provider.isOK)
				justPrint = true;
			return provider.isOK;
		}
	}

}