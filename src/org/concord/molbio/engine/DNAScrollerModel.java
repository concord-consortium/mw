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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.concord.molbio.event.MutationEvent;
import org.concord.molbio.event.MutationListener;
import org.concord.molbio.event.DNAHistoryEvent;
import org.concord.molbio.event.DNAHistoryListener;
import java.util.*;

public class DNAScrollerModel implements MutationListener {

	public static final int DNA_STRAND_AVAILABLE_NONE = -1;
	public static final int DNA_STRAND_AVAILABLE_53 = 0;
	public static final int DNA_STRAND_AVAILABLE_35 = 1;
	public static final int DNA_STRAND_AVAILABLE_BOTH = 2;

	public static final int RNA_STRAND_AVAILABLE_NONE = -1;
	public static final int RNA_STRAND_AVAILABLE = 0;

	private int startWindowIndex = 0;
	private int currIndex = 0;
	private String dnaString53;
	private String dnaString35;
	private String rnaString;

	private DNA dna;
	private RNA rna;

	private Codon[] codons53;
	private Codon[] codons35;
	private int nbaseinwindow;
	private PropertyChangeSupport changeSupport;
	private boolean unfinishedCodon = false;

	private int strandAvailability = DNA_STRAND_AVAILABLE_BOTH;
	private String originalDNAString;
	private int runNumber = 0;
	private Stack<DNA> historyStack;
	private Vector<DNAHistoryListener> historyListeners;
	private DNAHistoryEvent modifiedHistoryEvent = null;
	private DNAHistoryEvent clearedHistoryEvent = null;

	public DNAScrollerModel(DNA dna) {
		setDNA(dna);
		modifiedHistoryEvent = new DNAHistoryEvent(this, DNAHistoryEvent.HISTORY_MODIFIED_EVENT);
		clearedHistoryEvent = new DNAHistoryEvent(this, DNAHistoryEvent.HISTORY_CLEARED_EVENT);
		initHistoryStack();
	}

	public void resetDNA() {
		reinit();
	}

	public void setDNA(DNA dna) {
		if (this.dna == null && dna == null)
			return;
		if (this.dna != null && this.dna.equals(dna))
			return;
		if (this.dna != null)
			this.dna.removeMutationListener(this);
		this.dna = dna;
		reinit();
		if (this.dna != null) {
			this.dna.addMutationListener(this);
			originalDNAString = dna.getFragmentAsString();
		}
	}

	public void addDNAHistoryListener(DNAHistoryListener l) {
		if (l == null)
			return;
		if (historyListeners == null)
			historyListeners = new Vector<DNAHistoryListener>();
		if (!historyListeners.contains(l))
			historyListeners.add(l);
	}

	public void removeDNAHistoryListener(DNAHistoryListener l) {
		if (l == null || historyListeners == null)
			return;
		if (historyListeners.contains(l))
			historyListeners.remove(l);
	}

	protected void notifyHistoryListeners(DNAHistoryEvent evt) {
		if (historyListeners == null || historyListeners.size() < 1)
			return;
		for (DNAHistoryListener l : historyListeners)
			l.historyChanged(evt);
	}

	private void pushIntoHistoryStack(DNA lDNA) {
		pushIntoHistoryStack(lDNA, true);
	}

	private void pushIntoHistoryStack(DNA lDNA, boolean notifyListeners) {
		if (lDNA != null) {
			historyStack.push(new DNA(lDNA.getFragmentAsString(), false));
			if (notifyListeners)
				notifyHistoryListeners(modifiedHistoryEvent);
		}
	}

	private void initHistoryStack() {
		if (historyStack == null) {
			historyStack = new Stack<DNA>();
		}
		else {
			historyStack.clear();
		}
		pushIntoHistoryStack(dna, false);
	}

	public void restoreToOriginalDNA() {
		if (originalDNAString == null)
			return;
		setDNA(new DNA(originalDNAString));
		runNumber = 0;
		initHistoryStack();
		notifyHistoryListeners(clearedHistoryEvent);
	}

