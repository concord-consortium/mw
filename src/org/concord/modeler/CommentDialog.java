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

import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.concord.modeler.event.CommentEvent;
import org.concord.modeler.event.CommentListener;
import org.concord.modeler.text.Page;

class CommentDialog extends JDialog implements CommentListener {

	private Page page;
	private CommentInputPane commentInputPane;
	private ActionListener viewCommentAction;

	CommentDialog(Page p, Person user) {

		super(JOptionPane.getFrameForComponent(p), "Please make comments on this page", true);
		String s = Modeler.getInternationalText("MakeComments");
		if (s != null)
			setTitle(s);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		page = p;

		commentInputPane = new CommentInputPane(true);
		commentInputPane.setRegisterAction(new Runnable() {
			public void run() {
				dispose();
				page.getNavigator().visitLocation(Modeler.getContextRoot() + "register.jsp?client=mw");
			}
		});
		commentInputPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		commentInputPane.setUser(user);
		commentInputPane.addCommentListener(this);
		setContentPane(commentInputPane);

		URL u = null;
		try {
			u = new URL(Modeler.getContextRoot() + "comment");
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (u != null)
			commentInputPane.setServletURL(u);

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(page));

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				commentInputPane.requestFocus();
			}
		});

	}

	void setViewCommentAction(ActionListener a) {
		viewCommentAction = a;
	}

	void setPageAddress(String s) {
		commentInputPane.setPageAddress(s);
	}

	public void commentSubmitted(CommentEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				dispose();
				if (viewCommentAction != null)
					viewCommentAction.actionPerformed(null);
			}
		});
	}

	public void commentCanceled(CommentEvent e) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				dispose();
			}
		});
	}

}