/* $RCSfile: Isosurface.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 21:10:05 $
 * $Revision: 1.11 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

/*
 * miguel 2005 07 17
 *
 *  System and method for the display of surface structures
 *  contained within the interior region of a solid body
 * United States Patent Number 4,710,876
 * Granted: Dec 1, 1987
 * Inventors:  Cline; Harvey E. (Schenectady, NY);
 *             Lorensen; William E. (Ballston Lake, NY)
 * Assignee: General Electric Company (Schenectady, NY)
 * Appl. No.: 741390
 * Filed: June 5, 1985
 *
 *
 * Patents issuing prior to June 8, 1995 can last up to 17
 * years from the date of issuance.
 *
 * Dec 1 1987 + 17 yrs = Dec 1 2004
 */

/*
 * Bob Hanson May 22, 2006
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 *  
 * inventing "Jmol Voxel File" format, *.jvxl
 * 
 * see http://www.stolaf.edu/people/hansonr/jmol/docs/JVXL-format.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters represting the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 */

package org.myjmol.viewer;

import java.io.BufferedReader;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

import org.myjmol.g3d.Graphics3D;
import org.myjmol.quantum.MepCalculation;
import org.myjmol.quantum.QuantumCalculation;
import org.myjmol.util.ArrayUtil;
import org.myjmol.util.Logger;

class Isosurface extends MeshCollection {

	void initShape() {
		super.initShape();
		myType = "isosurface";
	}

	boolean logMessages = false;
	boolean logCompression = false;
	boolean logCube = false;
	final static boolean colorByContourOnly = false;
	int state;
	final static int STATE_INITIALIZED = 1;
	final static int STATE_DATA_READ = 2;
	final static int STATE_DATA_COLORED = 3;

	final static float ANGSTROMS_PER_BOHR = JmolConstants.ANGSTROMS_PER_BOHR;
	final static int defaultEdgeFractionBase = 35; // #$%.......
	final static int defaultEdgeFractionRange = 90;
	final static int defaultColorFractionBase = 35;
	final static int defaultColorFractionRange = 90;
	final static float defaultMappedDataMin = 0f;
	final static float defaultMappedDataMax = 1.0f;
	final static float defaultCutoff = 0.02f;
	final static float defaultMepCutoff = 0.05f;
	final static float defaultMepMin = -0.05f;
	final static float defaultMepMax = 0.05f;
	final static float defaultOrbitalCutoff = 0.14f;
	final static float defaultQMOrbitalCutoff = 0.050f; // WebMO
	final static int defaultContourCount = 11; // odd is better
	final static int nContourMax = 100;
	final static int defaultColorNegative = Graphics3D.getArgbFromString("red");
	final static int defaultColorPositive = Graphics3D.getArgbFromString("blue");
	final static int defaultColorNegativeLCAO = Graphics3D.getArgbFromString("purple");
	final static int defaultColorPositiveLCAO = Graphics3D.getArgbFromString("orange");
	final static float defaultSolventRadius = 1.2f;

	String[] title = null;
	String colorScheme;
	short defaultColix;
	boolean colorBySign;
	int colorNeg;
	int colorPos;
	int colorPosLCAO;
	int colorNegLCAO;
	int colorPtr;
	boolean colorByPhase;
	int colorPhase;
	float resolution;
	boolean insideOut; // no longer does anything now that we are forcing 2-sided triangles

	float[] mepCharges;

	int qmOrbitalType;
	int qmOrbitalCount;
	final static int QM_TYPE_UNKNOWN = 0;
	final static int QM_TYPE_GAUSSIAN = 1;
	final static int QM_TYPE_SLATER = 2;
	Hashtable moData;
	float[] moCoefficients;

	boolean precalculateVoxelData;
	Vector functionXYinfo;
	String lcaoType;

	boolean isAnisotropic;
	boolean isEccentric;
	float eccentricityScale;
	float eccentricityRatio;

	boolean isAngstroms;
	float scale;
	Matrix3f eccentricityMatrix;
	Matrix3f eccentricityMatrixInverse;

	int atomIndex; // for lcaoCartoons

	final static int NO_ANISOTROPY = 1 << 5;
	final static int IS_SILENT = 1 << 6;
	final static int IS_SOLVENTTYPE = 1 << 7;
	final static int HAS_MAXGRID = 1 << 8;
	int dataType;
	int surfaceType;
	int mappingType;

	final static int SURFACE_NONE = 0;

	// getSurface only:
	final static int SURFACE_SPHERE = 1 | IS_SILENT;
	final static int SURFACE_ELLIPSOID = 2 | IS_SILENT;
	final static int SURFACE_LOBE = 3 | IS_SILENT;
	final static int SURFACE_LCAOCARTOON = 4 | IS_SILENT;

	final static int SURFACE_FUNCTIONXY = 5;

	// getSurface or mapColor:
	final static int SURFACE_SOLVENT = 11 | IS_SOLVENTTYPE | NO_ANISOTROPY;
	final static int SURFACE_SASURFACE = 12 | IS_SOLVENTTYPE | NO_ANISOTROPY;
	final static int SURFACE_MOLECULARORBITAL = 13 | NO_ANISOTROPY | HAS_MAXGRID;
	final static int SURFACE_ATOMICORBITAL = 14;
	final static int SURFACE_MEP = 16 | NO_ANISOTROPY | HAS_MAXGRID;
	final static int SURFACE_FILE = 17;
	final static int SURFACE_INFO = 18;
	final static int SURFACE_MOLECULAR = 19 | IS_SOLVENTTYPE | NO_ANISOTROPY;

	// mapColor only:

	final static int SURFACE_NOMAP = 20 | IS_SOLVENTTYPE | NO_ANISOTROPY;

	float solventRadius;
	float solventExtendedAtomRadius;
	float solventAtomRadiusFactor;
	float solventAtomRadiusAbsolute;
	float solventAtomRadiusOffset;
	boolean useIonic;
	boolean addHydrogens;

	int edgeFractionBase;
	int edgeFractionRange;
	int colorFractionBase;
	int colorFractionRange;
	float mappedDataMin;
	float mappedDataMax;

	float[] anisotropy = new float[3];
	final Point3f volumetricOrigin = new Point3f();
	final Vector3f[] volumetricVectors = new Vector3f[3];
	final Vector3f[] unitVolumetricVectors = new Vector3f[3];
	{
		volumetricVectors[0] = new Vector3f();
		volumetricVectors[1] = new Vector3f();
		volumetricVectors[2] = new Vector3f();
		unitVolumetricVectors[0] = new Vector3f();
		unitVolumetricVectors[1] = new Vector3f();
		unitVolumetricVectors[2] = new Vector3f();
	}
	final float[] volumetricVectorLengths = new float[3];

	final int[] voxelCounts = new int[3];
	final Matrix3f volumetricMatrix = new Matrix3f();
	float[][][] voxelData;

	int fileIndex; // one-based

	float cutoff = Float.MAX_VALUE;
	int nContours;
	int thisContour;
	boolean rangeDefined;
	float valueMappedToRed, valueMappedToBlue;

	boolean iAddGridPoints;
	boolean associateNormals;
	boolean newSolventMethod;
	boolean force2SidedTriangles;
	boolean isColorReversed;

	Point3f center;
	Point4f thePlane;
	boolean isContoured;
	boolean isBicolorMap;
	boolean isCutoffAbsolute;
	boolean isPositiveOnly;
	boolean isSilent;

	BufferedReader br;
	Hashtable surfaceInfo;
	BitSet bsSelected;
	BitSet bsIgnore;
	boolean iHaveBitSets;
	boolean iUseBitSets;

	void setProperty(String propertyName, Object value, BitSet bs) {

		Logger.debug("Isosurface state=" + state + " setProperty: " + propertyName + " = " + value);

		if ("init" == propertyName) {
			script = (String) value;
			initializeIsosurface();
			if (!(iHaveBitSets = getScriptBitSets()))
				bsSelected = bs; // THIS MAY BE NULL
			super.setProperty("thisID", null, null);
			return;
		}

		if ("title" == propertyName) {
			if (value == null) {
				title = null;
				return;
			}
			else if (value instanceof String[]) {
				title = (String[]) value;
			}
			else {
				int nLine = 1;
				String lines = (String) value;
				for (int i = lines.length(); --i >= 0;)
					if (lines.charAt(i) == '|')
						nLine++;
				title = new String[nLine];
				nLine = 0;
				int i0 = -1;
				for (int i = 0; i < lines.length(); i++)
					if (lines.charAt(i) == '|') {
						title[nLine++] = lines.substring(i0 + 1, i);
						i0 = i;
					}
				title[nLine] = lines.substring(i0 + 1);
			}
			for (int i = 0; i < title.length; i++)
				if (title[i].length() > 0)
					Logger.info("TITLE " + title[i]);
			return;
		}

		if ("debug" == propertyName) {
			boolean TF = ((Boolean) value).booleanValue();
			// Logger.setActiveLevel(Logger.LEVEL_DEBUG, TF);
			logMessages = TF;
			// logCompression = TF;
			logCube = TF;
			return;
		}

		if ("select" == propertyName) {
			if (!iHaveBitSets)
				bsSelected = (BitSet) value;
			return;
		}

		if ("ignore" == propertyName) {
			if (!iHaveBitSets)
				bsIgnore = (BitSet) value;
			return;
		}

		if ("cutoff" == propertyName) {
			cutoff = ((Float) value).floatValue();
			isPositiveOnly = false;
			return;
		}

		if ("cutoffPositive" == propertyName) {
			cutoff = ((Float) value).floatValue();
			isPositiveOnly = true;
			return;
		}

		if ("scale" == propertyName) {
			scale = ((Float) value).floatValue();
			return;
		}

		if ("angstroms" == propertyName) {
			isAngstroms = true;
			return;
		}

		if ("center" == propertyName) {
			center.set((Point3f) value);
			return;
		}

		if ("resolution" == propertyName) {
			resolution = ((Float) value).floatValue();
			if (resolution == 0)
				resolution = getDefaultResolution();
			return;
		}

		if ("anisotropy" == propertyName) {
			if ((dataType & NO_ANISOTROPY) != 0)
				return;
			Point3f pt = (Point3f) value;
			anisotropy[0] = pt.x;
			anisotropy[1] = pt.y;
			anisotropy[2] = pt.z;
			isAnisotropic = true;
			return;
		}

		if ("eccentricity" == propertyName) {
			setEccentricity((Point4f) value);
			return;
		}

		if ("addHydrogens" == propertyName) {
			addHydrogens = ((Boolean) value).booleanValue();
			return;
		}

		// / special effects

		if ("gridPoints" == propertyName) {
			iAddGridPoints = true;
			return;
		}

		if ("fixed" == propertyName) {
			isFixed = ((Boolean) value).booleanValue();
			setModelIndex();
			return;
		}

		if ("atomIndex" == propertyName) {
			atomIndex = ((Integer) value).intValue();
			return;
		}

		// / hidden options

		if ("fileIndex" == propertyName) {
			fileIndex = ((Integer) value).intValue();
			if (fileIndex < 1)
				fileIndex = 1;
			return;
		}

		if ("insideOut" == propertyName) {
			insideOut = true;
			return;
		}

		if ("remappable" == propertyName) {
			jvxlWritePrecisionColor = true;
			return;
		}

		// / color options

		if ("sign" == propertyName) {
			isCutoffAbsolute = true;
			colorBySign = true;
			colorPtr = 0;
			return;
		}

		if ("colorRGB" == propertyName) {
			int rgb = ((Integer) value).intValue();
			colorPos = colorPosLCAO = rgb;
			defaultColix = Graphics3D.getColix(rgb);
			if (colorPtr++ == 0)
				colorNeg = colorNegLCAO = rgb;
			return;
		}

		if ("red" == propertyName) {
			valueMappedToRed = ((Float) value).floatValue();
			return;
		}

		if ("blue" == propertyName) {
			valueMappedToBlue = ((Float) value).floatValue();
			rangeDefined = true;
			return;
		}

		if ("reverseColor" == propertyName) {
			isColorReversed = true;
			return;
		}

		if ("setColorScheme" == propertyName) {
			colorScheme = ((String) value);
			if (currentMesh != null && colorScheme.equals("sets")) {
				currentMesh.surfaceSet = getSurfaceSet(0);
				super.setProperty("color", "sets", null);
			}
			return;
		}

		if ("contour" == propertyName) {
			isContoured = true;
			int n = ((Integer) value).intValue();
			if (n > 0)
				nContours = n;
			else if (n == 0)
				nContours = defaultContourCount;
			else thisContour = -n;
			return;
		}

		if ("phase" == propertyName) {
			String color = (String) value;
			isCutoffAbsolute = true;
			colorBySign = true;
			colorByPhase = true;
			colorPhase = -1;
			for (int i = colorPhases.length; --i >= 0;)
				if (color.equalsIgnoreCase(colorPhases[i])) {
					colorPhase = i;
					break;
				}
			if (colorPhase < 0) {
				Logger.warn(" invalid color phase: " + color);
				colorPhase = 1;
			}
			if (logMessages)
				Logger.info("phase " + color + " " + colorPhase);
			if (state == STATE_DATA_READ) {
				dataType = surfaceType;
				state = STATE_DATA_COLORED;
				if (currentMesh != null)
					applyColorScale(currentMesh);
			}
			return;
		}

		if ("map" == propertyName) {
			if (currentMesh != null)
				state = STATE_DATA_READ;
			return;
		}

		// / final actions ///

		if ("plane" == propertyName) {
			thePlane = (Point4f) value;
			isContoured = true;
			++state;
			return;
		}

		if ("sphere" == propertyName) {
			sphere_radiusAngstroms = ((Float) value).floatValue();
			dataType = SURFACE_SPHERE;
			isSilent = !logMessages;
			setEccentricity(new Point4f(0, 0, 1, 1));
			cutoff = Float.MIN_VALUE;
			script = " center " + StateManager.escape(center) + " SPHERE " + sphere_radiusAngstroms;
			propertyName = "getSurface";
		}

		if ("ellipsoid" == propertyName) {
			Point4f v = (Point4f) value;
			setEccentricity(v);
			dataType = SURFACE_ELLIPSOID;
			sphere_radiusAngstroms = 1.0f;
			cutoff = Float.MIN_VALUE;
			propertyName = "getSurface";
			script = " center " + StateManager.escape(center) + (Float.isNaN(scale) ? "" : " scale " + scale)
					+ " ELLIPSOID {" + v.x + " " + v.y + " " + v.z + " " + v.w + "}";
		}

		if ("lobe" == propertyName) {
			Point4f v = (Point4f) value;
			setEccentricity(v);
			dataType = SURFACE_LOBE;
			if (cutoff == Float.MAX_VALUE)
				cutoff = defaultOrbitalCutoff;
			script = " center " + StateManager.escape(center) + (Float.isNaN(scale) ? "" : " scale " + scale)
					+ " LOBE {" + v.x + " " + v.y + " " + v.z + " " + v.w + "}";
			propertyName = "getSurface";
		}

		if ("lcaoType" == propertyName) {
			lcaoType = (String) value;
			if (colorPtr == 1)
				colorPosLCAO = colorNegLCAO;
			isSilent = !logMessages;
			return;
		}

		if ("lcaoCartoon" == propertyName) {
			if (++state != STATE_DATA_READ)
				return;
			Vector3f[] info = (Vector3f[]) value;
			// z x center
			if (center.x == Float.MAX_VALUE)
				center.set(info[2]);
			drawLcaoCartoon(lcaoType, info[0], info[1]);
		}

		/*
		 * Based on the form of the parameters, returns and encoded radius as follows:
		 * 
		 * script meaning range encoded
		 * 
		 * +1.2 offset [0 - 10] x -1.2 offset 0) x 1.2 absolute (0 - 10] x + 10 -30% 70% (-100 - 0) x + 200 +30% 130% (0
		 * x + 200 80% percent (0 x + 100
		 * 
		 * in each case, numbers can be integer or float
		 * 
		 */

		if ("vdwRadius" == propertyName || "ionicRadius" == propertyName) {
			useIonic = (propertyName.charAt(0) == 'i');
			float radius = ((Float) value).floatValue();
			if (radius >= 100)
				solventAtomRadiusFactor = (radius - 100) / 100;
			else if (radius > 10)
				solventAtomRadiusAbsolute = radius - 10;
			else solventAtomRadiusOffset = radius;
			return;
		}

		if ("molecular" == propertyName || "solvent" == propertyName || "sasurface" == propertyName
				|| "nomap" == propertyName) {
			// plain plane
			isEccentric = isAnisotropic = false;
			// anisotropy[0] = anisotropy[1] = anisotropy[2] = 1f;
			solventRadius = ((Float) value).floatValue();
			if (solventRadius < 0)
				solventRadius = defaultSolventRadius;
			dataType = ("nomap" == propertyName ? SURFACE_NOMAP : "molecular" == propertyName ? SURFACE_MOLECULAR
					: "sasurface" == propertyName || solventRadius == 0f ? SURFACE_SASURFACE : SURFACE_SOLVENT);
			switch (dataType) {
			case SURFACE_NOMAP:
				solventExtendedAtomRadius = solventRadius;
				solventRadius = 0f;
				isContoured = false;
				break;
			case SURFACE_MOLECULAR:
				solventExtendedAtomRadius = 0f;
				Logger.info("creating molecular surface with radius " + solventRadius);
				break;
			case SURFACE_SOLVENT:
				solventExtendedAtomRadius = 0f;
				if (bsIgnore == null)
					bsIgnore = viewer.getAtomBitSet("(solvent)");
				Logger.info("creating solvent-excluded surface with radius " + solventRadius);
				break;
			case SURFACE_SASURFACE:
				solventExtendedAtomRadius = solventRadius;
				solventRadius = 0f;
				if (bsIgnore == null)
					bsIgnore = viewer.getAtomBitSet("(solvent)");
				Logger.info("creating solvent-accessible surface with radius " + solventExtendedAtomRadius);
				break;
			}
			if (state == STATE_DATA_READ) {
				propertyName = "mapColor";
			}
			else {
				cutoff = 0.0f;
				propertyName = "getSurface";
			}
		}

		if ("moData" == propertyName) {
			moData = (Hashtable) value;
			propertyName = "mapColor";
			return;
		}

		if ("molecularOrbital" == propertyName) {
			qm_moNumber = ((Integer) value).intValue();
			qmOrbitalType = (moData.containsKey("gaussians") ? QM_TYPE_GAUSSIAN
					: moData.containsKey("slaterInfo") ? QM_TYPE_SLATER : QM_TYPE_UNKNOWN);
			if (qmOrbitalType == QM_TYPE_UNKNOWN) {
				Logger.error("moData does not contain data of a known type");
				return;
			}
			Vector mos = (Vector) (moData.get("mos"));
			qmOrbitalCount = mos.size();
			Logger.info("Molecular orbital #" + qm_moNumber + "/" + qmOrbitalCount + " "
					+ moData.get("calculationType"));
			Hashtable mo = (Hashtable) mos.get(qm_moNumber - 1);
			if (title == null) {
				title = new String[5];
				title[0] = "%F";
				title[1] = "Model %M  MO %I/%N";
				title[2] = "Energy = %E %U";
				title[3] = "?Symmetry = %S";
				title[4] = "?Occupancy = %O";
			}

			for (int i = title.length; --i >= 0;)
				addMOTitleInfo(i, mo);
			moCoefficients = (float[]) mo.get("coefficients");
			dataType = SURFACE_MOLECULARORBITAL;
			if (state == STATE_DATA_READ) {
				propertyName = "mapColor";
			}
			else {
				colorBySign = true;
				colorByPhase = true;
				colorPhase = 0;
				if (cutoff == Float.MAX_VALUE)
					cutoff = defaultQMOrbitalCutoff;
				isCutoffAbsolute = (cutoff > 0 && !isPositiveOnly);
				isBicolorMap = true;
				propertyName = "getSurface";
			}
		}

		if ("mep" == propertyName) {
			mepCharges = (float[]) value;
			isEccentric = isAnisotropic = false;
			dataType = SURFACE_MEP;
			if (state == STATE_DATA_READ) {
				if (!rangeDefined) {
					valueMappedToRed = defaultMepMin;
					valueMappedToBlue = defaultMepMax;
					rangeDefined = true;
				}
				propertyName = "mapColor";
			}
			else {
				colorBySign = true;
				colorByPhase = true;
				colorPhase = 0;
				if (cutoff == Float.MAX_VALUE)
					cutoff = defaultMepCutoff;
				isCutoffAbsolute = (cutoff > 0 && !isPositiveOnly);
				isBicolorMap = true;
				propertyName = "getSurface";
			}
		}

		if ("hydrogenOrbital" == propertyName) {
			dataType = SURFACE_ATOMICORBITAL;
			float[] nlmZ = (float[]) value;
			psi_n = (int) nlmZ[0];
			psi_l = (int) nlmZ[1];
			psi_m = (int) nlmZ[2];
			psi_Znuc = nlmZ[3];
			psi_ptsPerAngstrom = 10;
			// quantum rule is abs(m) <= l < n
			if (psi_Znuc <= 0 || Math.abs(psi_m) > psi_l || psi_l >= psi_n) {
				Logger.error("must have |m| <= l < n and Znuc > 0: " + psi_n + " " + psi_l + " " + psi_m + " "
						+ psi_Znuc);
				return;
			}
			if (state == STATE_DATA_READ) {
				propertyName = "mapColor";
			}
			else {
				if (cutoff == Float.MAX_VALUE)
					cutoff = defaultOrbitalCutoff;
				isCutoffAbsolute = true;
				if (colorBySign)
					isBicolorMap = true;
				propertyName = "getSurface";
			}
		}

		if ("functionXY" == propertyName) {
			dataType = SURFACE_FUNCTIONXY;
			functionXYinfo = (Vector) value;
			// if (state == STATE_DATA_READ) { for now, mapping function not allowed
			// propertyName = "mapColor";
			// } else {
			// isContoured = false;
			// I think the problem is that there's no direct way of knowing
			// that a function is being used with contours. And then, if
			// it is contoured, it should not be displayed.
			if (isContoured)
				setPlaneParameters(new Point4f(0, 0, 1, 0)); // xy plane through origin
			cutoff = Float.MIN_VALUE;
			isEccentric = isAnisotropic = false;
			propertyName = "getSurface";
			// }
		}

		if ("getSurface" == propertyName) {
			if (++state != STATE_DATA_READ)
				return;
			if (dataType == SURFACE_NONE) {
				if (value instanceof BufferedReader) {
					br = (BufferedReader) value;
					dataType = SURFACE_FILE;
				}
				else if (value instanceof Hashtable) {
					surfaceInfo = (Hashtable) value;
					dataType = SURFACE_INFO;
				}
				else {
					Logger.error("unknown surface data type??");
					return;
				}
				if (colorBySign)
					isBicolorMap = true;
			}
			surfaceType = dataType;
			if (!isSilent)
				Logger.info("loading voxel data...");
			checkFlags();
			long timeBegin = System.currentTimeMillis();
			if (!createIsosurface()) {
				Logger.error("Could not create isosurface");
				return;
			}
			if (!isSilent)
				Logger.info("surface calculation time: " + (System.currentTimeMillis() - timeBegin) + " ms");
			initializeMesh(force2SidedTriangles);
			jvxlFileMessage = (jvxlDataIsColorMapped ? "mapped" : "");
			if (isContoured && thePlane == null) {
				planarVectors[0].set(volumetricVectors[0]);
				planarVectors[1].set(volumetricVectors[1]);
				pixelCounts[0] = voxelCounts[0];
				pixelCounts[1] = voxelCounts[1];
			}
			if (jvxlDataIs2dContour)
				colorIsosurface();
			currentMesh.nBytes = nBytes;
			if (colorByPhase || colorBySign) {
				state = STATE_DATA_COLORED;
				mappingType = dataType;
				applyColorScale(currentMesh);
			}
			if (colorScheme.equals("sets")) {
				currentMesh.surfaceSet = getSurfaceSet(0);
				super.setProperty("color", "sets", null);
			}
			setModelIndex();
			if (logMessages && thePlane == null && !isSilent)
				Logger.debug("\n" + jvxlGetFile(currentMesh, jvxlFileMessage, true, 1));
			discardTempData(jvxlDataIs2dContour);
			dataType = SURFACE_NONE;
			mappedDataMin = Float.MAX_VALUE;
			return;
		}

		if ("mapColor" == propertyName) {
			if (++state != STATE_DATA_COLORED)
				return;
			if (!isSilent)
				Logger.info("mapping data...");
			if (dataType == SURFACE_NONE) {
				if (value instanceof BufferedReader) {
					br = (BufferedReader) value;
					dataType = SURFACE_FILE;
				}
				else if (value instanceof Hashtable) {
					surfaceInfo = (Hashtable) value;
					dataType = SURFACE_INFO;
				}
				else {
					Logger.error("unknown surface data type??");
					return;
				}
			}
			mappingType = dataType;
			checkFlags();
			if (thePlane != null) {
				createIsosurface(); // for the plane
				initializeMesh(true);
				readVolumetricData(true); // for the data
				colorIsosurface();
			}
			else {
				readData(true);
				if (jvxlDataIsColorMapped) {
					jvxlReadColorData(currentMesh);
				}
				else {
					colorIsosurface();
				}
			}
			currentMesh.nBytes = nBytes;
			if (logMessages && !isSilent)
				Logger.debug("\n" + jvxlGetFile(currentMesh, jvxlFileMessage, true, 1));
			setModelIndex();
			discardTempData(true);
			dataType = SURFACE_NONE;
			return;
		}

		if ("delete" == propertyName) {
			if (currentMesh == null)
				nLCAO = 0;
			// fall through to meshCollection
		}

		// processed by meshCollection
		super.setProperty(propertyName, value, bs);
	}

