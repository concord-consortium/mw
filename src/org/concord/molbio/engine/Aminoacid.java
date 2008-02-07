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

import java.awt.Color;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class Aminoacid {

	public static final int MWEIGHT_AMINO_PARAM = 0;
	public static final int CHARGE_AMINO_PARAM = 1;
	public static final int PHOB_RB_AMINO_PARAM = 2;
	public static final int PK_AMINO_PARAM = 3;
	public static final int SURFACE_AMINO_PARAM = 4;
	public static final int VOLUME_AMINO_PARAM = 5;
	public static final int SOLUBILITY_AMINO_PARAM = 6;
	public static final int PHOB_AMINO_PARAM = 7;
	public static final int NUMB_AMINO_PARAM = 8;

	private static Aminoacid[] allAminoacids;
	private static AminoacidBundle bundle = new AminoacidBundle();
	private static final float MAX_USAGE_FOR_COLOR = 0.040f;

	private String abbreviation;
	private String standardAbbreviation;
	private String name;
	private char symbol;
	private String property;
	private float[] params = new float[NUMB_AMINO_PARAM];
	private Map<Object, Object> properties;
	private Map<String, Float> charges;

	protected Aminoacid(String name, String abbreviation, char symbol, float[] params, String property) {
		this.name = name;
		this.property = property;
		this.abbreviation = abbreviation;
		this.symbol = symbol;
		standardAbbreviation = createStandardAbbreviation(abbreviation);
		if (params != null) {
			int extLength = params.length;
			int minLength = Math.min(extLength, NUMB_AMINO_PARAM);
			System.arraycopy(params, 0, this.params, 0, minLength);
		}
	}

	protected void loadCharges() {
		String resourceName = "/org/concord/molbio/data/amino/charges/" + abbreviation.toUpperCase() + "_CH.properties";
		Properties p = new Properties();
		try {
			p.load(getClass().getResourceAsStream(resourceName));
		}
		catch (Throwable t) {
		}
		charges = new HashMap<String, Float>();
		Iterator it = p.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			float charge = 0f;
			try {
				charge = Float.parseFloat((String) p.get(key));
			}
			catch (Throwable t) {
				charge = 0f;
			}
			charges.put(key.trim(), charge);
		}
	}

	public float getChargeAtom(String pdbAtomName) {
		if (charges == null)
			loadCharges();
		Float c = charges.get(pdbAtomName.trim());
		if (c == null)
			return 0;
		return c.floatValue();
	}

	public float getParam(int kind) {
		if (kind < 0 || kind >= NUMB_AMINO_PARAM)
			return 0;
		return params[kind];
	}

	public float getMolWeight() {
		return getParam(MWEIGHT_AMINO_PARAM);
	}

	public float getCharge() {
		return getParam(CHARGE_AMINO_PARAM);
	}

	public float getPhob() {
		return getParam(PHOB_RB_AMINO_PARAM);
	}

	public float getPK() {
		return getParam(PK_AMINO_PARAM);
	}

	public float getVolume() {
		return getParam(VOLUME_AMINO_PARAM);
	}

	public float getSurface() {
		return getParam(SURFACE_AMINO_PARAM);
	}

	public float getSolubility() {
		return getParam(SOLUBILITY_AMINO_PARAM);
	}

	public float getHydrophobicity() {
		return getParam(PHOB_AMINO_PARAM);
	}

	public static Aminoacid getBySymbol(char c) {
		return aminoacidsSymb.get(Character.toUpperCase(c));
	}

	public static Aminoacid getByAbbreviation(String abbreviation) {
		if (abbreviation == null)
			return null;
		return aminoacidsAbbr.get(abbreviation.toUpperCase());
	}

	public static Aminoacid getByName(String name) {
		if (name == null)
			return null;
		return aminoacidsName.get(name.toLowerCase());
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Aminoacid))
			return false;
		if (name == null || ((Aminoacid) obj).name == null)
			return false;
		if (obj == this)
			return true;
		return name.equalsIgnoreCase(((Aminoacid) obj).name);
	}

	public Vector getDNACodons() {
		return getDNACodons(EXPRESS_FROM_35DNA_STRAND);
	}

	public Vector<String> getDNACodons(int expressStyle) {
		Vector<String> v = new Vector<String>();
		for (Enumeration e = aminoCreation.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			if (this == (aminoCreation.get(key)).amino) {
				String s = (String) key;
				if (s.length() != 3)
					continue;
				String cds = convertFromRNAStyleCodon(s, expressStyle);
				if (cds == null)
					continue;
				v.addElement(cds);
			}
		}
		return v;
	}

	// possible expressStyle values
	public final static int EXPRESS_FROM_53DNA_STRAND = 0;
	public final static int EXPRESS_FROM_35DNA_STRAND = 1;
	public final static int EXPRESS_FROM_RNA = 2;

	public final static int EXPRESS_TO_53DNA_STRAND = 0;
	public final static int EXPRESS_TO_35DNA_STRAND = 1;
	public final static int EXPRESS_TO_RNA = 2;

	public String getCodon(int expressStyle) throws IllegalArgumentException {
		/*
		 * if(expressStyle < EXPRESS_FROM_53DNA_STRAND || expressStyle > EXPRESS_FROM_RNA) throw new
		 * IllegalArgumentException("Aminoacid.express: expressStyle should be >= 0 and <= 2"); if(expressStyle ==
		 * EXPRESS_FROM_RNA) return getCodon(true); String codonStr = getCodon(false); if(expressStyle ==
		 * EXPRESS_FROM_35DNA_STRAND) getCodon(false); return
		 * getCodon(true).replace(Nucleotide.URACIL_NAME,Nucleotide.THYMINE_NAME);
		 */
		if (expressStyle < EXPRESS_FROM_53DNA_STRAND || expressStyle > EXPRESS_FROM_RNA)
			throw new IllegalArgumentException("Aminoacid.express: expressStyle should be >= 0 and <= 2");
		String codonStr = getCodon(true);
		return convertFromRNAStyleCodon(codonStr, expressStyle);
	}

	public String getRNACodon() throws IllegalArgumentException {
		return getCodon(EXPRESS_FROM_RNA);
	}

	public String getDNA53Codon() throws IllegalArgumentException {
		return getCodon(EXPRESS_FROM_53DNA_STRAND);
	}

	public String getDNA35Codon() throws IllegalArgumentException {
		return getCodon(EXPRESS_FROM_35DNA_STRAND);
	}

	public String getCodonRandom() {
		return getCodonRandom(EXPRESS_TO_RNA);
	}

	public String getCodonRandom(int expressStyle) {
		Vector codons = getDNACodons();
		float[] probabilities = new float[codons.size()];
		float s = 0;
		for (int i = 0; i < codons.size(); i++) {
			String codonStr = convertToRNAStyleCodon((String) codons.elementAt(i), EXPRESS_FROM_35DNA_STRAND);
			probabilities[i] = getUsageForCodon(codonStr) * 1000;
			s += probabilities[i];
		}
		for (int i = 0; i < probabilities.length; i++)
			probabilities[i] /= s;
		float[] limits = new float[probabilities.length + 1];
		limits[0] = 0;
		for (int i = 1; i < probabilities.length; i++)
			limits[i] = limits[i - 1] + probabilities[i - 1];
		limits[probabilities.length] = 1;
		float newrandom = (float) Math.random();
		String newCodonString = null;
		for (int i = 0; i < limits.length - 1; i++) {
			if (newrandom >= limits[i] && newrandom < limits[i + 1]) {
				newCodonString = convertToRNAStyleCodon((String) codons.elementAt(i), EXPRESS_FROM_35DNA_STRAND);
				break;
			}
		}
		if (newCodonString == null)
			newCodonString = getCodon();
		return convertFromRNAStyleCodon(newCodonString, expressStyle);
	}

	public String getCodon() {
		return getCodon(true);
	}

	public String getCodon(boolean rnaStyle) {
		String retValue = null;
		java.util.Collection v = getDNACodons();
		if (v != null) {
			java.util.Iterator it = v.iterator();
			float usage = -1;
			while (it.hasNext()) {
				Codon codon = new Codon((String) it.next());
				Nucleotide[] bases = codon.bases;
				StringBuffer sb = new StringBuffer();
				StringBuffer sb1 = new StringBuffer();
				for (int i = 0; i < bases.length; i++) {
					sb.append(bases[i].getComplimentaryNucleotideName(true));
					sb1.append(bases[i].getName());
				}
				String c = sb.toString();
				// System.out.println("c "+c);
				float usage1 = getUsageForCodon(c) * 1000;
				if (usage1 > usage) {
					usage = usage1;
					if (rnaStyle) {
						retValue = c;
					}
					else {
						retValue = sb1.toString();
					}
				}
			}
			if (usage < 0)
				retValue = null;
		}
		return retValue;
	}

	public char[] encode() {// Charles compatible
		String cd = getDNA35Codon();
		/*
		 * if(cd == null || cd.length() < 1) return null; char []ret = new char[cd.length()]; for(int i = 0; i <
		 * ret.length; i++) ret[i] = cd.charAt(i);
		 */
		return cd.toCharArray();

	}

	public char[] encodeRandomly() {
		String cd = getCodonRandom(EXPRESS_TO_53DNA_STRAND);
		/*
		 * if(cd == null || cd.length() < 1) return null; char []ret = new char[cd.length()]; for(int i = 0; i <
		 * ret.length; i++) ret[i] = cd.charAt(i);
		 */
		return cd.toCharArray();
	}

	// that equivalent to second method with expressStyle = EXPRESS_FROM_53DNA_STRAND
	public static Aminoacid express(char[] c) throws IllegalArgumentException {
		return express(c, EXPRESS_FROM_53DNA_STRAND);
	}

	public static Aminoacid express(char[] c, int expressStyle) throws IllegalArgumentException {
		if (c == null || c.length != 3)
			throw new IllegalArgumentException("Aminoacid.express: parameter should be char array with size = 3");
		if (expressStyle < EXPRESS_FROM_53DNA_STRAND || expressStyle > EXPRESS_FROM_RNA)
			throw new IllegalArgumentException("Aminoacid.express: expressStyle should be >= 0 and <= 2");
		Nucleotide[] nucleos = new Nucleotide[3];
		for (int i = 0; i < 3; i++) {
			if (c[i] == Nucleotide.URACIL_NAME && expressStyle != EXPRESS_FROM_RNA)
				throw new IllegalArgumentException("Aminoacid.express: uracil isn't allowed in DNA style express");
			if (c[i] == Nucleotide.THYMINE_NAME && expressStyle == EXPRESS_FROM_RNA)
				throw new IllegalArgumentException("Aminoacid.express: thymine isn't allowed in RNA style express");
			nucleos[i] = Nucleotide.getNucleotide(c[i]);
		}
		char n1 = 0, n2 = 0, n3 = 0;
		switch (expressStyle) {
		case EXPRESS_FROM_53DNA_STRAND:
			n1 = Nucleotide.convert53DNAStrandToRNA(nucleos[0].getName());
			n2 = Nucleotide.convert53DNAStrandToRNA(nucleos[1].getName());
			n3 = Nucleotide.convert53DNAStrandToRNA(nucleos[2].getName());
			break;
		case EXPRESS_FROM_35DNA_STRAND:
			n1 = nucleos[0].getComplimentaryNucleotideName(true);
			n2 = nucleos[1].getComplimentaryNucleotideName(true);
			n3 = nucleos[2].getComplimentaryNucleotideName(true);
			break;
		case EXPRESS_FROM_RNA:
			n1 = nucleos[0].getName();
			n2 = nucleos[1].getName();
			n3 = nucleos[2].getName();
			break;
		}
		return Codon.getCodon(n1, n2, n3).createAminoacid();
	}

	public String getDNACodonsStringForTable() {
		StringBuffer sb = new StringBuffer();
		java.util.Collection v = getDNACodons();
		if (v != null) {
			java.util.Iterator it = v.iterator();
			while (it.hasNext()) {
				Codon codon = new Codon((String) it.next());
				Nucleotide[] bases = codon.bases;
				for (int i = 0; i < bases.length; i++) {
					sb.append(bases[i].getComplimentaryNucleotideName(true));
				}
				if (it.hasNext()) {
					sb.append(",");
				}
			}
		}
		return sb.toString();
	}

	public float getUsageForCodon(String codon, int expressStyle) throws IllegalArgumentException {
		return getUsageForCodon(convertToRNAStyleCodon(codon, expressStyle));
	}

	static String convertToRNAStyleCodon(String codon, int expressStyle) throws IllegalArgumentException {
		if (expressStyle == EXPRESS_FROM_RNA)
			return codon;
		if (expressStyle == EXPRESS_FROM_53DNA_STRAND)
			return codon.replace(Nucleotide.THYMINE_NAME, Nucleotide.URACIL_NAME);
		if (expressStyle == EXPRESS_FROM_35DNA_STRAND) {
			if (codon != null && codon.length() == 3) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < 3; i++) {
					Nucleotide n = Nucleotide.getNucleotide(codon.charAt(i));
					sb.append(n.getComplimentaryNucleotideName(true));
				}
				return sb.toString();
			}
		}
		throw new IllegalArgumentException("Aminoacid.convertToRNAStyleCodon: expressStyle should be >= 0 and <= 2");
	}

	static String convertFromRNAStyleCodon(String codon, int expressStyle) {
		if (expressStyle == EXPRESS_TO_RNA)
			return codon;
		if (expressStyle == EXPRESS_TO_53DNA_STRAND)
			return codon.replace(Nucleotide.URACIL_NAME, Nucleotide.THYMINE_NAME);
		if (expressStyle == EXPRESS_TO_35DNA_STRAND) {
			if (codon != null && codon.length() == 3) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < 3; i++) {
					Nucleotide n = Nucleotide.getNucleotide(codon.charAt(i));
					sb.append(n.getComplimentaryNucleotideName(false));
				}
				return sb.toString();
			}
		}
		throw new IllegalArgumentException("Aminoacid.convertFromRNAStyleCodon: expressStyle should be >= 0 and <= 2");
	}

	public float getUsageForCodonRelative(String codon, int expressStyle) throws IllegalArgumentException {
		String rnaCodon = convertToRNAStyleCodon(codon, expressStyle);
		Vector codons = getDNACodons();
		float s = 0;
		float myUsage = -1;
		for (int i = 0; i < codons.size(); i++) {
			String cd = (String) codons.elementAt(i);
			float usage = getUsageForCodon(cd, EXPRESS_FROM_35DNA_STRAND);
			s += usage;
			if (myUsage < 0 && convertToRNAStyleCodon(cd, EXPRESS_FROM_35DNA_STRAND).equalsIgnoreCase(rnaCodon)) {
				myUsage = usage;
			}
		}
		myUsage /= s;
		if (myUsage < 0)
			myUsage = 0;
		if (myUsage > 1)
			myUsage = 1;
		return myUsage;
	}

	public float getUsageForCodon(String codon) throws IllegalArgumentException {
		AminoCodonHolder holder = aminoCreation.get(codon);
		if (this != holder.amino) {
			throw new IllegalArgumentException("Codon " + codon + " doesn't produce " + standardAbbreviation);
		}
		return holder.codonUsage / 1000f;
	}

	/**
	 * Returns the usage of the codon
	 * 
	 * @param codonStr
	 *            (RNA style codon)
	 * @return a codon usage
	 */
	public static float getCodonUsage(String codonStr) throws IllegalArgumentException {
		float retValue = 0;
		try {
			Codon codon = Codon.getCodon(codonStr);
			Aminoacid amino = codon.createAminoacid();
			retValue = amino.getUsageForCodon(codonStr);
		}
		catch (Throwable t) {
			retValue = 0;
		}
		return retValue;
	}

	public String toString() {
		return "[" + getName() + "]";
	}

	protected static String createStandardAbbreviation(String abbreviation) {
		char[] ch = new char[3];
		abbreviation.toLowerCase().getChars(0, 3, ch, 0);
		ch[0] = Character.toUpperCase(ch[0]);
		return new String(ch, 0, 3);
	}

	public static Color getUsageColor(String codonStr) {
		codonStr = codonStr.replace('T', 'U');
		float usage = getCodonUsage(codonStr);
		if (usage > MAX_USAGE_FOR_COLOR)
			usage = MAX_USAGE_FOR_COLOR;
		float r = (MAX_USAGE_FOR_COLOR - usage) / MAX_USAGE_FOR_COLOR;
		return new Color(r, r, 1);
	}

	public String getStandardAbbreviation() {
		return standardAbbreviation;
	}

	public String getAbbreviation() {
		return abbreviation;
	}

	public String getName() {
		return name;
	}

	public char getSymbol() {
		return symbol;
	}

	public String getFullName() {
		return getName();
	}

	public String getProperty() {
		return property;
	}

	public char getLetter() {
		return getSymbol();
	}

	final static Hashtable<String, Aminoacid> aminoacidsName = new Hashtable<String, Aminoacid>();
	final static Hashtable<String, Aminoacid> aminoacidsAbbr = new Hashtable<String, Aminoacid>();
	final static Hashtable<Character, Aminoacid> aminoacidsSymb = new Hashtable<Character, Aminoacid>();
	final static Hashtable<String, AminoCodonHolder> aminoCreation = new Hashtable<String, AminoCodonHolder>();
	final static Hashtable codonUsage = new Hashtable();
	final static Hashtable icons = new Hashtable();
	final static Hashtable formulas = new Hashtable();

	static {
		for (Enumeration e = bundle.getKeys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			Aminoacid amino = (Aminoacid) bundle.getObject((String) key);
			aminoacidsName.put(amino.name.toLowerCase(), amino);
			aminoacidsAbbr.put(amino.abbreviation.toUpperCase(), amino);
			aminoacidsSymb.put(amino.symbol, amino);
		}

		Aminoacid aminoToAdd = getByAbbreviation("PHE");
		aminoCreation.put("UUU", new AminoCodonHolder(aminoToAdd, 16.6f));// 16.6
		aminoCreation.put("UUC", new AminoCodonHolder(aminoToAdd, 20.7f));// 20.7
		aminoToAdd = getByAbbreviation("LEU");
		aminoCreation.put("UUA", new AminoCodonHolder(aminoToAdd, 7f));// 7
		aminoCreation.put("UUG", new AminoCodonHolder(aminoToAdd, 12f));// 12
		aminoCreation.put("CUU", new AminoCodonHolder(aminoToAdd, 12.4f));// 12.4
		aminoCreation.put("CUC", new AminoCodonHolder(aminoToAdd, 19.3f));// 19.3
		aminoCreation.put("CUA", new AminoCodonHolder(aminoToAdd, 6.8f));// 6.8
		aminoCreation.put("CUG", new AminoCodonHolder(aminoToAdd, 40f));// 40
		aminoToAdd = getByAbbreviation("ILE");
		aminoCreation.put("AUU", new AminoCodonHolder(aminoToAdd, 15.7f));// 15.7
		aminoCreation.put("AUC", new AminoCodonHolder(aminoToAdd, 22.3f));// 22.3
		aminoCreation.put("AUA", new AminoCodonHolder(aminoToAdd, 7f));// 7
		aminoToAdd = getByAbbreviation("MET");
		aminoCreation.put("AUA" + Codon.MITOCHONDRIA_SUFFIX, new AminoCodonHolder(aminoToAdd, 0));// ?
		aminoCreation.put("AUG", new AminoCodonHolder(aminoToAdd, 22.2f));// 22.2
		aminoToAdd = getByAbbreviation("VAL");
		aminoCreation.put("GUU", new AminoCodonHolder(aminoToAdd, 10.7f));// 10.7
		aminoCreation.put("GUC", new AminoCodonHolder(aminoToAdd, 14.8f));// 14.8
		aminoCreation.put("GUA", new AminoCodonHolder(aminoToAdd, 6.8f));// 6.8
		aminoCreation.put("GUG", new AminoCodonHolder(aminoToAdd, 29.3f));// 29.3
		aminoToAdd = getByAbbreviation("SER");
		aminoCreation.put("UCU", new AminoCodonHolder(aminoToAdd, 14.5f));// 14.5
		aminoCreation.put("UCC", new AminoCodonHolder(aminoToAdd, 17.7f));// 17.7
		aminoCreation.put("UCA", new AminoCodonHolder(aminoToAdd, 11.4f));// 11.4
		aminoCreation.put("UCG", new AminoCodonHolder(aminoToAdd, 4.5f));// 4.5
		aminoCreation.put("AGU", new AminoCodonHolder(aminoToAdd, 11.7f));// 11.7
		aminoCreation.put("AGC", new AminoCodonHolder(aminoToAdd, 19.3f));// 19.3
		aminoToAdd = getByAbbreviation("PRO");
		aminoCreation.put("CCU", new AminoCodonHolder(aminoToAdd, 17.2f));// 17.2
		aminoCreation.put("CCC", new AminoCodonHolder(aminoToAdd, 20.3f));// 20.3
		aminoCreation.put("CCA", new AminoCodonHolder(aminoToAdd, 16.5f));// 16.5
		aminoCreation.put("CCG", new AminoCodonHolder(aminoToAdd, 7.1f));// 7.1
		aminoToAdd = getByAbbreviation("THR");
		aminoCreation.put("ACU", new AminoCodonHolder(aminoToAdd, 12.7f));// 12.7
		aminoCreation.put("ACC", new AminoCodonHolder(aminoToAdd, 19.9f));// 19.9
		aminoCreation.put("ACA", new AminoCodonHolder(aminoToAdd, 14.7f));// 14.7
		aminoCreation.put("ACG", new AminoCodonHolder(aminoToAdd, 6.4f));// 6.4
		aminoToAdd = getByAbbreviation("ALA");
		aminoCreation.put("GCU", new AminoCodonHolder(aminoToAdd, 18.4f));// 18.4
		aminoCreation.put("GCC", new AminoCodonHolder(aminoToAdd, 28.6f));// 28.6
		aminoCreation.put("GCA", new AminoCodonHolder(aminoToAdd, 15.6f));// 15.6
		aminoCreation.put("GCG", new AminoCodonHolder(aminoToAdd, 7.7f));// 7.7
		aminoToAdd = getByAbbreviation("TYR");
		aminoCreation.put("UAU", new AminoCodonHolder(aminoToAdd, 12.1f));// 12.1
		aminoCreation.put("UAC", new AminoCodonHolder(aminoToAdd, 16.3f));// 16.3
		aminoToAdd = getByAbbreviation("HIS");
		aminoCreation.put("CAU", new AminoCodonHolder(aminoToAdd, 10.1f));// 10.1
		aminoCreation.put("CAC", new AminoCodonHolder(aminoToAdd, 14.9f));// 14.9
		aminoToAdd = getByAbbreviation("GLN");
		aminoCreation.put("CAA", new AminoCodonHolder(aminoToAdd, 11.8f));// 11.8
		aminoCreation.put("CAG", new AminoCodonHolder(aminoToAdd, 34.4f));// 34.4
		aminoToAdd = getByAbbreviation("ASN");
		aminoCreation.put("AAU", new AminoCodonHolder(aminoToAdd, 16.8f));// 16.8
		aminoCreation.put("AAC", new AminoCodonHolder(aminoToAdd, 19.4f));// 19.4
		aminoToAdd = getByAbbreviation("LYS");
		aminoCreation.put("AAA", new AminoCodonHolder(aminoToAdd, 23.6f));// 23.6
		aminoCreation.put("AAG", new AminoCodonHolder(aminoToAdd, 33.2f));// 33.2
		aminoToAdd = getByAbbreviation("ASP");
		aminoCreation.put("GAU", new AminoCodonHolder(aminoToAdd, 22.2f));// 22.2
		aminoCreation.put("GAC", new AminoCodonHolder(aminoToAdd, 26.5f));// 26.5
		aminoToAdd = getByAbbreviation("GLU");
		aminoCreation.put("GAA", new AminoCodonHolder(aminoToAdd, 28.6f));// 28.6
		aminoCreation.put("GAG", new AminoCodonHolder(aminoToAdd, 40.6f));// 40.6
		aminoToAdd = getByAbbreviation("CYS");
		aminoCreation.put("UGU", new AminoCodonHolder(aminoToAdd, 9.7f));// 9.7
		aminoCreation.put("UGC", new AminoCodonHolder(aminoToAdd, 12.4f));// 12.4
		aminoToAdd = getByAbbreviation("TRP");
		aminoCreation.put("UGG", new AminoCodonHolder(aminoToAdd, 13f));// 13
		aminoCreation.put("UGA" + Codon.MITOCHONDRIA_SUFFIX, new AminoCodonHolder(aminoToAdd, 0));// ?
		aminoToAdd = getByAbbreviation("ARG");
		aminoCreation.put("CGU", new AminoCodonHolder(aminoToAdd, 4.7f));// 4.7
		aminoCreation.put("CGC", new AminoCodonHolder(aminoToAdd, 11f));// 11
		aminoCreation.put("CGA", new AminoCodonHolder(aminoToAdd, 6.2f));// 6.2
		aminoCreation.put("CGG", new AminoCodonHolder(aminoToAdd, 11.6f));// 11.6
		aminoCreation.put("AGA", new AminoCodonHolder(aminoToAdd, 11.2f));// 11.2
		aminoCreation.put("AGG", new AminoCodonHolder(aminoToAdd, 11.1f));// 11.1
		aminoToAdd = getByAbbreviation("GLY");
		aminoCreation.put("GGU", new AminoCodonHolder(aminoToAdd, 10.9f));// 10.9
		aminoCreation.put("GGC", new AminoCodonHolder(aminoToAdd, 23.1f));// 23.1
		aminoCreation.put("GGA", new AminoCodonHolder(aminoToAdd, 16.4f));// 16.4
		aminoCreation.put("GGG", new AminoCodonHolder(aminoToAdd, 16.5f));// 16.5

	}

	static class AminoCodonHolder {
		public float codonUsage = 0;
		public Aminoacid amino;

		AminoCodonHolder(Aminoacid amino, float codonUsage) {
			this.codonUsage = codonUsage;
			this.amino = amino;
		}
	}

	public void putProperty(Object key, Object value) {
		if (properties == null)
			properties = new HashMap<Object, Object>();
		if (properties == null)
			return;
		properties.put(key, value);
	}

	public Object getProperty(Object key) {
		if (properties == null)
			return null;
		return properties.get(key);
	}

	public static Aminoacid[] getAllAminoacids() {
		if (allAminoacids == null) {
			allAminoacids = new Aminoacid[20];
			allAminoacids[0] = getByAbbreviation("Gly");
			allAminoacids[1] = getByAbbreviation("Ala");
			allAminoacids[2] = getByAbbreviation("Val");
			allAminoacids[3] = getByAbbreviation("Leu");
			allAminoacids[4] = getByAbbreviation("Ile");
			allAminoacids[5] = getByAbbreviation("Phe");
			allAminoacids[6] = getByAbbreviation("Pro");
			allAminoacids[7] = getByAbbreviation("Trp");
			allAminoacids[8] = getByAbbreviation("Met");
			allAminoacids[9] = getByAbbreviation("Cys");
			allAminoacids[10] = getByAbbreviation("Asn");
			allAminoacids[11] = getByAbbreviation("Gln");
			allAminoacids[12] = getByAbbreviation("Ser");
			allAminoacids[13] = getByAbbreviation("Thr");
			allAminoacids[14] = getByAbbreviation("Tyr");
			allAminoacids[15] = getByAbbreviation("Asp");
			allAminoacids[16] = getByAbbreviation("Glu");
			allAminoacids[17] = getByAbbreviation("Lys");
			allAminoacids[18] = getByAbbreviation("Arg");
			allAminoacids[19] = getByAbbreviation("His");
		}
		return allAminoacids;
	}
}

