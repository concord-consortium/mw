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
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

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
class PageNumericBoxMaker extends ComponentMaker {

	private PageNumericBox pageNumericBox;
	private JDialog dialog;
	private JComboBox modelComboBox;
	private JComboBox timeSeriesComboBox;
	private JComboBox filterComboBox;
	private JComboBox dataComboBox;
	private JComboBox formatComboBox;
	private JComboBox fontNameComboBox;
	private JComboBox fontSizeComboBox;
	private JComboBox borderComboBox;
	private ColorComboBox fontColorComboBox;
	private IntegerTextField maximumFractionDigitField;
	private IntegerTextField maximumIntegerDigitField;
	private IntegerTextField widthField, heightField;
	private FloatNumberTextField multiplierField;
	private FloatNumberTextField addendField;
	private JButton okButton;
	private int filterType;
	private JPanel contentPane;
	private static Font smallFont;

	private ItemListener modelSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			Model m = (Model) modelComboBox.getSelectedItem();
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				m.removeModelListener(pageNumericBox);
			}
			else {
				m.addModelListener(pageNumericBox);
				pageNumericBox.setModelID(pageNumericBox.page.getComponentPool().getIndex(m));
				timeSeriesComboBox.removeAllItems();
				fillTimeSeriesComboBox();
			}
		}
	};

	private ItemListener timeSeriesSelectionListener = new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.DESELECTED)
				return;
			Object obj = timeSeriesComboBox.getSelectedItem();
			if (obj instanceof FloatQueue) {
				switch (pageNumericBox.dataType) {
				case PageNumericBox.INSTANTANEOUS:
					pageNumericBox.setValue(((FloatQueue) obj).getCurrentValue());
					break;
				case PageNumericBox.AVERAGE:
					pageNumericBox.setValue(((FloatQueue) obj).getAverage());
					break;
				case PageNumericBox.RMSD:
					pageNumericBox.setValue(((FloatQueue) obj).getRMSDeviation());
					break;
				}
			}
		}
	};

	PageNumericBoxMaker(PageNumericBox pnb) {
		setObject(pnb);
	}

	void setObject(PageNumericBox pnb) {
		pageNumericBox = pnb;
	}

	private void confirm() {
		pageNumericBox.setBorderType((String) borderComboBox.getSelectedItem());
		pageNumericBox.setForeground(fontColorComboBox.getSelectedColor());
		String fontType = (String) fontNameComboBox.getSelectedItem();
		int fontSize = (Integer) fontSizeComboBox.getSelectedItem();
		pageNumericBox.setFont(new Font(fontType, Font.PLAIN, fontSize));
		pageNumericBox.setFormat((String) formatComboBox.getSelectedItem());
		pageNumericBox.multiplier = multiplierField.getValue();
		pageNumericBox.addend = addendField.getValue();
		pageNumericBox.formatter.setMaximumFractionDigits(maximumFractionDigitField.getValue());
		pageNumericBox.formatter.setMaximumIntegerDigits(maximumIntegerDigitField.getValue());
		switch (dataComboBox.getSelectedIndex()) {
		case 0:
			pageNumericBox.setDataType(PageNumericBox.INSTANTANEOUS);
			break;
		case 1:
			pageNumericBox.setDataType(PageNumericBox.AVERAGE);
			break;
		case 2:
			pageNumericBox.setDataType(PageNumericBox.RMSD);
			break;
		}
		pageNumericBox.setValue(pageNumericBox.value);
		Object obj = timeSeriesComboBox.getSelectedItem();
		if (obj instanceof DataQueue) {
			pageNumericBox.setDescription(((DataQueue) obj).getName());
		}
		Model m = (Model) modelComboBox.getSelectedItem();
		m.addModelListener(pageNumericBox);
		m.getMovie().addMovieListener(pageNumericBox);
		pageNumericBox.setModelClass(m.getClass().getName());
		if (m instanceof MDModel) {
			pageNumericBox.setModelID(pageNumericBox.page.getComponentPool().getIndex(m));
		}
		else if (m instanceof Embeddable) {
			pageNumericBox.setModelID(((Embeddable) m).getIndex());
		}
		pageNumericBox.setPreferredSize(new Dimension(widthField.getValue(), heightField.getValue()));
		pageNumericBox.page.getSaveReminder().setChanged(true);
		pageNumericBox.page.settleComponentSize();
	}

	void invoke(Page page) {

		pageNumericBox.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {
			String s = Modeler.getInternationalText("CustomizeNumericBoxDialogTitle");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize numeric box", true);
			dialog.setContentPane(contentPane);
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel = true;
					dialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					widthField.selectAll();
					widthField.requestFocus();
				}
			});
		}

		modelComboBox.removeItemListener(modelSelectionListener);
		modelComboBox.removeAllItems();
		timeSeriesComboBox.removeItemListener(timeSeriesSelectionListener);
		filterComboBox.setSelectedIndex(0);

		if (pageNumericBox.isMaximumSizeSet()) {
			widthField.setValue(pageNumericBox.getMaximumSize().width);
			heightField.setValue(pageNumericBox.getMaximumSize().height);
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

		if (pageNumericBox.isTargetClass()) {
			if (pageNumericBox.modelID != -1) {
				try {
					Object o = page.getEmbeddedComponent(Class.forName(pageNumericBox.modelClass),
							pageNumericBox.modelID);
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
					pageNumericBox.setModelID(((Embeddable) m).getIndex());
			}
		}
		else {
			if (pageNumericBox.modelID != -1) {
				ModelCanvas mc = componentPool.get(pageNumericBox.modelID);
				modelComboBox.setSelectedItem(mc.getContainer().getModel());
				mc.getContainer().getModel().addModelListener(pageNumericBox);
			}
			else {
				Model m = (Model) modelComboBox.getSelectedItem();
				if (m != null) {
					pageNumericBox.setModelID(componentPool.getIndex(m));
					m.addModelListener(pageNumericBox);
				}
			}
		}

		modelComboBox.addItemListener(modelSelectionListener);
		fillTimeSeriesComboBox();
		timeSeriesComboBox.addItemListener(timeSeriesSelectionListener);

		formatComboBox.setSelectedItem(pageNumericBox.getFormat());
		switch (pageNumericBox.dataType) {
		case PageNumericBox.INSTANTANEOUS:
			dataComboBox.setSelectedIndex(0);
			break;
		case PageNumericBox.AVERAGE:
			dataComboBox.setSelectedIndex(1);
			break;
		case PageNumericBox.RMSD:
			dataComboBox.setSelectedIndex(2);
			break;
		}
		multiplierField.setValue(pageNumericBox.multiplier);
		addendField.setValue(pageNumericBox.addend);
		maximumFractionDigitField.setValue(pageNumericBox.formatter.getMaximumFractionDigits());
		maximumIntegerDigitField.setValue(pageNumericBox.formatter.getMaximumIntegerDigits());
		fontColorComboBox.setColor(pageNumericBox.getForeground());
		fontNameComboBox.setSelectedItem(pageNumericBox.getFont().getName());
		fontSizeComboBox.setSelectedItem(new Integer(pageNumericBox.getFont().getSize()));
		borderComboBox.setSelectedItem(pageNumericBox.getBorderType());
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
			if (q.getName().equals(pageNumericBox.getDescription())) {
				timeSeriesComboBox.setSelectedItem(q);
				guard++;
			}
		}

		if (guard >= 2)
			throw new IllegalStateException("More than one time series are using this bar graph (" + guard + ")");

	}

	private void createContentPane() {

		if (contentPane != null)
			return;

		contentPane = new JPanel(new BorderLayout());

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel = false;
				confirm();
				dialog.dispose();
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

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.CENTER);

		// row 1
		s = Modeler.getInternationalText("SelectModelLabel");
		p.add(new JLabel(s != null ? s : "Select a model", SwingConstants.LEFT));
		modelComboBox = new JComboBox();
		if (smallFont == null)
			smallFont = new Font(modelComboBox.getFont().getFamily(), modelComboBox.getFont().getStyle(), 10);
		modelComboBox.setFont(smallFont);
		modelComboBox.setRenderer(new LabelRenderer());
		modelComboBox.setPreferredSize(new Dimension(200, 20));
		modelComboBox
				.setToolTipText("If there are multiple models on the page, select the one that outputs to this numeric box.");
		p.add(modelComboBox);

		// row 2
		s = Modeler.getInternationalText("SelectVariableLabel");
		p.add(new JLabel(s != null ? s : "Select a variable", SwingConstants.LEFT));
		timeSeriesComboBox = new JComboBox();
		timeSeriesComboBox.setFont(smallFont);
		timeSeriesComboBox.setToolTipText("Select the time series output to be displayed in this numeric box.");
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
		s = Modeler.getInternationalText("NumericDataType");
		p.add(new JLabel(s != null ? s : "Data type", SwingConstants.LEFT));
		dataComboBox = new JComboBox(
				new String[] { "Instantaneous value", "Time average", "Root mean square deviation" });
		dataComboBox
				.setToolTipText("Select the instantaneous value, time average value, or root mean square deviation to display.");
		p.add(dataComboBox);

		// row 5
		p.add(new JLabel("Multiplier", SwingConstants.LEFT));
		multiplierField = new FloatNumberTextField(1, -Float.MAX_VALUE, Float.MAX_VALUE);
		multiplierField.setToolTipText("Type in a value to multiply the output.");
		multiplierField.addActionListener(okListener);
		p.add(multiplierField);

		// row 6
		p.add(new JLabel("Addend", SwingConstants.LEFT));
		addendField = new FloatNumberTextField(0, -Float.MAX_VALUE, Float.MAX_VALUE);
		addendField.setToolTipText("Type in a value to be added to the output.");
		addendField.addActionListener(okListener);
		p.add(addendField);

		// row 7
		s = Modeler.getInternationalText("DigitalFormat");
		p.add(new JLabel(s != null ? s : "Digital format", SwingConstants.LEFT));
		formatComboBox = new JComboBox(new String[] { "Fixed point", "Scientific notation" });
		formatComboBox.setToolTipText("Select the digital format to display numbers.");
		p.add(formatComboBox);

		// row 8
		s = Modeler.getInternationalText("MaximumFractionDigits");
		p.add(new JLabel(s != null ? s : "Maximum fraction digits", SwingConstants.LEFT));
		maximumFractionDigitField = new IntegerTextField(5, 0, 20);
		maximumFractionDigitField
				.setToolTipText("Sets the maximum number of digits allowed in the fraction portion of a number");
		maximumFractionDigitField.addActionListener(okListener);
		p.add(maximumFractionDigitField);

		// row 9
		s = Modeler.getInternationalText("MaximumIntegerDigits");
		p.add(new JLabel(s != null ? s : "Maximum integer digits", SwingConstants.LEFT));
		maximumIntegerDigitField = new IntegerTextField(1, 0, 20);
		maximumIntegerDigitField
				.setToolTipText("Sets the maximum number of digits allowed in the integer portion of a number");
		maximumIntegerDigitField.addActionListener(okListener);
		p.add(maximumIntegerDigitField);

		// row 10
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new IntegerTextField(SwingUtilities.computeStringWidth(pageNumericBox
				.getFontMetrics(pageNumericBox.getFont()), pageNumericBox.getText()) + 20, 10, 200);
		widthField.setToolTipText("Type in an integer to set the width of this box.");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 11
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new IntegerTextField(20, 10, 200);
		heightField.setToolTipText("Type in an integer to set the height of this box.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 12
		s = Modeler.getInternationalText("NumericFontColor");
		p.add(new JLabel(s != null ? s : "Font color", SwingConstants.LEFT));
		fontColorComboBox = new ColorComboBox(pageNumericBox);
		fontColorComboBox.setToolTipText("Select the font color.");
		fontColorComboBox.setRequestFocusEnabled(false);
		p.add(fontColorComboBox);

		// row 13
		s = Modeler.getInternationalText("NumericFontType");
		p.add(new JLabel(s != null ? s : "Font type", SwingConstants.LEFT));
		fontNameComboBox = ModelerUtilities.createFontNameComboBox();
		fontNameComboBox.setSelectedItem(Page.getDefaultFontFamily());
		p.add(fontNameComboBox);

		// row 14
		s = Modeler.getInternationalText("NumericFontSize");
		p.add(new JLabel(s != null ? s : "Font size", SwingConstants.LEFT));
		fontSizeComboBox = ModelerUtilities.createFontSizeComboBox();
		fontSizeComboBox.setSelectedItem(new Integer(Page.getDefaultFontSize()));
		p.add(fontSizeComboBox);

		// row 15
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type.");
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 15, 2, 5, 5, 10, 2);

	}

}