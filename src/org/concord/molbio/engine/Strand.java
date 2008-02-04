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

import java.util.Vector;

import org.concord.molbio.event.MutationListener;

public class Strand {

	Mutator[] mutators = new Mutator[Mutator.MUTATOR_MIXED + 1];
	Vector<Nucleotide> bases = new Vector<Nucleotide>();
	boolean mutatorWasCreated = false;

	public Strand() {
		createDefaultMutators();
	}

	protected Strand(String str) throws IllegalArgumentException {
		if (str == null || str.length() < 1)
			return;
		for (int i = 0; i < str.length(); i++) {
			addNucleotide(str.charAt(i));
		}
	}

	protected Strand getComplimentaryStrand() {
		Strand strand = new Strand();
		if (bases == null)
			return strand;
		String str = toString();
		for (int i = 0; i < str.length(); i++) {
			char n = str.charAt(i);
			Nucleotide nuc = Nucleotide.getNucleotide(n);
			Nucleotide complNuc = nuc.getComplimentaryNucleotide(false);
			strand.addNucleotide(complNuc);
		}
		return strand;
	}

	protected DNA replicate() {
		if (bases == null)
			return new DNA();
		return new DNA(toString());
	}

	public void removeNucleotide(int index) {
		if (bases == null || bases.size() < 1)
			return;
		if (index < 0)
			index = 0;
		if (index >= bases.size())
			index = bases.size() - 1;
		bases.removeElementAt(index);
	}

	public Nucleotide getNucleotide(int index) {
		if (index < 0 || index >= getLength())
			return null;
		return bases.elementAt(index);
	}

	public void addNucleotide(char b) throws IllegalArgumentException {
		addNucleotide(Nucleotide.getNucleotide(b));
	}

	protected void addNucleotide(Nucleotide b) throws IllegalArgumentException {
		if (b.getName() == Nucleotide.URACIL_NAME)
			throw new IllegalArgumentException("Uracil could not be added to the DNA strand");
		addNucleotide0(b);
	}

	protected void addRandomNucleotide() {
		double dr = 4 * Math.random();
		Nucleotide nn = null;
		if (dr >= 0 && dr < 1)
			nn = Nucleotide.ADENINE;
		else if (dr >= 1 && dr < 2)
			nn = Nucleotide.GUANINE;
		else if (dr >= 2 && dr < 3)
			nn = Nucleotide.CYTOSINE;
		else if (dr >= 3 && dr < 4)
			nn = Nucleotide.THYMINE;
		addNucleotide(nn);
	}

	protected Nucleotide getRandomNucleotide(Nucleotide n) {
		if (n == null)
			return null;
		Nucleotide[] candidates = new Nucleotide[3];
		if (n == Nucleotide.ADENINE) {
			candidates[0] = Nucleotide.GUANINE;
			candidates[1] = Nucleotide.CYTOSINE;
			candidates[2] = Nucleotide.THYMINE;
		}
		else if (n == Nucleotide.GUANINE) {
			candidates[0] = Nucleotide.ADENINE;
			candidates[1] = Nucleotide.CYTOSINE;
			candidates[2] = Nucleotide.THYMINE;
		}
		else if (n == Nucleotide.CYTOSINE) {
			candidates[0] = Nucleotide.ADENINE;
			candidates[1] = Nucleotide.GUANINE;
			candidates[2] = Nucleotide.THYMINE;
		}
		else if (n == Nucleotide.THYMINE) {
			candidates[0] = Nucleotide.ADENINE;
			candidates[1] = Nucleotide.GUANINE;
			candidates[2] = Nucleotide.CYTOSINE;
		}
		double dr = 3 * Math.random();
		Nucleotide nn = null;
		if (dr >= 0 && dr < 1)
			nn = candidates[0];
		else if (dr >= 1 && dr < 2)
			nn = candidates[1];
		else if (dr >= 2 && dr < 3)
			nn = candidates[2];
		return nn;
	}

	public Nucleotide substituteNucleotide(int index, Nucleotide nucleo) {
		if (nucleo == null)
			return null;
		if (nucleo == Nucleotide.URACIL)
			return null;
		bases.removeElementAt(index);
		bases.insertElementAt(nucleo, index);
		return nucleo;
	}

	public Nucleotide substituteNucleotideRandomly(int index) {
		Nucleotide n = getNucleotide(index);
		if (n == null)
			return null;
		Nucleotide nn = getRandomNucleotide(n);
		if (nn == null)
			return null;
		bases.removeElementAt(index);
		bases.insertElementAt(nn, index);
		return nn;
	}

