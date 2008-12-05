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
class ImageQuestionMaker extends ComponentMaker {

	private ImageQuestion imageQuestion;
	private JDialog dialog;
	private JCheckBox transparentCheckBox;
	private ColorComboBox bgComboBox;
	private JComboBox borderComboBox;
	private IntegerTextField widthField, heightField;
	private JTextArea questionArea;
	private JPanel contentPane;

	ImageQuestionMaker(ImageQuestion iq) {
		setObject(iq);
	}

	void setObject(ImageQuestion iq) {
		imageQuestion = iq;
	}

	private boolean confirm() {

		String s = Modeler.getInternationalText("YouMustSetQuestion");
		String s2 = Modeler.getInternationalText("SetQuestion");
		if (questionArea.getText() == null || questionArea.getText().trim().equals("")) {
			JOptionPane.showMessageDialog(dialog, s != null ? s : "You must set a question.", s2 != null ? s2
					: "Set question", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		imageQuestion.setQuestion(questionArea.getText());
		imageQuestion.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		imageQuestion.setBorderType((String) borderComboBox.getSelectedItem());
		imageQuestion.setOpaque(!transparentCheckBox.isSelected());
		if (imageQuestion.isOpaque())
			imageQuestion.setBackground(bgComboBox.getSelectedColor());

		String address = imageQuestion.page.getAddress() + "#"
				+ ModelerUtilities.getSortableString(imageQuestion.index, 3) + "%" + ImageQuestion.class.getName();
		QuestionAndAnswer q = UserData.sharedInstance().getData(address);
		if (q != null) {
			// if the question text has been changed
			if (!q.getQuestion().equals(imageQuestion.getQuestion())) {
				q = new QuestionAndAnswer(imageQuestion.getQuestion(), q.getAnswer());
				UserData.sharedInstance().putData(address, q);
			}
		}

		imageQuestion.page.getSaveReminder().setChanged(true);
		imageQuestion.page.settleComponentSize();
		return true;

	}

	void invoke(Page page) {

		imageQuestion.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeImageQuestionDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize image question", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					questionArea.requestFocusInWindow();
					questionArea.setCaretPosition(0);
				}
			});
		}

		widthField.setValue(imageQuestion.getPreferredSize().width);
		heightField.setValue(imageQuestion.getPreferredSize().height);
		questionArea.setText(ModelerUtilities.deUnicode(imageQuestion.getQuestion()));
		borderComboBox.setSelectedItem(imageQuestion.getBorderType());
		transparentCheckBox.setSelected(!imageQuestion.isOpaque());
		bgComboBox.setColor(imageQuestion.getBackground());

		dialog.setVisible(true);

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!confirm())
					return;
				dialog.dispose();
				cancel = false;
			}
		};

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
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
				Modeler.openWithNewInstance(imageQuestion.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/imagequestion.cml");
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p.add(p2, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		p2.add(p1, BorderLayout.NORTH);

		s = Modeler.getInternationalText("WidthLabel");
		p1.add(new JLabel((s != null ? s : "Width") + " : ", SwingConstants.LEFT));
		widthField = new IntegerTextField(imageQuestion.getWidth() <= 0 ? 400 : imageQuestion.getWidth(), 100, 1000, 5);
		widthField.addActionListener(okListener);
		p1.add(widthField);

		s = Modeler.getInternationalText("HeightLabel");
		p1.add(new JLabel((s != null ? s : "Height") + " : ", SwingConstants.LEFT));
		heightField = new IntegerTextField(imageQuestion.getHeight() <= 0 ? 300 : imageQuestion.getHeight(), 100, 800,
				5);
		heightField.addActionListener(okListener);
		p1.add(heightField);

		s = Modeler.getInternationalText("BorderLabel");
		p1.add(new JLabel((s != null ? s : "Border") + " : ", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p1.getBackground());
		p1.add(borderComboBox);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
		p2.add(p1, BorderLayout.CENTER);

		s = Modeler.getInternationalText("Transparent");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setSelected(true);
		p1.add(transparentCheckBox);

		s = Modeler.getInternationalText("BackgroundColorLabel");
		p1.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(imageQuestion);
		p1.add(bgComboBox);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("SetQuestion");
		p.add(new JLabel((s != null ? s : "Set question") + " (HTML OK):"), BorderLayout.NORTH);
		questionArea = new PastableTextArea();
		questionArea.setBorder(BorderFactory.createLoweredBevelBorder());
		JScrollPane scroller = new JScrollPane(questionArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroller.setPreferredSize(new Dimension(300, 200));
		p.add(scroller, BorderLayout.CENTER);

	}

}