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

/**
 * A queue that stores booleans.
 * 
 * @author Charles Xie
 */

public class BooleanQueue extends DataQueue {

	private boolean[] data;

	public BooleanQueue() {
		super();
		data = new boolean[DEFAULT_SIZE];
	}

	public BooleanQueue(int m) {
		super();
		setLength(m);
	}

	public BooleanQueue(String s, int m) {
		this(m);
		setName(s);
	}

	/** no effect */
	public void setMultiplier(float multiplier) {
	}

	/** no effect */
	public void setAddend(float addend) {
	}

	public void clear() {
		setPointer(0);
		if (data == null)
			return;
		Arrays.fill(data, false);
	}

	public void clearAfter(int n) {
		if (data == null)
			return;
		Arrays.fill(data, n, data.length, false);
	}

	public void clearBefore(int n) {
		if (data == null)
			return;
		Arrays.fill(data, 0, n, false);
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
				boolean[] oldData = data;
				data = new boolean[m];
				if (m < pointer)
					setPointer(m);
				System.arraycopy(oldData, 0, data, 0, pointer);
				// for(int i=0; i<pointer; i++) data[i]=oldData[i];
			}
			else {
				data = new boolean[m];
			}
		}
	}

	public int getLength() {
		return data == null ? 0 : data.length;
	}

	public void setData(Object o) throws IllegalArgumentException {
		if (!(o instanceof boolean[]))
			throw new IllegalArgumentException("You must input a boolean array");
		data = (boolean[]) o;
		setPointer(data.length);
	}

	public Object getData() {
		return data;
	}

	public boolean getData(int i) {
		if (data == null)
			throw new NullPointerException("in BooleanQueue.getData(int)");
		if (i >= data.length || i < 0)
			throw new ArrayIndexOutOfBoundsException("Index out of queue's bounds: " + i + ">" + data.length);
		return data[i];
	}

	public boolean getCurrentValue() {
		if (data == null)
			throw new NullPointerException("in BooleanQueue.getCurrentValue()");
		if (pointer <= 0)
			throw new RuntimeException("no data in queue");
		return data[pointer - 1];
	}

	public void update(boolean b) {
		if (pointer < data.length) {
			data[pointer++] = b;
		}
		else {
			for (int i = 0; i < pointer - 1; i++)
				data[i] = data[i + 1];
			data[pointer - 1] = b;
		}
	}

	public void move(int n) {
		int len = getLength();
		if (n >= len)
			throw new IllegalArgumentException("This queue cannot be moved that far");
		for (int i = n; i < len; i++)
			data[i - n] = data[i];
		Arrays.fill(data, len - n, len, false);
	}

	public void copyFrom(DataQueue q) {
		if (!(q instanceof BooleanQueue))
			throw new IllegalArgumentException("Elements of the input queue must be booleans");
		boolean[] array = ((BooleanQueue) q).data;
		if (data == null || array.length != data.length)
			data = new boolean[array.length];
		System.arraycopy(array, 0, data, 0, array.length);
		setPointer(q.getPointer());
	}

}
