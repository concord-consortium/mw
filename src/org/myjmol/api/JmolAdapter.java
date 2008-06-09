/* $RCSfile: JmolAdapter.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:13 $
 * $Revision: 1.10 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

package org.myjmol.api;

import java.io.BufferedReader;
import java.util.Properties;
import java.util.Hashtable;
import java.util.BitSet;

/***********************************************************************************************************************
 * The JmolAdapter interface defines the API used by the JmolViewer to read external files and fetch atom properties
 * necessary for rendering.
 * 
 * A client of the JmolViewer implements this interface on top of their existing molecular model representation. The
 * JmolViewer then requests information from the implementation using this API.
 * 
 * Jmol will automatically calculate some atom properties if the client is not capable or does not want to supply them.
 * 
 * Note: If you are seeing pink atoms that have lots of bonds, then your methods for getElementNumber(clientAtom) or
 * getElementSymbol(clientAtom) are probably returning stray values. Therefore, these atoms are getting mapped to
 * element 0 (Xx), which has color pink and a relatively large covalent bonding radius.
 * 
 * @see org.myjmol.api.JmolViewer
 **********************************************************************************************************************/
public abstract class JmolAdapter {

	public final static short ORDER_COVALENT_SINGLE = 1;
	public final static short ORDER_COVALENT_DOUBLE = 2;
	public final static short ORDER_COVALENT_TRIPLE = 3;
	public final static short ORDER_AROMATIC = (1 << 2);
	public final static short ORDER_HBOND = (1 << 6);
	public final static short ORDER_STEREO_NEAR = ((1 << 3) | 1);
	public final static short ORDER_STEREO_FAR = ((2 << 3) | 2);
	public final static short ORDER_PARTIAL01 = (1 << 10);
	public final static short ORDER_PARTIAL12 = (1 << 11);

	// ////////////////////////////////////////////////////////////////
	// file related
	// ////////////////////////////////////////////////////////////////

	String adapterName;
	public Logger logger;

	public JmolAdapter(String adapterName, Logger logger) {
		this.adapterName = adapterName;
		this.logger = (logger == null ? new Logger() : logger);
	}

	/**
	 * Associate a clientFile object with a bufferedReader.
	 * 
	 * <p>
	 * Given the BufferedReader, return an object which represents the file contents. The parameter <code>name</code>
	 * is assumed to be the file name or URL which is the source of reader. Note that this 'file' may have been
	 * automatically decompressed. Also note that the name may be 'String', representing a string constant. Therefore,
	 * few assumptions should be made about the <code>name</code> parameter.
	 * 
	 * The return value is an object which represents a <code>clientFile</code>. This <code>clientFile</code> will
	 * be passed back in to other methods. If the return value is <code>instanceof String</code> then it is considered
	 * an error condition and the returned String is the error message.
	 * 
	 * @param name
	 *            File name, String or URL acting as the source of the reader
	 * @param bufferedReader
	 *            The BufferedReader
	 * @return The clientFile or String with an error message
	 */
	public Object openBufferedReader(String name, BufferedReader bufferedReader) {
		return openBufferedReader(name, bufferedReader, null, null);
	}

	/**
	 * @param name
	 *            File name, String or URL acting as the source of the reader
	 * @param bufferedReader
	 *            The BufferedReader
	 * @param logger
	 *            The logger
	 * @return The clientFile or String with an error message
	 * @see #openBufferedReader(String, BufferedReader)
	 */
	public Object openBufferedReader(String name, BufferedReader bufferedReader, Logger logger) {
		return openBufferedReader(name, bufferedReader, null, null);
	}

	/**
	 * @param name
	 *            File name, String or URL acting as the source of the reader
	 * @param bufferedReader
	 *            The BufferedReader
	 * @param params
	 *            Optional integer parameters
	 * @return The clientFile or String with an error message
	 * @see #openBufferedReader(String, BufferedReader)
	 */
	public Object openBufferedReader(String name, BufferedReader bufferedReader, int[] params) {
		return openBufferedReader(name, bufferedReader, null, params);
	}

