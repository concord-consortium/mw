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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JOptionPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * This is a text field for receiving only double-precision real number inputs. It does not accept non-digit inputs, nor
 * a value out of the specified bounds.
 * 
 * @author Charles Xie
 */

public class RealNumberTextField extends PastableTextField {

	private NumberFormat formatter;
	private double max = 100.0, min = -100.0, prefer = 0.0;
	private boolean checkBounds;

	public RealNumberTextField() {
		super();
		init();
	}

	public RealNumberTextField(double min, double max) throws IllegalArgumentException {
		this();
		if (max < min)
			throw new IllegalArgumentException("min cannot be greater than max.");
		this.max = max;
		this.min = min;
	}

	public RealNumberTextField(double value, double min, double max) throws IllegalArgumentException {
		this();
		if (max < min)
			throw new IllegalArgumentException("min cannot be greater than max.");
		this.max = max;
		this.min = min;
		prefer = value;
		setValue(value);
	}

	public RealNumberTextField(double value, double min, double max, int columns) throws IllegalArgumentException {
		super(columns);
		if (max < min)
			throw new IllegalArgumentException("min cannot be greater than max.");
		this.max = max;
		this.min = min;
		prefer = value;
		init();
		setValue(value);
	}

	public void setCheckBounds(boolean b) {
		checkBounds = b;
	}

	public boolean getCheckBounds() {
		return checkBounds;
	}

	public void setMaximumFractionDigits(int i) {
		if (formatter == null)
			return;
		formatter.setMaximumFractionDigits(i);
	}

	public void setMaximumIntegerDigits(int i) {
		if (formatter == null)
			return;
		formatter.setMaximumIntegerDigits(i);
	}

	public void setMaxValue(double d) {
		max = d;
	}

	public double getMaxValue() {
		return max;
	}

	public void setMinValue(double d) {
		min = d;
	}

	public double getMinValue() {
		return min;
	}

	public void setPreferredValue(double d) {
		prefer = d;
	}

	public double getPreferredValue() {
		return prefer;
	}

	public double getValue() {
		double retVal = 0;
		try {
			retVal = formatter.parse(getText()).doubleValue();
		}
		catch (ParseException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"The input does not contain any data, or\ncontains data that cannot be parsed.", "Parsing error",
					JOptionPane.ERROR_MESSAGE);
			setValue(min);
			return min;
		}
		if (checkBounds) {
			if (retVal < min)
				return min;
			if (retVal > max)
				return max;
		}
		return retVal;
	}

	public void setValue(double value) {
		if (checkBounds) {
			if (value > max)
				value = max;
			else if (value < min)
				value = min;
		}
		setText(formatter.format(value));
	}

	protected Document createDefaultModel() {

		return new PlainDocument() {
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

				int countDot = 0;
				char[] old = RealNumberTextField.this.getText().toCharArray();

				int i;
				for (i = 0; i < old.length; i++) {
					if (old[i] == '.' || old[i] == ',') {
						countDot++;
					}
				}

				char[] source = str.toCharArray();
				char[] result = new char[source.length];
				int j = 0;

				for (i = 0; i < result.length; i++) {
					if (Character.isDigit(source[i])) {
						result[j++] = source[i];
					}
					else if (source[i] == '.' || source[i] == ',') {
						if (countDot == 0)
							result[j++] = source[i];
					}
					else if (source[i] == '-' && min < 0.0) {
						if (old.length > 0 && old[0] != '-') {
							offs = 0;
							result[j++] = source[i];
						}
						else if (old.length == 0) {
							result[j++] = source[i];
						}
					}
				}

				super.insertString(offs, new String(result, 0, j), a);

			}

		};

	}

	private boolean noText() {
		return getText() == null || getText().trim().equals("");
	}

	private void init() {
		formatter = NumberFormat.getNumberInstance();
		formatter.setParseIntegerOnly(false);
		formatter.setMaximumFractionDigits(8);
		formatter.setMaximumIntegerDigits(8);
		setEditable(true);
		setBackground(Color.white);
		setHorizontalAlignment(LEFT);
		addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (noText()) {
					setValue(min);
				}
				else {
					double d = getValue();
					if (d > max)
						setValue(max);
					else if (d < min)
						setValue(min);
				}
			}
		});
	}

}
