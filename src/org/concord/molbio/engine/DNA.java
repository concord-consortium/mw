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

import org.concord.molbio.event.MutationListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DNA implements Cloneable {

	public final static byte DNA_STRAND_BASE = 0;// 53
	public final static byte DNA_STRAND_COMPL = 1;// 35
	public final static byte DNA_STRAND_53 = DNA_STRAND_BASE;// 53
	public final static byte DNA_STRAND_35 = DNA_STRAND_COMPL;// 35

	private final static String END_FRAGMENT_STR = "TGANTAGNTAA";
	private final static String END_FRAGMENT_REG_STR = "TGA[A,T,C,G]TAG[A,T,C,G]TAA";
	private final static String END_FRAGMENT_REG_STR2 = END_FRAGMENT_REG_STR + "[A,T,C,G]";
	final static DNA endFragment = new DNA(END_FRAGMENT_STR, false);
	private final static String PROMOTER_FRAGMENT_STR = "TTGACANNNNNNNNNNNNNNNNNNTATAATNNNNNN";
	private final static String PROMOTER_FRAGMENT_REG_STR = "TTGACA[A,T,C,G]{18}TATAAT[A,T,C,G]{6}";
	private final static DNA promoterFragment = new DNA(PROMOTER_FRAGMENT_STR, false);
	private final static String TERMINATOR_FRAGMENT_STR = "NCCACAGGCCGCCAGTTCCGCTGGCGGCATTTT";
	private final static String TERMINATOR_FRAGMENT_REG_STR = "[A,T,C,G]CCACAGGCCGCCAGTTCCGCTGGCGGCATTTT";
	private final static DNA terminatorFragment = new DNA(TERMINATOR_FRAGMENT_STR, false);
	private final static String START_FRAGMENT_STR = "ATG";
	private final static DNA startFragment = new DNA(START_FRAGMENT_STR, false);

	public final static int PROMOTER_LENGTH = PROMOTER_FRAGMENT_STR.length();
	public final static int TERMINATOR_LENGTH = TERMINATOR_FRAGMENT_STR.length();
	public final static int START_LENGTH = START_FRAGMENT_STR.length();
	public final static int END_LENGTH = END_FRAGMENT_STR.length() + 1;

	private final static Pattern PROMOTER_PATTERN = Pattern.compile("^" + PROMOTER_FRAGMENT_REG_STR);
	private final static Pattern TERMINATOR_PATTERN = Pattern.compile(TERMINATOR_FRAGMENT_REG_STR + "$");
	private final static Pattern PROMOTER_START_PATTERN = Pattern.compile("^" + PROMOTER_FRAGMENT_REG_STR
			+ START_FRAGMENT_STR);
	private final static Pattern END_PATTERN = Pattern.compile(END_FRAGMENT_REG_STR2 + "$");
	private final static String PROMOTER_START_FRAGMENT_STR = PROMOTER_FRAGMENT_STR + START_FRAGMENT_STR;

	private Strand[] strands = new Strand[2];
	private int[] probabilities = new int[Mutator.NUMB_POSSIBLE_MUTATORS];
	private boolean stopProduceRNAonStopCodon = true;

	private boolean endFragmentExists = false;
	private boolean startFragmentExists = false;
	private boolean terminatorFragmentExists = false;
	private boolean promoterFragmentExists = false;
	private boolean needVerifyFragments = true;

	protected DNA() {
		initMixedMutatorProbabilities();
	}

	// [ACGTX ]+ ACTG GGG
	public DNA(String dna) throws IllegalArgumentException {
		this(dna, false);
	}

	public DNA(String dna, boolean needCheckFragments) throws IllegalArgumentException {
		this();
		if (dna == null)
			return;
		strands[DNA_STRAND_BASE] = new Strand();
		strands[DNA_STRAND_COMPL] = new Strand();
		strands[DNA_STRAND_53].setMutationDirection(1);
		strands[DNA_STRAND_35].setMutationDirection(-1);
		for (int i = 0; i < dna.length(); i++) {
			char n = dna.charAt(i);
			if (n == ' ')
				continue;
			if (n == 'N') {
				int nt = (int) Math.round(4 * Math.random());
				switch (nt) {
				case 0:
					n = Nucleotide.ADENINE_NAME;
					break;
				case 1:
					n = Nucleotide.GUANINE_NAME;
					break;
				case 2:
					n = Nucleotide.THYMINE_NAME;
					break;
				case 3:
					n = Nucleotide.CYTOSINE_NAME;
					break;
				default:
					n = Nucleotide.ADENINE_NAME;
					break;
				}
			}
			Nucleotide nuc = Nucleotide.getNucleotide(n);
			Nucleotide complNuc = nuc.getComplimentaryNucleotide(false);
			strands[DNA_STRAND_BASE].addNucleotide(nuc);
			strands[DNA_STRAND_COMPL].addNucleotide(complNuc);
		}
		if (needCheckFragments)
			checkForPredefinedFragments();
	}

	public static DNA createDNAFrom35Strand(String dna) {
		if (dna == null)
			return null;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < dna.length(); i++) {
			char n = dna.charAt(i);
			if (n == ' ')
				continue;
			if (n == 'N') {
				int nt = (int) Math.round(4 * Math.random());
				switch (nt) {
				case 0:
					n = Nucleotide.ADENINE_NAME;
					break;
				case 1:
					n = Nucleotide.GUANINE_NAME;
					break;
				case 2:
					n = Nucleotide.THYMINE_NAME;
					break;
				case 3:
					n = Nucleotide.CYTOSINE_NAME;
					break;
				default:
					n = Nucleotide.ADENINE_NAME;
					break;
				}
			}
			Nucleotide nuc = Nucleotide.getNucleotide(n);
			Nucleotide complNuc = nuc.getComplimentaryNucleotide(false);
			sb.append(complNuc.toString());
		}
		return new DNA(sb.toString());
	}

	public static DNA createDNAFrom53Strand(String dna) {
		return new DNA(dna);
	}

	private void initMixedMutatorProbabilities() {
		for (int i = 0; i < probabilities.length; i++)
			probabilities[i] = 1;
	}

	private synchronized void verifyFragments() {
		promoterFragmentExists = false;
		endFragmentExists = false;
		startFragmentExists = false;
		terminatorFragmentExists = false;
		String str = getFragmentAsString();
		if (str == null)
			return;
		Matcher matcher = PROMOTER_PATTERN.matcher(str);
		promoterFragmentExists = matcher.find();
		matcher = TERMINATOR_PATTERN.matcher(str);
		terminatorFragmentExists = matcher.find();
		if (!promoterFragmentExists) {
			startFragmentExists = str.startsWith(START_FRAGMENT_STR);
		}
		else {
			matcher = PROMOTER_START_PATTERN.matcher(str);
			startFragmentExists = matcher.find();
		}
		if (terminatorFragmentExists)
			str = str.substring(0, getLength() - TERMINATOR_LENGTH);
		matcher = END_PATTERN.matcher(str);
		endFragmentExists = matcher.find();
		needVerifyFragments = false;
	}

	public int getOffsetToTheCodingRegion() {
		if (startWithPromoter()) {
			return PROMOTER_LENGTH;
		}
		return 0;
	}

	public int getLengthOfTheCodingRegion() {
		int promoterOffset = getOffsetToTheCodingRegion();
		int retValue = getLength() - promoterOffset;
		if (endWithTerminator()) {
			retValue -= TERMINATOR_LENGTH;
		}
		if (hasEndFragment()) {
			retValue -= END_LENGTH;
		}
		if (retValue < 0)
			retValue = 0;
		return retValue;
	}

	public String getCodingRegionAsString() {
		int promoterOffset = getOffsetToTheCodingRegion();
		int lengthCodingRegion = getLengthOfTheCodingRegion();
		if (lengthCodingRegion < 1)
			return null;
		return getFragmentAsString(promoterOffset, promoterOffset + lengthCodingRegion, DNA_STRAND_BASE);
	}

	public boolean startWithPromoter() {
		if (needVerifyFragments) {
			verifyFragments();
		}
		return promoterFragmentExists;
	}

	public boolean endWithTerminator() {
		if (needVerifyFragments) {
			verifyFragments();
		}
		return terminatorFragmentExists;
	}

	public boolean hasStartFragment() {
		if (needVerifyFragments) {
			verifyFragments();
		}
		return startFragmentExists;
	}

	public boolean hasEndFragment() {
		if (needVerifyFragments) {
			verifyFragments();
		}
		return endFragmentExists;
	}

	private void checkForPredefinedFragments() {
		String str = getFragmentAsString();
		if (str == null)
			return;
		boolean needPromoter = !str.startsWith(PROMOTER_FRAGMENT_STR);
		boolean needStart = !str.startsWith((needPromoter) ? START_FRAGMENT_STR : PROMOTER_START_FRAGMENT_STR);

		boolean needTerminator = !str.endsWith(TERMINATOR_FRAGMENT_STR);
		Pattern pattern = null;
		if (needTerminator) {
			pattern = Pattern.compile(END_FRAGMENT_REG_STR + "$");
		}
		else {
			// pattern = Pattern.compile(END_FRAGMENT_REG_STR+TERMINATOR_FRAGMENT_STR+"$");
			pattern = Pattern.compile(END_FRAGMENT_REG_STR + TERMINATOR_FRAGMENT_REG_STR + "$");
		}
		Matcher matcher = pattern.matcher(str);
		boolean needEnd = !matcher.find();

		// System.out.println("needEnd "+needEnd+" before add start "+toString());
		if (needPromoter) {
			if (needStart)
				appendStartDNAFragment();
			// System.out.println("after add start (1) "+toString());
			appendPromoterDNAFragment();
		}
		else if (needStart) {
			appendStartDNAFragment(PROMOTER_FRAGMENT_STR.length());
		}

		// System.out.println("after add promoter "+toString());
		if (needTerminator) {
			if (needEnd)
				appendEndDNAFragment();
			// System.out.println("after add end(1) "+toString());
			appendTerminatorDNAFragment();
		}
		else if (needEnd) {
			appendEndDNAFragment(getLength() - TERMINATOR_FRAGMENT_STR.length());
			// System.out.println("after add end(2) "+toString());
		}
		// System.out.println("FINAL "+toString());

	}

	public DNA[] replicate() {
		if (strands == null || strands[DNA_STRAND_BASE] == null || strands[DNA_STRAND_COMPL] == null)
			return null;
		DNA[] dnas = new DNA[2];
		dnas[0] = strands[DNA_STRAND_BASE].replicate();
		dnas[1] = strands[DNA_STRAND_COMPL].replicate();
		return dnas;
	}

	private void appendEndDNAFragment() {
		appendEndDNAFragment(getLength());
	}

	private void appendEndDNAFragment(int offset) {
		Nucleotide randomNucleotide = Nucleotide.getRandomNucleotide();
		char randomNucleotideChar = randomNucleotide.toString().charAt(0);
		StringBuffer sb = new StringBuffer(END_FRAGMENT_STR.replace('N', randomNucleotideChar));
		sb.append(randomNucleotideChar);
		insertDNA(offset, sb.toString());
	}

	private void appendTerminatorDNAFragment() {
		Nucleotide randomNucleotide = Nucleotide.getRandomNucleotide();
		char randomNucleotideChar = randomNucleotide.toString().charAt(0);
		StringBuffer sb = new StringBuffer(TERMINATOR_FRAGMENT_STR.replace('N', randomNucleotideChar));
		insertDNA(getLength(), sb.toString());
	}

	private void appendStartDNAFragment() {
		appendStartDNAFragment(0);
	}

	private void appendStartDNAFragment(int offset) {
		insertDNA(offset, START_FRAGMENT_STR);
	}

	private void appendPromoterDNAFragment() {
		Nucleotide randomNucleotide = Nucleotide.getRandomNucleotide();
		char randomNucleotideChar = randomNucleotide.toString().charAt(0);
		StringBuffer sb = new StringBuffer(PROMOTER_FRAGMENT_STR.replace('N', randomNucleotideChar));
		insertDNA(0, sb.toString());
	}

	private void insertDNA(int index, String str) {
		if (strands == null || strands[DNA_STRAND_BASE] == null || strands[DNA_STRAND_COMPL] == null)
			return;
		if (str == null || str.length() < 1)
			return;
		if (index < 0)
			index = 0;
		if (index > getLength())
			index = getLength();
		for (int i = 0; i < str.length(); i++) {
			char n = str.charAt(i);
			Nucleotide nuc = Nucleotide.getNucleotide(n);
			Nucleotide complNuc = nuc.getComplimentaryNucleotide(false);
			strands[DNA_STRAND_BASE].addNucleotide(index, nuc);
			strands[DNA_STRAND_COMPL].addNucleotide(index, complNuc);
			index++;
		}
	}

	Strand getStrand(int index) {
		if (index == DNA_STRAND_BASE)
			return strands[DNA_STRAND_BASE];
		if (index == DNA_STRAND_COMPL)
			return strands[DNA_STRAND_COMPL];
		return null;
	}

	Strand getComplimentaryStrand(int index) {
		if (index == DNA_STRAND_BASE)
			return strands[DNA_STRAND_COMPL];
		if (index == DNA_STRAND_COMPL)
			return strands[DNA_STRAND_BASE];
		return null;
	}

	public int getLength() {
		if (strands == null || strands[DNA_STRAND_BASE] == null)
			return 0;
		return strands[DNA_STRAND_BASE].getLength();
	}

	boolean checkStrandComplementarity() {
		if (strands[DNA_STRAND_BASE] == null || strands[DNA_STRAND_COMPL] == null)
			return false;
		if (strands[DNA_STRAND_BASE].getLength() != strands[DNA_STRAND_COMPL].getLength())
			return false;
		int len = strands[DNA_STRAND_BASE].getLength();
		for (int i = 0; i < len; i++) {
			Nucleotide n1 = strands[DNA_STRAND_BASE].getNucleotide(i);
			Nucleotide n2 = strands[DNA_STRAND_COMPL].getNucleotide(i);
			if (n1 == null || n2 == null)
				return false;
			Nucleotide nc = n1.getComplimentaryNucleotide();
			if (nc != n2)
				return false;
		}
		return true;
	}

	public RNA transcript(int strandIndex) {
		return transcript(null, strandIndex, 0, getLength() - 1);
	}

	public RNA transcript(int[] indexes, int strandIndex, int startIndex, int endIndex) {
		int[] indx = indexes;
		RNA rna = null;
		int checkIndex = 0;// (startWithPromoter())?PROMOTER_LENGTH:0;
		if (startIndex < checkIndex)
			startIndex = checkIndex;
		if (endIndex < startIndex)
			return null;
		if (endIndex < 0 || endIndex >= getLength())
			endIndex = getLength();
		if (indx != null && ((indx.length % 2) == 1)) {
			indx = new int[indexes.length + 1];
			System.arraycopy(indexes, 0, indx, 0, indexes.length);
			indx[indexes.length] = getLength();
		}
		rna = new RNA();
		Strand strand = getStrand(strandIndex);
		Nucleotide[] nc = new Nucleotide[3];
		if (strandIndex == DNA_STRAND_53) {
			for (int i = startIndex; i < endIndex; i += 3) {
				boolean endStrand = false;
				for (int n = 0; n < 3; n++) {
					// endStrand = (i+n >= endIndex);
					endStrand = (i + n > endIndex);
					if (endStrand)
						break;
					nc[n] = strand.bases.elementAt(i + n);
				}
				if (endStrand)
					break;
				Codon codon = new Codon(nc[0], nc[1], nc[2]);
				if (stopProduceRNAonStopCodon && codon.getTranscripted().isCodonStop())
					break;
				for (int n = 0; n < 3; n++)
					rna.addNucleotide(nc[n].getComplimentaryNucleotide(true));
			}
		}
		else {
			for (int i = startIndex; i < endIndex; i += 3) {
				boolean endStrand = false;
				for (int n = 0; n < 3; n++) {
					endStrand = (i + n > endIndex);
					if (endStrand)
						break;
					nc[n] = strand.bases.elementAt(i + n);
				}
				if (endStrand)
					break;
				Codon codon = new Codon(nc[0], nc[1], nc[2]);
				if (stopProduceRNAonStopCodon && codon.getTranscripted().isCodonStop())
					break;
				for (int n = 0; n < 3; n++)
					rna.addNucleotide(nc[n].getComplimentaryNucleotide(true));
			}
			/*
			 * for(int i = endIndex; i > startIndex; i-=3){ boolean endStrand = false; for(int n = 0; n < 3; n++){
			 * endStrand = (i - n <= startIndex); if(endStrand) break; nc[n] = (Nucleotide)strand.bases.elementAt(i-n);
			 * } if(endStrand) break; Codon codon = new Codon(nc[0],nc[1],nc[2]); if(stopProduceRNAonStopCodon &&
			 * codon.getTranscripted().isCodonStop()) break; for(int n = 0; n < 3; n++)
			 * rna.addNucleotide(nc[n].getComplimentaryNucleotide(true)); }
			 */
		}
		if (indx != null)
			for (int i = 1; i < indx.length; i++)
				indx[i] -= startIndex;
		rna.setIndexes(indx, startIndex, endIndex);
		return rna;
	}

	void setStopProduceRNAonStopCodon(boolean val) {
		stopProduceRNAonStopCodon = val;
	}

	public String getFragmentAsString() throws IllegalArgumentException {
		return getFragmentAsString(0, getLength());
	}

	String getFragmentAsString(int startIndex, int endIndex) throws IllegalArgumentException {
		return getFragmentAsString(startIndex, endIndex, DNA_STRAND_BASE);
	}

	private String getFragmentAsString(int startIndex, int endIndex, int strandIndex) throws IllegalArgumentException {
		if (strandIndex != DNA_STRAND_BASE && strandIndex != DNA_STRAND_COMPL)
			throw new IllegalArgumentException("DNA.getFragmentAsString strandIndex isn't correct " + strandIndex);
		if (strands == null || strands[strandIndex] == null)
			return null;
		return strands[strandIndex].getFragmentAsString(startIndex, endIndex);
	}

	public String toString() {
		Strand strand = getStrand(DNA_STRAND_BASE);
		if (strand == null)
			return "";
		return strand.toString();
	}

	public static DNA getEndFragment() {
		return endFragment;
	}

	public static DNA getStartFragment() {
		return startFragment;
	}

	public static DNA getPromoterFragment() {
		return promoterFragment;
	}

	public static DNA getTerminatorFragment() {
		return terminatorFragment;
	}

	public void setMutatorToStrand(int strandType, int kind, Mutator mutator) throws IllegalArgumentException {
		if (strandType < DNA_STRAND_BASE || strandType > DNA_STRAND_COMPL) {
			throw new IllegalArgumentException("illegal Strand's type " + strandType);
		}
		if (strands[strandType] != null)
			strands[strandType].setMutator(kind, mutator);
	}

	public void addMutationListener(int strandType, MutationListener l) throws IllegalArgumentException {
		if (strandType < DNA_STRAND_BASE || strandType > DNA_STRAND_COMPL) {
			throw new IllegalArgumentException("illegal Strand's type " + strandType);
		}
		if (strands[strandType] != null)
			strands[strandType].addMutationListener(l);
	}

	public void addMutationListener(MutationListener l) throws IllegalArgumentException {
		if (strands[DNA_STRAND_BASE] != null)
			strands[DNA_STRAND_BASE].addMutationListener(l);
		if (strands[DNA_STRAND_COMPL] != null)
			strands[DNA_STRAND_COMPL].addMutationListener(l);
	}

	public void removeMutationListener(int strandType, MutationListener l) throws IllegalArgumentException {
		if (strandType < DNA_STRAND_BASE || strandType > DNA_STRAND_COMPL) {
			throw new IllegalArgumentException("illegal Strand's type " + strandType);
		}
		if (strands[strandType] != null)
			strands[strandType].removeMutationListener(l);
	}

	public void removeMutationListener(MutationListener l) throws IllegalArgumentException {
		if (strands[DNA_STRAND_BASE] != null)
			strands[DNA_STRAND_BASE].removeMutationListener(l);
		if (strands[DNA_STRAND_COMPL] != null)
			strands[DNA_STRAND_COMPL].removeMutationListener(l);
	}

	public void clearMutationListeners() {
		if (strands[DNA_STRAND_BASE] != null)
			strands[DNA_STRAND_BASE].clearMutationListeners();
		if (strands[DNA_STRAND_COMPL] != null)
			strands[DNA_STRAND_COMPL].clearMutationListeners();
	}

	public void mutate(int strandIndex, int mutatorKind, int index, Nucleotide nucleotideTo) {
		Strand strand = getStrand(strandIndex);
		if (strand == null)
			return;
		Mutator mutator = strand.getMutator(mutatorKind);
		if (mutator == null)
			return;
		if ((mutator instanceof SubstitutionMutator) || (mutator instanceof InsertionMutator)) {
			if (nucleotideTo == null)
				mutator.setMutationParam(null);
			else {
				String nucleoString = new String(new char[] { nucleotideTo.getName() });
				mutator.setMutationParam(new Object[] { nucleoString });
			}
		}
		else if (mutator instanceof MixedMutator) {
			Object[] mutatorParams = new Object[probabilities.length];
			for (int i = 0; i < mutatorParams.length; i++) {
				mutatorParams[i] = new Integer(probabilities[i]);
			}
			mutator.setMutationParam(mutatorParams);
		}
		mutator.mutate(this, strandIndex, index);
		mutator.setMutationParam(null);
		verifyFragments();
	}

	void setProbabilitiesForMixedMutator(int ident, int subst, int insert, int delet) {
		probabilities[Mutator.MUTATOR_IDENTITY] = ident;
		probabilities[Mutator.MUTATOR_SUBSTITUTION] = subst;
		probabilities[Mutator.MUTATOR_INSERTION] = insert;
		probabilities[Mutator.MUTATOR_DELETION] = delet;
	}

}