	/**
	 * @param name
	 *            File name, String or URL acting as the source of the reader
	 * @param bufferedReader
	 *            The BufferedReader
	 * @param logger
	 *            The logger
	 * @param params
	 *            Optional integer parameters
	 * @return The clientFile or String with an error message
	 * @see #openBufferedReader(String, BufferedReader)
	 */
	public Object openBufferedReader(String name, BufferedReader bufferedReader, Logger logger, int[] params) {
		return null;
	}

	/**
	 * Associate a clientFile object with an array of BufferedReader.
	 * 
	 * <p>
	 * Given the array of BufferedReader, return an object which represents the concatenation of every file contents.
	 * The parameter <code>name</code> is assumed to be the file names or URL which are the source of each reader.
	 * Note that each of this 'file' may have been automatically decompressed. Also note that the name may be 'String',
	 * representing a string constant. Therefore, few assumptions should be made about the <code>name</code>
	 * parameter.
	 * 
	 * The return value is an object which represents a <code>clientFile</code>. This <code>clientFile</code> will
	 * be passed back in to other methods. If the return value is <code>instanceof String</code> then it is considered
	 * an error condition and the returned String is the error message.
	 * 
	 * @param name
	 *            File names, String or URL acting as the source of each reader
	 * @param bufferedReader
	 *            The array of BufferedReader
	 * @return The clientFile or String with an error message
	 */
	public Object openBufferedReaders(String[] name, BufferedReader[] bufferedReader) {
		return openBufferedReaders(name, bufferedReader, null);
	}

	/**
	 * @param name
	 *            File names, String or URL acting as the source of each reader
	 * @param bufferedReader
	 *            The array of BufferedReader
	 * @param logger
	 *            The logger
	 * @return The clientFile or String with an error message
	 * @see #openBufferedReaders(String[], BufferedReader[])
	 */
	public Object openBufferedReaders(String[] name, BufferedReader[] bufferedReader, Logger logger) {
		return null;
	}

	public Object openDOMReader(Object DOMNode) {
		return openDOMReader(DOMNode, null);
	}

	public Object openDOMReader(Object DOMNode, Logger logger) {
		return null;
	}

	public void finish(Object clientFile) {
		logger = null;
	}

	/**
	 * Get the type of this file or molecular model, if known.
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The type of this file or molecular model, default <code>"unknown"</code>
	 */
	public String getFileTypeName(Object clientFile) {
		return "unknown";
	}

	/**
	 * Get the name of the atom set collection, if known.
	 * 
	 * <p>
	 * Some file formats contain a formal name of the molecule in the file. If this method returns <code>null</code>
	 * then the JmolViewer will automatically supply the file/URL name as a default.
	 * 
	 * @param clientFile
	 * @return The atom set collection name or <code>null</code>
	 */
	public String getAtomSetCollectionName(Object clientFile) {
		return null;
	}

	/**
	 * Get the properties for this atomSetCollection.
	 * 
	 * <p>
	 * Not yet implemented everywhere, it is in the smarterJmolAdapter
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The properties for this atomSetCollection or <code>null</code>
	 */
	public Properties getAtomSetCollectionProperties(Object clientFile) {
		return null;
	}

	/**
	 * Get the auxiliary information for this atomSetCollection.
	 * 
	 * <p>
	 * Via the smarterJmolAdapter
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The auxiliaryInfo Hashtable that may be available for particular filetypes for this atomSetCollection or
	 *         <code>null</code>
	 */
	public Hashtable getAtomSetCollectionAuxiliaryInfo(Object clientFile) {
		return null;
	}

	/**
	 * Get number of atomSets in the file.
	 * 
	 * <p>
	 * NOTE WARNING: <br>
	 * Not yet implemented everywhere, it is in the smarterJmolAdapter
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The number of atomSets in the file, default 1
	 */
	public int getAtomSetCount(Object clientFile) {
		return 1;
	}

	/**
	 * Get the number identifying each atomSet.
	 * 
	 * <p>
	 * For a PDB file, this is is the model number. For others it is a 1-based atomSet number.
	 * <p>
	 * <i>Note that this is not currently implemented in PdbReader</i>
	 * 
	 * @param clientFile
	 *            The client file
	 * @param atomSetIndex
	 *            The atom set's index for which to get the atom set number
	 * @return The number identifying each atom set, default atomSetIndex+1.
	 */
	public int getAtomSetNumber(Object clientFile, int atomSetIndex) {
		return atomSetIndex + 1;
	}

