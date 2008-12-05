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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;

class ColorBarDialog extends JDialog {

	private LineIcon lineIcon;
	private JCheckBox fillCheckBox;
	private AbstractButton upperLeftArcButton;
	private AbstractButton lowerLeftArcButton;
	private AbstractButton upperRightArcButton;
	private AbstractButton lowerRightArcButton;
	private ColorComboBox bgComboBox;
	private FloatNumberTextField widthField;
	private IntegerTextField heightField;
	private IntegerTextField topMarginField;
	private IntegerTextField bottomMarginField;
	private IntegerTextField leftMarginField;
	private IntegerTextField rightMarginField;
	private IntegerTextField arcWidthField;
	private IntegerTextField arcHeightField;
	private JTextArea textArea;
	private JLabel label;
	private int option = JOptionPane.CANCEL_OPTION;
	private Page page;

	public ColorBarDialog(Page pg) {

		super(JOptionPane.getFrameForComponent(pg), "Color bar", true);
		String s = Modeler.getInternationalText("ColorBar");
		if (s != null)
			setTitle(s);

		page = pg;

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(p, BorderLayout.SOUTH);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (confirm()) {
					option = JOptionPane.OK_OPTION;
					dispose();
				}
			}
		};

		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				option = JOptionPane.CANCEL_OPTION;
				dispose();
			}
		});
		p.add(button);

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(page.getNavigator().getHomeDirectory() + "tutorial/ColorBar.cml");
			}
		});
		p.add(button);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		getContentPane().add(panel, BorderLayout.NORTH);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel((s != null ? s : "Width") + ": (0, 1] for relative, >1 for absolute", SwingConstants.LEFT));
		widthField = new FloatNumberTextField(1, 0, 2000);
		widthField.setMaximumFractionDigits(4);
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 2
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(3, 0, 1000);
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 3
		s = Modeler.getInternationalText("LeftMargin");
		p.add(new JLabel(s != null ? s : "Left Margin", SwingConstants.LEFT));
		leftMarginField = new IntegerTextField(0, 0, 1000);
		leftMarginField.addActionListener(okListener);
		p.add(leftMarginField);

		// row 4
		s = Modeler.getInternationalText("RightMargin");
		p.add(new JLabel(s != null ? s : "Right Margin", SwingConstants.LEFT));
		rightMarginField = new IntegerTextField(0, 0, 1000);
		rightMarginField.addActionListener(okListener);
		p.add(rightMarginField);

		// row 5
		s = Modeler.getInternationalText("TopMargin");
		p.add(new JLabel(s != null ? s : "Top Margin", SwingConstants.LEFT));
		topMarginField = new IntegerTextField(2, -20, 20);
		topMarginField.addActionListener(okListener);
		p.add(topMarginField);

		// row 6
		s = Modeler.getInternationalText("BottomMargin");
		p.add(new JLabel(s != null ? s : "Bottom Margin", SwingConstants.LEFT));
		bottomMarginField = new IntegerTextField(2, -20, 20);
		bottomMarginField.addActionListener(okListener);
		p.add(bottomMarginField);

		// row 7
		s = Modeler.getInternationalText("ArcWidth");
		p.add(new JLabel(s != null ? s : "Arc Width", SwingConstants.LEFT));
		arcWidthField = new IntegerTextField(0, 0, 1000);
		arcWidthField.addActionListener(okListener);
		p.add(arcWidthField);

		// row 8
		s = Modeler.getInternationalText("ArcHeight");
		p.add(new JLabel(s != null ? s : "Arc Height", SwingConstants.LEFT));
		arcHeightField = new IntegerTextField(0, 0, 1000);
		arcHeightField.addActionListener(okListener);
		p.add(arcHeightField);

		// row 9
		s = Modeler.getInternationalText("Color");
		p.add(new JLabel(s != null ? s : "Fill Color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(this);
		bgComboBox.setRequestFocusEnabled(false);
		p.add(bgComboBox);

		ModelerUtilities.makeCompactGrid(p, 9, 2, 5, 5, 15, 5);

		p = new JPanel();
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10), BorderFactory
				.createEtchedBorder()));
		panel.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("Corner");
		p.add(new JLabel((s != null ? s : "Arc Corner") + ":", SwingConstants.LEFT));

		Dimension dim = ModelerUtilities.getSystemToolBarButtonSize();

		upperLeftArcButton = new JToggleButton(new ImageIcon(Page.class.getResource("images/UpperLeftCornerArc.gif")));
		upperLeftArcButton.setToolTipText("Draw an arc at the upper-left corner");
		upperLeftArcButton.setFocusPainted(false);
		upperLeftArcButton.setPreferredSize(dim);
		upperLeftArcButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					lineIcon.addCornerArc(LineIcon.UPPER_LEFT);
				}
				else {
					lineIcon.removeCornerArc(LineIcon.UPPER_LEFT);
				}
			}
		});
		p.add(upperLeftArcButton);

		lowerLeftArcButton = new JToggleButton(new ImageIcon(Page.class.getResource("images/LowerLeftCornerArc.gif")));
		lowerLeftArcButton.setToolTipText("Draw an arc at the lower-left corner");
		lowerLeftArcButton.setPreferredSize(dim);
		lowerLeftArcButton.setFocusPainted(false);
		lowerLeftArcButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					lineIcon.addCornerArc(LineIcon.LOWER_LEFT);
				}
				else {
					lineIcon.removeCornerArc(LineIcon.LOWER_LEFT);
				}
			}
		});
		p.add(lowerLeftArcButton);

		upperRightArcButton = new JToggleButton(new ImageIcon(Page.class.getResource("images/UpperRightCornerArc.gif")));
		upperRightArcButton.setToolTipText("Draw an arc at the upper-right corner");
		upperRightArcButton.setPreferredSize(dim);
		upperRightArcButton.setFocusPainted(false);
		upperRightArcButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					lineIcon.addCornerArc(LineIcon.UPPER_RIGHT);
				}
				else {
					lineIcon.removeCornerArc(LineIcon.UPPER_RIGHT);
				}
			}
		});
		p.add(upperRightArcButton);

		lowerRightArcButton = new JToggleButton(new ImageIcon(Page.class.getResource("images/LowerRightCornerArc.gif")));
		lowerRightArcButton.setToolTipText("Draw an arc at the lower-right corner");
		lowerRightArcButton.setPreferredSize(dim);
		lowerRightArcButton.setFocusPainted(false);
		lowerRightArcButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					lineIcon.addCornerArc(LineIcon.LOWER_RIGHT);
				}
				else {
					lineIcon.removeCornerArc(LineIcon.LOWER_RIGHT);
				}
			}
		});
		p.add(lowerRightArcButton);

		s = Modeler.getInternationalText("Fill");
		fillCheckBox = new JCheckBox(s != null ? s : "Filled");
		p.add(fillCheckBox);

		panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		getContentPane().add(panel, BorderLayout.CENTER);

		label = new JLabel("Plain text or HTML text:");
		panel.add(label, BorderLayout.NORTH);
		textArea = new PastableTextArea();
		textArea.setBorder(BorderFactory.createLoweredBevelBorder());
		JScrollPane scroller = new JScrollPane(textArea);
		scroller.setPreferredSize(new Dimension(300, 100));
		panel.add(scroller, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(getOwner());

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				option = JOptionPane.CANCEL_OPTION;
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				textArea.requestFocusInWindow();
			}
		});

	}

	void setColorBar(LineIcon lineIcon) {
		this.lineIcon = lineIcon;
		if (lineIcon != null) {
			fillCheckBox.setSelected(lineIcon.isFilled());
			bgComboBox.setColor(lineIcon.getColor());
			widthField.setValue(lineIcon.getWidth());
			heightField.setValue(lineIcon.getHeight());
			topMarginField.setValue(lineIcon.getTopMargin());
			bottomMarginField.setValue(lineIcon.getBottomMargin());
			leftMarginField.setValue(lineIcon.getLeftMargin());
			rightMarginField.setValue(lineIcon.getRightMargin());
			arcWidthField.setValue(lineIcon.getArcWidth());
			arcHeightField.setValue(lineIcon.getArcHeight());
			textArea.setText(lineIcon.getText());
			textArea.setCaretPosition(0);
			ModelerUtilities.setWithoutNotifyingListeners(upperLeftArcButton,
					(lineIcon.getCornerArc() & LineIcon.UPPER_LEFT) == LineIcon.UPPER_LEFT);
			ModelerUtilities.setWithoutNotifyingListeners(upperRightArcButton,
					(lineIcon.getCornerArc() & LineIcon.UPPER_RIGHT) == LineIcon.UPPER_RIGHT);
			ModelerUtilities.setWithoutNotifyingListeners(lowerLeftArcButton,
					(lineIcon.getCornerArc() & LineIcon.LOWER_LEFT) == LineIcon.LOWER_LEFT);
			ModelerUtilities.setWithoutNotifyingListeners(lowerRightArcButton,
					(lineIcon.getCornerArc() & LineIcon.LOWER_RIGHT) == LineIcon.LOWER_RIGHT);
			label.setText("Plain text or HTML text: #"
					+ (lineIcon.getWrapper() != null ? (lineIcon.getWrapper().getIndex() + 1) + "" : ""));
		}
	}

	int getOption() {
		return option;
	}

	private float getInputWidth() {
		float w = widthField.getValue();
		if (w <= 1)
			return w * page.getWidth();
		return w;
	}

	private float getInputHeight() {
		float h = heightField.getValue();
		if (h <= 1)
			return h * page.getHeight();
		return h;
	}

	private boolean confirm() {
		if (lineIcon == null)
			return true;
		if (arcWidthField.getValue() * 2 > getInputWidth()) {
			JOptionPane.showMessageDialog(this, "Arc width cannot be larger than half of total width.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (arcHeightField.getValue() * 2 > getInputHeight()) {
			JOptionPane.showMessageDialog(this, "Arc height cannot be larger than half of total height.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if ((lineIcon.getCornerArc() & LineIcon.UPPER_LEFT) == LineIcon.UPPER_LEFT
				|| (lineIcon.getCornerArc() & LineIcon.UPPER_RIGHT) == LineIcon.UPPER_RIGHT
				|| (lineIcon.getCornerArc() & LineIcon.LOWER_LEFT) == LineIcon.LOWER_LEFT
				|| (lineIcon.getCornerArc() & LineIcon.LOWER_RIGHT) == LineIcon.LOWER_RIGHT) {
			if (arcWidthField.getValue() == 0) {
				JOptionPane.showMessageDialog(this, "Please specify a nonzero arc width.", "Reminder",
						JOptionPane.WARNING_MESSAGE);
			}
			else if (arcHeightField.getValue() == 0) {
				JOptionPane.showMessageDialog(this, "Please specify a nonzero arc height.", "Reminder",
						JOptionPane.WARNING_MESSAGE);
			}
		}
		boolean resized = lineIcon.getIconWidth() != widthField.getValue()
				|| lineIcon.getIconHeight() != heightField.getValue();
		lineIcon.setFilled(fillCheckBox.isSelected());
		lineIcon.setColor(bgComboBox.getSelectedColor());
		lineIcon.setWidth(widthField.getValue());
		lineIcon.setHeight(heightField.getValue());
		lineIcon.setTopMargin(topMarginField.getValue());
		lineIcon.setBottomMargin(bottomMarginField.getValue());
		lineIcon.setLeftMargin(leftMarginField.getValue());
		lineIcon.setRightMargin(rightMarginField.getValue());
		lineIcon.setArcWidth(arcWidthField.getValue());
		lineIcon.setArcHeight(arcHeightField.getValue());
		lineIcon.setText(textArea.getText());
		if (resized) {
			if (lineIcon.getWrapper() != null) {
				lineIcon.getWrapper()
						.setPreferredSize(new Dimension(lineIcon.getIconWidth(), lineIcon.getIconHeight()));
			}
			page.settleComponentSize();
		}
		page.saveReminder.setChanged(true);
		return true;
	}

}