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

package org.concord.functiongraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.concord.modeler.util.Evaluator;

/**
 * This object encapsulates the data, its appearance, and other properties
 * 
 * @author Connie J. Chen
 */

public final class DataSource {

	private final static Evaluator evaluator = new Evaluator();
	private final static DecimalFormat format = new DecimalFormat("###.##");
	private float[] data;
	private int pointer;
	private BasicStroke stroke;
	private Color color;
	private Color handleColor = Color.black;
	private Ellipse2D hotSpot, oldHotSpot;
	private boolean selected;
	private boolean hideHotSpot;
	private Color highlightColor;
	private String name = "Unknown";
	private String principalVariable = "x";
	private String expression;
	private String[] tableColumnNames;
	private Map<String, Float> parameters;
	private float xSelected, ySelected;
	private boolean showSelectedPoint;
	private int preferredPointNumber = 100;

	static {
		evaluator.setImplicitMultiplication(true);
	}

	// table model to display data in numeric format
	private final DefaultTableModel tableModel = new DefaultTableModel() {
		public boolean isCellEditable(int row, int col) {
			return false;
		}
	};

	public DataSource() {
		data = new float[preferredPointNumber * 2];
		setDefaultProperties();
	}

	public DataSource(int len) {
		if (len <= 0)
			throw new IllegalArgumentException("Argument must be positive integer");
		if (len % 2 == 0) {
			data = new float[len];
		}
		else {
			throw new IllegalArgumentException("The argument must be even number");
		}
		preferredPointNumber = len / 2;
		setDefaultProperties();
	}