	Object getProperty(String property, int index) {
		if (property == "moNumber")
			return new Integer(qm_moNumber);
		if (currentMesh == null)
			return "no current isosurface";
		if (property == "jvxlFileData")
			return jvxlGetFile(currentMesh, "", true, index);
		if (property == "jvxlSurfaceData")
			return jvxlGetFile(currentMesh, "", false, 1);
		return super.getProperty(property, index);
	}

	boolean getScriptBitSets() {
		if (script == null)
			return false;
		int i = script.indexOf("# ({");
		if (i < 0)
			return false;
		int j = script.indexOf("})", i);
		bsSelected = StateManager.unescapeBitset(script.substring(i + 3, j + 1));
		if ((i = script.indexOf("({", j)) < 0)
			return false;
		j = script.indexOf("})", i);
		bsIgnore = StateManager.unescapeBitset(script.substring(i + 1, j + 1));
		return true;
	}

	String fixScript() {
		if (script.indexOf("# ({") >= 0)
			return script;
		if (script.charAt(0) == ' ')
			return myType + " " + currentMesh.thisID + script;
		if (!iUseBitSets)
			return script;
		return script + "# " + (bsSelected == null ? "({null})" : StateManager.escape(bsSelected)) + " "
				+ (bsIgnore == null ? "({null})" : StateManager.escape(bsIgnore));
	}

	void initializeIsosurface() {
		logMessages = Logger.isActiveLevel(Logger.LEVEL_DEBUG);
		logCube = logCompression = false;
		isSilent = false;
		title = null;
		fileIndex = 1;
		insideOut = false;
		isFixed = false;
		atomIndex = -1;
		precalculateVoxelData = false;
		isColorReversed = false;
		iAddGridPoints = false;
		newSolventMethod = true;
		associateNormals = true;
		force2SidedTriangles = true;
		colorBySign = colorByPhase = false;
		defaultColix = 0;
		colorNeg = defaultColorNegative;
		colorPos = defaultColorPositive;
		colorNegLCAO = defaultColorNegativeLCAO;
		colorPosLCAO = defaultColorPositiveLCAO;
		colorScheme = "roygb";
		addHydrogens = false;
		isEccentric = isAnisotropic = false;
		scale = Float.NaN;
		isAngstroms = false;
		resolution = Float.MAX_VALUE;
		center = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		// anisotropy[0] = anisotropy[1] = anisotropy[2] = 1f;
		cutoff = Float.MAX_VALUE;
		thePlane = null;
		nBytes = 0;
		nContours = 0;
		colorPtr = 0;
		thisContour = -1;
		isContoured = false;
		rangeDefined = false;
		mappedDataMin = Float.MAX_VALUE;
		isBicolorMap = isCutoffAbsolute = isPositiveOnly = false;
		precalculateVoxelData = false;
		bsIgnore = null;
		iUseBitSets = false;
		solventExtendedAtomRadius = 0;
		solventAtomRadiusFactor = 1;
		solventAtomRadiusAbsolute = 0;
		solventAtomRadiusOffset = 0;
		useIonic = false;
		jvxlInitFlags();
		initState();
	}

	void initState() {
		state = STATE_INITIALIZED;
		assocGridPointMap = new Hashtable();
		assocGridPointNormals = new Hashtable();
		dataType = surfaceType = mappingType = SURFACE_NONE;
	}

	void setEccentricity(Point4f info) {
		/*
		 * {cx cy cz fab/c}
		 * 
		 * 1) set ecc = {cx cy cz} 2) normalize 3) add z and normalize again. This gives the vector about which a
		 * 180-degree rotation turns {0 0 1} into ecc.
		 * 
		 */
		Vector3f ecc = new Vector3f(info.x, info.y, info.z);
		float c = (scale > 0 ? scale : info.w < 0 ? 1f : ecc.length());
		float fab_c = Math.abs(info.w);
		ecc.normalize();
		Vector3f z = new Vector3f(0, 0, 1);
		ecc.add(z);
		ecc.normalize();
		if (Float.isNaN(ecc.x)) // was exactly {0 0 -1} -- just rotate about x
			ecc.set(1, 0, 0);
		eccentricityMatrix = new Matrix3f();
		eccentricityMatrix.setIdentity();
		eccentricityMatrix.set(new AxisAngle4f(ecc, (float) Math.PI));
		eccentricityMatrixInverse = new Matrix3f();
		eccentricityMatrixInverse.invert(eccentricityMatrix);
		isEccentric = isAnisotropic = true;
		eccentricityScale = c;
		eccentricityRatio = fab_c;
		if (fab_c > 1)
			eccentricityScale *= fab_c;
		anisotropy[0] = fab_c * c;
		anisotropy[1] = fab_c * c;
		anisotropy[2] = c;
		if (center.x == Float.MAX_VALUE)
			center.set(0, 0, 0);
	}

	void colorIsosurface() {
		setMapRanges();
		if (isContoured) { // did NOT work here.
			generateContourData(jvxlDataIs2dContour);
			initializeMesh(true);
			if (!colorByContourOnly)
				applyColorScale(currentMesh);
		}
		else {
			applyColorScale(currentMesh);
		}
		currentMesh.jvxlExtraLine = jvxlExtraLine(1);
		jvxlFileMessage = "mapped: min = " + valueMappedToRed + "; max = " + valueMappedToBlue;
	}

	void setMapRanges() {
		// ["mapColor" | "getSurface/jvxl-2dContour] --> colorIsosurface
		// ["phase" | colorIsoSurface] --> applyColorScale
		// colorIsosurface --> generateContourData --> createContours
		if (colorByPhase || colorBySign || isBicolorMap && !isContoured) {
			mappedDataMin = -1;
			mappedDataMax = 1;
		}
		if (mappedDataMin == Float.MAX_VALUE || mappedDataMin == mappedDataMax) {
			mappedDataMin = getMinMappedValue();
			mappedDataMax = getMaxMappedValue();
		}
		if (logMessages)
			Logger.debug("setMapRanges: all mapped data " + mappedDataMin + " to " + mappedDataMax
					+ ", red-blue selected " + valueMappedToRed + " to " + valueMappedToBlue);
		if (mappedDataMin == 0 && mappedDataMax == 0) {
			// just set default -1/1 if there is no obvious data
			mappedDataMin = -1;
			mappedDataMax = 1;
		}

		if (!rangeDefined) {
			valueMappedToRed = mappedDataMin;
			valueMappedToBlue = mappedDataMax;
		}
		if (logMessages)
			Logger.debug("setMapRanges: " + mappedDataMin + " " + mappedDataMax + " " + valueMappedToRed + " "
					+ valueMappedToBlue);
		currentMesh.valueMappedToRed = valueMappedToRed;
		currentMesh.valueMappedToBlue = valueMappedToBlue;
		currentMesh.mappedDataMin = mappedDataMin;
		currentMesh.mappedDataMax = mappedDataMax;
	}

	void checkFlags() {
		if (viewer.getTestFlag1()) // turn off new solvent method
			newSolventMethod = false;
		if (viewer.getTestFlag2())
			associateNormals = false;
		if (viewer.getTestFlag4()) // turn off 2-sided if showing normals
			force2SidedTriangles = false;
		if (logMessages) {
			Logger.debug("Isosurface using testflag4: no 2-sided triangles = " + !force2SidedTriangles);
			Logger.debug("Isosurface using testflag2: no associative grouping = " + !associateNormals);
			Logger.debug("IsosurfaceRenderer using testflag3: separated triangles = " + viewer.getTestFlag3());
			Logger.debug("IsosurfaceRenderer using testflag4: show vertex normals = " + viewer.getTestFlag4());
			Logger.debug("For grid points, use: isosurface delete myiso gridpoints \"\"");
		}
	}

	boolean createIsosurface() {
		resetIsosurface();
		try {
			readData(false);
			calcVoxelVertexVectors();
			generateSurfaceData();
		}
		catch (Exception e) {
			return false;
		}
		currentMesh.jvxlFileHeader = "" + jvxlFileHeader;
		currentMesh.cutoff = (isJvxl ? jvxlCutoff : cutoff);
		currentMesh.jvxlColorData = "";
		currentMesh.jvxlEdgeData = "" + fractionData;
		currentMesh.isBicolorMap = isBicolorMap;
		currentMesh.isContoured = isContoured;
		currentMesh.nContours = nContours;
		if (jvxlDataIsColorMapped)
			jvxlReadColorData(currentMesh);
		currentMesh.colix = getDefaultColix();
		currentMesh.jvxlExtraLine = jvxlExtraLine(1);
		if (thePlane != null && iAddGridPoints)
			addGridPointCube();
		return true;
	}

	void resetIsosurface() {
		if (currentMesh == null)
			allocMesh(null);
		currentMesh.clear("isosurface");
		contourVertexCount = 0;
		currentMesh.firstViewableVertex = 0;
		if (iAddGridPoints) {
			currentMesh.showPoints = true;
			currentMesh.hasGridPoints = true;
		}
		if (cutoff == Float.MAX_VALUE)
			cutoff = defaultCutoff;
		currentMesh.jvxlSurfaceData = "";
		currentMesh.jvxlEdgeData = "";
		currentMesh.jvxlColorData = "";
		edgeCount = 0;
	}

	void addGridPointCube() {
		for (int x = 0; x < voxelCounts[0]; x += 5)
			for (int y = 0; y < voxelCounts[1]; y += 5)
				for (int z = 0; z < voxelCounts[2]; z += 5) {
					Point3f pt = new Point3f();
					addVertexCopy(pt, 0, false, "");
				}
	}

	boolean isJvxl;
	boolean endOfData;

	void readData(boolean isMapData) {
		isJvxl = false;
		endOfData = false;
		mappedDataMin = Float.MAX_VALUE;
		int nSurfaces = readVolumetricHeader();
		if (nSurfaces < fileIndex) {
			Logger.warn("not enough surfaces in file -- resetting fileIndex to " + nSurfaces);
			fileIndex = nSurfaces;
		}
		if (isJvxl && isMapData)
			try {
				int nPoints = nPointsX * nPointsY * nPointsZ;
				gotoData(fileIndex - 1, nPoints);
				jvxlSkipData(nPoints, false);
			}
			catch (Exception e) {
				Logger.error(null, e);
			}
		else readVolumetricData(isMapData);
	}

	void discardTempData(boolean discardAll) {
		voxelData = null;
		if (dataType == SURFACE_FILE)
			try {
				br.close();
			}
			catch (Exception e) {
			}
		if (!discardAll)
			return;
		assocGridPointMap = null;
		assocGridPointNormals = null;
		pixelData = null;
		planarSquares = null;
		contourVertexes = null;
		contourVertexCount = 0;
	} // //////////////////////////////////////////////////////////////

	// default color stuff
	// //////////////////////////////////////////////////////////////

	int indexColorPositive;
	int indexColorNegative;

	short getDefaultColix() {
		if (defaultColix != 0)
			return defaultColix;
		int argb;
		if (cutoff >= 0) {
			indexColorPositive = (indexColorPositive % JmolConstants.argbsIsosurfacePositive.length);
			argb = JmolConstants.argbsIsosurfacePositive[indexColorPositive++];
		}
		else {
			indexColorNegative = (indexColorNegative % JmolConstants.argbsIsosurfaceNegative.length);
			argb = JmolConstants.argbsIsosurfaceNegative[indexColorNegative++];
		}
		return Graphics3D.getColix(argb);
	}

	// //////////////////////////////////////////////////////////////
	// CUBE/JVXL file reading stuff
	// //////////////////////////////////////////////////////////////

	int readVolumetricHeader() {
		precalculateVoxelData = false;
		try {
			switch (dataType) {
			case SURFACE_SPHERE:
			case SURFACE_ELLIPSOID:
				setupSphere();
				break;
			case SURFACE_LOBE:
				setupLobe();
				break;
			case SURFACE_NOMAP:
			case SURFACE_SOLVENT:
			case SURFACE_MOLECULAR:
			case SURFACE_SASURFACE:
				setupSolvent();
				break;
			case SURFACE_ATOMICORBITAL:
				setupOrbital();
				break;
			case SURFACE_MOLECULARORBITAL:
				setupQMOrbital();
				break;
			case SURFACE_FUNCTIONXY:
				setupFunctionXY();
				break;
			case SURFACE_MEP:
				setupMep();
				break;
			case SURFACE_INFO:
				setupSurfaceInfo();
				break;
			case SURFACE_FILE:
			default:
				readTitleLines();
				readAtomCountAndOrigin();
				if (!isSilent)
					Logger.debug("voxel grid origin:" + volumetricOrigin);
				for (int i = 0; i < 3; ++i) {
					readVoxelVector(i);
					if (!isSilent)
						Logger.debug("voxel grid vector:" + volumetricVectors[i]);
				}
			}

			setupMatrix(volumetricMatrix, volumetricVectors);
			if (dataType != SURFACE_LOBE && center.x != Float.MAX_VALUE)
				offsetCenter();
			readAtoms();
			return readExtraLine();
		}
		catch (Exception e) {
			Logger.error(null, e);
			throw new NullPointerException();
		}
	}

	void readVolumetricData(boolean isMapData) {
		try {
			readVoxelData(isMapData);
			if (isJvxl && jvxlEdgeDataCount > 0)
				jvxlEdgeDataRead = jvxlReadData("edge", jvxlEdgeDataCount);
			if (isJvxl && jvxlColorDataCount > 0)
				jvxlColorDataRead = jvxlReadData("color", jvxlColorDataCount);

		}
		catch (Exception e) {
			Logger.error(null, e);
			throw new NullPointerException();
		}
	}

	StringBuffer jvxlFileHeader = new StringBuffer();
	String jvxlFileMessage;
	String jvxlEdgeDataRead;
	String jvxlColorDataRead;

	void readTitleLines() throws Exception {
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append(br.readLine());
		jvxlFileHeader.append('\n');
		jvxlFileHeader.append(br.readLine());
		jvxlFileHeader.append('\n');
		if (!isSilent)
			Logger.info("" + jvxlFileHeader);
	}

	int atomCount;
	boolean negativeAtomCount;

	void readAtomCountAndOrigin() throws Exception {
		line = br.readLine();
		if (!isSilent)
			Logger.debug(line);
		atomCount = parseInt(line);
		String atomLine = line.substring(ichNextParse);
		negativeAtomCount = (atomCount < 0);
		if (!isSilent)
			Logger.debug("atom Count: " + atomCount);

		if (negativeAtomCount)
			atomCount = -atomCount;

		int jvxlAtoms = (atomCount == 0 ? -2 : -atomCount);
		volumetricOrigin.set(parseFloat(), parseFloat(), parseFloat());
		if (!isAngstroms)
			volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
		jvxlFileHeader.append(jvxlAtoms + atomLine + '\n');
	}

	void readVoxelVector(int voxelVectorIndex) throws Exception {
		line = br.readLine();
		jvxlFileHeader.append(line);
		jvxlFileHeader.append('\n');
		Vector3f voxelVector = volumetricVectors[voxelVectorIndex];
		voxelCounts[voxelVectorIndex] = parseInt(line);
		voxelVector.set(parseFloat(), parseFloat(), parseFloat());
		if (!isAngstroms)
			voxelVector.scale(ANGSTROMS_PER_BOHR);
		volumetricVectorLengths[voxelVectorIndex] = voxelVector.length();
		unitVolumetricVectors[voxelVectorIndex].normalize(voxelVector);
		for (int i = 0; i < voxelVectorIndex; i++) {
			float orthoTest = Math.abs(unitVolumetricVectors[i].dot(unitVolumetricVectors[voxelVectorIndex]));
			if (orthoTest > 1.001 || orthoTest < 0.999 && orthoTest > 0.001)
				Logger.warn("Warning: voxel coordinate vectors are not orthogonal.");
		}
	}

	void setupMatrix(Matrix3f mat, Vector3f[] cols) {
		for (int i = 0; i < 3; i++)
			mat.setColumn(i, cols[i]);
	}

