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

package org.concord.modeler.draw.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.DefaultTextContainer;
import org.concord.modeler.draw.TextContainer;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;

public class TextBoxPanel extends PropertiesPanel {

	protected TextContainer textBox;

	private JComboBox fontNameComboBox, fontSizeComboBox;
	private ColorComboBox fgComboBox;
	private BackgroundComboBox bgComboBox;
	private JComboBox borderComboBox;
	private JComboBox shadowComboBox;
	private JToggleButton boldButton, italicButton;
	private JCheckBox callOutCheckBox;
	private JSpinner angleSpinner;
	private JTextArea textArea;
	private TextContainer savedCopy;

	public TextBoxPanel(TextContainer c) {

		super(new BorderLayout(5, 5));

		if (c == null)
			throw new IllegalArgumentException("input cannot be null");
		localize();
		textBox = c;
		storeSettings();

		textArea = new JTextArea(textBox.getText(), 5, 10);
		textArea.setForeground(textBox.getForegroundColor());
		textArea.setFont(textBox.getFont());
		textArea.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				textBox.setText(textArea.getText());
				textBox.getComponent().repaint();
			}
		});

		add(new JScrollPane(textArea), BorderLayout.CENTER);

		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(p, BorderLayout.NORTH);

		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p.add(p2, BorderLayout.NORTH);

		String s = getInternationalText("TextBox");
		JLabel label = new JLabel("  " + (s != null ? s : "Text Box") + " #" + getIndex() + ", at ("
				+ (int) textBox.getRx() + ", " + (int) textBox.getRy() + ") ");
		label.setBackground(SystemColor.controlLtHighlight);
		label.setOpaque(true);
		label.setBorder(BorderFactory.createLineBorder(SystemColor.controlDkShadow));
		p2.add(label);

		s = getInternationalText("FontType");
		p2.add(new JLabel((s != null ? s : "Font type") + ":"));
		fontNameComboBox = ModelerUtilities.createFontNameComboBox();
		fontNameComboBox.setSelectedItem(textBox.getFont().getFamily());
		fontNameComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Font f = textBox.getFont();
				f = new Font((String) fontNameComboBox.getSelectedItem(), f.getStyle(), f.getSize());
				textBox.setFont(f);
				textArea.setFont(f);
				textBox.getComponent().repaint();
			}
		});
		p2.add(fontNameComboBox);

		s = getInternationalText("FontSize");
		p2.add(new JLabel((s != null ? s : "Size") + ":"));
		fontSizeComboBox = ModelerUtilities.createFontSizeComboBox(60);
		fontSizeComboBox.setSelectedItem(new Integer(textBox.getFont().getSize()));
		fontSizeComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Font f = textBox.getFont();
				f = new Font(f.getFontName(), f.getStyle(), ((Integer) fontSizeComboBox.getSelectedItem()).intValue());
				textBox.setFont(f);
				textArea.setFont(f);
				textBox.getComponent().repaint();
			}
		});
		p2.add(fontSizeComboBox);

		s = getInternationalText("FontColor");
		p2.add(new JLabel((s != null ? s : "Color") + ":"));
		fgComboBox = new ColorComboBox(this);
		fgComboBox.setColor(textBox.getForegroundColor());
		fgComboBox.setRenderer(new ComboBoxRenderer.ColorCell(textBox.getForegroundColor()));
		fgComboBox.setToolTipText("Font color");
		fgComboBox.setPreferredSize(new Dimension(80, fontSizeComboBox.getPreferredSize().height));
		fgComboBox.setAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (fgComboBox.getSelectedIndex() >= ColorRectangle.COLORS.length + 1) {
					fgComboBox.updateColor(new Runnable() {
						public void run() {
							textBox.setForegroundColor(fgComboBox.getMoreColor());
							textBox.getComponent().repaint();
							textArea.setForeground(textBox.getForegroundColor());
						}
					});
				}
				else {
					textBox.setForegroundColor(fgComboBox.getSelectedColor());
					textBox.getComponent().repaint();
					textArea.setForeground(textBox.getForegroundColor());
				}
			}
		});
		p2.add(fgComboBox);

		ActionListener styleListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int style = (boldButton.isSelected() ? Font.BOLD : Font.PLAIN)
						| (italicButton.isSelected() ? Font.ITALIC : Font.PLAIN);
				Font f = textBox.getFont();
				f = new Font(f.getFamily(), style, f.getSize());
				textBox.setFont(f);
				textArea.setFont(f);
				textBox.getComponent().repaint();
			}
		};

		boldButton = new JToggleButton(new ImageIcon(TextBoxPanel.class.getResource("resources/Bold.gif")));
		boldButton.setSelected((textBox.getFont().getStyle() & Font.BOLD) == Font.BOLD);
		boldButton.setPreferredSize(new Dimension(20, fontSizeComboBox.getPreferredSize().height));
		boldButton.addActionListener(styleListener);
		p2.add(boldButton);

		italicButton = new JToggleButton(new ImageIcon(TextBoxPanel.class.getResource("resources/Italic.gif")));
		italicButton.setSelected((textBox.getFont().getStyle() & Font.ITALIC) == Font.ITALIC);
		italicButton.setPreferredSize(boldButton.getPreferredSize());
		italicButton.addActionListener(styleListener);
		p2.add(italicButton);

		int wp2 = p2.getPreferredSize().width;

		p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		p.add(p2, BorderLayout.CENTER);

		s = getInternationalText("Callout");
		callOutCheckBox = new JCheckBox(s != null ? s : "Callout");
		callOutCheckBox.setSelected(textBox.isCallOut());
		callOutCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				textBox.setCallOut(e.getStateChange() == ItemEvent.SELECTED);
				textBox.getComponent().repaint();
			}
		});
		p2.add(callOutCheckBox);

		s = getInternationalText("Rotation");
		p2.add(new JLabel("<html>" + (s != null ? s : "Rotation") + " (&deg;)</html>", JLabel.LEFT));
		angleSpinner = new JSpinner(new SpinnerNumberModel(0, -180, 180, 1));
		angleSpinner.setValue((int) textBox.getAngle());
		angleSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				textBox.setAngle((Integer) angleSpinner.getValue());
				textBox.getComponent().repaint();
			}
		});
		p2.add(angleSpinner);

		s = getInternationalText("Fill");
		p2.add(new JLabel((s != null ? s : "Fill") + ":"));
		bgComboBox = new BackgroundComboBox(this, ModelerUtilities.colorChooser, ModelerUtilities.fillEffectChooser);
		bgComboBox.setToolTipText("Background filling");
		bgComboBox.setFillMode(textBox.getFillMode());
		bgComboBox.setPreferredSize(new Dimension(bgComboBox.getPreferredSize().width, fontNameComboBox
				.getPreferredSize().height));
		bgComboBox.getColorMenu().setNoFillAction(new AbstractAction("No Fill") {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = FillMode.getNoFillMode();
				if (fm.equals(textBox.getFillMode()))
					return;
				textBox.setFillMode(fm);
				textBox.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setColorArrayAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColor());
				if (fm.equals(textBox.getFillMode()))
					return;
				textBox.setFillMode(fm);
				textBox.getComponent().repaint();
				notifyChange();
			}
		});
		bgComboBox.getColorMenu().setMoreColorAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				if (fm.equals(textBox.getFillMode()))
					return;
				textBox.setFillMode(fm);
				textBox.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						textBox.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) textBox
								.getFillMode()).getColor() : null);
				if (c == null)
					return;
				FillMode fm = new FillMode.ColorFill(c);
				if (fm.equals(textBox.getFillMode()))
					return;
				textBox.setFillMode(fm);
				textBox.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setFillEffectActions(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = bgComboBox.getColorMenu().getFillEffectChooser().getFillMode();
				if (fm.equals(textBox.getFillMode()))
					return;
				textBox.setFillMode(fm);
				textBox.getComponent().repaint();
				notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		}, null);
		p2.add(bgComboBox);

		s = getInternationalText("Border");
		p2.add(new JLabel((s != null ? s : "Border") + ":"));
		String[] borderOption = new String[] { "None", "Rectangle", "Rounded Rectangle" };
		for (int i = 0; i < 2; i++) {
			s = getInternationalText(borderOption[i]);
			if (s != null)
				borderOption[i] = s;
		}
		s = getInternationalText("RoundedRectangle");
		if (s != null)
			borderOption[2] = s;
		borderComboBox = new JComboBox(borderOption);
		borderComboBox.setSelectedIndex(textBox.getBorderType());
		borderComboBox.setPreferredSize(new Dimension(borderComboBox.getPreferredSize().width, fontNameComboBox
				.getPreferredSize().height));
		borderComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textBox.setBorderType((byte) borderComboBox.getSelectedIndex());
				textBox.getComponent().repaint();
			}
		});
		p2.add(borderComboBox);

		s = getInternationalText("Shadow");
		p2.add(new JLabel((s != null ? s : "Shadow") + ":"));
		String[] shadowOption = new String[] { "None", "Lower Right", "Lower Left", "Upper Right", "Upper Left" };
		s = getInternationalText("None");
		if (s != null)
			shadowOption[0] = s;
		s = getInternationalText("LowerRight");
		if (s != null)
			shadowOption[1] = s;
		s = getInternationalText("LowerLeft");
		if (s != null)
			shadowOption[2] = s;
		s = getInternationalText("UpperRight");
		if (s != null)
			shadowOption[3] = s;
		s = getInternationalText("UpperLeft");
		if (s != null)
			shadowOption[4] = s;
		shadowComboBox = new JComboBox(shadowOption);
		shadowComboBox.setSelectedIndex(textBox.getShadowType());
		if (wp2 > p2.getPreferredSize().width) {
			shadowComboBox.setPreferredSize(new Dimension(wp2 - p2.getPreferredSize().width - 5, fontNameComboBox
					.getPreferredSize().height));
		}
		shadowComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textBox.setShadowType((byte) shadowComboBox.getSelectedIndex());
				textBox.getComponent().repaint();
			}
		});
		p2.add(shadowComboBox);

		// p2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		// p.add(p2, BorderLayout.SOUTH);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(p, BorderLayout.SOUTH);

		s = getInternationalText("HTMLNotSupported");
		p.add(new JLabel(s != null ? s : "Note: HTML is not supported."));

		s = getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				notifyChange();
				textBox.setText(textArea.getText());
				textBox.getComponent().repaint();
				if (dialog != null) {
					offset = dialog.getLocationOnScreen();
					dialog.dispose();
				}
			}
		});
		p.add(button);

		s = getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				restoreSettings();
				textBox.getComponent().repaint();
				cancelled = true;
				if (dialog != null) {
					offset = dialog.getLocationOnScreen();
					dialog.dispose();
				}
			}
		});
		p.add(button);

	}

	public void setDialog(JDialog d) {
		super.setDialog(d);
		if (dialog != null) {
			String s = getInternationalText("TextBoxProperties");
			if (s != null)
				dialog.setTitle(s);
		}
	}

	public void storeSettings() {
		if (savedCopy == null)
			savedCopy = new DefaultTextContainer();
		savedCopy.setTextContainer(textBox);
	}

	public void restoreSettings() {
		if (savedCopy == null)
			return;
		textBox.setTextContainer(savedCopy);
	}

	public void windowActivated() {
		textArea.selectAll();
		textArea.requestFocus();
	}

}