	private void setDefaultProperties() {
		stroke = new BasicStroke(3.0f);
		color = new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));

	}

	public void setPreferredPointNumber(int n) {
		preferredPointNumber = n;
	}

	public int getPreferredPointNumber() {
		return preferredPointNumber;
	}

	/*
	 * test if a data point (x, y) is proximate to this data source according to the criterion that the closest distance
	 * between the point and the data source is less than the specified value
	 */
	Point2D isProximate(float x, float y, float distance) {
		if (getExpression() == null) { // if no math expression, use the data array directly
			int n = data.length / 2;
			float dx, dy;
			for (int i = 0; i < n; i++) {
				dx = data[2 * i] - x;
				dy = data[2 * i + 1] - y;
				if (dx * dx + dy * dy < distance * distance)
					return new Point2D.Float(data[2 * i], data[2 * i + 1]);
			}
		}
		else {
			float dx = (float) (getHotSpot().getX() - getOldHotSpot().getX());
			float dy = (float) (getHotSpot().getY() - getOldHotSpot().getY());
			float x1 = x - dx;
			float y1 = -evaluate(x1) - dy;
			if (Math.abs(y - y1) < distance)
				return new Point2D.Float(x1, y1);
		}
		return null;
	}

	/**
	 * return the functions supported by the math expression parser associated with the data source class
	 */
	public static Hashtable getSupportedFunctions() {
		return evaluator.getFunctionTable();
	}

	public void setShowSelectedPoint(boolean b) {
		showSelectedPoint = b;
	}

	public boolean isSelectedPointShown() {
		return showSelectedPoint;
	}

	/** set the selected point of this data source. */
	public void setSelectedPoint(float x, float y) {
		xSelected = x;
		ySelected = y;
	}

	public void setSelectedX(float x) {
		xSelected = x;
	}

	public void setSelectedY(float y) {
		ySelected = y;
	}

	public float getSelectedX() {
		return xSelected;
	}

	public float getSelectedY() {
		return ySelected;
	}

	/** return the table model associated with this data source */
	public TableModel getTableModel() {
		return tableModel;
	}

	/**
	 * set the names of the columns when this data source is displayed in the form of a table. This can be used to set
	 * the names of the data columns when the data are provided in raw discrete form, i.e. not generated by a math
	 * expression.
	 */
	public void setTableColumnNames(String[] names) {
		tableColumnNames = names;
	}

	/**
	 * get the names of the columns when this data source is displayed in the form of a table
	 */
	public String[] getTableColumnNames() {
		return tableColumnNames;
	}

	/*
	 * clear and re-populate the cells of the data table after the data array has been changed
	 */
	void populateTable() {

		// if there are any data, remove them from the table first
		while (tableModel.getRowCount() > 0) {
			tableModel.removeRow(0);
		}
		tableModel.setColumnCount(0);

		int numberOfRows = pointer / 2;
		Object t[] = new Object[numberOfRows];
		for (int i = 0; i < numberOfRows; i++) {
			t[i] = i + "";
		}
		tableModel.addColumn("#", t);
		for (int i = 0; i < numberOfRows; i++) {
			t[i] = format.format(data[i + i]);
		}
		tableModel.addColumn(tableColumnNames == null ? (getPrincipalVariable() == null ? "x" : getPrincipalVariable())
				: tableColumnNames[0], t);
		for (int i = 0; i < numberOfRows; i++) {
			t[i] = format.format(-data[i + i + 1]);
		}
		tableModel.addColumn(tableColumnNames != null && tableColumnNames.length > 1 ? tableColumnNames[1]
				: (getExpression() == null ? "y" : getExpression()), t);

	}

	/**
	 * set a math expression that fills in this data source with the data it is used to generate.
	 */
	public synchronized void setExpression(String s) {
		expression = s;
	}

	/** return the expression dictating this data source */
	public synchronized String getExpression() {
		return expression;
	}

	/**
	 * return the expression that substitutes parameters with their values (except x). For example, instead of returning
	 * a*x+b, where a=2 and b=1, returns 2*x+1
	 */
	public synchronized String getParameterlessExpression() {
		String s = new String(expression);
		Hashtable table = evaluator.getSymbolTable();
		String key, value;
		synchronized (table) {
			for (Iterator it = table.keySet().iterator(); it.hasNext();) {
				key = (String) it.next();
				if (!key.equals(getPrincipalVariable())) {
					value = table.get(key).toString();
					s = s.replaceAll(key, value);
				}
			}
		}
		return s;
	}

	public synchronized void setVariableValue(String name, float value) {
		if (parameters == null)
			parameters = Collections.synchronizedMap(new HashMap<String, Float>());
		parameters.put(name, value);
	}

	/** set the name of the principal variable. The default name is "x". */
	public void setPrincipalVariable(String s) {
		principalVariable = s;
	}

	/** get the name of the principal variable. */
	public String getPrincipalVariable() {
		return principalVariable;
	}

	/** compute the value with the input principal variable */
	public synchronized float evaluate(float x) {
		if (getExpression() == null)
			throw new RuntimeException("No expression has been set");
		setVariableValue(principalVariable, x);
		resetEvaluator();
		return (float) evaluator.eval();
	}

	/* feed the evaluator the variable-value pairs of this data source */
	private synchronized void resetEvaluator() {
		evaluator.removeAllVariables();
		synchronized (parameters) {
			if (parameters != null && !parameters.isEmpty()) {
				for (String name : parameters.keySet()) {
					evaluator.setVariableValue(name, parameters.get(name));
				}
			}
		}
		evaluator.setExpression(expression);
	}

	/**
	 * get the length of the data array, namely the length of the array. The capacity is always no smaller than the
	 * pointer.
	 */
	public int getCapacity() {
		return data.length;
	}

	/*
	 * return the pointer of the valid data zone. A valid data zone is the zone in which the data array is filled with
	 * valid data in the current data context.
	 */
	synchronized int getPointer() {
		return pointer;
	}

	/**
	 * create data array using the math expression associated with this data source
	 * 
	 * @param xmin
	 *            the lower bound of the principal variable
	 * @param xmax
	 *            the upper bound of the principal variable
	 */
	public synchronized void generateData(float xmin, float xmax) {
		if (getExpression() == null)
			return;
		reset();
		resetEvaluator();
		int n = getCapacity() / 2;
		float delta = (xmax - xmin) / (n - 1);
		double value;
		for (float x = xmin; x < xmax + delta * 0.01f; x += delta) {
			evaluator.setVariableValue(principalVariable, x);
			value = evaluator.eval();
			if (Double.doubleToLongBits(value) != Double.doubleToLongBits(Double.NaN)) {
				addValue(x, (float) value);
			}
		}
		// prevent unintensionally triggering the expansion of the data array
		if (getCapacity() > 2 * n)
			throw new RuntimeException("Data array should not be expanded. Old size=" + 2 * n + ", new size="
					+ getCapacity());
	}

	/**
	 * translate the whole valid zone of the data array by the specified increments
	 */
	public synchronized void translateData(float dx, float dy) {
		if (data == null || pointer <= 0)
			return;
		for (int i = 0; i < pointer - 2; i += 2) {
			data[i] += dx;
			data[i + 1] += dy;
		}
	}

	public void setHideHotSpot(boolean b) {
		hideHotSpot = b;
	}

	public boolean getHideHotSpot() {
		return hideHotSpot;
	}

	/** set a name (a string representation) of this data source */
	public void setName(String s) {
		name = s;
	}

	/** get the name of this data source */
	public String getName() {
		return name;
	}

	/** mark this data source as "selected" */
	public synchronized void setSelected(boolean b) {
		selected = b;
	}

	/** check if this data source is selected */
	public synchronized boolean isSelected() {
		return selected;
	}

	/** set the highlight color. Null if highlighting is not desired. */
	public void setHighlightColor(Color c) {
		highlightColor = c;
	}

	/** get the highlight color */
	public Color getHighlightColor() {
		return highlightColor;
	}

	public void setHandleColor(Color c) {
		handleColor = c;
	}

	public Color getHandleColor() {
		return handleColor;
	}

	/** set the principal color this data source bears */
	public void setColor(Color c) {
		color = c;
		setHandleColor(c);
	}

	/** get the principal color this data source bears */
	public Color getColor() {
		return color;
	}

	/** set the line stroke if this data source is represented by lines */
	public void setStroke(BasicStroke s) {
		stroke = s;
	}

	/** get the line stroke of this data source */
	public BasicStroke getStroke() {
		return stroke;
	}

	public void setLineWeight(float weight) {
		if (stroke == null) {
			stroke = new BasicStroke(weight);
		}
		else {
			stroke = new BasicStroke(weight, stroke.getEndCap(), stroke.getLineJoin(), stroke.getMiterLimit(), stroke
					.getDashArray(), stroke.getDashPhase());
		}
	}

	public float getLineWeight() {
		return stroke.getLineWidth();
	}

	/**
	 * set the hotspot of this data source. A hotspot is a handle with which the user will interact with the data
	 * source, which is graphically represented by an ellipse. It MUST be set if the designer desires interactivity. If
	 * a hotspot is not set, i.e. it is null, the dats source will not be manipulable.
	 */
	public synchronized void setHotSpot(Ellipse2D p) {
		if (p == null) {
			hotSpot = null;
		}
		else {
			if (hotSpot == null) {
				hotSpot = new Ellipse2D.Float((float) p.getX(), (float) p.getY(), (float) p.getWidth(), (float) p
						.getHeight());
				oldHotSpot = new Ellipse2D.Float((float) p.getX(), (float) p.getY(), (float) p.getWidth(), (float) p
						.getHeight());
			}
			else {
				hotSpot.setFrame(p.getX(), p.getY(), p.getWidth(), p.getHeight());
			}
		}
	}

	/** set the location and area of the hotspot */
	public synchronized void setHotSpot(double x, double y, double w, double h) {
		if (hotSpot == null) {
			hotSpot = new Ellipse2D.Float((float) x, (float) y, (float) w, (float) h);
			oldHotSpot = new Ellipse2D.Float((float) x, (float) y, (float) w, (float) h);
		}
		else {
			hotSpot.setFrame(x, y, w, h);
		}
	}

	/** get the hotspot of this data source */
	public synchronized Ellipse2D getHotSpot() {
		return hotSpot;
	}

	synchronized void changeHotSpot(double x, double y) {
		if (hotSpot != null)
			hotSpot.setFrame(x, y, hotSpot.getWidth(), hotSpot.getHeight());
		if (oldHotSpot != null)
			oldHotSpot.setFrame(x, y, oldHotSpot.getWidth(), oldHotSpot.getHeight());
	}

	synchronized Ellipse2D getOldHotSpot() {
		return oldHotSpot;
	}

	/**
	 * add a pair of (x,y) values to this data source. If the data array cannot accommodate more data points, expand it.
	 * A data source MUST contain the x-coordinates. The x and y coordinates are arranged in the data array as
	 * x,y,x,y,x,y,......, namely, the x's are positioned at even-number locations, and the y's are at odd-number
	 * locations. It is not required that x or y should increase or decrease monotonically along the array. If they do
	 * change monotonically, it is not required that the coordinates increase or decrease with the same amount of change
	 * at each step. Neither is it required that the data sources in a graph have the same set of x coordinates. That is
	 * to say, a data source can have x coordinates that are totally different from those of the others.
	 */
	public synchronized void addValue(float x, float y) {
		if (pointer == data.length) {
			float[] data1 = new float[2 * data.length];
			System.arraycopy(data, 0, data1, 0, data.length);
			data = data1;
		}
		data[pointer] = x;
		pointer++;
		data[pointer] = -y;
		pointer++;
	}

	/**
	 * reset this data source, so that the data array will be filled with new data. If this method is called, the
	 * hotspot will be deactivated. You MUST redefine the hotspot after resetting, if you want this data source to be
	 * manipulable.
	 */
	public synchronized void reset() {
		pointer = 0;
		// hotSpot=null;
	}

	/**
	 * set the data array. This method is primarily used to exchange data between data sources. You should avoid using
	 * it for different purposes.
	 */
	public synchronized void setData(float[] data) {
		this.data = data;
	}

	/* return the data array of this data source */
	synchronized float[] getDataArray() {
		return data;
	}

	/**
	 * return a completely new data array which contains exactly the valid data zone of the original data array. This
	 * method should NOT frequently called (such as in a painting procedure).
	 */
	public synchronized float[] getData() {
		if (pointer <= 0)
			return null;
		float[] curData = new float[pointer];
		System.arraycopy(data, 0, curData, 0, pointer);
		return curData;
	}

	/** return the first-order derivative of this data source */
	public synchronized float[] derivative() {
		if (pointer <= 0)
			return null;
		float[] deri = new float[pointer - 2];
		int ii = 0;
		for (int i = 0; i < pointer - 2; i += 2) {
			ii = i + i;
			deri[ii] = data[ii];
			deri[ii + 1] = (data[ii + 3] - data[ii + 1]) / (data[ii + 2] - data[ii]);
		}
		return deri;
	}

	/* return the first-order derivate at the i-th data point */
	synchronized float derivative(int i) {
		if (pointer <= 0)
			return Float.NaN;
		int ii = i + i;
		if (ii > pointer - 4)
			return Float.NaN;
		return (data[ii + 3] - data[ii + 1]) / (data[ii + 2] - data[ii]);
	}

	public String toString() {
		if (!name.equals("Unknown"))
			return name;
		if (getExpression() != null)
			return getExpression();
		return name;
	}

}