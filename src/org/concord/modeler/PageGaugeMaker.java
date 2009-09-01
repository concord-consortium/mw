/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
import java.util.Map;

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
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.QueueGroup;
import org.concord.mw2d.models.MDModel;

/**
 * @author Charles Xie
 * 
 */
class PageGaugeMaker extends ComponentMaker {

	private PageGauge pageGauge;
	private int filterType;
	private JDialog dialog;
	private JTextField uidField;
	private JTextField descriptionField;
	private JComboBox modelComboBox, timeSeriesComboBox, filterComboBox;
	private JComboBox borderComboBox, tickComboBox, labelComboBox, titleComboBox;
	private JComboBox typeComboBox;
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
	private JLabel parameterLabel;
	private FloatNumberTextField parameterField;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageGauge);
			}
			else {
				m.addModelListener(pageGauge);
				pageGauge.setModelID(pageGauge.page.getComponentPool().getIndex(m));
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
					pageGauge.setValue(((FloatQueue) obj).getCurrentValue());
					maxField.setValue((float) ((FloatQueue) obj).getReferenceUpperBound());
					minField.setValue((float) ((FloatQueue) obj).getReferenceLowerBound());
				}
				valueField.setValue((float) pageGauge.getValue());
				descriptionField.setText(obj.toString());
			}
			catch (Exception exception) {
				valueField.setText("Unknown");
			}
		}
	};

	PageGaugeMaker(PageGauge pg) {
		setObject(pg);
	}

	void setObject(PageGauge pbg) {
		pageGauge = pbg;
	}

	private boolean confirm() {
		if (minField.getValue() >= maxField.getValue()) {
			JOptionPane.showMessageDialog(dialog, "Upper bound must be greater than lower bound.", "Input error",
					JOptionPane.ERROR_MESSAGE);
			focusTextComponent = maxField;
			return false;
		}
		String s = uidField.getText();
		if (s != null) {
			s = s.trim();
			pageGauge.setUid(s.equals("") ? null : s);
		}
		else {
			pageGauge.setUid(null);
		}
		switch (typeComboBox.getSelectedIndex()) {
		case 0:
			pageGauge.setAverageType(PageGauge.INSTANTANEOUS);
			break;
		case 1:
			pageGauge.setAverageType(PageGauge.GROWING_POINT_RUNNING_AVERAGE);
			break;
		case 2:
			pageGauge.setAverageType(PageGauge.SIMPLE_RUNNING_AVERAGE);
			int n = (int) parameterField.getValue();
			if (n < 10 || n > Modeler.tapeLength) {
				JOptionPane.showMessageDialog(dialog, "The number of sampling points you set " + n + " is illegal.",
						"Sampling points error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			pageGauge.setSamplingPoints(n);
			break;
		case 3:
			pageGauge.setAverageType(PageGauge.EXPONENTIAL_RUNNING_AVERAGE);
			float sf = parameterField.getValue();
			if (sf < 0 || sf > 1) {
				JOptionPane.showMessageDialog(dialog, "The smoothing factor you set " + sf + " is illegal.",
						"Smoothing factor error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			pageGauge.setSmoothingFactor(sf);
			break;
		}
		pageGauge.setFormat((String) formatComboBox.getSelectedItem());
		pageGauge.setMaximumFractionDigits(maximumFractionDigitField.getValue());
		pageGauge.setMaximumIntegerDigits(maximumIntegerDigitField.getValue());
		pageGauge.setMinorTicks(((Integer) minorTickSpinner.getValue()));
		pageGauge.setMajorTicks(((Integer) majorTickSpinner.getValue()));
		pageGauge.setBackground(bgComboBox.getSelectedColor());
		pageGauge.setForeground(fgComboBox.getSelectedColor());
		pageGauge.setBorderType((String) borderComboBox.getSelectedItem());
		pageGauge.setPaintTitle(titleComboBox.getSelectedIndex() == 0);
		pageGauge.setPaintTicks(tickComboBox.getSelectedIndex() == 0);
		pageGauge.setPaintLabels(labelComboBox.getSelectedIndex() == 0);
		pageGauge.setMinimum(minField.getValue());
		pageGauge.setMaximum(maxField.getValue());
		Object obj = timeSeriesComboBox.getSelectedItem();
		if (obj instanceof DataQueue) {
			pageGauge.setTimeSeriesName(((DataQueue) obj).getName());
		}
		if (descriptionField.getText() != null && !descriptionField.getText().trim().equals(""))
			pageGauge.setDescription(descriptionField.getText());
		Model m = (Model) modelComboBox.getSelectedItem();
		m.addModelListener(pageGauge);
		m.getMovie().addMovieListener(pageGauge);
		pageGauge.setModelClass(m.getClass().getName());
		if (m instanceof MDModel) {
			pageGauge.setModelID(pageGauge.page.getComponentPool().getIndex(m));
		}
		else if (m instanceof Embeddable) {
			pageGauge.setModelID(((Embeddable) m).getIndex());
		}
		pageGauge.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageGauge.setChangable(true);
		pageGauge.page.getSaveReminder().setChanged(true);
		pageGauge.page.settleComponentSize();
		return true;
	}

	void invoke(Page page) {

		pageGauge.page = page;
		page.deselect();
		focusTextComponent = null;
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeGaugeDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize gauge", true);
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
						descriptionField.requestFocusInWindow();
					}
					else {
						focusTextComponent.selectAll();
						focusTextComponent.requestFocusInWindow();
					}
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		timeSeriesComboBox.removeItemListener(timeSeriesSelectionListener);
		filterComboBox.setSelectedIndex(0);
		switch (pageGauge.getAverageType()) {
		case PageGauge.INSTANTANEOUS:
			typeComboBox.setSelectedIndex(0);
			parameterLabel.setEnabled(false);
			String s = Modeler.getInternationalText("Parameter");
			parameterLabel.setText(s != null ? s : "Parameter");
			parameterField.setText(null);
			parameterField.setEditable(false);
			break;
		case PageGauge.GROWING_POINT_RUNNING_AVERAGE:
			typeComboBox.setSelectedIndex(1);
			parameterLabel.setEnabled(false);
			s = Modeler.getInternationalText("Parameter");
			parameterLabel.setText(s != null ? s : "Parameter");
			parameterField.setText(null);
			parameterField.setEditable(false);
			break;
		case PageGauge.SIMPLE_RUNNING_AVERAGE:
			typeComboBox.setSelectedIndex(2);
			parameterLabel.setEnabled(true);
			s = Modeler.getInternationalText("SamplingPoints");
			parameterLabel.setText(s != null ? s : "Sampling points");
			parameterField.setValue(pageGauge.samplingPoints);
			parameterField.setMinValue(10);
			parameterField.setMaxValue(Modeler.tapeLength);
			parameterField.setEditable(true);
			break;
		case PageGauge.EXPONENTIAL_RUNNING_AVERAGE:
			typeComboBox.setSelectedIndex(3);
			parameterLabel.setEnabled(true);
			s = Modeler.getInternationalText("SmoothingFactor");
			parameterLabel.setText(s != null ? s : "Smoothing factor");
			parameterField.setValue(pageGauge.smoothingFactor);
			parameterField.setMinValue(0);
			parameterField.setMaxValue(1);
			parameterField.setEditable(true);
			break;
		}

		uidField.setText(pageGauge.getUid());
		titleComboBox.setSelectedIndex(pageGauge.getPaintTitle() ? 0 : 1);
		tickComboBox.setSelectedIndex(pageGauge.getPaintTicks() ? 0 : 1);
		labelComboBox.setSelectedIndex(pageGauge.getPaintLabels() ? 0 : 1);
		descriptionField.setText(pageGauge.getDescription());
		minField.setValue((float) pageGauge.getMinimum());
		maxField.setValue((float) pageGauge.getMaximum());
		valueField.setValue((float) pageGauge.getValue());
		if (pageGauge.isMaximumSizeSet()) {
			widthField.setValue(pageGauge.getMaximumSize().width);
			heightField.setValue(pageGauge.getMaximumSize().height);
		}

		// add legacy MD models to the model list
		ComponentPool componentPool = page.getComponentPool();
		synchronized (componentPool) {
			for (ModelCanvas mc : componentPool.getModels()) {
				if (mc.isUsed()) {
					modelComboBox.addItem(mc.getMdContainer().getModel());
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

		if (pageGauge.isTargetClass()) {
			if (pageGauge.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageGauge.modelClass), pageGauge.modelID);
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
					pageGauge.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageGauge.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageGauge.modelID);
				modelComboBox.setSelectedItem(mc.getMdContainer().getModel());
				mc.getMdContainer().getModel().addModelListener(pageGauge);
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				if (m != null) {
					pageGauge.setModelID(componentPool.getIndex(m));
					m.addModelListener(pageGauge);
				}
			}
		}

		modelComboBox.addItemListener(modelSelectionListener);
		fillTimeSeriesComboBox();
		timeSeriesComboBox.addItemListener(timeSeriesSelectionListener);

		formatComboBox.setSelectedItem(pageGauge.getFormat());
		maximumFractionDigitField.setValue(pageGauge.getMaximumFractionDigits());
		maximumIntegerDigitField.setValue(pageGauge.getMaximumIntegerDigits());
		majorTickSpinner.setValue(pageGauge.getMajorTicks());
		minorTickSpinner.setValue(pageGauge.getMinorTicks());
		bgComboBox.setColor(pageGauge.getBackground());
		fgComboBox.setColor(pageGauge.getForeground());
		borderComboBox.setSelectedItem(pageGauge.getBorderType());
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
			if (q.getName().equals(pageGauge.timeSeriesName)) {
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
			throw new IllegalStateException("More than one time series are using this gauge (" + guard + ")");

		/*
		 * when no time series is using this gauge, display in the value field the newest value of this first time
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

		s = Modeler.getInternationalText("Help");
		button = new JButton(s != null ? s : "Help");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Modeler.openWithNewInstance(pageGauge.getPage().getNavigator().getHomeDirectory()
						+ "tutorial/insertGauge.cml");
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
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one that outputs to this gauge.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectVariableLabel");
		p.add(new JLabel(s != null ? s : "Select a variable", SwingConstants.LEFT));
		timeSeriesComboBox = new JComboBox();
		timeSeriesComboBox.setFont(smallFont);
		timeSeriesComboBox.setToolTipText("Select the time series output to be displayed in this gauge.");
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
		s = Modeler.getInternationalText("UniqueIdentifier");
		p.add(new JLabel((s != null ? s : "Unique identifier") + " (A-z, 0-9)", SwingConstants.LEFT));
		uidField = new JTextField();
		uidField.setToolTipText("Type in a string to be used as the unique identifier of this gauge.");
		uidField.addActionListener(okListener);
		p.add(uidField);

		// row 5
		s = Modeler.getInternationalText("Type");
		p.add(new JLabel(s != null ? s : "Type", SwingConstants.LEFT));
		typeComboBox = new JComboBox(new String[] { "Instantaneous", "Growing-point running average",
				"Simple running average", "Exponential running average" });
		typeComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				switch (typeComboBox.getSelectedIndex()) {
				case 0:
				case 1:
					String s = Modeler.getInternationalText("Parameter");
					parameterLabel.setText(s != null ? s : "Parameter");
					parameterLabel.setEnabled(false);
					parameterField.setText(null);
					parameterField.setEditable(false);
					break;
				case 2:
					s = Modeler.getInternationalText("SamplingPoints");
					parameterLabel.setText(s != null ? s : "Sampling points");
					parameterLabel.setEnabled(true);
					parameterField.setValue(pageGauge.samplingPoints);
					parameterField.setEditable(true);
					parameterField.setMinValue(10);
					parameterField.setMaxValue(Modeler.tapeLength);
					break;
				case 3:
					s = Modeler.getInternationalText("SmoothingFactor");
					parameterLabel.setText(s != null ? s : "Smoothing factor");
					parameterLabel.setEnabled(true);
					parameterField.setValue(pageGauge.smoothingFactor);
					parameterField.setEditable(true);
					parameterField.setMinValue(0);
					parameterField.setMaxValue(1);
					break;
				}
			}
		});
		p.add(typeComboBox);

		// row 6
		parameterLabel = new JLabel();
		p.add(parameterLabel);
		parameterField = new FloatNumberTextField(0.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		parameterField.addActionListener(okListener);
		p.add(parameterField);

		// row 7
		s = Modeler.getInternationalText("TextLabel");
		p.add(new JLabel(s != null ? s : "Description", SwingConstants.LEFT));
		descriptionField = new JTextField();
		descriptionField.addActionListener(okListener);
		descriptionField.setToolTipText("Type in the text to be displayed in the gauge.");
		p.add(descriptionField);

		// row 8
		s = Modeler.getInternationalText("UpperBoundLabel");
		p.add(new JLabel(s != null ? s : "Set upper bound", SwingConstants.LEFT));
		maxField = new FloatNumberTextField(1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		maxField.addActionListener(okListener);
		maxField.setToolTipText("Type in the upper bound this gauge displays.");
		p.add(maxField);

		// row 9
		s = Modeler.getInternationalText("LowerBoundLabel");
		p.add(new JLabel(s != null ? s : "Set lower bound", SwingConstants.LEFT));
		minField = new FloatNumberTextField(-1.0f, -Float.MAX_VALUE, Float.MAX_VALUE);
		minField.addActionListener(okListener);
		minField.setToolTipText("Type in the lower bound this gauge displays.");
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
		widthField = new IntegerTextField(100, 10, 800);
		widthField.setToolTipText("Type in the width.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 12
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(200, 10, 800);
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

		// row 2
		s = Modeler.getInternationalText("DrawTicks");
		p.add(new JLabel(s != null ? s : "Draw ticks", SwingConstants.LEFT));
		tickComboBox = new JComboBox(yesno);
		tickComboBox.setToolTipText("Select yes if ticks should be drawn; select no otherwise.");
		p.add(tickComboBox);

		// row 3
		s = Modeler.getInternationalText("MajorTicks");
		p.add(new JLabel(s != null ? s : "Major ticks", SwingConstants.LEFT));
		majorTickSpinner = new JSpinner(new SpinnerNumberModel(pageGauge.getMajorTicks(), 0, 20, 1));
		((JSpinner.DefaultEditor) majorTickSpinner.getEditor()).getTextField().setToolTipText(
				"Select the major tick spacing.");
		((JSpinner.DefaultEditor) majorTickSpinner.getEditor()).getTextField().addActionListener(okListener);
		p.add(majorTickSpinner);

		// row 4
		s = Modeler.getInternationalText("MinorTicks");
		p.add(new JLabel(s != null ? s : "Minor ticks", SwingConstants.LEFT));
		minorTickSpinner = new JSpinner(new SpinnerNumberModel(pageGauge.getMinorTicks(), 0, 100, 1));
		((JSpinner.DefaultEditor) minorTickSpinner.getEditor()).getTextField().setToolTipText(
				"Select the minor tick spacing.");
		((JSpinner.DefaultEditor) minorTickSpinner.getEditor()).getTextField().addActionListener(okListener);
		p.add(minorTickSpinner);

		// row 5
		s = Modeler.getInternationalText("DigitalFormat");
		p.add(new JLabel(s != null ? s : "Digital format", SwingConstants.LEFT));
		formatComboBox = new JComboBox(new String[] { "Fixed point", "Scientific notation" });
		formatComboBox.setToolTipText("Select the digital format.");
		p.add(formatComboBox);

		// row 6
		s = Modeler.getInternationalText("MaximumFractionDigits");
		p.add(new JLabel(s != null ? s : "Maximum fraction digits", SwingConstants.LEFT));
		maximumFractionDigitField = new IntegerTextField(5, 0, 20);
		maximumFractionDigitField
				.setToolTipText("Sets the maximum number of digits allowed in the fraction portion of a number");
		maximumFractionDigitField.addActionListener(okListener);
		p.add(maximumFractionDigitField);

		// row 7
		s = Modeler.getInternationalText("MaximumIntegerDigits");
		p.add(new JLabel(s != null ? s : "Maximum integer digits", SwingConstants.LEFT));
		maximumIntegerDigitField = new IntegerTextField(1, 0, 20);
		maximumIntegerDigitField
				.setToolTipText("Sets the maximum number of digits allowed in the integer portion of a number");
		maximumIntegerDigitField.addActionListener(okListener);
		p.add(maximumIntegerDigitField);

		// row 8
		s = Modeler.getInternationalText("DrawLabels");
		p.add(new JLabel(s != null ? s : "Draw labels", SwingConstants.LEFT));
		labelComboBox = new JComboBox(yesno);
		labelComboBox.setToolTipText("Select yes if labels should be drawn; select no otherwise.");
		p.add(labelComboBox);

		// row 9
		s = Modeler.getInternationalText("ForegroundColorLabel");
		p.add(new JLabel(s != null ? s : "Foreground color", SwingConstants.LEFT));
		fgComboBox = new ColorComboBox(pageGauge);
		fgComboBox.setSelectedIndex(0);
		fgComboBox.setRequestFocusEnabled(false);
		fgComboBox.setToolTipText("Select the foreground color.");
		p.add(fgComboBox);

		// row 10
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgComboBox = new ColorComboBox(pageGauge);
		bgComboBox.setMinimumSize(new Dimension(80, 24));
		bgComboBox.setSelectedIndex(6);
		bgComboBox.setRequestFocusEnabled(false);
		bgComboBox.setToolTipText("Select the background color.");
		p.add(bgComboBox);

		// row 11
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border type", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		p.add(borderComboBox);

		// row 12
		p.add(new JPanel());
		p.add(new JPanel());

		ModelerUtilities.makeCompactGrid(p, 12, 2, 5, 5, 10, 2);

	}

}