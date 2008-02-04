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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.QueueGroup;

/**
 * @author Charles Xie
 * 
 */
class PageBarGraphMaker extends ComponentMaker {

	private PageBarGraph pageBarGraph;
	private int filterType;
	private JDialog dialog;
	private JTextField descriptionField;
	private FloatNumberTextField multiplierField;
	private FloatNumberTextField addendField;
	private JComboBox modelComboBox, timeSeriesComboBox, filterComboBox, orieComboBox;
	private JComboBox tickComboBox, labelComboBox, titleComboBox;
	private JComboBox averageTypeComboBox;
	private JComboBox formatComboBox;
	private ColorComboBox bgComboBox, fgComboBox;
	private FloatNumberTextField maxField, minField, valueField;
	private IntegerTextField maximumFractionDigitField;
	private IntegerTextField maximumIntegerDigitField;
	private IntegerTextField widthField, heightField;
	private JSpinner majorTickSpinner, minorTickSpinner;
	private JButton okButton;
	private static Font smallFont;
	private JPanel contentPane;
	private JTextComponent focusTextComponent;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageBarGraph);
			}
			else {
				m.addModelListener(pageBarGraph);
				pageBarGraph.setModelID(pageBarGraph.page.getComponentPool().getIndex(m));
				fillTimeSeriesComboBox();
			}
		}
	};

	private ItemListener timeSeriesSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			Object obj = timeSeriesComboBox.getSelectedItem();
			try {
				if (obj instanceof FloatQueue) {
					pageBarGraph.setValue(((FloatQueue) obj).getCurrentValue());
					maxField.setValue((float) ((FloatQueue) obj).getReferenceUpperBound());
					minField.setValue((float) ((FloatQueue) obj).getReferenceLowerBound());
				}
				valueField.setValue((float) pageBarGraph.getValue());
				descriptionField.setText(obj.toString());
			}
			catch (Exception exception) {
				valueField.setText("Unknown");
			}
		}
	};

	PageBarGraphMaker(PageBarGraph pbg) {
		setObject(pbg);
	}

	void setObject(PageBarGraph pbg) {
		pageBarGraph = pbg;
	}

	private boolean confirm() {
		if (minField.getValue() >= maxField.getValue()) {
			JOptionPane.showMessageDialog(dialog, "Upper bound must be greater than lower bound.", "Input error",
					JOptionPane.ERROR_MESSAGE);
			focusTextComponent = maxField;
			return false;
		}
		switch (averageTypeComboBox.getSelectedIndex()) {
		case 0:
			pageBarGraph.setAverageType(PageBarGraph.GROWING_POINT_RUNNING_AVERAGE);
			break;
		case 1:
			pageBarGraph.setAverageType(PageBarGraph.SIMPLE_RUNNING_AVERAGE);
			break;
		case 2:
			pageBarGraph.setAverageType(PageBarGraph.EXPONENTIAL_RUNNING_AVERAGE);
			break;
		}
		pageBarGraph.setFormat((String) formatComboBox.getSelectedItem());
		pageBarGraph.setMultiplier(multiplierField.getValue());
		pageBarGraph.setAddend(addendField.getValue());
		pageBarGraph.setMaximumFractionDigits(maximumFractionDigitField.getValue());
		pageBarGraph.setMaximumIntegerDigits(maximumIntegerDigitField.getValue());
		pageBarGraph.setMinorTicks(((Integer) minorTickSpinner.getValue()));
		pageBarGraph.setMajorTicks(((Integer) majorTickSpinner.getValue()));
		pageBarGraph.setOrientation(orieComboBox.getSelectedIndex() == 0 ? PageBarGraph.HORIZONTAL
				: PageBarGraph.VERTICAL);
		pageBarGraph.setBackground(bgComboBox.getSelectedColor());
		pageBarGraph.setForeground(fgComboBox.getSelectedColor());
		pageBarGraph.setPaintTitle(titleComboBox.getSelectedIndex() == 0);
		pageBarGraph.setPaintTicks(tickComboBox.getSelectedIndex() == 0);
		pageBarGraph.setPaintLabels(labelComboBox.getSelectedIndex() == 0);
		pageBarGraph.setMinimum(minField.getValue());
		pageBarGraph.setMaximum(maxField.getValue());
		Object obj = timeSeriesComboBox.getSelectedItem();
		if (obj instanceof DataQueue) {
			pageBarGraph.setTimeSeriesName(((DataQueue) obj).getName());
		}
		if (descriptionField.getText() != null && !descriptionField.getText().trim().equals(""))
			pageBarGraph.setDescription(descriptionField.getText());
		Model m = (Model) modelComboBox.getSelectedItem();
		m.addModelListener(pageBarGraph);
		m.getMovie().addMovieListener(pageBarGraph);
		pageBarGraph.setModelID(pageBarGraph.page.getComponentPool().getIndex(m));
		pageBarGraph.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageBarGraph.setChangable(true);
		pageBarGraph.page.getSaveReminder().setChanged(true);
		pageBarGraph.page.reload();
		return true;
	}

	void invoke(Page page) {

		pageBarGraph.page = page;
		page.deselect();
		focusTextComponent = null;
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeBarGraphDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize bar graph", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					if (focusTextComponent == null) {
						descriptionField.selectAll();
						descriptionField.requestFocus();
					}
					else {
						focusTextComponent.selectAll();
						focusTextComponent.requestFocus();
					}
				}
			});
		}

		final ComponentPool componentPool = page.getComponentPool();

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		timeSeriesComboBox.removeItemListener(timeSeriesSelectionListener);
		filterComboBox.setSelectedIndex(0);
		switch (pageBarGraph.averageType) {
		case PageBarGraph.GROWING_POINT_RUNNING_AVERAGE:
			averageTypeComboBox.setSelectedIndex(0);
			break;
		case PageBarGraph.SIMPLE_RUNNING_AVERAGE:
			averageTypeComboBox.setSelectedIndex(1);
			break;
		case PageBarGraph.EXPONENTIAL_RUNNING_AVERAGE:
			averageTypeComboBox.setSelectedIndex(2);
			break;
		}

		orieComboBox.setSelectedIndex(pageBarGraph.getOrientation() == PageBarGraph.HORIZONTAL ? 0 : 1);
		titleComboBox.setSelectedIndex(pageBarGraph.getPaintTitle() ? 0 : 1);
		tickComboBox.setSelectedIndex(pageBarGraph.getPaintTicks() ? 0 : 1);
		labelComboBox.setSelectedIndex(pageBarGraph.getPaintLabels() ? 0 : 1);
		descriptionField.setText(pageBarGraph.getDescription());
		multiplierField.setValue(pageBarGraph.getMultiplier());
		addendField.setValue(pageBarGraph.getAddend());
		minField.setValue((float) pageBarGraph.getMinimum());
		maxField.setValue((float) pageBarGraph.getMaximum());
		valueField.setValue((float) pageBarGraph.getValue());
		if (pageBarGraph.isMaximumSizeSet()) {
			widthField.setValue(pageBarGraph.getMaximumSize().width);
			heightField.setValue(pageBarGraph.getMaximumSize().height);
		}

		synchronized (componentPool) {
			for (ModelCanvas mc : componentPool.getModels()) {
				if (mc.isUsed()) {
					modelComboBox.addItem(mc.getContainer().getModel());
				}
			}
		}
		if (pageBarGraph.modelID != -1) {
			ModelCanvas mc = componentPool.get(pageBarGraph.modelID);
			modelComboBox.setSelectedItem(mc.getContainer().getModel());
			mc.getContainer().getModel().addModelListener(pageBarGraph);
		}
		else {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (m != null) {
				pageBarGraph.setModelID(componentPool.getIndex(m));
				m.addModelListener(pageBarGraph);
			}
		}
		modelComboBox.addItemListener(modelSelectionListener);
		fillTimeSeriesComboBox();
		timeSeriesComboBox.addItemListener(timeSeriesSelectionListener);

		formatComboBox.setSelectedItem(pageBarGraph.getFormat());
		maximumFractionDigitField.setValue(pageBarGraph.getMaximumFractionDigits());
		maximumIntegerDigitField.setValue(pageBarGraph.getMaximumIntegerDigits());
		majorTickSpinner.setValue(pageBarGraph.getMajorTicks());
		minorTickSpinner.setValue(pageBarGraph.getMinorTicks());
		bgComboBox.setColor(pageBarGraph.getBackground());
		fgComboBox.setColor(pageBarGraph.getForeground());
		okButton.setEnabled(modelComboBox.getItemCount() > 0 && timeSeriesComboBox.getItemCount() > 0);

		dialog.setVisible(true);

	}

	@SuppressWarnings("unchecked")
	private void fillTimeSeriesComboBox() {
		timeSeriesComboBox.removeAllItems();
		Model model = (Model) modelComboBox.getSelectedItem();
		if (model == null)
			return;
		QueueGroup qg = model.getMovieQueueGroup();
		Collections.sort(qg);
		DataQueue q = null;
		if (qg != null) {
			String s;
			synchronized (qg.getSynchronizedLock()) {
				for (Iterator it = qg.iterator(); it.hasNext();) {
					q = (DataQueue) it.next();
					switch (filterType) {
					case 0:
						timeSeriesComboBox.addItem(q);
						break;
					case 1:
						s = q.toString();
						if (s.startsWith("Rx:") || s.startsWith("Ry:") || s.startsWith("Vx:") || s.startsWith("Vy:")
								|| s.startsWith("Ax:") || s.startsWith("Ay:")) {
							timeSeriesComboBox.addItem(q);
						}
						break;
					case 2:
						s = q.toString();
						if (!s.startsWith("Rx:") && !s.startsWith("Ry:") && !s.startsWith("Vx:")
								&& !s.startsWith("Vy:") && !s.startsWith("Ax:") && !s.startsWith("Ay:")) {
							timeSeriesComboBox.addItem(q);
						}
						break;
					}
				}
			}
		}
		int nitem = timeSeriesComboBox.getItemCount();
		int guard = 0;
		for (int i = 0; i < nitem; i++) {
			q = (DataQueue) timeSeriesComboBox.getItemAt(i);
			if (q.getName().equals(pageBarGraph.timeSeriesName)) {
				timeSeriesComboBox.setSelectedItem(q);
				if (q instanceof FloatQueue) {
					try {
						valueField.setValue(((FloatQueue) q).getCurrentValue());
					}
					catch (Exception e) {
						valueField.setText("Unknown");
					}
				}
				guard++;
			}
		}

		if (guard >= 2)
			throw new IllegalStateException("More than one time series are using this bar graph (" + guard + ")");

		/*
		 * when no time series is using this bar graph, display in the value field the newest value of this first time
		 * series in the combo box
		 */
		if (guard == 0 && nitem > 0) {
			q = (DataQueue) timeSeriesComboBox.getItemAt(0);
			if (q instanceof FloatQueue) {
				try {
					valueField.setValue(((FloatQueue) q).getCurrentValue());
				}
				catch (Exception exception) {
					valueField.setText("Unknown");
				}
			}
		}

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

		Box box = new Box(BoxLayout.X_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(box, BorderLayout.CENTER);

		/* left panel */

		p = new JPanel(new SpringLayout());
		p.setPreferredSize(new Dimension(300, 300));
		box.add(p);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one that outputs to this bar graph.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectVariableLabel");
		p.add(new JLabel(s != null ? s : "Select a variable", SwingConstants.LEFT));
		timeSeriesComboBox = new JComboBox();
		timeSeriesComboBox.setFont(smallFont);
		timeSeriesComboBox.setToolTipText("Select the time series output to be displayed in this bar graph.");
		p.add(timeSeriesComboBox);

		// row 3
		s = Modeler.getInternationalText("VariableFilter");
		p.add(new JLabel(s != null ? s : "Variable filter", SwingConstants.LEFT));
		filterComboBox = new JComboBox(new Object[] { "All variables", "Particle variables", "System variables" });
		filterComboBox.setToolTipText("Filter variables by types to find them more easily.");
		filterComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					filterType = filterComboBox.getSelectedIndex();
					fillTimeSeriesComboBox();
				}
			}
		});
		p.add(filterComboBox);

		// row 4
		s = Modeler.getInternationalText("AverageType");
		p.add(new JLabel(s != null ? s : "Average type", SwingConstants.LEFT));
		averageTypeComboBox = new JComboBox(new String[] { "Growing-point running average", "Simple running average",
				"Exponential running average" });
		averageTypeComboBox.setToolTipText("Select the method of calculating the running average.");
		p.add(averageTypeComboBox);

		// row 5
		s = Modeler.getInternationalText("TextLabel");
		p.add(new JLabel(s != null ? s : "Description", SwingConstants.LEFT));
		descriptionField = new JTextField();
		descriptionField.addActionListener(okListener);
		descriptionField.setToolTipText("Type in the text to be displayed in the bar graph.");
		p.add(descriptionField);

		// row 6
		p.add(new JLabel("Multiplier", SwingConstants.LEFT));
		multiplierField = new FloatNumberTextField(1, -Float.MAX_VALUE, Float.MAX_VALUE);
		multiplierField.addActionListener(okListener);
		multiplierField.setToolTipText("Type in a value to multiply the output.");
		p.add(multiplierField);

		// row 7
		p.add(new JLabel("Addend", SwingConstants.LEFT));
		addendField = new FloatNumberTextField(0, -Float.MAX_VALUE, Float.MAX_VALUE);
		addendField.addActionListener(okListener);
		addendField.setToolTipText("Type in a value to be added to the output.");
		p.add(addendField);

		// row 8
		s = Modeler.getInternationalText("UpperBoundLabel");
		p.add(new JLabel(s != null ? s : "Set upper bound", SwingConstants.LEFT));
		maxField = new FloatNumberTextField(1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		maxField.addActionListener(okListener);
		maxField.setToolTipText("Type in the upper bound this bar graph displays.");
		p.add(maxField);

		// row 9
		s = Modeler.getInternationalText("LowerBoundLabel");
		p.add(new JLabel(s != null ? s : "Set lower bound", SwingConstants.LEFT));
		minField = new FloatNumberTextField(-1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		minField.addActionListener(okListener);
		minField.setToolTipText("Type in the lower bound this bar graph displays.");
		p.add(minField);

		// row 10
		s = Modeler.getInternationalText("CurrentValueLabel");
		p.add(new JLabel(s != null ? s : "Current value", SwingConstants.LEFT));
		valueField = new FloatNumberTextField(0.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		valueField.setEnabled(false);
		valueField.setToolTipText("The current value of the output.");
		p.add(valueField);

		// row 11
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(100, 10, 400);
		widthField.setToolTipText("Type in the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 12
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(200, 10, 400);
		heightField.setToolTipText("Type in the height.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		ModelerUtilities.makeCompactGrid(p, 12, 2, 5, 5, 10, 2);

		/* right panel */

		box.add(Box.createHorizontalStrut(20));

		p = new JPanel(new SpringLayout());
		p.setPreferredSize(new Dimension(250, 300));
		box.add(p);

		// row 1
		s = Modeler.getInternationalText("OrientationLabel");
		p.add(new JLabel(s != null ? s : "Select orientation", SwingConstants.LEFT));
		orieComboBox = new JComboBox(new Object[] { "Horizontal", "Vertical" });
		orieComboBox.setToolTipText("Set the orientation of this bar graph.");
		p.add(orieComboBox);

		// row 2
		s = Modeler.getInternationalText("DrawTitle");
		p.add(new JLabel(s != null ? s : "Draw title", SwingConstants.LEFT));
		String[] yesno = new String[] { "Yes", "No" };
		s = Modeler.getInternationalText("Yes");
		if (s != null)
			yesno[0] = s;
		s = Modeler.getInternationalText("No");
		if (s != null)
			yesno[1] = s;
		titleComboBox = new JComboBox(yesno);
		titleComboBox.setToolTipText("Select yes if a title should be drawn; select no otherwise.");
		p.add(titleComboBox);

		// row 3
		s = Modeler.getInternationalText("DrawTicks");
		p.add(new JLabel(s != null ? s : "Draw ticks", SwingConstants.LEFT));
		tickComboBox = new JComboBox(yesno);
		tickComboBox.setToolTipText("Select yes if ticks should be drawn; select no otherwise.");
		p.add(tickComboBox);

		// row 4
		s = Modeler.getInternationalText("MajorTicks");
		p.add(new JLabel(s != null ? s : "Major ticks", SwingConstants.LEFT));
		majorTickSpinner = new JSpinner(new SpinnerNumberModel(pageBarGraph.getMajorTicks(), 0, 20, 1));
		((JSpinner.DefaultEditor) majorTickSpinner.getEditor()).getTextField().setToolTipText(
				"Select the major tick spacing.");
		((JSpinner.DefaultEditor) majorTickSpinner.getEditor()).getTextField().addActionListener(okListener);
		p.add(majorTickSpinner);

		// row 5
		s = Modeler.getInternationalText("MinorTicks");
		p.add(new JLabel(s != null ? s : "Minor ticks", SwingConstants.LEFT));
		minorTickSpinner = new JSpinner(new SpinnerNumberModel(pageBarGraph.getMinorTicks(), 0, 100, 1));
		((JSpinner.DefaultEditor) minorTickSpinner.getEditor()).getTextField().setToolTipText(
				"Select the minor tick spacing.");
		((JSpinner.DefaultEditor) minorTickSpinner.getEditor()).getTextField().addActionListener(okListener);
		p.add(minorTickSpinner);

		// row 6
		s = Modeler.getInternationalText("DigitalFormat");
		p.add(new JLabel(s != null ? s : "Digital format", SwingConstants.LEFT));
		formatComboBox = new JComboBox(new String[] { "Fixed point", "Scientific notation" });
		formatComboBox.setToolTipText("Select the digital format.");
		p.add(formatComboBox);

		// row 7
		s = Modeler.getInternationalText("MaximumFractionDigits");
		p.add(new JLabel(s != null ? s : "Maximum fraction digits", SwingConstants.LEFT));
		maximumFractionDigitField = new IntegerTextField(5, 0, 20);
		maximumFractionDigitField
				.setToolTipText("Sets the maximum number of digits allowed in the fraction portion of a number");
		maximumFractionDigitField.addActionListener(okListener);
		p.add(maximumFractionDigitField);

		// row 8
		s = Modeler.getInternationalText("MaximumIntegerDigits");
		p.add(new JLabel(s != null ? s : "Maximum integer digits", SwingConstants.LEFT));
		maximumIntegerDigitField = new IntegerTextField(1, 0, 20);
		maximumIntegerDigitField
				.setToolTipText("Sets the maximum number of digits allowed in the integer portion of a number");
		maximumIntegerDigitField.addActionListener(okListener);
		p.add(maximumIntegerDigitField);

		// row 9
		s = Modeler.getInternationalText("DrawLabels");
		p.add(new JLabel(s != null ? s : "Draw labels", SwingConstants.LEFT));
		labelComboBox = new JComboBox(yesno);
		labelComboBox.setToolTipText("Select yes if labels should be drawn; select no otherwise.");
		p.add(labelComboBox);

		// row 10
		s = Modeler.getInternationalText("BarColor");
		p.add(new JLabel(s != null ? s : "Bar color", SwingConstants.LEFT));
		s = Modeler.getInternationalText("FillingColor");
		fgComboBox = new ColorComboBox(pageBarGraph);
		fgComboBox.setSelectedIndex(0);
		fgComboBox.setRequestFocusEnabled(false);
		fgComboBox.setToolTipText("Select the color of the bar.");
		p.add(fgComboBox);

		// row 11
		p.add(new JLabel(s != null ? s : "Filling color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageBarGraph);
		bgComboBox.setMinimumSize(new Dimension(80, 24));
		bgComboBox.setSelectedIndex(6);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the filling color in the bar box.");
		p.add(bgComboBox);

		ModelerUtilities.makeCompactGrid(p, 11, 2, 5, 5, 10, 2);

	}

}