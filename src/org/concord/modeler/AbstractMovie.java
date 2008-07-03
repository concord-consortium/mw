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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.concord.modeler.event.MovieEvent;
import org.concord.modeler.event.MovieListener;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.util.DataQueue;
import org.concord.modeler.util.SwingWorker;

import static javax.swing.Action.*;

public abstract class AbstractMovie implements Movie {

	public Action rewindMovie, stepForwardMovie, stepBackMovie;
	protected Action playMovie, pauseMovie, fastForwardMovie, runAction, stopAction;
	protected volatile int frameIndex;
	protected volatile int frameTime = 50;
	protected volatile boolean paused;
	protected volatile int capacity = DataQueue.DEFAULT_SIZE;
	protected volatile int segmentStart;
	protected volatile int segmentEnd;

	private List<MovieListener> listeners;
	private long timePassed, lastTime;

	public AbstractMovie() {
		createActions();
	}

	private abstract class DefaultAction extends AbstractAction {
		public String toString() {
			return (String) getValue(SHORT_DESCRIPTION);
		}
	}

	private void createActions() {

		playMovie = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				enableMovieActions(false);
				new SwingWorker("Play Movie") {
					public Object construct() {
						return new Boolean(play());
					}

					public void finished() {
						Boolean b = (Boolean) getValue();
						if (b)
							enableMovieActions(true);
					}
				}.start();
			}
		};
		playMovie.putValue(NAME, "Play");
		playMovie.putValue(SHORT_DESCRIPTION, "Play movie");
		playMovie.putValue(SMALL_ICON, IconPool.getIcon("play"));

		pauseMovie = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				enableMovieActions(true);
				pause();
			}
		};
		pauseMovie.putValue(NAME, "Pause");
		pauseMovie.putValue(SHORT_DESCRIPTION, "Pause movie");
		pauseMovie.putValue(SMALL_ICON, IconPool.getIcon("pause"));

		fastForwardMovie = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				enableMovieActions(false);
				new SwingWorker("Fast Forward Movie") {
					public Object construct() {
						return new Boolean(fastForward());
					}

					public void finished() {
						Boolean b = (Boolean) getValue();
						if (b)
							enableMovieActions(true);
					}
				}.start();
			}
		};
		fastForwardMovie.putValue(NAME, "Fast Forward");
		fastForwardMovie.putValue(SHORT_DESCRIPTION, "Fast forward movie");

		rewindMovie = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				enableMovieActions(false);
				new SwingWorker("Rewind Movie") {
					public Object construct() {
						return new Boolean(rewind());
					}

					public void finished() {
						Boolean b = (Boolean) getValue();
						if (b)
							enableMovieActions(true);
					}
				}.start();
			}
		};
		rewindMovie.putValue(NAME, "Rewind");
		rewindMovie.putValue(SHORT_DESCRIPTION, "Rewind movie");
		rewindMovie.putValue(SMALL_ICON, new ImageIcon(AbstractMovie.class.getResource("images/Rewind.gif")));

		stepForwardMovie = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				stepForward();
			}
		};
		stepForwardMovie.putValue(NAME, "Step Forward");
		stepForwardMovie.putValue(SHORT_DESCRIPTION, "Step forward movie");
		stepForwardMovie.putValue(SMALL_ICON, new ImageIcon(AbstractMovie.class.getResource("images/StepForward.gif")));

		stepBackMovie = new DefaultAction() {
			public void actionPerformed(ActionEvent e) {
				stepBack();
			}
		};
		stepBackMovie.putValue(NAME, "Step Back");
		stepBackMovie.putValue(SHORT_DESCRIPTION, "Step back movie");
		stepBackMovie.putValue(SMALL_ICON, new ImageIcon(AbstractMovie.class.getResource("images/StepBack.gif")));

	}

	public synchronized int getCurrentFrameIndex() {
		return frameIndex;
	}

	public synchronized void setCurrentFrameIndex(int i) {
		frameIndex = i;
	}

	void changeToolTipText() {
		if (runAction != null) {
			if (frameIndex < length() - 1) {
				runAction.putValue(Action.SHORT_DESCRIPTION, "Play back");
			}
			else {
				runAction.putValue(Action.SHORT_DESCRIPTION, "Run the model");
			}
		}
	}

	public void setFrameTime(int i) {
		frameTime = i;
	}

	public int getFrameTime() {
		return frameTime;
	}

	/** speed the same as normal play */
	public boolean rewind() {
		paused = false;
		while (!paused && frameIndex > 0) {
			timePassed = System.currentTimeMillis() - lastTime;
			showPreviousFrame();
			if (timePassed < frameTime) {
				try {
					Thread.sleep(frameTime - timePassed);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			lastTime = System.currentTimeMillis();
		}
		paused = true;
		changeToolTipText();
		return frameIndex == 0;
	}

	/** three times faster than normal play. */
	public boolean fastForward() {
		paused = false;
		int len = length() - 1;
		while (!paused && frameIndex < len) {
			timePassed = System.currentTimeMillis() - lastTime;
			showNextFrame();
			if (timePassed < frameTime / 3) {
				try {
					Thread.sleep(frameTime / 3 - timePassed);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			lastTime = System.currentTimeMillis();
		}
		paused = true;
		changeToolTipText();
		return frameIndex == len;
	}

	public void stepBack() {
		if (paused) {
			showPreviousFrame();
			changeToolTipText();
		}
	}

	public void stepForward() {
		if (paused) {
			showNextFrame();
			changeToolTipText();
		}
	}

	public boolean play() {
		paused = false;
		int l1 = length() - 1;
		while (!paused && frameIndex < l1) {
			timePassed = System.currentTimeMillis() - lastTime;
			showNextFrame();
			if (timePassed < frameTime) {
				try {
					Thread.sleep(frameTime - timePassed);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			lastTime = System.currentTimeMillis();
		}
		paused = true;
		changeToolTipText();
		return frameIndex == l1;
	}

	public void showFrame(final int i) {
		if (paused) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					boolean x = i > 0;
					stepBackMovie.setEnabled(x);
					rewindMovie.setEnabled(x);
					x = i < length() - 1;
					stepForwardMovie.setEnabled(x);
					fastForwardMovie.setEnabled(x);
				}
			});
		}
	}

	public void showFirstFrame() {
		frameIndex = 0;
		showFrame(0);
		notifyMovieListeners(new MovieEvent(this, MovieEvent.FRAME_CHANGED, frameIndex));
	}

	public void showLastFrame() {
		if (length() <= 0)
			return;
		frameIndex = length() - 1;
		showFrame(frameIndex);
		notifyMovieListeners(new MovieEvent(this, MovieEvent.FRAME_CHANGED, frameIndex));
	}

	public void showNextFrame() {
		if (frameIndex >= length() - 1)
			return;
		showFrame(++frameIndex);
		notifyMovieListeners(new MovieEvent(this, MovieEvent.FRAME_CHANGED, frameIndex));
	}

	public void showPreviousFrame() {
		if (frameIndex <= 0)
			return;
		showFrame(--frameIndex);
		notifyMovieListeners(new MovieEvent(this, MovieEvent.FRAME_CHANGED, frameIndex));
	}

	public void init() {
		showLastFrame();
		pause();
	}

	public void pause() {
		paused = true;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pauseMovie.setEnabled(false);
				changeToolTipText();
			}
		});
	}

	public void setCapacity(int i) {
		capacity = i;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setSegment(int begin, int end) {
		segmentStart = begin;
		segmentEnd = end;
	}

	public int[] getSegment() {
		return new int[] { segmentStart, segmentEnd };
	}

	public List<MovieListener> getMovieListeners() {
		return listeners;
	}

	public void addMovieListener(MovieListener ml) {
		if (ml == null)
			throw new IllegalArgumentException("null input");
		if (listeners == null) {
			listeners = new ArrayList<MovieListener>();
		}
		else {
			if (listeners.contains(ml))
				return;
		}
		listeners.add(ml);
	}

	public void removeMovieListener(MovieListener ml) {
		if (ml == null)
			throw new IllegalArgumentException("null input");
		if (listeners == null || listeners.isEmpty())
			return;
		listeners.remove(ml);
	}

	public void notifyMovieListeners(MovieEvent e) {
		if (listeners == null || listeners.isEmpty())
			return;
		for (MovieListener l : listeners)
			l.frameChanged(e);
	}

	protected void enableMovieActions(final boolean isPaused) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pauseMovie.setEnabled(!isPaused);
				if (isPaused) {
					boolean x = frameIndex > 0;
					stepBackMovie.setEnabled(x);
					rewindMovie.setEnabled(x);
					x = frameIndex < length() - 1;
					stepForwardMovie.setEnabled(x);
					fastForwardMovie.setEnabled(x);
				}
				else {
					stepBackMovie.setEnabled(false);
					rewindMovie.setEnabled(false);
					stepForwardMovie.setEnabled(false);
					fastForwardMovie.setEnabled(false);
				}
				playMovie.setEnabled(isPaused);
				if (runAction != null)
					runAction.setEnabled(isPaused);
				if (stopAction != null)
					stopAction.setEnabled(!isPaused);
			}
		});
	}

	public void enableAllMovieActions(final boolean b) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				pauseMovie.setEnabled(b);
				if (b) {
					boolean x = frameIndex > 0;
					stepBackMovie.setEnabled(x);
					rewindMovie.setEnabled(x);
					x = frameIndex < length() - 1;
					stepForwardMovie.setEnabled(x);
					fastForwardMovie.setEnabled(x);
				}
				else {
					stepBackMovie.setEnabled(false);
					rewindMovie.setEnabled(false);
					stepForwardMovie.setEnabled(false);
					fastForwardMovie.setEnabled(false);
				}
				playMovie.setEnabled(b);
			}
		});
	}

	/** optional */
	public void setRunAction(Action a) {
		runAction = a;
	}

	/** optional */
	public void setStopAction(Action a) {
		stopAction = a;
	}

}