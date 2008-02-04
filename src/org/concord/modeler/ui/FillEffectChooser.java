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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.draw.GradientFactory;
import org.concord.modeler.draw.PatternFactory;
import org.concord.modeler.event.ImageEvent;
import org.concord.modeler.event.ImageImporter;
import org.concord.modeler.util.ImageReader;

public class FillEffectChooser extends JTabbedPane implements ImageImporter {

	private final static int ONE_COLOR = 8531;
	private final static int TWO_COLOR = 8532;
	private final static int PRESET = 8533;
	private final static float[] dash = { 2.0f };
	private final static BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			dash, 0.0f);
	private static ResourceBundle bundle;
	private static boolean isUSLocale;

	private JPanel colorPanel;
	private GradientPanel[] gp;
	private PatternPanel[] pp;
	private JButton inputImageButton;
	private String imageURL;
	private JLabel imageLabel;
	private FillMode fillMode;

	/**
	 * Creates and returns a new dialog containing the specified FillEffectChooser pane along with "OK", "Cancel", and
	 * "Reset" buttons. If the "OK" or "Cancel" buttons are pressed, the dialog is automatically hidden (but not
	 * disposed). If the "Reset" button is pressed, the chooser's selection will be reset to the option which was set
	 * the last time show was invoked on the dialog and the dialog will remain showing.
	 */
	public static JDialog createDialog(Component parent, String title, boolean modal, FillEffectChooser chooser,
			ActionListener okListener, ActionListener cancelListener) {

		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), title == null ? "Fill Effects"
				: title, modal);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		Container container = dialog.getContentPane();
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		container.add(panel, BorderLayout.CENTER);

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 6;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(chooser, c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(new JLabel(), c);

		final ActionListener okListener1 = okListener;
		final FillEffectChooser chooser1 = chooser;

		JButton button = new JButton("OK");
		String s = getInternationalText("OKButton");
		if (s != null)
			button.setText(s);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (chooser1.getSelectedIndex()) {
				case 0:
					GradientPanel gp = chooser1.getSelectedGradientPanel();
					if (gp != null) {
						chooser1.fillMode = new FillMode.GradientFill(gp.getColor1(), gp.getColor2(), gp.getStyle(), gp
								.getVariant());
					}
					break;
				case 1:
					PatternPanel pp = chooser1.getSelectedPatternPanel();
					if (pp != null) {
						chooser1.fillMode = new FillMode.PatternFill(pp.getForeground().getRGB(), pp.getBackground()
								.getRGB(), pp.getStyle(), pp.getCellWidth(), pp.getCellHeight());
					}
					break;
				case 2:
					if (chooser1.imageURL != null) {
						chooser1.fillMode = new FillMode.ImageFill(chooser1.imageURL);
					}
				}
				okListener1.actionPerformed(e);
				dialog.dispose();
			}
		});
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(button, c);

		button = new JButton("Cancel");
		s = getInternationalText("CancelButton");
		if (s != null)
			button.setText(s);
		if (cancelListener != null) {
			button.addActionListener(cancelListener);
		}
		else {
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dialog.dispose();
				}
			});
		}
		c.gridy = 2;
		panel.add(button, c);

		dialog.pack();

		if (parent == null) {
			dialog.setLocation(200, 200);
		}
		else {
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
		}

		return dialog;

	}

	public FillEffectChooser() {

		super();

		if (bundle == null) {
			isUSLocale = Locale.getDefault().equals(Locale.US);
			try {
				bundle = ResourceBundle.getBundle("org.concord.modeler.ui.images.FillEffectChooser", Locale
						.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}

		setPreferredSize(new Dimension(300, 300));

		String s = getInternationalText("GradientTab");
		addTab(s == null ? "Gradient" : s, createGradientPanel());

		s = getInternationalText("PatternTab");
		addTab(s == null ? "Pattern" : s, createPatternPanel());

		s = getInternationalText("ImageTab");
		addTab(s == null ? "Image" : s, createPicturePanel());

	}

	private static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (name == null)
			return null;
		if (isUSLocale)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	private GradientPanel getSelectedGradientPanel() {
		for (GradientPanel p : gp) {
			if (p.isSelected())
				return p;
		}
		return null;
	}

	private PatternPanel getSelectedPatternPanel() {
		for (PatternPanel p : pp) {
			if (p.isSelected())
				return p;
		}
		return null;
	}

	private void setGradientPanelColor1(Color c) {
		for (GradientPanel p : gp)
			p.setColor1(c);
	}

	private void setGradientPanelColor2(Color c) {
		for (GradientPanel p : gp)
			p.setColor2(c);
	}

	private void setPatternPanelForeground(Color c) {
		for (PatternPanel p : pp)
			p.setForeground(c);
	}

	private void setPatternPanelBackground(Color c) {
		for (PatternPanel p : pp)
			p.setBackground(c);
	}

	private void createColorPanel(int type) {

		boolean b = colorPanel.getComponentCount() > 0;
		if (b)
			colorPanel.removeAll();

		switch (type) {

		case ONE_COLOR:

			JPanel p3 = new JPanel(new BorderLayout());
			colorPanel.add(p3, BorderLayout.NORTH);

			String s = getInternationalText("Color1");
			JLabel label = new JLabel((s == null ? "Color 1" : s) + " :");
			p3.add(label, BorderLayout.NORTH);
			ColorComboBox colorComboBox = new ColorComboBox(this);
			colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
			colorComboBox.setToolTipText(s == null ? "Color 1" : s);
			colorComboBox.setSelectedIndex(ColorRectangle.COLORS.length);
			colorComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final ColorComboBox cb = (ColorComboBox) e.getSource();
					int id = cb.getSelectedIndex();
					if (id >= ColorRectangle.COLORS.length + 1) {
						cb.updateColor(new Runnable() {
							public void run() {
								setGradientPanelColor1(cb.getMoreColor());
							}
						});
					}
					else if (id == ColorRectangle.COLORS.length) {
						setGradientPanelColor1(cb.getMoreColor());
					}
					else {
						setGradientPanelColor1(ColorRectangle.COLORS[id]);
					}
				}
			});
			p3.add(colorComboBox, BorderLayout.CENTER);
			label.setLabelFor(colorComboBox);
			setGradientPanelColor1(Color.white);

			JSlider slider = new JSlider(0, 255, 128);
			slider.setToolTipText("Change darkness");
			slider.setPaintLabels(true);
			slider.setPaintTicks(false);
			slider.setPaintTrack(true);
			slider.setSnapToTicks(true);
			slider.setMajorTickSpacing(8);
			slider.setMinorTickSpacing(1);
			slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
			Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			s = getInternationalText("Dark");
			label = new JLabel(s != null ? s : "Dark");
			labels.put(20, label);
			s = getInternationalText("Light");
			label = new JLabel(s != null ? s : "Light");
			labels.put(235, label);
			slider.setLabelTable(labels);
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						int i = source.getValue();
						setGradientPanelColor2(new Color(i, i, i));
					}
				}
			});
			colorPanel.add(slider, BorderLayout.CENTER);
			setGradientPanelColor2(new Color(128, 128, 128));

			break;

		case TWO_COLOR:

			p3 = new JPanel(new BorderLayout());
			colorPanel.add(p3, BorderLayout.NORTH);

			s = getInternationalText("Color1");
			label = new JLabel((s == null ? "Color 1" : s) + " :");
			p3.add(label, BorderLayout.NORTH);
			colorComboBox = new ColorComboBox(this);
			colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
			colorComboBox.setToolTipText(s == null ? "Color 1" : s);
			colorComboBox.setSelectedIndex(ColorRectangle.COLORS.length);
			colorComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final ColorComboBox cb = (ColorComboBox) e.getSource();
					int id = cb.getSelectedIndex();
					if (id >= ColorRectangle.COLORS.length + 1) {
						cb.updateColor(new Runnable() {
							public void run() {
								setGradientPanelColor1(cb.getMoreColor());
							}
						});
					}
					else if (id == ColorRectangle.COLORS.length) {
						setGradientPanelColor1(cb.getMoreColor());
					}
					else {
						setGradientPanelColor1(ColorRectangle.COLORS[id]);
					}
				}
			});
			p3.add(colorComboBox, BorderLayout.CENTER);
			label.setLabelFor(colorComboBox);
			setGradientPanelColor1(Color.white);

			p3 = new JPanel(new BorderLayout());
			colorPanel.add(p3, BorderLayout.CENTER);

			s = getInternationalText("Color2");
			label = new JLabel((s == null ? "Color 2" : s) + " :");
			p3.add(label, BorderLayout.NORTH);
			colorComboBox = new ColorComboBox(this);
			colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
			colorComboBox.setToolTipText(s == null ? "Color 2" : s);
			colorComboBox.setSelectedIndex(0);
			colorComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final ColorComboBox cb = (ColorComboBox) e.getSource();
					int id = cb.getSelectedIndex();
					if (id >= ColorRectangle.COLORS.length + 1) {
						cb.updateColor(new Runnable() {
							public void run() {
								setGradientPanelColor2(cb.getMoreColor());
							}
						});
					}
					else if (id == ColorRectangle.COLORS.length) {
						setGradientPanelColor2(cb.getMoreColor());
					}
					else {
						setGradientPanelColor2(ColorRectangle.COLORS[id]);
					}
				}
			});
			p3.add(colorComboBox, BorderLayout.CENTER);
			label.setLabelFor(colorComboBox);
			setGradientPanelColor2(Color.black);

			break;

		case PRESET:

			p3 = new JPanel(new BorderLayout());
			colorPanel.add(p3, BorderLayout.NORTH);

			s = getInternationalText("PresetColors");
			label = new JLabel((s != null ? s : "Preset colors") + " :");
			p3.add(label, BorderLayout.NORTH);
			JComboBox comboBox = new JComboBox(new String[] { "Early Sunset", "Late Sunset", "Night Fall", "Daybreak" });
			comboBox.setToolTipText("Preset color schemes");
			comboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					switch (((JComboBox) e.getSource()).getSelectedIndex()) {
					case 0:
						setGradientPanelColor1(Color.blue);
						setGradientPanelColor2(Color.red);
						break;
					case 1:
						setGradientPanelColor1(Color.black);
						setGradientPanelColor2(Color.red);
						break;
					case 2:
						setGradientPanelColor1(Color.blue);
						setGradientPanelColor2(Color.black);
						break;
					case 3:
						setGradientPanelColor1(Color.white);
						setGradientPanelColor2(new Color(20, 100, 200));
						break;
					}
				}
			});
			p3.add(comboBox, BorderLayout.CENTER);
			label.setLabelFor(comboBox);
			setGradientPanelColor1(Color.blue);
			setGradientPanelColor2(Color.red);

			break;

		}

		if (b)
			colorPanel.revalidate();

	}

	private JPanel createGradientPanel() {

		gp = new GradientPanel[4];
		gp[0] = new GradientPanel(GradientFactory.VARIANT1);
		gp[0].setSelected(true);
		gp[1] = new GradientPanel(GradientFactory.VARIANT2);
		gp[2] = new GradientPanel(GradientFactory.VARIANT3);
		gp[3] = new GradientPanel(GradientFactory.VARIANT4);

		JPanel p = new JPanel(new BorderLayout(5, 5));

		JPanel p1 = new JPanel(new GridLayout(1, 2, 15, 5));
		String s = getInternationalText("ColorsPanel");
		p1.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Colors", 0, 0));

		JPanel p2 = new JPanel(new BorderLayout());
		p1.add(p2);

		ButtonGroup bg = new ButtonGroup();

		s = getInternationalText("OneColor");
		JRadioButton rb = new JRadioButton(s != null ? s : "One color");
		rb.setSelected(true);
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createColorPanel(ONE_COLOR);
			}
		});
		p2.add(rb, BorderLayout.NORTH);
		bg.add(rb);

		s = getInternationalText("TwoColor");
		rb = new JRadioButton(s != null ? s : "Two color");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createColorPanel(TWO_COLOR);
			}
		});
		p2.add(rb, BorderLayout.CENTER);
		bg.add(rb);

		s = getInternationalText("Preset");
		rb = new JRadioButton(s != null ? s : "Preset");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createColorPanel(PRESET);
			}
		});
		p2.add(rb, BorderLayout.SOUTH);
		bg.add(rb);

		colorPanel = new JPanel(new BorderLayout());
		p1.add(colorPanel);
		createColorPanel(ONE_COLOR);

		p.add(p1, BorderLayout.NORTH);

		p1 = new JPanel(new GridLayout(1, 2, 5, 5));

		p2 = new JPanel(new GridLayout(6, 1, 2, 2));
		s = getInternationalText("ShadingStyles");
		p2.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Shading Styles", 0, 0));
		p1.add(p2);

		bg = new ButtonGroup();

		s = getInternationalText("Horizontal");
		rb = new JRadioButton(s != null ? s : "Horizontal");
		rb.setSelected(true);
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < gp.length; i++)
					gp[i].setStyle(GradientFactory.HORIZONTAL);
			}
		});
		p2.add(rb);
		bg.add(rb);

		s = getInternationalText("Vertical");
		rb = new JRadioButton(s != null ? s : "Vertical");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < gp.length; i++)
					gp[i].setStyle(GradientFactory.VERTICAL);
			}
		});
		p2.add(rb);
		bg.add(rb);

		s = getInternationalText("DiagonalUp");
		rb = new JRadioButton(s != null ? s : "Diagonal up");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < gp.length; i++)
					gp[i].setStyle(GradientFactory.DIAGONAL_UP);
			}
		});
		p2.add(rb);
		bg.add(rb);

		s = getInternationalText("DiagonalDown");
		rb = new JRadioButton(s != null ? s : "Diagonal down");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < gp.length; i++)
					gp[i].setStyle(GradientFactory.DIAGONAL_DOWN);
			}
		});
		p2.add(rb);
		bg.add(rb);

		s = getInternationalText("FromCorner");
		rb = new JRadioButton(s != null ? s : "From corner");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < gp.length; i++)
					gp[i].setStyle(GradientFactory.FROM_CORNER);
			}
		});
		p2.add(rb);
		bg.add(rb);

		s = getInternationalText("FromCenter");
		rb = new JRadioButton(s != null ? s : "From center");
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < gp.length; i++)
					gp[i].setStyle(GradientFactory.FROM_CENTER);
			}
		});
		p2.add(rb);
		bg.add(rb);

		p2 = new JPanel(new GridLayout(2, 2, 1, 1));
		s = getInternationalText("Variants");
		p2.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Variants", 0, 0));
		p1.add(p2);

		for (int i = 0; i < gp.length; i++) {
			p2.add(gp[i]);
			final int ii = i;
			gp[i].addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					for (int j = 0; j < gp.length; j++) {
						gp[j].setSelected(j == ii);
						gp[j].repaint();
					}
				}
			});
		}

		p.add(p1, BorderLayout.CENTER);

		return p;

	}

	private JPanel createPatternPanel() {

		JPanel p = new JPanel(new BorderLayout(10, 10));

		int size = PatternFactory.STYLE_ARRAY.length;
		int grid = (int) Math.sqrt(size + 0.0001);

		JPanel texturePanel = new JPanel(new GridLayout(grid, grid * grid < size ? grid + 1 : grid, 2, 2));
		p.add(texturePanel, BorderLayout.CENTER);

		pp = new PatternPanel[size];
		int cell = 10;
		for (int i = 0; i < pp.length; i++) {
			switch (PatternFactory.SIZE_ARRAY[i]) {
			case PatternFactory.SMALL:
				cell = 4;
				break;
			case PatternFactory.MEDIUM:
				cell = 10;
				break;
			case PatternFactory.LARGE:
				cell = 12;
				break;
			}
			pp[i] = new PatternPanel(PatternFactory.STYLE_ARRAY[i], cell, cell);
			texturePanel.add(pp[i]);
			final int ii = i;
			pp[i].addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					for (int k = 0; k < pp.length; k++) {
						pp[k].setSelected(k == ii);
						pp[k].repaint();
					}
				}
			});
		}
		pp[0].setSelected(true);

		JPanel p4 = new JPanel(new GridLayout(1, 2, 10, 10));
		p4.setBorder(new EmptyBorder(10, 10, 10, 10));
		p.add(p4, BorderLayout.SOUTH);

		JPanel p3 = new JPanel(new BorderLayout());
		p4.add(p3);

		String s = getInternationalText("ForegroundColor");
		JLabel label = new JLabel((s != null ? s : "Foreground Color") + " :");
		p3.add(label, BorderLayout.NORTH);
		ColorComboBox colorComboBox = new ColorComboBox(this);
		colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
		colorComboBox.setToolTipText(s != null ? s : "Foreground color");
		colorComboBox.setSelectedIndex(0);
		colorComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final ColorComboBox cb = (ColorComboBox) e.getSource();
				int id = cb.getSelectedIndex();
				if (id >= ColorRectangle.COLORS.length + 1) {
					cb.updateColor(new Runnable() {
						public void run() {
							setPatternPanelForeground(cb.getMoreColor());
						}
					});
				}
				else if (id == ColorRectangle.COLORS.length) {
					setPatternPanelForeground(cb.getMoreColor());
				}
				else {
					setPatternPanelForeground(ColorRectangle.COLORS[id]);
				}
			}
		});
		p3.add(colorComboBox, BorderLayout.CENTER);
		label.setLabelFor(colorComboBox);

		p3 = new JPanel(new BorderLayout());
		p4.add(p3);

		s = getInternationalText("BackgroundColor");
		label = new JLabel((s != null ? s : "Background Color") + " :");
		p3.add(label, BorderLayout.NORTH);
		colorComboBox = new ColorComboBox(this);
		colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell());
		colorComboBox.setToolTipText(s != null ? s : "Background color");
		colorComboBox.setSelectedIndex(ColorRectangle.COLORS.length);
		colorComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final ColorComboBox cb = (ColorComboBox) e.getSource();
				int id = cb.getSelectedIndex();
				if (id >= ColorRectangle.COLORS.length + 1) {
					cb.updateColor(new Runnable() {
						public void run() {
							setPatternPanelBackground(cb.getMoreColor());
						}
					});
				}
				else if (id == ColorRectangle.COLORS.length) {
					setPatternPanelBackground(cb.getMoreColor());
				}
				else {
					setPatternPanelBackground(ColorRectangle.COLORS[id]);
				}
			}
		});
		p3.add(colorComboBox, BorderLayout.CENTER);
		label.setLabelFor(colorComboBox);

		return p;

	}

	private JPanel createPicturePanel() {

		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.setBorder(new EmptyBorder(5, 5, 5, 5));
		ImagePreview ip = new ImagePreview(ModelerUtilities.fileChooser);
		p.add(ip, BorderLayout.CENTER);

		String s = getInternationalText("ImageLocation");
		imageLabel = new JLabel((s != null ? s : "Image Location") + " :");

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		inputImageButton = new JButton("Select Image");
		p1.add(inputImageButton);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.add(p1, BorderLayout.SOUTH);
		p2.add(imageLabel, BorderLayout.CENTER);

		p.add(p2, BorderLayout.SOUTH);

		s = getInternationalText("ImageTab");
		p.add(new JLabel((s != null ? s : "Image") + " :"), BorderLayout.NORTH);

		return p;

	}

	class PatternPanel extends JPanel {

		private boolean selected;
		private byte style = PatternFactory.POLKA;
		private int cellWidth = 12;
		private int cellHeight = 12;

		public PatternPanel(byte style, int cellWidth, int cellHeight) {
			setBackground(Color.white);
			setForeground(Color.black);
			this.style = style;
			this.cellWidth = cellWidth;
			this.cellHeight = cellHeight;
		}

		public void setStyle(byte i) {
			style = i;
		}

		public byte getStyle() {
			return style;
		}

		public void setCellWidth(int i) {
			cellWidth = i;
		}

		public int getCellWidth() {
			return cellWidth;
		}

		public void setCellHeight(int i) {
			cellHeight = i;
		}

		public int getCellHeight() {
			return cellHeight;
		}

		public void setSelected(boolean b) {
			selected = b;
		}

		public boolean isSelected() {
			return selected;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			int w = getWidth();
			int h = getHeight();
			g2d.setPaint(PatternFactory.createPattern(style, cellWidth, cellHeight, getForeground(), getBackground()));
			g2d.fillRect(0, 0, w, h);
			if (selected) {
				g2d.setStroke(dashed);
				g2d.setColor(Color.white);
				g2d.drawRect(2, 2, w - 5, h - 5);
			}
		}

	}

	class GradientPanel extends JPanel {

		private Color color1 = Color.white;
		private Color color2 = new Color(128, 128, 128);
		private int style = GradientFactory.HORIZONTAL;
		private int variant = GradientFactory.VARIANT1;
		private boolean selected;

		public void setColor1(Color c) {
			if (color1.equals(c))
				return;
			color1 = c;
			repaint();
		}

		public void setColor2(Color c) {
			if (color2.equals(c))
				return;
			color2 = c;
			repaint();
		}

		public void setVariant(int i) {
			if (variant == i)
				return;
			variant = i;
		}

		public void setStyle(int i) {
			if (style == i)
				return;
			style = i;
			repaint();
		}

		public int getStyle() {
			return style;
		}

		public int getVariant() {
			return variant;
		}

		public Color getColor1() {
			return color1;
		}

		public Color getColor2() {
			return color2;
		}

		public void setSelected(boolean b) {
			selected = b;
		}

		public boolean isSelected() {
			return selected;
		}

		public GradientPanel(int variant) {
			this.variant = variant;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			int w = getWidth();
			int h = getHeight();
			GradientFactory.paintRect(g2d, style, variant, color1, color2, 0, 0, w, h);
			if (selected) {
				g2d.setStroke(dashed);
				g2d.setColor(Color.white);
				g2d.drawRect(3, 3, w - 6, h - 6);
			}
		}

	}

	public void imageImported(ImageEvent e) {
		imageURL = e.getPath();
		imageLabel.setText("Image Location: " + imageURL);
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setFillMode(FillMode fm) {
		fillMode = fm;
	}

	public void setImageReader(ImageReader ir) {
		ir.addImageImporter(this);
		inputImageButton.setAction(ir);
		inputImageButton.setIcon(new ImageIcon(FillEffectChooser.class.getResource("images/open.gif")));
		String s = getInternationalText("InputImage");
		if (s != null) {
			inputImageButton.setText(s);
			ir.putValue("i18n", s);
		}
	}

}
