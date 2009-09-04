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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
class PageTextFieldMaker extends ComponentMaker {

	private PageTextField pageTextField;
	private JDialog dialog;
	private JTextField uidField;
	private JTextArea titleArea, answerArea;
	private IntegerTextField widthField, heightField;
	private JRadioButton topRadioButton, leftRadioButton;
	private JCheckBox transparentCheckBox;
	private ColorComboBox bgComboBox;
	private JComboBox borderComboBox;
	private JPanel contentPane;

	PageTextFieldMaker(PageTextField ptf) {
		setObject(ptf);
	}

	void setObject(PageTextField ptf) {
		pageTextField = ptf;
	}

	private boolean confirm() {

		if (titleArea.getText() == null || titleArea.getText().trim().equals("")) {
			JOptionPane.showMessageDialog(dialog, "You must set the question for this text field.", "Missing question",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (!checkAndSetUid(uidField.getText(), pageTextField, dialog))
			return false;

		pageTextField.setTitle(titleArea.getText());
		pageTextField.setReferenceAnswer(answerArea.getText());
		pageTextField.setQuestionPosition(topRadioButton.isSelected() ? BorderLayout.NORTH : BorderLayout.WEST);
		pageTextField.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageTextField.setOpaque(!transparentCheckBox.isSelected());
		pageTextField.setBorderType((String) borderComboBox.getSelectedItem());
		if (pageTextField.isOpaque())
			pageTextField.setBackground(bgComboBox.getSelectedColor());

		String address = pageTextField.page.getAddress() + "#"
				+ ModelerUtilities.getSortableString(pageTextField.index, 3) + "%" + PageTextField.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(address);
		if (q != null) {
			q.setQuestion(pageTextField.getTitle());
			q.setReferenceAnswer(pageTextField.referenceAnswer);
		}

		pageTextField.page.getSaveReminder().setChanged(true);
		pageTextField.page.settleComponentSize();

		return true;

	}

	void invoke(Page page) {

		pageTextField.setPage(page);
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeTextFieldDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize text field", true);
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

		uidField.setText(pageTextField.getUid());
		borderComboBox.setSelectedItem(pageTextField.getBorderType());
		transparentCheckBox.setSelected(!pageTextField.isOpaque());
		bgComboBox.setColor(pageTextField.getBackground());
		if (pageTextField.getTitle() == null || pageTextField.getTitle().trim().equals(""))
			pageTextField.setTitle("<html><body><face=Verdana>\n   Question\n</html>");
		widthField.setValue(pageTextField.getPreferredSize().width);
		heightField.setValue(pageTextField.getPreferredSize().height);
		titleArea.setText(ModelerUtilities.deUnicode(pageTextField.getTitle()));
		titleArea.setCaretPosition(0);
		answerArea.setText(pageTextField.getReferenceAnswer());
		if (pageTextField.layout.equals(BorderLayout.NORTH)) {
			topRadioButton.setSelected(true);
		}
		else {
			leftRadioButton.setSelected(true);
		}

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
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!confirm())
					return;
				dialog.dispose();
				cancel = false;
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(pageTextField.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/textfield.cml");
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p.add(p2, BorderLayout.NORTH);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		p2.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("QuestionPosition");
		p1.add(new JLabel((s != null ? s : "Question position") + ":"));

		ButtonGroup bg = new ButtonGroup();

		s = Modeler.getInternationalText("Top");
		topRadioButton = new JRadioButton(s != null ? s : "Top");
		topRadioButton.setSelected(true);
		bg.add(topRadioButton);
		p1.add(topRadioButton);

		s = Modeler.getInternationalText("Left");
		leftRadioButton = new JRadioButton(s != null ? s : "Left");
		bg.add(leftRadioButton);
		p1.add(leftRadioButton);

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

		s = Modeler.getInternationalText("UniqueIdentifier");
		p1.add(new JLabel((s != null ? s : "Unique identifier") + " : ", SwingConstants.LEFT));
		uidField = new JTextField(10);
		uidField.setToolTipText("Type in a string to be used as the unique identifier of this text field.");
		uidField.addActionListener(okListener);
		p1.add(uidField);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		p2.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel((s != null ? s : "Border") + ":"));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		borderComboBox.setSelectedIndex(0);
		p1.add(borderComboBox);

		s = Modeler.getInternationalText("Transparent");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setSelected(true);
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("BackgroundColorLabel");
		p1.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageTextField);
		p1.add(bgComboBox);

		titleArea = new PastableTextArea(pageTextField.getTitle());
		JScrollPane sp = new JScrollPane(titleArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setPreferredSize(new Dimension(300, 150));
		p1 = new JPanel(new BorderLayout());
		s = Modeler.getInternationalText("QuestionText");
		p1.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Question text"));
		p1.add(sp, BorderLayout.CENTER);
		p.add(p1, BorderLayout.CENTER);

		answerArea = new PastableTextArea(pageTextField.referenceAnswer);
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