	void readAtoms() throws Exception {
		for (int i = 0; i < atomCount; ++i)
			jvxlFileHeader.append(br.readLine() + "\n");
		if (atomCount == 0) {
			Point3f pt = new Point3f(volumetricOrigin);
			jvxlFileHeader
					.append("1 1.0 " + pt.x + " " + pt.y + " " + pt.z + " //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
			for (int i = 0; i < 3; i++)
				pt.scaleAdd(voxelCounts[i] - 1, volumetricVectors[i], pt);
			jvxlFileHeader.append("2 2.0 " + pt.x + " " + pt.y + " " + pt.z
					+ " //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
		}
	}

	int readExtraLine() throws Exception {
		int nSurfaces;
		edgeFractionBase = defaultEdgeFractionBase;
		edgeFractionRange = defaultEdgeFractionRange;
		colorFractionBase = defaultColorFractionBase;
		colorFractionRange = defaultColorFractionRange;
		if (negativeAtomCount) {
			line = br.readLine();
			Logger.info("Reading extra orbital/JVXL information line: " + line);
			nSurfaces = parseInt(line);
			isJvxl = (nSurfaces < 0);
			if (isJvxl) {
				nSurfaces = -nSurfaces;
				Logger.info("jvxl file surfaces: " + nSurfaces);
				int ich;
				if ((ich = parseInt()) == Integer.MIN_VALUE) {
					Logger.info("using default edge fraction base and range");
				}
				else {
					edgeFractionBase = ich;
					edgeFractionRange = parseInt();
				}
				if ((ich = parseInt()) == Integer.MIN_VALUE) {
					Logger.info("using default color fraction base and range");
				}
				else {
					colorFractionBase = ich;
					colorFractionRange = parseInt();
				}
			}
		}
		else {
			nSurfaces = 1;
		}
		return nSurfaces;
	}

	int nBytes;
	int nDataPoints;
	String surfaceData;
	int nPointsX, nPointsY, nPointsZ;

	void readVoxelData(boolean isMapData) throws Exception {
		/*
		 * possibilities:
		 * 
		 * cube file data only -- monochrome surface (single pass) cube file with plane (color, two pass) cube file data +
		 * cube file color data (two pass) jvxl file no color data (single pass) jvxl file with color data (single pass)
		 * jvxl file with plane (single pass)
		 * 
		 * 
		 */
		/*
		 * This routine is used twice in the case of color mapping. First (isMapData = false) to read the surface
		 * values, which might be a plane, then (isMapData = true) to color them based on a second data set.
		 * 
		 * Planes are only compatible with data sets that return actual numbers at all grid points -- cube files,
		 * orbitals, functionXY -- not solvent, spheres, elipsoids, or lobes.
		 * 
		 * It is possible to map a QM orbital onto a plane. In the first pass we defined the plane; in the second pass
		 * we just calculate the new voxel values and return.
		 * 
		 */
		boolean inside = false;
		int dataCount = 0;
		ichNextParse = 0;
		nThisValue = 0;
		surfaceData = "";
		nPointsX = voxelCounts[0];
		nPointsY = voxelCounts[1];
		nPointsZ = voxelCounts[2];
		int nPoints = nPointsX * nPointsY * nPointsZ;
		if (nPointsX <= 0 || nPointsY <= 0 || nPointsZ <= 0)
			return;
		if (!isSilent)
			Logger.debug("entering readVoxelData for fileIndex = " + fileIndex + "; " + nPoints
					+ " data points mapping=" + isMapData);

		// skip to the correct dataset and read the JVXL definition line if present
		// several JVXL variables and the plane will be defined here
		gotoData(fileIndex - 1, nPoints);

		thisInside = (!isJvxl || !isContoured);
		if (insideOut)
			thisInside = !thisInside;

		if (thePlane != null) {
			setPlaneParameters(thePlane);
			cutoff = 0f;
		}
		else if (isJvxl) {
			cutoff = (isBicolorMap || colorBySign ? 0.01f : 0.5f);
		}
		if (!isSilent)
			Logger.info("isosurface cutoff = " + cutoff);

		boolean justDefiningPlane = (!isMapData && thePlane != null);
		boolean isPrecalculation = (precalculateVoxelData && !justDefiningPlane);
		if (dataType == SURFACE_INFO) {
			if (justDefiningPlane) {
				voxelData = new float[nPointsX][][];
			}
			else {
				voxelData = tempVoxelData;
				tempVoxelData = null;
			}
		}
		else {
			voxelData = new float[nPointsX][][];
			if (isPrecalculation) {
				for (int x = 0; x < nPointsX; ++x) {
					voxelData[x] = new float[nPointsY][];
					for (int y = 0; y < nPointsY; ++y)
						voxelData[x][y] = new float[nPointsZ];
				}
				if (dataType == SURFACE_MOLECULARORBITAL)
					generateQuantumCube();
				else if (dataType == SURFACE_MEP)
					generateMepCube();
				else if ((dataType & IS_SOLVENTTYPE) != 0)
					generateSolventCube();
				else Logger.error("code error -- isPrecalculation, but how?");
				if (isMapData || thePlane != null)
					return;
			}
		}
		nDataPoints = 0;
		float zValue = 0;
		line = "";
		for (int x = 0; x < nPointsX; ++x) {
			float[][] plane;
			if (isPrecalculation) {
				plane = voxelData[x];
			}
			else {
				plane = new float[nPointsY][];
				voxelData[x] = plane;
			}
			for (int y = 0; y < nPointsY; ++y) {
				float[] strip;
				if (isPrecalculation) {
					strip = plane[y];
				}
				else {
					strip = new float[nPointsZ];
					plane[y] = strip;
				}
				if (dataType == SURFACE_FUNCTIONXY)
					zValue = getFunctionValue(x, y);
				for (int z = 0; z < nPointsZ; ++z) {
					float voxelValue;
					if (justDefiningPlane) {
						voxelValue = calcVoxelPlaneDistance(x, y, z);
					}
					else {
						switch (dataType) {
						case SURFACE_SPHERE:
						case SURFACE_ELLIPSOID:
							voxelValue = getSphereValue(x, y, z);
							break;
						case SURFACE_LOBE:
							voxelValue = getLobeValue(x, y, z);
							break;
						case SURFACE_MOLECULAR:
						case SURFACE_SOLVENT:
						case SURFACE_SASURFACE:
							if (isPrecalculation)
								voxelValue = strip[z]; // precalculated
							else
							// old way (for testing)
							voxelValue = getSolventValue(x, y, z);
							break;
						case SURFACE_ATOMICORBITAL:
							voxelValue = getPsi(x, y, z);
							break;
						case SURFACE_MOLECULARORBITAL:
						case SURFACE_MEP:
							voxelValue = strip[z]; // precalculated
							break;
						case SURFACE_FUNCTIONXY:
							voxelValue = (thePlane == null ? zValue - z : zValue);
							break;
						case SURFACE_INFO:
							voxelValue = voxelData[x][y][z];
							break;
						case SURFACE_NOMAP:
							voxelValue = 0f;
							break;
						case SURFACE_FILE:
						default:
							voxelValue = getNextVoxelValue();
						}
					}
					strip[z] = voxelValue;
					++nDataPoints;
					if (isJvxl && thePlane == null || isMapData)
						continue;

					// update surfaceData

					if (logCube)
						if (x < 20 && y < 20 && z < 20) {
							voxelPtToXYZ(x, y, z, ptXyzTemp);
							Logger.info("voxelData[" + x + "][" + y + "][" + z + "] xyz(Angstroms)=" + ptXyzTemp
									+ " value=" + voxelValue);
						}
					if (inside == isInside(voxelValue, cutoff)) {
						dataCount++;
					}
					else {
						if (dataCount != 0)
							surfaceData += " " + dataCount;
						dataCount = 1;
						inside = !inside;
					}
				}
			}
		}
		if (!isJvxl)
			surfaceData += " " + dataCount + "\n";
		if (!isMapData) {
			currentMesh.jvxlSurfaceData = (thePlane == null ? surfaceData : "");
			currentMesh.jvxlPlane = thePlane;
		}
		if (!isSilent)
			Logger.debug("Successfully read " + nPointsX + " x " + nPointsY + " x " + nPointsZ + " data points; "
					+ edgeCount + " edges");
	}

	final Vector3f thePlaneNormal = new Vector3f();
	float thePlaneNormalMag;

	void setPlaneParameters(Point4f plane) {
		if (plane.x + plane.y + plane.z == 0)
			plane.z = 1; // {0 0 0 w} becomes {0 0 1 w}
		thePlaneNormal.set(plane.x, plane.y, plane.z);
		thePlaneNormalMag = thePlaneNormal.length();
	}

	final Point3f ptXyzTemp = new Point3f();

	float calcVoxelPlaneDistance(int x, int y, int z) {
		voxelPtToXYZ(x, y, z, ptXyzTemp);
		return distancePointToPlane(ptXyzTemp, thePlane);
	}

	float distancePointToPlane(Point3f pt, Point4f plane) {
		return (plane.x * pt.x + plane.y * pt.y + plane.z * pt.z + plane.w) / thePlaneNormalMag;
	}

	int jvxlSurfaceDataCount;
	int jvxlEdgeDataCount;
	int jvxlColorDataCount;
	boolean jvxlDataIsColorMapped;
	boolean jvxlDataisBicolorMap;
	boolean jvxlDataIsPrecisionColor;
	boolean jvxlWritePrecisionColor;
	boolean jvxlDataIs2dContour;
	float jvxlCutoff;

	void jvxlInitFlags() {
		jvxlEdgeDataRead = "";
		jvxlColorDataRead = "";
		jvxlDataIs2dContour = false;
		jvxlDataIsColorMapped = false;
		jvxlDataIsPrecisionColor = false;
		jvxlDataisBicolorMap = false;
		jvxlWritePrecisionColor = false;
	}

	void jvxlReadDefinitionLine(boolean showMsg) throws Exception {
		while ((line = br.readLine()) != null && line.length() == 0 || line.charAt(0) == '#') {
		}
		if (showMsg)
			Logger.info("reading jvxl data set: " + line);

		jvxlCutoff = parseFloat(line);
		Logger.info("JVXL read: cutoff " + jvxlCutoff);

		// cutoff param1 param2 param3
		// | | |
		// when | | > 0 ==> 1-byte jvxlDataIsColorMapped
		// when | | == -1 ==> not color mapped
		// when | | < -1 ==> 2-byte jvxlDataIsPrecisionColor
		// when == -1 && == -1 ==> noncontoured plane
		// when == -1 && == -2 ==> contourable plane
		// when < -1 && > 0 ==> contourable functionXY
		// when > 0 && < 0 ==> jvxlDataisBicolorMap

		// early on I wasn't contouring planes, so it's possible that a plane would
		// not be contoured (-1 -1), but that is NOT a possibility anymore with Jmol.
		// instead, we just set "contour 1" to indicate just one contour to demo that.
		// In addition, now we consider contouring functionXY, so in that case we would
		// have surface data, edge data, and color data

		int param1 = parseInt();
		int param2 = parseInt();
		int param3 = parseInt();
		if (param3 == Integer.MIN_VALUE || param3 == -1)
			param3 = 0;

		if (param1 == -1) {
			// a plane is defined
			try {
				thePlane = new Point4f(parseFloat(), parseFloat(), parseFloat(), parseFloat());
			}
			catch (Exception e) {
				Logger.error("Error reading 4 floats for PLANE definition -- setting to 0 0 1 0  (z=0)");
				thePlane = new Point4f(0, 0, 1, 0);
			}
			Logger.info("JVXL read: {" + thePlane.x + " " + thePlane.y + " " + thePlane.z + " " + thePlane.w + "}");
		}
		else {
			thePlane = null;
		}
		if (param1 < 0 && param2 != -1) {
			isContoured = (param3 != 0);
			// contours are defined (possibly overridden -- this is just a display option
			// could be plane or functionXY
			int nContoursRead = parseInt();
			if (nContours == 0 && nContoursRead != Integer.MIN_VALUE && nContoursRead != 0
					&& nContoursRead <= nContourMax) {
				nContours = nContoursRead;
				Logger.info("JVXL read: contours " + nContours);
			}
		}
		else {
			isContoured = false;
		}

		jvxlDataIsPrecisionColor = (param1 == -1 && param2 == -2 || param3 < 0);
		isBicolorMap = jvxlDataisBicolorMap = (param1 > 0 && param2 < 0);
		jvxlDataIsColorMapped = (param3 != 0);
		jvxlDataIs2dContour = (jvxlDataIsColorMapped && isContoured);

		if (isBicolorMap || colorBySign)
			jvxlCutoff = 0;
		jvxlSurfaceDataCount = (param1 < -1 ? -param1 : param1 > 0 ? param1 : 0);
		if (param1 == -1)
			jvxlEdgeDataCount = 0; // plane
		else jvxlEdgeDataCount = (param2 < -1 ? -param2 : param2 > 0 ? param2 : 0);
		jvxlColorDataCount = (param3 < -1 ? -param3 : param3 > 0 ? param3 : 0);

		if (jvxlDataIsColorMapped) {
			float dataMin = parseFloat();
			float dataMax = parseFloat();
			float red = parseFloat();
			float blue = parseFloat();
			if (!Float.isNaN(dataMin) && !Float.isNaN(dataMax)) {
				if (dataMax == 0 && dataMin == 0) {
					// set standard -1/1; bit of a hack
					dataMin = -1;
					dataMax = 1;
				}
				mappedDataMin = dataMin;
				mappedDataMax = dataMax;
				Logger.info("JVXL read: data min/max: " + mappedDataMin + "/" + mappedDataMax);
			}
			if (!rangeDefined)
				if (!Float.isNaN(red) && !Float.isNaN(blue)) {
					if (red == 0 && blue == 0) {
						// set standard -1/1; bit of a hack
						red = -1;
						blue = 1;
					}
					valueMappedToRed = red;
					valueMappedToBlue = blue;
					rangeDefined = true;
				}
				else {
					valueMappedToRed = 0f;
					valueMappedToBlue = 1f;
					rangeDefined = true;
				}
			Logger.info("JVXL read: color red/blue: " + valueMappedToRed + " " + valueMappedToBlue);
		}
	}

	int nThisValue;
	boolean thisInside;

	float getNextVoxelValue() throws Exception {
		if (isJvxl) {
			if (jvxlSurfaceDataCount <= 0)
				return 0f; // unnecessary -- probably a plane
			if (nThisValue == 0) {
				nThisValue = parseInt();
				if (nThisValue == Integer.MIN_VALUE) {
					line = br.readLine();
					if (line == null || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
						if (!endOfData)
							Logger.error("end of file in JvxlReader?" + " line=" + line);
						endOfData = true;
						nThisValue = 10000;
						// throw new NullPointerException();
					}
					else {
						surfaceData += line + "\n";
					}
				}
				thisInside = !thisInside;
			}
			--nThisValue;
			return (thisInside ? 1f : 0f);
		}
		float voxelValue = parseFloat();
		if (Float.isNaN(voxelValue)) {
			line = br.readLine();
			if (line == null || Float.isNaN(voxelValue = parseFloat(line))) {
				if (!endOfData)
					Logger.warn("end of file reading cube voxel data? nBytes=" + nBytes + " nDataPoints=" + nDataPoints
							+ " (line):" + line);
				endOfData = true;
				line = "0 0 0 0 0 0 0 0 0 0";
			}
			nBytes += line.length() + 1;
		}
		return voxelValue;
	}

	void gotoData(int n, int nPoints) throws Exception {
		if (n > 0)
			Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
		for (int i = 0; i < n; i++)
			if (isJvxl) {
				jvxlReadDefinitionLine(true);
				Logger.info("JVXL skipping: jvxlSurfaceDataCount=" + jvxlSurfaceDataCount + " jvxlEdgeDataCount="
						+ jvxlEdgeDataCount + " jvxlDataIsColorMapped=" + jvxlDataIsColorMapped);
				jvxlSkipData(nPoints, true);
			}
			else {
				skipData(nPoints, true);
			}
		if (isJvxl)
			jvxlReadDefinitionLine(true);
	}

	void jvxlSkipData(int nPoints, boolean doSkipColorData) throws Exception {
		if (jvxlSurfaceDataCount > 0)
			skipData(nPoints, true);
		if (jvxlEdgeDataCount > 0)
			skipData(jvxlEdgeDataCount, false);
		if (jvxlDataIsColorMapped && doSkipColorData)
			skipData(jvxlColorDataCount, false);
	}

	void skipData(int nPoints, boolean isInt) throws Exception {
		int iV = 0;
		while (iV < nPoints) {
			line = br.readLine();
			iV += (isInt ? countData(line) : jvxlUncompressString(line).length());
		}
	}

	String jvxlReadData(String type, int nPoints) {
		String str = "";
		try {
			while (str.length() < nPoints) {
				line = br.readLine();
				str += jvxlUncompressString(line);
			}
		}
		catch (Exception e) {
			Logger.error("Error reading " + type + " data " + e);
			throw new NullPointerException();
		}
		return str;
	}

	int countData(String str) {
		int count = 0;
		if (isJvxl) {
			int n = parseInt(str);
			while (n != Integer.MIN_VALUE) {
				count += n;
				n = parseInt(str, ichNextParse);
			}
			return count;
		}
		int ich = 0;
		int ichMax = str.length();
		char ch;
		while (ich < ichMax) {
			while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
				++ich;
			if (ich < ichMax)
				++count;
			while (ich < ichMax && ((ch = str.charAt(ich)) != ' ' && ch != '\t'))
				++ich;
		}
		return count;
	}

	// //////////////////////////////////////////////////////////////
	// associated vertex normalization
	// //////////////////////////////////////////////////////////////

	Hashtable assocGridPointMap;
	Hashtable assocGridPointNormals;

	@SuppressWarnings("unchecked")
	int addVertexCopy(Point3f vertex, float value, boolean iHaveAssociation, String sKey) {
		int vPt = currentMesh.addVertexCopy(vertex, value);
		if (associateNormals && iHaveAssociation) {
			assocGridPointMap.put(new Integer(vPt), sKey);
			if (!assocGridPointNormals.containsKey(sKey))
				assocGridPointNormals.put(sKey, new Vector3f(0, 0, 0));
		}
		return vPt;
	}

	void initializeMesh(boolean use2Sided) {
		int vertexCount = currentMesh.vertexCount;
		Vector3f[] vectorSums = new Vector3f[vertexCount];

		if (!isSilent)
			Logger.debug(" initializeMesh " + vertexCount);
		/*
		 * OK, so if there is an associated grid point (because the point is so close to one), we now declare that
		 * associated point to be used for the vectorSum instead of a new, independent one for the point itself.
		 * 
		 * Bob Hanson, 05/2006
		 * 
		 * having 2-sided normixes is INCOMPATIBLE with this when not a plane
		 * 
		 */

		for (int i = vertexCount; --i >= 0;)
			vectorSums[i] = new Vector3f();
		currentMesh.sumVertexNormals(vectorSums);
		Enumeration e = assocGridPointMap.keys();
		while (e.hasMoreElements()) {
			Integer I = (Integer) e.nextElement();
			((Vector3f) assocGridPointNormals.get(assocGridPointMap.get(I))).add(vectorSums[I.intValue()]);
		}
		e = assocGridPointMap.keys();
		while (e.hasMoreElements()) {
			Integer I = (Integer) e.nextElement();
			vectorSums[I.intValue()] = ((Vector3f) assocGridPointNormals.get(assocGridPointMap.get(I)));
		}
		short[] norm = currentMesh.normixes = new short[vertexCount];
		if (use2Sided)
			for (int i = vertexCount; --i >= 0;)
				norm[i] = g3d.get2SidedNormix(vectorSums[i]);
		else for (int i = vertexCount; --i >= 0;)
			norm[i] = g3d.getNormix(vectorSums[i]);
	}

	// //////////////////////////////////////////////////////////////
	// color mapping methods
	// //////////////////////////////////////////////////////////////

	String remainderString;

	void applyColorScale(Mesh mesh) {
		// ONLY the current mesh now. Previous was just too weird.
		if (colorPhase == 0 && dataType != SURFACE_ATOMICORBITAL)
			colorPhase = 1;
		int vertexCount = mesh.vertexCount;
		short[] colixes = mesh.vertexColixes;
		colorFractionBase = defaultColorFractionBase;
		colorFractionRange = defaultColorFractionRange;
		setMapRanges();
		float min = mappedDataMin;
		float max = mappedDataMax;
		Logger.info("full mapped data range: " + min + " to " + max);
		if (isBicolorMap && thePlane == null || colorBySign)
			Logger.info("coloring by sign");
		else if (colorByPhase)
			Logger.info("coloring by phase: " + colorPhases[colorPhase]);
		else Logger.info("coloring red to blue over range: " + valueMappedToRed + " to " + valueMappedToBlue);
		if (colixes == null)
			mesh.vertexColixes = colixes = new short[vertexCount];
		String list = "";
		String list1 = "";
		int incr = (mesh.hasGridPoints && mesh.jvxlPlane == null ? 3 : 1);
		if (jvxlDataIsPrecisionColor || isContoured)
			jvxlWritePrecisionColor = true;
		for (int i = 0; i < vertexCount; i += incr) {
			float value = getVertexColorValue(mesh, i);
			if (mesh.firstViewableVertex == 0 || i < mesh.firstViewableVertex) {
				char ch;
				if (jvxlWritePrecisionColor) {
					ch = jvxlValueAsCharacter2(value, min, max, colorFractionBase, colorFractionRange);
					list1 += remainder;
					if (logCompression)
						Logger.debug("setcolor precision "
								+ value
								+ " as '"
								+ ch
								+ jvxlValueFromCharacter2(ch, remainder, min, max, colorFractionBase,
										colorFractionRange) + "'/remainder=" + remainder + " ");
				}
				else {
					// isColorReversed
					ch = jvxlValueAsCharacter(value, valueMappedToRed, valueMappedToBlue, colorFractionBase,
							colorFractionRange);
					if (logCompression)
						Logger.info("setcolor noprecision " + value + " as " + ch);
				}
				list += ch;
			}
		}
		mesh.isJvxlPrecisionColor = jvxlWritePrecisionColor;
		mesh.jvxlColorData = (colorByPhase && !isBicolorMap && !colorBySign ? "" : list + list1 + "\n");
		if (logMessages)
			Logger.info("color data: " + mesh.jvxlColorData);
	}

	float getVertexColorValue(Mesh mesh, int vertexIndex) {
		float value, datum;
		/*
		 * but RETURNS the actual value, not the truncated one right, so what we are doing here is setting a range
		 * within the data for which we want red-->blue, but returning the actual number so it can be encoded more
		 * precisely. This turned out to be the key to making the JVXL contours work.
		 * 
		 */
		if (isBicolorMap && !isContoured) // will be current mesh only
			datum = value = mesh.vertexValues[vertexIndex];
		else if (colorByPhase)
			datum = value = getPhase(mesh.vertices[vertexIndex]);
		else if (jvxlDataIs2dContour)
			datum = value = getInterpolatedPixelValue(mesh.vertices[vertexIndex]);
		else datum = value = lookupInterpolatedVoxelValue(mesh.vertices[vertexIndex]);
		if (isBicolorMap && !isContoured || colorBySign) {
			if (value <= 0)
				mesh.vertexColixes[vertexIndex] = Graphics3D.getColix(isColorReversed ? colorPos : colorNeg);
			if (value > 0)
				mesh.vertexColixes[vertexIndex] = Graphics3D.getColix(isColorReversed ? colorNeg : colorPos);
			if (!isContoured)
				datum = (value > 0 ? 0.999f : -0.999f);
		}
		else {
			if (value < valueMappedToRed)
				value = valueMappedToRed;
			if (value >= valueMappedToBlue)
				value = valueMappedToBlue;
			mesh.vertexColixes[vertexIndex] = viewer.getColixFromPalette(isColorReversed ? valueMappedToBlue
					+ valueMappedToRed - value : value, valueMappedToRed, valueMappedToBlue, colorScheme);
		}
		return datum;
	}

	final static String[] colorPhases = { "_orb", "x", "y", "z", "xy", "yz", "xz", "x2-y2", "z2" };

	float getPhase(Point3f pt) {
		ptPsi.set(pt);
		getCalcPoint(ptPsi);
		switch (colorPhase) {
		case 0:
			return (hydrogenAtomPsiAt(ptPsi, psi_n, psi_l, psi_m) > 0 ? 1 : -1);
		case -1:
		case 1:
			return (pt.x > 0 ? 1 : -1);
		case 2:
			return (pt.y > 0 ? 1 : -1);
		case 3:
			return (pt.z > 0 ? 1 : -1);
		case 4:
			return (pt.x * pt.y > 0 ? 1 : -1);
		case 5:
			return (pt.y * pt.z > 0 ? 1 : -1);
		case 6:
			return (pt.x * pt.z > 0 ? 1 : -1);
		case 7:
			return (pt.x * pt.x - pt.y * pt.y > 0 ? 1 : -1);
		case 8:
			return (pt.z * pt.z * 2f - pt.x * pt.x - pt.y * pt.y > 0 ? 1 : -1);
		}
		return 1;
	}

	float getMinMappedValue() {
		if (currentMesh != null)
			return getMinMappedValue(currentMesh);
		float min = Float.MAX_VALUE;
		for (int i = meshCount; --i >= 0;) {
			float challenger = getMinMappedValue(meshes[i]);
			if (challenger < min)
				min = challenger;
		}
		Logger.debug("minimum mapped value: " + min);
		return min;
	}

	float getMinMappedValue(Mesh mesh) {
		int vertexCount = mesh.vertexCount;
		Point3f[] vertexes = mesh.vertices;
		float min = Float.MAX_VALUE;
		int incr = (mesh.hasGridPoints ? 3 : 1);
		for (int i = 0; i < vertexCount; i += incr)
			if (mesh.firstViewableVertex == 0 || i < mesh.firstViewableVertex) {
				float challenger;
				if (jvxlDataIs2dContour)
					challenger = getInterpolatedPixelValue(vertexes[i]);
				else challenger = lookupInterpolatedVoxelValue(vertexes[i]);
				if (challenger < min)
					min = challenger;
			}
		Logger.debug("minimum mapped value: " + min);
		return min;
	}

	float getMaxMappedValue() {
		if (currentMesh != null)
			return getMaxMappedValue(currentMesh);
		float max = -Float.MAX_VALUE;
		for (int i = meshCount; --i >= 0;) {
			float challenger = getMaxMappedValue(meshes[i]);
			if (challenger > max)
				max = challenger;
		}
		return max;
	}

	float getMaxMappedValue(Mesh mesh) {
		int vertexCount = mesh.vertexCount;
		Point3f[] vertexes = mesh.vertices;
		float max = -Float.MAX_VALUE;
		int incr = (mesh.hasGridPoints ? 3 : 1);
		for (int i = 0; i < vertexCount; i += incr)
			if (mesh.firstViewableVertex == 0 || i < mesh.firstViewableVertex) {
				float challenger;
				if (jvxlDataIs2dContour)
					challenger = getInterpolatedPixelValue(vertexes[i]);
				else challenger = lookupInterpolatedVoxelValue(vertexes[i]);
				if (challenger == Float.MAX_VALUE)
					challenger = 0; // for now TESTING ONLY
				if (challenger > max && challenger != Float.MAX_VALUE)
					max = challenger;
			}
		Logger.debug("maximum mapped value: " + max);
		return max;
	}

	float lookupInterpolatedVoxelValue(Point3f point) {
		// ARGH!!! ONLY FOR ORTHOGONAL AXES!!!!!
		// the dot product presumes axes are PERPENDICULAR.
		Point3f pt = new Point3f();
		xyzToVoxelPt(point, pt);
		return getInterpolatedVoxelValue(pt);
	}

	float getInterpolatedVoxelValue(Point3f pt) {
		int iMax;
		int xDown = indexDown(pt.x, iMax = voxelCounts[0] - 1);
		int xUp = xDown + (pt.x < 0 || xDown == iMax ? 0 : 1);
		int yDown = indexDown(pt.y, iMax = voxelCounts[1] - 1);
		int yUp = yDown + (pt.y < 0 || yDown == iMax ? 0 : 1);
		int zDown = indexDown(pt.z, iMax = voxelCounts[2] - 1);
		int zUp = zDown + (pt.z < 0 || zDown == iMax || jvxlDataIs2dContour ? 0 : 1);
		float v1 = getFractional2DValue(pt.x - xDown, pt.y - yDown, voxelData[xDown][yDown][zDown],
				voxelData[xUp][yDown][zDown], voxelData[xDown][yUp][zDown], voxelData[xUp][yUp][zDown]);
		float v2 = getFractional2DValue(pt.x - xDown, pt.y - yDown, voxelData[xDown][yDown][zUp],
				voxelData[xUp][yDown][zUp], voxelData[xDown][yUp][zUp], voxelData[xUp][yUp][zUp]);
		return v1 + (pt.z - zDown) * (v2 - v1);
	}

	final Vector3f pointVector = new Vector3f();

	float getInterpolatedPixelValue(Point3f ptXYZ) {
		pointVector.set(ptXYZ);
		xyzToPixelVector(pointVector);
		float x = pointVector.x;
		float y = pointVector.y;
		int xDown = (x >= pixelCounts[0] ? pixelCounts[0] - 1 : x < 0 ? 0 : (int) x);
		int yDown = (y >= pixelCounts[1] ? pixelCounts[1] - 1 : y < 0 ? 0 : (int) y);
		int xUp = xDown + (xDown == pixelCounts[0] - 1 ? 0 : 1);
		int yUp = yDown + (yDown == pixelCounts[1] - 1 ? 0 : 1);
		float value = getFractional2DValue(x - xDown, y - yDown, pixelData[xDown][yDown], pixelData[xUp][yDown],
				pixelData[xDown][yUp], pixelData[xUp][yUp]);
		return value;
	}

	int indexDown(float value, int iMax) {
		if (value < 0)
			return 0;
		int floor = (int) value;
		return (floor > iMax ? iMax : floor);
	}

	float getFractional2DValue(float fx, float fy, float x11, float x12, float x21, float x22) {
		float v1 = x11 + fx * (x12 - x11);
		float v2 = x21 + fx * (x22 - x21);
		return v1 + fy * (v2 - v1);
	}

	float contourPlaneMinimumValue;
	float contourPlaneMaximumValue;

	void jvxlReadColorData(Mesh mesh) {

		// standard jvxl file read for color

		fractionPtr = 0;
		int vertexCount = mesh.vertexCount;
		short[] colixes = mesh.vertexColixes;
		fractionData = new StringBuffer();
		strFractionTemp = (isJvxl ? jvxlColorDataRead : "");
		fractionPtr = 0;
		Logger.info("JVXL reading color data base/range: " + mappedDataMin + "/" + mappedDataMax + " for "
				+ vertexCount + " vertices." + " using encoding keys " + colorFractionBase + " " + colorFractionRange);
		Logger.info("mapping red-->blue for " + valueMappedToRed + " to " + valueMappedToBlue + " colorPrecision:"
				+ jvxlDataIsPrecisionColor);

		float min = (mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMin : mappedDataMin);
		float range = (mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMax : mappedDataMax) - min;
		float colorRange = valueMappedToBlue - valueMappedToRed;
		contourPlaneMinimumValue = Float.MAX_VALUE;
		contourPlaneMaximumValue = -Float.MAX_VALUE;
		if (colixes == null || colixes.length < vertexCount)
			mesh.vertexColixes = colixes = new short[vertexCount];
		int n = (isContoured ? contourVertexCount : vertexCount);
		String data = jvxlColorDataRead;
		int cpt = 0;
		for (int i = 0; i < n; i++) {
			float fraction, value;
			if (jvxlDataIsPrecisionColor) {
				// this COULD be an option for mapped surfaces;
				// necessary for planes.
				// precision is used for FULL-data range encoding, allowing full
				// treatment of JVXL files as though they were CUBE files.
				fraction = jvxlFractionFromCharacter2(data.charAt(cpt), data.charAt(cpt + n), colorFractionBase,
						colorFractionRange);
				value = min + fraction * range;
			}
			else {
				// my original encoding scheme
				// low precision only allows for mapping relative to the defined color range
				fraction = jvxlFractionFromCharacter(data.charAt(cpt), colorFractionBase, colorFractionRange, 0.5f);
				value = valueMappedToRed + fraction * colorRange;
			}
			++cpt;
			if (value < contourPlaneMinimumValue)
				contourPlaneMinimumValue = value;
			if (value > contourPlaneMaximumValue)
				contourPlaneMaximumValue = value;

			if (isContoured) {
				contourVertexes[i].setValue(value);
			}
			else if (colorBySign) {
				colixes[i] = Graphics3D.getColix((isColorReversed ? value > 0 : value <= 0) ? colorNeg : colorPos);
			}
			else {
				colixes[i] = viewer.getColixFromPalette(isColorReversed ? valueMappedToRed + valueMappedToBlue - value
						: value, valueMappedToRed, valueMappedToBlue, colorScheme);
				if (logMessages)
					Logger.info("readColor " + i + ": " + fraction + " " + value + " " + valueMappedToRed + " "
							+ valueMappedToBlue + " " + colixes[i]);
				if (mesh.hasGridPoints) {
					colixes[++i] = viewer.getColixFromPalette(0.2f, 0f, 1f, colorScheme);
					colixes[++i] = viewer.getColixFromPalette(0.8f, 0f, 1f, colorScheme);
				}
			}
		}
		if (mappedDataMin == Float.MAX_VALUE) {
			mappedDataMin = contourPlaneMinimumValue;
			mappedDataMax = contourPlaneMaximumValue;
		}
		mesh.jvxlColorData = data + "\n";
	}

	// //////////////////////////////////////////////////////////////
	// //////// JVXL FILE READING/WRITING ////////////
	// //////////////////////////////////////////////////////////////

	int fractionPtr;
	String strFractionTemp = "";
	StringBuffer fractionData = new StringBuffer();

	float jvxlGetNextFraction(int base, int range, float fracOffset) {
		if (fractionPtr >= strFractionTemp.length()) {
			if (!endOfData)
				Logger.error("end of file reading compressed fraction data at point " + fractionData.length());
			endOfData = true;
			strFractionTemp = "" + (char) base;
			fractionData.append(strFractionTemp);
			fractionData.append('\n');
			fractionPtr = 0;
		}
		return jvxlFractionFromCharacter(strFractionTemp.charAt(fractionPtr++), base, range, fracOffset);
	}

	float jvxlValueFromCharacter(int ich, float min, float max, int base, int range, float fracOffset) {
		float fraction = jvxlFractionFromCharacter(ich, base, range, fracOffset);
		return (max == min ? fraction : min + fraction * (max - min));
	}

	float jvxlFractionFromCharacter(int ich, int base, int range, float fracOffset) {
		if (ich < base)
			ich = 92; // ! --> \
		float fraction = (ich - base + fracOffset) / range;
		if (fraction < 0f)
			fraction = 0f;
		if (fraction > 1f)
			fraction = 0.999999f;
		if (ich == base + range)
			fraction = Float.NaN;
		if (logCompression)
			Logger.info("ffc: " + fraction + " <-- " + ich + " " + (char) ich);
		return fraction;
	}

	char jvxlValueAsCharacter(float value, float min, float max, int base, int range) {
		float fraction = (min == max ? value : (value - min) / (max - min));
		return jvxlFractionAsCharacter(fraction, base, range);
	}

	char jvxlFractionAsCharacter(float fraction, int base, int range) {
		if (fraction > 0.9999f)
			fraction = 0.9999f;
		if (Float.isNaN(fraction))
			fraction = 1.0001f;
		int ich = (int) (fraction * range + base);
		if (ich < base)
			ich = base;
		if (ich == 92)
			ich = 33; // \ --> !
		if (logCompression)
			Logger.info("fac: " + fraction + " --> " + ich + " " + (char) ich);
		return (char) ich;
	}

	float jvxlValueFromCharacter2(int ich, int ich2, float min, float max, int base, int range) {
		float fraction = jvxlFractionFromCharacter2(ich, ich2, base, range);
		return (max == min ? fraction : min + fraction * (max - min));
	}

	float jvxlFractionFromCharacter2(int ich1, int ich2, int base, int range) {
		float fraction = jvxlFractionFromCharacter(ich1, base, range, 0);
		float remains = jvxlFractionFromCharacter(ich2, base, range, 0.5f);
		if (logMessages)
			Logger.info("fraction:" + fraction + " + " + (remains / range) + " r=" + range + " " + (char) ich1
					+ (char) ich2 + " = " + (fraction + remains / range));
		return fraction + remains / range;
	}

	char remainder;

	char jvxlValueAsCharacter2(float value, float min, float max, int base, int range) {
		float fraction = (min == max ? value : (value - min) / (max - min));
		char ch1 = jvxlFractionAsCharacter(fraction, base, range);
		fraction -= jvxlFractionFromCharacter(ch1, base, range, 0);
		remainder = jvxlFractionAsCharacter(fraction * range, base, range);
		return ch1;
	}

	String jvxlExtraLine(int n) {
		return (-n) + " " + edgeFractionBase + " " + edgeFractionRange + " " + colorFractionBase + " "
				+ colorFractionRange + " Jmol voxel format version 0.9f\n";
		// 0.9e adds color contours for planes and min/max range, contour settings
	}

	String jvxlGetFile(Mesh mesh, String msg, boolean includeHeader, int nSurfaces) {
		String data = "";
		if (includeHeader) {
			data = mesh.jvxlFileHeader
					+ (nSurfaces > 0 ? (-nSurfaces) + mesh.jvxlExtraLine.substring(2) : mesh.jvxlExtraLine);
			if (data.indexOf("JVXL") != 0)
				data = "JVXL " + data;
		}
		data += "# " + msg + "\n";
		if (title != null)
			for (int i = 0; i < title.length; i++)
				data += "# " + title[i] + "\n";
		data += mesh.jvxlDefinitionLine + "\n";
		String compressedData = (mesh.jvxlPlane == null ? mesh.jvxlSurfaceData : "");
		if (logMessages)
			Logger.info(" jvxlGetFile: " + mesh.jvxlSurfaceData + "\n" + mesh.jvxlEdgeData + "\n" + mesh.jvxlColorData
					+ "\n" + mesh.jvxlPlane);

		if (mesh.jvxlPlane == null) {
			// no real point in compressing this unless it's a sign-based coloring
			compressedData += jvxlCompressString(mesh.jvxlEdgeData + mesh.jvxlColorData);
		}
		else {
			compressedData += jvxlCompressString(mesh.jvxlColorData);
		}
		if (!isJvxl && mesh.nBytes > 0)
			mesh.jvxlCompressionRatio = (int) (((float) mesh.nBytes + mesh.jvxlFileHeader.length()) / (data.length() + compressedData
					.length()));
		data += compressedData;
		if (msg != null)
			data += "#-------end of jvxl file data-------\n";
		return data;
	}

	String jvxlGetDefinitionLine(Mesh mesh) {
		String definitionLine = mesh.cutoff + " ";

		// cutoff param1 param2 param3
		// | | |
		// when | | > 0 ==> jvxlDataIsColorMapped
		// when | | == -1 ==> not color mapped
		// when | | < -1 ==> jvxlDataIsPrecisionColor
		// when == -1 && == -1 ==> noncontoured plane
		// when == -1 && == -2 ==> contourable plane
		// when < -1 && > 0 ==> contourable functionXY
		// when > 0 && < 0 ==> jvxlDataisBicolorMap

		int nSurfaceData = mesh.jvxlSurfaceData.length();
		int nEdgeData = (mesh.jvxlEdgeData.length() - 1);
		int nColorData = (mesh.jvxlColorData.length() - 1);
		if (mesh.jvxlPlane == null) {
			if (mesh.isContoured)
				definitionLine += (-nSurfaceData) + " " + nEdgeData;
			else if (mesh.isBicolorMap)
				definitionLine += (nSurfaceData) + " " + (-nEdgeData);
			else definitionLine += nSurfaceData + " " + nEdgeData;
			definitionLine += " " + (mesh.isJvxlPrecisionColor && nColorData != -1 ? -nColorData : nColorData);
		}
		else {
			definitionLine += "-1 -2 " + (-nColorData) + " " + mesh.jvxlPlane.x + " " + mesh.jvxlPlane.y + " "
					+ mesh.jvxlPlane.z + " " + mesh.jvxlPlane.w;
		}
		if (mesh.isContoured)
			definitionLine += " " + mesh.nContours;

		// ... mappedDataMin mappedDataMax valueMappedToRed valueMappedToBlue ...
		definitionLine += " " + mesh.mappedDataMin + " " + mesh.mappedDataMax + " " + mesh.valueMappedToRed + " "
				+ mesh.valueMappedToBlue;
		// ... information only ...
		if (mesh.jvxlPlane != null)
			definitionLine += " CONTOUR PLANE " + mesh.jvxlPlane;
		if (mesh.jvxlCompressionRatio > 0)
			definitionLine += " approximate compressionRatio=" + mesh.jvxlCompressionRatio + ":1";
		return definitionLine;
	}

	String jvxlCompressString(String data) {
		/*
		 * just a simple compression, but allows 2000-6000:1 CUBE:JVXL for planes!
		 * 
		 * "X~nnn " means "nnn copies of character X"
		 * 
		 * ########## becomes "#~10 " ~ becomes "~~"
		 * 
		 */
		if (logCompression)
			Logger.info(data.length() + " compressing\n" + data);
		String dataOut = "";
		String dataBuffer = "";
		char chLast = '\0';
		data += '\0';
		int nLast = 0;
		for (int i = 0; i < data.length(); i++) {
			char ch = data.charAt(i);
			if (ch == chLast) {
				++nLast;
				dataBuffer += ch;
				if (ch != '~')
					ch = '\0';
			}
			else if (nLast > 0) {
				dataOut += (nLast < 4 || chLast == '~' || chLast == ' ' || chLast == '\t' ? dataBuffer : "~" + nLast
						+ " ");
				dataBuffer = "";
				nLast = 0;
			}
			if (ch != '\0') {
				dataOut += ch;
				chLast = ch;
			}
		}
		if (logCompression) {
			Logger.info(dataOut.length() + "\n" + dataOut);
			data = jvxlUncompressString(dataOut);
			Logger.info(data.length() + " uncompressing\n" + data);
		}
		return dataOut;
	}

	String jvxlUncompressString(String data) {
		if (data.indexOf("~") < 0)
			return data;
		if (logCompression)
			Logger.info(data.length() + " uncompressing\n" + data);
		String dataOut = "";
		char chLast = '\0';
		for (int i = 0; i < data.length(); i++) {
			char ch = data.charAt(i);
			if (ch == '~') {
				int nChar = parseInt(data, ++i);
				if (nChar == Integer.MIN_VALUE) {
					if (chLast == '~') {
						dataOut += '~';
						while ((ch = data.charAt(++i)) == '~')
							dataOut += '~';
					}
					else {
						Logger.error("Error uncompressing string " + data.substring(0, i) + "?");
					}
				}
				else {
					for (int c = 0; c < nChar; c++)
						dataOut += chLast;
					i = ichNextParse;
				}
			}
			else {
				dataOut += ch;
				chLast = ch;
			}
		}
		if (logCompression)
			Logger.info(dataOut.length() + "\n" + dataOut);
		return dataOut;
	}

	// //////////////////////////////////////////////////////////////
	// marching cube stuff
	// //////////////////////////////////////////////////////////////

	final float[] vertexValues = new float[8];
	final Point3i[] vertexPoints = new Point3i[8];
	final Point3f[] surfacePoints = new Point3f[12];
	{
		for (int i = 12; --i >= 0;)
			surfacePoints[i] = new Point3f();
		for (int i = 8; --i >= 0;)
			vertexPoints[i] = new Point3i();
	}
	final int[] surfacePointIndexes = new int[12];
	int cubeCountX, cubeCountY, cubeCountZ;
	int contourType; // 0, 1, or 2

	int getContourType(Point4f plane) {
		Vector3f norm = new Vector3f(plane.x, plane.y, plane.z);
		float dotX = norm.dot(volumetricVectors[0]);
		float dotY = norm.dot(volumetricVectors[1]);
		float dotZ = norm.dot(volumetricVectors[2]);
		dotX *= dotX;
		dotY *= dotY;
		dotZ *= dotZ;
		float max = dotX;
		if (max < dotY)
			max = dotY;
		int iType = (max < dotZ ? 2 : max == dotY ? 1 : 0);
		Logger.info("contouring planar pixel subset " + iType);
		return iType;
	}

	void generateSurfaceData() {
		cubeCountX = voxelData.length - 1;
		cubeCountY = voxelData[0].length - 1;
		cubeCountZ = voxelData[0][0].length - 1;
		fractionData = new StringBuffer();
		strFractionTemp = (isJvxl ? jvxlEdgeDataRead : "");
		fractionPtr = 0;
		if (thePlane != null) {
			contourVertexCount = 0;
			contourType = getContourType(thePlane);
		}
		else if (isContoured) {
			contourVertexCount = 0;
			contourType = 2;
		}
		if (!isSilent || logMessages) {
			Logger.info("cutoff=" + cutoff + " voxel cubes=" + cubeCountX + "," + cubeCountY + "," + cubeCountZ + ","
					+ " total=" + (cubeCountX * cubeCountY * cubeCountZ));
			Logger.info("resolutions(x,y,z)=" + 1 / volumetricVectors[0].length() + "," + 1
					/ volumetricVectors[1].length() + "," + 1 / volumetricVectors[2].length());
		}

		int[][] isoPointIndexes = new int[cubeCountY * cubeCountZ][12];
		for (int i = cubeCountY * cubeCountZ; --i >= 0;)
			isoPointIndexes[i] = new int[12];
		int insideCount = 0, outsideCount = 0, surfaceCount = 0;
		for (int x = cubeCountX; --x >= 0;) {
			for (int y = cubeCountY; --y >= 0;) {
				for (int z = cubeCountZ; --z >= 0;) {
					int[] voxelPointIndexes = propagateNeighborPointIndexes(x, y, z, isoPointIndexes);
					int insideMask = 0;
					for (int i = 8; --i >= 0;) {
						Point3i offset = cubeVertexOffsets[i];
						float voxelValue = voxelData[x + offset.x][y + offset.y][z + offset.z];
						vertexValues[i] = voxelValue;
						if (logCube)
							vertexPoints[i].set(x + offset.x, y + offset.y, z + offset.z);
						if (isInside(voxelValue, cutoff))
							insideMask |= 1 << i;
					}

					if (insideMask == 0) {
						++outsideCount;
						continue;
					}
					if (insideMask == 0xFF) {
						++insideCount;
						continue;
					}
					++surfaceCount;
					if (!processOneCubical(insideMask, cutoff, voxelPointIndexes, x, y, z) || isContoured)
						continue;

					byte[] triangles = triangleTable[insideMask];
					for (int i = triangles.length; (i -= 3) >= 0;) {
						if (!isCutoffAbsolute
								|| checkCutoff(voxelPointIndexes[triangles[i]], voxelPointIndexes[triangles[i + 1]],
										voxelPointIndexes[triangles[i + 2]]))
							currentMesh.addTriangle(voxelPointIndexes[triangles[i]],
									voxelPointIndexes[triangles[i + 1]], voxelPointIndexes[triangles[i + 2]]);
					}
				}
			}
		}
		if (isJvxl) {
			fractionData = new StringBuffer();
			fractionData.append(jvxlEdgeDataRead);
		}
		fractionData.append('\n'); // from generateSurfaceData
		if (!isSilent || logMessages)
			Logger.info("insideCount=" + insideCount + " outsideCount=" + outsideCount + " surfaceCount="
					+ surfaceCount + " total=" + (insideCount + outsideCount + surfaceCount));
	}

	boolean checkCutoff(int v1, int v2, int v3) {
		// never cross a +/- junction with a triangle in the case of orbitals,
		// where we are using |psi| instead of psi for the surface generation.
		if (v1 < 0 || v2 < 0 || v3 < 0)
			return false;
		float val1 = currentMesh.vertexValues[v1];
		float val2 = currentMesh.vertexValues[v2];
		float val3 = currentMesh.vertexValues[v3];
		return (val1 * val2 >= 0 && val2 * val3 >= 0);
	}

	boolean isInside(float voxelValue, float max) {
		return ((max > 0 && (isCutoffAbsolute ? Math.abs(voxelValue) : voxelValue) >= max) || (max <= 0 && voxelValue <= max));
	}

	final int[] nullNeighbor = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

	int[] propagateNeighborPointIndexes(int x, int y, int z, int[][] isoPointIndexes) {
		/*
		 * Y 4 --------4--------- 5 /| /| / | / | / | / | 7 8 5 | / | / 9 / | / | 7 --------6--------- 6 | | | | | | 0
		 * ---------0--|----- 1 X | / | / 11 / 10 / | 3 | 1 | / | / | / | / 3 ---------2-------- 2 Z
		 * 
		 */
		int cellIndex = y * cubeCountZ + z;
		int[] voxelPointIndexes = isoPointIndexes[cellIndex];

		boolean noXNeighbor = (x == cubeCountX - 1);
		// the x neighbor is myself from my last pass through here
		if (noXNeighbor) {
			voxelPointIndexes[1] = -1;
			voxelPointIndexes[9] = -1;
			voxelPointIndexes[5] = -1;
			voxelPointIndexes[10] = -1;
		}
		else {
			voxelPointIndexes[1] = voxelPointIndexes[3];
			voxelPointIndexes[9] = voxelPointIndexes[8];
			voxelPointIndexes[5] = voxelPointIndexes[7];
			voxelPointIndexes[10] = voxelPointIndexes[11];
		}

		// from the y neighbor pick up the top
		boolean noYNeighbor = (y == cubeCountY - 1);
		int[] yNeighbor = noYNeighbor ? nullNeighbor : isoPointIndexes[cellIndex + cubeCountZ];

		voxelPointIndexes[6] = yNeighbor[2];
		voxelPointIndexes[7] = yNeighbor[3];
		voxelPointIndexes[4] = yNeighbor[0];
		if (noXNeighbor)
			voxelPointIndexes[5] = yNeighbor[1];

		// from my z neighbor
		boolean noZNeighbor = (z == cubeCountZ - 1);
		int[] zNeighbor = noZNeighbor ? nullNeighbor : isoPointIndexes[cellIndex + 1];

		voxelPointIndexes[2] = zNeighbor[0];
		voxelPointIndexes[11] = zNeighbor[8];
		if (noYNeighbor)
			voxelPointIndexes[6] = zNeighbor[4];
		if (noXNeighbor)
			voxelPointIndexes[10] = zNeighbor[9];

		// these must always be calculated
		voxelPointIndexes[0] = -1;
		voxelPointIndexes[3] = -1;
		voxelPointIndexes[8] = -1;

		return voxelPointIndexes;
	}

	int firstCriticalVertex;
	int lastCriticalVertex;
	int edgeCount;
	final static float assocCutoff = 0.3f;

	boolean processOneCubical(int insideMask, float cutoff, int[] voxelPointIndexes, int x, int y, int z) {
		int edgeMask = insideMaskTable[insideMask];
		boolean isNaN = false;
		for (int iEdge = 12; --iEdge >= 0;) {
			if ((edgeMask & (1 << iEdge)) == 0)
				continue;
			if (voxelPointIndexes[iEdge] >= 0)
				continue; // propagated from neighbor
			++edgeCount;
			int vertexA = edgeVertexes[2 * iEdge];
			int vertexB = edgeVertexes[2 * iEdge + 1];
			float valueA = vertexValues[vertexA];
			float valueB = vertexValues[vertexB];
			if (Float.isNaN(valueA) || Float.isNaN(valueB))
				isNaN = true;
			calcVertexPoints(x, y, z, vertexA, vertexB);
			float fraction = calcSurfacePoint(cutoff, valueA, valueB, surfacePoints[iEdge]);
			if (isContoured) {
				// Logger.info(" processVoxel " + x + "," + y + "," + z + " "
				// + iEdge + " " + binaryString(edgeMask) + " " + contourType
				// + " fraction " + fraction + " valueA " + valueA + " valueB "
				// + valueB + " cutoff " + cutoff);
				/*
				 * we are collecting just the desired type of intersection for the 2D marching square contouring -- x,
				 * y, or z. In the case of a contoured f(x,y) surface, we take every point.
				 * 
				 */
				int vPt = Integer.MAX_VALUE;
				if (edgeTypeTable[iEdge] == contourType)
					vPt = addContourData(x, y, z, cubeVertexOffsets[vertexA], surfacePoints[iEdge], cutoff);
				voxelPointIndexes[iEdge] = vPt;
				continue;
			}
			int assocVertex = (fraction < assocCutoff ? -1 : fraction > 1 - assocCutoff ? 1 : 0);
			String sKey = (assocVertex == 0 ? "" : calcDataKey(x, y, z, assocVertex < 0 ? vertexA : vertexB));
			voxelPointIndexes[iEdge] = addVertexCopy(surfacePoints[iEdge], thisValue, (assocVertex != 0), sKey);
			if (logCube)
				Logger.info("edge " + vertexPoints[vertexA] + " " + vertexPoints[vertexB] + " surface "
						+ surfacePoints[iEdge] + " " + thisValue);
			if (iAddGridPoints) {
				addVertexCopy(valueA < valueB ? pointA : pointB, Float.NaN, false, "");
				addVertexCopy(valueA < valueB ? pointB : pointA, Float.NaN, false, "");
			}
		}
		return !isNaN;
	}

	String calcDataKey(int x, int y, int z, int vertexPt) {
		Point3i offset = cubeVertexOffsets[vertexPt];
		return (x + offset.x) + "_" + (y + offset.y) + "_" + (z + offset.z);
	}

	final Point3f voxelOrigin = new Point3f();
	final Point3f voxelT = new Point3f();
	final Point3f pointA = new Point3f();
	final Point3f pointB = new Point3f();
	final Vector3f edgeVector = new Vector3f();
	float thisValue;

	float calcSurfacePoint(float cutoff, float valueA, float valueB, Point3f surfacePoint) {
		float fraction;
		if (isJvxl && jvxlEdgeDataCount > 0) {
			fraction = jvxlGetNextFraction(edgeFractionBase, edgeFractionRange, 0.5f);
			thisValue = fraction;
		}
		else {
			float diff = valueB - valueA;
			fraction = (cutoff - valueA) / diff;
			if (isCutoffAbsolute && (fraction < 0 || fraction > 1))
				fraction = (-cutoff - valueA) / diff;

			if (fraction < 0 || fraction > 1) {
				Logger.error("problem with unusual fraction=" + fraction + " cutoff=" + cutoff + " A:" + valueA + " B:"
						+ valueB);
				fraction = Float.NaN;
			}
			thisValue = valueA + fraction * diff;
			if (!isJvxl)
				fractionData.append(jvxlFractionAsCharacter(fraction, edgeFractionBase, edgeFractionRange));
		}
		edgeVector.sub(pointB, pointA);
		surfacePoint.scaleAdd(fraction, edgeVector, pointA);
		return fraction;
	}

	void calcVertexPoints(int x, int y, int z, int vertexA, int vertexB) {
		voxelPtToXYZ(x, y, z, voxelOrigin);
		pointA.add(voxelOrigin, voxelVertexVectors[vertexA]);
		pointB.add(voxelOrigin, voxelVertexVectors[vertexB]);
	}

	final static Point3i[] cubeVertexOffsets = { new Point3i(0, 0, 0), new Point3i(1, 0, 0), new Point3i(1, 0, 1),
			new Point3i(0, 0, 1), new Point3i(0, 1, 0), new Point3i(1, 1, 0), new Point3i(1, 1, 1),
			new Point3i(0, 1, 1) };

	final static Vector3f[] cubeVertexVectors = { new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1),
			new Vector3f(0, 0, 1), new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, 1, 1),
			new Vector3f(0, 1, 1) };