/*
 * http://www.tcd.ie/Genetics/staff/Miguel_DeArce_Bioinf_01/bioinformatics_01/htm/air.htm Frequency Expressed in
 * aminoacids per thousand. Calculated to be an average for all proteins, ignoring codon preference, and assuming an
 * averaga aminoacid composition in Frame 1. FR1, FR2 and FR3 are the three reading frames. Source, Staden R. (1990)
 * Finding Protein Coding regions in Genomic sequences. Methods in Enzymology, vol 183. Formula The projection molecules
 * were obtained from http://www.ccdc.cam.ac.uk/support/csd_doc/volume3/z309.html, where further references could be
 * found. The alpha carbon is shown in the top row with the side chain projecting downwards. Codons This is the Standard
 * Genetic Code. In DNA sequence, U is replaced by T. A Codon Usage Database can be seen here, where other species are
 * also listed. Also included in that reference is a codon usage program, where sequences can be pasted and transformed
 * into usage tables. The codon usage reported in the table above is given in 'per thousand residues', and refers to the
 * entire human genome, as per GeneBank release 117.0 (15th April 2000). For other genetic codes see for instance
 * http://www.ebi.ac.uk/cgi-bin/mutations/trtables.cgi MW Molecular weight. Surface In squared angstroms. This and the
 * following columns are taken from http://www.imb-jena.de/IMAGE_AA.html, which inspired this table in the first place.
 * Volume In cubic angstroms. pKa and pI these refer to the side chain, and were measured at 25C in aqueous solution.
 * The pKa for the alpha-carbon-COOH is near 2, and for the alpha-caron-NH2, about 10 for all aminoacids. Like the pH,
 * they are a quantitative measure of acidity/alkalinity in solution. Solubility (in water) is in g/100g, at 25C.
 * Hydrophobicity Three scales are shown. RS is residue non-polar surface area, measured in squared ansgtroms. RB is the
 * estimated hdrophobic effect for residue burial, measured in kcal/mol. SB is the estimated hydrophobic effect for side
 * chain burial, measured as before The values are obtained from the previous column by subtractiing the value for Gly
 * (1.18 kcal/mol). Scale this is the hydrophobicity scale of J. Kyte and RF Doolittle J. Mol. Biol. (1982) 157,
 * 105-132.
 */
