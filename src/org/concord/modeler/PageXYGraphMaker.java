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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.g2d.Curve;
import org.concord.modeler.g2d.DataSet;
import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.modeler.util.QueueGroup;
import org.concord.mw2d.models.MDModel;

import static org.concord.modeler.PageXYGraph.MAX;

/**
 * @author Charles Xie
 * 
 */
class PageXYGraphMaker extends ComponentMaker {

	private PageXYGraph pageXYGraph;
	private JDialog dialog;
	private JComboBox modelComboBox, xComboBox, xFilterComboBox, borderComboBox;
	private JComboBox[] yComboBox, yFilterComboBox, smootherComboBox;
	private JComboBox[] lineStyleComboBox;
	private JComboBox[] lineSymbolComboBox;
	private JComboBox[] lineWidthComboBox;
	private ColorComboBox[] lineColorComboBox;
	private JSpinner[] symbolSpacingSpinner;
	private JSpinner[] symbolSizeSpinner;
	private ColorComboBox bgComboBox, fgComboBox;
	private JComboBox autoScaleXComboBox, autoScaleYComboBox, autoUpdateComboBox;
	private JTextField xLabelTextField, yLabelTextField;
	private IntegerTextField widthField, heightField;
	private IntegerTextField legendXField, legendYField;
	private FloatNumberTextField xMultiplierField, xAddendField;
	private FloatNumberTextField[] yMultiplierField, yAddendField;
	private JTextField[] scopeField;
	private JLabel[] scopeLabel;
	private JButton okButton;
	private JPanel contentPane;
	private static Font smallFont;
	private int xFilterType;
	private int[] yFilterType;
	private JTabbedPane tabbedPane;
	private JTextComponent focusTextComponent;