	public Stack getHistoryStack() {// safe
		if (historyStack == null) {
			historyStack = new Stack<DNA>();
			pushIntoHistoryStack(dna, false);
		}
		else if (historyStack.size() < 1 && dna != null) {
			pushIntoHistoryStack(dna, false);
		}
		return historyStack;
	}

	public DNA getDNA() {
		return dna;
	}

	public RNA getRNA() {
		return rna;
	}

	private void reinit() {
		rna = (dna != null) ? dna.transcript(DNA.DNA_STRAND_COMPL) : null;
		createDNAStrings();
		createCodons();
	}

	public void setStopProduceRNAonStopCodon(boolean val) {
		if (dna != null) {
			dna.setStopProduceRNAonStopCodon(val);
			reinit();
		}
	}

	public int getCodonsNumber() {
		return (codons53 == null) ? 0 : codons53.length;
	}

	public int getNBaseInWindow() {
		return (dna == null) ? 0 : nbaseinwindow;
	}

	public void setNBaseInWindow(int nbaseinwindow) {
		this.nbaseinwindow = nbaseinwindow;
	}

	public int getStartWindowIndex() {
		return dna == null ? 0 : startWindowIndex;
	}

	public void setStartWindowIndex(int startWindowIndex) {
		this.startWindowIndex = startWindowIndex;
		checkStartWindowIndex();
		createDNAStrings();
	}

	private void checkStartWindowIndex() {
		if (dna == null)
			return;
		if (startWindowIndex < 0)
			startWindowIndex = 0;
		if (startWindowIndex > dna.getLength() - nbaseinwindow)
			startWindowIndex = 3 * ((dna.getLength() - nbaseinwindow) / 3);
	}

	public void setStartIndexToCurrent() {
		setStartWindowIndex(currIndex);
	}

	public int getCurrIndex() {
		return currIndex;
	}

	public void setCurrIndex(int currIndex) {
		this.currIndex = currIndex;
		checkCurrIndex();
	}

	public void setCurrIndexFromOffset(int offsCurrIndex) {
		setCurrIndex(startWindowIndex + offsCurrIndex);
	}

	public boolean setCurrIndexToCodonStartFromOffset(int offset) {
		if (dna == null)
			return false;
		int dnaLength = dna.getLength();
		int temp = dnaLength % 3;
		if ((temp != 0) && (dnaLength - startWindowIndex - offset <= temp))
			return false;
		setCurrIndex(3 * ((startWindowIndex + offset) / 3));
		return true;
	}

	private void checkCurrIndex() {
		if (dna == null)
			return;
		if (currIndex > dna.getLength() - 3)
			currIndex = dna.getLength() - 3;
		if (currIndex < 0)
			currIndex = 0;
	}

	public int getDNALength() {
		return dna == null ? 0 : dna.getLength();
	}

	private void createCodons() {
		if (dna == null) {
			codons53 = null;
			codons35 = null;
			return;
		}
		int ncodons = dna.getLength() / 3;
		codons53 = new Codon[ncodons];
		codons35 = new Codon[ncodons];
		String s53 = dna.getStrand(DNA.DNA_STRAND_BASE).toString();
		String s35 = dna.getComplimentaryStrand(DNA.DNA_STRAND_BASE).toString();
		for (int i = 0; i < dna.getLength(); i += 3) {
			if (i > dna.getLength() - 2 || i > dna.getLength() - 3)
				continue;
			codons53[i / 3] = Codon.getCodon(s53.charAt(i), s53.charAt(i + 1), s53.charAt(i + 2))/* .getTranscripted() */;
			codons35[i / 3] = Codon.getCodon(s35.charAt(i + 2), s35.charAt(i + 1), s35.charAt(i))/* .getTranscripted() */;
		}
		unfinishedCodon = ((dna.getLength() % 3) != 0);
	}

	public boolean isUnfinishedCodon() {
		return unfinishedCodon;
	}

	public Codon get53CodonFromOffset(int offset) {
		return get53Codon(startWindowIndex + offset);
	}

	public Codon get35CodonFromOffset(int offset) {
		return get35Codon(startWindowIndex + offset);
	}

