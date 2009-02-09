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

package org.concord.molbio.engine;

import java.util.Hashtable;
import java.util.Vector;

import org.concord.molbio.event.MutationEvent;
import org.concord.molbio.event.MutationListener;

public abstract class Mutator implements MutationSource {

	public final static int MUTATOR_UNKNOWN = -1;
	public final static int MUTATOR_IDENTITY = 0;
	public final static int MUTATOR_SUBSTITUTION = 1;
	public final static int MUTATOR_DELETION = 2;
	public final static int MUTATOR_INSERTION = 3;
	public final static int MUTATOR_MIXED = 4;
	public final static int NUMB_POSSIBLE_MUTATORS = 4;

	int mutatorType = MUTATOR_UNKNOWN;
	int fragmentLength = 1;
	private int mutationDirection = 1;
	private Vector<MutationListener> mutationListeners = new Vector<MutationListener>();
	private static Hashtable<Integer, Mutator> mutators = new Hashtable<Integer, Mutator>();
	private boolean eventNotificationEnabled = true;

	public final static Mutator getInstance(int mutatorType) {
		Mutator mutator = mutators.get(mutatorType);
		if (mutator != null)
			return mutator;
		switch (mutatorType) {
		case MUTATOR_IDENTITY:
			mutator = new IdentityMutator();
			break;
		case MUTATOR_SUBSTITUTION:
			mutator = new SubstitutionMutator();
			break;
		case MUTATOR_DELETION:
			mutator = new DeletionMutator();
			break;
		case MUTATOR_INSERTION:
			mutator = new InsertionMutator();
			break;
		case MUTATOR_MIXED:
			mutator = new MixedMutator();
			break;
		}
		if (mutator != null)
			mutators.put(mutatorType, mutator);
		return mutator;
	}

	public final static Mutator getMixedInstance(int ident, int subst, int insert, int delet) {
		Mutator mutator = mutators.get(MUTATOR_MIXED);
		if (mutator == null) {
			mutator = new MixedMutator(ident, subst, insert, delet);
			mutators.put(MUTATOR_MIXED, mutator);
		}
		else {
			mutator.setMutationParam(new Integer[] { ident, subst, insert, delet });
		}
		return mutator;
	}

	public abstract void setMutationParam(Object[] params);

	protected abstract void mutate(DNA dna, int strandIndex, int nucleotideIndex);

	protected abstract void mutate(Strand strand, int nucleotideIndex);

	public synchronized void doMutation(DNA dna, int strandIndex, int nucleotideIndex, int length) {
		Mutator mutator = mutators.get(mutatorType);
		if (mutator != null) {
			mutator.setFragmentLength(length);
			mutator.mutate(dna, strandIndex, nucleotideIndex);
		}
	}

	public synchronized void doMutation(Strand strand, int nucleotideIndex, int length) {
		Mutator mutator = mutators.get(mutatorType);
		if (mutator != null) {
			mutator.setFragmentLength(length);
			mutator.mutate(strand, nucleotideIndex);
		}
	}

	public int getMutatorType() {
		return mutatorType;
	}

	public int getFragmentLength() {
		return fragmentLength;
	}

	public void setFragmentLength(int val) {
		if (val < 0)
			val = -val;
		this.fragmentLength = val;
	}

	public void defineRightMutationInterval(int strandLength, int nucleotideIndex, int[] intervals) {
		if (intervals == null || intervals.length != 2)
			return;
		int startMutation = nucleotideIndex;
		int endMutation = nucleotideIndex;
		if (getMutationDirection() < 0) {
			startMutation -= getFragmentLength();
			startMutation++;
			endMutation++;
		}
		else {
			endMutation += getFragmentLength();
		}
		if (startMutation < 0)
			startMutation = 0;
		if (endMutation > strandLength)
			endMutation = strandLength;
		intervals[0] = startMutation;
		intervals[1] = endMutation;
	}

	public synchronized void addMutationListener(MutationListener l) {
		if (l != null && !mutationListeners.contains(l)) {
			mutationListeners.addElement(l);
		}
	}

	public synchronized void removeMutationListener(MutationListener l) {
		if (l != null && mutationListeners.contains(l))
			mutationListeners.removeElement(l);
	}

	public void clearMutationListeners() {
		if (mutationListeners != null)
			mutationListeners.removeAllElements();
	}

	protected synchronized void notifyMutationListeners(MutationEvent evt) {
		if (!isEventNotificationEnabled())
			return;
		for (MutationListener l : mutationListeners)
			l.mutationOccurred(evt);
	}

	public void setMutationDirection(int direction) {
		mutationDirection = (direction < 0) ? -1 : 1;
	}

	public int getMutationDirection() {
		return mutationDirection;
	}

	public boolean isEventNotificationEnabled() {
		return eventNotificationEnabled;
	}

	public void setEventNotificationEnabled(boolean eventNotificationEnabled) {
		this.eventNotificationEnabled = eventNotificationEnabled;
	}
}