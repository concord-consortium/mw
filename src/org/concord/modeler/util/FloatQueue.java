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

package org.concord.modeler.util;

import java.util.Arrays;

public class FloatQueue extends DataQueue {

	private float[] data;
	private float multiplier = 1.0f;
	private float addend;

	public FloatQueue() {
		super();
		data = new float[DEFAULT_SIZE];
	}

	public FloatQueue(int m) {
		super();
		setLength(m);
	}

	public FloatQueue(String s, int m) {
		this(m);
		setName(s);
	}

	public FloatQueue(FloatQueue q) {
		super();
		copyFrom(q);
	}

	public void setMultiplier(float multiplier) {
		this.multiplier = multiplier;
	}

	public float getMultiplier() {
		return multiplier;
	}

	public void setAddend(float addend) {
		this.addend = addend;
	}

	public float getAddend() {
		return addend;
	}

	public void clear() {
		setPointer(0);
		if (data == null)
			return;
		Arrays.fill(data, 0.0f);
	}

	public void clearAfter(int n) {
		if (data == null)
			return;
		Arrays.fill(data, n, data.length, 0.0f);
	}

	public void clearBefore(int n) {
		if (data == null)
			return;
		Arrays.fill(data, 0, n, 0.0f);
	}

	/** sum the data between start and end, inclusive. */
	public float sum(int start, int end) {
		if (end < start)
			throw new IllegalArgumentException("end cannot be smaller than start");
		float s = 0.0f;
		for (int i = start; i <= end; i++)
			s += data[i];
		return s;
	}

	public boolean isEmpty() {
		return data == null || data.length == 0;
	}

	public void setLength(int m) {
		if (m <= 0) {
			data = null;
			setPointer(0);
		}
		else {
			if (pointer > 0) {
				float[] oldData = data;
				data = new float[m];
				if (m < pointer)
					setPointer(m);
				System.arraycopy(oldData, 0, data, 0, pointer);
				// for(int i=0; i<pointer; i++) data[i]=oldData[i];
			}
			else {
				data = new float[m];
			}
		}
	}

	public int getLength() {
		return data == null ? 0 : data.length;
	}

	public void setData(Object o) throws IllegalArgumentException {
		if (o instanceof float[]) {
			data = (float[]) o;
			setPointer(data.length);
		}
		else if (o instanceof int[]) {
			int[] temp = (int[]) o;
			if (temp.length > data.length)
				throw new ArrayIndexOutOfBoundsException("Input data out of queue bounds");
			System.arraycopy(temp, 0, data, 0, temp.length);
			setPointer(temp.length);
		}
		else if (o instanceof double[]) {
			double[] temp = (double[]) o;
			if (temp.length > data.length)
				throw new ArrayIndexOutOfBoundsException("Input data out of queue bounds");
			System.arraycopy(temp, 0, data, 0, temp.length);
			setPointer(temp.length);
		}
		else if (o instanceof long[]) {
			long[] temp = (long[]) o;
			if (temp.length > data.length)
				throw new ArrayIndexOutOfBoundsException("Input data out of queue bounds");
			System.arraycopy(temp, 0, data, 0, temp.length);
			setPointer(temp.length);
		}
		else if (o instanceof DataQueue) {
			setData(((DataQueue) o).getData());
			setPointer(((DataQueue) o).getPointer());
		}
		else {
			throw new IllegalArgumentException("You must input an array");
		}
	}

	public Object getData() {
		return data;
	}

	public float getData(int i) {
		return data[i];
	}

	public void setData(int i, float f) {
		data[i] = f;
	}

	/** if there is no data in this queue, return 0 */
	public float getCurrentValue() {
		if (data == null)
			throw new NullPointerException("in FloatQueue.getCurrentValue()");
		if (pointer <= 0)
			return 0;
		return data[pointer - 1];
	}