	Vector3f[] voxelVertexVectors = new Vector3f[8];

	void calcVoxelVertexVectors() {
		for (int i = 8; --i >= 0;) {
			voxelVertexVectors[i] = new Vector3f();
			volumetricMatrix.transform(cubeVertexVectors[i], voxelVertexVectors[i]);
		}
		if (logMessages)
			for (int i = 0; i < 8; ++i) {
				Logger.info("voxelVertexVectors[" + i + "]=" + voxelVertexVectors[i]);
			}
	}

	/*
	 * Y 4 --------4--------- 5 /| /| / | / | / | / | 7 8 5 | / | / 9 / | / | 7 --------6--------- 6 | | | | | | 0
	 * ---------0--|----- 1 X | / | / 11 / 10 / | 3 | 1 | / | / | / | / 3 ---------2-------- 2 Z
	 * 
	 * 
	 * type 0: x-edges: 0 2 4 6 typw 1: y-edges: 8 9 10 11 type 2: z-edges: 1 3 5 7
	 * 
	 * 
	 * 
	 */
	final static int edgeTypeTable[] = { 0, 2, 0, 2, 0, 2, 0, 2, 1, 1, 1, 1 };

	final static byte edgeVertexes[] = { 0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7 };

	final static short insideMaskTable[] = { 0x0000, 0x0109, 0x0203, 0x030A, 0x0406, 0x050F, 0x0605, 0x070C, 0x080C,
			0x0905, 0x0A0F, 0x0B06, 0x0C0A, 0x0D03, 0x0E09, 0x0F00, 0x0190, 0x0099, 0x0393, 0x029A, 0x0596, 0x049F,
			0x0795, 0x069C, 0x099C, 0x0895, 0x0B9F, 0x0A96, 0x0D9A, 0x0C93, 0x0F99, 0x0E90, 0x0230, 0x0339, 0x0033,
			0x013A, 0x0636, 0x073F, 0x0435, 0x053C, 0x0A3C, 0x0B35, 0x083F, 0x0936, 0x0E3A, 0x0F33, 0x0C39, 0x0D30,
			0x03A0, 0x02A9, 0x01A3, 0x00AA, 0x07A6, 0x06AF, 0x05A5, 0x04AC, 0x0BAC, 0x0AA5, 0x09AF, 0x08A6, 0x0FAA,
			0x0EA3, 0x0DA9, 0x0CA0, 0x0460, 0x0569, 0x0663, 0x076A, 0x0066, 0x016F, 0x0265, 0x036C, 0x0C6C, 0x0D65,
			0x0E6F, 0x0F66, 0x086A, 0x0963, 0x0A69, 0x0B60, 0x05F0, 0x04F9, 0x07F3, 0x06FA, 0x01F6, 0x00FF, 0x03F5,
			0x02FC, 0x0DFC, 0x0CF5, 0x0FFF, 0x0EF6, 0x09FA, 0x08F3, 0x0BF9, 0x0AF0, 0x0650, 0x0759, 0x0453, 0x055A,
			0x0256, 0x035F, 0x0055, 0x015C, 0x0E5C, 0x0F55, 0x0C5F, 0x0D56, 0x0A5A, 0x0B53, 0x0859, 0x0950, 0x07C0,
			0x06C9, 0x05C3, 0x04CA, 0x03C6, 0x02CF, 0x01C5, 0x00CC, 0x0FCC, 0x0EC5, 0x0DCF, 0x0CC6, 0x0BCA, 0x0AC3,
			0x09C9, 0x08C0, 0x08C0, 0x09C9, 0x0AC3, 0x0BCA, 0x0CC6, 0x0DCF, 0x0EC5, 0x0FCC, 0x00CC, 0x01C5, 0x02CF,
			0x03C6, 0x04CA, 0x05C3, 0x06C9, 0x07C0, 0x0950, 0x0859, 0x0B53, 0x0A5A, 0x0D56, 0x0C5F, 0x0F55, 0x0E5C,
			0x015C, 0x0055, 0x035F, 0x0256, 0x055A, 0x0453, 0x0759, 0x0650, 0x0AF0, 0x0BF9, 0x08F3, 0x09FA, 0x0EF6,
			0x0FFF, 0x0CF5, 0x0DFC, 0x02FC, 0x03F5, 0x00FF, 0x01F6, 0x06FA, 0x07F3, 0x04F9, 0x05F0, 0x0B60, 0x0A69,
			0x0963, 0x086A, 0x0F66, 0x0E6F, 0x0D65, 0x0C6C, 0x036C, 0x0265, 0x016F, 0x0066, 0x076A, 0x0663, 0x0569,
			0x0460, 0x0CA0, 0x0DA9, 0x0EA3, 0x0FAA, 0x08A6, 0x09AF, 0x0AA5, 0x0BAC, 0x04AC, 0x05A5, 0x06AF, 0x07A6,
			0x00AA, 0x01A3, 0x02A9, 0x03A0, 0x0D30, 0x0C39, 0x0F33, 0x0E3A, 0x0936, 0x083F, 0x0B35, 0x0A3C, 0x053C,
			0x0435, 0x073F, 0x0636, 0x013A, 0x0033, 0x0339, 0x0230, 0x0E90, 0x0F99, 0x0C93, 0x0D9A, 0x0A96, 0x0B9F,
			0x0895, 0x099C, 0x069C, 0x0795, 0x049F, 0x0596, 0x029A, 0x0393, 0x0099, 0x0190, 0x0F00, 0x0E09, 0x0D03,
			0x0C0A, 0x0B06, 0x0A0F, 0x0905, 0x080C, 0x070C, 0x0605, 0x050F, 0x0406, 0x030A, 0x0203, 0x0109, 0x0000 };

