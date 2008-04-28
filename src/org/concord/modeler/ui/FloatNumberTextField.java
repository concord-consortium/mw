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
 * This is a text field for receiving only single-precision floating point inputs. It does not accept non-digit inputs,
 * nor a value out of the specified bounds.
 * 
 * @author Charles Xie
 */

public class FloatNumberTextField extends PastableTextField {

	private NumberFormat formatter;
	private float max = 1.0f, min = -1.0f, prefer = 0.0f;

	public FloatNumberTextField(float min, float max) throws IllegalArgumentException {
		super();
		if (max < min)
			throw new IllegalArgumentException("min cannot be greater than max.");
		this.max = max;
		this.min = min;
		init();
	}

	public FloatNumberTextField(float value, float min, float max) throws IllegalArgumentException {
		super();
		if (max < min)
			throw new IllegalArgumentException("min cannot be greater than max.");
		this.max = max;
		this.min = min;
		prefer = value;
		init();
		setValue(value);
	}

	public FloatNumberTextField(float value, float min, float max, int columns) throws IllegalArgumentException {
		super(columns);
		if (max < min)
			throw new IllegalArgumentException("min cannot be greater than max.");
		this.max = max;
		this.min = min;
		prefer = value;
		init();
		setValue(value);
	}

	public void setMaximumFractionDigits(int i) {
		if (formatter == null)
			return;
		formatter.setMaximumFractionDigits(i);
	}

	public void setMinimumFractionDigits(int i) {
		if (formatter == null)
			return;
		formatter.setMinimumFractionDigits(i);
	}

	public void setMaxValue(float d) {
		max = d;
	}

	public float getMaxValue() {
		return max;
	}

	public void setMinValue(float d) {
		min = d;
	}

	public float getMinValue() {
		return min;
	}

	public void setPreferredValue(float d) {
		prefer = d;
	}

	public float getPreferredValue() {
		return prefer;
	}

	public float getValue() {
		float retVal = 0;
		try {
			retVal = formatter.parse(getText()).floatValue();
		}
		catch (ParseException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"The input does not contain any data, or\ncontains data that cannot be parsed.", "Parsing error",
					JOptionPane.ERROR_MESSAGE);
			setValue(min);
			return min;
		}
		if (retVal < min)
			return min;
		if (retVal > max)
			return max;
		return retVal;
	}

	public void setValue(float value) {
		if (value > max)
			value = max;
		else if (value < min)
			value = min;
		setText(formatter.format(value));
	}

	protected Document createDefaultModel() {

		return new PlainDocument() {

			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {

				int countDot = 0;
				char[] old = FloatNumberTextField.this.getText().toCharArray();

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
					setText(formatter.format(min));
				}
				else {
					float d = getValue();
					if (d > max)
						setText(formatter.format(max));
					else if (d < min)
						setText(formatter.format(min));
				}
			}
		});
	}

}
