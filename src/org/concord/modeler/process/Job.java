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

import java.awt.Component;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * This is an implementation of the dynamical task pool.
 * </p>
 * 
 * <p>
 * Like a car, a job process can be turned off (engine-off), or just stopped (braked). A stopped job can be continued in
 * a simple way (just like releasing the brake). A turned-off job has to do some extra work (like turning the engine on)
 * in order to get started.
 * </p>
 * 
 * <p>
 * Such a mechanism is used to make the GUI responsive: A user may pull down a menu while the job is running. If he does
 * so, the job will stop to yield to his actions, and automatically continue running once he is done. But if he works on
 * the GUI when the job is not running (i.e. the engine is already turned off), the job will not automatically start
 * after his action with the GUI is over. In the case that the job has been "turned off", the only way to get it run is
 * to "turn it on" again.
 * </p>
 * 
 * @author Charles Xie
 */

public abstract class Job implements Runnable {

	/** the task pool */
	protected List<Loadable> taskPool;

	/** the running index of step of this job */
	protected int indexOfStep;

	private volatile Thread mainThread;
	private volatile boolean off = true, stopped = true;
	private List<Loadable> jobToRemove, jobToAdd;
	private String name;
	private JobTable jobTable;

	/**
	 * @param name
	 *            the name of this job
	 */
	public Job(String name) {
		this.name = name;
		taskPool = Collections.synchronizedList(new ArrayList<Loadable>());
		jobToAdd = Collections.synchronizedList(new ArrayList<Loadable>());
		jobToRemove = Collections.synchronizedList(new ArrayList<Loadable>());
		jobTable = new JobTable(this);
	}

	public String toString() {
		return taskPool.toString();
	}

	/** remove all the tasks in the pool */
	public void clear() {
		taskPool.clear();
		if (mainThread != null)
			mainThread.interrupt();
	}

	/** remove all the non-system tasks */
	public void removeAllNonSystemTasks() {
		synchronized (taskPool) {
			for (Loadable l : taskPool) {
				if (!l.isSystemTask() || (l instanceof DelayModelTimeLoadable))
					remove(l);
			}
		}
		processPendingRequests();
	}

	/** get customer tasks */
	public List<Loadable> getCustomTasks() {
		List list = new ArrayList();
		synchronized (taskPool) {
			for (Loadable l : taskPool) {
				if (!l.isSystemTask())
					list.add(l);
			}
		}
		return list;
	}

	/** get the running index of step */
	public int getIndexOfStep() {
		return indexOfStep;
	}

	public void setIndexOfStep(int i) {
		indexOfStep = i;
	}

	/** set the name of this job */
	public void setName(String s) {
		name = s;
	}

	/** get the name of this job */
	public String getName() {
		return name;
	}

	/** add a task to the task pool. */
	public void add(Loadable l) {
		if (l == null)
			throw new IllegalArgumentException("job to be added cannot be null.");
		if (jobToAdd.contains(l))
			return;
		jobToAdd.add(l);
	}

	/** remove a task from the task pool. */
	public void remove(Loadable l) {
		if (l == null)
			throw new IllegalArgumentException("job to be added cannot be null.");
		if (jobToRemove.contains(l))
			return;
		jobToRemove.add(l);
	}

	/** return true if the task pool contains the specified job */
	public boolean contains(Loadable l) {
		return taskPool.contains(l);
	}

	public boolean containsName(String name) {
		synchronized (taskPool) {
			for (Loadable l : taskPool) {
				if (l.getName().equals(name))
					return true;
			}
		}
		return false;
	}

	public boolean toBeAdded(Loadable l) {
		return jobToAdd.contains(l);
	}

	public boolean hasJobToAdd() {
		return !jobToAdd.isEmpty();
	}

	public boolean toBeRemoved(Loadable l) {
		return jobToRemove.contains(l);
	}

	public boolean hasJobToRemove() {
		return !jobToRemove.isEmpty();
	}

