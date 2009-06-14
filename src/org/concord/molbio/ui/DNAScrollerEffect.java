/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
package org.concord.molbio.ui;

interface DNAScrollerEffect {

	static final int DEFAULT_EFFECT_MAX_STEP = 25;
	static final int DEFAULT_EFFECT_TIME_DELAY = 100;

	void startEffect();

	void endEffect();

	void step();

	void forceEndEffect();

	int getCurrentStep();

	int getMaximumStep();

	int getEffectDelay();

	void setMaximumSteps(int val);

	void setEffectDelay(int val);

	boolean isInEffect();

	boolean isEffectDone();

	void destroy();

}

class BeginTranscriptionEffect implements DNAScrollerEffect {

	private Thread effectThread;
	private DNAScrollerWithRNA owner;
	private int effectDelay = DEFAULT_EFFECT_TIME_DELAY;
	private int maximumStep = DEFAULT_EFFECT_MAX_STEP;
	private boolean effectDone = false;
	private int initialState;
	private int finalState;
	private int currentStep;
	private boolean effectForcedToEnd = false;

	BeginTranscriptionEffect(DNAScrollerWithRNA owner, int initialState, int finalState) {
		this.owner = owner;
		this.initialState = initialState;
		this.finalState = finalState;
	}

	public void destroy() {
		if (effectThread != null)
			effectThread.interrupt();
	}

	public void startEffect() {
		currentStep = 0;
		effectDone = false;
		owner.setScrollerState(initialState);
		effectForcedToEnd = false;
		if (maximumStep > 0) {
			effectThread = new Thread(new Runnable() {
				public void run() {
					try {
						while (true) {
							step();
							Thread.sleep(effectDelay);
						}
					}
					catch (Throwable t) {
					}
					endEffect();
				}
			});
			effectThread.setName("Transcription and translation effect thread");
			effectThread.setPriority(Thread.MIN_PRIORITY);
			effectThread.start();
		}
		else {
			endEffect();
		}
	}

	public void forceEndEffect() {
		effectForcedToEnd = true;
		if (isInEffect()) {
			effectThread.interrupt();
		}
	}

	public int getCurrentStep() {
		return currentStep;
	}

	public void endEffect() {
		effectDone = true;
		effectThread = null;
		owner.setScrollerState(finalState);
		owner.clearEffect();
		if (!effectForcedToEnd)
			owner.effectJustEnded(this);
	}

	public void step() {
		currentStep++;
		if (currentStep > maximumStep) {
			effectThread.interrupt();
		}
		else {
			owner.resetDNA();
			owner.repaint();
		}
	}

	public void setMaximumSteps(int maximumStep) {
		this.maximumStep = maximumStep;
	}

	public int getMaximumStep() {
		return maximumStep;
	}

	public void setEffectDelay(int effectDelay) {
		this.effectDelay = effectDelay;
		if (this.effectDelay < 0)
			this.effectDelay = 0;
	}

	public int getEffectDelay() {
		return effectDelay;
	}

	public boolean isInEffect() {
		return effectThread != null && effectThread.isAlive();
	}

	public boolean isEffectDone() {
		return effectDone;
	}
}

class EndTranscriptionEffect implements DNAScrollerEffect {

	private Thread effectThread;
	private DNAScrollerWithRNA owner;
	private int effectDelay = DEFAULT_EFFECT_TIME_DELAY;
	private int maximumStep = DEFAULT_EFFECT_MAX_STEP;
	private boolean effectDone = false;
	private int initialState;
	private int finalState;
	private int currentStep;
	private boolean effectForcedToEnd = false;

	EndTranscriptionEffect(DNAScrollerWithRNA owner, int initialState, int finalState) {
		this.owner = owner;
		this.initialState = initialState;
		this.finalState = finalState;
	}

	public void destroy() {
		if (effectThread != null)
			effectThread.interrupt();
	}

	public void startEffect() {
		currentStep = 0;
		effectDone = false;
		owner.setScrollerState(initialState);
		effectForcedToEnd = false;
		if (maximumStep > 0) {
			effectThread = new Thread(new Runnable() {
				public void run() {
					try {
						while (true) {
							step();
							Thread.sleep(effectDelay);
						}
					}
					catch (Throwable t) {
					}
					endEffect();
				}
			});
			effectThread.setName("End transcription thread");
			effectThread.setPriority(Thread.MIN_PRIORITY);
			effectThread.start();
		}
		else {
			endEffect();
		}
	}

	public void forceEndEffect() {
		effectForcedToEnd = true;
		if (isInEffect()) {
			effectThread.interrupt();
		}
	}

	public int getCurrentStep() {
		return currentStep;
	}

	public void endEffect() {
		effectDone = true;
		effectThread = null;
		owner.setScrollerState(finalState);
		owner.clearEffect();
		if (!effectForcedToEnd)
			owner.effectJustEnded(this);
	}

	public void step() {
		currentStep++;
		if (currentStep > maximumStep) {
			effectThread.interrupt();
		}
		else {
			owner.resetDNA();
			owner.repaint();
		}
	}

	public void setMaximumSteps(int maximumStep) {
		this.maximumStep = maximumStep;
	}

	public int getMaximumStep() {
		return maximumStep;
	}

	public void setEffectDelay(int effectDelay) {
		this.effectDelay = effectDelay;
		if (this.effectDelay < 0)
			this.effectDelay = 0;
	}

	public int getEffectDelay() {
		return effectDelay;
	}

	public boolean isInEffect() {
		return effectThread != null && effectThread.isAlive();
	}

	public boolean isEffectDone() {
		return effectDone;
	}

}
