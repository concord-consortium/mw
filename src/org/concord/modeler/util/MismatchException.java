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

/**
 * <p>
 * If a user tries to add a <tt>TimeSeries</tt> that mismatches the existing ones to a <tt>TimeSeriesGroup</tt>,
 * throw this exception.
 * </p>
 * 
 * <p>
 * Two time series are considered mismatched if they do not have the same sampling frequency, pointer index and length.
 * If two time series do not have the same length, either part of the array of the longer one will not be used, or an
 * <tt>ArrayIndexOutOfBounds</tt> exception will be thrown. If the two time series do not have the same interval
 * (sampling frequency), it is generally not a good idea to put them together into a time series group. So is in the
 * case of mismatched pointers. Even if the two time series have the same length and interval, they will have a phase
 * shift if their pointers do not pointer to the same index. This can lead to serious mistakes in data analysis.
 * </p>
 * 
 * @author Qian Xie
 */

public class MismatchException extends IllegalArgumentException {

	public MismatchException() {
		super("Arrays mismatch in a time series/queue group.");
	}

	public MismatchException(String s) {
		super(s);
	}

}