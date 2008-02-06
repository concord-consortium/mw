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

package org.concord.modeler.g2d;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.List;

import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.MismatchException;

/**
 * <p>
 * This class defines a data model for a curve. A curve should contains an array or arrays of data, and the
 * corresponding information about how it is to be rendered (its flavor).
 * </p>
 * 
 * <p>
 * As a matter of fact, this class is used as an adapter to Leigh Brookshaw's <tt>DataSet</tt> class, which is not and
 * cannot be made serializable. In any circumstance you want to save the data in your graph together with all the
 * graphical options (e.g. legends, styles, etc.), you should consider serialize this object and/or its container class
 * <tt>CurveGroup</tt>.
 * </p>
 * 
 * @author Charles Xie
 */

public class Curve implements Serializable {

	public final static byte INSTANTANEOUS_VALUE = 0;
	public final static byte SIMPLE_RUNNING_AVERAGE = 1;
	public final static byte EXPONENTIAL_RUNNING_AVERAGE = 2;

	final static float DEFAULT_SMOOTHING_FACTOR = 0.05f;
	final static int DEFAULT_POINTS = 10;

	private byte smoothFilter = INSTANTANEOUS_VALUE;
	private Legend legend;
	private CurveFlavor flavor;
	private double[] data;
	private transient DataSet dataSet;