	final static byte[][] triangleTable = { null, { 0, 8, 3 }, { 0, 1, 9 }, { 1, 8, 3, 9, 8, 1 }, { 1, 2, 10 },
			{ 0, 8, 3, 1, 2, 10 }, { 9, 2, 10, 0, 2, 9 }, { 2, 8, 3, 2, 10, 8, 10, 9, 8 }, { 3, 11, 2 },
			{ 0, 11, 2, 8, 11, 0 }, { 1, 9, 0, 2, 3, 11 }, { 1, 11, 2, 1, 9, 11, 9, 8, 11 }, { 3, 10, 1, 11, 10, 3 },
			{ 0, 10, 1, 0, 8, 10, 8, 11, 10 }, { 3, 9, 0, 3, 11, 9, 11, 10, 9 }, { 9, 8, 10, 10, 8, 11 }, { 4, 7, 8 },
			{ 4, 3, 0, 7, 3, 4 }, { 0, 1, 9, 8, 4, 7 }, { 4, 1, 9, 4, 7, 1, 7, 3, 1 }, { 1, 2, 10, 8, 4, 7 },
			{ 3, 4, 7, 3, 0, 4, 1, 2, 10 }, { 9, 2, 10, 9, 0, 2, 8, 4, 7 }, { 2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4 },
			{ 8, 4, 7, 3, 11, 2 }, { 11, 4, 7, 11, 2, 4, 2, 0, 4 }, { 9, 0, 1, 8, 4, 7, 2, 3, 11 },
			{ 4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1 }, { 3, 10, 1, 3, 11, 10, 7, 8, 4 },
			{ 1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4 }, { 4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3 },
			{ 4, 7, 11, 4, 11, 9, 9, 11, 10 }, { 9, 5, 4 }, { 9, 5, 4, 0, 8, 3 }, { 0, 5, 4, 1, 5, 0 },
			{ 8, 5, 4, 8, 3, 5, 3, 1, 5 }, { 1, 2, 10, 9, 5, 4 }, { 3, 0, 8, 1, 2, 10, 4, 9, 5 },
			{ 5, 2, 10, 5, 4, 2, 4, 0, 2 }, { 2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8 }, { 9, 5, 4, 2, 3, 11 },
			{ 0, 11, 2, 0, 8, 11, 4, 9, 5 }, { 0, 5, 4, 0, 1, 5, 2, 3, 11 }, { 2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5 },
			{ 10, 3, 11, 10, 1, 3, 9, 5, 4 }, { 4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10 },
			{ 5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3 }, { 5, 4, 8, 5, 8, 10, 10, 8, 11 }, { 9, 7, 8, 5, 7, 9 },
			{ 9, 3, 0, 9, 5, 3, 5, 7, 3 }, { 0, 7, 8, 0, 1, 7, 1, 5, 7 }, { 1, 5, 3, 3, 5, 7 },
			{ 9, 7, 8, 9, 5, 7, 10, 1, 2 }, { 10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3 },
			{ 8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2 }, { 2, 10, 5, 2, 5, 3, 3, 5, 7 }, { 7, 9, 5, 7, 8, 9, 3, 11, 2 },
			{ 9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11 }, { 2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7 },
			{ 11, 2, 1, 11, 1, 7, 7, 1, 5 }, { 9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11 },
			{ 5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0 }, { 11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0 },
			{ 11, 10, 5, 7, 11, 5 }, { 10, 6, 5 }, { 0, 8, 3, 5, 10, 6 }, { 9, 0, 1, 5, 10, 6 },
			{ 1, 8, 3, 1, 9, 8, 5, 10, 6 }, { 1, 6, 5, 2, 6, 1 }, { 1, 6, 5, 1, 2, 6, 3, 0, 8 },
			{ 9, 6, 5, 9, 0, 6, 0, 2, 6 }, { 5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8 }, { 2, 3, 11, 10, 6, 5 },
			{ 11, 0, 8, 11, 2, 0, 10, 6, 5 }, { 0, 1, 9, 2, 3, 11, 5, 10, 6 },
			{ 5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11 }, { 6, 3, 11, 6, 5, 3, 5, 1, 3 },
			{ 0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6 }, { 3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9 },
			{ 6, 5, 9, 6, 9, 11, 11, 9, 8 }, { 5, 10, 6, 4, 7, 8 }, { 4, 3, 0, 4, 7, 3, 6, 5, 10 },
			{ 1, 9, 0, 5, 10, 6, 8, 4, 7 }, { 10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4 }, { 6, 1, 2, 6, 5, 1, 4, 7, 8 },
			{ 1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7 }, { 8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6 },
			{ 7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9 }, { 3, 11, 2, 7, 8, 4, 10, 6, 5 },
			{ 5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11 }, { 0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6 },
			{ 9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6 }, { 8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6 },
			{ 5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11 }, { 0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7 },
			{ 6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9 }, { 10, 4, 9, 6, 4, 10 }, { 4, 10, 6, 4, 9, 10, 0, 8, 3 },
			{ 10, 0, 1, 10, 6, 0, 6, 4, 0 }, { 8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10 }, { 1, 4, 9, 1, 2, 4, 2, 6, 4 },
			{ 3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4 }, { 0, 2, 4, 4, 2, 6 }, { 8, 3, 2, 8, 2, 4, 4, 2, 6 },
			{ 10, 4, 9, 10, 6, 4, 11, 2, 3 }, { 0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6 },
			{ 3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10 }, { 6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1 },
			{ 9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3 }, { 8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1 },
			{ 3, 11, 6, 3, 6, 0, 0, 6, 4 }, { 6, 4, 8, 11, 6, 8 }, { 7, 10, 6, 7, 8, 10, 8, 9, 10 },
			{ 0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10 }, { 10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0 },
			{ 10, 6, 7, 10, 7, 1, 1, 7, 3 }, { 1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7 },
			{ 2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9 }, { 7, 8, 0, 7, 0, 6, 6, 0, 2 }, { 7, 3, 2, 6, 7, 2 },
			{ 2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7 }, { 2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7 },
			{ 1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11 }, { 11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1 },
			{ 8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6 }, { 0, 9, 1, 11, 6, 7 },
			{ 7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0 }, { 7, 11, 6 }, { 7, 6, 11 }, { 3, 0, 8, 11, 7, 6 },
			{ 0, 1, 9, 11, 7, 6 }, { 8, 1, 9, 8, 3, 1, 11, 7, 6 }, { 10, 1, 2, 6, 11, 7 },
			{ 1, 2, 10, 3, 0, 8, 6, 11, 7 }, { 2, 9, 0, 2, 10, 9, 6, 11, 7 },
			{ 6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8 }, { 7, 2, 3, 6, 2, 7 }, { 7, 0, 8, 7, 6, 0, 6, 2, 0 },
			{ 2, 7, 6, 2, 3, 7, 0, 1, 9 }, { 1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6 }, { 10, 7, 6, 10, 1, 7, 1, 3, 7 },
			{ 10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8 }, { 0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7 },
			{ 7, 6, 10, 7, 10, 8, 8, 10, 9 }, { 6, 8, 4, 11, 8, 6 }, { 3, 6, 11, 3, 0, 6, 0, 4, 6 },
			{ 8, 6, 11, 8, 4, 6, 9, 0, 1 }, { 9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6 }, { 6, 8, 4, 6, 11, 8, 2, 10, 1 },
			{ 1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6 }, { 4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9 },
			{ 10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3 }, { 8, 2, 3, 8, 4, 2, 4, 6, 2 }, { 0, 4, 2, 4, 6, 2 },
			{ 1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8 }, { 1, 9, 4, 1, 4, 2, 2, 4, 6 },
			{ 8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1 }, { 10, 1, 0, 10, 0, 6, 6, 0, 4 },
			{ 4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3 }, { 10, 9, 4, 6, 10, 4 }, { 4, 9, 5, 7, 6, 11 },
			{ 0, 8, 3, 4, 9, 5, 11, 7, 6 }, { 5, 0, 1, 5, 4, 0, 7, 6, 11 }, { 11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5 },
			{ 9, 5, 4, 10, 1, 2, 7, 6, 11 }, { 6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5 },
			{ 7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2 }, { 3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6 },
			{ 7, 2, 3, 7, 6, 2, 5, 4, 9 }, { 9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7 },
			{ 3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0 }, { 6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8 },
			{ 9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7 }, { 1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4 },
			{ 4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10 }, { 7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10 },
			{ 6, 9, 5, 6, 11, 9, 11, 8, 9 }, { 3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5 },
			{ 0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11 }, { 6, 11, 3, 6, 3, 5, 5, 3, 1 },
			{ 1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6 }, { 0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10 },
			{ 11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5 }, { 6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3 },
			{ 5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2 }, { 9, 5, 6, 9, 6, 0, 0, 6, 2 },
			{ 1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8 }, { 1, 5, 6, 2, 1, 6 },
			{ 1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6 }, { 10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0 },
			{ 0, 3, 8, 5, 6, 10 }, { 10, 5, 6 }, { 11, 5, 10, 7, 5, 11 }, { 11, 5, 10, 11, 7, 5, 8, 3, 0 },
			{ 5, 11, 7, 5, 10, 11, 1, 9, 0 }, { 10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1 },
			{ 11, 1, 2, 11, 7, 1, 7, 5, 1 }, { 0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11 },
			{ 9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7 }, { 7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2 },
			{ 2, 5, 10, 2, 3, 5, 3, 7, 5 }, { 8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5 },
			{ 9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2 }, { 9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2 },
			{ 1, 3, 5, 3, 7, 5 }, { 0, 8, 7, 0, 7, 1, 1, 7, 5 }, { 9, 0, 3, 9, 3, 5, 5, 3, 7 }, { 9, 8, 7, 5, 9, 7 },
			{ 5, 8, 4, 5, 10, 8, 10, 11, 8 }, { 5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0 },
			{ 0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5 }, { 10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4 },
			{ 2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8 }, { 0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11 },
			{ 0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5 }, { 9, 4, 5, 2, 11, 3 },
			{ 2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4 }, { 5, 10, 2, 5, 2, 4, 4, 2, 0 },
			{ 3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9 }, { 5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2 },
			{ 8, 4, 5, 8, 5, 3, 3, 5, 1 }, { 0, 4, 5, 1, 0, 5 }, { 8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5 }, { 9, 4, 5 },
			{ 4, 11, 7, 4, 9, 11, 9, 10, 11 }, { 0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11 },
			{ 1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11 }, { 3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4 },
			{ 4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2 }, { 9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3 },
			{ 11, 7, 4, 11, 4, 2, 2, 4, 0 }, { 11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4 },
			{ 2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9 }, { 9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7 },
			{ 3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10 }, { 1, 10, 2, 8, 7, 4 }, { 4, 9, 1, 4, 1, 7, 7, 1, 3 },
			{ 4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1 }, { 4, 0, 3, 7, 4, 3 }, { 4, 8, 7 }, { 9, 10, 8, 10, 11, 8 },
			{ 3, 0, 9, 3, 9, 11, 11, 9, 10 }, { 0, 1, 10, 0, 10, 8, 8, 10, 11 }, { 3, 1, 10, 11, 3, 10 },
			{ 1, 2, 11, 1, 11, 9, 9, 11, 8 }, { 3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9 }, { 0, 2, 11, 8, 0, 11 },
			{ 3, 2, 11 }, { 2, 3, 8, 2, 8, 10, 10, 8, 9 }, { 9, 10, 2, 0, 9, 2 },
			{ 2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8 }, { 1, 10, 2 }, { 1, 3, 8, 9, 1, 8 }, { 0, 9, 1 }, { 0, 3, 8 },
			null };

	// //////////////////////////////////////////////////////////////
	// contour plane implementation
	// //////////////////////////////////////////////////////////////

	void generateContourData(boolean iHaveContourVertexesAlready) {

		/*
		 * (1) define the plane (2) calculate the grid "pixel" points (3) generate the contours using marching squares
		 * (4) The idea is to first just catalog the vertices, then see what we need to do about them.
		 * 
		 */

		if (nContours == 0 || nContours > nContourMax)
			nContours = defaultContourCount;
		Logger.info("generateContours:" + nContours);
		getPlanarVectors();
		setPlanarTransform();
		getPlanarOrigin();
		setupMatrix(planarMatrix, planarVectors);

		calcPixelVertexVectors();
		getPixelCounts();
		createPlanarSquares();

		loadPixelData(iHaveContourVertexesAlready);
		if (logMessages) {
			int n = pixelCounts[0] / 2;
			Logger.info(dumpArray("generateContourData", pixelData, n - 4, n + 4, n - 4, n + 4));
		}
		createContours();
		triangulateContours();
	}

	// (1) define the plane

	final Point3f planarOrigin = new Point3f();
	final Vector3f[] planarVectors = new Vector3f[3];
	final Vector3f[] unitPlanarVectors = new Vector3f[3];
	final float[] planarVectorLengths = new float[2];
	final Matrix3f matXyzToPlane = new Matrix3f();
	{
		planarVectors[0] = new Vector3f();
		planarVectors[1] = new Vector3f();
		planarVectors[2] = new Vector3f();
		unitPlanarVectors[0] = new Vector3f();
		unitPlanarVectors[1] = new Vector3f();
		unitPlanarVectors[2] = new Vector3f();
	}

	void getPlanarVectors() {
		/*
		 * Imagine a parallelpiped defined by our original Vx, Vy, Vz. We pick ONE of these to be our "contour type"
		 * defining vector. I call that particular vector Vz here. It is the vector best aligned with the normal to the
		 * plane we are interested in visualizing, for which the normal is N. (N is just {a b c} in ax + by + cz + d =
		 * 0.)
		 * 
		 * We want to know what the new Vx' and Vy' are going to be for the planar parallelogram defining our marching
		 * "squares".
		 * 
		 * Vx' = Vx - Vz * (Vx dot N) / (Vz dot N) Vy' = Vy - Vz * (Vy dot N) / (Vz dot N)
		 * 
		 * Thus, if we start with a rectangular grid and Vz IS N, then Vx dot N is zero, so Vx' = Vx; if were to poorly
		 * choose Vz such that it was perpendicular to N, then Vz dot N would be 0, and our grid would have an
		 * infinitely long side.
		 * 
		 * For clues, see http://mathworld.wolfram.com/Point-PlaneDistance.html
		 * 
		 */

		planarVectors[2].set(0, 0, 0);

		if (thePlane == null)
			return; // done already

		Vector3f vZ = volumetricVectors[contourType];
		float vZdotNorm = vZ.dot(thePlaneNormal);
		switch (contourType) {
		case 0: // x
			planarVectors[0].scaleAdd(-volumetricVectors[1].dot(thePlaneNormal) / vZdotNorm, vZ, volumetricVectors[1]);
			planarVectors[1].scaleAdd(-volumetricVectors[2].dot(thePlaneNormal) / vZdotNorm, vZ, volumetricVectors[2]);
			break;
		case 1: // y
			planarVectors[0].scaleAdd(-volumetricVectors[2].dot(thePlaneNormal) / vZdotNorm, vZ, volumetricVectors[2]);
			planarVectors[1].scaleAdd(-volumetricVectors[0].dot(thePlaneNormal) / vZdotNorm, vZ, volumetricVectors[0]);
			break;
		case 2: // z
			planarVectors[0].scaleAdd(-volumetricVectors[0].dot(thePlaneNormal) / vZdotNorm, vZ, volumetricVectors[0]);
			planarVectors[1].scaleAdd(-volumetricVectors[1].dot(thePlaneNormal) / vZdotNorm, vZ, volumetricVectors[1]);
		}
	}

	void setPlanarTransform() {
		planarVectorLengths[0] = planarVectors[0].length();
		planarVectorLengths[1] = planarVectors[1].length();
		unitPlanarVectors[0].normalize(planarVectors[0]);
		unitPlanarVectors[1].normalize(planarVectors[1]);
		unitPlanarVectors[2].cross(unitPlanarVectors[0], unitPlanarVectors[1]);

		setupMatrix(matXyzToPlane, unitPlanarVectors);
		matXyzToPlane.invert();

		float alpha = planarVectors[0].angle(planarVectors[1]);
		Logger.info("planar axes type " + contourType + " axis angle = " + (alpha / Math.PI * 180) + " normal="
				+ unitPlanarVectors[2]);
		for (int i = 0; i < 2; i++)
			Logger.info("planar vectors / lengths:" + planarVectors[i] + " / " + planarVectorLengths[i]);
		for (int i = 0; i < 3; i++)
			Logger.info("unit orthogonal plane vectors:" + unitPlanarVectors[i]);
	}

	void getPlanarOrigin() {
		/*
		 * just find the minimum value such that all coordinates are positive. note that this may be out of the actual
		 * range of data
		 * 
		 */
		planarOrigin.set(0, 0, 0);
		if (contourVertexCount == 0)
			return;

		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		planarOrigin.set(contourVertexes[0].vertexXYZ);
		for (int i = 0; i < contourVertexCount; i++) {
			pointVector.set(contourVertexes[i].vertexXYZ);
			xyzToPixelVector(pointVector);
			if (logMessages && i < 10)
				Logger.info("getPlanarOrigin: " + contourVertexes[i].vertexXYZ + " is 2D: " + pointVector);

			if (pointVector.x < minX)
				minX = pointVector.x;
			if (pointVector.y < minY)
				minY = pointVector.y;
		}
		if (logMessages)
			Logger.info("getPlanarOrigin: minX, minY: " + minX + "," + minY);
		planarOrigin.set(pixelPtToXYZ((int) (minX * 1.0001f), (int) (minY * 1.0001f)));
		Logger.info("generatePixelData planarOrigin = " + planarOrigin + ":" + locatePixel(planarOrigin));
	}

	// (2) calculate the grid points

	int contourVertexCount;
	ContourVertex[] contourVertexes;

	class ContourVertex {
		Point3f vertexXYZ = new Point3f();
		Point3i voxelLocation;
		int[] pixelLocation = new int[2];
		float value;
		int vertexIndex;

		ContourVertex(int x, int y, int z, Point3f vertexXYZ, int vPt) {
			this.vertexXYZ.set(vertexXYZ);
			voxelLocation = new Point3i(x, y, z);
			vertexIndex = vPt;
		}