	/**
	 * Get the name of an atomSet.
	 * 
	 * @param clientFile
	 *            The client file
	 * @param atomSetIndex
	 *            The atom set index
	 * @return The name of the atom set, default the string representation of atomSetIndex
	 */
	public String getAtomSetName(Object clientFile, int atomSetIndex) {
		return "" + getAtomSetNumber(clientFile, atomSetIndex);
	}

	/**
	 * Get the properties for an atomSet.
	 * 
	 * @param clientFile
	 *            The client file
	 * @param atomSetIndex
	 *            The atom set index
	 * @return The properties for an atom set or <code>null</code>
	 */
	public Properties getAtomSetProperties(Object clientFile, int atomSetIndex) {
		return null;
	}

	/**
	 * Get the auxiliary information for a particular atomSet.
	 * 
	 * <p>
	 * Via the smarterJmolAdapter
	 * 
	 * @param clientFile
	 *            The client file
	 * @param atomSetIndex
	 *            The atom set index
	 * @return The auxiliaryInfo Hashtable that may be available for particular filetypes for this atomSet or
	 *         <code>null</code>
	 */
	public Hashtable getAtomSetAuxiliaryInfo(Object clientFile, int atomSetIndex) {
		return null;
	}

	/**
	 * Get the estimated number of atoms contained in the file.
	 * 
	 * <p>
	 * Just return -1 if you don't know (or don't want to figure it out)
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The estimated number of atoms in the file
	 */
	abstract public int getEstimatedAtomCount(Object clientFile);

	/**
	 * Get the boolean whether coordinates are fractional.
	 * 
	 * @param clientFile
	 *            The client file
	 * @return true if the coordinates are fractional, default <code>false</code>
	 */
	public boolean coordinatesAreFractional(Object clientFile) {
		return false;
	}

	/**
	 * Get the notional unit cell.
	 * 
	 * <p>
	 * This method returns the parameters that define a crystal unitcell the parameters are returned in a float[] in the
	 * following order <code>a, b, c, alpha, beta, gamma</code> <br>
	 * <code>a, b, c</code> : angstroms <br>
	 * <code>alpha, beta, gamma</code> : degrees <br>
	 * if there is no unit cell data then return null
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The array of the values or <code>null</code>
	 */
	public float[] getNotionalUnitcell(Object clientFile) {
		return null;
	}

	/**
	 * Get the PDB scale matrix.
	 * 
	 * <p>
	 * Does not seem to be overriden by any descendent
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The array of 9 floats for the matrix or <code>null</code>
	 */
	public float[] getPdbScaleMatrix(Object clientFile) {
		return null;
	}

	/**
	 * Get the PDB scale translation vector.
	 * <p>
	 * Does not seem to be overriden by any descendent
	 * 
	 * @param clientFile
	 *            The client file
	 * @return The x, y and z translation values or <code>null</code>
	 */
	public float[] getPdbScaleTranslate(Object clientFile) {
		return null;
	}

	/**
	 * Get a property from a clientAtom.
	 * 
	 * @param clientAtom
	 *            The clientAtom
	 * @param propertyName
	 *            the key of the property
	 * @return The value of the property
	 */
	public String getClientAtomStringProperty(Object clientAtom, String propertyName) {
		return null;
	}

	/**
	 * Get an AtomIterator for retrieval of all atoms in the file.
	 * 
	 * <p>
	 * This method may not return <code>null</code>.
	 * 
	 * @param clientFile
	 *            The client file
	 * @return An AtomIterator
	 * @see AtomIterator
	 */
	abstract public AtomIterator getAtomIterator(Object clientFile);

	/**
	 * Get a BondIterator for retrieval of all bonds in the file.
	 * 
	 * <p>
	 * If this method returns <code>null</code> and no bonds are defined then the JmolViewer will automatically apply
	 * its rebonding code to build bonds between atoms.
	 * 
	 * @param clientFile
	 *            The client file
	 * @return A BondIterator or <code>null</code>
	 * @see BondIterator
	 */
	public BondIterator getBondIterator(Object clientFile) {
		return null;
	}

