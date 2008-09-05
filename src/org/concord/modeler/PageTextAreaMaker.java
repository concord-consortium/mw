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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;

/**
 * @author Charles Xie
 * 
 */
class PageTextAreaMaker extends ComponentMaker {

	private PageTextArea pageTextArea;
	private JDialog dialog;
	private IntegerTextField widthField, heightField;
	private JTextArea titleArea, answerArea;
	private JCheckBox transparentCheckBox;
	private ColorComboBox bgComboBox;
	private JComboBox borderComboBox;
	private JPanel contentPane;

	PageTextAreaMaker(PageTextArea pta) {
		setObject(pta);
	}

	void setObject(PageTextArea pta) {
		pageTextArea = pta;
	}

	private boolean confirm() {

		if (titleArea.getText() == null || titleArea.getText().trim().equals("")) {
			JOptionPane.showMessageDialog(dialog, "You must set the question for this text area.", "Missing question",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		pageTextArea.setTitle(titleArea.getText());
		pageTextArea.setReferenceAnswer(answerArea.getText());
		pageTextArea.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageTextArea.setOpaque(!transparentCheckBox.isSelected());
		pageTextArea.setBorderType((String) borderComboBox.getSelectedItem());
		if (pageTextArea.isOpaque())
			pageTextArea.setBackground(bgComboBox.getSelectedColor());

		String address = pageTextArea.page.getAddress() + "#"
				+ ModelerUtilities.getSortableString(pageTextArea.index, 3) + "%" + PageTextArea.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(address);
		if (q != null) {
			q.setQuestion(pageTextArea.getTitle());
			q.setReferenceAnswer(pageTextArea.referenceAnswer);
		}

		pageTextArea.page.getSaveReminder().setChanged(true);
		pageTextArea.page.settleComponentSize();

		return true;

	}

	void invoke(Page page) {

		pageTextArea.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeTextAreaDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize text area", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					titleArea.requestFocusInWindow();
				}
			});
		}

		borderComboBox.setSelectedItem(pageTextArea.getBorderType());
		transparentCheckBox.setSelected(!pageTextArea.isOpaque());
		bgComboBox.setColor(pageTextArea.getBackground());
		if (pageTextArea.getTitle() == null || pageTextArea.getTitle().trim().equals(""))
			pageTextArea.setTitle("<html>\n   Question\n</html>");
		widthField.setValue(pageTextArea.getPreferredSize().width);
		heightField.setValue(pageTextArea.getPreferredSize().height);
		titleArea.setText(ModelerUtilities.deUnicode(pageTextArea.getTitle()));
		titleArea.setCaretPosition(0);
		answerArea.setText(pageTextArea.getReferenceAnswer());

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (confirm()) {
					dialog.dispose();
					cancel = false;
				}
			}
		};

		contentPane = new JPanel(new BorderLayout());

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!confirm())
					return;
				dialog.dispose();
				cancel = false;
			}
		});
		p.add(b);

		s = Modeler.getInternationalText("CancelButton");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(b);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p.add(p2, BorderLayout.NORTH);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		p2.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("WidthLabel");
		p1.add(new JLabel((s != null ? s : "Width") + ":", SwingConstants.LEFT));

		widthField = new IntegerTextField(200, 20, 800, 5);
		widthField.addActionListener(okListener);
		p1.add(widthField);

		s = Modeler.getInternationalText("HeightLabel");
		p1.add(new JLabel((s != null ? s : "Height") + ":", SwingConstants.LEFT));

		heightField = new IntegerTextField(100, 20, 800, 5);
		heightField.addActionListener(okListener);
		p1.add(heightField);

		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel((s != null ? s : "Border") + ":"));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		borderComboBox.setSelectedIndex(0);
		p1.add(borderComboBox);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		p2.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("Transparent");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setSelected(true);
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("BackgroundColorLabel");
		p1.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageTextArea);
		p1.add(bgComboBox);

		titleArea = new PastableTextArea(pageTextArea.getTitle());
		JScrollPane sp = new JScrollPane(titleArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setPreferredSize(new Dimension(300, 150));
		p1 = new JPanel(new BorderLayout());
		s = Modeler.getInternationalText("QuestionText");
		p1.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Question text"));
		p1.add(sp, BorderLayout.CENTER);
		p.add(p1, BorderLayout.CENTER);

		answerArea = new PastableTextArea(pageTextArea.referenceAnswer);
		sp = new JScrollPane(answerArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setPreferredSize(new Dimension(300, 50));
		p1 = new JPanel(new BorderLayout());
		s = Modeler.getInternationalText("ReferenceAnswer");
		p1.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Reference answer"));
		p1.add(sp, BorderLayout.CENTER);
		p.add(p1, BorderLayout.SOUTH);

	}

}