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
 * A queue triplet is a special queue group to bundle three sibling properties such as <tt>Rx, Ry, Rz</tt> of a
 * particle.
 * 
 * @author Charles Xie
 */

public class FloatQueueTriplet extends Object implements Serializable {

	private FloatQueue q1, q2, q3;

	public FloatQueueTriplet() {
		q1 = new FloatQueue();
		q2 = new FloatQueue();
		q3 = new FloatQueue();
	}

	public FloatQueueTriplet(int n) {
		q1 = new FloatQueue(n);
		q2 = new FloatQueue(n);
		q3 = new FloatQueue(n);
	}

	public FloatQueueTriplet(FloatQueue t1, FloatQueue t2, FloatQueue t3) {
		setQueue(t1, t2, t3);
	}

	public void setQueue(FloatQueue t1, FloatQueue t2, FloatQueue t3) throws MismatchException {
		if (t1 == null || t2 == null || t3 == null)
			throw new NullPointerException("the args cannot be null");
		if (t1.getLength() != t2.getLength() || t2.getLength() != t3.getLength() || t3.getLength() != t1.getLength())
			throw new MismatchException("queue 1, 2, 3 have different lengths!");
		if (t1.getPointer() != t2.getPointer() || t2.getPointer() != t3.getPointer()
				|| t3.getPointer() != t1.getPointer())
			throw new MismatchException("queue 1, 2, 3 have different pointer indices!");
		if (t1.getInterval() != t2.getInterval() || t2.getInterval() != t3.getInterval()
				|| t3.getInterval() != t1.getInterval())
			throw new MismatchException("queue 1, 2, 3 have different intervals!");
		q1 = t1;
		q2 = t2;
		q3 = t3;
	}

	public void setQueue1(FloatQueue t) throws MismatchException {
		if (t == null)
			throw new NullPointerException("the arg cannot be null");
		if (q2 != null) {
			if (t.getLength() != q2.getLength())
				throw new MismatchException("queue 1 and 2 have different lengths!");
			if (t.getPointer() != q2.getPointer())
				throw new MismatchException("queue 1 and 2 have different pointer indices!");
			if (t.getInterval() != q2.getInterval())
				throw new MismatchException("queue 1 and 2 have different intervals!");
		}
		if (q3 != null) {
			if (t.getLength() != q3.getLength())
				throw new MismatchException("queue 1 and 3 have different lengths!");
			if (t.getPointer() != q3.getPointer())
				throw new MismatchException("queue 1 and 3 have different pointer indices!");
			if (t.getInterval() != q3.getInterval())
				throw new MismatchException("queue 1 and 3 have different intervals!");
		}
		q1 = t;
	}

	public void setQueue2(FloatQueue t) throws MismatchException {
		if (t == null)
			throw new NullPointerException("the arg cannot be null");
		if (q1 != null) {
			if (t.getLength() != q1.getLength())
				throw new MismatchException("queue 1 and 2 have different lengths!");
			if (t.getPointer() != q1.getPointer())
				throw new MismatchException("queue 1 and 2 have different pointer indices!");
			if (t.getInterval() != q1.getInterval())
				throw new MismatchException("queue 1 and 2 have different intervals!");
		}
		if (q3 != null) {
			if (t.getLength() != q3.getLength())
				throw new MismatchException("queue 2 and 3 have different lengths!");
			if (t.getPointer() != q3.getPointer())
				throw new MismatchException("queue 2 and 3 have different pointer indices!");
			if (t.getInterval() != q3.getInterval())
				throw new MismatchException("queue 2 and 3 have different intervals!");
		}
		q2 = t;
	}

	public void setQueue3(FloatQueue t) throws MismatchException {
		if (t == null)
			throw new NullPointerException("the arg cannot be null");
		if (q1 != null) {
			if (t.getLength() != q1.getLength())
				throw new MismatchException("queue 1 and 3 have different lengths!");
			if (t.getPointer() != q1.getPointer())
				throw new MismatchException("queue 1 and 3 have different pointer indices!");
			if (t.getInterval() != q1.getInterval())
				throw new MismatchException("queue 1 and 3 have different intervals!");
		}
		if (q2 != null) {
			if (t.getLength() != q2.getLength())
				throw new MismatchException("queue 2 and 3 have different lengths!");
			if (t.getPointer() != q2.getPointer())
				throw new MismatchException("queue 2 and 3 have different pointer indices!");
			if (t.getInterval() != q2.getInterval())
				throw new MismatchException("queue 2 and 3 have different intervals!");
		}
		q3 = t;
	}

	public FloatQueue getQueue1() {
		return q1;
	}

	public FloatQueue getQueue2() {
		return q2;
	}

	public FloatQueue getQueue3() {
		return q3;
	}

	/** @see org.concord.modeler.util.DataQueue#coordinateQ */
	public void setCoordinateQueue(FloatQueue q) {
		q1.setCoordinateQueue(q);
		q2.setCoordinateQueue(q);
		q3.setCoordinateQueue(q);
	}

	public boolean isEmpty() {
		if (q1 == null || q2 == null || q3 == null)
			return true;
		return q1.isEmpty() || q2.isEmpty() || q3.isEmpty();
	}

	public void setLength(int i) {
		q1.setLength(i);
		q2.setLength(i);
		q3.setLength(i);
	}

	public int getLength() {
		return q1.getLength();
	}

	public void setInterval(int i) {
		q1.setInterval(i);
		q2.setInterval(i);
		q3.setInterval(i);
	}

	public int getInterval() {
		return q1.getInterval();
	}

	public void setPointer(int i) {
		q1.setPointer(i);
		q2.setPointer(i);
		q3.setPointer(i);
	}

	public int getPointer() {
		return q1.getPointer();
	}

	public void clear() {
		q1.clear();
		q2.clear();
		q3.clear();
	}

	public void clearBefore(int n) {
		q1.clearBefore(n);
		q2.clearBefore(n);
		q3.clearBefore(n);
	}

	public void clearAfter(int n) {
		q1.clearAfter(n);
		q2.clearAfter(n);
		q3.clearAfter(n);
	}

	public void copyFrom(FloatQueueTriplet t) {
		q1.copyFrom(t.q1);
		q2.copyFrom(t.q2);
		q3.copyFrom(t.q3);
	}

	public void update(float x, float y, float z) {
		q1.update(x);
		q2.update(y);
		q3.update(z);
	}

	public void update(double x, double y, double z) {
		q1.update((float) x);
		q2.update((float) y);
		q3.update((float) z);
	}

	public void move(int n) {
		q1.move(n);
		q2.move(n);
		q3.move(n);
	}

}