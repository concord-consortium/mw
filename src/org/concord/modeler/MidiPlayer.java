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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

public class MidiPlayer {

	private Sequencer sequencer;
	private boolean stopRequested;

	public MidiPlayer() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
		}
		catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
	}

	public void addMetaEventListener(MetaEventListener listener) {
		if (sequencer == null)
			return;
		sequencer.addMetaEventListener(listener);
	}

	public void removeMetaEventListener(MetaEventListener listener) {
		if (sequencer == null)
			return;
		sequencer.removeMetaEventListener(listener);
	}

	public void requestStop() {
		stopRequested = true;
	}

	public void play(File file) {
		if (sequencer == null)
			return;
		if (stopRequested || sequencer.getSequence() == null) {
			try {
				stopRequested = false;
				sequencer.setSequence(MidiSystem.getSequence(file));
			}
			catch (IOException e) {
				e.printStackTrace();
				return;
			}
			catch (InvalidMidiDataException e) {
				e.printStackTrace();
				return;
			}
		}
		sequencer.start();
	}

	public void setLoopCount(int i) {
		if (sequencer == null)
			return;
		sequencer.setLoopCount(i);
	}

	public void pause() {
		if (sequencer == null)
			return;
		if (sequencer.isRunning())
			sequencer.stop();
	}

	public void stop() {
		if (sequencer == null)
			return;
		sequencer.stop();
		stopRequested = true;
	}

	public void changeVolume(int n) {
		if (sequencer == null)
			return;
		Synthesizer s = null;
		if (System.getProperty("java.version").compareTo("1.5.0") < 0) {
			s = (Synthesizer) sequencer;
		}
		else {
			try {
				s = MidiSystem.getSynthesizer();
			}
			catch (MidiUnavailableException ex) {
				ex.printStackTrace();
			}
			if (s == null)
				return;
		}
		MidiChannel[] channels = s.getChannels();
		for (int i = 0; i < channels.length; i++) {
			channels[i].controlChange(7, (int) (n * 12.7f));
		}
	}

	public void mute(boolean on) {
	}

	public void destroy() {
		if (sequencer == null)
			return;
		sequencer.stop();
		sequencer.close();
		sequencer = null;
	}

}