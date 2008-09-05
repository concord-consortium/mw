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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.RealNumberTextField;

/**
 * @author Charles Xie
 * 
 */
class PageTableMaker extends ComponentMaker {

	private PageTable pageTable;
	private JDialog dialog;
	private ColorComboBox bgColorComboBox, gridColorComboBox;
	private JComboBox borderComboBox;
	private JTextField columnNameField, rowNameField;
	private RealNumberTextField widthField, heightField;
	private JSpinner rowSpinner, columnSpinner;
	private JSpinner rowMarginSpinner, columnMarginSpinner;
	private JCheckBox transparentCheckBox, hlineCheckBox, vlineCheckBox;
	private JPanel contentPane;

	PageTableMaker(PageTable pageTable) {
		setObject(pageTable);
	}

	void setObject(PageTable pt) {
		pageTable = pt;
	}

	private void confirm() {
		pageTable.setTableBackground(bgColorComboBox.getSelectedColor());
		pageTable.setGridColor(gridColorComboBox.getSelectedColor());
		pageTable.setBorderType((String) borderComboBox.getSelectedItem());
		int nrow = ((Integer) rowSpinner.getValue()).intValue();
		int ncol = ((Integer) columnSpinner.getValue()).intValue();
		TableColumnModel tcm = pageTable.table.getColumnModel();
		int[] columnWidth = new int[ncol];
		if (ncol == pageTable.getColumnCount()) {
			for (int k = 0; k < ncol; k++) {
				columnWidth[k] = tcm.getColumn(k).getWidth();
			}
		}
		DefaultTableModel tm = (DefaultTableModel) pageTable.table.getModel();
		tm.setRowCount(nrow);
		tm.setColumnCount(ncol);
		pageTable.setColumnNames(columnNameField.getText());
		float w = (float) widthField.getValue();
		if (w < 1.05f) {
			pageTable.setWidthRatio(w);
			w *= pageTable.page.getWidth();
			pageTable.setWidthRelative(true);
		}
		float h = (float) heightField.getValue();
		if (h < 1.05f) {
			pageTable.setHeightRatio(h);
			h *= pageTable.page.getHeight();
			pageTable.setHeightRelative(true);
		}
		pageTable.setPreferredSize(new Dimension((int) w, (int) h));
		pageTable.setRowHeight((int) (((float) pageTable.getTableBodyHeight() / (float) nrow)));
		pageTable.setRowNames(rowNameField.getText());
		pageTable.setOpaque(!transparentCheckBox.isSelected());
		pageTable.table.setShowHorizontalLines(hlineCheckBox.isSelected());
		pageTable.table.setShowVerticalLines(vlineCheckBox.isSelected());
		if (ncol == pageTable.getColumnCount()) {
			for (int k = 0; k < ncol; k++) {
				tcm.getColumn(k).setPreferredWidth(columnWidth[k]);
			}
		}
		pageTable.page.getSaveReminder().setChanged(true);
		pageTable.page.settleComponentSize();
	}

