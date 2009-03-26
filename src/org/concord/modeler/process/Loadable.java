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

package org.concord.modeler.process;

import java.beans.PropertyChangeListener;

/**
 * <p>
 * There are many processes in a simulation. For example, there are some data mining processes that collect information
 * while the simulation is running. In general, all processes can be classified into two types:
 * </p>
 * 
 * <p>
 * <ul>
 * <li>Synchronous: Process run by the main thread running the molecular dynamics
 * <li>Asynchronous: Process run by a different thread than the one running the molecular dynamics
 * </ul>
 * </p>
 * 
 * <p>
 * This interface defines a synchronous implementation, which should be enclosed or nested in a main job process. The
 * mechanism for an object implementing this interface is pretty much like SwingUtilities' invokeLater(Runnable r),
 * which puts a runnable job in the event-dispatching thread.
 * </p>
 * 
 * <p>
 * Basically, a simulation process has its natural cycles: The molecular dynamics defines an inherent clock for it. Each
 * step of its evolutionary course consumes certain number of CPU cycles, dependent on the basic algorithmic operations
 * needed for unfolding the model. In this synchronous implementation, it is very easy to control a process.
 * </p>
 * 
 * <p>
 * The task manager executes the loadable subtasks that were added to its pool at each step. There can be many subtasks.
 * In order to stipulate which one goes before another, a loadable subtask can have a priority number. A subtask that
 * has a higher priority will be executed earlier. The difference between subtask prioritization and thread
 * prioritization is that all subtasks will be predictably done each time the task manager traverses them, but the time
 * a thread is invoked or completed may be unpredicted.
 * </p>
 * 
 * @author Charles Xie
 */

public interface Loadable extends Executable {

	public static final int ETERNAL = Integer.MAX_VALUE;

	public void setSystemTask(boolean b);

	public boolean isSystemTask();
	
	public boolean isEnabled();
	
	public void setEnabled(boolean b);

	public String getName();

	public void setName(String name);

	public String getDescription();

	public void setDescription(String text);

	public void setScript(String script);

	public String getScript();

	public void setCompleted(boolean b);

	public boolean isCompleted();

	public void setPriority(int i);

	public int getPriority();

	/** return the time interval (in steps) between every two next calls to <tt>execute()</tt>. */
	public int getInterval();

	/** set the time interval (in steps) between every two next calls to <tt>execute()</tt>. */
	public void setInterval(int i);

	public void setMinInterval(int i);

	public int getMinInterval();

	public void setMaxInterval(int i);

	public int getMaxInterval();

	/** return the lifetime of this subtask */
	public int getLifetime();

	/** set the lifetime of this subtask */
	public void setLifetime(int i);

	public void setMinLifetime(int i);

	public int getMinLifetime();

	public void setMaxLifetime(int i);

	public int getMaxLifetime();

	public void addPropertyChangeListener(PropertyChangeListener pcl);

	public void removePropertyChangeListener(PropertyChangeListener pcl);

}