		void setValue(float value) {
			this.value = value;
			voxelData[voxelLocation.x][voxelLocation.y][voxelLocation.z] = value;
			if (Math.abs(value) < 0.0000001)
				currentMesh.invalidateVertex(vertexIndex);

		}

		void setPixelLocation(Point3i pt) {
			pixelLocation[0] = pt.x;
			pixelLocation[1] = pt.y;
		}
	}

	int addContourData(int x, int y, int z, Point3i offsets, Point3f vertexXYZ, float value) {
		if (contourVertexes == null)
			contourVertexes = new ContourVertex[256];
		if (contourVertexCount == contourVertexes.length)
			contourVertexes = (ContourVertex[]) ArrayUtil.doubleLength(contourVertexes);
		x += offsets.x;
		y += offsets.y;
		z += offsets.z;
		int vPt = addVertexCopy(vertexXYZ, value, false, "");
		contourVertexes[contourVertexCount++] = new ContourVertex(x, y, z, vertexXYZ, vPt);
		if (Math.abs(value) < 0.0000001)
			currentMesh.invalidateVertex(vPt);
		currentMesh.firstViewableVertex = currentMesh.vertexCount;
		if (logMessages)
			Logger.info("addCountourdata " + x + " " + y + " " + z + offsets + vertexXYZ + value);
		return vPt;
	} // (3) generate the contours using marching squares

	final int[] pixelCounts = new int[2];
	final Matrix3f planarMatrix = new Matrix3f();
	float[][] pixelData;

	final float[] vertexValues2d = new float[4];
	final Point3f[] contourPoints = new Point3f[4];
	{
		for (int i = 4; --i >= 0;)
			contourPoints[i] = new Point3f();
	}
	final int[] contourPointIndexes = new int[4];
	int squareCountX, squareCountY;

	PlanarSquare[] planarSquares;
	int nSquares;

	class PlanarSquare {
		int[] edgeMask12; // one per contour
		int edgeMask12All;
		int nInside;
		int nOutside;
		int nThrough;
		int contourBits;
		int x, y;
		Point3f origin;
		int[] vertexes;
		int[][] intersectionPoints;

		PlanarSquare(Point3f origin, int x, int y) {
			edgeMask12 = new int[nContours];
			intersectionPoints = new int[nContours][4];
			vertexes = new int[4];
			edgeMask12All = 0;
			contourBits = 0;
			this.origin = origin;
			this.x = x;
			this.y = y;
		}

		void setIntersectionPoints(int contourIndex, int[] pts) {
			for (int i = 0; i < 4; i++)
				intersectionPoints[contourIndex][i] = pts[i];
		}

		void setVertex(int iV, int pt) {
			if (vertexes[iV] != 0 && vertexes[iV] != pt)
				Logger.error("IV IS NOT 0 or pt:" + iV + " " + vertexes[iV] + "!=" + pt);
			vertexes[iV] = pt;
		}

		void addEdgeMask(int contourIndex, int edgeMask4, int insideMask) {
			/*
			 * binary abcd abcd vvvv where abcd is edge intersection mask and vvvv is the inside/outside mask (0-15) the
			 * duplication is so that this can be used efficiently either as
			 */
			if (insideMask != 0)
				contourBits |= (1 << contourIndex);
			edgeMask12[contourIndex] = (((edgeMask4 << 4) + edgeMask4) << 4) + insideMask;
			edgeMask12All |= edgeMask12[contourIndex];
			if (insideMask == 0)
				++nOutside;
			else if (insideMask == 0xF)
				++nInside;
			else ++nThrough;
		}
	}

	void getPixelCounts() {
		// skipping the dimension designated by the contourType
		if (thePlane == null)
			return;
		int max = 1;
		for (int i = 0; i < 3; i++) {
			if (i != contourType)
				max = Math.max(max, voxelCounts[i]);
		}
		pixelCounts[0] = pixelCounts[1] = max;
		// just use the maximum value -- this isn't too critical,
		// but we want to have enough, and there were
		// problems with hkl = 110

		// if (logMessages)
		Logger.info("getPixelCounts " + pixelCounts[0] + "," + pixelCounts[1]);
	}

	void createPlanarSquares() {
		squareCountX = pixelCounts[0] - 1;
		squareCountY = pixelCounts[1] - 1;

		planarSquares = new PlanarSquare[squareCountX * squareCountY];
		nSquares = 0;
		for (int x = 0; x < squareCountX; x++)
			for (int y = 0; y < squareCountY; y++)
				planarSquares[nSquares++] = new PlanarSquare(null, x, y);
		Logger.info("nSquares = " + nSquares);
	}

	void loadPixelData(boolean iHaveContourVertexesAlready) {
		pixelData = new float[pixelCounts[0]][pixelCounts[1]];
		int x, y;
		Logger.info("loadPixelData haveContourVertices? " + iHaveContourVertexesAlready);
		contourPlaneMinimumValue = Float.MAX_VALUE;
		contourPlaneMaximumValue = -Float.MAX_VALUE;
		for (int i = 0; i < contourVertexCount; i++) {
			ContourVertex c = contourVertexes[i];
			Point3i pt = locatePixel(c.vertexXYZ);
			c.setPixelLocation(pt);
			float value;
			if (iHaveContourVertexesAlready) {
				value = c.value;
			}
			else {
				value = lookupInterpolatedVoxelValue(c.vertexXYZ);
				c.setValue(value);
			}
			if (value < contourPlaneMinimumValue)
				contourPlaneMinimumValue = value;
			if (value > contourPlaneMaximumValue)
				contourPlaneMaximumValue = value;
			if (logMessages)
				Logger.info("loadPixelData " + c.vertexXYZ + value + pt);
			if ((x = pt.x) >= 0 && x < pixelCounts[0] && (y = pt.y) >= 0 && y < pixelCounts[1]) {
				pixelData[x][y] = value;
				if (x != squareCountX && y != squareCountY)
					planarSquares[x * squareCountY + y].setVertex(0, c.vertexIndex);
				if (x != 0 && y != squareCountY)
					planarSquares[(x - 1) * squareCountY + y].setVertex(1, c.vertexIndex);
				if (y != 0 && x != squareCountX)
					planarSquares[x * squareCountY + y - 1].setVertex(3, c.vertexIndex);
				if (y != 0 && x != 0)
					planarSquares[(x - 1) * squareCountY + y - 1].setVertex(2, c.vertexIndex);
			}
			else {
				Logger.error("loadPixelData out of bounds: " + pt.x + " " + pt.y + "?");
			}
		}
	}

	int contourIndex;

	void createContours() {
		colorFractionBase = defaultColorFractionBase;
		colorFractionRange = defaultColorFractionRange;
		setMapRanges();
		float min = valueMappedToRed;
		float max = valueMappedToBlue;
		float diff = max - min;
		Logger.info("generateContourData min=" + min + " max=" + max + " nContours=" + nContours);
		for (int i = 0; i < nContours; i++) {
			contourIndex = i;
			float cutoff = min + (i * 1f / nContours) * diff;
			/*
			 * cutoffs right near zero cause problems, so we adjust just a tad
			 * 
			 */
			generateContourData(cutoff);
		}
	}

	void generateContourData(float contourCutoff) {

		/*
		 * Y 3 ---2---- 2 | | | | 3 1 | | 0 ---0---- 1 X
		 */

		int[][] isoPointIndexes2d = new int[squareCountY][4];
		for (int i = squareCountY; --i >= 0;)
			isoPointIndexes2d[i][0] = isoPointIndexes2d[i][1] = isoPointIndexes2d[i][2] = isoPointIndexes2d[i][3] = -1; // new
		// int[4];

		if (Math.abs(contourCutoff) < 0.0001)
			contourCutoff = (contourCutoff <= 0 ? -0.0001f : 0.0001f);
		int insideCount = 0, outsideCount = 0, contourCount = 0;
		for (int x = squareCountX; --x >= 0;) {
			for (int y = squareCountY; --y >= 0;) {
				int[] pixelPointIndexes = propagateNeighborPointIndexes2d(x, y, isoPointIndexes2d);
				int insideMask = 0;
				for (int i = 4; --i >= 0;) {
					Point3i offset = squareVertexOffsets[i];
					float vertexValue = pixelData[x + offset.x][y + offset.y];
					vertexValues2d[i] = vertexValue;
					if (isInside2d(vertexValue, contourCutoff))
						insideMask |= 1 << i;
				}
				if (insideMask == 0) {
					++outsideCount;
					continue;
				}
				if (insideMask == 0x0F) {
					++insideCount;
					planarSquares[x * squareCountY + y].addEdgeMask(contourIndex, 0, 0x0F);
					continue;
				}
				++contourCount;
				processOneQuadrilateral(insideMask, contourCutoff, pixelPointIndexes, x, y);
			}
		}

		if (logMessages)
			Logger.info("contourCutoff=" + contourCutoff + " pixel squares=" + squareCountX + "," + squareCountY + ","
					+ " total=" + (squareCountX * squareCountY) + "\n" + " insideCount=" + insideCount
					+ " outsideCount=" + outsideCount + " contourCount=" + contourCount + " total="
					+ (insideCount + outsideCount + contourCount));
	}

	boolean isInside2d(float voxelValue, float max) {
		return (max > 0 && voxelValue >= max) || (max <= 0 && voxelValue <= max);
	}

	final int[] nullNeighbor2d = { -1, -1, -1, -1 };

	int[] propagateNeighborPointIndexes2d(int x, int y, int[][] isoPointIndexes2d) {

		// propagates only the intersection point -- one in the case of a square

		int[] pixelPointIndexes = isoPointIndexes2d[y];

		boolean noXNeighbor = (x == squareCountX - 1);
		// the x neighbor is myself from my last pass through here
		if (noXNeighbor) {
			pixelPointIndexes[0] = -1;
			pixelPointIndexes[1] = -1;
			pixelPointIndexes[2] = -1;
			pixelPointIndexes[3] = -1;
		}
		else {
			pixelPointIndexes[1] = pixelPointIndexes[3];
		}

		// from my y neighbor
		boolean noYNeighbor = (y == squareCountY - 1);
		pixelPointIndexes[2] = (noYNeighbor ? -1 : isoPointIndexes2d[y + 1][0]);

		// these must always be calculated
		pixelPointIndexes[0] = -1;
		pixelPointIndexes[3] = -1;
		return pixelPointIndexes;
	}

	void processOneQuadrilateral(int insideMask, float cutoff, int[] pixelPointIndexes, int x, int y) {
		int edgeMask = insideMaskTable2d[insideMask];
		planarSquares[x * squareCountY + y].addEdgeMask(contourIndex, edgeMask, insideMask);
		for (int iEdge = 4; --iEdge >= 0;) {
			if ((edgeMask & (1 << iEdge)) == 0) {
				continue;
			}
			if (pixelPointIndexes[iEdge] >= 0)
				continue; // propagated from neighbor
			int vertexA = edgeVertexes2d[2 * iEdge];
			int vertexB = edgeVertexes2d[2 * iEdge + 1];
			float valueA = vertexValues2d[vertexA];
			float valueB = vertexValues2d[vertexB];
			if (thePlane == null) // contouring f(x,y)
				calcVertexPoints3d(x, y, vertexA, vertexB);
			else calcVertexPoints2d(x, y, vertexA, vertexB);
			calcContourPoint(cutoff, valueA, valueB, contourPoints[iEdge]);
			pixelPointIndexes[iEdge] = addVertexCopy(contourPoints[iEdge], cutoff, false, "");
		}
		// this must be a square that is involved in this particular contour
		planarSquares[x * squareCountY + y].setIntersectionPoints(contourIndex, pixelPointIndexes);
	}

	final Point3f pixelOrigin = new Point3f();
	final Point3f pixelT = new Point3f();

	void calcVertexPoints2d(int x, int y, int vertexA, int vertexB) {
		pixelOrigin.scaleAdd(x, planarVectors[0], planarOrigin);
		pixelOrigin.scaleAdd(y, planarVectors[1], pixelOrigin);
		pointA.add(pixelOrigin, pixelVertexVectors[vertexA]);
		pointB.add(pixelOrigin, pixelVertexVectors[vertexB]);
	}

	void calcVertexPoints3d(int x, int y, int vertexA, int vertexB) {
		contourLocateXYZ(x + squareVertexOffsets[vertexA].x, y + squareVertexOffsets[vertexA].y, pointA);
		contourLocateXYZ(x + squareVertexOffsets[vertexB].x, y + squareVertexOffsets[vertexB].y, pointB);
	}

	void contourLocateXYZ(int ix, int iy, Point3f pt) {
		int i = findContourVertex(ix, iy);
		if (i < 0) {
			pt.x = Float.NaN;
			return;
		}
		ContourVertex c = contourVertexes[i];
		pt.set(c.vertexXYZ);
	}

	int findContourVertex(int ix, int iy) {
		for (int i = 0; i < contourVertexCount; i++) {
			if (contourVertexes[i].pixelLocation[0] == ix && contourVertexes[i].pixelLocation[1] == iy)
				return i;
		}
		return -1;
	}

	float calcContourPoint(float cutoff, float valueA, float valueB, Point3f contourPoint) {

		float diff = valueB - valueA;
		float fraction = (cutoff - valueA) / diff;
		edgeVector.sub(pointB, pointA);
		contourPoint.scaleAdd(fraction, edgeVector, pointA);
		return fraction;
	}

	Vector3f[] pixelVertexVectors = new Vector3f[4];

	void calcPixelVertexVectors() {
		for (int i = 4; --i >= 0;)
			pixelVertexVectors[i] = calcPixelVertexVector(squareVertexVectors[i]);
	}

	Vector3f calcPixelVertexVector(Vector3f squareVector) {
		Vector3f v = new Vector3f();
		planarMatrix.transform(squareVector, v);
		return v;
	}

	void triangulateContours() {
		currentMesh.vertexColixes = new short[currentMesh.vertexCount];

		for (int i = 0; i < nContours; i++) {
			if (thisContour <= 0 || thisContour == i + 1)
				createContourTriangles(i);
		}
	}

	void createContourTriangles(int contourIndex) {
		for (int i = 0; i < nSquares; i++) {
			triangulateContourSquare(i, contourIndex);
		}
	}

	void triangulateContourSquare(int squareIndex, int contourIndex) {
		PlanarSquare square = planarSquares[squareIndex];
		int edgeMask0 = square.edgeMask12[contourIndex] & 0x00FF;
		if (edgeMask0 == 0) // all outside
			return;

		// unnecessary inside square?
		// full square and next contour is also a full square there
		if (edgeMask0 == 15 && contourIndex + 1 < nContours && square.edgeMask12[contourIndex + 1] == 15)
			return;

		// still working here.... not efficient; stubbornly trying to avoid just
		// writing the damn triangle table.
		boolean isOK = true;
		int edgeMask = edgeMask0;
		if (contourIndex < nContours - 1) {
			edgeMask0 = square.edgeMask12[contourIndex + 1];
			if (edgeMask0 != 0x0F) {
				isOK = false;
				if (((edgeMask ^ edgeMask0) & 0xF0) == 0) {
					isOK = false;
				}
				edgeMask &= 0x0FF;
				edgeMask ^= edgeMask0 & 0x0F0F;
			}
		}
		if (contourIndex > 0 && edgeMask == 0)
			return;
		fillSquare(square, contourIndex, edgeMask, false);
		if (!isOK) // a lazy hack instead of really figuring out the order
			fillSquare(square, contourIndex, edgeMask, true);
	}

	int[] triangleVertexList = new int[20];

	void fillSquare(PlanarSquare square, int contourIndex, int edgeMask, boolean reverseWinding) {
		int vPt = 0;
		boolean flip = reverseWinding;
		int nIntersect = 0;
		boolean newIntersect;
		for (int i = 0; i < 4; i++) {
			newIntersect = false;
			if ((edgeMask & (1 << i)) != 0) {
				triangleVertexList[vPt++] = square.vertexes[i];
			}
			// order here needs to be considered for when Edges(A)==Edges(B)
			// for proper winding -- isn't up to snuff

			if (flip && (edgeMask & (1 << (8 + i))) != 0) {
				nIntersect++;
				newIntersect = true;
				triangleVertexList[vPt++] = square.intersectionPoints[contourIndex + 1][i];
			}
			if ((edgeMask & (1 << (4 + i))) != 0) {
				nIntersect++;
				newIntersect = true;
				triangleVertexList[vPt++] = square.intersectionPoints[contourIndex][i];
			}
			if (!flip && (edgeMask & (1 << (8 + i))) != 0) {
				nIntersect++;
				newIntersect = true;
				triangleVertexList[vPt++] = square.intersectionPoints[contourIndex + 1][i];
			}
			if (nIntersect == 2 && newIntersect)
				flip = !flip;
		}
		/*
		 * Logger.debug("\nfillSquare (" + square.x + " " + square.y + ") " + contourIndex + " " +
		 * binaryString(edgeMask) + "\n"); Logger.debug("square vertexes:" + dumpIntArray(square.vertexes, 4));
		 * Logger.debug("square inters. pts:" + dumpIntArray(square.intersectionPoints[contourIndex], 4));
		 * Logger.debug(dumpIntArray(triangleVertexList, vPt));
		 */
		createTriangleSet(vPt);
	}

	void createTriangleSet(int nVertex) {
		int k = triangleVertexList[1];
		for (int i = 2; i < nVertex; i++) {
			currentMesh.addTriangle(triangleVertexList[0], k, triangleVertexList[i]);
			k = triangleVertexList[i];
		}
	}

	final static Point3i[] squareVertexOffsets = { new Point3i(0, 0, 0), new Point3i(1, 0, 0), new Point3i(1, 1, 0),
			new Point3i(0, 1, 0) };

	final static Vector3f[] squareVertexVectors = { new Vector3f(0, 0, 0), new Vector3f(1, 0, 0),
			new Vector3f(1, 1, 0), new Vector3f(0, 1, 0) };

	final static byte edgeVertexes2d[] = { 0, 1, 1, 2, 2, 3, 3, 0 };

	final static byte insideMaskTable2d[] = { 0, 9, 3, 10, 6, 15, 5, 12, 12, 5, 15, 6, 10, 3, 9, 0 };

	// position in the table corresponds to the binary equivalent of which corners are inside
	// for example, 0th is completely outside; 15th is completely inside;
	// the 4th entry (0b0100; 2**3), corresponding to only the third corner inside, is 6 (0b1100).
	// Bits 2 and 3 are set, so edges 2 and 3 intersect the contour.

	// //////// debug utility methods /////////

	String binaryString(int value) {
		String str = "0b";
		if (value == 0)
			return "0";
		int i = 0;
		while (value != 0) {
			str += (value % 2 == 1 ? "1" : "0");
			value = value >> 1;
			if (i++ != 0 && i % 4 == 0)
				str += " ";
		}
		return str;
	}

	String dumpArray(String msg, float[][] A, int x1, int x2, int y1, int y2) {
		String s = "dumpArray: " + msg + "\n";
		for (int x = x1; x <= x2; x++)
			s += "\t*" + x + "*";
		for (int y = y2; y >= y1; y--) {
			s += "\n*" + y + "*";
			for (int x = x1; x <= x2; x++)
				s += "\t" + (x < A.length && y < A[x].length ? A[x][y] : Float.NaN);
		}
		return s;
	}

	String dumpIntArray(int[] A, int n) {
		String str = "";
		for (int i = 0; i < n; i++)
			str += " " + A[i];
		return str;
	}

	void voxelPtToXYZ(int x, int y, int z, Point3f pt) {
		pt.scaleAdd(x, volumetricVectors[0], volumetricOrigin);
		pt.scaleAdd(y, volumetricVectors[1], pt);
		pt.scaleAdd(z, volumetricVectors[2], pt);
		return;
	}

	float scaleByVoxelVector(Vector3f vector, int voxelVectorIndex) {
		// ORTHOGONAL ONLY!!! -- required for creating planes
		return (vector.dot(unitVolumetricVectors[voxelVectorIndex]) / volumetricVectorLengths[voxelVectorIndex]);
	}

	void xyzToVoxelPt(Point3f point, Point3f pt2) {
		pointVector.set(point);
		pointVector.sub(volumetricOrigin);
		pt2.x = scaleByVoxelVector(pointVector, 0);
		pt2.y = scaleByVoxelVector(pointVector, 1);
		pt2.z = scaleByVoxelVector(pointVector, 2);
	}

	void xyzToVoxelPt(float x, float y, float z, Point3i pt2) {
		pointVector.set(x, y, z);
		pointVector.sub(volumetricOrigin);
		ptXyzTemp.x = scaleByVoxelVector(pointVector, 0);
		ptXyzTemp.y = scaleByVoxelVector(pointVector, 1);
		ptXyzTemp.z = scaleByVoxelVector(pointVector, 2);
		pt2.set((int) ptXyzTemp.x, (int) ptXyzTemp.y, (int) ptXyzTemp.z);
	}

	void offsetCenter() {
		Point3f pt = new Point3f();
		pt.scaleAdd((voxelCounts[0] - 1) / 2f, volumetricVectors[0], pt);
		pt.scaleAdd((voxelCounts[1] - 1) / 2f, volumetricVectors[1], pt);
		pt.scaleAdd((voxelCounts[2] - 1) / 2f, volumetricVectors[2], pt);
		volumetricOrigin.sub(center, pt);
	}

	Point3f pixelPtToXYZ(int x, int y) {
		Point3f ptXyz = new Point3f();
		ptXyz.scaleAdd(x, planarVectors[0], planarOrigin);
		ptXyz.scaleAdd(y, planarVectors[1], ptXyz);
		return ptXyz;
	}

	final Point3i ptiTemp = new Point3i();

	Point3i locatePixel(Point3f ptXyz) {
		pointVector.set(ptXyz);
		xyzToPixelVector(pointVector);
		ptiTemp.x = (int) (pointVector.x + 0.5f);
		// NOTE: fails if negative -- (int) (-0.9 + 0.5) = (int) (-0.4) = 0
		ptiTemp.y = (int) (pointVector.y + 0.5f);
		return ptiTemp;
	}

	void xyzToPixelVector(Vector3f vector) {
		// factored for nonorthogonality; assumes vector is IN the plane already
		vector.sub(vector, planarOrigin);
		matXyzToPlane.transform(vector);
		vector.x /= planarVectorLengths[0];
		vector.y /= planarVectorLengths[1];
	}

	// ///// for any sort of functional mapping ////////

	void getCalcPoint(Point3f pt) {

		pt.sub(center);
		if (isEccentric)
			eccentricityMatrixInverse.transform(pt);
		if (isAnisotropic) {
			pt.x /= anisotropy[0];
			pt.y /= anisotropy[1];
			pt.z /= anisotropy[2];
		}
	}

	class Voxel extends Point3i {
		Point3f ptXyz = new Point3f();
		float value;

		void setValue(int x, int y, int z, float value) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.value = value;
			voxelPtToXYZ(x, y, z, this.ptXyz);
		}

