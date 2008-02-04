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

public class ByteQueue extends DataQueue {

	private byte[] data;
	private float multiplier = 1;
	private float addend;

	public ByteQueue() {
		super();
		data = new byte[DEFAULT_SIZE];
	}

	public ByteQueue(int m) {
		super();
		setLength(m);
	}

	public ByteQueue(String s, int m) {
		this(m);
		setName(s);
	}

	public ByteQueue(ByteQueue q) {
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
		Arrays.fill(data, (byte) 0);
	}

	public void clearAfter(int n) {
		if (data == null)
			return;
		Arrays.fill(data, n, data.length, (byte) 0);
	}

	public void clearBefore(int n) {
		if (data == null)
			return;
		Arrays.fill(data, 0, n, (byte) 0);
	}

	/** sum the data between start and end, inclusive. */
	public int sum(int start, int end) {
		if (end < start)
			throw new IllegalArgumentException("end cannot be smaller than start");
		int s = 0;
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
				byte[] oldData = data;
				data = new byte[m];
				if (m < pointer)
					setPointer(m);
				System.arraycopy(oldData, 0, data, 0, pointer);
			}
			else {
				data = new byte[m];
			}
		}
	}

	public int getLength() {
		return data == null ? 0 : data.length;
	}

	public void setData(Object o) throws IllegalArgumentException {
		if (o instanceof byte[]) {
			byte[] temp = (byte[]) o;
			if (temp.length > data.length)
				throw new ArrayIndexOutOfBoundsException("Input data out of queue bounds");
			System.arraycopy(temp, 0, data, 0, temp.length);
			setPointer(temp.length);
		}
		else {
			throw new IllegalArgumentException("You must input a byte array");
		}
	}

	public Object getData() {
		return data;
	}

	public byte getData(int i) {
		return data[i];
	}

	public void setData(int i, byte x) {
		data[i] = x;
	}

	/** if there is no data in this queue, return 0 */
	public byte getCurrentValue() {
		if (data == null)
			throw new NullPointerException("in ByteQueue.getCurrentValue()");
		if (pointer <= 0)
			return 0;
		return data[pointer - 1];
	}

	public byte getMinValue() {
		if (pointer <= 0)
			throw new RuntimeException("no data in queue");
		int n = pointer < data.length ? pointer : data.length;
		byte min = Byte.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			if (data[i] < min)
				min = data[i];
		}
		return min;
	}

	public byte getMaxValue() {
		if (pointer <= 0)
			throw new RuntimeException("no data in queue");
		int n = pointer < data.length - 1 ? pointer : data.length;
		byte max = -Byte.MAX_VALUE;
		for (int i = 0; i < n; i++) {
			if (data[i] > max)
				max = data[i];
		}
		return max;
	}

	/**
	 * @param n
	 *            how many frames will be used to do the average *
	 * @return the average over the last n frames. If there is no data in this queue, return 0.
	 */
	public float getAverage(int n) {
		if (n <= 1)
			throw new RuntimeException("n must be greater than 1");
		if (pointer <= 0)
			return 0;
		int m = Math.min(pointer, n);
		int av = 0;
		for (int i = 0; i < m; i++)
			av += data[i];
		return (float) av / (float) m;
	}

	/** @return the average over all the stored frames */
	public float getAverage() {
		return getAverage(getLength());
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

	public void update(byte x) {
		if (pointer < data.length) {
			data[pointer++] = x;
		}
		else {
			for (int i = 0; i < pointer - 1; i++)
				data[i] = data[i + 1];
			data[pointer - 1] = x;
		}
	}

	public void move(int n) {
		int len = getLength();
		if (n >= len)
			throw new IllegalArgumentException("This queue cannot be moved that far");
		for (int i = n; i < len; i++)
			data[i - n] = data[i];
		Arrays.fill(data, len - n, len, (byte) 0);
	}

	public void copyFrom(DataQueue q) {
		if (!(q instanceof ByteQueue))
			throw new IllegalArgumentException("Elements of the input queue must be floats");
		byte[] array = ((ByteQueue) q).data;
		if (data == null || array.length != data.length)
			data = new byte[array.length];
		System.arraycopy(array, 0, data, 0, array.length);
		setPointer(q.getPointer());
	}

}