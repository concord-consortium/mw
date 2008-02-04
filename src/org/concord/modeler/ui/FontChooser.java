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

package org.concord.modeler.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.concord.modeler.ModelerUtilities;

public class FontChooser extends JComponent {

	private final static String previewText = "Preview Font";
	private final static String[] htmlTag = new String[] { "<html>", "</html>" };
	private final static String[] underlineTag = new String[] { "<u>", "</u>" };
	private final static String[] strikeoutTag = new String[] { "<s>", "</s>" };
	private final static String[] subTag = new String[] { "<sub>", "</sub>" };
	private final static String[] supTag = new String[] { "<sup>", "</sup>" };
	private final static Font font = new Font("Arial", Font.PLAIN, 11);

	protected int option = JOptionPane.CLOSED_OPTION;
	protected OpenList lstFontName;
	protected OpenList lstFontSize;
	protected JCheckBox checkBoxBold;
	protected JCheckBox checkBoxItalic;
	protected JCheckBox checkBoxUnderline;
	protected JCheckBox checkBoxStrikethrough;
	protected JCheckBox checkBoxSubscript;
	protected JCheckBox checkBoxSuperscript;
	protected ColorComboBox cbColor;
	protected JLabel preview;

	public void enableUnderline(boolean b) {
		checkBoxUnderline.setEnabled(b);
	}

	public void enableStrikethrough(boolean b) {
		checkBoxStrikethrough.setEnabled(b);
	}

	public void enableSubscript(boolean b) {
		checkBoxSubscript.setEnabled(b);
	}

	public void enableSuperscript(boolean b) {
		checkBoxSuperscript.setEnabled(b);
	}

	public void enableFontColor(boolean b) {
		cbColor.setEnabled(b);
	}

	public boolean isBold() {
		return checkBoxBold.isSelected();
	}

	public void setBold(boolean b) {
		checkBoxBold.setSelected(b);
	}

	public boolean isItalic() {
		return checkBoxItalic.isSelected();
	}

	public void setItalic(boolean b) {
		checkBoxItalic.setSelected(b);
	}

	public boolean isUnderline() {
		return checkBoxUnderline.isSelected();
	}

	public void setUnderline(boolean b) {
		checkBoxUnderline.setSelected(b);
	}

	public int getFontStyle() {
		int i = isBold() ? Font.BOLD : Font.PLAIN;
		if (isItalic())
			i = i | Font.ITALIC;
		return i;
	}

	public String getFontName() {
		return lstFontName.getSelected();
	}

	public void setFontName(String s) {
		lstFontName.setSelected(s);
	}

	public int getFontSize() {
		return Math.max(lstFontSize.getSelectedInt(), ModelerUtilities.FONT_SIZE[0]);
	}

	public void setFontSize(int i) {
		lstFontSize.setSelectedInt(i);
	}

	public Color getFontColor() {
		return cbColor.getSelectedColor();
	}

	public void setFontColor(Color c) {
		cbColor.setColor(c);
	}

