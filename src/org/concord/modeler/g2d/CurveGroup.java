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

package org.concord.modeler.g2d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This defines a curve group, which is a bunch of x-y data sets stored in an <tt>ArrayList</tt>. Data must be
 * one-dimensional arrays, with even-number elements storing x data and odd-number elements storing y data, i.e. in the
 * sequence of <tt>xyxyxyxy......</tt>
 * 
 * @author Qian Xie
 */

public class CurveGroup implements Serializable {

	private String title;
	private AxisLabel xLabel;
	private AxisLabel yLabel;
	private List<Curve> list;

	public CurveGroup() {
		xLabel = new AxisLabel("x");
		yLabel = new AxisLabel("y");
		title = "y(x)";
		list = Collections.synchronizedList(new ArrayList<Curve>());
	}

	/**
	 * construct a curve group.
	 * 
	 * @param title
	 *            the title of this curve group
	 * @param xLabel
	 *            the label of the x axis
	 * @param yLabel
	 *            the label of the y axis
	 */
	public CurveGroup(String title, AxisLabel xLabel, AxisLabel yLabel) {
		this.title = title;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		list = Collections.synchronizedList(new ArrayList<Curve>());
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String s) {
		title = s;
	}

	public AxisLabel getLabelOfX() {
		return xLabel;
	}

	public void setLabelOfX(AxisLabel s) {
		xLabel = s;
	}

	public AxisLabel getLabelOfY() {
		return yLabel;
	}

	public void setLabelOfY(AxisLabel s) {
		yLabel = s;
	}

	public Curve getCurve(int index) {
		return list.get(index);
	}

	public boolean addCurve(Curve c) {
		return list.add(c);
	}

	public boolean addCurveGroup(CurveGroup group) {
		return list.addAll(group.list);
	}

	public boolean hasNoCurve() {
		return list.isEmpty();
	}

	public boolean removeCurve(Curve c) {
		return list.remove(c);
	}

	public int curveCount() {
		return list == null ? 0 : list.size();
	}

	public Curve setCurve(int index, Curve c) {
		return list.set(index, c);
	}

	public void removeAllCurves() {
		list.clear();
	}

	public boolean containsCurve(Curve c) {
		return list.contains(c);
	}

	public Object getSynchronizationLock() {
		return list;
	}

	public Iterator iterator() {
		return list.iterator();
	}

}
