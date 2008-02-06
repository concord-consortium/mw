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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;

import org.concord.modeler.ActivityButton;
import org.concord.modeler.ConnectionManager;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.QuestionAndAnswer;
import org.concord.modeler.UserData;
import org.concord.modeler.util.FileUtilities;

class GradeAction extends AbstractAction {

	private Page page;
	private PageNameGroup pageNameGroup;
	private URL url;
	private HttpURLConnection connection;
	private String title;
	private GradeSubmissionDialog gradeSubmissionDialog;

	GradeAction(Page page) {
		super();
		this.page = page;
		putValue(NAME, "Submit for Grade");
		putValue(SHORT_DESCRIPTION, "Submit for grade");
		pageNameGroup = new PageNameGroup();
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		if (gradeSubmissionDialog == null) {
			gradeSubmissionDialog = new GradeSubmissionDialog(JOptionPane.getFrameForComponent(page));
			gradeSubmissionDialog.pack();
			gradeSubmissionDialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(page));
		}
		gradeSubmissionDialog.setVisible(true);
		if (gradeSubmissionDialog.isCancelled())
			return;
		Object src = e.getSource();
		if (src instanceof ActivityButton) {
			title = ((ActivityButton) src).getReportTitle();
			pageNameGroup.setNameGroup(((ActivityButton) src).getPageNameGroup());
			Object o = ((ActivityButton) src).getClientProperty("grade_uri");
			if (o instanceof String) {
				try {
					url = new URL((String) o);
				}
				catch (MalformedURLException ex) {
					ex.printStackTrace(System.err);
					url = null;
				}
			}
			else {
				url = null;
			}
		}
		Thread t = new Thread("Grade Submitter") {
			public void run() {
				post(encode());
				showFeedback();
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

	private void showFeedback() {
		if (url == null)
			return;
		if (connection == null)
			return;
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(page), "Grading results", false);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JEditorPane htmlPane = new JEditorPane();
		htmlPane.setEditable(false);
		htmlPane.setContentType("text/html");
		HTMLEditorKit kit = (HTMLEditorKit) htmlPane.getEditorKit();
		try {
			kit.read(connection.getInputStream(), htmlPane.getDocument(), 0);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			htmlPane.setText("<html><body><font size=\"5\" color=\"#ff0000\"><b>Error!</b></html></body></html>");
		}
		final JScrollPane scrollPane = new JScrollPane(htmlPane);
		scrollPane.setPreferredSize(new Dimension(600, 400));
		d.getContentPane().add(scrollPane, BorderLayout.CENTER);
		JPanel p = new JPanel();
		d.getContentPane().add(p, BorderLayout.SOUTH);
		String s = Modeler.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		p.add(button);
		d.pack();
		d.setLocationRelativeTo(JOptionPane.getFrameForComponent(page));
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				scrollPane.getViewport().setViewPosition(new Point(0, 0));
			}
		});
		d.setVisible(true);
	}

	private void post(String s) {
		if (url == null)
			return;
		connection = ConnectionManager.getConnection(url);
		if (connection == null)
			return;
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);
		try {
			connection.setRequestMethod("POST");
			connection.getOutputStream().write(s.getBytes());
		}
		catch (SocketTimeoutException e) {
			e.printStackTrace(System.err);
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
		}
		finally {
			try {
				connection.getOutputStream().close();
			}
			catch (IOException e) {
			}
		}
	}

	private String encode() {

		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<document>\n");
		sb.append("  <head>\n");
		sb.append("    <title>" + title + "</title>\n");
		if (Modeler.user != null && !Modeler.user.isEmpty()) {
			sb.append("    <student>\n");
			sb.append("      <first>" + Modeler.user.getFirstName() + "</first>\n");
			sb.append("      <last>" + Modeler.user.getLastName() + "</last>\n");
			sb.append("      <email>" + Modeler.user.getEmailAddress() + "</email>\n");
			sb.append("      <school>" + Modeler.user.getInstitution() + "</school>\n");
			sb.append("    </student>\n");
		}
		if (Modeler.user.getTeacher() != null) {
			sb.append("    <teacher>" + Modeler.user.getTeacher() + "</teacher>\n");
		}
		sb.append("    <start></start>\n");
		sb.append("    <finish>" + System.currentTimeMillis() + "</finish>\n");
		sb.append("  </head>\n");

		sb.append("  <body>\n\n");

		String key;
		QuestionAndAnswer val;
		int n = pageNameGroup.size();
		String parent = FileUtilities.getCodeBase(page.getAddress());
		for (int i = 0; i < n; i++) {
			for (Iterator it = UserData.sharedInstance().keySet().iterator(); it.hasNext();) {
				key = (String) it.next();
				if (!FileUtilities.getCodeBase(key).equals(parent))
					continue;
				int i2 = key.indexOf("#");
				if (i2 >= 0 && key.substring(0, i2).endsWith(pageNameGroup.getPageName(i))) {
					val = UserData.sharedInstance().getData(key);
					if (key.endsWith("MultipleChoice")) {
						sb.append(encodeMultipleChoice(key, val));
						sb.append('\n');
					}
					else if (key.endsWith("TextField") || key.endsWith("TextArea")) {
						sb.append(encodeEssayQuestion(key, val));
						sb.append('\n');
					}
				}
			}
		}
		sb.append("  </body>\n");

		sb.append("</document>");
		return sb.toString();

	}

	private static boolean isIncluded(int x, int[] set) {
		if (set == null)
			return false;
		for (int i = 0; i < set.length; i++)
			if (x == set[i])
				return true;
		return false;
	}

	private StringBuffer encodeMultipleChoice(String key, QuestionAndAnswer val) {

		int i1 = key.indexOf("#");
		int i2 = key.indexOf("%");
		String address = key.substring(0, i1);
		String id = key.substring(i1 + 1, i2);
		String body = val.getQuestion();

		int[] correct = null;
		String ra = val.getReferenceAnswer();
		if (ra != null) {
			String[] s = ra.split("\\s");
			correct = new int[s.length];
			for (int i = 0; i < s.length; i++)
				correct[i] = Integer.parseInt(s[i]);
		}

		int[] selected = null;
		String answer = val.getAnswer();
		if (answer != null) {
			String[] s = answer.split("\\s");
			selected = new int[s.length];
			for (int i = 0; i < s.length; i++)
				selected[i] = Integer.parseInt(s[i]);
		}

		i1 = body.indexOf("(a)");
		String question = body.substring(0, i1 - 2);
		char c = 'b';
		StringBuffer choices = new StringBuffer();
		while ((i2 = body.indexOf("(" + c + ")")) != -1) {
			choices.append("      <choice");
			if (isIncluded((c - 'a' - 1), correct))
				choices.append(" correct=\"true\"");
			if (isIncluded((c - 'a' - 1), selected))
				choices.append(" selected=\"true\"");
			choices.append(">");
			choices.append(XMLCharacterEncoder.encode(body.substring(i1 + 4, i2 - 1)) + "</choice>\n");
			i1 = i2;
			c++;
		}
		i2 = body.indexOf("My answer is");
		choices.append("      <choice");
		if (isIncluded((c - 'a' - 1), correct))
			choices.append(" correct=\"true\"");
		if (isIncluded((c - 'a' - 1), selected))
			choices.append(" selected=\"true\"");
		choices.append(">");
		choices.append(XMLCharacterEncoder.encode(body.substring(i1 + 4, i2 - 2)) + "</choice>\n");

		StringBuffer guess = null;
		List list = val.getGuesses();
		if (list != null && !list.isEmpty()) {
			guess = new StringBuffer();
			for (Iterator it = list.iterator(); it.hasNext();) {
				guess.append("      <guess>" + it.next() + "</guess>\n");
			}
		}

		StringBuffer sb = new StringBuffer();
		sb.append("    <multiplechoice>\n");
		sb.append("      <page>" + XMLCharacterEncoder.encode(address) + "</page>\n");
		sb.append("      <id>" + id + "</id>\n");
		if (val.getTimestamp() > 0)
			sb.append("      <timestamp>" + val.getTimestamp() + "</timestamp>\n");
		sb.append("      <question>" + XMLCharacterEncoder.encode(question) + "</question>\n");
		sb.append(choices);
		if (guess != null)
			sb.append(guess);
		sb.append("    </multiplechoice>\n");

		return sb;

	}

	private StringBuffer encodeEssayQuestion(String key, QuestionAndAnswer val) {
		int i1 = key.indexOf("#");
		int i2 = key.indexOf("%");
		String address = key.substring(0, i1);
		String id = key.substring(i1 + 1, i2);
		StringBuffer sb = new StringBuffer();
		sb.append("    <qanda>\n");
		sb.append("      <page>" + XMLCharacterEncoder.encode(address) + "</page>\n");
		sb.append("      <id>" + id + "</id>\n");
		if (val.getTimestamp() > 0)
			sb.append("      <timestamp>" + val.getTimestamp() + "</timestamp>\n");
		sb.append("      <question>" + XMLCharacterEncoder.encode(val.getQuestion()) + "</question>\n");
		sb.append("      <answer>" + XMLCharacterEncoder.encode(val.getAnswer()) + "</answer>\n");
		if (val.getReferenceAnswer() != null && !val.getReferenceAnswer().trim().equals("")) {
			sb.append("      <referenceanswer>");
			sb.append(XMLCharacterEncoder.encode(val.getReferenceAnswer()));
			sb.append("</referenceanswer>\n");
		}
		sb.append("    </qanda>\n");
		return sb;
	}

}