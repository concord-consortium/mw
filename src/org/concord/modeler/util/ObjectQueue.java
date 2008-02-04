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
 * A queue that stores objects.
 * 
 * @author Charles Xie
 */

public class ObjectQueue extends DataQueue {

	private Object[] data;

	public ObjectQueue() {
		super();
		data = new Object[DEFAULT_SIZE];
	}

	public ObjectQueue(int m) {
		super();
		setLength(m);
	}

	public ObjectQueue(String s, int m) {
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
		Arrays.fill(data, null);
	}

	public void clearBefore(int n) {
		if (data == null)
			return;
		Arrays.fill(data, 0, n, null);
	}

	public void clearAfter(int n) {
		if (data == null)
			return;
		Arrays.fill(data, n, data.length, null);
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
				Object[] oldData = data;
				data = new Object[m];
				if (m < pointer)
					setPointer(m);
				System.arraycopy(oldData, 0, data, 0, pointer);
				// for(int i=0; i<pointer; i++) data[i]=oldData[i];
			}
			else {
				data = new Object[m];
			}
		}
	}

	public int getLength() {
		return data == null ? 0 : data.length;
	}

	public void setData(Object o) throws IllegalArgumentException {
		if (o instanceof ObjectQueue) {
			setData(((ObjectQueue) o).getData());
			setPointer(((ObjectQueue) o).getPointer());
		}
		else if (o instanceof Object[]) {
			data = (Object[]) o;
			setPointer(data.length);
		}
		else {
			throw new IllegalArgumentException("You must input an array or object queue");
		}
	}

	public Object getData() {
		return data;
	}

	public Object getData(int i) {
		if (data == null)
			throw new NullPointerException("in Queue.getData(int)");
		if (i >= data.length || i < 0)
			throw new ArrayIndexOutOfBoundsException("Index out of queue's bounds: " + i + ">" + data.length);
		return data[i];
	}

	public Object getCurrentValue() {
		if (data == null)
			throw new NullPointerException("in Queue.getData(int)");
		if (pointer <= 0)
			throw new RuntimeException("no data in queue");
		return data[pointer - 1];
	}

	public void setPointer(int i) {
		super.setPointer(i);
	}

	public void update(Object o) {
		if (pointer < data.length) {
			data[pointer++] = o;
		}
		else {
			for (int i = 0; i < pointer - 1; i++)
				data[i] = data[i + 1];
			data[pointer - 1] = o;
		}
	}

	public void move(int n) {
		int len = getLength();
		if (n >= len)
			throw new IllegalArgumentException("This queue cannot be moved that far");
		for (int i = n; i < len; i++)
			data[i - n] = data[i];
		Arrays.fill(data, len - n, len, null);
	}

	public void copyFrom(DataQueue q) {
		if (!(q instanceof ObjectQueue))
			throw new IllegalArgumentException("Must input an object queue");
		Object[] array = ((ObjectQueue) q).data;
		if (array.length != data.length)
			data = new Object[array.length];
		System.arraycopy(array, 0, data, 0, data.length);
		setPointer(q.getPointer());
	}

}