	/**
	 * if a model is selected, reload the time series combo boxes for the selected model.
	 */
	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageXYGraph);
			}
			else {
				m.addModelListener(pageXYGraph);
				pageXYGraph.setModelID(pageXYGraph.page.getComponentPool().getIndex(m));
				xComboBox.removeAllItems();
				for (int i = 0; i < MAX; i++)
					yComboBox[i].removeAllItems();
				fillTimeSeriesComboBox(xComboBox);
				for (int i = 0; i < MAX; i++)
					fillTimeSeriesComboBox(yComboBox[i]);
			}
		}
	};

	/** if a time series is selected, do something. */
	private ItemListener timeSeriesSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				JComboBox cb = (JComboBox) e.getSource();
				Object item = cb.getSelectedItem();
				if (item instanceof String) {
					if (((String) item).equalsIgnoreCase("none")) {
						for (int i = 0; i < MAX; i++) {
							if (yComboBox[i] == cb)
								pageXYGraph.descriptions[i + 1] = null;
						}
					}
				}
			}
		}
	};

	PageXYGraphMaker(PageXYGraph pxyg) {
		yFilterType = new int[MAX];
		Arrays.fill(yFilterType, 0);
		setObject(pxyg);
	}

	void setObject(PageXYGraph pxyg) {
		pageXYGraph = pxyg;
	}

	private boolean confirm() {

		pageXYGraph.setChangable(true);
		pageXYGraph.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageXYGraph.setBorderType((String) borderComboBox.getSelectedItem());
		pageXYGraph.getXAxis().setTitleText(xLabelTextField.getText());
		pageXYGraph.getYAxis().setTitleText(yLabelTextField.getText());

		pageXYGraph.getGraph().setDataBackground(fgComboBox.getSelectedColor());
		pageXYGraph.getGraph().setGraphBackground(bgComboBox.getSelectedColor());

		Model model = (Model) modelComboBox.getSelectedItem();
		model.addModelListener(pageXYGraph);
		model.getMovie().addMovieListener(pageXYGraph);
		pageXYGraph.setModelClass(model.getClass().getName());
		if (model instanceof MDModel) {
			pageXYGraph.setModelID(pageXYGraph.page.getComponentPool().getIndex(model));
		}
		else if (model instanceof Embeddable) {
			pageXYGraph.setModelID(((Embeddable) model).getIndex());
		}

		pageXYGraph.getGraph().detachDataSets();

		DataQueue q1 = (DataQueue) xComboBox.getSelectedItem();
		pageXYGraph.xMultiplier = xMultiplierField.getValue();
		pageXYGraph.xAddend = xAddendField.getValue();

		if (q1 == null) {
			JOptionPane.showMessageDialog(dialog, "Variable X must be selected.", "Please select X",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		boolean noYIsSelected = true;

		for (int i = 0; i < MAX; i++) {

			DataQueue q2 = null;
			try {
				q2 = (DataQueue) yComboBox[i].getSelectedItem();
			}
			catch (ClassCastException e) {
				// not a queue, ignore
			}
			pageXYGraph.yMultiplier[i] = yMultiplierField[i].getValue();
			pageXYGraph.yAddend[i] = yAddendField[i].getValue();

			if (q2 != null) {

				if (q1 == q2) {
					JOptionPane.showMessageDialog(dialog, "Variable X and Y" + (i + 1) + " cannot be identical.",
							"Please reselect X and/or Y" + (i + 1), JOptionPane.ERROR_MESSAGE);
					return false;
				}
				if (DataQueueUtilities.mismatch(q1, q2)) {
					JOptionPane.showMessageDialog(dialog, "Variable X and Y" + (i + 1)
							+ " are not synchronously sampled. They cannot form a function.", "Please reselect X and Y"
							+ (i + 1), JOptionPane.ERROR_MESSAGE);
					return false;
				}
				q1.setFunctionalSlot(0);
				q2.setFunctionalSlot(i + 1);
				pageXYGraph.append(q1.getName() + "-" + q2.getName(), q1, q2, pageXYGraph.smoothers[i]);
				pageXYGraph.setDescription(0, q1.toString());
				pageXYGraph.setDescription(i + 1, q2.toString());
				noYIsSelected = false;
				decorateCurve(i);

			}

		}

		if (noYIsSelected) {
			JOptionPane.showMessageDialog(dialog, "None of the Y-variables is selected.", "Please select Y",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		pageXYGraph.setLabelForXAxis(xLabelTextField.getText());
		pageXYGraph.setLabelForYAxis(yLabelTextField.getText());
		pageXYGraph.getCurveGroup().getLabelOfX().setText(xLabelTextField.getText());
		pageXYGraph.getCurveGroup().getLabelOfY().setText(yLabelTextField.getText());

		pageXYGraph.setAutoScaleX(autoScaleXComboBox.getSelectedIndex() == 0);
		pageXYGraph.setAutoScaleY(autoScaleYComboBox.getSelectedIndex() == 0);
		if (!pageXYGraph.autoScaleX) {
			double xmin0 = pageXYGraph.dataWindow_xmin;
			double xmax0 = pageXYGraph.dataWindow_xmax;
			if (!scopeField[0].getText().equals("Auto")) {
				try {
					pageXYGraph.dataWindow_xmin = Double.parseDouble(scopeField[0].getText());
				}
				catch (Exception exception) {
					JOptionPane.showMessageDialog(dialog, "Xmin: Must type in a number.", "Please enter a number",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			else {
				JOptionPane.showMessageDialog(dialog, "As you have disabled Auto-Scaling X, you must specify xmin.",
						"Xmin not set", JOptionPane.ERROR_MESSAGE);
				focusTextComponent = scopeField[0];
				return false;
			}
			if (!scopeField[2].getText().equals("Auto")) {
				try {
					pageXYGraph.dataWindow_xmax = Double.parseDouble(scopeField[2].getText());
				}
				catch (Exception exception) {
					JOptionPane.showMessageDialog(dialog, "Xmax: Must type in a number.", "Please enter a number",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			else {
				JOptionPane.showMessageDialog(dialog, "As you have disabled Auto-Scaling X, you must specify xmax.",
						"Xmax not set", JOptionPane.ERROR_MESSAGE);
				focusTextComponent = scopeField[2];
				return false;
			}
			if (pageXYGraph.dataWindow_xmin >= pageXYGraph.dataWindow_xmax) {
				JOptionPane.showMessageDialog(dialog, "Xmax must be greater than Xmin.", "Xmax<Xmin error",
						JOptionPane.ERROR_MESSAGE);
				pageXYGraph.dataWindow_xmin = xmin0;
				pageXYGraph.dataWindow_xmax = xmax0;
				pageXYGraph.fixScopeOfX(pageXYGraph.dataWindow_xmin, pageXYGraph.dataWindow_xmax);
				focusTextComponent = scopeField[2];
				return false;
			}
			pageXYGraph.fixScopeOfX(pageXYGraph.dataWindow_xmin, pageXYGraph.dataWindow_xmax);
		}
		if (!pageXYGraph.autoScaleY) {
			double ymin0 = pageXYGraph.dataWindow_ymin;
			double ymax0 = pageXYGraph.dataWindow_ymax;
			if (!scopeField[1].getText().equals("Auto")) {
				try {
					pageXYGraph.dataWindow_ymin = Double.parseDouble(scopeField[1].getText());
				}
				catch (Exception exception) {
					JOptionPane.showMessageDialog(dialog, "Ymin: Must type in a number.", "Please enter a number",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			else {
				JOptionPane.showMessageDialog(dialog, "As you have disabled Auto-Scaling Y, you must specify ymin.",
						"Ymin not set", JOptionPane.ERROR_MESSAGE);
				focusTextComponent = scopeField[1];
				return false;
			}
			if (!scopeField[3].getText().equals("Auto")) {
				try {
					pageXYGraph.dataWindow_ymax = Double.parseDouble(scopeField[3].getText());
				}
				catch (Exception exception) {
					JOptionPane.showMessageDialog(dialog, "Ymax: Must type in a number.", "Please enter a number",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			else {
				JOptionPane.showMessageDialog(dialog, "As you have disabled Auto-Scaling Y, you must specify ymax.",
						"Ymax not set", JOptionPane.ERROR_MESSAGE);
				focusTextComponent = scopeField[3];
				return false;
			}
			if (pageXYGraph.dataWindow_ymin >= pageXYGraph.dataWindow_ymax) {
				JOptionPane.showMessageDialog(dialog, "Ymax must be greater than Ymin.", "Ymax<Ymin error",
						JOptionPane.ERROR_MESSAGE);
				pageXYGraph.dataWindow_ymin = ymin0;
				pageXYGraph.dataWindow_ymax = ymax0;
				pageXYGraph.fixScopeOfY(pageXYGraph.dataWindow_ymin, pageXYGraph.dataWindow_ymax);
				focusTextComponent = scopeField[3];
				return false;
			}
			pageXYGraph.fixScopeOfY(pageXYGraph.dataWindow_ymin, pageXYGraph.dataWindow_ymax);
		}

		pageXYGraph.setAutoUpdate(autoUpdateComboBox.getSelectedIndex() == 0);

		pageXYGraph.getGraph().setLegendLocation(legendXField.getValue(), legendYField.getValue());

		pageXYGraph.page.getSaveReminder().setChanged(true);
		pageXYGraph.page.settleComponentSize();

		return true;

	}

	void invoke(Page page) {

		pageXYGraph.page = page;
		page.deselect();
		focusTextComponent = null;
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeXYGraphDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize X-Y graph", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(JOptionPane.getFrameForComponent(page));
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					if (focusTextComponent == null) {
						xLabelTextField.selectAll();
						xLabelTextField.requestFocus();
					}
					else {
						focusTextComponent.selectAll();
						focusTextComponent.requestFocus();
					}
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		xComboBox.removeItemListener(timeSeriesSelectionListener);
		if (xFilterComboBox != null)
			xFilterComboBox.setSelectedIndex(0);
		for (int i = 0; i < MAX; i++) {
			if (yComboBox[i] != null)
				yComboBox[i].removeItemListener(timeSeriesSelectionListener);
			if (yFilterComboBox[i] != null)
				yFilterComboBox[i].setSelectedIndex(0);
		}

		xLabelTextField.setText(pageXYGraph.getXAxis().getTitleText());
		yLabelTextField.setText(pageXYGraph.getYAxis().getTitleText());
		if (pageXYGraph.isMaximumSizeSet()) {
			widthField.setValue(pageXYGraph.getMaximumSize().width);
			heightField.setValue(pageXYGraph.getMaximumSize().height);
		}

		// add legacy MD models to the model list
		ComponentPool componentPool = page.getComponentPool();
		synchronized (componentPool) {
			for (ModelCanvas mc : componentPool.getModels()) {
				if (mc.isUsed()) {
					modelComboBox.addItem(mc.getContainer().getModel());
				}
			}
		}
		// add target models to the model list
		for (Class c : ModelCommunicator.targetClass) {
			Map map = page.getEmbeddedComponent(c);
			if (map != null && !map.isEmpty()) {
				for (Object o : map.keySet()) {
					modelComboBox.addItem(map.get(o));
				}
			}
		}

		if (pageXYGraph.isTargetClass()) {
			if (pageXYGraph.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageXYGraph.modelClass), pageXYGraph.modelID);
					if (o != null)
						modelComboBox.setSelectedItem(o);
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				if (m instanceof Embeddable)
					pageXYGraph.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageXYGraph.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageXYGraph.modelID);
				modelComboBox.setSelectedItem(mc.getContainer().getModel());
				mc.getContainer().getModel().addModelListener(pageXYGraph);
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				if (m != null) {
					pageXYGraph.setModelID(componentPool.getIndex(m));
					m.addModelListener(pageXYGraph);
				}
			}
		}

		modelComboBox.addItemListener(modelSelectionListener);
		fillTimeSeriesComboBox(xComboBox);
		for (int i = 0; i < MAX; i++)
			fillTimeSeriesComboBox(yComboBox[i]);

		xMultiplierField.setValue(pageXYGraph.xMultiplier);
		xAddendField.setValue(pageXYGraph.xAddend);
		for (int i = 0; i < MAX; i++) {
			if (pageXYGraph.lineColors[i] != null)
				lineColorComboBox[i].setColor(pageXYGraph.lineColors[i]);
			lineSymbolComboBox[i].setSelectedIndex(pageXYGraph.lineSymbols[i]);
			lineWidthComboBox[i].setSelectedIndex((int) pageXYGraph.lineWidths[i]);
			lineStyleComboBox[i].setSelectedIndex(pageXYGraph.lineStyles[i]);
			symbolSpacingSpinner[i].setValue(new Integer(pageXYGraph.symbolSpacings[i]));
			symbolSizeSpinner[i].setValue(new Integer(pageXYGraph.symbolSizes[i]));
			yMultiplierField[i].setValue(pageXYGraph.yMultiplier[i]);
			yAddendField[i].setValue(pageXYGraph.yAddend[i]);
			switch (pageXYGraph.smoothers[i]) {
			case Curve.INSTANTANEOUS_VALUE:
				smootherComboBox[i].setSelectedIndex(0);
				break;
			case Curve.SIMPLE_RUNNING_AVERAGE:
				smootherComboBox[i].setSelectedIndex(1);
				break;
			case Curve.EXPONENTIAL_RUNNING_AVERAGE:
				smootherComboBox[i].setSelectedIndex(2);
				break;
			}
		}
		autoScaleXComboBox.setSelectedIndex(pageXYGraph.autoScaleX ? 0 : 1);
		autoScaleYComboBox.setSelectedIndex(pageXYGraph.autoScaleY ? 0 : 1);
		autoUpdateComboBox.setSelectedIndex(pageXYGraph.autoUpdate ? 0 : 1);
		enableScopeXFields(!pageXYGraph.autoScaleX);
		enableScopeYFields(!pageXYGraph.autoScaleY);
		if (!pageXYGraph.autoScaleX) {
			scopeField[0].setText("" + pageXYGraph.dataWindow_xmin);
			scopeField[2].setText("" + pageXYGraph.dataWindow_xmax);
		}
		if (!pageXYGraph.autoScaleY) {
			scopeField[1].setText("" + pageXYGraph.dataWindow_ymin);
			scopeField[3].setText("" + pageXYGraph.dataWindow_ymax);
		}
		bgComboBox.setColor(pageXYGraph.getGraph().getGraphBackground());
		borderComboBox.setSelectedItem(pageXYGraph.getBorderType());
		if (pageXYGraph.getGraph().getDataBackground() != null)
			fgComboBox.setColor(pageXYGraph.getGraph().getDataBackground());
		if (pageXYGraph.getGraph().getLegendLocation() != null) {
			legendXField.setValue(pageXYGraph.getGraph().getLegendLocation().x);
			legendYField.setValue(pageXYGraph.getGraph().getLegendLocation().y);
		}
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && xComboBox.getItemCount() > 0
				&& yComboBox[0].getItemCount() > 0 && yComboBox[MAX - 1].getItemCount() > 0);

		tabbedPane.setSelectedIndex(0);

		dialog.setVisible(true);

	}

	private void enableScopeXFields(boolean b) {
		scopeField[0].setEnabled(b);
		scopeField[2].setEnabled(b);
		scopeLabel[0].setEnabled(b);
		scopeLabel[2].setEnabled(b);
		if (!b) {
			scopeField[0].setText("Auto");
			scopeField[2].setText("Auto");
		}
	}

	private void enableScopeYFields(boolean b) {
		scopeField[1].setEnabled(b);
		scopeField[3].setEnabled(b);
		scopeLabel[1].setEnabled(b);
		scopeLabel[3].setEnabled(b);
		if (!b) {
			scopeField[1].setText("Auto");
			scopeField[3].setText("Auto");
		}
	}

	private static void addFilteredTimeSeries(int filterType, DataQueue q, JComboBox comboBox) {
		String s;
		switch (filterType) {
		case 0:
			comboBox.addItem(q);
			break;
		case 1:
			s = q.toString();
			if (s.startsWith("Rx:") || s.startsWith("Ry:") || s.startsWith("Vx:") || s.startsWith("Vy:")
					|| s.startsWith("Ax:") || s.startsWith("Ay:")) {
				comboBox.addItem(q);
			}
			break;
		case 2:
			s = q.toString();
			if (!s.startsWith("Rx:") && !s.startsWith("Ry:") && !s.startsWith("Vx:") && !s.startsWith("Vy:")
					&& !s.startsWith("Ax:") && !s.startsWith("Ay:")) {
				comboBox.addItem(q);
			}
			break;
		}
	}

	@SuppressWarnings("unchecked")
	private void fillTimeSeriesComboBox(JComboBox comboBox) {
		comboBox.removeItemListener(timeSeriesSelectionListener);
		comboBox.removeAllItems();
		Model model = (Model) modelComboBox.getSelectedItem();
		if (model == null)
			return;
		QueueGroup qg = model.getMovieQueueGroup();
		Collections.sort(qg);
		DataQueue q = null;
		int nitem = 0;
		boolean xSet = false;
		int jtime = -1;
		// by default, set the x variable to be model time
		if (comboBox == xComboBox) {
			if (qg != null && !qg.isEmpty()) {
				synchronized (qg.getSynchronizedLock()) {
					for (Iterator it = qg.iterator(); it.hasNext();) {
						q = (DataQueue) it.next();
						addFilteredTimeSeries(xFilterType, q, comboBox);
					}
				}
			}
			if (xFilterType != 1)
				comboBox.addItem(model.getModelTimeQueue());
			nitem = comboBox.getItemCount();
			for (int i = 0; i < nitem; i++) {
				q = (DataQueue) comboBox.getItemAt(i);
				if (q.getName().equals(pageXYGraph.getDescription(0))) {
					comboBox.setSelectedItem(q);
					xSet = true;
				}
				else {
					if (q == model.getModelTimeQueue())
						jtime = i;
				}
			}
			if (!xSet && jtime != -1)
				comboBox.setSelectedIndex(jtime);
		}
		else {
			comboBox.addItem("None");
			int n = 0;
			for (int i = 0; i < yComboBox.length; i++) {
				if (comboBox == yComboBox[i]) {
					n = i;
					break;
				}
			}
			if (qg != null && !qg.isEmpty()) {
				synchronized (qg.getSynchronizedLock()) {
					for (Iterator it = qg.iterator(); it.hasNext();) {
						q = (DataQueue) it.next();
						addFilteredTimeSeries(yFilterType[n], q, comboBox);
					}
				}
			}
			nitem = comboBox.getItemCount();
			for (int i = 0; i < nitem; i++) {
				try {
					q = (DataQueue) comboBox.getItemAt(i);
				}
				catch (ClassCastException e) {
					// not a data queue, ignore
				}
				if (q != null && q.getName().equals(pageXYGraph.getDescription(n + 1))) {
					comboBox.setSelectedItem(q);
				}
			}
		}
		comboBox.addItemListener(timeSeriesSelectionListener);

	}

	private void decorateCurve(int i) {

		DataSet set = (DataSet) pageXYGraph.getGraph().getDataSets().lastElement();

		set.setLineStroke((Integer) lineStyleComboBox[i].getSelectedItem());
		pageXYGraph.lineStyles[i] = lineStyleComboBox[i].getSelectedIndex();

		set.setSymbol((Integer) lineSymbolComboBox[i].getSelectedItem());
		pageXYGraph.lineSymbols[i] = lineSymbolComboBox[i].getSelectedIndex();

		set.setLineStroke((Float) lineWidthComboBox[i].getSelectedItem());
		pageXYGraph.lineWidths[i] = lineWidthComboBox[i].getSelectedIndex();

		pageXYGraph.lineColors[i] = lineColorComboBox[i].getSelectedColor();
		set.setLineColor(pageXYGraph.lineColors[i]);
		set.legendColor(pageXYGraph.lineColors[i]);

		set.setSymbolSize((Integer) symbolSizeSpinner[i].getValue());
		pageXYGraph.symbolSizes[i] = set.getSymbolSize();

		set.setSymbolSpacing((Integer) symbolSpacingSpinner[i].getValue());
		pageXYGraph.symbolSpacings[i] = set.getSymbolSpacing();

	}

	private JPanel createLine(int n, ActionListener okListener) {

		if (n < 0 || n >= MAX)
			throw new IllegalArgumentException("overflow");

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		p.setPreferredSize(new Dimension(300, 300));

		String s = Modeler.getInternationalText("SymbolSpacing");
		final JLabel symbolSpacingLabel = new JLabel(s != null ? s : "Symbol spacing", SwingConstants.LEFT);
		s = Modeler.getInternationalText("SymbolSize");
		final JLabel symbolSizeLabel = new JLabel(s != null ? s : "Symbol size", SwingConstants.LEFT);

		// row 1
		s = Modeler.getInternationalText("SelectVariableLabel");
		p.add(new JLabel((s != null ? s : "Select a variable to be ") + "Y" + (n + 1), SwingConstants.LEFT));
		p.add(yComboBox[n]);

		// row 2
		s = Modeler.getInternationalText("VariableFilter");
		p.add(new JLabel(s != null ? s : "Variable filter", SwingConstants.LEFT));
		yFilterComboBox[n] = new JComboBox(new Object[] { "All variables", "Particle variables", "System variables" });
		yFilterComboBox[n].setToolTipText("Filter variables by types to find them more easily.");
		final int n2 = n;
		yFilterComboBox[n].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					yFilterType[n2] = yFilterComboBox[n2].getSelectedIndex();
					fillTimeSeriesComboBox(yComboBox[n2]);
				}
			}
		});
		p.add(yFilterComboBox[n]);

		// row 3
		s = Modeler.getInternationalText("CurveSmoother");
		p.add(new JLabel(s != null ? s : "Smoother", SwingConstants.LEFT));
		smootherComboBox[n] = new JComboBox(new Object[] { "None", "Simple running average",
				"Exponential running average" });
		smootherComboBox[n].setToolTipText("Select the method to smoothen the data.");
		final int n3 = n;
		smootherComboBox[n].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (smootherComboBox[n3].getSelectedIndex()) {
					case 0:
						pageXYGraph.setSmoothFilter(n3, Curve.INSTANTANEOUS_VALUE);
						break;
					case 1:
						pageXYGraph.setSmoothFilter(n3, Curve.SIMPLE_RUNNING_AVERAGE);
						break;
					case 2:
						pageXYGraph.setSmoothFilter(n3, Curve.EXPONENTIAL_RUNNING_AVERAGE);
						break;
					}
				}
			}
		});
		p.add(smootherComboBox[n]);

		// row 4
		p.add(new JLabel("Multiplier for Y" + (n + 1), SwingConstants.LEFT));
		yMultiplierField[n] = new FloatNumberTextField(pageXYGraph.yMultiplier[n], -Float.MAX_VALUE, Float.MAX_VALUE);
		yMultiplierField[n].setToolTipText("Type in a value to multiply the y" + (n + 1) + " output.");
		p.add(yMultiplierField[n]);

		// row 5
		p.add(new JLabel("Addend to Y" + (n + 1), SwingConstants.LEFT));
		yAddendField[n] = new FloatNumberTextField(pageXYGraph.yAddend[n], -Float.MAX_VALUE, Float.MAX_VALUE);
		yAddendField[n].setToolTipText("Type in a value to be added to the y" + (n + 1) + " output.");
		p.add(yAddendField[n]);

		// row 6
		s = Modeler.getInternationalText("LineStyle");
		p.add(new JLabel((s != null ? s : "Line style for ") + "Y" + (n + 1) + "(X)", SwingConstants.LEFT));
		lineStyleComboBox[n] = new JComboBox();
		lineStyleComboBox[n].setToolTipText("Select the line style for the y" + (n + 1) + " curve.");
		lineStyleComboBox[n].setRenderer(new ComboBoxRenderer.LineStyles());
		lineStyleComboBox[n].addItem(new Integer(LineStyle.STROKE_NUMBER_1));
		lineStyleComboBox[n].addItem(new Integer(LineStyle.STROKE_NUMBER_2));
		lineStyleComboBox[n].addItem(new Integer(LineStyle.STROKE_NUMBER_3));
		lineStyleComboBox[n].addItem(new Integer(LineStyle.STROKE_NUMBER_4));
		lineStyleComboBox[n].addItem(new Integer(LineStyle.STROKE_NUMBER_5));
		p.add(lineStyleComboBox[n]);

		// row 7
		s = Modeler.getInternationalText("LineSymbol");
		p.add(new JLabel((s != null ? s : "Line symbol for ") + "Y" + (n + 1) + "(X)", SwingConstants.LEFT));
		lineSymbolComboBox[n] = new JComboBox();
		lineSymbolComboBox[n].setToolTipText("Select the line symbol type for the y" + (n + 1) + " curve.");
		lineSymbolComboBox[n].setRenderer(new ComboBoxRenderer.Symbols());
		lineSymbolComboBox[n].addItem(new Integer(0));
		lineSymbolComboBox[n].addItem(new Integer(1));
		lineSymbolComboBox[n].addItem(new Integer(2));
		lineSymbolComboBox[n].addItem(new Integer(3));
		lineSymbolComboBox[n].addItem(new Integer(4));
		lineSymbolComboBox[n].addItem(new Integer(5));
		lineSymbolComboBox[n].addItem(new Integer(6));
		lineSymbolComboBox[n].addItem(new Integer(7));
		lineSymbolComboBox[n].setSelectedIndex(1);
		final int nn = n;
		lineSymbolComboBox[n].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					JComboBox cb = (JComboBox) e.getSource();
					boolean b = cb.getSelectedIndex() != 0;
					symbolSpacingSpinner[nn].setEnabled(b);
					symbolSizeSpinner[nn].setEnabled(b);
					symbolSpacingLabel.setEnabled(b);
					symbolSizeLabel.setEnabled(b);
				}
			}
		});
		p.add(lineSymbolComboBox[n]);

		// row 8
		p.add(symbolSpacingLabel);
		symbolSpacingSpinner[n] = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
		((JSpinner.DefaultEditor) symbolSpacingSpinner[n].getEditor()).getTextField().setToolTipText(
				"Select the spacing between symbols for the y" + (n + 1) + " curve.");
		((JSpinner.DefaultEditor) symbolSpacingSpinner[n].getEditor()).getTextField().addActionListener(okListener);
		p.add(symbolSpacingSpinner[n]);

		// row 9
		p.add(symbolSizeLabel);
		symbolSizeSpinner[n] = new JSpinner(new SpinnerNumberModel(6, 4, 12, 1));
		((JSpinner.DefaultEditor) symbolSizeSpinner[n].getEditor()).getTextField().setToolTipText(
				"Select the size of symbols for the y" + (n + 1) + " curve.");
		((JSpinner.DefaultEditor) symbolSizeSpinner[n].getEditor()).getTextField().addActionListener(okListener);
		p.add(symbolSizeSpinner[n]);

		// row 10
		s = Modeler.getInternationalText("LineWidth");
		p.add(new JLabel((s != null ? s : "Line width for ") + "Y" + (n + 1) + "(X)", SwingConstants.LEFT));
		lineWidthComboBox[n] = new JComboBox();
		lineWidthComboBox[n].setToolTipText("Select the line width for the y" + (n + 1) + " curve.");
		lineWidthComboBox[n].setRenderer(new ComboBoxRenderer.LineThickness());
		lineWidthComboBox[n].addItem(new Float(LineWidth.STROKE_WIDTH_0));
		lineWidthComboBox[n].addItem(new Float(LineWidth.STROKE_WIDTH_1));
		lineWidthComboBox[n].addItem(new Float(LineWidth.STROKE_WIDTH_2));
		lineWidthComboBox[n].addItem(new Float(LineWidth.STROKE_WIDTH_3));
		lineWidthComboBox[n].addItem(new Float(LineWidth.STROKE_WIDTH_4));
		lineWidthComboBox[n].addItem(new Float(LineWidth.STROKE_WIDTH_5));
		lineWidthComboBox[n].setSelectedIndex(1);
		p.add(lineWidthComboBox[n]);

		// row 11
		s = Modeler.getInternationalText("LineColor");
		p.add(new JLabel((s != null ? s : "Line color for ") + "Y" + (n + 1) + "(X)", SwingConstants.LEFT));
		lineColorComboBox[n] = new ColorComboBox(pageXYGraph);
		lineColorComboBox[n].setToolTipText("Select the line color for the y" + (n + 1) + " curve.");
		lineColorComboBox[n].setRenderer(new ComboBoxRenderer.ColorCell());
		lineColorComboBox[n].setSelectedIndex(0);
		lineColorComboBox[n].setRequestFocusEnabled(false);
		p.add(lineColorComboBox[n]);

		ModelerUtilities.makeCompactGrid(p, 11, 2, 5, 5, 10, 2);

		return p;

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (confirm()) {
					cancel = false;
					dialog.dispose();
				}
			}
		};

		yComboBox = new JComboBox[MAX];
		yFilterComboBox = new JComboBox[MAX];
		smootherComboBox = new JComboBox[MAX];
		yMultiplierField = new FloatNumberTextField[MAX];
		yAddendField = new FloatNumberTextField[MAX];
		lineWidthComboBox = new JComboBox[MAX];
		lineColorComboBox = new ColorComboBox[MAX];
		lineStyleComboBox = new JComboBox[MAX];
		lineSymbolComboBox = new JComboBox[MAX];
		symbolSizeSpinner = new JSpinner[MAX];
		symbolSpacingSpinner = new JSpinner[MAX];

		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		contentPane.add(p, BorderLayout.SOUTH);

		String s = Modeler.getInternationalText("OKButton");
		okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(okListener);
		p.add(okButton);

		s = Modeler.getInternationalText("CancelButton");
		JButton button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(button);

		p = new JPanel(new BorderLayout(10, 10));
		p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		contentPane.add(p, BorderLayout.CENTER);

		for (int n = 0; n < MAX; n++) {
			yComboBox[n] = new JComboBox();
			yComboBox[n].setFont(smallFont);
			yComboBox[n].setToolTipText("Select the time series output to represent y" + (n + 1) + " variable.");
		}

		JPanel box = new JPanel(new GridLayout(1, 2, 5, 5));
		p.add(box, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new SpringLayout());
		p2.setPreferredSize(new Dimension(300, 300));
		box.add(p2);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p2.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one that outputs to this X-Y graph.");
		p2.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectVariableLabel");
		p2.add(new JLabel((s != null ? s : "Select a variable to be ") + "X", SwingConstants.LEFT));
		xComboBox = new JComboBox();
		xComboBox.setFont(smallFont);
		xComboBox.setToolTipText("Select the time series output to represent x variable.");
		p2.add(xComboBox);

		// row 3
		s = Modeler.getInternationalText("VariableFilter");
		p2.add(new JLabel(s != null ? s : "Variable filter", SwingConstants.LEFT));
		xFilterComboBox = new JComboBox(new Object[] { "All variables", "Particle variables", "System variables" });
		xFilterComboBox.setToolTipText("Filter variables by types to find them more easily.");
		xFilterComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					xFilterType = xFilterComboBox.getSelectedIndex();
					fillTimeSeriesComboBox(xComboBox);
				}
			}
		});
		p2.add(xFilterComboBox);

		// row 4
		p2.add(new JLabel("Multiplier for X", SwingConstants.LEFT));
		xMultiplierField = new FloatNumberTextField(pageXYGraph.xMultiplier, -Float.MAX_VALUE, Float.MAX_VALUE);
		xMultiplierField.setToolTipText("Type in a value to multiply the x output.");
		p2.add(xMultiplierField);

		// row 5
		p2.add(new JLabel("Addend to X", SwingConstants.LEFT));
		xAddendField = new FloatNumberTextField(pageXYGraph.xAddend, -Float.MAX_VALUE, Float.MAX_VALUE);
		xAddendField.setToolTipText("Type in a value to be added to the x output.");
		p2.add(xAddendField);

		// row 6
		s = Modeler.getInternationalText("TitleForXAxis");
		p2.add(new JLabel(s != null ? s : "Title for X axis", SwingConstants.LEFT));
		xLabelTextField = new JTextField("x");
		xLabelTextField.addActionListener(okListener);
		xLabelTextField.setToolTipText("Type in the title for the x axis.");
		p2.add(xLabelTextField);

		// row 7
		s = Modeler.getInternationalText("TitleForYAxis");
		p2.add(new JLabel(s != null ? s : "Title for Y axis", SwingConstants.LEFT));
		yLabelTextField = new JTextField("y");
		yLabelTextField.addActionListener(okListener);
		yLabelTextField.setToolTipText("Type in the title for the y axis.");
		p2.add(yLabelTextField);

		// row 8
		s = Modeler.getInternationalText("AutoScaleXAxis");
		p2.add(new JLabel(s != null ? s : "Auto scale X axis", SwingConstants.LEFT));
		String[] yesno = new String[] { "Yes", "No" };
		s = Modeler.getInternationalText("Yes");
		if (s != null)
			yesno[0] = s;
		s = Modeler.getInternationalText("No");
		if (s != null)
			yesno[1] = s;
		autoScaleXComboBox = new JComboBox(yesno);
		autoScaleXComboBox.setToolTipText("Select yes to auto-scale the x axis; select no otherwise.");
		autoScaleXComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					enableScopeXFields(autoScaleXComboBox.getSelectedIndex() == 1);
			}
		});
		p2.add(autoScaleXComboBox);

		// row 9
		s = Modeler.getInternationalText("AutoScaleYAxis");
		p2.add(new JLabel(s != null ? s : "Auto scale Y axis", SwingConstants.LEFT));
		autoScaleYComboBox = new JComboBox(yesno);
		autoScaleYComboBox.setToolTipText("Select yes to auto-scale the y axis; select no otherwise.");
		autoScaleYComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					enableScopeYFields(autoScaleYComboBox.getSelectedIndex() == 1);
			}
		});
		p2.add(autoScaleYComboBox);

		// row 10
		s = Modeler.getInternationalText("AutoUpdate");
		p2.add(new JLabel(s != null ? s : "Auto update", SwingConstants.LEFT));
		autoUpdateComboBox = new JComboBox(yesno);
		autoUpdateComboBox.setToolTipText("Select yes to auto-update the graph; select no otherwise.");
		autoUpdateComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					pageXYGraph.autoUpdate = autoUpdateComboBox.getSelectedIndex() == 0;
			}
		});
		p2.add(autoUpdateComboBox);

		// row 11
		s = Modeler.getInternationalText("InsideBackgroundColor");
		p2.add(new JLabel(s != null ? s : "Inside background color", SwingConstants.LEFT));
		fgComboBox = new ColorComboBox(pageXYGraph);
		fgComboBox.setSelectedIndex(6);
		fgComboBox.setToolTipText("Select graph window background color.");
		fgComboBox.setRequestFocusEnabled(false);
		p2.add(fgComboBox);

		// row 12
		s = Modeler.getInternationalText("OutsideBackgroundColor");
		p2.add(new JLabel(s != null ? s : "Outside background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageXYGraph);
		bgComboBox.setSelectedIndex(6);
		bgComboBox.setToolTipText("Select the background color for the pane outside the graph window.");
		bgComboBox.setRequestFocusEnabled(false);
		p2.add(bgComboBox);

		// row 13
		s = Modeler.getInternationalText("BorderLabel");
		p2.add(new JLabel(s != null ? s : "Outside border type", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p2.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		p2.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p2, 13, 2, 5, 5, 10, 2);

		p2 = new JPanel(new GridLayout(1, 2, 5, 5));
		p.add(p2, BorderLayout.SOUTH);

		JPanel temp = new JPanel(new GridLayout(2, 4, 5, 5));
		s = Modeler.getInternationalText("GraphSize");
		String s1 = Modeler.getInternationalText("LegendPosition");
		temp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s + "," + s1
				: "Size and legends", 0, 0));
		p2.add(temp, BorderLayout.WEST);

		s = Modeler.getInternationalText("WidthLabel");
		temp.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));

		widthField = new IntegerTextField(250, 100, 600);
		widthField.setToolTipText("Type in the width for this graph.");
		widthField.addActionListener(okListener);
		temp.add(widthField);

		s = Modeler.getInternationalText("HeightLabel");
		temp.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));

		heightField = new IntegerTextField(300, 100, 600);
		heightField.setToolTipText("Type in the height for this graph.");
		heightField.addActionListener(okListener);
		temp.add(heightField);

		temp.add(new JLabel((s1 != null ? s1 : "Legend") + " X", SwingConstants.LEFT));

		legendXField = new IntegerTextField(pageXYGraph.getGraph().getWidth() - 80, 0, 600);
		legendXField.setToolTipText("Type in the x coordinate for the legend.");
		legendXField.addActionListener(okListener);
		temp.add(legendXField);

		temp.add(new JLabel((s1 != null ? s1 : "Legend") + " Y", SwingConstants.LEFT));

		legendYField = new IntegerTextField(pageXYGraph.getGraph().borderTop + 15, 0, 600);
		legendYField.setToolTipText("Type in the y coordinate for the legend.");
		legendYField.addActionListener(okListener);
		temp.add(legendYField);

		temp = new JPanel(new GridLayout(2, 4, 5, 5));
		s = Modeler.getInternationalText("SetDrawingRange");
		temp.setBorder(new javax.swing.border.TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "If not automatically fit to the window, set the drawing range", 0, 0));
		p2.add(temp, BorderLayout.EAST);

		scopeField = new JTextField[4];
		scopeLabel = new JLabel[4];

		scopeLabel[0] = new JLabel("Xmin");
		temp.add(scopeLabel[0]);

		scopeField[0] = new JTextField("Auto");
		scopeField[0].setToolTipText("Type in a value for xmin if not auto-scaling the x axis");
		scopeField[0].addActionListener(okListener);
		temp.add(scopeField[0]);

		scopeLabel[1] = new JLabel("Ymin");
		temp.add(scopeLabel[1]);

		scopeField[1] = new JTextField("Auto");
		scopeField[1].setToolTipText("Type in a value for ymin if not auto-scaling the y axis");
		scopeField[1].addActionListener(okListener);
		temp.add(scopeField[1]);

		scopeLabel[2] = new JLabel("Xmax");
		temp.add(scopeLabel[2]);

		scopeField[2] = new JTextField("Auto");
		scopeField[2].setToolTipText("Type in a value for xmax if not auto-scaling the x axis");
		scopeField[2].addActionListener(okListener);
		temp.add(scopeField[2]);

		scopeLabel[3] = new JLabel("Ymax");
		temp.add(scopeLabel[3]);

		scopeField[3] = new JTextField("Auto");
		scopeField[3].setToolTipText("Type in a value for ymax if not auto-scaling the y axis");
		scopeField[3].addActionListener(okListener);
		temp.add(scopeField[3]);

		enableScopeXFields(false);
		enableScopeYFields(false);

		tabbedPane = new JTabbedPane();
		box.add(tabbedPane);

		for (int i = 0; i < MAX; i++) {
			tabbedPane.addTab("Y" + (i + 1), createLine(i, okListener));
		}

	}

}