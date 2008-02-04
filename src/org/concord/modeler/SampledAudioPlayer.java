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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SampledAudioPlayer {

	private Clip clip;
	private boolean stopRequested = true;
	private List<LineListener> listeners;

	public SampledAudioPlayer() {
	}

	public void addLineListener(LineListener listener) {
		if (listeners == null)
			listeners = new ArrayList<LineListener>();
		listeners.add(listener);
	}

	public void removeLineListener(LineListener listener) {
		if (listeners != null)
			listeners.remove(listener);
		if (clip == null)
			return;
		clip.removeLineListener(listener);
	}

	public void requestStop() {
		stopRequested = true;
	}

	public void play(File file) {
		if (stopRequested) {
			stopRequested = false;
			try {
				AudioInputStream stream = AudioSystem.getAudioInputStream(file);
				AudioFormat format = stream.getFormat();
				DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(),
						((int) stream.getFrameLength() * format.getFrameSize()));
				clip = (Clip) AudioSystem.getLine(info);
				if (listeners != null) {
					for (LineListener l : listeners)
						clip.addLineListener(l);
				}
				clip.open(stream); // This method does not return until the audio file is completely loaded
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (LineUnavailableException e) {
				e.printStackTrace();
			}
			catch (UnsupportedAudioFileException e) {
				e.printStackTrace();
			}
		}
		if (clip != null)
			clip.start();
	}

	public void stop() {
		if (clip == null)
			return;
		clip.stop();
		clip.close();
		stopRequested = true;
	}

	public void pause() {
		if (clip == null)
			return;
		if (clip.isRunning()) {
			clip.stop();
		}
	}

	public void closeClip() {
		if (clip == null)
			return;
		if (clip.getFramePosition() == clip.getFrameLength()) {
			stopRequested = true;
			clip.close();
		}
	}

	public void changeVolume(int i) {
		if (clip == null)
			return;
		FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		fc.setValue((float) (Math.log(0.1 * i) / Math.log(10.0) * 20.f));
	}

	public void setLoopCount(int i) {
		if (clip == null)
			return;
		clip.loop(i);
	}

	public void mute(boolean on) {
		if (clip == null)
			return;
		BooleanControl bc = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
		bc.setValue(on);
	}

	public void destroy() {
		if (clip != null) {
			if (listeners != null && !listeners.isEmpty()) {
				for (LineListener l : listeners)
					clip.removeLineListener(l);
			}
			clip.close();
			clip = null;
		}
		if (listeners != null)
			listeners.clear();
	}

}