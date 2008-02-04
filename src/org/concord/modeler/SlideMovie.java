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

/**
 * Movie is an abstract structure on top of model. It does not contain real data, or pointers to real data. A movie
 * realizes its functions through an adapter built in with the model it serves, which is called a tape. In a model, a
 * tape does not contain real data either. What it does is to provide references to the real data, which are time series
 * of each object of the model. Time series is the ultimate mechanism to store and update data for movies.
 * 
 * @author Charles Xie
 */

public abstract class SlideMovie extends AbstractMovie {

	protected MovieSlider movieSlider;

	public SlideMovie() {
		super();
		movieSlider = new MovieSlider(this);
		addMovieListener(movieSlider);
	}

	public MovieSlider getMovieSlider() {
		return movieSlider;
	}

	public void setCurrentFrameIndex(final int i) {
		super.setCurrentFrameIndex(i);
		movieSlider.setCurrentFrame(i);
	}

	public void setCapacity(int i) {
		super.setCapacity(i);
		movieSlider.setTotalFrame(i);
	}

	public void showLastFrame() {
		super.showLastFrame();
		movieSlider.setCurrentFrame(frameIndex);
	}

	public void enableMovieActions(final boolean isPaused) {
		super.enableMovieActions(isPaused);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				movieSlider.setEnabled(isPaused);
			}
		});
	}

	public boolean shouldFlash() {
		if (stopAction == null)
			return !movieSlider.isEnabled() && !pauseMovie.isEnabled();
		return !movieSlider.isEnabled() && !runAction.isEnabled() && getCurrentFrameIndex() >= length() - 1;
	}

}