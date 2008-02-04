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
 * A queue twin is a special queue group to bundle two sibling properties such as <tt>Rx, Ry</tt> of a particle.
 * 
 * @author Charles Xie
 */

public class FloatQueueTwin extends Object implements Serializable {

	private FloatQueue q1, q2;

	public FloatQueueTwin() {
		q1 = new FloatQueue();
		q2 = new FloatQueue();
	}

	public FloatQueueTwin(int n) {
		q1 = new FloatQueue(n);
		q2 = new FloatQueue(n);
	}

	public FloatQueueTwin(FloatQueue t1, FloatQueue t2) {
		setQueue(t1, t2);
	}

	public void setQueue(FloatQueue t1, FloatQueue t2) throws MismatchException {
		if (t1 == null || t2 == null)
			throw new NullPointerException("the args cannot be null");
		if (t1.getLength() != t2.getLength())
			throw new MismatchException("queue 1 and 2 have different lengths!");
		if (t1.getPointer() != t2.getPointer())
			throw new MismatchException("queue 1 and 2 have different pointer indices!");
		if (t1.getInterval() != t2.getInterval())
			throw new MismatchException("queue 1 and 2 have different intervals!");
		q1 = t1;
		q2 = t2;
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
		q2 = t;
	}

	public FloatQueue getQueue1() {
		return q1;
	}

	public FloatQueue getQueue2() {
		return q2;
	}

	/** @see org.concord.modeler.util.DataQueue#coordinateQ */
	public void setCoordinateQueue(FloatQueue q) {
		q1.setCoordinateQueue(q);
		q2.setCoordinateQueue(q);
	}

	public boolean isEmpty() {
		if (q1 == null || q2 == null)
			return true;
		return q1.isEmpty() || q2.isEmpty();
	}

	public void setLength(int i) {
		q1.setLength(i);
		q2.setLength(i);
	}

	public int getLength() {
		if (q1 == null)
			return 0;
		return q1.getLength();
	}

	public void setInterval(int i) {
		q1.setInterval(i);
		q2.setInterval(i);
	}

	public int getInterval() {
		return q1.getInterval();
	}

	public void setPointer(int i) {
		q1.setPointer(i);
		q2.setPointer(i);
	}

	public int getPointer() {
		return q1.getPointer();
	}

	public void clear() {
		q1.clear();
		q2.clear();
	}

	public void clearBefore(int n) {
		q1.clearBefore(n);
		q2.clearBefore(n);
	}

	public void clearAfter(int n) {
		q1.clearAfter(n);
		q2.clearAfter(n);
	}

	public void copyFrom(FloatQueueTwin twin) {
		q1.copyFrom(twin.q1);
		q2.copyFrom(twin.q2);
	}

	public void update(float x, float y) {
		q1.update(x);
		q2.update(y);
	}

	public void update(double x, double y) {
		q1.update((float) x);
		q2.update((float) y);
	}

	public void move(int n) {
		q1.move(n);
		q2.move(n);
	}

}