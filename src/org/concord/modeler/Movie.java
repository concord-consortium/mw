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

import java.util.List;

import org.concord.modeler.event.MovieEvent;
import org.concord.modeler.event.MovieListener;

public interface Movie {

	public void addMovieListener(MovieListener ml);

	public void removeMovieListener(MovieListener ml);

	public void notifyMovieListeners(MovieEvent e);

	public List getMovieListeners();

	/**
	 * return the length of the movie. If the tape of the movie is not full, the length is smaller than the capacity,
	 * otherwise they are equal.
	 */
	public int length();

	/** return the capacity of the movie. */
	public int getCapacity();

	/**
	 * set the capacity of this movie. The capacity of a movie is the number of (t, v) pairs allowed to store in any of
	 * the time series of a movie
	 */
	public void setCapacity(int i);

	/** get the index of the currently showing frame of the movie */
	public int getCurrentFrameIndex();

	/**
	 * set the index of the currently showing frame of the movie. This method should not be called by the movie player,
	 * but it must be called by the recorder to set the current frame to be the latest recorded one.
	 */
	public void setCurrentFrameIndex(int i);

	/** set the time for a frame to persist, in milliseconds */
	public void setFrameTime(int i);

	/** get the time for a frame to persist, in milliseconds */
	public int getFrameTime();

	/** show an arbitrary frame by its index */
	public void showFrame(int i);

	/** show the previous frame if it exists */
	public void showPreviousFrame();

	/** show the next frame if it exists */
	public void showNextFrame();

	/** show the first frame of the movie */
	public void showFirstFrame();

	/** show the last frame of the movie */
	public void showLastFrame();

	/** set the begin and end indices of the selected segment */
	public void setSegment(int begin, int end);

	/** get the begin and end indices of the selected segment, return them as an array of integers. */
	public int[] getSegment();

	/** as soon as the movie is loaded, call this method */
	public void init();

	/**
	 * rewind the tape
	 * 
	 * @return true if rewound to the head.
	 */
	public boolean rewind();

	/**
	 * fast forward the tape
	 * 
	 * @return true if forwarded to the end
	 */
	public boolean fastForward();

	/** step back one frame from the current one */
	public void stepBack();

	/** step forward one frame from the current one */
	public void stepForward();

	/**
	 * play the movie
	 * 
	 * @return true if the movie is played to the end
	 */
	public boolean play();

	/** pause the movie */
	public void pause();

}