		void setValue(float value) {
			if (this.value < value)
				return;
			if (logCube)
				Logger.info("voxel.setValue " + x + " " + y + " " + z + ptXyz + ": " + value + " was " + this.value);
			this.value = value;
		}
	}

	int setVoxelRange(int index, float min, float max, float ptsPerAngstrom, int gridMax) {
		float range = max - min;
		int nGrid;
		if (resolution != Float.MAX_VALUE) {
			ptsPerAngstrom = resolution;
			nGrid = (int) (range * ptsPerAngstrom);
		}
		else {
			nGrid = (int) (range * ptsPerAngstrom);
		}
		if (nGrid > gridMax) {
			if ((dataType & HAS_MAXGRID) > 0) {
				if (resolution != Float.MAX_VALUE)
					Logger.info("Maximum number of voxels for index=" + index);
				nGrid = gridMax;
			}
			else if (resolution == Float.MAX_VALUE) {
				nGrid = gridMax;
			}
		}
		ptsPerAngstrom = nGrid / range;
		float d = volumetricVectorLengths[index] = 1f / ptsPerAngstrom;
		voxelCounts[index] = nGrid + ((dataType & IS_SOLVENTTYPE) != 0 ? 3 : 0);

		switch (index) {
		case 0:
			volumetricVectors[0].set(d, 0, 0);
			volumetricOrigin.x = min;
			break;
		case 1:
			volumetricVectors[1].set(0, d, 0);
			volumetricOrigin.y = min;
			break;
		case 2:
			volumetricVectors[2].set(0, 0, d);
			volumetricOrigin.z = min;
			if (isEccentric)
				eccentricityMatrix.transform(volumetricOrigin);
			if (center.x != Float.MAX_VALUE)
				volumetricOrigin.add(center);
		}
		if (isEccentric)
			eccentricityMatrix.transform(volumetricVectors[index]);
		unitVolumetricVectors[index].normalize(volumetricVectors[index]);
		return voxelCounts[index];
	}

	String jvxlGetVolumeHeader(int nAtoms) {
		String str = (-nAtoms) + " " + (volumetricOrigin.x / ANGSTROMS_PER_BOHR) + " "
				+ (volumetricOrigin.y / ANGSTROMS_PER_BOHR) + " " + (volumetricOrigin.z / ANGSTROMS_PER_BOHR) + "\n";
		for (int i = 0; i < 3; i++)
			str += voxelCounts[i] + " " + (volumetricVectors[i].x / ANGSTROMS_PER_BOHR) + " "
					+ (volumetricVectors[i].y / ANGSTROMS_PER_BOHR) + " "
					+ (volumetricVectors[i].z / ANGSTROMS_PER_BOHR) + "\n";
		return str;
	}

	// just for small factorials
	static float factorial(int n) {
		if (n == 0)
			return 1;
		return n * factorial(n - 1);
	}

	float[] fact = new float[20];

	float getDefaultResolution() {
		return Float.MAX_VALUE;
		// for popup menu?
		/*
		 * maybe.... int nAtoms = viewer.getAtomCount(); float res = 0; if (nAtoms < 200) res = 4; else if (nAtoms <
		 * 2000) res = 1; return res;
		 */
	}

	// ///// spheres and ellipsoids //////

	int sphere_gridMax = 20;
	float sphere_ptsPerAngstrom = 10f;
	float sphere_radiusAngstroms;

	void setupSphere() {
		if (center.x == Float.MAX_VALUE)
			center.set(0, 0, 0);
		float radius = sphere_radiusAngstroms * 1.1f * eccentricityScale;
		for (int i = 0; i < 3; i++)
			setVoxelRange(i, -radius, radius, sphere_ptsPerAngstrom, sphere_gridMax);
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("SPHERE \nres="
				+ sphere_ptsPerAngstrom
				+ " rad="
				+ sphere_radiusAngstroms
				+ (isAnisotropic ? " anisotropy=(" + anisotropy[0] + "," + anisotropy[1] + "," + anisotropy[2] + ")"
						: "") + "\n");
		jvxlFileHeader.append(jvxlGetVolumeHeader(2));
		atomCount = 0;
		negativeAtomCount = false;
	}

	float getSphereValue(int x, int y, int z) {
		voxelPtToXYZ(x, y, z, ptPsi);
		getCalcPoint(ptPsi);
		return sphere_radiusAngstroms - (float) Math.sqrt(ptPsi.x * ptPsi.x + ptPsi.y * ptPsi.y + ptPsi.z * ptPsi.z);
	}

	// ///// hydrogen-like Schroedinger orbitals ///////

	int psi_gridMax = 40;
	float psi_ptsPerAngstrom = 5f;
	float psi_radiusAngstroms;

	double[] rfactor = new double[10];
	double[] pfactor = new double[10];
	int lastFactorial = -1;

	final static double A0 = 0.52918f; // x10^-10 meters

	final static double ROOT2 = 1.414214;
	int psi_n = 2;
	int psi_l = 1;
	int psi_m = 1;
	float psi_Znuc = 1; // hydrogen

	void setupOrbital() {
		psi_radiusAngstroms = autoScaleOrbital();
		if (center.x == Float.MAX_VALUE)
			center.set(0, 0, 0);
		for (int i = 0; i < 3; i++)
			setVoxelRange(i, -psi_radiusAngstroms, psi_radiusAngstroms, psi_ptsPerAngstrom, psi_gridMax);
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("hydrogen-like orbital \nn="
				+ psi_n
				+ ", l="
				+ psi_l
				+ ", m="
				+ psi_m
				+ " Znuc="
				+ psi_Znuc
				+ " res="
				+ psi_ptsPerAngstrom
				+ " rad="
				+ psi_radiusAngstroms
				+ (isAnisotropic ? " anisotropy=(" + anisotropy[0] + "," + anisotropy[1] + "," + anisotropy[2] + ")"
						: "") + "\n");
		jvxlFileHeader.append(jvxlGetVolumeHeader(2));
		atomCount = 0;
		negativeAtomCount = false;
		calcFactors(psi_n, psi_l, psi_m);
	}

	float autoScaleOrbital() {
		float w = (psi_n * (psi_n + 3) - 5f) / psi_Znuc;
		if (w < 1)
			w = 1;
		if (psi_n < 3)
			w += 1;
		float aMax = 0;
		if (!isAnisotropic)
			return w;
		for (int i = 3; --i >= 0;)
			if (anisotropy[i] > aMax)
				aMax = anisotropy[i];
		return w * aMax;
	}

	void calcFactors(int n, int el, int m) {
		int abm = Math.abs(m);
		if (lastFactorial < n + el) {
			for (int i = lastFactorial + 1; i <= n + el; i++)
				fact[i] = factorial(i);
			lastFactorial = n + el;
		}
		double Nnl = Math.pow(2 * psi_Znuc / n / A0, 1.5)
				* Math.sqrt(fact[n - el - 1] / 2 / n / Math.pow(fact[n + el], 3));
		double Lnl = fact[n + el] * fact[n + el];
		double Plm = Math.pow(2, -el) * fact[el] * fact[el + abm]
				* Math.sqrt((2 * el + 1) * fact[el - abm] / 2 / fact[el + abm]);

		for (int p = 0; p <= n - el - 1; p++)
			rfactor[p] = Nnl * Lnl / fact[p] / fact[n - el - p - 1] / fact[2 * el + p + 1];
		for (int p = abm; p <= el; p++)
			pfactor[p] = Math.pow(-1, el - p) * Plm / fact[p] / fact[el + abm - p] / fact[el - p] / fact[p - abm];
	}

	final Point3f ptPsi = new Point3f();

	float getPsi(int x, int y, int z) {
		voxelPtToXYZ(x, y, z, ptPsi);
		getCalcPoint(ptPsi);
		return (float) hydrogenAtomPsiAt(ptPsi, psi_n, psi_l, psi_m);
	}

	double hydrogenAtomPsiAt(Point3f pt, int n, int el, int m) {
		// ref: http://www.stolaf.edu/people/hansonr/imt/concept/schroed.pdf
		int abm = Math.abs(m);
		double x2y2 = pt.x * pt.x + pt.y * pt.y;
		double r2 = x2y2 + pt.z * pt.z;
		double r = Math.sqrt(r2);
		double rho = 2d * psi_Znuc * r / n / A0;
		double ph, th, cth, sth;
		double theta_lm = 0;
		double phi_m = 0;
		double sum = 0;
		for (int p = 0; p <= n - el - 1; p++)
			sum += Math.pow(-rho, p) * rfactor[p];
		double rnl = Math.exp(-rho / 2) * Math.pow(rho, el) * sum;
		ph = Math.atan2(pt.y, pt.x);
		th = Math.atan2(Math.sqrt(x2y2), pt.z);
		cth = Math.cos(th);
		sth = Math.sin(th);
		sum = 0;
		for (int p = abm; p <= el; p++)
			sum += Math.pow(1 + cth, p - abm) * Math.pow(1 - cth, el - p) * pfactor[p];
		theta_lm = Math.abs(Math.pow(sth, abm)) * sum;
		if (m == 0)
			phi_m = 1;
		else if (m > 0)
			phi_m = Math.cos(m * ph) * ROOT2;
		else phi_m = Math.sin(-m * ph) * ROOT2;
		if (Math.abs(phi_m) < 0.0000000001)
			phi_m = 0;
		return rnl * theta_lm * phi_m;
	}

	// ////// lobes ////////

	float lobe_sizeAngstroms = 1.0f;
	int lobe_gridMax = 21;
	int lobe_ptsPerAngstrom = 10;

	void setupLobe() {
		psi_n = 3;
		psi_l = 2;
		psi_m = 0;
		// using Hens Borkent's 3dz2 lobe idea
		psi_Znuc = 15;
		if (center.x == Float.MAX_VALUE)
			center.set(0, 0, 0);
		float radius = lobe_sizeAngstroms * 1.1f * eccentricityRatio * eccentricityScale;
		if (eccentricityScale > 0 && eccentricityScale < 1)
			radius /= eccentricityScale;
		setVoxelRange(0, -radius, radius, lobe_ptsPerAngstrom, lobe_gridMax);
		setVoxelRange(1, -radius, radius, lobe_ptsPerAngstrom, lobe_gridMax);
		setVoxelRange(2, 0, radius / eccentricityRatio, lobe_ptsPerAngstrom, lobe_gridMax);
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("lobe \nn="
				+ psi_n
				+ ", l="
				+ psi_l
				+ ", m="
				+ psi_m
				+ " Znuc="
				+ psi_Znuc
				+ " res="
				+ lobe_ptsPerAngstrom
				+ " rad="
				+ radius
				+ (isAnisotropic ? " anisotropy=(" + anisotropy[0] + "," + anisotropy[1] + "," + anisotropy[2] + ")"
						: "") + "\n");
		jvxlFileHeader.append(jvxlGetVolumeHeader(2));
		atomCount = 0;
		negativeAtomCount = false;
		calcFactors(psi_n, psi_l, psi_m);
	}

	float getLobeValue(int x, int y, int z) {
		voxelPtToXYZ(x, y, z, ptPsi);
		getCalcPoint(ptPsi);
		float value = (float) hydrogenAtomPsiAt(ptPsi, psi_n, psi_l, psi_m);
		if (value < 0)
			value = 0;
		return value;
	}

	// ///// ab initio/semiempirical quantum mechanical orbitals ///////

	int qm_gridMax = QuantumCalculation.MAX_GRID;
	float qm_ptsPerAngstrom = 10f;
	float qm_marginAngstroms = 1f; // may have to adjust this
	int qm_nAtoms;
	int qm_moNumber = Integer.MAX_VALUE;

	Atom[] qm_atoms;

	void addMOTitleInfo(int iLine, Hashtable mo) {
		String line = title[iLine];
		int pt = line.indexOf("%");
		if (line.length() == 0 || pt < 0)
			return;
		boolean replaced = false;
		for (int i = pt; i < line.length() - 1; i++) {
			if (line.charAt(i) == '%') {
				String info = "";
				switch (line.charAt(i + 1)) {
				case 'F':
					info = viewer.getFileName();
					break;
				case 'I':
					info += qm_moNumber;
					break;
				case 'N':
					info += qmOrbitalCount;
					break;
				case 'M':
					info += viewer.getModelNumber(viewer.getDisplayModelIndex());
					break;
				case 'E':
					info += mo.get("energy");
					break;
				case 'U':
					if (mo.containsKey("energyUnits"))
						info += moData.get("energyUnits");
					break;
				case 'S':
					if (mo.containsKey("symmetry"))
						info += mo.get("symmetry");
					break;
				case 'O':
					if (mo.containsKey("occupancy"))
						info += mo.get("occupancy");
					break;
				}
				replaced |= (info.length() > 0);
				line = line.substring(0, i) + info + line.substring(i + 2);
				i += info.length();
			}
		}
		line = (replaced || line.charAt(0) != '?' ? line : "");
		title[iLine] = (line.length() > 1 && line.charAt(0) == '?' ? line.substring(1) : line);
	}

	void setupQMOrbital() {
		Atom[] atoms = frame.atoms;
		Point3f xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Point3f xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		int modelIndex = viewer.getDisplayModelIndex();
		int iAtom = 0;
		int nSelected = 0;
		int nAtoms = viewer.getAtomCount();
		iUseBitSets = true;
		for (int i = 0; i < nAtoms; i++)
			if (atoms[i].modelIndex == modelIndex) {
				++iAtom;
				if (bsSelected == null || bsSelected.get(i))
					++nSelected;
			}
		qm_nAtoms = iAtom;
		if (nSelected > 0)
			Logger.info(nSelected + " of " + qm_nAtoms + " atoms will be used in the orbital calculation");
		if (qm_nAtoms > 0)
			qm_atoms = new Atom[qm_nAtoms];
		iAtom = 0;
		for (int i = 0; i < nAtoms; i++) {
			Atom atom = atoms[i];
			if (atom.modelIndex != modelIndex)
				continue;
			Point3f pt = new Point3f(atom);
			if (nSelected == 0 || bsSelected == null || bsSelected.get(i)) {
				float rA = atom.getVanderwaalsRadiusFloat() + qm_marginAngstroms;
				if (pt.x - rA < xyzMin.x)
					xyzMin.x = pt.x - rA;
				if (pt.x + rA > xyzMax.x)
					xyzMax.x = pt.x + rA;
				if (pt.y - rA < xyzMin.y)
					xyzMin.y = pt.y - rA;
				if (pt.y + rA > xyzMax.y)
					xyzMax.y = pt.y + rA;
				if (pt.z - rA < xyzMin.z)
					xyzMin.z = pt.z - rA;
				if (pt.z + rA > xyzMax.z)
					xyzMax.z = pt.z + rA;
				qm_atoms[iAtom++] = atom;
			}
			else {
				++iAtom;
			}
		}
		if (!Float.isNaN(scale)) {
			Vector3f v = new Vector3f(xyzMax);
			v.sub(xyzMin);
			v.scale(0.5f);
			xyzMin.add(v);
			v.scale(scale);
			xyzMax.set(xyzMin);
			xyzMax.add(v);
			xyzMin.sub(v);
		}

		Logger.info("MO range bohr " + xyzMin + " to " + xyzMax);
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("MO range bohr " + xyzMin + " to " + xyzMax + "\ncalculation type: "
				+ moData.get("calculationType") + "\n");

		int maxGrid = qm_gridMax;

		setVoxelRange(0, xyzMin.x, xyzMax.x, qm_ptsPerAngstrom, maxGrid);
		setVoxelRange(1, xyzMin.y, xyzMax.y, qm_ptsPerAngstrom, maxGrid);
		setVoxelRange(2, xyzMin.z, xyzMax.z, qm_ptsPerAngstrom, maxGrid);
		jvxlFileHeader.append(jvxlGetVolumeHeader(iAtom));
		Point3f pt = new Point3f();
		for (int i = 0; i < nAtoms; i++) {
			Atom atom = atoms[i];
			if (atom.modelIndex != modelIndex)
				continue;
			pt.set(atom);
			pt.scale(1 / ANGSTROMS_PER_BOHR);
			jvxlFileHeader.append(atom.getAtomicAndIsotopeNumber() + " " + atom.getAtomicAndIsotopeNumber() + ".0 "
					+ pt.x + " " + pt.y + " " + pt.z + "\n");
		}
		atomCount = -Integer.MAX_VALUE;
		negativeAtomCount = false;
		precalculateVoxelData = true;
	}

	void generateQuantumCube() {
		QuantumCalculation q;
		float[] origin = { volumetricOrigin.x, volumetricOrigin.y, volumetricOrigin.z };
		switch (qmOrbitalType) {
		case QM_TYPE_GAUSSIAN:
			q = new QuantumCalculation((String) moData.get("calculationType"), qm_atoms, (Vector) moData.get("shells"),
					(float[][]) moData.get("gaussians"), (Hashtable) moData.get("atomicOrbitalOrder"), null, null,
					moCoefficients);
			q.createGaussianCube(voxelData, voxelCounts, origin, volumetricVectorLengths);
			break;
		case QM_TYPE_SLATER:
			q = new QuantumCalculation((String) moData.get("calculationType"), qm_atoms, (Vector) moData.get("shells"),
					null, null, (int[][]) moData.get("slaterInfo"), (float[][]) moData.get("slaterData"),
					moCoefficients);
			q.createSlaterCube(voxelData, voxelCounts, origin, volumetricVectorLengths);
			break;
		default:
		}
	}

	// ///// molecular electrostatic potential ///////

	int mep_gridMax = MepCalculation.MAX_GRID;
	float mep_ptsPerAngstrom = 3f;
	float mep_marginAngstroms = 1f; // may have to adjust this
	int mep_nAtoms;

	Atom[] mep_atoms;

	void setupMep() {
		Atom[] atoms = frame.atoms;
		Point3f xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Point3f xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		int modelIndex = viewer.getDisplayModelIndex();
		int iAtom = 0;
		int nSelected = 0;
		iUseBitSets = true;
		int nAtoms = viewer.getAtomCount();
		for (int i = 0; i < nAtoms; i++)
			if (atoms[i].modelIndex == modelIndex) {
				++iAtom;
				if (bsSelected.get(i))
					++nSelected;
			}
		mep_nAtoms = iAtom;
		if (nSelected > 0)
			Logger.info(nSelected + " of " + mep_nAtoms + " atoms will be used in the mep calculation");
		if (mep_nAtoms > 0)
			mep_atoms = new Atom[mep_nAtoms];
		iAtom = 0;
		for (int i = 0; i < nAtoms; i++) {
			Atom atom = atoms[i];
			if (atom.modelIndex != modelIndex)
				continue;
			Point3f pt = new Point3f(atom);
			if (bsSelected.get(i)) {
				float rA = atom.getVanderwaalsRadiusFloat() + mep_marginAngstroms;
				if (pt.x - rA < xyzMin.x)
					xyzMin.x = pt.x - rA;
				if (pt.x + rA > xyzMax.x)
					xyzMax.x = pt.x + rA;
				if (pt.y - rA < xyzMin.y)
					xyzMin.y = pt.y - rA;
				if (pt.y + rA > xyzMax.y)
					xyzMax.y = pt.y + rA;
				if (pt.z - rA < xyzMin.z)
					xyzMin.z = pt.z - rA;
				if (pt.z + rA > xyzMax.z)
					xyzMax.z = pt.z + rA;
				mep_atoms[iAtom++] = atom;
			}
			else {
				++iAtom;
			}
		}
		if (!Float.isNaN(scale)) {
			Vector3f v = new Vector3f(xyzMax);
			v.sub(xyzMin);
			v.scale(0.5f);
			xyzMin.add(v);
			v.scale(scale);
			xyzMax.set(xyzMin);
			xyzMax.add(v);
			xyzMin.sub(v);
		}

		Logger.info("MEP range bohr " + xyzMin + " to " + xyzMax);
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("MEP range bohr " + xyzMin + " to " + xyzMax + "\n");

		int maxGrid = mep_gridMax;

		setVoxelRange(0, xyzMin.x, xyzMax.x, mep_ptsPerAngstrom, maxGrid);
		setVoxelRange(1, xyzMin.y, xyzMax.y, mep_ptsPerAngstrom, maxGrid);
		setVoxelRange(2, xyzMin.z, xyzMax.z, mep_ptsPerAngstrom, maxGrid);
		jvxlFileHeader.append(jvxlGetVolumeHeader(iAtom));
		Point3f pt = new Point3f();
		for (int i = 0; i < nAtoms; i++) {
			Atom atom = atoms[i];
			if (atom.modelIndex != modelIndex)
				continue;
			pt.set(atom);
			pt.scale(1 / ANGSTROMS_PER_BOHR);
			jvxlFileHeader.append(atom.getAtomicAndIsotopeNumber() + " " + atom.getAtomicAndIsotopeNumber() + ".0 "
					+ pt.x + " " + pt.y + " " + pt.z + "\n");
		}
		atomCount = -Integer.MAX_VALUE;
		negativeAtomCount = false;
		precalculateVoxelData = true;
	}

	void generateMepCube() {
		float[] origin = { volumetricOrigin.x, volumetricOrigin.y, volumetricOrigin.z };
		MepCalculation m = new MepCalculation(mep_atoms, mepCharges);
		m.createMepCube(voxelData, voxelCounts, origin, volumetricVectorLengths);
	}

	// /// solvent-accessible, solvent-excluded surface //////

	float solvent_ptsPerAngstrom = 4f;
	int solvent_gridMax = 60;
	int solvent_modelIndex;
	float[] solvent_atomRadius;
	Point3f[] solvent_ptAtom;
	int solvent_nAtoms;
	int solvent_firstNearbyAtom;
	boolean solvent_quickPlane;
	BitSet atomSet = new BitSet();
	BitSet bsSolventSelected;
	Voxel solvent_voxel = new Voxel();

	void setupSolvent() {
		/*
		 * The surface fragment idea:
		 * 
		 * isosurface solvent|sasurface both work on the SELECTED atoms, thus allowing for a subset of the molecule to
		 * be involved. But in that case we don't want to be creating a surface that goes right through another atom.
		 * Rather, what we want (probably) is just the portion of the OVERALL surface that involves these atoms.
		 * 
		 * The addition of Mesh.voxelValue[] means that we can specify any voxel we want to NOT be excluded (NaN). Here
		 * we first exclude any voxel that would have been INSIDE a nearby atom. This will take care of any portion of
		 * the vanderwaals surface that would be there. Then we exclude any special-case voxel that is between two
		 * nearby atoms.
		 * 
		 * Bob Hanson 13 Jul 2006
		 * 
		 */
		bsSolventSelected = new BitSet();
		if (thePlane != null)
			setPlaneParameters(thePlane);
		solvent_quickPlane = true;// viewer.getTestFlag1();
		Atom[] atoms = frame.atoms;
		Point3f xyzMin = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Point3f xyzMax = new Point3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		solvent_modelIndex = -1;
		int iAtom = 0;
		int nAtoms = viewer.getAtomCount();
		int nSelected = 0;
		iUseBitSets = true;
		if (bsIgnore == null) {
			bsIgnore = new BitSet();
		}
		for (int i = 0; i < nAtoms; i++) {
			if (bsSelected.get(i) && (!bsIgnore.get(i))) {
				if (solvent_quickPlane && thePlane != null
						&& Math.abs(distancePointToPlane(atoms[i], thePlane)) > 2 * solventWorkingRadius(atoms[i])) {
					continue;
				}
				bsSolventSelected.set(i);
				nSelected++;
			}
		}
		atomSet = new BitSet();
		int firstSet = -1;
		int lastSet = 0;
		for (int i = 0; i < nAtoms; i++)
			if (bsSolventSelected.get(i)) {
				if (solvent_modelIndex < 0)
					solvent_modelIndex = atoms[i].modelIndex;
				if (solvent_modelIndex != atoms[i].modelIndex) {
					bsIgnore.set(i);
					continue;
				}
				++iAtom;
				atomSet.set(i);
				if (firstSet == -1)
					firstSet = i;
				lastSet = i;
			}
		int nH = 0;
		int[] atomNo = null;
		if (iAtom > 0) {
			Point3f[] hAtoms = null;
			if (addHydrogens) {
				hAtoms = viewer.getAdditionalHydrogens(atomSet);
				nH = hAtoms.length;
			}
			solvent_atomRadius = new float[iAtom + nH];
			solvent_ptAtom = new Point3f[iAtom + nH];
			atomNo = new int[iAtom + nH];

			float r = solventWorkingRadius(null);
			for (int i = 0; i < nH; i++) {
				atomNo[i] = 1;
				solvent_atomRadius[i] = r;
				solvent_ptAtom[i] = hAtoms != null ? hAtoms[i] : null;
			}
			iAtom = nH;
			for (int i = firstSet; i <= lastSet; i++) {
				if (!atomSet.get(i))
					continue;
				atomNo[iAtom] = atoms[i].getElementNumber();
				solvent_ptAtom[iAtom] = atoms[i];
				solvent_atomRadius[iAtom++] = solventWorkingRadius(atoms[i]);
			}
		}
		solvent_nAtoms = solvent_firstNearbyAtom = iAtom;
		Logger.info(iAtom + " atoms will be used in the solvent-accessible surface calculation");

		for (int i = 0; i < solvent_nAtoms; i++) {
			Point3f pt = solvent_ptAtom[i];
			float rA = solvent_atomRadius[i];
			if (pt.x - rA < xyzMin.x)
				xyzMin.x = pt.x - rA;
			if (pt.x + rA > xyzMax.x)
				xyzMax.x = pt.x + rA;
			if (pt.y - rA < xyzMin.y)
				xyzMin.y = pt.y - rA;
			if (pt.y + rA > xyzMax.y)
				xyzMax.y = pt.y + rA;
			if (pt.z - rA < xyzMin.z)
				xyzMin.z = pt.z - rA;
			if (pt.z + rA > xyzMax.z)
				xyzMax.z = pt.z + rA;
		}
		Logger.info("surface range " + xyzMin + " to " + xyzMax);
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("solvent-" + (dataType == SURFACE_SASURFACE ? "accesible" : "excluded")
				+ " surface\nrange " + xyzMin + " to " + xyzMax + "\n");

		// fragment idea

		Point3f pt = new Point3f();

		BitSet bsNearby = new BitSet();
		int nNearby = 0;
		firstSet = -1;
		lastSet = 0;
		for (int i = 0; i < nAtoms; i++) {
			if (atomSet.get(i) || bsIgnore.get(i))
				continue;
			float rA = solventWorkingRadius(atoms[i]);
			if (solvent_quickPlane && thePlane != null && Math.abs(distancePointToPlane(atoms[i], thePlane)) > 2 * rA)
				continue;
			pt = atoms[i];
			if (pt.x + rA > xyzMin.x && pt.x - rA < xyzMax.x && pt.y + rA > xyzMin.y && pt.y - rA < xyzMax.y
					&& pt.z + rA > xyzMin.z && pt.z - rA < xyzMax.z) {
				if (firstSet == -1)
					firstSet = i;
				lastSet = i;
				bsNearby.set(i);
				nNearby++;
			}
		}

		if (nNearby != 0) {
			solvent_nAtoms += nNearby;
			solvent_atomRadius = ArrayUtil.setLength(solvent_atomRadius, solvent_nAtoms);
			solvent_ptAtom = (Point3f[]) ArrayUtil.setLength(solvent_ptAtom, solvent_nAtoms);

			iAtom = solvent_firstNearbyAtom;
			for (int i = firstSet; i <= lastSet; i++) {
				if (!bsNearby.get(i))
					continue;
				solvent_ptAtom[iAtom] = atoms[i];
				solvent_atomRadius[iAtom++] = solventWorkingRadius(atoms[i]);
			}
		}

		int maxGrid;
		maxGrid = solvent_gridMax;
		setVoxelRange(0, xyzMin.x, xyzMax.x, solvent_ptsPerAngstrom, maxGrid);
		setVoxelRange(1, xyzMin.y, xyzMax.y, solvent_ptsPerAngstrom, maxGrid);
		setVoxelRange(2, xyzMin.z, xyzMax.z, solvent_ptsPerAngstrom, maxGrid);
		precalculateVoxelData = newSolventMethod;
		int nAtomsWritten = Math.min(solvent_firstNearbyAtom, 100);
		jvxlFileHeader.append(jvxlGetVolumeHeader(nAtomsWritten));

		pt = new Point3f();
		for (int i = 0; i < nAtomsWritten; i++) {
			pt.set(solvent_ptAtom[i]);
			pt.scale(1 / ANGSTROMS_PER_BOHR);
			if (atomNo != null)
				jvxlFileHeader.append(atomNo[i] + " " + atomNo[i] + ".0 " + pt.x + " " + pt.y + " " + pt.z + "\n");
		}
		atomCount = -Integer.MAX_VALUE;
		negativeAtomCount = false;
	}

	float solventWorkingRadius(Atom atom) {
		float r = (solventAtomRadiusAbsolute > 0 ? solventAtomRadiusAbsolute
				: atom == null ? JmolConstants.vanderwaalsMars[1] / 1000f : useIonic ? atom.getBondingRadiusFloat()
						: atom.getVanderwaalsRadiusFloat());
		r *= solventAtomRadiusFactor;
		r += solventExtendedAtomRadius + solventAtomRadiusOffset;
		if (r < 0.1)
			r = 0.1f;
		return r;
	}

	void generateSolventCube() {
		long time = System.currentTimeMillis();
		float rA, rB;
		Point3f ptA;
		Point3f ptY0 = new Point3f(), ptZ0 = new Point3f();
		Point3i pt0 = new Point3i(), pt1 = new Point3i();
		float maxValue = (dataType == SURFACE_NOMAP ? Float.MAX_VALUE : Float.MAX_VALUE);
		for (int x = 0; x < nPointsX; ++x)
			for (int y = 0; y < nPointsY; ++y)
				for (int z = 0; z < nPointsZ; ++z)
					voxelData[x][y][z] = maxValue;
		if (dataType == SURFACE_NOMAP)
			return;
		float maxRadius = 0;
		for (int iAtom = 0; iAtom < solvent_nAtoms; iAtom++) {
			ptA = solvent_ptAtom[iAtom];
			rA = solvent_atomRadius[iAtom];
			if (rA > maxRadius)
				maxRadius = rA;
			boolean isNearby = (iAtom >= solvent_firstNearbyAtom);
			setGridLimitsForAtom(ptA, rA, pt0, pt1);
			voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
			for (int i = pt0.x; i < pt1.x; i++) {
				ptY0.set(ptXyzTemp);
				for (int j = pt0.y; j < pt1.y; j++) {
					ptZ0.set(ptXyzTemp);
					for (int k = pt0.z; k < pt1.z; k++) {
						float v = ptXyzTemp.distance(ptA) - rA;
						if (v < voxelData[i][j][k])
							voxelData[i][j][k] = (isNearby ? Float.NaN : v);
						ptXyzTemp.add(volumetricVectors[2]);
					}
					ptXyzTemp.set(ptZ0);
					ptXyzTemp.add(volumetricVectors[1]);
				}
				ptXyzTemp.set(ptY0);
				ptXyzTemp.add(volumetricVectors[0]);
			}
		}
		if ((dataType == SURFACE_SOLVENT || dataType == SURFACE_MOLECULAR) && solventRadius > 0) {
			Point3i ptA0 = new Point3i();
			Point3i ptB0 = new Point3i();
			Point3i ptA1 = new Point3i();
			Point3i ptB1 = new Point3i();
			for (int iAtom = 0; iAtom < solvent_firstNearbyAtom - 1; iAtom++)
				if (solvent_ptAtom[iAtom] instanceof Atom) {
					ptA = solvent_ptAtom[iAtom];
					rA = solvent_atomRadius[iAtom] + solventRadius;
					setGridLimitsForAtom(ptA, rA - solventRadius, ptA0, ptA1);
					AtomIterator iter = frame.getWithinModelIterator((Atom) ptA, rA + solventRadius + maxRadius);
					while (iter.hasNext()) {
						Atom ptB = iter.next();
						if (ptB.atomIndex <= ((Atom) ptA).atomIndex)
							continue;
						// selected
						// only consider selected neighbors
						if (!bsSolventSelected.get(ptB.atomIndex))
							continue;
						rB = solventWorkingRadius(ptB) + solventRadius;
						if (solvent_quickPlane && thePlane != null
								&& Math.abs(distancePointToPlane(ptB, thePlane)) > 2 * rB)
							continue;

						float dAB = ptA.distance(ptB);
						if (dAB >= rA + rB)
							continue;
						// defining pt0 and pt1 very crudely -- this could be refined
						setGridLimitsForAtom(ptB, rB - solventRadius, ptB0, ptB1);
						pt0.x = Math.min(ptA0.x, ptB0.x);
						pt0.y = Math.min(ptA0.y, ptB0.y);
						pt0.z = Math.min(ptA0.z, ptB0.z);
						pt1.x = Math.max(ptA1.x, ptB1.x);
						pt1.y = Math.max(ptA1.y, ptB1.y);
						pt1.z = Math.max(ptA1.z, ptB1.z);
						voxelPtToXYZ(pt0.x, pt0.y, pt0.z, ptXyzTemp);
						for (int i = pt0.x; i < pt1.x; i++) {
							ptY0.set(ptXyzTemp);
							for (int j = pt0.y; j < pt1.y; j++) {
								ptZ0.set(ptXyzTemp);
								for (int k = pt0.z; k < pt1.z; k++) {
									float dVS = checkSpecialVoxel(ptA, rA, ptB, rB, dAB, ptXyzTemp);
									if (!Float.isNaN(dVS)) {
										float v = solventRadius - dVS;
										if (v < voxelData[i][j][k])
											voxelData[i][j][k] = v;
									}
									ptXyzTemp.add(volumetricVectors[2]);
								}
								ptXyzTemp.set(ptZ0);
								ptXyzTemp.add(volumetricVectors[1]);
							}
							ptXyzTemp.set(ptY0);
							ptXyzTemp.add(volumetricVectors[0]);
						}
					}
				}
		}
		if (thePlane != null) {
			maxValue = 0.001f;
			for (int x = 0; x < nPointsX; ++x)
				for (int y = 0; y < nPointsY; ++y)
					for (int z = 0; z < nPointsZ; ++z)
						if (voxelData[x][y][z] < maxValue) {
							// Float.NaN will also match ">=" this way
						}
						else {
							voxelData[x][y][z] = maxValue;
						}
		}
		Logger.debug("solvent surface time:" + (System.currentTimeMillis() - time));
	}

	void setGridLimitsForAtom(Point3f ptA, float rA, Point3i pt0, Point3i pt1) {
		xyzToVoxelPt(ptA.x - rA, ptA.y - rA, ptA.z - rA, pt0);
		pt0.x -= 1;
		pt0.y -= 1;
		pt0.z -= 1;
		if (pt0.x < 0)
			pt0.x = 0;
		if (pt0.y < 0)
			pt0.y = 0;
		if (pt0.z < 0)
			pt0.z = 0;
		xyzToVoxelPt(ptA.x + rA, ptA.y + rA, ptA.z + rA, pt1);
		pt1.x += 2;
		pt1.y += 2;
		pt1.z += 2;
		if (pt1.x >= nPointsX)
			pt1.x = nPointsX;
		if (pt1.y >= nPointsY)
			pt1.y = nPointsY;
		if (pt1.z >= nPointsZ)
			pt1.z = nPointsZ;
	}

	float getSolventValue(int x, int y, int z) {

		// old method -- not used

		solvent_voxel.setValue(x, y, z, Float.MAX_VALUE);
		float rA, rB;
		Point3f ptA, ptB;
		for (int i = 0; i < solvent_nAtoms && solvent_voxel.value >= -0.5; i++) {
			ptA = solvent_ptAtom[i];
			rA = solvent_atomRadius[i];
			float v = solvent_voxel.ptXyz.distance(ptA) - rA;
			if (v < solvent_voxel.value)
				solvent_voxel.setValue(i >= solvent_firstNearbyAtom ? Float.NaN : v);
		}
		if (solventRadius == 0)
			return solvent_voxel.value;
		Point3f ptV = solvent_voxel.ptXyz;
		for (int i = 0; i < solvent_nAtoms - 1 && solvent_voxel.value >= -0.5; i++) {
			ptA = solvent_ptAtom[i];
			rA = solvent_atomRadius[i] + solventRadius;
			for (int j = i + 1; j < solvent_nAtoms && solvent_voxel.value >= -0.5; j++) {
				if (i >= solvent_firstNearbyAtom && j >= solvent_firstNearbyAtom)
					continue;
				ptB = solvent_ptAtom[j];
				rB = solvent_atomRadius[j] + solventRadius;
				float dAB = ptA.distance(ptB);
				if (dAB >= rA + rB)
					continue;
				float dVS = checkSpecialVoxel(ptA, rA, ptB, rB, dAB, ptV);
				if (!Float.isNaN(dVS))
					solvent_voxel.setValue(solventRadius - dVS);
			}
		}
		return solvent_voxel.value;
	}

	final Point3f ptS = new Point3f();

	float checkSpecialVoxel(Point3f ptA, float rAS, Point3f ptB, float rBS, float dAB, Point3f ptV) {
		/*
		 * Checking here for voxels that are in the situation:
		 * 
		 * A------)-- V ---((--))-- S --(------B |----d--------| or
		 * 
		 * B------)-- V ---((--))-- S --(------A |----d--------|
		 * 
		 * A and B are the two atom centers; V is the voxel; S is a hypothetical PROJECTED solvent center based on the
		 * position of V in relation to first A, then B; ( and ) are atom radii and (( )) are the overlapping
		 * atom+solvent radii.
		 * 
		 * That is, where the projected solvent location for one voxel is within the solvent radius sphere of another,
		 * this voxel should be checked in relation to solvent distance, not atom distance.
		 * 
		 * 
		 * S ++ / \ ++ ++ / | \ ++ + V + x want V such that angle ASV < angle ASB / ****** \ A --+--+----B b
		 * 
		 * A, B are atoms; S is solvent center; V is voxel point objective is to calculate dSV. ++ Here represents the
		 * van der Waals radius for each atom. ***** is the key "trough" location.
		 * 
		 * Getting dVS:
		 * 
		 * Known: rAB, rAS, rBS, giving angle BAS (theta) Known: rAB, rAV, rBV, giving angle VAB (alpha) Determined:
		 * angle VAS (theta - alpha), and from that, dSV, using the cosine law:
		 * 
		 * a^2 + b^2 - 2ab Cos(theta) = c^2.
		 * 
		 * The trough issue:
		 * 
		 * Since the voxel might be at point x (above), outside the triangle, we have to test for that. What we will be
		 * looking for in the "trough" will be that angle ASV < angle ASB that is, cosASB < cosASV
		 * 
		 * If we find the voxel in the "trough", then we set its value to (solvent radius - dVS).
		 * 
		 */
		float dAV = ptA.distance(ptV);
		float dBV = ptB.distance(ptV);
		float dVS = Float.NaN;
		float f = rAS / dAV;
		if (f > 1) {
			ptS.set(ptA.x + (ptV.x - ptA.x) * f, ptA.y + (ptV.y - ptA.y) * f, ptA.z + (ptV.z - ptA.z) * f);
			if (ptB.distance(ptS) < rBS) {
				dVS = solventDistance(ptV, ptA, ptB, rAS, rBS, dAB, dAV, dBV);
				if (!voxelIsInTrough(dVS, rAS * rAS, rBS, dAB, dAV, dBV))
					return Float.NaN;
			}
			return dVS;
		}
		f = rBS / dBV;
		if (f <= 1)
			return dVS;
		ptS.set(ptB.x + (ptV.x - ptB.x) * f, ptB.y + (ptV.y - ptB.y) * f, ptB.z + (ptV.z - ptB.z) * f);
		if (ptA.distance(ptS) < rAS) {
			dVS = solventDistance(ptV, ptB, ptA, rBS, rAS, dAB, dBV, dAV);
			if (!voxelIsInTrough(dVS, rAS * rAS, rBS, dAB, dAV, dBV))
				return Float.NaN;
		}
		return dVS;
	}

	boolean voxelIsInTrough(float dVS, float rAS2, float rBS, float dAB, float dAV, float dBV) {
		// only calculate what we need -- a factor proportional to cos
		float cosASBf = (rAS2 + rBS * rBS - dAB * dAB) / rBS; // /2 /rAS);
		float cosASVf = (rAS2 + dVS * dVS - dAV * dAV) / dVS; // /2 /rAS);
		return (cosASBf < cosASVf);
	}

	float solventDistance(Point3f ptV, Point3f ptA, Point3f ptB, float rAS, float rBS, float dAB, float dAV, float dBV) {
		double angleVAB = Math.acos((dAV * dAV + dAB * dAB - dBV * dBV) / (2 * dAV * dAB));
		double angleBAS = Math.acos((dAB * dAB + rAS * rAS - rBS * rBS) / (2 * dAB * rAS));
		float dVS = (float) Math.sqrt(rAS * rAS + dAV * dAV - 2 * rAS * dAV * Math.cos(angleBAS - angleVAB));
		return dVS;
	}

	// ////// file-based data already in Hashtable /////////

	float[][][] tempVoxelData;

	void setupSurfaceInfo() {
		volumetricOrigin.set((Point3f) surfaceInfo.get("volumetricOrigin"));
		Vector3f[] v = (Vector3f[]) surfaceInfo.get("volumetricVectors");
		for (int i = 0; i < 3; i++) {
			volumetricVectors[i].set(v[i]);
			volumetricVectorLengths[i] = volumetricVectors[i].length();
			unitVolumetricVectors[i].normalize(volumetricVectors[i]);
		}
		int[] counts = (int[]) surfaceInfo.get("voxelCounts");
		for (int i = 0; i < 3; i++)
			voxelCounts[i] = counts[i];
		tempVoxelData = voxelData = (float[][][]) surfaceInfo.get("voxelData");
		precalculateVoxelData = true;
	}

	// ////// user function //////////

	String functionName;

	void setupFunctionXY() {
		jvxlFileHeader = new StringBuffer();
		jvxlFileHeader.append("functionXY\n" + functionXYinfo + "\n");
		functionName = (String) functionXYinfo.get(0);
		volumetricOrigin.set((Point3f) functionXYinfo.get(1));
		if (!isAngstroms)
			volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
		for (int i = 0; i < 3; i++) {
			Point4f info = (Point4f) functionXYinfo.get(i + 2);
			voxelCounts[i] = (int) info.x;
			volumetricVectors[i].set(info.y, info.z, info.w);
			if (!isAngstroms)
				volumetricVectors[i].scale(ANGSTROMS_PER_BOHR);
			volumetricVectorLengths[i] = volumetricVectors[i].length();
			unitVolumetricVectors[i].normalize(volumetricVectors[i]);
		}
		jvxlFileHeader.append(jvxlGetVolumeHeader(2));
		atomCount = 0;
		negativeAtomCount = false;
	}

	float getFunctionValue(int x, int y) {
		return viewer.functionXY(functionName, x, y);
	}

	// // LCAO Cartoons ////

	int nLCAO = 0;

	void drawLcaoCartoon(String lcaoCartoon, Vector3f z, Vector3f x) {
		Vector3f y = new Vector3f();
		boolean isReverse = (lcaoCartoon.length() > 0 && lcaoCartoon.charAt(0) == '-');
		if (isReverse)
			lcaoCartoon = lcaoCartoon.substring(1);
		colorPos = colorPosLCAO;
		colorNeg = colorNegLCAO;
		int sense = (isReverse ? -1 : 1);
		y.cross(z, x);
		String id = (currentMesh == null ? "lcao" + (++nLCAO) + "_" + lcaoCartoon : currentMesh.thisID);
		if (currentMesh == null)
			allocMesh(id);
		defaultColix = Graphics3D.getColix(colorPos);

		if (lcaoCartoon.equals("px")) {
			currentMesh.thisID += "a";
			createLcaoLobe(x, sense);
			setProperty("thisID", id + "b", null);
			createLcaoLobe(x, -sense);
			currentMesh.colix = Graphics3D.getColix(colorNeg);
			return;
		}
		if (lcaoCartoon.equals("py")) {
			currentMesh.thisID += "a";
			createLcaoLobe(y, sense);
			setProperty("thisID", id + "b", null);
			createLcaoLobe(y, -sense);
			currentMesh.colix = Graphics3D.getColix(colorNeg);
			return;
		}
		if (lcaoCartoon.equals("pz")) {
			currentMesh.thisID += "a";
			createLcaoLobe(z, sense);
			setProperty("thisID", id + "b", null);
			createLcaoLobe(z, -sense);
			currentMesh.colix = Graphics3D.getColix(colorNeg);
			return;
		}
		if (lcaoCartoon.equals("pxa")) {
			createLcaoLobe(x, sense);
			return;
		}
		if (lcaoCartoon.equals("pxb")) {
			createLcaoLobe(x, -sense);
			return;
		}
		if (lcaoCartoon.equals("pya")) {
			createLcaoLobe(y, sense);
			return;
		}
		if (lcaoCartoon.equals("pyb")) {
			createLcaoLobe(y, -sense);
			return;
		}
		if (lcaoCartoon.equals("pza")) {
			createLcaoLobe(z, sense);
			return;
		}
		if (lcaoCartoon.equals("pzb")) {
			createLcaoLobe(z, -sense);
			return;
		}
		if (lcaoCartoon.indexOf("sp") == 0 || lcaoCartoon.indexOf("lp") == 0) {
			createLcaoLobe(z, sense);
			return;
		}

		// assume s
		createLcaoLobe(null, 1);
		return;
	}

	Point4f lcaoDir = new Point4f();

	void createLcaoLobe(Vector3f lobeAxis, float factor) {
		initState();
		Logger.debug("creating isosurface " + currentMesh.thisID);
		if (lobeAxis == null) {
			setProperty("sphere", new Float(factor / 2f), null);
			return;
		}
		lcaoDir.x = lobeAxis.x * factor;
		lcaoDir.y = lobeAxis.y * factor;
		lcaoDir.z = lobeAxis.z * factor;
		lcaoDir.w = 0.7f;
		setProperty("lobe", lcaoDir, null);
	}

	void setModelIndex() {
		setModelIndex(atomIndex);
		currentMesh.ptCenter.set(center);
		currentMesh.title = title;
		currentMesh.jvxlDefinitionLine = jvxlGetDefinitionLine(currentMesh);
		if (script != null)
			currentMesh.scriptCommand = fixScript();
	}

	@SuppressWarnings("unchecked")
	Vector getShapeDetail() {
		Vector V = new Vector();
		for (int i = 0; i < meshCount; i++) {
			Hashtable info = new Hashtable();
			Mesh mesh = meshes[i];
			if (mesh == null)
				continue;
			info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
			info.put("vertexCount", new Integer(mesh.vertexCount));
			if (mesh.ptCenter.x != Float.MAX_VALUE)
				info.put("center", mesh.ptCenter);
			if (mesh.jvxlDefinitionLine != null)
				info.put("jvxlDefinitionLine", mesh.jvxlDefinitionLine);
			info.put("modelIndex", new Integer(mesh.modelIndex));
			if (mesh.title != null)
				info.put("title", mesh.title);
			V.add(info);
		}
		return V;
	}

	BitSet[] surfaceSet;
	int nSets = 0;
	boolean setsSuccessful;

	BitSet[] getSurfaceSet(int level) {
		if (currentMesh == null)
			return null;
		if (level == 0) {
			surfaceSet = new BitSet[100];
			nSets = 0;
		}
		setsSuccessful = true;
		for (int i = 0; i < currentMesh.polygonCount; i++) {
			int[] p = currentMesh.polygonIndexes[i];
			int pt0 = findSet(p[0]);
			int pt1 = findSet(p[1]);
			int pt2 = findSet(p[2]);
			if (pt0 < 0 && pt1 < 0 && pt2 < 0) {
				createSet(p[0], p[1], p[2]);
				continue;
			}
			if (pt0 == pt1 && pt2 == pt2)
				continue;
			if (pt0 >= 0) {
				surfaceSet[pt0].set(p[1]);
				surfaceSet[pt0].set(p[2]);
				if (pt1 >= 0 && pt1 != pt0)
					mergeSets(pt0, pt1);
				if (pt2 >= 0 && pt2 != pt0 && pt2 != pt1)
					mergeSets(pt0, pt2);
				continue;
			}
			if (pt1 >= 0) {
				surfaceSet[pt1].set(p[0]);
				surfaceSet[pt1].set(p[2]);
				if (pt2 >= 0 && pt2 != pt1)
					mergeSets(pt1, pt2);
				continue;
			}
			surfaceSet[pt2].set(p[0]);
			surfaceSet[pt2].set(p[1]);
		}
		int n = 0;
		for (int i = 0; i < nSets; i++)
			if (surfaceSet[i] != null)
				n++;
		BitSet[] temp = new BitSet[n];
		n = 0;
		for (int i = 0; i < nSets; i++)
			if (surfaceSet[i] != null)
				temp[n++] = surfaceSet[i];
		nSets = n;
		surfaceSet = temp;
		if (!setsSuccessful && level < 2)
			getSurfaceSet(++level);
		return surfaceSet;
	}

	int findSet(int vertex) {
		for (int i = 0; i < nSets; i++)
			if (surfaceSet[i] != null && surfaceSet[i].get(vertex))
				return i;
		return -1;
	}

	void createSet(int v1, int v2, int v3) {
		int i;
		for (i = 0; i < nSets; i++)
			if (surfaceSet[i] == null)
				break;
		if (i >= 100) {
			setsSuccessful = false;
			return;
		}
		if (i == nSets)
			nSets = i + 1;
		surfaceSet[i] = new BitSet();
		surfaceSet[i].set(v1);
		surfaceSet[i].set(v2);
		surfaceSet[i].set(v3);
	}

	void mergeSets(int a, int b) {
		surfaceSet[a].or(surfaceSet[b]);
		surfaceSet[b] = null;
	}
}