/*
 * http://babbage.infobiogen.fr/doc/GCGdoc/Data_Files/protein_analysis_tables.html#aminoacid.dat%20and%20extinctcoef.dat
 * AmAcid MolWt Charge Aroma Acid Base Sulfur Phil phob retn2.1 retn7.4 Ala 89.09 0 0 0 0 0 0 1 -0.1 0.5 Arg 174.20 1 0
 * 0 1 0 1 0 -4.5 0.8 Asn 132.12 0 0 0 0 0 1 0 -1.6 -0.8 Asp 133.10 -1 0 1 0 0 1 0 -2.8 -8.2 Cys 121.15 0 0 0 0 1 0 1
 * -2.2 -6.8 Gln 146.15 0 0 0 0 0 1 0 -2.5 -4.8 Glu 147.13 -1 0 1 0 0 1 0 -7.5 -16.9 Gly 75.07 0 0 0 0 0 0 1 -0.5 0.0
 * His 155.16 0 0 0 0 0 1 0 0.8 -3.5 Ile 131.17 0 0 0 0 0 0 1 11.8 13.9 Leu 131.17 0 0 0 0 0 0 1 10.0 8.8 Lys 146.19 1 0
 * 0 1 0 1 0 -3.2 0.1 Met 149.21 0 0 0 0 1 0 1 7.1 4.8 Phe 165.19 0 1 0 0 0 0 1 13.9 13.2 Pro 115.13 0 0 0 0 0 1 0 8.0
 * 6.1 Ser 105.09 0 0 0 0 0 1 0 -3.7 1.2 Thr 119.12 0 0 0 0 0 1 0 1.5 2.7 Trp 204.23 0 1 0 0 0 0 1 18.1 14.9 Tyr 181.19
 * 0 1 0 0 0 0 1 8.2 6.1 Val 117.15 0 0 0 0 0 0 1 3.3 2.7
 * 
 * 
 * Asx 132.61 -.5 0 1 0 0 1 0 -2.2 -4.5 Glx 146.64 -.5 0 1 0 0 1 0 -5.0 -10.8 NH2 0 0 0 0 0 0 0 0 -0.4 2.4 Ave 128.16 0
 * 0 0 0 0 0 0 0.0 0.0 COO 0 0 0 0 0 0 0 0 6.9 -3.0 End 0 0 0 0 0 0 0 0 0.0 0.0
 * 
 * pK C 8.3 D 3.91 E 4.25 H 6.50 K 10.79 R 12.50 Y 10.95
 * 
 * NH2 8.56 COOH 3.56
 * 
 */