	public String get53ToolTipString(int offset) {
		return get53ToolTipString(offset, false);
	}

	public String get53ToolTipString(int offset, boolean reverse) {
		if (dnaString53 == null)
			return "";
		int roundOffset = 3 * (offset / 3);
		if (roundOffset + 1 >= dnaString53.length())
			return null;
		if (roundOffset + 2 >= dnaString53.length())
			return null;

		StringBuffer sb = new StringBuffer();
		sb.append(dnaString53.charAt((reverse) ? roundOffset + 2 : roundOffset));
		sb.append(dnaString53.charAt(roundOffset + 1));
		sb.append(dnaString53.charAt((reverse) ? roundOffset : roundOffset + 2));
		return sb.toString();
	}

	public String get35ToolTipString(int offset) {
		return get35ToolTipString(offset, false);
	}

	public String get35ToolTipString(int offset, boolean reverse) {
		if (dnaString35 == null)
			return "";
		int roundOffset = 3 * (offset / 3);
		if (roundOffset + 1 >= dnaString35.length())
			return null;
		if (roundOffset + 2 >= dnaString35.length())
			return null;
		StringBuffer sb = new StringBuffer();
		sb.append(dnaString35.charAt((reverse) ? roundOffset : roundOffset + 2));
		sb.append(dnaString35.charAt(roundOffset + 1));
		sb.append(dnaString35.charAt((reverse) ? roundOffset + 2 : roundOffset));
		return sb.toString();
	}

	public Codon get53Codon(int index) {
		if (codons53 == null || index < 0 || (index / 3) >= codons53.length)
			return null;
		return codons53[index / 3];
	}

	public Codon get35Codon(int index) {
		if (codons35 == null || index < 0 || (index / 3) >= codons35.length)
			return null;
		return codons35[index / 3];
	}

	private void createDNAStrings() {
		if (dna == null) {
			dnaString53 = "";
			dnaString35 = "";
			rnaString = "";
			return;
		}
		Strand s53 = dna.getStrand(DNA.DNA_STRAND_BASE);
		Strand s35 = dna.getComplimentaryStrand(DNA.DNA_STRAND_BASE);
		dnaString53 = s53.toString().substring(startWindowIndex);
		dnaString35 = s35.toString().substring(startWindowIndex);
		rnaString = (rna != null && (startWindowIndex < rna.getLength())) ? rna.toString().substring(startWindowIndex)
				: "";
	}

	public String getDNA53String() {
		return dnaString53;
	}

	public String getDNA35String() {
		return dnaString35;
	}

	public String getRNAString() {
		return rnaString;
	}

	public String getFullRNAString() {
		if (rna == null)
			return null;
		return rna.toString();
	}

	public String getFullDNA53String() {
		if (dna == null)
			return null;
		Strand strand = dna.getStrand(DNA.DNA_STRAND_BASE);
		if (strand == null)
			return null;
		return strand.toString();
	}

	public String getFullDNA35String() {
		if (dna == null)
			return null;
		Strand strand = dna.getStrand(DNA.DNA_STRAND_COMPL);
		if (strand == null)
			return null;
		return strand.toString();
	}

	public char[] get53Chars() {
		return (dnaString53 == null) ? null : dnaString53.toCharArray();
	}

	public char[] get35Chars() {
		return (dnaString35 == null) ? null : dnaString35.toCharArray();
	}

	public char[] getRNAChars() {
		return (rnaString == null) ? null : rnaString.toCharArray();
	}

	public int get53StrandLengthFromCurrIndex() {
		return (dnaString53 == null) ? 0 : dnaString53.length();
	}

	public int get35StrandLengthFromCurrIndex() {
		return (dnaString35 == null) ? 0 : dnaString35.length();
	}

	public int getRNALengthFromCurrIndex() {
		return (rnaString == null) ? 0 : rnaString.length();
	}

	public boolean isStrand53Available() {
		return (strandAvailability == DNA_STRAND_AVAILABLE_BOTH || strandAvailability == DNA_STRAND_AVAILABLE_53);
	}