	/** search a loadable subtask by name. */
	public Loadable getTask(String s) {
		synchronized (taskPool) {
			for (Loadable l : taskPool) {
				if (l.getName().equals(s))
					return l;
			}
		}
		return null;
	}

	/** create a new thread and activate the subtasks registered with the task dispatcher. */
	public synchronized void start() {
		stopped = false;
		if (mainThread == null) {
			mainThread = new Thread(this, name);
			mainThread.setPriority(Thread.MIN_PRIORITY);
			mainThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				public void uncaughtException(Thread t, Throwable e) {
					mainThread = null;
					e.printStackTrace();
				}
			});
			mainThread.start();
		}
		else {
			// just in case there is a missed notification problem
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
			}
			notifyAll();
		}
	}

	/** send the script to the model supported by this Job. */
	public abstract void runScript(String script);

	/** notify the model supported by this Job that the tasks have changed. */
	public abstract void notifyChange();

	/** This method must be overriden/customized when you construct a <tt>Job</tt> object. */
	public synchronized void run() {
		off = false;
	}

	/** turn off the job */
	public synchronized void turnOff() {
		stop();
		off = true;
	}

	/** return true if this job has been turned off */
	public synchronized boolean isTurnedOff() {
		return off;
	}

	/** stop the job */
	public synchronized void stop() {
		// mainThread=null; // do not create a new thread.
		stopped = true;
	}

	/** return true of this job is stopped. */
	public synchronized boolean isStopped() {
		// return mainThread==null;
		return stopped;
	}

	/**
	 * execute the listed jobs sequentially. Do not synchronize this iterator block, or it may freeze (it froze on
	 * Linux), because we cannot 100% rule out that a task to be execute may call something that should have been called
	 * in the event thread.
	 */
	protected void execute() {
		processPendingRequests();
		Loadable task = null;
		try { // it probably won't hurt much not to synchronize this iterator
			for (Iterator it = taskPool.iterator(); it.hasNext();) {
				if (off || isStopped())
					return;
				task = (AbstractLoadable) it.next();
				if (task == null)
					continue;
				if (task.isCompleted()) {
					remove(task);
				}
				if (task.getInterval() <= 1) {
					task.execute();
				}
				else {
					if (indexOfStep % task.getInterval() == 0)
						task.execute();
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void processPendingRequests() {
		if (!jobToAdd.isEmpty()) {
			synchronized (jobToAdd) {
				for (Loadable l : jobToAdd)
					addJob(l);
			}
			jobToAdd.clear();
		}
		if (!jobToRemove.isEmpty()) {
			synchronized (jobToRemove) {
				for (Loadable l : jobToRemove)
					removeJob(l);
			}
			jobToRemove.clear();
		}
	}

	private void removeJob(final Loadable l) {
		if (!contains(l))
			return;
		taskPool.remove(l);
		if (EventQueue.isDispatchThread()) {
			jobTable.removeRow(l);
		}
		else {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					jobTable.removeRow(l);
				}
			});
		}
	}

	private void addJob(final Loadable l) {
		if (contains(l))
			return;
		if (taskPool.isEmpty()) {
			taskPool.add(l);
		}
		else {
			synchronized (taskPool) {
				int m = -1;
				int n = taskPool.size();
				for (int i = 0; i < n; i++) {
					if (l.getPriority() > taskPool.get(i).getPriority()) {
						m = i;
						break;
					}
				}
				if (m == -1) {
					taskPool.add(l);
				}
				else {
					taskPool.add(m, l);
				}
			}
		}
		if (EventQueue.isDispatchThread()) {
			jobTable.insertRow(l);
		}
		else {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					jobTable.insertRow(l);
				}
			});
		}
	}

	public void show(final Component owner) {
		if (EventQueue.isDispatchThread()) {
			jobTable.show(owner);
		}
		else {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					jobTable.show(owner);
				}
			});
		}
	}

}