	protected void addNucleotide0(Nucleotide b) throws IllegalArgumentException {
		bases.addElement(b);
	}

	public void addNucleotide(int index, char b) throws IllegalArgumentException {
		addNucleotide(index, Nucleotide.getNucleotide(b));
	}

	protected void addNucleotide(int index, Nucleotide b) throws IllegalArgumentException {
		if (b.getName() == Nucleotide.URACIL_NAME)
			throw new IllegalArgumentException("Uracil could not be added to the DNA strand");
		addNucleotide0(index, b);
	}

	protected void addNucleotide0(int index, Nucleotide b) throws IllegalArgumentException {
		bases.insertElementAt(b, index);
	}

	public int getLength() {
		if (bases == null)
			return 0;
		return bases.size();
	}

	protected String getFragmentAsString(int startIndex, int endIndex) throws IllegalArgumentException {
		if (bases == null || bases.size() < 1)
			return null;
		if (startIndex > endIndex)
			throw new IllegalArgumentException(
					"Strand.getFragmentAsString: startIndex couldn't be bigger than  endIndex");
		if (startIndex < 0)
			throw new IllegalArgumentException("Strand.getFragmentAsString: startIndex couldn't be negative");
		if (endIndex > bases.size())
			throw new IllegalArgumentException(
					"Strand.getFragmentAsString: endIndex couldn't be bigger than bases size");
		String str = "";
		for (int i = startIndex; i < endIndex; i++) {
			str += bases.elementAt(i).getName();
		}
		return str;
	}

	public Strand concatenate(Strand strand) {
		if (strand == null)
			return this;
		Vector b = strand.bases;
		if (b == null || b.size() < 1)
			return this;
		for (int i = 0; i < b.size(); i++) {
			addNucleotide((Nucleotide) b.elementAt(i));
		}
		return this;
	}

	public static Strand generateRandomStrand(int len) {
		if (len < 1)
			return null;
		Strand strand = new Strand();
		for (int i = 0; i < len; i++) {
			strand.addRandomNucleotide();
		}
		return strand;
	}

	public String toString() {
		String str = "";
		if (bases == null || bases.size() < 1)
			return str;
		try {
			str = getFragmentAsString(0, bases.size());
		}
		catch (IllegalArgumentException e) {
		}
		return str;
	}

	public synchronized void setMutator(int kind, Mutator mutator) throws IllegalArgumentException {
		if (!mutatorWasCreated)
			return;
		if (kind < 0 || kind >= mutators.length) {
			throw new IllegalArgumentException("illegal Mutator's kind " + kind);
		}
		mutators[kind] = mutator;
	}

	public synchronized void addMutationListener(MutationListener l) {
		for (int i = 0; i < mutators.length; i++) {
			if (mutators[i] != null)
				mutators[i].addMutationListener(l);
		}
	}

	public synchronized void removeMutationListener(MutationListener l) {
		for (int i = 0; i < mutators.length; i++) {
			if (mutators[i] != null)
				mutators[i].removeMutationListener(l);
		}
	}

	public void clearMutationListeners() {
		for (int i = 0; i < mutators.length; i++) {
			if (mutators[i] != null)
				mutators[i].clearMutationListeners();
		}
	}

	public synchronized Mutator getMutator(int mutatorKind) {
		if (mutators == null || mutatorKind < 0 || mutatorKind >= mutators.length)
			return null;
		return mutators[mutatorKind];
	}

	public void setMutationDirection(int direction) {
		if (direction == 0 || mutators == null)
			return;
		int dir = (direction < 0) ? -1 : 1;
		for (int i = 0; i < mutators.length; i++) {
			if (mutators[i] != null)
				mutators[i].setMutationDirection(dir);
		}
	}

	synchronized void createDefaultMutators() {
		if (mutatorWasCreated)
			return;
		mutators[Mutator.MUTATOR_IDENTITY] = MutatorFactory.getIdentityMutator();
		mutators[Mutator.MUTATOR_SUBSTITUTION] = MutatorFactory.getSubstitutionMutator();
		mutators[Mutator.MUTATOR_DELETION] = MutatorFactory.getDeletionMutator();
		mutators[Mutator.MUTATOR_INSERTION] = MutatorFactory.getInsertionMutator();
		mutators[Mutator.MUTATOR_MIXED] = MutatorFactory.getMixedMutator();
		for (int i = 0; i < mutators.length; i++) {
			if (mutators[i] == null)
				return;
		}
		mutatorWasCreated = true;
	}

}