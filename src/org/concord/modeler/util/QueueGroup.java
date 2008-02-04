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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This is for storing queues. It can be used to store heterogenous data, i.e. queues of different lengths and/or of
 * different primitive types, or homogenous data, i.e. queues of the same length and properties.
 * 
 * @author Charles Xie
 */

public class QueueGroup implements List {

	protected transient QueueGroupTable table;
	private String name = "Unknown";
	private List<DataQueue> list;

	public QueueGroup() {
		super();
		list = Collections.synchronizedList(new ArrayList<DataQueue>());
	}

	public QueueGroup(String name) {
		super();
		this.name = name;
		list = Collections.synchronizedList(new ArrayList<DataQueue>());
	}

	public QueueGroup(List list) {
		this();
		addAll(list);
	}

	public Object getSynchronizedLock() {
		return list;
	}

	public void setName(String s) {
		name = s;
	}

	public String getName() {
		return name;
	}

	public DataQueue getQueue(String s) {
		synchronized (list) {
			for (DataQueue q : list) {
				if (q.getName().equals(s))
					return q;
			}
		}
		return null;
	}

	public void add(int index, Object element) throws NotQueueException {
		if (element == null)
			return;
		if (!(element instanceof DataQueue))
			throw new NotQueueException();
		if (list.contains(element))
			return;
		list.add(index, (DataQueue) element);
	}

	public boolean add(Object o) throws NotQueueException {
		if (o == null)
			return false;
		if (list.contains(o))
			return false;
		if (o instanceof DataQueue)
			return list.add((DataQueue) o);
		if (o instanceof FloatQueueTwin)
			return list.add(((FloatQueueTwin) o).getQueue1()) && list.add(((FloatQueueTwin) o).getQueue2());
		throw new NotQueueException();
	}

	public boolean addAll(Collection c) {
		if (c == null)
			return false;
		synchronized (c) {
			for (Object o : c) {
				if (!add(o))
					return false;
			}
		}
		if (c instanceof QueueGroup)
			return list.addAll(((QueueGroup) c).list);
		return false;
	}

	public boolean addAll(int index, Collection c) throws NotQueueException {
		if (c == null)
			return false;
		synchronized (c) {
			for (Object o : c) {
				if (!add(o))
					return false;
			}
		}
		return true;
	}

	public void clear() {
		list.clear();
	}

	public boolean contains(Object o) {
		if (!(o instanceof DataQueue))
			return false;
		String s = ((DataQueue) o).getName();
		synchronized (list) {
			for (DataQueue q : list) {
				if (q.getName().equals(s))
					return true;
			}
		}
		return false;
	}

	public boolean containsAll(Collection c) {
		if (c == this)
			return true;
		synchronized (c) {
			for (Object o : c) {
				if (!contains(o))
					return false;
			}
		}
		return true;
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		return list.equals(o);
	}

	public Object get(int index) {
		return list.get(index);
	}

	public int hashCode() {
		return list.hashCode();
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator iterator() {
		return list.iterator();
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator listIterator() {
		return list.listIterator();
	}

	public ListIterator listIterator(int i) {
		return list.listIterator(i);
	}

	public Object remove(int index) {
		return list.remove(index);
	}

	public boolean remove(Object o) throws NotQueueException {
		if (o == null)
			return false;
		if (o instanceof DataQueue)
			return list.remove(o);
		if (o instanceof FloatQueueTwin)
			return list.remove(((FloatQueueTwin) o).getQueue1()) && list.remove(((FloatQueueTwin) o).getQueue2());
		if (o instanceof FloatQueueTriplet)
			return list.remove(((FloatQueueTriplet) o).getQueue1()) && list.remove(((FloatQueueTriplet) o).getQueue2())
					&& list.remove(((FloatQueueTriplet) o).getQueue3());
		throw new NotQueueException();
	}

	public boolean removeAll(Collection c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection c) {
		return list.retainAll(c);
	}

	public Object set(int index, Object element) throws NotQueueException {
		if (element == null)
			return null;
		if (!(element instanceof DataQueue))
			throw new NotQueueException();
		return list.set(index, (DataQueue) element);
	}

	public int size() {
		return list.size();
	}

	/** return a QueueGroup that contains the specified sub list */
	public List subList(int fromIndex, int toIndex) {
		return new QueueGroup(list.subList(fromIndex, toIndex));
	}

	public Object[] toArray() {
		return list.toArray();
	}

	@SuppressWarnings("unchecked")
	public Object[] toArray(Object[] a) {
		return list.toArray(a);
	}

	public QueueGroupTable getTable() {
		if (table == null) {
			table = new QueueGroupTable(this);
		}
		else {
			table.clear();
		}
		table.fill();
		return table;
	}

}