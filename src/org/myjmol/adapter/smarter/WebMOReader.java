/* $RCSfile: WebMOReader.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 18:22:43 $
 * $Revision: 1.2 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.adapter.smarter;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Vector;

import org.myjmol.util.Logger;

/**
 * A molecular orbital reader for WebMO files.
 * <p>
 * <a href='http://www.webmo.net/demo/'> http://www.webmo.net/demo/ </a>
 * 
 * right now WebMO files don't allow for multiple MOS, but we will assume here that that may change.
 * <p>
 */
class WebMOReader extends AtomSetCollectionReader {

	Hashtable moData = new Hashtable();
	Vector orbitals = new Vector();

	@SuppressWarnings("unchecked")
	AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
		this.reader = reader;
		atomSetCollection = new AtomSetCollection("webmo");
		readLine();
		modelNumber = 0;
		try {
			while (line != null) {
				if (line.equals("[HEADER]")) {
					readHeader();
					continue;
				}
				else if (line.equals("[ATOMS]")) {
					readAtoms();
					// atomSetCollection.setAtomSetName(thisDataSetName);
					continue;
				}
				else if (line.equals("[BONDS]")) {
					readBonds();
					continue;
				}
				else if (line.equals("[AO_ORDER]")) {
					readAtomicOrbitalOrder();
					continue;
				}
				else if (line.equals("[GTO]")) {
					readGaussianBasis();
					continue;
				}
				else if (line.equals("[STO]")) {
					readSlaterBasis();
					continue;
				}
				else if (line.indexOf("[MO") == 0) {
					if (++modelNumber == desiredModelNumber || desiredModelNumber <= 0) {
						readMolecularOrbital();
						if (desiredModelNumber > 0)
							break;
					}
					continue;
				}
				readLine();
			}
		}
		catch (Exception e) {
			Logger.error("Could not read file at line: " + line, e);
			// TODO: Why this?
			// new NullPointerException();
		}
		logger.log(orbitals.size() + " molecular orbitals read");
		moData.put("mos", orbitals);
		atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
		return atomSetCollection;
	}

	@SuppressWarnings("unchecked")
	void readHeader() throws Exception {
		while (readLine() != null && line.length() > 0) {
			moData.put("calculationType", "?");
			String[] tokens = getTokens(line);
			tokens[0] = tokens[0].substring(0, 1).toLowerCase() + tokens[0].substring(1, tokens[0].length());
			String str = "";
			for (int i = 1; i < tokens.length; i++)
				str += (i == 1 ? "" : " ") + tokens[i].toLowerCase();
			moData.put(tokens[0], str);
		}
	}

	void readAtoms() throws Exception {
		/*
		 * 
		 * [ATOMS] C 0 0 -1.11419008746451 O 0 0 1.11433559637682
		 * 
		 * !!!!or!!!!
		 * 
		 * [ATOMS] 6 0 0 0 6 2.81259696844285 0 0 16 1.40112510589261 3.14400070481769 0 6 4.21654880978248
		 * -0.850781692374614 -2.34559506901613
		 * 
		 */

		readLine();
		boolean isAtomicNumber = (parseInt(line) != Integer.MIN_VALUE);
		while (line != null && (line.length() == 0 || line.charAt(0) != '[')) {
			if (line.length() != 0) {
				Atom atom = atomSetCollection.addNewAtom();
				String[] tokens = getTokens(line);
				if (isAtomicNumber) {
					atom.elementSymbol = getElementSymbol(parseInt(tokens[0]));
				}
				else {
					atom.elementSymbol = tokens[0];
				}
				atom.x = parseFloat(tokens[1]) * ANGSTROMS_PER_BOHR;
				atom.y = parseFloat(tokens[2]) * ANGSTROMS_PER_BOHR;
				atom.z = parseFloat(tokens[3]) * ANGSTROMS_PER_BOHR;
			}
			readLine();
		}
	}

	void readBonds() throws Exception {
		/*
		 * 
		 * [BONDS] 1 2 2 1 3 1 1 4 1
		 * 
		 */

		while (readLine() != null && (line.length() == 0 || line.charAt(0) != '[')) {
			if (line.length() == 0)
				continue;
			String[] tokens = getTokens(line);
			int atomIndex1 = parseInt(tokens[0]);
			int atomIndex2 = parseInt(tokens[1]);
			int order = parseInt(tokens[2]);
			atomSetCollection.addBond(new Bond(atomIndex1 - 1, atomIndex2 - 1, order));
		}
	}

	@SuppressWarnings("unchecked")
	void readAtomicOrbitalOrder() throws Exception {
		/*
		 * [AO_ORDER] DOrbitals XX YY ZZ XY XZ YZ FOrbitals XXX YYY ZZZ XXY XXZ YYX YYZ ZZX ZZY XYZ
		 */
		Hashtable info = new Hashtable();
		while (readLine() != null && (line.length() == 0 || line.charAt(0) != '[')) {
			if (line.length() == 0)
				continue;
			String[] tokens = getTokens(line);
			info.put(tokens[0].substring(0, 1), tokens);
		}
		moData.put("atomicOrbitalOrder", info);
	}

	@SuppressWarnings("unchecked")
	void readGaussianBasis() throws Exception {
		/*
		 * standard Gaussian format:
		 * 
		 * [GTO] 1 S 3 172.2560000 2.0931324849764 25.9109000 2.93675143488078 5.5333500 1.80173711536432
		 * 
		 * 1 SP 2 3.6649800 -0.747384339731355 1.70917757609178 0.7705450 0.712661025209793 0.885622064435248
		 * 
		 */

		Vector sdata = new Vector();
		Vector gdata = new Vector();
		int atomIndex = 0;
		int gaussianPtr = 0;

		while (readLine() != null && (line.length() == 0 || line.charAt(0) != '[')) {
			String[] tokens = getTokens(line);
			if (tokens.length == 0)
				continue;
			if (tokens.length != 1) {
				logger.log("Error reading GTOs: missing atom index");
				new NullPointerException();
			}
			Hashtable slater = new Hashtable();
			atomIndex = parseInt(tokens[0]) - 1;
			tokens = getTokens(readLine());
			String basisType = tokens[0];
			int nGaussians = parseInt(tokens[1]);
			slater.put("atomIndex", new Integer(atomIndex));
			slater.put("basisType", basisType);
			slater.put("nGaussians", new Integer(nGaussians));
			slater.put("gaussianPtr", new Integer(gaussianPtr));
			for (int i = 0; i < nGaussians; i++) {
				String[] strData = getTokens(readLine());
				int nData = strData.length;
				float[] data = new float[nData];
				for (int d = 0; d < nData; d++)
					data[d] = parseFloat(strData[d]);
				gdata.add(data);
				gaussianPtr++;
			}
			sdata.add(slater);
		}
		float[][] garray = new float[gaussianPtr][];
		for (int i = 0; i < gaussianPtr; i++)
			garray[i] = (float[]) gdata.get(i);
		moData.put("shells", sdata);
		moData.put("gaussians", garray);
		logger.log(sdata.size() + " slater shells read");
		logger.log(garray.length + " gaussian primitives read");
	}

	@SuppressWarnings("unchecked")
	void readSlaterBasis() throws Exception {
		/*
		 * slater format: [STO] 1 0 0 0 1 1.565085 0.998181645138011 1 1 0 0 0 1.842345 2.59926303779824 1 0 1 0 0
		 * 1.842345 2.59926303779824 1 0 0 1 0 1.842345 2.59926303779824
		 */
		Vector intinfo = new Vector();
		Vector floatinfo = new Vector();
		int ndata = 0;
		while (readLine() != null && (line.length() == 0 || line.charAt(0) != '[')) {
			String[] tokens = getTokens(line);
			if (tokens.length < 7)
				continue;
			float fdata[] = new float[2];
			int idata[] = new int[5];
			idata[0] = parseInt(tokens[0]) - 1;
			for (int i = 1; i < 5; i++)
				idata[i] = parseInt(tokens[i]);
			fdata[0] = parseFloat(tokens[5]);
			fdata[1] = parseFloat(tokens[6]);
			intinfo.add(idata);
			floatinfo.add(fdata);
			ndata++;
		}
		int[][] iarray = new int[ndata][];
		for (int i = 0; i < ndata; i++)
			iarray[i] = (int[]) intinfo.get(i);
		moData.put("slaterInfo", iarray);
		float[][] farray = new float[ndata][];
		for (int i = 0; i < ndata; i++)
			farray[i] = (float[]) floatinfo.get(i);
		moData.put("slaterData", farray);
	}

	@SuppressWarnings("unchecked")
	void readMolecularOrbital() throws Exception {
		/*
		 * [MOn] -11.517 2 1 0.0939313753737777 2 0.204585583790748 3 0.111068760356317 4 -0.020187156204269
		 */
		Hashtable mo = new Hashtable();
		Vector data = new Vector();
		float energy = parseFloat(readLine());
		int occupancy = parseInt(readLine());
		while (readLine() != null && (line.length() == 0 || line.charAt(0) != '[')) {
			if (line.length() == 0)
				continue;
			String[] tokens = getTokens(line);
			data.add(tokens[1]);
		}
		float[] coefs = new float[data.size()];
		for (int i = data.size(); --i >= 0;)
			coefs[i] = parseFloat((String) data.get(i));
		mo.put("energy", new Float(energy));
		mo.put("occupancy", new Integer(occupancy));
		mo.put("coefficients", coefs);
		orbitals.add(mo);
	}
}