	public FontChooser() {

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel p = new JPanel(new GridLayout(1, 2, 10, 2));
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Font", 0, 0,
				new Font("Arial", Font.PLAIN, 11), Color.black));
		lstFontName = new OpenList(ModelerUtilities.FONT_FAMILY_NAMES, "Name:");
		p.add(lstFontName);

		lstFontSize = new OpenList(ModelerUtilities.FONT_SIZE, "Size:");
		p.add(lstFontSize);
		add(p);

		p = new JPanel(new GridLayout(2, 3, 10, 5));
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Effects", 0, 0, new Font("Arial", Font.PLAIN,
				11), Color.black));
		checkBoxBold = new JCheckBox("Bold");
		checkBoxBold.setFont(font);
		p.add(checkBoxBold);
		checkBoxItalic = new JCheckBox("Italic");
		checkBoxItalic.setFont(font);
		p.add(checkBoxItalic);

		checkBoxUnderline = new JCheckBox("Underline");
		checkBoxUnderline.setFont(font);
		p.add(checkBoxUnderline);
		checkBoxStrikethrough = new JCheckBox("Strikethrough");
		checkBoxStrikethrough.setFont(font);
		p.add(checkBoxStrikethrough);

		checkBoxSubscript = new JCheckBox("Subscript");
		checkBoxSubscript.setFont(font);
		p.add(checkBoxSubscript);
		checkBoxSuperscript = new JCheckBox("Superscript");
		checkBoxSuperscript.setFont(font);
		p.add(checkBoxSuperscript);
		add(p);

		add(Box.createVerticalStrut(5));
		p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		p.add(new JLabel("Color:"));

		cbColor = new ColorComboBox(this);
		cbColor.setRenderer(new ComboBoxRenderer.ColorCell());
		cbColor.setPreferredSize(new Dimension(150, 20));
		cbColor.setMaximumSize(new Dimension(150, 20));
		cbColor.setSelectedIndex(0);
		cbColor.setRequestFocusEnabled(false);
		p.add(cbColor);
		p.add(Box.createHorizontalStrut(10));
		add(p);

		p = new JPanel(new BorderLayout());
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Preview", 0, 0, new Font("Arial", Font.PLAIN,
				11), Color.black));
		preview = new JLabel(previewText);
		preview.setHorizontalAlignment(JLabel.CENTER);
		preview.setBackground(Color.white);
		preview.setForeground(Color.black);
		preview.setOpaque(true);
		preview.setBorder(new LineBorder(Color.black));
		preview.setPreferredSize(new Dimension(120, 40));
		p.add(preview, BorderLayout.CENTER);
		add(p);

		ListSelectionListener lsel = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				updatePreview();
			}
		};
		lstFontName.addListSelectionListener(lsel);
		lstFontSize.addListSelectionListener(lsel);

		ActionListener lst = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updatePreview();
			}
		};

		checkBoxUnderline.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (checkBoxStrikethrough.isSelected()) {
						checkBoxStrikethrough.setSelected(false);
					}
				}
			}
		});

		checkBoxStrikethrough.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (checkBoxUnderline.isSelected()) {
						checkBoxUnderline.setSelected(false);
					}
				}
			}
		});

		checkBoxSubscript.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (checkBoxSuperscript.isSelected()) {
						checkBoxSuperscript.setSelected(false);
					}
				}
			}
		});

		checkBoxSuperscript.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (checkBoxSubscript.isSelected()) {
						checkBoxSubscript.setSelected(false);
					}
				}
			}
		});

		checkBoxBold.addActionListener(lst);
		checkBoxItalic.addActionListener(lst);
		cbColor.addActionListener(lst);
		checkBoxUnderline.addActionListener(lst);
		checkBoxStrikethrough.addActionListener(lst);
		checkBoxSubscript.addActionListener(lst);
		checkBoxSuperscript.addActionListener(lst);

	}

	public int getOption() {
		return option;
	}

	protected void updatePreview() {
		String name = lstFontName.getSelected();
		int size = lstFontSize.getSelectedInt();
		if (size <= 0)
			return;
		int style = Font.PLAIN;
		if (checkBoxBold.isSelected())
			style |= Font.BOLD;
		if (checkBoxItalic.isSelected())
			style |= Font.ITALIC;
		String s = htmlTag[0];
		if (checkBoxSubscript.isSelected()) {
			s += subTag[0];
		}
		else if (checkBoxSuperscript.isSelected()) {
			s += supTag[0];
		}
		if (checkBoxUnderline.isSelected()) {
			s += underlineTag[0];
		}
		else if (checkBoxStrikethrough.isSelected()) {
			s += strikeoutTag[0];
		}
		s += previewText;
		if (checkBoxStrikethrough.isSelected()) {
			s += strikeoutTag[1];
		}
		else if (checkBoxUnderline.isSelected()) {
			s += underlineTag[1];
		}
		if (checkBoxSuperscript.isSelected()) {
			s += supTag[1];
		}
		else if (checkBoxSubscript.isSelected()) {
			s += subTag[1];
		}
		s += htmlTag[1];
		preview.setText(s);

		preview.setFont(new Font(name, style, size));

		int i = ((Integer) cbColor.getSelectedItem()).intValue();
		preview.setForeground(ColorRectangle.COLORS[i]);
		preview.repaint();
	}

}