	/**
	 * Get a StructureIterator.
	 * 
	 * @param clientFile
	 *            The client file
	 * @return A StructureIterator or <code>null</code>
	 */

	public StructureIterator getStructureIterator(Object clientFile) {
		return null;
	}

	/*******************************************************************************************************************
	 * AtomIterator is used to enumerate all the <code>clientAtom</code> objects in a specified frame. Note that Java
	 * 1.1 does not have java.util.Iterator so we will define our own AtomIterator
	 ******************************************************************************************************************/
	public abstract class AtomIterator {
		public abstract boolean hasNext();

		public int getAtomSetIndex() {
			return 0;
		}

		public BitSet getAtomSymmetry() {
			return null;
		}

		public int getAtomSite() {
			return Integer.MIN_VALUE;
		}

		abstract public Object getUniqueID();

		public int getElementNumber() {
			return -1;
		} // may be atomicNumber + isotopeNumber*128

		public String getElementSymbol() {
			return null;
		}

		public String getAtomName() {
			return null;
		}

		public int getFormalCharge() {
			return 0;
		}

		public float getPartialCharge() {
			return Float.NaN;
		}

		abstract public float getX();

		abstract public float getY();

		abstract public float getZ();

		public float getVectorX() {
			return Float.NaN;
		}

		public float getVectorY() {
			return Float.NaN;
		}

		public float getVectorZ() {
			return Float.NaN;
		}

		public float getBfactor() {
			return Float.NaN;
		}

		public int getOccupancy() {
			return 100;
		}

		public boolean getIsHetero() {
			return false;
		}

		public int getAtomSerial() {
			return Integer.MIN_VALUE;
		}

		public char getChainID() {
			return (char) 0;
		}

		public char getAlternateLocationID() {
			return (char) 0;
		}

		public String getGroup3() {
			return null;
		}

		public int getSequenceNumber() {
			return Integer.MIN_VALUE;
		}

		public char getInsertionCode() {
			return (char) 0;
		}

		public Object getClientAtomReference() {
			return null;
		}
	}

	/*******************************************************************************************************************
	 * BondIterator is used to enumerate all the bonds
	 ******************************************************************************************************************/

	public abstract class BondIterator {
		public abstract boolean hasNext();

		public abstract Object getAtomUniqueID1();

		public abstract Object getAtomUniqueID2();

		public abstract int getEncodedOrder();
	}

	/*******************************************************************************************************************
	 * StructureIterator is used to enumerate Structures Helix, Sheet, Turn
	 ******************************************************************************************************************/

	public abstract class StructureIterator {
		public abstract boolean hasNext();

		public abstract int getModelIndex();

		public abstract String getStructureType();

		public abstract char getStartChainID();

		public abstract int getStartSequenceNumber();

		public abstract char getStartInsertionCode();

		public abstract char getEndChainID();

		public abstract int getEndSequenceNumber();

		public abstract char getEndInsertionCode();
	}

	/**
	 * Logger that writes to stdout
	 */
	public class Logger { // default logger will log to stdout
		public boolean isLogging() {
			return true;
		}

		public void log(String str1) {
			org.myjmol.util.Logger.info(adapterName + ":" + str1);
		}

		public void log(String str1, Object obj1) {
			org.myjmol.util.Logger.info(adapterName + ":" + str1 + ":" + obj1);
		}

		public void log(String str1, Object obj1, Object obj2) {
			org.myjmol.util.Logger.info(adapterName + ":" + str1 + ":" + obj1 + ":" + obj2);
		}
	}

	// ////////////////////////////////////////////////////////////////
	// range-checking routines
	// ///////////////////////////////////////////////////////////////

	public final static char canonizeAlphaDigit(char ch) {
		if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9'))
			return ch;
		return '\0';
	}

	public final static char canonizeChainID(char chainID) {
		return canonizeAlphaDigit(chainID);
	}

	public final static char canonizeInsertionCode(char insertionCode) {
		return canonizeAlphaDigit(insertionCode);
	}

	public final static char canonizeAlternateLocationID(char altLoc) {
		// pdb altLoc
		return canonizeAlphaDigit(altLoc);
	}
}
