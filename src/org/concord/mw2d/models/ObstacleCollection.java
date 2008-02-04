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

package org.concord.mw2d.models;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ObstacleCollection {

	List<RectangularObstacle> obsList;
	private PropertyChangeSupport pcs;
	private boolean fireCollisionEvents;
	private MDModel model;

	public ObstacleCollection(MDModel model) {
		super();
		this.model = model;
		obsList = Collections.synchronizedList(new ArrayList<RectangularObstacle>());
		pcs = new PropertyChangeSupport(this);
	}

	public ArrayList getList() {
		return new ArrayList<RectangularObstacle>(obsList);
	}

	public Iterator iterator() {
		return obsList.iterator();
	}

	public Object getSynchronizationLock() {
		return obsList;
	}

	public Object[] toArray() {
		return obsList.toArray();
	}

	public void add(int i, RectangularObstacle o) {
		obsList.add(i, o);
		o.setModel(model);
	}

	public boolean add(RectangularObstacle o) {
		o.setModel(model);
		return obsList.add(o);
	}

	public boolean addAll(List<RectangularObstacle> c) {
		return obsList.addAll(c);
	}

	public boolean contains(Object o) {
		return obsList.contains(o);
	}

	public boolean containsAll(Collection c) {
		return obsList.containsAll(c);
	}

	public int indexOf(Object o) {
		return obsList.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return obsList.lastIndexOf(o);
	}

	public RectangularObstacle set(int i, RectangularObstacle o) {
		return obsList.set(i, o);
	}

	public boolean remove(RectangularObstacle o) {
		return obsList.remove(o);
	}

	public void clear() {
		obsList.clear();
	}

	public int size() {
		return obsList.size();
	}

	public boolean isEmpty() {
		return obsList.isEmpty();
	}

	public RectangularObstacle get(int index) {
		return obsList.get(index);
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	public void setFireCollisionEvents(boolean b) {
		fireCollisionEvents = b;
	}

	public boolean getFireCollisionEvents() {
		return fireCollisionEvents;
	}

	public void move(double timeStep, double timeStep2, int n, Atom[] atom) {
		synchronized (obsList) {
			for (RectangularObstacle o : obsList)
				o.move(timeStep, timeStep2, n, atom);
		}
	}

	/*
	 * collisions of atoms with the obstacles. An obstacle reflection action needs information from dynamics history,
	 * i.e. displacements dx, dy, to find out which faces atoms have crossed through.
	 * 
	 * @param n the number of atoms @param atom the atom array @return a mapping of which side of which obstacle was hit
	 * by which atoms with what velocity at what location, null if no obstacle is found, or no details are needed. If no
	 * atom hits an obstacle, the value (an array list) retrieved from that key will be empty. Right now, the event of
	 * an atom hitting the vertex of an obstacle is considered rare. If such a thing does happen, it will be given a
	 * common side ID -1 without distinguishing which edge was hit. The faces of an obstacle are represented by the
	 * constants: TOP, LEFT, BOTTOM, RIGHT.
	 */
	Map collide(int n, Atom[] atom) {

		if (isEmpty())
			return null;

		if (fireCollisionEvents) {

			Map<RectangularObstacle, List<FaceCollision>> collision = new HashMap<RectangularObstacle, List<FaceCollision>>();
			synchronized (obsList) {
				for (RectangularObstacle o : obsList) {
					List<FaceCollision> list = o.collide(n, atom, true);
					if (list == null || list.isEmpty())
						continue;
					collision.put(o, list);
				}
			}
			if (!collision.isEmpty())
				pcs.firePropertyChange(RectangularObstacle.COLLISION_EVENT, null, collision);
			return collision;

		}

		synchronized (obsList) {
			for (RectangularObstacle o : obsList)
				o.collide(n, atom, false);
		}

		return null;

	}

	public void setSelectionSet(BitSet bs) {
		if (isEmpty())
			return;
		model.setExclusiveSelection(false);
		synchronized (obsList) {
			for (int i = 0; i < obsList.size(); i++)
				obsList.get(i).setSelected(bs == null ? false : bs.get(i));
		}
	}

	public void clearSelection() {
		synchronized (obsList) {
			for (RectangularObstacle o : obsList)
				o.setSelected(false);
		}
	}

}