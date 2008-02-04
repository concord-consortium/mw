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

import java.util.Iterator;

/**
 * This queue group stores only homogenous data, i.e. queues of the same properties (length, interval etc.). A queue
 * that has different properties will be rejected.
 * 
 * @author Charles Xie
 */

public class HomoQueueGroup extends QueueGroup {

	public HomoQueueGroup() {
		super();
	}

	public HomoQueueGroup(String s) {
		super(s);
	}

	public boolean add(Object o) throws NotQueueException {
		if (o == null)
			return false;
		if (o instanceof FloatQueueTwin)
			return add(((FloatQueueTwin) o).getQueue1()) && add(((FloatQueueTwin) o).getQueue2());
		if (o instanceof FloatQueueTriplet)
			return add(((FloatQueueTriplet) o).getQueue1()) && add(((FloatQueueTriplet) o).getQueue2())
					&& add(((FloatQueueTriplet) o).getQueue3());
		if (o instanceof DataQueue) {
			if (isEmpty())
				return super.add(o);
			DataQueue q0 = (DataQueue) get(0);
			DataQueue q = (DataQueue) o;
			if (q0.getInterval() == q.getInterval() && q0.getLength() == q.getLength())
				return super.add(o);
			return false;
		}
		throw new NotQueueException();
	}

	public void setInterval(int i) {
		DataQueue q = null;
		synchronized (getSynchronizedLock()) {
			for (Iterator it = iterator(); it.hasNext();) {
				q = (DataQueue) it.next();
				q.setInterval(i);
			}
		}
	}

	public int getInterval() {
		if (isEmpty())
			return -1;
		DataQueue q = (DataQueue) get(0);
		if (q != null)
			return q.getInterval();
		return -1;
	}

}