	void invoke(Page page) {

		pageTable.page = page;
		page.deselect();
		createContentPane();

		if (needNewDialog(dialog, page)) {

			String s = Modeler.getInternationalText("CustomizeThisTable");
			dialog = ModelerUtilities.getChildDialog(page, s != null ? s : "Customize table", true);
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

		gridColorComboBox.setColor(pageTable.table.getGridColor());
		bgColorComboBox.setColor(pageTable.table.getBackground());
		borderComboBox.setSelectedItem(pageTable.getBorderType());
		if (pageTable.widthIsRelative) {
			widthField.setValue(pageTable.widthRatio);
		}
		else {
			widthField.setValue(pageTable.getPreferredSize().width);
		}
		if (pageTable.heightIsRelative) {
			heightField.setValue(pageTable.heightRatio);
		}
		else {
			heightField.setValue(pageTable.getPreferredSize().height);
		}
		rowSpinner.setValue(new Integer(pageTable.getRowCount()));
		columnSpinner.setValue(new Integer(pageTable.getColumnCount()));
		Dimension intercell = pageTable.table.getIntercellSpacing();
		rowMarginSpinner.setValue(new Integer(intercell.height));
		columnMarginSpinner.setValue(new Integer(intercell.width));
		hlineCheckBox.setSelected(pageTable.table.getShowHorizontalLines());
		vlineCheckBox.setSelected(pageTable.table.getShowVerticalLines());
		columnNameField.setText(pageTable.hasTableColumnHeader() ? pageTable.getColumnNames() : null);
		rowNameField.setText(pageTable.hasTableRowHeader() ? pageTable.getRowNames() : null);
		transparentCheckBox.setSelected(!pageTable.table.isOpaque());

		dialog.setVisible(true);

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
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(okListener);
		p.add(b);

		s = Modeler.getInternationalText("CancelButton");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				cancel = true;
			}
		});
		p.add(b);

		p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = Modeler.getInternationalText("NumberOfRows");
		p.add(new JLabel(s != null ? s : "Number of rows", SwingConstants.LEFT));
		rowSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
		p.add(rowSpinner);

		// row 2
		s = Modeler.getInternationalText("NumberOfColumns");
		p.add(new JLabel(s != null ? s : "Number of columns", SwingConstants.LEFT));
		columnSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
		p.add(columnSpinner);

		// row 3
		s = Modeler.getInternationalText("ColumnNames");
		p.add(new JLabel(s != null ? s : "Column names", SwingConstants.LEFT));
		columnNameField = new JTextField();
		columnNameField
				.setToolTipText("<html>Type the names of the columns, separated by a comma.<br>Don't enter anything if you don't want a column header.</html>");
		columnNameField.addActionListener(okListener);
		p.add(columnNameField);

		// row 4
		s = Modeler.getInternationalText("RowNames");
		p.add(new JLabel(s != null ? s : "Row names", SwingConstants.LEFT));
		rowNameField = new JTextField();
		rowNameField
				.setToolTipText("<html>Type the names of the rows, separated by a comma.<br>Don't enter anything if you don't want a row header.</html>");
		rowNameField.addActionListener(okListener);
		p.add(rowNameField);

		// row 5
		s = Modeler.getInternationalText("RowMargin");
		p.add(new JLabel(s != null ? s : "Row margin", SwingConstants.LEFT));
		rowMarginSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 50, 1));
		((JSpinner.DefaultEditor) rowMarginSpinner.getEditor()).getTextField().setToolTipText(
				"Set the amount of empty space between cells in adjacent rows.");
		rowMarginSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				pageTable.table.setRowMargin(((Integer) (rowMarginSpinner.getValue())).intValue());
			}
		});
		p.add(rowMarginSpinner);

		// row 6
		s = Modeler.getInternationalText("ColumnMargin");
		p.add(new JLabel(s != null ? s : "Column margin", SwingConstants.LEFT));
		columnMarginSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 50, 1));
		((JSpinner.DefaultEditor) columnMarginSpinner.getEditor()).getTextField().setToolTipText(
				"Set the amount of empty space between cells in adjacent columns.");
		columnMarginSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Dimension dim = pageTable.getIntercellSpacing();
				dim.width = ((Integer) (columnMarginSpinner.getValue())).intValue();
				pageTable.table.setIntercellSpacing(dim);
			}
		});
		p.add(columnMarginSpinner);

		// row 7
		s = Modeler.getInternationalText("WidthLabel");
		p.add(new JLabel(s != null ? s : "Width", SwingConstants.LEFT));
		widthField = new RealNumberTextField(400, 0.01, 800);
		widthField
				.setToolTipText("<html>Set the width of this table.<br>A value in (0, 1] will be considered relative to the width of the page.</html>");
		widthField.addActionListener(okListener);
		p.add(widthField);

		// row 8
		s = Modeler.getInternationalText("HeightLabel");
		p.add(new JLabel(s != null ? s : "Height", SwingConstants.LEFT));
		heightField = new RealNumberTextField(200, 0.01, 800);
		heightField.setToolTipText("Set the height of this table.");
		heightField.addActionListener(okListener);
		p.add(heightField);

		// row 9
		s = Modeler.getInternationalText("GridLineColor");
		p.add(new JLabel(s != null ? s : "Grid line color", SwingConstants.LEFT));
		gridColorComboBox = new ColorComboBox(pageTable);
		gridColorComboBox.setToolTipText("Select the color for the grid lines.");
		p.add(gridColorComboBox);

		// row 12
		s = Modeler.getInternationalText("BackgroundColorLabel");
		p.add(new JLabel(s != null ? s : "Background color", SwingConstants.LEFT));
		bgColorComboBox = new ColorComboBox(pageTable);
		bgColorComboBox.setToolTipText("Select the background color.");
		p.add(bgColorComboBox);

		// row 13
		s = Modeler.getInternationalText("BorderLabel");
		p.add(new JLabel(s != null ? s : "Border", SwingConstants.LEFT));
		borderComboBox = new JComboBox(BorderManager.BORDER_TYPE);
		borderComboBox.setRenderer(new ComboBoxRenderer.BorderCell());
		borderComboBox.setBackground(p.getBackground());
		borderComboBox.setToolTipText("Select the border type for this table.");
		borderComboBox.setPreferredSize(new Dimension(100, 24));
		p.add(borderComboBox);

		ModelerUtilities.makeCompactGrid(p, 11, 2, 5, 5, 15, 5);

		p = new JPanel();
		contentPane.add(p, BorderLayout.CENTER);

		s = Modeler.getInternationalText("TransparencyCheckBox");
		transparentCheckBox = new JCheckBox(s != null ? s : "Transparent");
		transparentCheckBox.setToolTipText("Select to set the table to be transparent.");
		p.add(transparentCheckBox);

		s = Modeler.getInternationalText("DrawHorizontalLines");
		hlineCheckBox = new JCheckBox(s != null ? s : "Horizontal lines");
		hlineCheckBox.setToolTipText("Select to draw horizontal lines.");
		p.add(hlineCheckBox);

		s = Modeler.getInternationalText("DrawVerticalLines");
		vlineCheckBox = new JCheckBox(s != null ? s : "Vertical lines");
		vlineCheckBox.setToolTipText("Select to draw vertical lines.");
		p.add(vlineCheckBox);

	}

}