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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.functiongraph.DataSource;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.draw.StrokeFactory;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.IntegerTextField;

/**
 * @author Charles Xie
 * 
 */
class PageFunctionGraphMaker extends ComponentMaker {

	private PageFunctionGraph pageFunctionGraph;
	private JDialog dialog;
	private JComboBox functionComboBox;
	private JComboBox pointNumberComboBox;
	private JComboBox lineWeightComboBox;
	private JComboBox lineStyleComboBox;
	private ColorComboBox lineColorComboBox;
	private ColorComboBox bgComboBox;
	private JComboBox borderComboBox;
	private JCheckBox transparentCheckBox;
	private IntegerTextField widthField, heightField;
	private JPanel contentPane;

	PageFunctionGraphMaker(PageFunctionGraph pfg) {
		setObject(pfg);
	}

	void setObject(PageFunctionGraph pfg) {
		pageFunctionGraph = pfg;
	}

	private void confirm() {
		if (functionComboBox.getItemCount() > 0) {
			int i = functionComboBox.getSelectedIndex();
			DataSource ds = pageFunctionGraph.graph.getDataSource(i);
			ds.setColor(lineColorComboBox.getSelectedColor());
			int datapoint = ((Integer) pointNumberComboBox.getSelectedItem()).intValue();
			ds.setPreferredPointNumber(datapoint);
		}
		pageFunctionGraph.setOpaque(!transparentCheckBox.isSelected());
		pageFunctionGraph.setBorderType((String) borderComboBox.getSelectedItem());
		pageFunctionGraph.setBackground(bgComboBox.getSelectedColor());
		pageFunctionGraph.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageFunctionGraph.page.getSaveReminder().setChanged(true);
		pageFunctionGraph.page.settleComponentSize();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pageFunctionGraph.graph.repaint(); // must be called later to redraw
			}
		});
	}

	void invoke(Page page) {

		pageFunctionGraph.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeFunctionGraphDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize function graph", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}
			});
		}

		transparentCheckBox.setSelected(!pageFunctionGraph.graph.isOpaque());
		borderComboBox.setSelectedItem(pageFunctionGraph.getBorderType());
		bgComboBox.setColor(pageFunctionGraph.graph.getBackground());
		widthField.setValue(pageFunctionGraph.getWidth() <= 0 ? 500 : pageFunctionGraph.getWidth());
		heightField.setValue(pageFunctionGraph.getHeight() <= 0 ? 300 : pageFunctionGraph.getHeight());

		fillFunctionComboBox();
		DataSource ds = pageFunctionGraph.graph.getDataSource(functionComboBox.getSelectedIndex());
		if (ds != null) {
			pointNumberComboBox.setSelectedItem(new Integer(ds.getPreferredPointNumber()));
			lineWeightComboBox.setSelectedItem(new Float(ds.getLineWeight()));
			lineStyleComboBox.setSelectedItem(new Integer(LineStyle.getStrokeNumber(ds.getStroke())));
		}

		dialog.setVisible(true);

	}

	private void fillFunctionComboBox() {
		functionComboBox.removeAllItems();
		List list = pageFunctionGraph.graph.getExpressions();
		if (list != null) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				functionComboBox.addItem(it.next());
			}
		}
	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
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

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("SelectFunction");
		p.add(new JLabel(s != null ? s : "Select function", SwingConstants.LEFT));
		functionComboBox = new JComboBox();
		functionComboBox.setToolTipText("Select a function");
		functionComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					DataSource ds = pageFunctionGraph.graph.getDataSource(functionComboBox.getSelectedIndex());
					lineColorComboBox.setColor(ds.getColor());
					pointNumberComboBox.setSelectedItem(new Integer(ds.getPreferredPointNumber()));
					lineStyleComboBox.setSelectedItem(new Integer(LineStyle.getStrokeNumber(ds.getStroke())));
				}
			}
		});
		p.add(functionComboBox);

		// row 2
		s = Modeler.getInternationalText("PreferredNumberOfPoints");
		p.add(new JLabel(s != null ? s : "Preferred number of points", SwingConstants.LEFT));
		pointNumberComboBox = new JComboBox(new Object[] { new Integer(100), new Integer(200) });
		pointNumberComboBox.setToolTipText("Set the number of data points for the selected function.");
		pointNumberComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (functionComboBox.getItemCount() == 0)
					return;
				if (e.getStateChange() == ItemEvent.SELECTED) {
					DataSource ds = pageFunctionGraph.graph.getDataSource(functionComboBox.getSelectedIndex());
					int datapoint = ((Integer) pointNumberComboBox.getSelectedItem()).intValue();
					ds.setPreferredPointNumber(datapoint);
				}
			}
		});
		p.add(pointNumberComboBox);

		// row 3
		s = Modeler.getInternationalText("LineColor");
		p.add(new JLabel(s != null ? s : "Line color", SwingConstants.LEFT));
		lineColorComboBox = new ColorComboBox(pageFunctionGraph);
		lineColorComboBox.setToolTipText("Select the color for the selected function.");
		p.add(lineColorComboBox);

		// row 4
		s = Modeler.getInternationalText("LineWidth");
		p.add(new JLabel(s != null ? s : "Line weight", SwingConstants.LEFT));
		lineWeightComboBox = new JComboBox();
		lineWeightComboBox.setToolTipText("Select the line weight for the selected function.");
		lineWeightComboBox.setRenderer(new ComboBoxRenderer.LineThickness());
		lineWeightComboBox.addItem(new Float(LineWidth.STROKE_WIDTH_0));
		lineWeightComboBox.addItem(new Float(LineWidth.STROKE_WIDTH_1));
		lineWeightComboBox.addItem(new Float(LineWidth.STROKE_WIDTH_2));
		lineWeightComboBox.addItem(new Float(LineWidth.STROKE_WIDTH_3));
		lineWeightComboBox.addItem(new Float(LineWidth.STROKE_WIDTH_4));
		lineWeightComboBox.addItem(new Float(LineWidth.STROKE_WIDTH_5));
		lineWeightComboBox.setSelectedIndex(4);
		lineWeightComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (functionComboBox.getItemCount() == 0)
					return;
				if (e.getStateChange() == ItemEvent.SELECTED) {
					DataSource ds = pageFunctionGraph.graph.getDataSource(functionComboBox.getSelectedIndex());
					float weight = ((Float) lineWeightComboBox.getSelectedItem()).floatValue();
					ds.setLineWeight(weight);
				}
			}
		});
		p.add(lineWeightComboBox);

		// row 5
		s = Modeler.getInternationalText("LineStyle");
		p.add(new JLabel(s != null ? s : "Line style", SwingConstants.LEFT));
		lineStyleComboBox = new JComboBox();
		lineStyleComboBox.setToolTipText("Select the line style for the selected function.");
		lineStyleComboBox.setRenderer(new ComboBoxRenderer.LineStyles());
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_1));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_2));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_3));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_4));
		lineStyleComboBox.addItem(new Integer(LineStyle.STROKE_NUMBER_5));
		lineStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (functionComboBox.getItemCount() == 0)
					return;
				if (e.getStateChange() == ItemEvent.SELECTED) {
					DataSource ds = pageFunctionGraph.graph.getDataSource(functionComboBox.getSelectedIndex());
					int style = ((Integer) lineStyleComboBox.getSelectedItem()).intValue();
					ds.setStroke(StrokeFactory.changeStyle(ds.getStroke(), LineStyle.getDashArray(style)));
				}
			}
		});
		p.add(lineStyleComboBox);

		// row 6
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(500, 300, 1000);
		widthField.setToolTipText("Type in an integer to set the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 7
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(300, 200, 1000);
		heightField.setToolTipText("Type in an integer to set the height.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 8
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageFunctionGraph);
		bgComboBox.setToolTipText("Select the background color.");
		p.add(bgComboBox);

		// row 9
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 9, 2, 5, 5, 15, 5);

		p = new JPanel();
		p.setBorder(BorderFactory.createEtchedBorder());
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setToolTipText("Set to be transparent.");
		p.add(transparentCheckBox);

	}

}