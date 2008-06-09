/* $RCSfile: StateManager.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
 * $Revision: 1.2 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.myjmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Matrix3f;

import java.util.Hashtable;
import java.util.BitSet;
import java.util.Enumeration;
import java.text.DecimalFormat;

import org.myjmol.g3d.Graphics3D;
import org.myjmol.util.CommandHistory;

class StateManager {
	Viewer viewer;
	Hashtable saved = new Hashtable();
	String lastOrientation = "";
	String lastConnections = "";
	String lastSelected = "";
	String lastState = "";

	StateManager(Viewer viewer) {
		this.viewer = viewer;
	}

	GlobalSettings getGlobalSettings() {
		return new GlobalSettings();
	}

	void clear(GlobalSettings global) {
		global.clear();
		// other state clearing? -- place here
	}

	void setCrystallographicDefaults() {
		// axes on and mode unitCell; unitCell on; perspective depth off;
		viewer.setShapeSize(JmolConstants.SHAPE_AXES, 200);
		viewer.setShapeSize(JmolConstants.SHAPE_UCCAGE, -1);
		viewer.setAxesModeUnitCell(true);
		viewer.setBooleanProperty("perspectiveDepth", false);
	}

	void setCommonDefaults() {
		viewer.setBooleanProperty("perspectiveDepth", true);
		viewer.setIntProperty("percentVdwAtom", JmolConstants.DEFAULT_PERCENT_VDW_ATOM);
		viewer.setFloatProperty("bondTolerance", JmolConstants.DEFAULT_BOND_TOLERANCE);
		viewer.setFloatProperty("minBondDistance", JmolConstants.DEFAULT_MIN_BOND_DISTANCE);
		viewer.setIntProperty("bondRadiusMilliAngstroms", JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS);
	}

	void setJmolDefaults() {
		setCommonDefaults();
		viewer.setStringProperty("defaultColorScheme", "Jmol");
		viewer.setBooleanProperty("axesOrientationRasmol", false);
		viewer.setBooleanProperty("zeroBasedXyzRasmol", false);
	}

	void setRasMolDefaults() {
		setCommonDefaults();
		viewer.setStringProperty("defaultColorScheme", "RasMol");
		viewer.setBooleanProperty("axesOrientationRasmol", true);
		viewer.setBooleanProperty("zeroBasedXyzRasmol", true);
		viewer.setIntProperty("percentVdwAtom", 0);
		viewer.setIntProperty("bondRadiusMilliAngstroms", 1);
	}

	private DecimalFormat[] formatters;

	private static String[] formattingStrings = { "0", "0.0", "0.00", "0.000", "0.0000", "0.00000", "0.000000",
			"0.0000000", "0.00000000", "0.000000000" };

	String formatDecimal(float value, int decimalDigits) {
		if (decimalDigits < 0)
			return "" + value;
		if (formatters == null)
			formatters = new DecimalFormat[formattingStrings.length];
		if (decimalDigits >= formattingStrings.length)
			decimalDigits = formattingStrings.length - 1;
		DecimalFormat formatter = formatters[decimalDigits];
		if (formatter == null)
			formatter = formatters[decimalDigits] = new DecimalFormat(formattingStrings[decimalDigits]);
		return formatter.format(value);
	}

	String getStandardLabelFormat() {
		// from the RasMol 2.6b2 manual: RasMol uses the label
		// "%n%r:%c.%a" if the molecule contains more than one chain:
		// "%e%i" if the molecule has only a single residue (a small molecule) and
		// "%n%r.%a" otherwise.
		String strLabel;
		int modelCount = viewer.getModelCount();
		if (viewer.getChainCount() > modelCount)
			strLabel = "[%n]%r:%c.%a";
		else if (viewer.getGroupCount() <= modelCount)
			strLabel = "%e%i";
		else strLabel = "[%n]%r.%a";
		if (viewer.getModelCount() > 1)
			strLabel += "/%M";
		return strLabel;
	}

	String listSavedStates() {
		String names = "";
		Enumeration e = saved.keys();
		while (e.hasMoreElements())
			names += "\n" + e.nextElement();
		return names;
	}

	@SuppressWarnings("unchecked")
	void saveSelection(String saveName, BitSet bsSelected) {
		saveName = lastSelected = "Selected_" + saveName;
		BitSet bs = (BitSet) bsSelected.clone();
		saved.put(saveName, bs);
	}

	boolean restoreSelection(String saveName) {
		String name = (saveName.length() > 0 ? "Selected_" + saveName : lastSelected);
		BitSet bsSelected = (BitSet) saved.get(name);
		if (bsSelected == null) {
			viewer.select(new BitSet(), false);
			return false;
		}
		viewer.select(bsSelected, false);
		return true;
	}

	@SuppressWarnings("unchecked")
	void saveState(String saveName) {
		saveName = lastState = "State_" + saveName;
		saved.put(saveName, viewer.getStateInfo());
	}

	String getSavedState(String saveName) {
		String name = (saveName.length() > 0 ? "State_" + saveName : lastState);
		String script = (String) saved.get(name);
		return (script == null ? "" : script);
	}

	boolean restoreState(String saveName) {
		// not used -- more efficient just to run the script
		String name = (saveName.length() > 0 ? "State_" + saveName : lastState);
		String script = (String) saved.get(name);
		if (script == null)
			return false;
		viewer.script(script + CommandHistory.NOHISTORYATALL_FLAG);
		return true;
	}

	@SuppressWarnings("unchecked")
	void saveOrientation(String saveName) {
		Orientation o = new Orientation();
		o.saveName = lastOrientation = "Orientation_" + saveName;
		saved.put(o.saveName, o);
	}

	boolean restoreOrientation(String saveName, float timeSeconds) {
		String name = (saveName.length() > 0 ? "Orientation_" + saveName : lastOrientation);
		Orientation o = (Orientation) saved.get(name);
		if (o == null)
			return false;
		o.restore(timeSeconds);
		// Logger.info(listSavedStates());
		return true;
	}

	class Orientation {

		String saveName;

		Matrix3f rotationMatrix = new Matrix3f();
		float xTrans, yTrans;
		float zoom, rotationRadius;
		Point3f center = new Point3f();
		boolean windowCenteredFlag;

		Orientation() {
			viewer.getRotation(rotationMatrix);
			xTrans = viewer.getTranslationXPercent();
			yTrans = viewer.getTranslationYPercent();
			zoom = viewer.getZoomPercentFloat();
			center.set(viewer.getRotationCenter());
			windowCenteredFlag = viewer.isWindowCentered();
			rotationRadius = viewer.getRotationRadius();
		}

		void restore(float timeSeconds) {
			viewer.setBooleanProperty("windowCentered", windowCenteredFlag);
			viewer.moveTo(timeSeconds, rotationMatrix, center, zoom, xTrans, yTrans, rotationRadius);
		}
	}

	@SuppressWarnings("unchecked")
	void saveBonds(String saveName) {
		Connections b = new Connections();
		b.saveName = lastConnections = "Bonds_" + saveName;
		saved.put(b.saveName, b);
	}

	boolean restoreBonds(String saveName) {
		String name = (saveName.length() > 0 ? "Bonds_" + saveName : lastConnections);
		Connections c = (Connections) saved.get(name);
		if (c == null)
			return false;
		c.restore();
		// Logger.info(listSavedStates());
		return true;
	}

	class Connections {

		String saveName;
		int bondCount;
		Connection[] connections;

		Connections() {
			Frame frame = viewer.getFrame();
			if (frame == null)
				return;
			bondCount = frame.bondCount;
			connections = new Connection[bondCount + 1];
			Bond[] bonds = frame.bonds;
			for (int i = bondCount; --i >= 0;) {
				Bond b = bonds[i];
				connections[i] = new Connection(b.atom1.atomIndex, b.atom2.atomIndex, b.mad, b.colix, b.order,
						b.shapeVisibilityFlags);
			}
		}

		class Connection {
			int atomIndex1;
			int atomIndex2;
			short mad;
			short colix;
			short order;
			int shapeVisibilityFlags;

			Connection(int atom1, int atom2, short mad, short colix, short order, int shapeVisibilityFlags) {
				atomIndex1 = atom1;
				atomIndex2 = atom2;
				this.mad = mad;
				this.colix = colix;
				this.order = order;
				this.shapeVisibilityFlags = shapeVisibilityFlags;
			}
		}

		void restore() {
			Frame frame = viewer.getFrame();
			if (frame == null)
				return;
			frame.deleteAllBonds();
			for (int i = bondCount; --i >= 0;) {
				Connection c = connections[i];
				if (c.atomIndex1 >= frame.atomCount || c.atomIndex2 >= frame.atomCount)
					continue;
				Bond b = frame.bondAtoms(frame.atoms[c.atomIndex1], frame.atoms[c.atomIndex2], c.order, c.mad);
				b.shapeVisibilityFlags = c.shapeVisibilityFlags;
			}
			viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "reportAll", null);
		}
	}

	class GlobalSettings {

		/*
		 * Mostly these are just saved and restored directly from Viewer. They are collected here for reference and to
		 * ensure that no methods are written that bypass viewer's get/set methods.
		 * 
		 * Because these are not Frame variables, they should persist past a new file loading. There is some question in
		 * my mind whether all should be in this category.
		 * 
		 */

		GlobalSettings() {
			//
		}

		// file loading

		char inlineNewlineChar = '|'; // pseudo static

		boolean zeroBasedXyzRasmol = false;
		boolean forceAutoBond = false;
		boolean autoBond = true;
		int percentVdwAtom = JmolConstants.DEFAULT_PERCENT_VDW_ATOM;
		short marBond = JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
		float bondTolerance = JmolConstants.DEFAULT_BOND_TOLERANCE;
		float minBondDistance = JmolConstants.DEFAULT_MIN_BOND_DISTANCE;
		String defaultLoadScript = "";
		String defaultDirectory = null;

		/**
		 * these settings are determined when the file is loaded and are kept even though they might later change. So we
		 * list them here and ALSO let them be defined in the settings. 10.9.98 missed this.
		 * 
		 * @return script command
		 */
		String getLoadState() {
			StringBuffer str = new StringBuffer();
			if (defaultDirectory != null)
				appendCmd(str, "set defaultDirectory " + escape(defaultDirectory));
			appendCmd(str, "set autoBond " + autoBond);
			appendCmd(str, "set forceAutoBond " + forceAutoBond);
			appendCmd(str, "set zeroBasedXyzRasmol " + zeroBasedXyzRasmol);
			appendCmd(str, "set percentVdwAtom " + percentVdwAtom);
			appendCmd(str, "set bondRadiusMilliAngstroms " + marBond);
			appendCmd(str, "set minBondDistance " + minBondDistance);
			appendCmd(str, "set bondTolerance " + bondTolerance);
			appendCmd(str, "set defaultLattice " + escape(ptDefaultLattice));
			if (defaultLoadScript.length() > 0)
				appendCmd(str, "set defaultLoadScript " + escape(defaultLoadScript));
			return str + "\n";
		}

		private final Point3f ptDefaultLattice = new Point3f();

		void setDefaultLattice(Point3f ptLattice) {
			ptDefaultLattice.set(ptLattice);
		}

		Point3f getDefaultLatticePoint() {
			return ptDefaultLattice;
		}

		int[] getDefaultLatticeArray() {
			int[] A = new int[4];
			A[1] = (int) ptDefaultLattice.x;
			A[2] = (int) ptDefaultLattice.y;
			A[3] = (int) ptDefaultLattice.z;
			return A;
		}

		void clear() {

			// OK, here is where we would put any
			// "global" settings that
			// need to be reset whenever a file is loaded

			clearVolatileProperties();
		}

		// centering and perspective

		boolean allowCameraMoveFlag = true;
		boolean adjustCameraFlag = true;

		// solvent

		boolean solventOn = false;
		float solventProbeRadius = 1.2f;

		// measurements

		boolean measureAllModels = false;
		boolean justifyMeasurements = false;
		String defaultDistanceLabel = "%v %u"; // also %_ and %a1 %a2 %m1 %m2, etc.
		String defaultAngleLabel = "%v %u";
		String defaultTorsionLabel = "%v %u";

		// rendering

		boolean enableFullSceneAntialiasing = false;
		boolean greyscaleRendering = false;
		boolean zoomLarge = true; // false would be like Chime
		boolean dotsSelectedOnlyFlag = false;
		boolean dotSurfaceFlag = true;
		boolean displayCellParameters = true;
		boolean showHiddenSelectionHalos = false;
		boolean showMeasurements = true;
		boolean frankOn = false;
		boolean centerPointer = true;

		// atoms and bonds

		boolean bondSelectionModeOr = false;
		boolean showMultipleBonds = true;
		boolean showHydrogens = true;
		boolean ssbondsBackbone = false;
		boolean hbondsBackbone = false;
		boolean hbondsSolid = false;

		byte modeMultipleBond = JmolConstants.MULTIBOND_NOTSMALL;
		int defaultVectorMad = 0;

		// secondary structure + Rasmol

		boolean rasmolHydrogenSetting = true;
		boolean rasmolHeteroSetting = true;
		boolean cartoonRocketFlag = false;
		boolean ribbonBorder = false;
		boolean chainCaseSensitive = false;
		boolean rangeSelected = false;

		boolean traceAlpha = true;
		boolean highResolutionFlag = false;
		int ribbonAspectRatio = 16;
		int hermiteLevel = 0;
		float sheetSmoothing = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints

		// misc

		boolean hideNameInPopup = false;
		boolean disablePopupMenu = false;
		float defaultVibrationScale = 1f;
		float defaultVibrationPeriod = 1f;
		float defaultVectorScale = 1f;

		// window

		int argbBackground = 0xFF000000;
		String stereoState = null;
		boolean navigationMode = false;

		String getWindowState() {
			StringBuffer str = new StringBuffer("# window state;\n# height " + viewer.getScreenHeight() + ";\n# width "
					+ viewer.getScreenWidth() + ";\n");
			appendCmd(str, "initialize");
			appendCmd(str, "set refreshing false");
			appendCmd(str, "background " + escapeColor(argbBackground));
			if (stereoState != null)
				appendCmd(str, "stereo " + stereoState);
			return str + "\n";
		}

		int axesMode = JmolConstants.AXES_MODE_BOUNDBOX;
		int pickingSpinRate = 10;

		String helpPath = null;
		String defaultHelpPath = JmolConstants.DEFAULT_HELP_PATH;
		String propertyStyleString = "";

		// testing

		boolean debugScript = false;
		boolean testFlag1 = false;
		boolean testFlag2 = false;
		boolean testFlag3 = false;
		boolean testFlag4 = false;

		// measurements

		// controlled access:
		private String measureDistanceUnits = "nanometers";

		boolean setMeasureDistanceUnits(String units) {
			if (units.equalsIgnoreCase("angstroms"))
				measureDistanceUnits = "angstroms";
			else if (units.equalsIgnoreCase("nanometers") || units.equalsIgnoreCase("nm"))
				measureDistanceUnits = "nanometers";
			else if (units.equalsIgnoreCase("picometers") || units.equalsIgnoreCase("pm"))
				measureDistanceUnits = "picometers";
			else return false;
			return true;
		}

		String getMeasureDistanceUnits() {
			return measureDistanceUnits;
		}

		Hashtable htParameterValues = new Hashtable();
		Hashtable htPropertyFlags = new Hashtable();

		final static String volatileProperties =
		// indicate all properties here in lower case
		// surrounded by ";" that should be reset upon file load
		// frame properties and such:
		";selectionhalos;";

		final static String unnecessaryProperties =
		// these are handled individually
		// NOT EXCLUDING the load state settings, because although we
		// handle these specially for the CURRENT FILE, their current
		// settings won't be reflected in the load state, which is determined
		// earlier, when the file loads.
		";refreshing;defaults;backgroundmodel;backgroundcolor;stereo;"
				+ ";debugscript;frank;showaxes;showunitcell;showboundbox;"
				+ ";slabEnabled;zoomEnabled;axeswindow;axesunitcell;axesmolecular;windowcentered;"
				+ ";vibrationscale;vibrationperiod;";

		void clearVolatileProperties() {
			Enumeration e;
			e = htPropertyFlags.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				if (volatileProperties.indexOf(";" + key + ";") >= 0 || key.charAt(0) == '@')
					htPropertyFlags.remove(key);
			}
			e = htParameterValues.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				if (volatileProperties.indexOf(";" + key + ";") >= 0 || key.charAt(0) == '@')
					htParameterValues.remove(key);
			}
		}

		@SuppressWarnings( { "unchecked", "unchecked" })
		void setPropertyFlag(String key, boolean value) {
			key = key.toLowerCase();
			htPropertyFlags.put(key, value ? Boolean.TRUE : Boolean.FALSE);
		}

		@SuppressWarnings("unchecked")
		void setParameterValue(String name, int value) {
			name = name.toLowerCase();
			htParameterValues.put(name, new Integer(value));
		}

		@SuppressWarnings("unchecked")
		void setParameterValue(String name, float value) {
			name = name.toLowerCase();
			htParameterValues.put(name, new Float(value));
		}

		@SuppressWarnings("unchecked")
		void setParameterValue(String name, String value) {
			name = name.toLowerCase();
			htParameterValues.put(name, value);
		}

		boolean doRegister(String name) {
			return (unnecessaryProperties.indexOf(";" + name + ";") < 0);
		}

		Object getParameter(String name) {
			return htParameterValues.get(name);
		}

		String getState() {
			StringBuffer commands = new StringBuffer("# settings;\n");
			appendCmd(commands, "set refreshing false");
			Enumeration e;
			// two rounds here because default settings
			// must be declared first
			String key;
			// defaults
			e = htParameterValues.keys();
			while (e.hasMoreElements()) {
				key = (String) e.nextElement();
				if (key.indexOf("default") >= 0 && key.charAt(0) != '@' && doRegister(key))
					appendCmd(commands, "set " + key + " " + htParameterValues.get(key));
			}
			// booleans
			e = htPropertyFlags.keys();
			while (e.hasMoreElements()) {
				key = (String) e.nextElement();
				if (doRegister(key))
					appendCmd(commands, "set " + key + " " + htPropertyFlags.get(key));
			}
			// nondefault, nonvariables
			// save as _xxxx if you don't want "set" to be there first
			e = htParameterValues.keys();
			while (e.hasMoreElements()) {
				key = (String) e.nextElement();
				if (key.indexOf("default") < 0 && key.charAt(0) != '@' && doRegister(key)) {
					Object value = htParameterValues.get(key);
					if (key.charAt(0) == '_') {
						key = key.substring(1);
					}
					else {
						key = "set " + key;
						if (value instanceof String)
							value = escape((String) value);
					}
					appendCmd(commands, key + " " + value);
				}
			}
			switch (axesMode) {
			case JmolConstants.AXES_MODE_UNITCELL:
				appendCmd(commands, "set axesUnitcell");
				break;
			case JmolConstants.AXES_MODE_BOUNDBOX:
				appendCmd(commands, "set axesWindow");
				break;
			default:
				appendCmd(commands, "set axesMolecular");
			}
			// variables only:
			e = htParameterValues.keys();
			while (e.hasMoreElements()) {
				key = (String) e.nextElement();
				if (key.charAt(0) == '@')
					appendCmd(commands, key + " " + htParameterValues.get(key));
			}
			return commands + "\n";
		}

	}

	// /////// state serialization

	static String escape(BitSet bs) {
		if (bs == null)
			return "({})";
		StringBuffer s = new StringBuffer("({");
		int imax = bs.size();
		int iLast = -1;
		int iFirst = -2;
		int i = -1;
		while (++i <= imax) {
			boolean isSet = bs.get(i);
			if (i == imax || iLast >= 0 && !isSet) {
				if (iLast >= 0 && iFirst != iLast)
					s.append((iFirst == iLast - 1 ? " " : ":") + iLast);
				if (i == imax) {
					s.append("})");
					return s.toString();
				}
				iLast = -1;
			}
			if (bs.get(i)) {
				if (iLast < 0) {
					s.append((iFirst == -2 ? "" : " ") + i);
					iFirst = i;
				}
				iLast = i;
			}
		}
		return "({})"; // impossible return
	}

	static BitSet unescapeBitset(String strBitset) {
		if (strBitset == "{null}")
			return null;
		BitSet bs = new BitSet();
		int len = strBitset.length();
		int iPrev = -1;
		int iThis = -2;
		char ch;
		if (len < 3)
			return bs;
		for (int i = 0; i < len; i++) {
			switch (ch = strBitset.charAt(i)) {
			case '}':
			case '{':
			case ' ':
				if (iThis < 0)
					break;
				if (iPrev < 0)
					iPrev = iThis;
				for (int j = iPrev; j <= iThis; j++)
					bs.set(j);
				iPrev = -1;
				iThis = -2;
				break;
			case ':':
				iPrev = iThis;
				iThis = -2;
				break;
			default:
				if (Character.isDigit(ch)) {
					if (iThis < 0)
						iThis = 0;
					iThis = (iThis << 3) + (iThis << 1) + (ch - '0');
				}
			}
		}
		return bs;
	}

	static String escape(String str) {
		if (str == null)
			return "\"\"";
		int pt = -2;
		while ((pt = str.indexOf("\"", pt + 2)) >= 0)
			str = str.substring(0, pt) + '\\' + str.substring(pt);
		return "\"" + str + "\"";
	}

	static String escape(Tuple3f xyz) {
		return "{" + xyz.x + " " + xyz.y + " " + xyz.z + "}";
	}

	static String escapeColor(int argb) {
		return "[x" + Graphics3D.getHexColorFromRGB(argb) + "]";
	}

	@SuppressWarnings("unchecked")
	static void setStateInfo(Hashtable ht, int i1, int i2, String key) {
		BitSet bs;
		if (ht.containsKey(key)) {
			bs = (BitSet) ht.get(key);
		}
		else {
			bs = new BitSet();
			ht.put(key, bs);
		}
		for (int i = i1; i <= i2; i++)
			bs.set(i);
	}

	static String getCommands(Hashtable ht) {
		return getCommands(ht, null, -1, "select");
	}

	static String getCommands(Hashtable htDefine, Hashtable htMore, int nAll) {
		return getCommands(htDefine, htMore, nAll, "select");
	}

	static String getCommands(Hashtable htDefine, Hashtable htMore, int nAll, String selectCmd) {
		StringBuffer s = new StringBuffer();
		String setPrev = getCommands(htDefine, s, null, nAll, selectCmd);
		if (htMore != null)
			getCommands(htMore, s, setPrev, nAll, selectCmd);
		return s.toString();
	}

	static String getCommands(Hashtable ht, StringBuffer s, String setPrev, int nAll, String selectCmd) {
		if (ht == null)
			return "";
		String strAll = "({0:" + (nAll - 1) + "})";
		Enumeration e = ht.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String set = escape((BitSet) ht.get(key));
			if (set.length() < 5) // nothing selected
				continue;
			if (!set.equals(setPrev))
				appendCmd(s, selectCmd + " " + (set.equals(strAll) ? "*" : set));
			setPrev = set;
			if (key.indexOf("-") < 0) // - for key means none required
				appendCmd(s, key);
		}
		return setPrev;
	}

	static void appendCmd(StringBuffer s, String cmd) {
		if (cmd.length() == 0)
			return;
		s.append(cmd + ";\n");
	}
}