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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.UserAction;

public class GayBerneConfigure extends JPanel {

	public final static String GB_COLOR = "GB color";
	public final static double GB_LIMIT = 0.1;

	public static double eeVsEs = 1.0;
	public static double epsilon0 = 0.01;
	public static int breadth = 20, length = 60;
	public static double theta = 0.0;
	public static Color color = Color.blue;

	private JSlider slider1, slider2;
	private JLabel eAnisotropy, epsilon0_label;
	private EllipsePanel ellipsePanel;
	private ColorComboBox colorComboBox;
	private static Cursor rotateCursor1, rotateCursor2, rotateCursor3;

	private final static Font littleFont = new Font(null, Font.PLAIN, 10);
	private final static DecimalFormat scientificFormat = new DecimalFormat();

	public JDialog showDialog(Frame owner) {

		colorComboBox.setParent(owner);

		final JDialog d = new JDialog(owner, "Customize Gay-Berne Particle", true);
		d.setResizable(false);
		d.setLocation(230, 200);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		d.getContentPane().add(p, BorderLayout.SOUTH);

		String s = MDContainer.getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.removeAll();
				d.dispose();
			}
		});
		p.add(button);
		s = MDContainer.getInternationalText("ResetButton");
		button = new JButton(s != null ? s : "Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});
		p.add(button);

		d.getContentPane().add(this, BorderLayout.CENTER);

		d.pack();

		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.removeAll();
				d.dispose();
			}
		});

		return d;

	}

	public GayBerneConfigure() {

		super(new BorderLayout());

		if (rotateCursor1 == null) {
			rotateCursor1 = UserAction.createCursor("images/cursors/rotate1.gif", new Point(16, 17), "rotate");
			rotateCursor2 = UserAction.createCursor("images/cursors/rotate2.gif", new Point(14, 11), "rotate");
			rotateCursor3 = UserAction.createCursor("images/cursors/rotate3.gif", new Point(16, 17), "rotate");
		}

		ellipsePanel = new EllipsePanel();

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(ellipsePanel, BorderLayout.CENTER);
		panel.setBorder(BorderFactory.createTitledBorder("Geometric Anisotropy"));

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		ButtonGroup bg = new ButtonGroup();
		Dimension dim = ModelerUtilities.getSystemToolBarButtonSize();

		AbstractButton rb = new JToggleButton(IconPool.getIcon("resize"));
		rb.setToolTipText("Resize the particle");
		rb.setPreferredSize(dim);
		rb.setHorizontalAlignment(SwingConstants.CENTER);
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ellipsePanel.setState(EllipsePanel.RESIZE);
				ellipsePanel.repaint();
			}
		});
		rb.setSelected(true);
		p.add(rb);
		bg.add(rb);

		rb = new JToggleButton(new ImageIcon(getClass().getResource("images/Rotate.gif")));
		rb.setToolTipText("Assign an initial angle");
		rb.setPreferredSize(dim);
		rb.setHorizontalAlignment(SwingConstants.CENTER);
		rb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ellipsePanel.setState(EllipsePanel.ROTATE);
				ellipsePanel.repaint();
			}
		});
		p.add(rb);
		bg.add(rb);

		colorComboBox = new ColorComboBox(this);
		colorComboBox.setName(GB_COLOR);
		colorComboBox.setPreferredSize(new Dimension(100, dim.height));
		colorComboBox.setSelectedIndex(2);
		colorComboBox.updateColor(new Runnable() {
			public void run() {
				color = colorComboBox.getMoreColor();
				ellipsePanel.repaint();
			}
		});
		colorComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String actionName = colorComboBox.getName();
				int id = 0;
				if (actionName.equals(GB_COLOR)) {
					id = ((Integer) colorComboBox.getSelectedItem()).intValue();
				}
				if (id == 6 || id == 100) {
					color = colorComboBox.getMoreColor();
				}
				else {
					color = ColorRectangle.COLORS[id];
				}
				ellipsePanel.repaint();
			}
		});
		p.add(colorComboBox);

		panel.add(p, BorderLayout.NORTH);

		add(panel, BorderLayout.NORTH);

		eAnisotropy = new JLabel("<html>&#949;<sub>e</sub>/&#949;<sub>s</sub></html>", SwingConstants.CENTER);

		slider1 = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
		slider1.setPreferredSize(new Dimension(100, 50));
		slider1.setMajorTickSpacing(5);
		slider1.setMinorTickSpacing(5);
		slider1.setPaintTicks(true);
		slider1.setPaintTrack(true);
		slider1.setPaintLabels(true);
		slider1.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					eeVsEs = source.getValue();
					if (eeVsEs < 1.0 && eeVsEs > -1.0)
						eeVsEs = 1.0;
					eeVsEs = eeVsEs > 0 ? eeVsEs : -1.0 / eeVsEs;
					eAnisotropy.setText("<html>&#949;<sub>e</sub>/&#949;<sub>s</sub> = "
							+ scientificFormat.format(eeVsEs) + "</html>");
				}
			}
		});
		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		JLabel label = new JLabel("10.0");
		label.setFont(littleFont);
		tableOfLabels.put(10, label);
		label = new JLabel("5.0");
		label.setFont(littleFont);
		tableOfLabels.put(5, label);
		label = new JLabel("1.0");
		label.setFont(littleFont);
		tableOfLabels.put(0, label);
		label = new JLabel("0.2");
		label.setFont(littleFont);
		tableOfLabels.put(-5, label);
		label = new JLabel("0.1");
		label.setFont(littleFont);
		tableOfLabels.put(-10, label);
		slider1.setLabelTable(tableOfLabels);

		panel = new JPanel(new BorderLayout(0, 20));
		panel.add(slider1, BorderLayout.NORTH);

		scientificFormat.applyPattern("##.###");
		eAnisotropy.setText("<html>&#949;<sub>e</sub>/&#949;<sub>s</sub>  = " + scientificFormat.format((float) eeVsEs)
				+ "</html>");
		eAnisotropy.setBorder(BorderFactory.createTitledBorder("Slider Value"));
		panel.add(eAnisotropy, BorderLayout.CENTER);

		p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder("Energetic Anisotropic Ratio"));
		p.add(panel, BorderLayout.EAST);

		label = new JLabel(new ImageIcon(getClass().getResource("images/EnergyAnisotropy.gif")));
		label.setBorder(BorderFactory.createLoweredBevelBorder());
		p.add(label, BorderLayout.WEST);

		add(p, BorderLayout.CENTER);

		panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		scientificFormat.applyPattern("#.###");
		epsilon0_label = new JLabel("<html>&#949;<sub>0</sub> = " + scientificFormat.format(epsilon0) + "</html>");
		epsilon0_label.setBorder(BorderFactory.createTitledBorder("Value"));
		panel.add(epsilon0_label, BorderLayout.EAST);

		slider2 = new JSlider(JSlider.HORIZONTAL, 0, 50, 10);
		slider2.setBorder(BorderFactory.createTitledBorder("Potential Well Depth"));
		slider2.setMajorTickSpacing(10);
		slider2.setMinorTickSpacing(5);
		slider2.setPaintTicks(true);
		slider2.setPaintTrack(true);
		slider2.setPaintLabels(true);
		tableOfLabels = new Hashtable<Integer, JLabel>();
		label = new JLabel("0.1 eV");
		label.setFont(littleFont);
		tableOfLabels.put(50, label);
		label = new JLabel("0.05 eV");
		label.setFont(littleFont);
		tableOfLabels.put(25, label);
		label = new JLabel("0 eV");
		label.setFont(littleFont);
		tableOfLabels.put(0, label);
		slider2.setLabelTable(tableOfLabels);
		slider2.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					epsilon0 = source.getValue() * 0.002;
					epsilon0_label.setText("<html>&#949;<sub>0</sub>  = " + scientificFormat.format(epsilon0)
							+ "</html>");
				}
			}
		});
		panel.add(slider2, BorderLayout.CENTER);

		add(panel, BorderLayout.SOUTH);

	}

	private void reset() {

		eeVsEs = 1.0;
		slider1.setValue(0);
		eAnisotropy.setText("<html>&#949;<sub>e</sub>/&#949;<sub>s</sub>  = " + scientificFormat.format(eeVsEs)
				+ "</html>");

		epsilon0 = 0.1;
		slider2.setValue((int) (epsilon0 * 50));
		epsilon0_label.setText("<html>&#949;<sub>0</sub>  = " + scientificFormat.format(epsilon0) + "</html>");

		breadth = 20;
		length = 60;
		theta = 0;
		ellipsePanel.firstTime = true;
		ellipsePanel.repaint();

	}

	private class EllipsePanel extends JComponent implements MouseListener, MouseMotionListener {

		final static byte RESIZE = 1;
		final static byte ROTATE = 2;

		private Ellipse2D.Double ellipse;
		private Rectangle rect1, rect2;
		private int selectedRect = -1;
		private boolean firstTime = true;
		private boolean particleToBeRotated;

		static final short XMIN = 50;
		static final short XMAX = 100;
		static final short YMIN = 20;
		static final short YMAX = 40;

		private byte state = 1;
		private int handleIndex = -1;

		private Ellipse2D rotCircle1;
		private Ellipse2D rotCircle2;
		private Ellipse2D rotCircle3;
		private Ellipse2D rotCircle4;

		EllipsePanel() {
			rect1 = new Rectangle(6, 6);
			rect2 = new Rectangle(6, 6);
			ellipse = new Ellipse2D.Double();
			setPreferredSize(new Dimension(200, 100));
			setBorder(BorderFactory.createLoweredBevelBorder());
			firstTime = true;
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		public byte getState() {
			return state;
		}

		public void setState(byte state) {
			this.state = state;
			if (state == ROTATE) {
				if (rotCircle1 == null)
					rotCircle1 = new Ellipse2D.Float();
				if (rotCircle2 == null)
					rotCircle2 = new Ellipse2D.Float();
				if (rotCircle3 == null)
					rotCircle3 = new Ellipse2D.Float();
				if (rotCircle4 == null)
					rotCircle4 = new Ellipse2D.Float();
				setCursor(rotateCursor2);
			}
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			update(g);
		}

		public void update(Graphics g) {

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Dimension dimension = getSize();

			g2.setColor(Color.white);
			g2.fillRect(0, 0, dimension.width, dimension.height);

			if (firstTime) {
				rect1.setLocation((dimension.width - length) / 2 - 3, dimension.height / 2 - 3);
				rect2.setLocation(dimension.width / 2 - 3, (dimension.height - breadth) / 2 - 3);
				ellipse.setFrame(0.5 * (dimension.width - length), 0.5 * (dimension.height - breadth), length, breadth);
				firstTime = false;
			}

			double xcen = ellipse.getX() + 0.5 * ellipse.getWidth();
			double ycen = ellipse.getY() + 0.5 * ellipse.getHeight();

			g2.setColor(Color.gray);
			g2.draw(ellipse);
			g2.drawLine(0, dimension.height / 2, dimension.width, dimension.height / 2);
			g2.drawLine(dimension.width / 2, 0, dimension.width / 2, dimension.height);

			AffineTransform savedAT = g2.getTransform();
			AffineTransform at = new AffineTransform();

			at.setToRotation(theta, xcen, ycen);
			g2.transform(at);
			g2.setColor(color);
			g2.fill(ellipse);
			g2.setColor(Color.black);
			g2.draw(ellipse);
			g2.setTransform(savedAT);

			g2.setColor(Color.black);
			g2.setFont(littleFont);
			g2.drawString(length * 0.1 + "\u212b", rect1.x - 12, dimension.height / 2 - 5);
			g2.drawString(breadth * 0.1 + "\u212b", dimension.width / 2 + 5, rect2.y - 2);

			if (state == RESIZE) {
				g2.setColor(Color.yellow);
				g2.fill(rect1);
				g2.fill(rect2);
				g2.setColor(Color.black);
				g2.draw(rect1);
				g2.draw(rect2);
			}
			else if (state == ROTATE) {
				double cosTheta = Math.cos(theta);
				double sinTheta = Math.sin(theta);
				double xpos, ypos, xold, yold;
				/* southeast circle */
				xold = 0.5 * ellipse.getWidth();
				yold = 0.5 * ellipse.getHeight();
				xpos = xcen + xold * cosTheta - yold * sinTheta;
				ypos = ycen + xold * sinTheta + yold * cosTheta;
				rotCircle1.setFrame(xpos - 3, ypos - 3, 6, 6);
				/* southwest circle */
				xold = -0.5 * ellipse.getWidth();
				yold = 0.5 * ellipse.getHeight();
				xpos = xcen + xold * cosTheta - yold * sinTheta;
				ypos = ycen + xold * sinTheta + yold * cosTheta;
				rotCircle2.setFrame(xpos - 3, ypos - 3, 6, 6);
				/* northwest circle */
				xold = -0.5 * ellipse.getWidth();
				yold = -0.5 * ellipse.getHeight();
				xpos = xcen + xold * cosTheta - yold * sinTheta;
				ypos = ycen + xold * sinTheta + yold * cosTheta;
				rotCircle3.setFrame(xpos - 3, ypos - 3, 6, 6);
				/* northeast circle */
				xold = 0.5 * ellipse.getWidth();
				yold = -0.5 * ellipse.getHeight();
				xpos = xcen + xold * cosTheta - yold * sinTheta;
				ypos = ycen + xold * sinTheta + yold * cosTheta;
				rotCircle4.setFrame(xpos - 3, ypos - 3, 6, 6);
				/* paint all four circles here */
				g2.setColor(Color.green);
				g2.fill(rotCircle1);
				g2.fill(rotCircle2);
				g2.fill(rotCircle3);
				g2.fill(rotCircle4);
				g2.setColor(Color.black);
				g2.draw(rotCircle1);
				g2.draw(rotCircle2);
				g2.draw(rotCircle3);
				g2.draw(rotCircle4);
			}

		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			if (rect1.contains(x, y)) {
				selectedRect = 1;
			}
			else if (rect2.contains(x, y)) {
				selectedRect = 2;
			}
			else {
				selectedRect = -1;
			}
		}

		public void mouseReleased(MouseEvent e) {
			length = getWidth() - 2 * rect1.x;
			breadth = getHeight() - 2 * rect2.y;
			if (length == breadth) {
				slider1.setValue(0);
				eeVsEs = 1.0;
				eAnisotropy.setText("<html>&#949;<sub>e</sub>/&#949;<sub>s</sub>  = " + scientificFormat.format(eeVsEs)
						+ "</html>");
			}
			EllipsePanel.this.repaint();
		}

		public void mouseDragged(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			if (state == RESIZE) {
				if (selectedRect != -1) {
					updateLocation(x, y);
				}
			}
			else if (state == ROTATE) {
				if (particleToBeRotated) {
					setCursor(rotateCursor3);
					rotateTo(x, y, handleIndex);
				}
			}
		}

		public void mouseMoved(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			if (state == RESIZE) {
				if (rect1.contains(x, y) || rect2.contains(x, y)) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
				else {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
			else if (state == ROTATE) {
				if (rotCircle1.contains(x, y)) {
					particleToBeRotated = true;
					handleIndex = 1;
					setCursor(rotateCursor1);
				}
				else if (rotCircle2.contains(x, y)) {
					particleToBeRotated = true;
					handleIndex = 2;
					setCursor(rotateCursor1);
				}
				else if (rotCircle3.contains(x, y)) {
					particleToBeRotated = true;
					handleIndex = 3;
					setCursor(rotateCursor1);
				}
				else if (rotCircle4.contains(x, y)) {
					particleToBeRotated = true;
					handleIndex = 4;
					setCursor(rotateCursor1);
				}
				else {
					particleToBeRotated = false;
					handleIndex = -1;
					setCursor(rotateCursor2);
				}
			}
		}

		private void updateLocation(int x, int y) {
			if (selectedRect == 1) {
				if (x < XMIN) {
					rect1.x = XMIN;
				}
				else if (x > XMAX) {
					rect1.x = XMAX;
				}
				else {
					rect1.x = x;
				}
				length = getWidth() - 2 * rect1.x;
				breadth = getHeight() - 2 * rect2.y;
				ellipse.setFrame(rect1.x + 3, rect2.y + 3, length - 6, breadth - 6);
			}
			if (selectedRect == 2) {
				if (y < YMIN) {
					rect2.y = YMIN;
				}
				else if (y > YMAX) {
					rect2.y = YMAX;
				}
				else {
					rect2.y = y;
				}
				length = getWidth() - 2 * rect1.x;
				breadth = getHeight() - 2 * rect2.y;
				ellipse.setFrame(rect1.x + 3, rect2.y + 3, length - 6, breadth - 6);
			}
			EllipsePanel.this.repaint();
		}

		private void rotateTo(int xget, int yget, int handle) {
			double xcen = ellipse.getX() + ellipse.getWidth() * 0.5;
			double ycen = ellipse.getY() + ellipse.getHeight() * 0.5;
			double distance = Math.sqrt((xcen - xget) * (xcen - xget) + (ycen - yget) * (ycen - yget));
			double theta1 = (xget - xcen) / distance;
			theta1 = yget > ycen ? Math.acos(theta1) : 2.0 * Math.PI - Math.acos(theta1);
			double theta0;
			double xold = 1.0, yold = 0.0;
			switch (handle) {
			case 1:
				xold = 0.5 * ellipse.getWidth();
				yold = 0.5 * ellipse.getHeight();
				break;
			case 2:
				xold = -0.5 * ellipse.getWidth();
				yold = 0.5 * ellipse.getHeight();
				break;
			case 3:
				xold = -0.5 * ellipse.getWidth();
				yold = -0.5 * ellipse.getHeight();
				break;
			case 4:
				xold = 0.5 * ellipse.getWidth();
				yold = -0.5 * ellipse.getHeight();
				break;
			}
			distance = Math.sqrt(xold * xold + yold * yold);
			theta0 = xold / distance;
			theta0 = yold > 0.0 ? Math.acos(theta0) : 2.0 * Math.PI - Math.acos(theta0);
			theta = theta1 - theta0;
			EllipsePanel.this.repaint();
		}

	}

}