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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.OpenList;

class FontDialog extends JDialog {

	private final static String previewText = "Preview Font";
	private final static String[] htmlTag = new String[] { "<html>", "</html>" };
	private final static String[] underlineTag = new String[] { "<u>", "</u>" };
	private final static String[] strikeoutTag = new String[] { "<s>", "</s>" };
	private final static String[] subTag = new String[] { "<sub>", "</sub>" };
	private final static String[] supTag = new String[] { "<sup>", "</sup>" };

	private int option = JOptionPane.CLOSED_OPTION;
	private OpenList lstFontName;
	private OpenList lstFontSize;
	private MutableAttributeSet attributes;
	private JCheckBox checkBoxBold;
	private JCheckBox checkBoxItalic;
	private JCheckBox checkBoxUnderline;
	private JCheckBox checkBoxStrikethrough;
	private JCheckBox checkBoxSubscript;
	private JCheckBox checkBoxSuperscript;
	private ColorComboBox cbColor;
	private JLabel preview;
	private ActionListener fontColorAction;

	FontDialog(final Page page) {

		super(JOptionPane.getFrameForComponent(page), "Font", true);
		String s = Modeler.getInternationalText("FontDialogTitle");
		if (s != null)
			setTitle(s);
		setResizable(false);

		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		fontColorAction = new FontColorAction();

		JPanel p = new JPanel(new GridLayout(1, 2, 10, 2));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		s = Modeler.getInternationalText("FontName");
		lstFontName = new OpenList(ModelerUtilities.FONT_FAMILY_NAMES, (s != null ? s : "Name") + ":");
		p.add(lstFontName);

		s = Modeler.getInternationalText("FontSize");
		lstFontSize = new OpenList(ModelerUtilities.FONT_SIZE, (s != null ? s : "Size") + ":");
		p.add(lstFontSize);
		getContentPane().add(p);

		p = new JPanel(new GridLayout(2, 3, 10, 5));
		s = Modeler.getInternationalText("FontEffects");
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Effects"));

		s = Modeler.getInternationalText("Bold");
		checkBoxBold = new JCheckBox(s != null ? s : "Bold");
		p.add(checkBoxBold);

		s = Modeler.getInternationalText("Italic");
		checkBoxItalic = new JCheckBox(s != null ? s : "Italic");
		p.add(checkBoxItalic);

		s = Modeler.getInternationalText("Underline");
		checkBoxUnderline = new JCheckBox(s != null ? s : "Underline");
		p.add(checkBoxUnderline);

		s = Modeler.getInternationalText("Strikethrough");
		checkBoxStrikethrough = new JCheckBox(s != null ? s : "Strikethrough");
		p.add(checkBoxStrikethrough);

		s = Modeler.getInternationalText("Subscript");
		checkBoxSubscript = new JCheckBox(s != null ? s : "Subscript");
		p.add(checkBoxSubscript);

		s = Modeler.getInternationalText("Superscript");
		checkBoxSuperscript = new JCheckBox(s != null ? s : "Superscript");
		p.add(checkBoxSuperscript);
		getContentPane().add(p);

		getContentPane().add(Box.createVerticalStrut(5));
		p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		s = Modeler.getInternationalText("Color");
		p.add(new JLabel((s != null ? s : "Color") + ":"));

		final StyledEditorKit.ForegroundAction[] fga = new StyledEditorKit.ForegroundAction[ColorRectangle.COLORS.length + 1];
		for (int i = 0; i < fga.length - 1; i++) {
			fga[i] = new StyledEditorKit.ForegroundAction(ColorRectangle.COLORS[i].toString(), ColorRectangle.COLORS[i]);
		}

		cbColor = new ColorComboBox(page);
		cbColor.setRenderer(new ComboBoxRenderer.ColorCell());
		cbColor.setPreferredSize(new Dimension(150, 20));
		cbColor.setMaximumSize(new Dimension(150, 20));
		cbColor.setSelectedIndex(0);
		cbColor.setRequestFocusEnabled(false);
		cbColor.addActionListener(fontColorAction);
		p.add(cbColor);
		p.add(Box.createHorizontalStrut(10));
		getContentPane().add(p);

		p = new JPanel(new BorderLayout());
		s = Modeler.getInternationalText("Preview");
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Preview"));
		preview = new JLabel(previewText);
		preview.setHorizontalAlignment(JLabel.CENTER);
		preview.setBackground(Color.white);
		preview.setForeground(Color.black);
		preview.setOpaque(true);
		preview.setBorder(new LineBorder(Color.black));
		preview.setPreferredSize(new Dimension(120, 40));
		p.add(preview, BorderLayout.CENTER);
		getContentPane().add(p);

		p = new JPanel(new FlowLayout());
		JPanel p1 = new JPanel(new GridLayout(1, 2, 10, 2));
		s = Modeler.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				option = JOptionPane.OK_OPTION;
				setVisible(false);
			}
		});
		p1.add(button);

		s = Modeler.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				option = JOptionPane.CANCEL_OPTION;
				setVisible(false);
			}
		});
		p1.add(button);
		p.add(p1);
		getContentPane().add(p);

		pack();
		setLocationRelativeTo(getOwner());

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

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				page.requestFocus();
				FontDialog.this.dispose();
			}

			public void windowActivated(WindowEvent e) {
				if (page.isEditable()) {
					lstFontName.getTextField().selectAll();
					lstFontName.getTextField().requestFocus();
				}
			}
		});

	}

	void setAttributes(AttributeSet a) {

		attributes = new SimpleAttributeSet();

		/***************************************************************************************************************
		 * IMPORTANT: remove component and icon attributes from the MutableAttributeSet. * This does not affect this
		 * function of dialog box, because it changes only the textual * attributes of the selected text. In particle,
		 * the strange $ename attribute must be remove as well. *
		 **************************************************************************************************************/

		java.util.Enumeration e = a.getAttributeNames();
		Object name, attr;
		while (e.hasMoreElements()) {
			name = e.nextElement();
			attr = a.getAttribute(name);
			if (!name.toString().equals(StyleConstants.ComponentElementName.toString())
					&& !name.toString().equals(StyleConstants.IconElementName.toString())
					&& !name.toString().equals("$ename")) {
				attributes.addAttribute(name, attr);
			}
		}

		String fontName = StyleConstants.getFontFamily(a);
		lstFontName.setSelected(fontName);
		int size = StyleConstants.getFontSize(a);
		lstFontSize.setSelectedInt(size);
		checkBoxBold.setSelected(StyleConstants.isBold(a));
		checkBoxItalic.setSelected(StyleConstants.isItalic(a));
		checkBoxUnderline.setSelected(StyleConstants.isUnderline(a));
		checkBoxStrikethrough.setSelected(StyleConstants.isStrikeThrough(a));
		checkBoxSubscript.setSelected(StyleConstants.isSubscript(a));
		checkBoxSuperscript.setSelected(StyleConstants.isSuperscript(a));

		Color foreground = StyleConstants.getForeground(a);
		int k = ColorRectangle.COLORS.length, i;
		for (i = 0; i < ColorRectangle.COLORS.length; i++) {
			if (foreground.equals(ColorRectangle.COLORS[i]))
				k = i;
		}
		if (k == ColorRectangle.COLORS.length) {
			((ColorRectangle) cbColor.getRenderer()).setMoreColor(foreground);
		}
		cbColor.removeActionListener(fontColorAction);
		cbColor.setSelectedIndex(k);
		cbColor.addActionListener(fontColorAction);

		updatePreview();

	}

	AttributeSet getAttributes() {
		if (attributes == null)
			return null;
		setAttributeSet(attributes);
		return attributes;
	}

	private void setAttributeSet(MutableAttributeSet mas) {

		if (mas == null)
			return;

		StyleConstants.setFontFamily(mas, lstFontName.getSelected());
		StyleConstants.setFontSize(mas, lstFontSize.getSelectedInt());
		StyleConstants.setBold(mas, checkBoxBold.isSelected());
		StyleConstants.setItalic(mas, checkBoxItalic.isSelected());
		StyleConstants.setUnderline(mas, checkBoxUnderline.isSelected());
		StyleConstants.setStrikeThrough(mas, checkBoxStrikethrough.isSelected());
		StyleConstants.setSubscript(mas, checkBoxSubscript.isSelected());
		StyleConstants.setSuperscript(mas, checkBoxSuperscript.isSelected());
		int i = ((Integer) cbColor.getSelectedItem()).intValue();
		if (i < ColorRectangle.COLORS.length)
			StyleConstants.setForeground(mas, ColorRectangle.COLORS[i]);
		else StyleConstants.setForeground(mas, cbColor.getMoreColor());

		// these do not seem to work!!!
		mas.removeAttribute(StyleConstants.ComponentElementName);
		mas.removeAttribute(StyleConstants.IconElementName);

	}

	int getOption() {
		return option;
	}

	private void updatePreview() {

		String name = lstFontName.getSelected();
		int size = lstFontSize.getSelectedInt();
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
		if (i < ColorComboBox.INDEX_MORE_COLOR) {
			preview.setForeground(ColorRectangle.COLORS[i]);
		}
		else {
			preview.setForeground(cbColor.getMoreColor());
		}
		preview.repaint();

	}

	private class FontColorAction implements ActionListener {

		StyledEditorKit.ForegroundAction[] fga = new StyledEditorKit.ForegroundAction[ColorRectangle.COLORS.length + 1];

		FontColorAction() {
			for (int i = 0; i < fga.length - 1; i++) {
				fga[i] = new StyledEditorKit.ForegroundAction(ColorRectangle.COLORS[i].toString(),
						ColorRectangle.COLORS[i]);
			}
		}

		public void actionPerformed(ActionEvent e) {
			final ColorComboBox cb = (ColorComboBox) e.getSource();
			if (cb.getSelectedIndex() >= fga.length) {
				cb.updateColor(new Runnable() {
					public void run() {
						fga[fga.length - 1] = new StyledEditorKit.ForegroundAction(cb.getMoreColor().toString(), cb
								.getMoreColor());
						fga[fga.length - 1].actionPerformed(null);
					}
				});
				return;
			}
			if (cb.getSelectedIndex() == fga.length - 1) {
				fga[fga.length - 1] = new StyledEditorKit.ForegroundAction(cb.getMoreColor().toString(), cb
						.getMoreColor());
			}
			fga[cb.getSelectedIndex()].actionPerformed(e);
		}

	}

}