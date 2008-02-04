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

package org.concord.modeler.event;

import java.util.EventObject;

public class ProgressEvent extends EventObject {

	private String description;
	private int percent = -1;
	private int min = 0;
	private int max = 100;

	public ProgressEvent(Object source, int percent) {
		super(source);
		this.percent = percent;
	}

	public ProgressEvent(Object source, String description) {
		super(source);
		this.description = description;
	}

	public ProgressEvent(Object source, int percent, String description) {
		super(source);
		this.percent = percent;
		this.description = description;
	}

	public ProgressEvent(Object source, int percent, int min, int max, String description) {
		this(source, percent);
		this.min = min;
		this.max = max;
		this.description = description;
	}

	public int getMinimum() {
		return min;
	}

	public int getMaximum() {
		return max;
	}

	public int getPercent() {
		return percent;
	}

	public String getDescription() {
		return description;
	}

}