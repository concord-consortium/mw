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

import java.io.Serializable;

/**
 * <p>
 * Queue, together with time series, are the two mechanisms to store time-dependent data produced in a simulation.
 * Unlike a time series which stores time of measurement in even positions and data in odd positions, a queue does not
 * store time of measurement. A queue just stores the measured data. Queues are updated according to the
 * first-in-first-out (FIFO) policy.
 * </p>
 * 
 * <p>
 * Queues are used in making a movie. Time series are more general, and can be used to store data measured at arbitrary
 * time.
 * </p>
 * 
 * @author Charles Xie
 */

public abstract class DataQueue extends Object implements Serializable, Comparable {

	/** default size of a queue */
	public final static short DEFAULT_SIZE = 200;

	/** name of this queue */
	protected String name = "Unknown";

	/** reference upper bound for rendering this queue in a graph, if applicable. */
	protected double referenceUpperBound = 1;

	/** reference lower bound for rendering this queue in a graph, if applicable. */
	protected double referenceLowerBound = -1;

	/**
	 * When an array is initialized and its elements subsequently filled, it occurs that until the array is full, some
	 * of the elements are empty, the pointer points to the begin index of unfilled segment (x position, not t position,
	 * thus pointer must hold even value). The pointer will stop at the last index of the array once the whole array is
	 * filled up. Pointer is only important when the queue is not full.
	 */
	protected int pointer;

	/** the number of integration steps elapsed between two adjacent measurements of this queue */
	protected int interval = 100;

	/**
	 * a queue that coordinates with this queue. For example, a time queue and this queue can form a time series.
	 */
	protected transient DataQueue coordinateQ;

	private int functionalSlot;

	/**
	 * set the functional slot of this queue when it is involved in generating a graph. Functional slot is explained as
	 * follows: If queue X's functional slot is 0, queue Y1's functional slot is 1, then Y1(X) constitutes a function
	 * that can be plugged into a graph (as the first data set). Multiple functions can be plugged into a graph. For
	 * example, if there is another queue Y2 whose slot is 2, then Y2(X) is the second data set of the graph.
	 * 
	 * @param i
	 *            natural number
	 * @see org.concord.modeler.PageXYGraph
	 */
	public void setFunctionalSlot(int i) {
		functionalSlot = i;
	}

	/** which slot of a graph does this queue plug into? */
	public int getFunctionalSlot() {
		return functionalSlot;
	}

	/**
	 * set all the elements of the array associated with this queue to be zero, and move the pointer back to position 0
	 */
	public abstract void clear();

	/** set all the elements from index n on to the end to its defaults (e.g. 0.0f, 0.0d, or null) */
	public abstract void clearAfter(int n);

	/** set all the elements from index 0 on n-1 to its defaults (e.g. 0.0f, 0.0d, or null) */
	public abstract void clearBefore(int n);

	/** set the name of this queue */
	public void setName(String s) {
		name = s;
	}

	/** get the name of this queue */
	public String getName() {
		return name;
	}

	public void setPointer(int i) {
		pointer = i;
	}

	public int getPointer() {
		return pointer;
	}

	public void setInterval(int i) {
		interval = i;
	}

	public int getInterval() {
		return interval;
	}

	public void setReferenceUpperBound(double d) {
		referenceUpperBound = d;
	}

	public double getReferenceUpperBound() {
		return referenceUpperBound;
	}

	public void setReferenceLowerBound(double d) {
		referenceLowerBound = d;
	}

	public double getReferenceLowerBound() {
		return referenceLowerBound;
	}

	/** set the data array of this queue */
	public abstract void setData(Object o);

	/** get the data array of this queue */
	public abstract Object getData();

	/**
	 * copy the data array of the passed queue to that of this one. Note: This method is different from
	 * <code>setData(Object)</code> in that the latter just maintains two references to the same array, but the former
	 * maintains two arrays.
	 */
	public abstract void copyFrom(DataQueue q);

	/** return the length of this queue */
	public abstract int getLength();

	/** set the length of this queue */
	public abstract void setLength(int i);

	/**
	 * return true if no data has been fed to this queue, or the pointer has been moved to position 0, which indicates
	 * that it is ready to overwrite the whole queue.
	 */
	public abstract boolean isEmpty();

	/**
	 * move this queue forward for n positions without updates. The result is that the queue will end up having only
	 * length()-n element after moving.
	 */
	public abstract void move(int n);

	public void setCoordinateQueue(DataQueue q) {
		coordinateQ = q;
	}

	public DataQueue getCoordinateQueue() {
		return coordinateQ;
	}

	public String toString() {
		return name;
	}

	public boolean equals(Object o) {
		if (!(o instanceof DataQueue))
			return false;
		return toString().equals(o.toString());
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public int compareTo(Object o) {
		return toString().compareTo(o.toString());
	}

	public abstract void setMultiplier(float multiplier);

	public abstract void setAddend(float addend);

}