	/* Make the transient properties BML-transient: */
	static {
		try {
			BeanInfo info = Introspector.getBeanInfo(Curve.class);
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors) {
				String name = pd.getName();
				if (name.equals("dataSet")) {
					pd.setValue("transient", Boolean.TRUE);
				}
			}
		}
		catch (IntrospectionException e) {
		}
	}

	/** construct a default curve, which is a point at the origin */
	public Curve() {
		legend = new Legend();
		flavor = new CurveFlavor();
		data = new double[] { 0, 0 };
	}

	/** construct a curve which uses the data in xQ as x and those in yQ as y */
	public Curve(DataQueue xQ, DataQueue yQ, CurveFlavor flavor, Legend legend) {
		this();
		if (flavor != null) {
			this.flavor = flavor;
		}
		if (legend != null) {
			this.legend = legend;
		}
		setCurve(xQ, yQ);
	}

	Curve(List list) {
		int length = list.size();
		data = new double[length + length];
		DataPoint p;
		for (int i = 0; i < length; i++) {
			p = (DataPoint) list.get(i);
			data[i + i] = p.getX();
			data[i + i + 1] = p.getY();
		}
	}

	private Curve(double[] dat) {
		if (dat == null)
			throw new IllegalArgumentException("Cannot draw a null data array");
		legend = new Legend();
		flavor = new CurveFlavor();
		data = new double[dat.length];
		System.arraycopy(dat, 0, data, 0, dat.length);
	}

	public void setSmoothFilter(byte i) {
		smoothFilter = i;
	}

	public byte getSmoothFilter() {
		return smoothFilter;
	}

	/** use the data in xQ as x and those in yQ as y */
	public void setCurve(DataQueue xQ, DataQueue yQ) {
		if (xQ instanceof FloatQueue && yQ instanceof FloatQueue) {
			setCurve((FloatQueue) xQ, (FloatQueue) yQ);
		}
		else {
			throw new IllegalArgumentException("Don't know how to assemble a graph from two object queues");
		}
	}

	private void setCurve(FloatQueue xQ, FloatQueue yQ) {
		if (DataQueueUtilities.mismatch(xQ, yQ))
			throw new MismatchException("Input queues mismatch: " + xQ + ":" + yQ + xQ.getLength() + ":"
					+ yQ.getLength() + "," + xQ.getPointer() + ":" + yQ.getPointer() + "," + xQ.getInterval() + ":"
					+ yQ.getInterval());
		int length = xQ.getPointer();
		if (length <= 0)
			return;
		if (data == null || data.length != length + length)
			data = new double[length + length];
		boolean bx = xQ.getMultiplier() == 1.0f;
		boolean by = yQ.getMultiplier() == 1.0f;
		float tmp = 0;
		for (int i = 0; i < length; i++) {
			switch (smoothFilter) {
			case INSTANTANEOUS_VALUE:
				tmp = yQ.getData(i);
				break;
			case SIMPLE_RUNNING_AVERAGE:
				if (i == 0) {
					tmp = yQ.getData(0);
				}
				else {
					int k = Math.min(i, DEFAULT_POINTS);
					tmp = 0;
					for (int p = i - k; p < i; p++)
						tmp += yQ.getData(p);
					tmp /= k;
				}
				break;
			case EXPONENTIAL_RUNNING_AVERAGE:
				if (i == 0) {
					tmp = yQ.getData(0);
				}
				else {
					tmp = DEFAULT_SMOOTHING_FACTOR * yQ.getData(i) + (1 - DEFAULT_SMOOTHING_FACTOR) * tmp;
				}
				break;
			}
			data[i + i] = bx ? xQ.getData(i) + xQ.getAddend() : xQ.getData(i) * xQ.getMultiplier() + xQ.getAddend();
			data[i + i + 1] = by ? tmp + yQ.getAddend() : tmp * yQ.getMultiplier() + yQ.getAddend();
		}
	}

	/** get first-order derivative */
	public Curve getDerivative() {
		double[] x = new double[data.length - 2];
		double dx = 0.0, dy = 0.0;
		for (int i = 0; i < data.length - 2; i += 2) {
			dx = data[i + 2] - data[i];
			dy = data[i + 3] - data[i + 1];
			x[i] = data[i] + dx * 0.5;
			x[i + 1] = dy / dx;
		}
		return new Curve(x);
	}

	/** get running average based on cumulative sum */
	public Curve getCumulativeRunningAverage() {
		double[] x = new double[data.length];
		double a = 0.0;
		for (int i = 0; i < data.length; i += 2) {
			x[i] = data[i];
			a += data[i + 1];
			x[i + 1] = a / (i + 2) * 2;
		}
		return new Curve(x);
	}

	/** get exponential running average with the specified smoothing factor */
	public Curve getExponentialRunningAverage(double a) {
		double[] x = new double[data.length / 2];
		double[] y = new double[data.length / 2];
		for (int i = 0; i < x.length; i++) {
			x[i] = data[i + i + 1];
		}
		y[0] = x[0];
		for (int i = 1; i < y.length; i++) {
			y[i] = y[i - 1] * (1 - a) + x[i] * a;
		}
		x = new double[data.length];
		for (int i = 0; i < y.length; i++) {
			x[i + i] = data[i + i];
			x[i + i + 1] = y[i];
		}
		return new Curve(x);
	}

	public DataPoint getAveragePoint() {
		double a = 0.0, b = 0.0;
		int n = data.length;
		for (int i = 0; i < n - 1; i += 2) {
			a += data[i];
			b += data[i + 1];
		}
		return new DataPoint(2 * a / n, 2 * b / n);
	}

	public void setLegend(Legend l) {
		legend = l;
	}

	public Legend getLegend() {
		return legend;
	}

	public void setFlavor(CurveFlavor f) {
		flavor = f;
	}

	public CurveFlavor getFlavor() {
		return flavor;
	}

	public void setData(short[] v) {
		if (v == null)
			return;
		if (data == null || data.length != v.length)
			data = new double[v.length];
		for (int i = 0, n = data.length; i < n; i++)
			data[i] = v[i];
	}

	public void setData(int[] v) {
		if (v == null)
			return;
		if (data == null || data.length != v.length)
			data = new double[v.length];
		for (int i = 0, n = data.length; i < n; i++)
			data[i] = v[i];
	}

	public void setData(long[] v) {
		if (v == null)
			return;
		if (data == null || data.length != v.length)
			data = new double[v.length];
		for (int i = 0, n = data.length; i < n; i++)
			data[i] = v[i];
	}

	public void setData(float[] v) {
		if (v == null)
			return;
		if (data == null || data.length != v.length)
			data = new double[v.length];
		for (int i = 0, n = data.length; i < n; i++)
			data[i] = v[i];
	}

	public void setData(double[] v) {
		if (v == null)
			return;
		if (data == null || data.length != v.length)
			data = new double[v.length];
		System.arraycopy(v, 0, data, 0, data.length);
	}

	double[] getData() {
		return data;
	}

	/** @return the data set reference this curve holds */
	public DataSet getDataSet() {
		return dataSet;
	}

	/** set the data set reference to be held by this curve */
	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

}
