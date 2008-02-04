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

package org.concord.modeler;

import org.concord.modeler.process.Job;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.FloatQueue;
import org.concord.modeler.util.HomoQueueGroup;

public interface Model extends BasicModel {

	/** get a property */
	public Object getProperty(Object key);

	/** put a property */
	public void putProperty(Object key, Object value);

	/** get the current model time. NOTE: This is not the computer time. */
	public float getModelTime();

	/** this call closes the <tt>InputStream</tt> to interrupt I/O blocking thread. */
	public void stopInput();

	/** return the job for unfolding this model */
	public Job getJob();

	public FloatQueue getModelTimeQueue();

	/** get the movie queue group */
	public HomoQueueGroup getMovieQueueGroup();

	/** search for a queue with a specified name */
	public DataQueue getQueue(String name);

	public Movie getMovie();

	public boolean getRecorderDisabled();

}