	public float getMinValue() {
		if (pointer <= 0)
			throw new RuntimeException("no data in queue");
		int n = pointer < data.length ? pointer : data.length;
		float min = java.lang.Float.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			if (data[i] < min)
				min = data[i];
		}
		return min;
	}

	public float getMaxValue() {
		if (pointer <= 0)
			throw new RuntimeException("no data in queue");
		int n = pointer < data.length - 1 ? pointer : data.length;
		float max = -java.lang.Float.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			if (data[i] > max)
				max = data[i];
		}
		return max;
	}

	/**
	 * @param n
	 *            how many frames will be used to do the average
	 * @return the average over the last n frames. If there is no data in this queue, return 0.
	 */
	public float getAverage(int n) {
		if (n <= 1)
			throw new RuntimeException("n must be greater than 1");
		if (pointer <= 0)
			return 0;
		int m = Math.min(pointer, n);
		float av = 0.0f;
		for (int i = 0; i < m; i++)
			av += data[i];
		return av / m;
	}

	/** @return the average over all the stored frames */
	public float getAverage() {
		return getAverage(getLength());
	}

	public float getExponentialRunningAverage(float weight) {
		return getExponentialRunningAverage(weight, pointer);
	}

	/**
	 * RA = weight * currentValue + (1 - weight) * RA See: http://en.wikipedia.org/wiki/Weighted_moving_average
	 * 
	 * @param weight
	 *            (typically a small number such as 0.05)
	 * @return the exponential running average
	 */
	public float getExponentialRunningAverage(float weight, int frame) {
		int m = Math.min(frame, getLength());
		if (m <= 0)
			return 0;
		if (m == 1)
			return data[0];
		float ra = data[0];
		for (int i = 1; i < m; i++)
			ra = weight * data[i] + (1 - weight) * ra;
		return ra;
	}

	public float getSimpleRunningAverage(int n) {
		return getSimpleRunningAverage(n, pointer);
	}

	/**
	 * calculate the n-point simple moving average. See http://en.wikipedia.org/wiki/Weighted_moving_average
	 * 
	 * @param n
	 *            the latest n points to average. must be greater than 1.
	 * @return the n-moving average
	 */
	public float getSimpleRunningAverage(int n, int frame) {
		if (n < 2)
			throw new IllegalArgumentException("n must be greater than 1.");
		int m = Math.min(frame, getLength());
		if (m <= 0)
			return 0;
		if (m == 1)
			return data[0];
		float ra = 0;
		int k = Math.max(0, m - n);
		for (int i = m - 1; i > k; i--)
			ra += data[i];
		return ra / (m - k);
	}

	/** @return the mean square */
	public float getMeanSquare() {
		int m = Math.min(pointer, getLength());
		if (m <= 0)
			return 0;
		float x = 0.0f;
		for (int i = 0; i < m; i++)
			x += data[i] * data[i];
		return x / m;
	}

	/** @return the root-mean-square deviation */
	public float getRMSDeviation() {
		int m = Math.min(pointer, getLength());
		if (m <= 0)
			return 0;
		float ave = getAverage();
		float x = 0.0f;
		for (int i = 0; i < m; i++)
			x += data[i] * data[i];
		return (float) Math.sqrt(x / m - ave * ave);
	}

	public void update(float f) {
		if (pointer < data.length) {
			data[pointer++] = f;
		}
		else {
			for (int i = 0; i < pointer - 1; i++)
				data[i] = data[i + 1];
			data[pointer - 1] = f;
		}
	}

	public void move(int n) {
		int len = getLength();
		if (n >= len)
			throw new IllegalArgumentException("This queue cannot be moved that far");
		for (int i = n; i < len; i++)
			data[i - n] = data[i];
		Arrays.fill(data, len - n, len, 0.0f);
	}

	public void copyFrom(DataQueue q) {
		if (!(q instanceof FloatQueue))
			throw new IllegalArgumentException("Elements of the input queue must be floats");
		float[] array = ((FloatQueue) q).data;
		if (data == null || array.length != data.length)
			data = new float[array.length];
		System.arraycopy(array, 0, data, 0, array.length);
		setPointer(q.getPointer());
	}

}