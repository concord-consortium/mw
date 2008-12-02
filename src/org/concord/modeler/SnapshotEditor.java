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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import org.concord.modeler.draw.DrawingElement;
import org.concord.modeler.ui.ColorArrayEvent;
import org.concord.modeler.ui.ColorArrayListener;
import org.concord.modeler.ui.ColorArrayPane;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.PastableTextArea;

class SnapshotEditor extends JPanel {

	SnapshotGallery gallery;
	ImageAnnotater annotater;
	JComponent toolBar;
	private JTextArea textArea;
	private JPanel descriptionPanel;
	private AbstractButton selectButton, gridButton;
	private ColorArrayPane measureLineColorArrayPane, gridColorArrayPane;
	private boolean flash;
	private JScrollPane scroller;
	private Component invoker;

	SnapshotEditor(SnapshotGallery g) {

		super(new BorderLayout(0, 0));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		gallery = g;

		descriptionPanel = new JPanel(new BorderLayout());
		add(descriptionPanel, BorderLayout.SOUTH);

		textArea = new PastableTextArea(5, 10);
		JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		descriptionPanel.add(scrollPane, BorderLayout.CENTER);

		String s = Modeler.getInternationalText("DescribeSnapshot");
		JLabel label = new JLabel(
				"<html>"
						+ (s != null ? s
								: "Describe this image in the box below (the image and description will be automatically included when you create a report) ")
						+ ":</html>");
		label.setPreferredSize(new Dimension(300, 50));
		label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		descriptionPanel.add(label, BorderLayout.NORTH);

		JPanel panel = new JPanel(new BorderLayout(0, 0));
		panel.setBorder(BorderFactory.createEtchedBorder());
		add(panel, BorderLayout.CENTER);

		annotater = new ImageAnnotater();
		annotater.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		scroller = new JScrollPane();
		scroller.getViewport().setView(annotater);
		panel.add(scroller, BorderLayout.CENTER);

		toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.add(toolBar, BorderLayout.NORTH);

		ButtonGroup bg = new ButtonGroup();
		int m = System.getProperty("os.name").startsWith("Mac") ? 6 : 2;
		Insets margin = new Insets(m, m, m, m);

		selectButton = new JToggleButton(IconPool.getIcon("select"));
		selectButton.setToolTipText("Select and move");
		if (!Modeler.isMac())
			selectButton.setMargin(margin);
		selectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				annotater.setMode(ImageAnnotater.DEFAULT_MODE);
			}
		});
		toolBar.add(selectButton);
		bg.add(selectButton);

		AbstractButton button = new JToggleButton(new ImageIcon(getClass().getResource("images/ruler.gif")));
		button.setToolTipText("Measure distance (change color of measuring line)");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				annotater.setMode(ImageAnnotater.MEASURE_MODE);
				if (e.getClickCount() < 2)
					return;
				if (measureLineColorArrayPane == null) {
					measureLineColorArrayPane = new ColorArrayPane();
					measureLineColorArrayPane.addColorArrayListener(new ColorArrayListener() {
						public void colorSelected(ColorArrayEvent e) {
							annotater.setMeasureLineColor(e.getSelectedColor());
							annotater.repaint();
						}
					});
				}
				String s = Modeler.getInternationalText("MeasureLineColor");
				measureLineColorArrayPane.createDialog(SnapshotEditor.this, s != null ? s : "Measure Line Color",
						ModelerUtilities.colorChooser, new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								annotater.setMeasureLineColor(ModelerUtilities.colorChooser.getColor());
								annotater.repaint();
							}
						}).setVisible(true);
			}
		});
		toolBar.add(button);
		bg.add(button);

		button = new JToggleButton(new ImageIcon(getClass().getResource("images/CallOutRectangle.gif")));
		button.setToolTipText("Add a text box");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				annotater.setMode(ImageAnnotater.DEFAULT_MODE);
				annotater.inputTextBox();
			}
		});
		toolBar.add(button);
		bg.add(button);

		button = new JToggleButton(IconPool.getIcon("arrowtool"));
		button.setToolTipText("Draw an arrow");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				annotater.setMode(ImageAnnotater.LINE_MODE);
			}
		});
		toolBar.add(button);
		bg.add(button);

		button = new JToggleButton(IconPool.getIcon("recttool"));
		button.setToolTipText("Draw a rectangle");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				annotater.setMode(ImageAnnotater.RECT_MODE);
			}
		});
		toolBar.add(button);
		bg.add(button);

		button = new JToggleButton(IconPool.getIcon("ellipsetool"));
		button.setToolTipText("Draw an ellipse");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				annotater.setMode(ImageAnnotater.ELLIPSE_MODE);
			}
		});
		toolBar.add(button);
		bg.add(button);

		button = new JToggleButton(IconPool.getIcon("triangletool"));
		button.setToolTipText("Draw a triangle");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				annotater.setMode(ImageAnnotater.TRIANGLE_MODE);
			}
		});
		toolBar.add(button);
		bg.add(button);

		gridButton = new JToggleButton(new ImageIcon(getClass().getResource("images/GridLines.gif")));
		gridButton.setToolTipText("Toggle grid lines (double-click to change color)");
		if (!Modeler.isMac())
			gridButton.setMargin(margin);
		gridButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectButton.doClick();
				boolean b = gridButton.isSelected();
				annotater.setShowGrid(b);
				String s = "grid:" + annotater.getImage().getDescription();
				if (b) {
					gallery.putProperty(s, new GridSetting(annotater.getGridCellSize()));
				}
				else {
					gallery.removeProperty(s);
				}
			}
		});
		gridButton.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.getClickCount() < 2)
					return;
				if (gridColorArrayPane == null) {
					gridColorArrayPane = new ColorArrayPane();
					gridColorArrayPane.addColorArrayListener(new ColorArrayListener() {
						public void colorSelected(ColorArrayEvent e) {
							annotater.setGridColor(e.getSelectedColor());
							annotater.repaint();
						}
					});
				}
				String s = Modeler.getInternationalText("GridColor");
				gridColorArrayPane.createDialog(SnapshotEditor.this, s != null ? s : "Grid Color",
						ModelerUtilities.colorChooser, new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								annotater.setGridColor(ModelerUtilities.colorChooser.getColor());
								annotater.repaint();
							}
						}).setVisible(true);
			}
		});
		toolBar.add(gridButton);

		button = new JButton(IconPool.getIcon("cut"));
		button.setToolTipText("Remove the selected element");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectButton.doClick();
				DrawingElement t = annotater.getSelectedElement();
				if (t == null)
					return;
				if (JOptionPane.showConfirmDialog(SnapshotEditor.this,
						"The selected element will be removed. Are you sure?", "Remove element",
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					annotater.removeElement(t);
				}
			}
		});
		toolBar.add(button);

		button = new JButton(IconPool.getIcon("copy"));
		button.setToolTipText("Copy the selected element");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectButton.doClick();
				DrawingElement t = annotater.getSelectedElement();
				if (t == null)
					return;
				annotater.copySelectedElement();
			}
		});
		toolBar.add(button);

		button = new JButton(IconPool.getIcon("paste"));
		button.setToolTipText("Paste");
		if (!Modeler.isMac())
			button.setMargin(margin);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				selectButton.doClick();
				annotater.pasteElement();
			}
		});
		toolBar.add(button);

	}

	private GridSetting getGridSetting() {
		if (annotater.getShowGrid())
			return new GridSetting(annotater.getGridCellSize());
		return null;
	}

	private void setImage(ImageIcon image, boolean inputDescription) {
		annotater.setImage(image);
		if (!gallery.hasNoProperty()) {
			Object o = gallery.getProperty("grid:" + image.getDescription());
			if (o instanceof GridSetting) {
				annotater.setShowGrid(true);
				annotater.setGridCellSize(((GridSetting) o).getCellSize());
			}
			else {
				annotater.setShowGrid(false);
			}
			o = gallery.getProperty("annotation:" + image.getDescription());
			if (o instanceof DrawingElement[]) {
				DrawingElement[] t = (DrawingElement[]) o;
				for (DrawingElement i : t)
					annotater.addElement(i);
			}
			if (inputDescription) {
				o = gallery.getProperty("comment:" + image.getDescription());
				if (o instanceof String) {
					textArea.setText((String) o);
				}
				else {
					textArea.setText(null);
				}
			}
		}
		else {
			if (inputDescription)
				textArea.setText(null);
			annotater.setShowGrid(false);
		}
		annotater.setPreferredSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
		annotater.repaint();
		scroller.doLayout();
	}

	private boolean areDrawingElementsOutsideImage() {
		DrawingElement[] annotations = annotater.getElements();
		if (annotations == null)
			return false;
		ImageIcon image = annotater.getImage();
		int dx = annotater.getWidth() - image.getIconWidth();
		int dy = annotater.getHeight() - image.getIconHeight();
		if (dx <= 0 && dy <= 0)
			return false;
		for (DrawingElement e : annotations) {
			if (e.getRx() > 0.5 * dx || e.getRy() > 0.5 * dy)
				return true;
		}
		return false;
	}

	private void updateProperties(boolean inputDescription, boolean added, String pageAddress) {
		ImageIcon image = annotater.getImage();
		String s = image.getDescription();
		if (inputDescription) {
			String comments = textArea.getText();
			if (comments != null && !comments.trim().equals("")) {
				gallery.putProperty("comment:" + s, comments);
			}
			else {
				gallery.removeProperty("comment:" + s);
			}
		}
		GridSetting g = getGridSetting();
		if (g != null) {
			gallery.putProperty("grid:" + s, g);
		}
		else {
			gallery.removeProperty("grid:" + s);
		}
		DrawingElement[] annotations = annotater.getElements();
		if (annotations != null) {
			gallery.putProperty("annotation:" + s, annotations);
		}
		else {
			gallery.removeProperty("annotation:" + s);
		}
		boolean annotationOut = areDrawingElementsOutsideImage();
		ImageIcon image2 = ModelerUtilities.componentToImageIcon(annotater, s, !annotationOut);
		if (!annotationOut) {
			int x = 0;
			int y = 0;
			if (image2.getIconWidth() > image.getIconWidth()) {
				x = (image2.getIconWidth() - image.getIconWidth()) / 2;
			}
			if (image2.getIconHeight() > image.getIconHeight()) {
				y = (image2.getIconHeight() - image.getIconHeight()) / 2;
			}
			if (x > 0 || y > 0) {
				image2 = new ImageIcon(((BufferedImage) image2.getImage()).getSubimage(x, y, image.getIconWidth(),
						image.getIconHeight()));
				image2.setDescription(s);
			}
		}
		saveAnnotatedImage(image2);
		if (added) {
			gallery.addImageName(s, pageAddress);
			gallery.notifyListeners(new SnapshotEvent(invoker, SnapshotEvent.SNAPSHOT_ADDED));
		}
		else {
			int i = gallery.getSelectedIndex();
			if (i >= 0) {
				gallery.setImageName(i, s, pageAddress);
				gallery.notifyListeners(new SnapshotEvent(invoker, SnapshotEvent.SNAPSHOT_CHANGED, s));
			}
		}
	}

	private void saveAnnotatedImage(ImageIcon image) {
		ModelerUtilities
				.saveImageIcon(image, new File(gallery.getAnnotatedImageFolder(), image.getDescription()), true);
	}

	boolean showInputDialog(Component c, final ImageIcon image, String title, final boolean inputDescription,
			boolean showSlideButtons, final String pageAddress) {

		invoker = c;
		setImage(image, inputDescription);

		if (inputDescription) {
			add(descriptionPanel, BorderLayout.SOUTH);
		}
		else {
			remove(descriptionPanel);
		}
		gridButton.setSelected(annotater.getShowGrid());

		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(c), title, true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.getContentPane().add(this, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		dialog.getContentPane().add(panel, BorderLayout.SOUTH);

		JButton button = null;
		String s = null;

		if (showSlideButtons) {

			s = Modeler.getInternationalText("PreviousSnapshot");
			final JButton previousButton = new JButton(s != null ? s : "Previous");
			s = Modeler.getInternationalText("NextSnapshot");
			final JButton nextButton = new JButton(s != null ? s : "Next");

			previousButton.setEnabled(gallery.getSelectedIndex() > 0);
			previousButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int i = gallery.getSelectedIndex();
					if (i > 0) {
						previousButton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						updateProperties(inputDescription, false, pageAddress);
						gallery.setSelectedIndex(--i);
						setImage(gallery.getSelectedOriginalImage(), inputDescription);
						previousButton.setEnabled(i > 0);
						nextButton.setEnabled(i < gallery.size() - 1);
						previousButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
			});
			panel.add(previousButton);

			nextButton.setEnabled(gallery.getSelectedIndex() < gallery.size() - 1);
			nextButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int i = gallery.getSelectedIndex();
					if (i < gallery.size() - 1) {
						nextButton.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						updateProperties(inputDescription, false, pageAddress);
						gallery.setSelectedIndex(++i);
						setImage(gallery.getSelectedOriginalImage(), inputDescription);
						previousButton.setEnabled(i > 0);
						nextButton.setEnabled(i < gallery.size() - 1);
						nextButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
			});
			panel.add(nextButton);

			s = Modeler.getInternationalText("CloseButton");
			button = new JButton(s != null ? s : "Close");
			button.setToolTipText("Close this window and return to thumbnail images");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JButton b = (JButton) e.getSource();
					b.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					updateProperties(inputDescription, false, pageAddress);
					b.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					dialog.dispose();
				}
			});
			panel.add(button);

		}
		else {

			s = Modeler.getInternationalText("OKButton");
			button = new JButton(s != null ? s : "OK");
			button.setToolTipText("Keep this snapshot and close the window");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					flash = true;
					JButton button = (JButton) e.getSource();
					button.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					boolean b = gallery.containsImageName(image.getDescription());
					if (!b)
						gallery.addOriginalImage(image, pageAddress);
					updateProperties(inputDescription, !b, pageAddress);
					button.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					dialog.dispose();
				}
			});
			panel.add(button);

			s = Modeler.getInternationalText("CancelButton");
			button = new JButton(s != null ? s : "Cancel");
			button.setToolTipText("Discard this snapshot and close the window");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					flash = false;
					dialog.dispose();
				}
			});
			panel.add(button);

		}

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				flash = false;
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				if (inputDescription)
					textArea.requestFocus();
			}
		});
		dialog.pack();
		dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(c));
		dialog.setVisible(true);

		if (selectButton != null)
			selectButton.setSelected(true);
		annotater.clearSelection();

		return flash;

	}

}