	public boolean isStrand35Available() {
		return (strandAvailability == DNA_STRAND_AVAILABLE_BOTH || strandAvailability == DNA_STRAND_AVAILABLE_35);
	}

	public boolean isStrandBothAvailable() {
		return (strandAvailability == DNA_STRAND_AVAILABLE_BOTH);
	}

	public boolean isStrandsAvailable() {
		return (strandAvailability != DNA_STRAND_AVAILABLE_NONE);
	}

	public void setStrandAvailability(int value) throws IllegalArgumentException {
		if (value < DNA_STRAND_AVAILABLE_NONE || value > DNA_STRAND_AVAILABLE_BOTH) {
			throw new IllegalArgumentException("wrong strand availability value " + value);
		}
		if (strandAvailability == value)
			return;
		int oldValue = strandAvailability;
		strandAvailability = value;
		if (changeSupport != null) {
			changeSupport.firePropertyChange("strandAvailability", oldValue, strandAvailability);
		}
	}

	public void mutationOccurred(MutationEvent evt) {
		reinit();
		if (changeSupport != null) {
			changeSupport.firePropertyChange("wasMutation", null, evt.getSource());
		}
	}

	public int getStrandAvailability() {
		return strandAvailability;
	}

	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (changeSupport == null)
			changeSupport = new PropertyChangeSupport(this);
		changeSupport.addPropertyChangeListener(l);
	}

	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (changeSupport != null) {
			changeSupport.removePropertyChangeListener(l);
		}
	}

	public void setMutatorToStrand(int strandType, int kind, Mutator mutator) throws IllegalArgumentException {
		if (dna != null)
			dna.setMutatorToStrand(strandType, kind, mutator);
	}

	public void addMutationListener(int strandType, MutationListener l) throws IllegalArgumentException {
		if (dna != null)
			dna.addMutationListener(strandType, l);
	}

	public void addMutationListener(MutationListener l) throws IllegalArgumentException {
		if (dna != null)
			dna.addMutationListener(l);
	}

	public void removeMutationListener(int strandType, MutationListener l) throws IllegalArgumentException {
		if (dna != null)
			dna.removeMutationListener(strandType, l);
	}

	public void removeMutationListener(MutationListener l) throws IllegalArgumentException {
		if (dna != null)
			dna.removeMutationListener(l);
	}

	public void clearMutationListeners() {
		if (dna != null)
			dna.clearMutationListeners();
	}

	public void mutate(int strand, int mutatorKind, int index, Nucleotide nucleotideTo) {
		if (dna != null)
			dna.mutate(strand, mutatorKind, index, nucleotideTo);
		if (rna != null) {
			if (mutatorKind == Mutator.MUTATOR_IDENTITY)
				return;
		}
	}

	public void setProbabilitiesForMixedMutator(int ident, int subst, int insert, int delet) {
		if (dna != null)
			dna.setProbabilitiesForMixedMutator(ident, subst, insert, delet);
	}

	public Protein expressFromStrand(int strandType) {
		DNA needDNA = dna;
		if (needDNA == null)
			return null;
		if (needDNA.hasStartFragment()) {
			String dnaStr = needDNA.getFragmentAsString().substring(DNA.PROMOTER_LENGTH);
			needDNA = new DNA(dnaStr, false);
		}
		Gene g = new Gene(needDNA, strandType);
		RNA rna = g.transcript();

		return rna.translate();
	}

	public void setNucleotide(int strandIndex, int nucleotideIndex, Nucleotide nucleotide) {
		if (dna == null)
			return;
		Strand strand = dna.getStrand(strandIndex);
		Mutator mutator = strand.getMutator(Mutator.MUTATOR_SUBSTITUTION);
		boolean notificationEnabled = mutator.isEventNotificationEnabled();
		mutator.setEventNotificationEnabled(false);
		mutate(strandIndex, Mutator.MUTATOR_SUBSTITUTION, nucleotideIndex, nucleotide);
		reinit();
		mutator.setEventNotificationEnabled(notificationEnabled);
	}

	public void mutateWithDeletionInsertion(float ratioToMutate) {
		if (runNumber < 0)
			return;
		if (ratioToMutate < 0)
			return;
		if (ratioToMutate > 1)
			ratioToMutate = 1;
		if (dna == null)
			return;
		runNumber++;
		boolean doMutationDeletionInsertion = (Math.random() < ratioToMutate);
		if (!doMutationDeletionInsertion)
			return;
		dna.setProbabilitiesForMixedMutator(0, 0, 50, 50);
		int startFragmentyIndex = 0;
		if (dna.startWithPromoter())
			startFragmentyIndex += DNA.PROMOTER_LENGTH;
		if (dna.hasStartFragment())
			startFragmentyIndex += DNA.START_LENGTH;
		int endFragmentIndex = dna.getLength() - 1;// last nucleotide included
		if (dna.endWithTerminator())
			endFragmentIndex -= DNA.TERMINATOR_LENGTH;
		if (dna.hasEndFragment())
			endFragmentIndex -= DNA.END_LENGTH;
		int fullLength = endFragmentIndex + 1 - startFragmentyIndex;
		int nucleotideIndex = startFragmentyIndex + (int) Math.round(fullLength * Math.random());
		if (nucleotideIndex >= endFragmentIndex)
			nucleotideIndex = endFragmentIndex;
		Strand strand = dna.getStrand(DNA.DNA_STRAND_BASE);
		Mutator mutator = strand.getMutator(Mutator.MUTATOR_MIXED);
		boolean notificationEnabled = mutator.isEventNotificationEnabled();
		mutator.setEventNotificationEnabled(false);
		dna.mutate(DNA.DNA_STRAND_BASE, Mutator.MUTATOR_MIXED, nucleotideIndex, null);
		reinit();
		mutator.setEventNotificationEnabled(notificationEnabled);
		runNumber = -1;
		pushIntoHistoryStack(dna);
	}

	public void mutateWithSubstitution(float ratioToMutate) {
		if (ratioToMutate < 0)
			return;
		if (ratioToMutate > 1)
			ratioToMutate = 1;
		if (dna == null)
			return;
		int startFragmentyIndex = 0;
		if (dna.startWithPromoter())
			startFragmentyIndex += DNA.PROMOTER_LENGTH;
		if (dna.hasStartFragment())
			startFragmentyIndex += DNA.START_LENGTH;
		int endFragmentIndex = dna.getLength() - 1;// last nucleotide included
		if (dna.endWithTerminator())
			endFragmentIndex -= DNA.TERMINATOR_LENGTH;
		if (dna.hasEndFragment())
			endFragmentIndex -= DNA.END_LENGTH;
		int fullLength = endFragmentIndex + 1 - startFragmentyIndex;
		int needMutate = (int) Math.round((double) fullLength * ratioToMutate);
		if (needMutate < 1)
			return;

		boolean[] indexesMask = new boolean[fullLength];
		int[] indexes = new int[needMutate];
		for (int i = 0; i < indexes.length; i++)
			indexes[i] = -1;
		int mutated = 0;
		while (mutated < needMutate) {
			boolean doExit = false;
			while (!doExit) {
				int index = (int) Math.round((fullLength - 1) * Math.random());
				if (!indexesMask[index]) {
					indexesMask[index] = true;
					indexes[mutated] = startFragmentyIndex + index;
					doExit = true;
				}
			}
			mutated++;
		}

		Strand strand = dna.getStrand(DNA.DNA_STRAND_BASE);
		Mutator mutator = strand.getMutator(Mutator.MUTATOR_SUBSTITUTION);
		boolean notificationEnabled = mutator.isEventNotificationEnabled();
		mutator.setEventNotificationEnabled(false);
		for (int i = 0; i < indexes.length; i++) {
			if (indexes[i] < 0)
				continue;
			dna.mutate(DNA.DNA_STRAND_BASE, Mutator.MUTATOR_SUBSTITUTION, indexes[i], null);
		}
		reinit();
		mutator.setEventNotificationEnabled(notificationEnabled);

		pushIntoHistoryStack(dna);

	}

}
