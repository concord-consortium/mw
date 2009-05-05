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

package org.concord.mw2d;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.IconPool;
import org.concord.mw2d.models.MDModel;

public final class UserAction {

	final static Cursor rotateCursor1 = createCursor("images/cursors/rotate1.gif", new Point(16, 17), "rotate");
	final static Cursor rotateCursor2 = createCursor("images/cursors/rotate2.gif", new Point(14, 11), "rotate");
	final static Cursor rotateCursor3 = createCursor("images/cursors/rotate3.gif", new Point(16, 17), "rotate");

	final static Icon meanposIcon = new ImageIcon(UserAction.class.getResource("images/meanpos.gif"));
	final static Icon meanforIcon = new ImageIcon(UserAction.class.getResource("images/meanfor.gif"));
	final static Icon polarIcon = new ImageIcon(UserAction.class.getResource("images/editor/PolarizeIcon.gif"));
	final static Icon steerIcon = new ImageIcon(UserAction.class.getResource("images/editor/Steer.gif"));
	final static Icon unsteerIcon = new ImageIcon(UserAction.class.getResource("images/editor/Unsteer.gif"));

	private final static Point hotSpot = new Point();
	private final static Cursor anchorCursor = createCursor("images/cursors/fingeranchor.gif", new Point(7, 0),
			"finger&anchor");
	private final static Cursor damperCursor = createCursor("images/cursors/fingerdamper.gif", new Point(7, 0),
			"finger&damper");
	private final static Cursor polarCursor = createCursor("images/cursors/polar.gif", hotSpot, "dipole");

	private static ResourceBundle bundle;
	private static boolean isUSLocale = Locale.getDefault().equals(Locale.US);

	static {
		if (!isUSLocale) {
			try {
				bundle = ResourceBundle.getBundle("org.concord.mw2d.images.UserAction", Locale.getDefault());
			}
			catch (MissingResourceException e) {
			}
		}
	}

	private UserAction() {
	}

	static String getInternationalText(String name) {
		if (bundle == null)
			return null;
		if (isUSLocale)
			return null;
		if (name == null)
			return null;
		String s = null;
		try {
			s = bundle.getString(name);
		}
		catch (MissingResourceException e) {
			s = null;
		}
		return s;
	}

	private static Map<Short, Cursor> cursor = new HashMap<Short, Cursor>();
	private static Map<Short, String> name = new HashMap<Short, String>();
	private static Map<Short, Icon> icon = new HashMap<Short, Icon>();
	private static Map<Short, Integer> mnemonic = new HashMap<Short, Integer>();
	private static Map<Short, KeyStroke> accelerator = new HashMap<Short, KeyStroke>();
	private static Map<Short, String> description = new HashMap<Short, String>();
	private static Map<Short, String> longDescription = new HashMap<Short, String>();

	public static Cursor createCursor(String s, Point hotSpot, String name) {
		URL url = UserAction.class.getResource(s);
		return ModelerUtilities.createCursor(url, hotSpot, name);
	}

	public static Action getAction(final short actionID, final MDModel model) {
		if (actionID == COUN_ID || actionID == MEAS_ID || actionID == TRAJ_ID || actionID == RAVE_ID
				|| actionID == WHAT_ID || actionID == MARK_ID || actionID == SELE_ID) {
			Action a = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					((MDView) model.getView()).setAction(actionID);
				}
			};
			a.putValue(Action.SMALL_ICON, UserAction.getIcon(actionID));
			a.putValue(Action.NAME, UserAction.getName(actionID));
			a.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(actionID));
			a.putValue(Action.ACCELERATOR_KEY, UserAction.getAcceleratorKey(actionID));
			a.putValue(Action.SHORT_DESCRIPTION, UserAction.getDescription(actionID));
			a.putValue(Action.LONG_DESCRIPTION, UserAction.getLongDescription(actionID));
			return a;
		}
		Action a = new ModelAction(model, new Executable() {
			public void execute() {
				((MDView) model.getView()).setAction(actionID);
			}
		}) {
		};
		a.putValue(Action.SMALL_ICON, UserAction.getIcon(actionID));
		a.putValue(Action.NAME, UserAction.getName(actionID));
		a.putValue(Action.MNEMONIC_KEY, UserAction.getMnemonicKey(actionID));
		a.putValue(Action.ACCELERATOR_KEY, UserAction.getAcceleratorKey(actionID));
		a.putValue(Action.SHORT_DESCRIPTION, UserAction.getDescription(actionID));
		a.putValue(Action.LONG_DESCRIPTION, UserAction.getLongDescription(actionID));
		return a;
	}

	public static Icon getIcon(short id) {
		return icon.get(id);
	}

	public static Integer getMnemonicKey(short id) {
		return mnemonic.get(id);
	}

	public static KeyStroke getAcceleratorKey(short id) {
		return accelerator.get(id);
	}

	public static String getDescription(short id) {
		return description.get(id);
	}

	public static String getLongDescription(short id) {
		return longDescription.get(id);
	}

	public static String getName(short id) {
		return name.get(id);
	}

	public static Cursor getCursor(short id) {
		Cursor c = cursor.get(id);
		if (c != null)
			return c;
		return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}

	public final static short NONE_ID = 0x1f3f;

	/** action ID for inserting an Nt atom */
	public final static short ADDA_ID = 0x1f40;
	static {
		name.put(ADDA_ID, "Add Nt");
		icon.put(ADDA_ID, new ImageIcon(UserAction.class.getResource("images/editor/AddAtomA.gif")));
		cursor.put(ADDA_ID, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddA");
		description.put(ADDA_ID, s != null ? s : "Drop an Nt(#1 fictitious element) Atom");
		longDescription
				.put(
						ADDA_ID,
						"<html><p><b><i>Drop an Nt(#1 fictitious element) atom at the mouse-clicked position.</i></b></p><p><b>Nt</b> is a fictitious type of chemical element in Molecular Workbench.</p></html>");
	}

	/** action ID for inserting an Pl atom */
	public final static short ADDB_ID = 0x1f41;
	static {
		name.put(ADDB_ID, "Add Pl");
		icon.put(ADDB_ID, new ImageIcon(UserAction.class.getResource("images/editor/AddAtomB.gif")));
		cursor.put(ADDB_ID, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddB");
		description.put(ADDB_ID, s != null ? s : "Drop an Pl(#2 fictitious element) Atom");
		longDescription
				.put(
						ADDB_ID,
						"<html><p><b><i>Drop an Pl(#2 fictitious element) atom at the mouse-clicked position.</i></b></p><p><b>Pl</b> is a fictitious type of chemical element in Molecular Workbench.</p></html>");
	}

	/** action ID for inserting an Ws atom */
	public final static short ADDC_ID = 0x1f42;
	static {
		name.put(ADDC_ID, "Add Ws");
		icon.put(ADDC_ID, new ImageIcon(UserAction.class.getResource("images/editor/AddAtomC.gif")));
		cursor.put(ADDC_ID, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddC");
		description.put(ADDC_ID, s != null ? s : "Drop an Ws(#3 fictitious element) Atom");
		longDescription
				.put(
						ADDC_ID,
						"<html><p><b><i>Drop an Ws(#3 fictitious element) atom at the mouse-clicked position.</i></b></p><p><b>Ws</b> is a fictitious type of chemical element in Molecular Workbench.</p></html>");
	}

	/** action ID for inserting an Ck atom */
	public final static short ADDD_ID = 0x1f43;
	static {
		name.put(ADDD_ID, "Add Ck");
		icon.put(ADDD_ID, new ImageIcon(UserAction.class.getResource("images/editor/AddAtomD.gif")));
		cursor.put(ADDD_ID, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddD");
		description.put(ADDD_ID, s != null ? s : "Drop an Ck(#4 fictitious element) Atom");
		longDescription
				.put(
						ADDD_ID,
						"<html><p><b><i>Drop an Ck(#4 fictitious element) atom at the mouse-clicked position.</i></b></p><p><b>Ck</b> is a fictitious type of chemical element in Molecular Workbench.</p></html>");
	}

	/** action ID for analyzing region */
	public final static short COUN_ID = 0x1f44;
	static {
		name.put(COUN_ID, "Count Objects");
		icon.put(COUN_ID, new ImageIcon(UserAction.class.getResource("images/editor/Counter.gif")));
		cursor.put(COUN_ID, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("CountObjects");
		description.put(COUN_ID, s != null ? s : "Count objects in the selected area");
		longDescription
				.put(
						COUN_ID,
						"<html><p><b><i>Count objects that fall within an area selected by mouse dragging.</i></b></p><p>The simulation has to be paused to use this counter.</p></html>");
	}

	/** action ID for deleting particles */
	public final static short DELE_ID = 0x1f45;
	static {
		Short i = new Short(DELE_ID);
		name.put(i, "Delete Objects");
		icon.put(i, IconPool.getIcon("remove rect"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("DeleteObjects");
		description.put(i, s != null ? s : "Select objects and remove");
		longDescription
				.put(
						i,
						"<html><p><b><i>Specify a rectangular area and remove all the objects that fall within,</p><p>or intersects with, the selected area.</i></b></p></html>");
	}

	/** action ID for drawing a line */
	public final static short LINE_ID = 0x1f46;
	static {
		Short i = new Short(LINE_ID);
		name.put(i, "Draw Lines");
		icon.put(i, IconPool.getIcon("linetool"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("DrawLine");
		description.put(i, s != null ? s : "Draw a line");
		longDescription.put(i, "<html><p><b><i>Draw a line.</i></b></p></html>");
	}

	/** action ID for increasing the restraint on an object */
	public final static short IRES_ID = 0x1f47;
	static {
		Short i = new Short(IRES_ID);
		name.put(i, "Increase Restraint");
		icon.put(i, IconPool.getIcon("restrain"));
		cursor.put(i, anchorCursor);
		String s = getInternationalText("IncreaseRestraint");
		description.put(i, s != null ? s : "Increase the restraint on an object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Increase the strength of the harmonic restraint applied to the selected object.</i></b></p><p>A harmonic restraint on an object is a harmonic force that is propotional to the displacemen of</p><p>the object from its equilibrium position. It can be used to keep an object at a fixed position,</p><p>while allowing it a vibrational degree of freedom.</p></html>");
	}

	/** action ID for decreasing the restraint on an object */
	public final static short DRES_ID = 0x1f48;
	static {
		Short i = new Short(DRES_ID);
		name.put(i, "Decrease Restraint");
		icon.put(i, IconPool.getIcon("release"));
		cursor.put(i, anchorCursor);
		String s = getInternationalText("DecreaseRestraint");
		description.put(i, s != null ? s : "Decrease the restraint on an object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Decrease the strength of the harmonic restraint applied to the selected object.</i></b></p><p>A harmonic restraint on an object is a harmonic force that is propotional to the displacemen of</p><p>the object from its equilibrium position. Decreasing the strength of a restraint allows the object</p><p>to vibrate in a larger area.</p></html>");
	}

	/** action ID for adding a fraction of positive charge to a particle */
	public final static short CPOS_ID = 0x1f49;
	static {
		Short i = new Short(CPOS_ID);
		name.put(i, "Add Positive Charge");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/poschar.gif")));
		cursor.put(i, createCursor("images/cursors/battery.gif", hotSpot, "positive charge"));
		String s = getInternationalText("AddPositiveCharge");
		description.put(i, s != null ? s : "Add positive charge");
		longDescription
				.put(
						i,
						"<html><p><b><i>Add a fraction of positive charge to the clicked spot of the selected object.</i></b></p><p>The amount of charge that will be added at every single click is set to be 0.25 e, but it can be customized.</p></html>");
	}

	/** action ID for adding a fraction of negative charge to a particle */
	public final static short CNEG_ID = 0x1f4a;
	static {
		Short i = new Short(CNEG_ID);
		name.put(i, "Add Negative Charge");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/negchar.gif")));
		cursor.put(i, createCursor("images/cursors/battery2.gif", hotSpot, "negative charge"));
		String s = getInternationalText("AddNegativeCharge");
		description.put(i, s != null ? s : "Add negative charge");
		longDescription
				.put(
						i,
						"<html><p><b><i>Add a fraction of negative charge to the clicked spot of the selected object.</i></b></p><p>The amount of charge that will be added at every single click is set to be -0.25 e, but it can be customized.</p></html>");
	}

	/** action ID for rotating an object */
	public final static short ROTA_ID = 0x1f4b;
	static {
		Short i = new Short(ROTA_ID);
		name.put(i, "Rotate Object");
		cursor.put(i, rotateCursor2);
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/Rotate.gif")));
		String s = getInternationalText("RotateObject");
		description.put(i, s != null ? s : "Rotate an object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Rotate the selected object.</i></b></p><p>To rotate, please grab the handle(s) and drag up and down.</p></html>");
	}

	/** action ID for inserting an Gay-Berne particle */
	public final static short ADGB_ID = 0x1f4c;
	static {
		Short i = new Short(ADGB_ID);
		name.put(i, "Add GB");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddGB.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddGB");
		description.put(i, s != null ? s : "Drop a Gay-Berne particle");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop an elliptical Gay-Berne particle at the mouse-clicked position.</i></b></p><p>This command can be customized, so that Gay-Berne particles with different sizes and</p><p>orientations can be dropped.</p></html>");
	}

	/** action ID for inserting an obstacle */
	public final static short ADOB_ID = 0x1f4d;
	static {
		Short i = new Short(ADOB_ID);
		name.put(i, "Add Obstacle");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddObstacle_Small.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("AddObstacle");
		description.put(i, s != null ? s : "Drop a rectangular obstacle");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop a rectangular obstacle whose upper-left corner is at the mouse-clicked position, and drag to specify its size.</i></b></p><p>A rectangular obstacle changes the motion of other objects by elastically colliding with them.</p></html>");
	}

	/** action ID for creating a radial bond by dragging between two atoms */
	public final static short DGBD_ID = 0x1f4e;
	static {
		Short i = new Short(DGBD_ID);
		name.put(i, "Create Radial Bond");
		cursor.put(i, ModelerUtilities.createCursor(new ImageIcon(IconPool.class
				.getResource("images/radialbondcursor.gif")), hotSpot, "bond"));
		icon.put(i, IconPool.getIcon("radial bond"));
		String s = getInternationalText("MakeRadialBond");
		description.put(i, s != null ? s : "Create a radial bond between a pair of atoms");
		longDescription
				.put(
						i,
						"<html><p><b><i>Create a radial bond by dragging between two atoms.</i></b></p><p>A radial bond is a harmonic force that connects a pair of atoms. It models the force caused by covalent</p><p>bonding that maintains the stability of the molecular structure.</p></html>");
	}

	/** action ID for building a radial bond */
	public final static short BBON_ID = 0x1f4f;
	static {
		Short i = new Short(BBON_ID);
		name.put(i, "Build Radial Bond");
		cursor.put(i, ModelerUtilities.createCursor(new ImageIcon(IconPool.class
				.getResource("images/radialbondcursor.gif")), hotSpot, "bond"));
		icon.put(i, IconPool.getIcon("radial bond"));
		String s = getInternationalText("MakeRadialBond");
		description.put(i, s != null ? s : "Make a radial bond between a pair of atoms");
		longDescription
				.put(
						i,
						"<html><p><b><i>Make a radial bond between the selected atom and the one that is to be clicked.</i></b></p><p>A radial bond is a harmonic force that connects a pair of atoms. It models the force caused by covalent</p><p>bonding that maintains the stability of the molecular structure.</p></html>");
	}

	/** action ID for building an angular bond */
	public final static short BBEN_ID = 0x1f50;
	static {
		Short i = new Short(BBEN_ID);
		name.put(i, "Build Angular Bond");
		icon.put(i, IconPool.getIcon("angular bond"));
		cursor.put(i, ModelerUtilities.createCursor(new ImageIcon(IconPool.class
				.getResource("images/angularbondcursor.gif")), hotSpot, "bend"));
		String s = getInternationalText("MakeAngularBond");
		description.put(i, s != null ? s : "Make an angular bond between a pair of radial bonds");
		longDescription
				.put(
						i,
						"<html><p><b><i>Make an angular bond between the selected radial bond and the one that is to be clicked.</i></b></p><p>An angular bond is a harmonic force that connects a pair of radial bonds that share a common atom. It models</p><p>the force caused by covalent bonding that maintains the stability of the molecular structure.</p></html>");
	}

	/** action ID for increasing damping on the selected object */
	public final static short IDMP_ID = 0x1f51;
	static {
		Short i = new Short(IDMP_ID);
		name.put(i, "Increase Damping");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/MoreDamping.gif")));
		cursor.put(i, damperCursor);
		String s = getInternationalText("IncreaseDamping");
		description.put(i, s != null ? s : "Increase damping");
		longDescription
				.put(
						i,
						"<html><p><b><i>Increase the damping on the selected object.</i></b></p><p>A damping force on an object is propotional to its speed, pointing to the opposite direction of</p><p>its velocity vector. When applied to an object, it slows down and finally stops it, if there is no</p><p>other driving force acting on it.</p></html>");
	}

	/** action ID for decreasing damping on the selected object */
	public final static short DDMP_ID = 0x1f52;
	static {
		Short i = new Short(DDMP_ID);
		name.put(i, "Decrease Damping");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/LessDamping.gif")));
		cursor.put(i, damperCursor);
		String s = getInternationalText("DecreaseDamping");
		description.put(i, s != null ? s : "Decrease damping");
		longDescription
				.put(
						i,
						"<html><p><b><i>Decrease the damping on the selected object.</i></b></p><p>A damping force on an object is propotional to its speed, pointing to the opposite direction of</p><p>its velocity vector. When applied to an object, it slows down and finally stops it, if there is no</p><p>other driving force acting on it.</p></html>");
	}

	/** action ID for inserting a benzene molecule */
	public final static short BENZ_ID = 0x1f53;
	static {
		Short i = new Short(BENZ_ID);
		name.put(i, "Add Benzene");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddBenz.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddBenzeneMolecule");
		description.put(i, s != null ? s : "Drop a benzene-like molecule");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop a benzene-like molecule whose center of mass will be placed at the mouse-clicked position.</i></b></p><p>A benzene-like molecule is made of 12 atoms, 6 of which are aligned in the vertices of a hexagon, and the other</p><p>are connected with the previous 6 from the outside of the hexagon, pointing to the center of mass.</p></html>");
	}

	/** action ID for inserting a triatomic molecule */
	public final static short WATE_ID = 0x1f54;
	static {
		Short i = new Short(WATE_ID);
		name.put(i, "Add Triatomic Molecule");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddABC.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddTriatomicMolecule");
		description.put(i, s != null ? s : "Drop a triatomic molecule");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop a triatomic molecule whose center of mass will be placed at the mouse-clicked position.</i></b></p><p>The triatomic molecule to be added can be customized.</p></html>");
	}

	/** action ID for inserting a diatomic molecule */
	public final static short ADDI_ID = 0x1f55;
	static {
		Short i = new Short(ADDI_ID);
		name.put(i, "Add Diatomic Molecule");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddAB.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddDiatomicMolecule");
		description.put(i, s != null ? s : "Drop a diatomic molecule");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop a diatomic molecule whose center of mass will be placed at the mouse-clicked position.</i></b></p><p>The diatomic molecule to be added can be customized.</p></html>");
	}

	/** action ID for inserting a chain molecule */
	public final static short ADCH_ID = 0x1f56;
	static {
		Short i = new Short(ADCH_ID);
		name.put(i, "Add Chain Molecule");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddChain.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("AddChainMolecule");
		description.put(i, s != null ? s : "Drop a chain molecule");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop a chain molecule whose center of mass will be placed at the mouse-clicked position.</i></b></p><p>The chain molecule to be added can be customized.</p></html>");
	}

	/** action ID for heating up the selected part of the system */
	public final static short HEAT_ID = 0x1f57;
	static {
		Short i = new Short(HEAT_ID);
		name.put(i, "Heat Selected Objects");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/HeatHere.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("HeatObject");
		description.put(i, s != null ? s : "Heat the selected objects");
		longDescription.put(i, "<html><p><b><i>Heat only the selected objects of the system.</i></b></p></html>");
	}

	/** action ID for cooling down the selected part of the system */
	public final static short COOL_ID = 0x1f58;
	static {
		Short i = new Short(COOL_ID);
		name.put(i, "Cool Selected Objects");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/CoolHere.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("CoolObject");
		description.put(i, s != null ? s : "Cool the selected objects");
		longDescription.put(i, "<html><p><b><i>Cool only the selected objects of the system.</i></b></p></html>");
	}

	/** action ID for setting up boundary */
	public final static short SBOU_ID = 0x1f59;
	static {
		Short i = new Short(SBOU_ID);
		name.put(i, "Setup Boundary");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/Bound.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		String s = getInternationalText("SetBoundaryConditions");
		description.put(i, s != null ? s : "Set boundary conditions");
		longDescription
				.put(
						i,
						"<html><p><b><i>Set up the boundary conditions.</i></b></p><p>Boundary conditions determine how objects move when they encounter the boundary.</p></html>");
	}

	/** action ID for mutation */
	public final static short MUTA_ID = 0x1f5a;
	static {
		Short i = new Short(MUTA_ID);
		name.put(i, "Change Type");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/mutate.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("ChangeObjectType");
		description.put(i, s != null ? s : "Change the type of this object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Change the type of the selected object.</i></b></p><p>This can be used to change, for instance, an atom of a molecule to another type of element.</p></html>");
	}

	/** action ID for setting up piston */
	public final static short SPIS_ID = 0x1f5b;
	static {
		Short i = new Short(SPIS_ID);
		name.put(i, "Setup Piston");
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		longDescription.put(i, "<html><p><b><i>Set up a piston.</i></b></p><p></p></html>");
	}

	/** action ID for increasing the dipole moment of a GB molecule */
	public final static short IPOL_ID = 0x1f5c;
	static {
		Short i = new Short(IPOL_ID);
		name.put(i, "Increase Dipole Moment");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/posdip.gif")));
		cursor.put(i, polarCursor);
		String s = getInternationalText("IncreaseDipoleMoment");
		description.put(i, s != null ? s : "Increase the dipole moment of this particle");
		longDescription
				.put(
						i,
						"<html><p><b><i>Increase the electric dipole moment of the selected object.</i></b></p><p>The electric dipole moment of a polar molecule is defined as the magnitude of the charge</p><p> times the distance vector from the negative charge to the positive one.</p></html>");
	}

	/** action ID for decreasing the dipole moment of a GB molecule */
	public final static short DPOL_ID = 0x1f5d;
	static {
		Short i = new Short(DPOL_ID);
		name.put(i, "Decrease Dipole Moment");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/negdip.gif")));
		cursor.put(i, polarCursor);
		String s = getInternationalText("DecreaseDipoleMoment");
		description.put(i, s != null ? s : "Decrease the dipole moment of this particle");
		longDescription
				.put(
						i,
						"<html><p><b><i>Decrease the electric dipole moment of the selected object.</i></b></p><p>The electric dipole moment of a polar molecule is defined as the magnitude of the charge</p><p> times the distance vector from the negative charge to the positive one.</p></html>");
	}

	/** action ID for resizing a GB molecule */
	public final static short RESI_ID = 0x1f5e;
	static {
		Short i = new Short(RESI_ID);
		name.put(i, "Resize GB");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/Resize.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		String s = getInternationalText("ResizeObject");
		description.put(i, s != null ? s : "Resize the selected object");
		longDescription.put(i, "<html><p><b><i>Resize the selected object.</i></b></p></html>");
	}

	/** action ID for showing the average position of an object */
	public final static short RAVE_ID = 0x1f5f;
	static {
		Short i = new Short(RAVE_ID);
		name.put(i, "Toggle Average Position");
		icon.put(i, meanposIcon);
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("ToggleAveragePosition");
		description.put(i, s != null ? s : "Toggle the average position of the selected object");
		longDescription
				.put(i,
						"<html><p><b><i>Show/hide the average position of the center of mass of the selected object.</i></b></p></html>");
	}

	/** action ID for drawing a rectangle */
	public final static short RECT_ID = 0x1f60;
	static {
		Short i = new Short(RECT_ID);
		name.put(i, "Draw Rectangle");
		icon.put(i, IconPool.getIcon("recttool"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("DrawRectangle");
		description.put(i, s != null ? s : "Draw a rectangle");
		longDescription.put(i, "<html><p><b><i>Draw a rectangle.</i></b></p></html>");
	}

	/** action ID for drawing an ellipse */
	public final static short ELLI_ID = 0x1f61;
	static {
		Short i = new Short(ELLI_ID);
		name.put(i, "Draw Ellipse");
		icon.put(i, IconPool.getIcon("ellipsetool"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("DrawEllipse");
		description.put(i, s != null ? s : "Draw an ellipse");
		longDescription.put(i, "<html><p><b><i>Draw an ellipse.</i></b></p></html>");
	}

	/** action ID for selecting an object */
	public final static short SELE_ID = 0x1f62;
	static {
		Short i = new Short(SELE_ID);
		name.put(i, "Select Object");
		icon.put(i, IconPool.getIcon("select"));
		// cursor.put(i, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		cursor.put(i, createCursor("images/cursors/pointToMove.gif", new Point(13, 8), "point&move"));
		String s = getInternationalText("SelectObject");
		description.put(i, s != null ? s : "Select an object");
		longDescription.put(i, "<html><p><b><i>Select an object.</i></b></p></html>");
	}

	/** action ID for measuring a distance between two objects */
	public final static short MEAS_ID = 0x1f63;
	static {
		Short i = new Short(MEAS_ID);
		name.put(i, "Measure Distance");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/Ruler.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("MeasureDistance");
		description.put(i, s != null ? s : "Measure a distance between two objects");
		longDescription
				.put(
						i,
						"<html><p><b><i>Measure the distance between the selected object and the one to be clicked, or a fixed point.</i></b></p></html>");
	}

	/** action ID for changing the velocity of an object */
	public final static short VELO_ID = 0x1f64;
	static {
		Short i = new Short(VELO_ID);
		name.put(i, "Change Velocity");
		icon.put(i, IconPool.getIcon("velocity"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("ChangeVelocity");
		description.put(i, s != null ? s : "Change the velocity of the selected object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Change the velocity of the selected object.</i></b></p><p>Both the direction and speed will be changed.</p></html>");
	}

	/** action ID for duplicating an object */
	public final static short DUPL_ID = 0x1f65;
	static {
		Short i = new Short(DUPL_ID);
		name.put(i, "Duplicate Object");
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		icon.put(i, IconPool.getIcon("copy"));
		String s = getInternationalText("DuplicateObject");
		description.put(i, s != null ? s : "Duplicate the selected object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Duplicate the selected object.</i></b></p><p>Click an object to be duplicated, drag away from it, and place the new object in other places.</html>");
	}

	/** action ID for showing the trajectory of an object */
	public final static short TRAJ_ID = 0x1f66;
	static {
		Short i = new Short(TRAJ_ID);
		name.put(i, "Toggle Trajectory");
		icon.put(i, IconPool.getIcon("traj"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("ToggleTrajectoryLine");
		description.put(i, s != null ? s : "Toggle the trajectory of the selected object");
		longDescription
				.put(i,
						"<html><p><b><i>Show/hide the trajectory of the center of mass of the selected object.</i></b></p></html>");
	}

	/** action ID for inquring an object */
	public final static short WHAT_ID = 0x1f67;
	static {
		Short i = new Short(WHAT_ID);
		name.put(i, "What's This?");
		cursor.put(i, createCursor("images/cursors/WhatIsThis.gif", hotSpot, "What's this"));
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/WhatIsThis.gif")));
		String s = getInternationalText("WhatIsThis");
		description.put(i, s != null ? s : "What is this?");
		longDescription.put(i, "<html><p><b><i>Get a short description of an object in the model.</i></b></p></html>");
	}

	/** action ID for marking object(s) */
	public final static short MARK_ID = 0x1f68;
	static {
		Short i = new Short(MARK_ID);
		name.put(i, "Mark Object");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/Mark.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("MarkObject");
		description.put(i, s != null ? s : "Mark object(s)");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drag a rectangle and mark all the objects that fall within the rectangle.</i></b></p><p>The marked objects will have a different color to indicate that they are marked.</p></html>");
	}

	/** action ID for changing angular velocity */
	public final static short OMEG_ID = 0x1f69;
	static {
		Short i = new Short(OMEG_ID);
		name.put(i, "Change Angular Velocity");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/Omega.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		String s = getInternationalText("ChangeAngularVelocity");
		description.put(i, s != null ? s : "Change the angular velocity of the selected object");
		longDescription
				.put(
						i,
						"<html><p><b><i>Change the angular velocity of the selected object.</i></b></p><p>Please drag clockwise or anti-clockwise to specify the direction.</p></html>");
	}

	/** action ID for adding curved molecular surface */
	public final static short SCUR_ID = 0x1f6a;
	static {
		Short i = new Short(SCUR_ID);
		name.put(i, "Add Curved Molecular Surface");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddCurvedSurface.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a curved molecular surface");
		longDescription
				.put(
						i,
						"<html><p><b><i>Drop a curved molecular surface.</i></b></p><p>A curved molecular surface is the closed solid shape with curved border.</p></html>");
	}

	/** action ID for adding curved molecular ribbon */
	public final static short RCUR_ID = 0x1f6b;
	static {
		Short i = new Short(RCUR_ID);
		name.put(i, "Add Curved Molecular Ribbon");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddCurvedRibbon.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a curved molecular ribbon");
		longDescription.put(i, "<html><p><b><i>Drop a curved molecular ribbon.</i></b></p></html>");
	}

	/** action ID for adding a circular molecular surface */
	public final static short SCIR_ID = 0x1f6c;
	static {
		Short i = new Short(SCIR_ID);
		name.put(i, "Add Circular Molecular Surface");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddCircularSurface.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a circular molecular surface");
		longDescription.put(i, "<html><p><b><i>Drop a circular molecular surface.</i></b></p></html>");
	}

	/** action ID for adding a circular molecular ribbon */
	public final static short RCIR_ID = 0x1f6d;
	static {
		Short i = new Short(RCIR_ID);
		name.put(i, "Add Circular Molecular Ribbon");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddCircularRibbon.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a circular molecular ribbon");
		longDescription.put(i, "<html><p><b><i>Drop a circular molecular ribbon.</i></b></p></html>");
	}

	/** action ID for adding a rectangular molecular surface */
	public final static short SREC_ID = 0x1f6e;
	static {
		Short i = new Short(SREC_ID);
		name.put(i, "Add Rectangular Molecular Surface");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddRectangularSurface.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a rectangular molecular surface");
		longDescription.put(i, "<html><p><b><i>Drop a rectangular molecular surface.</i></b></p></html>");
	}

	/** action ID for adding a rectangular molecular ribbon */
	public final static short RREC_ID = 0x1f6f;
	static {
		Short i = new Short(RREC_ID);
		name.put(i, "Add Rectangular Molecular Ribbon");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddRectangularRibbon.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a rectangular molecular ribbon");
		longDescription.put(i, "<html><p><b><i>Drop a rectangular molecular ribbon.</i></b></p></html>");
	}

	/** action ID for adding a free form molecular surface */
	public final static short SFRE_ID = 0x1f70;
	static {
		Short i = new Short(SFRE_ID);
		name.put(i, "Add Molecular Surface in Free-Form Shape");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AddFreeFormSurface.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		description.put(i, "Drop a molecular surface in free-form shape");
		longDescription.put(i, "<html><p><b><i>Drop a molecular surface in a free-form shape.</i></b></p></html>");
	}

	/** action ID for adding an amino acid to a polypeptide */
	public final static short AACD_ID = 0x1f71;
	static {
		Short i = new Short(AACD_ID);
		name.put(i, "Add an Amino Acid to Polypeptide");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AttachAminoAcid.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		description.put(i, "Add an amino acid to a polypeptide");
		longDescription
				.put(
						i,
						"<html><p><b><i>Add an amino acid to a polypeptide.</i></b></p><p>The newly added amino acid will be attached to one of the two terminals, whichever is closer to the clicked point.</html>");
	}

	/** action ID for subtracting an amino acid from a polypeptide */
	public final static short SACD_ID = 0x1f72;
	static {
		Short i = new Short(SACD_ID);
		name.put(i, "Subtract an Amino Acid from Polypeptide");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/DetachAminoAcid.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		description.put(i, "Subtract an amino acid from a polypeptide");
		longDescription
				.put(
						i,
						"<html><p><b><i>Subtract an amino acid from a polypeptide.</i></b></p><p>An amino acid will be detached from one of the two terminals, whichever is closer to the clicked point.</html>");
	}

	/** action ID for adding a nucleotide to a DNA strand */
	public final static short ANTD_ID = 0x1f73;
	static {
		Short i = new Short(ANTD_ID);
		name.put(i, "Add a Nucleotide to DNA");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/AttachNucleotide.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		description.put(i, "Add a nucleotide to a DNA strand");
		longDescription
				.put(
						i,
						"<html><p><b><i>Add a nucleotide to a DNA strand.</i></b></p><p>The newly added nucleotide will be attached to whichever end is closer to the clicked point.</html>");
	}

	/** action ID for subtracting a nucleotide from a DNA strand */
	public final static short SNTD_ID = 0x1f74;
	static {
		Short i = new Short(SNTD_ID);
		name.put(i, "Subtract a Nucleotide from DNA");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/DetachNucleotide.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		description.put(i, "Subtract a nucleotide from a DNA strand");
		longDescription
				.put(
						i,
						"<html><p><b><i>Subtract a nucleotide from a DNA strand.</i></b></p><p>A nucleotide will be detached from whichever end is closer to the clicked point.</html>");
	}

	/** action ID for filling an area with Nt atoms */
	public final static short FILA_ID = 0x1f75;
	static {
		Short i = new Short(FILA_ID);
		name.put(i, "Fill Area with Nt Atoms");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/FillAreaA.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("FillAreaWithA");
		description.put(i, s != null ? s : "Select an area and fill it with Nt(#1 fictitious element) atoms");
		longDescription
				.put(
						i,
						"<html><p><b><i>Specify a rectangular area and fill it with Nt(#1 fictitious element) atoms.</i></b></p>Atoms will be aligned on a lattice, and can be randomized.</html>");
	}

	/** action ID for filling an area with Pl atoms */
	public final static short FILB_ID = 0x1f76;
	static {
		Short i = new Short(FILB_ID);
		name.put(i, "Fill Area with Pl Atoms");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/FillAreaB.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("FillAreaWithB");
		description.put(i, s != null ? s : "Select an area and fill it with Pl(#2 fictitious element) atoms");
		longDescription
				.put(
						i,
						"<html><p><b><i>Specify a rectangular area and fill it with Pl(#2 fictitious element) atoms.</i></b></p>Atoms will be aligned on a lattice, and can be randomized.</html>");
	}

	/** action ID for filling an area with Ws atoms */
	public final static short FILC_ID = 0x1f77;
	static {
		Short i = new Short(FILC_ID);
		name.put(i, "Fill Area with Ws Atoms");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/FillAreaC.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("FillAreaWithC");
		description.put(i, s != null ? s : "Select an area and fill it with Ws(#3 fictitious element) atoms");
		longDescription
				.put(
						i,
						"<html><p><b><i>Specify a rectangular area and fill it with Ws(#3 fictitious element) atoms.</i></b></p>Atoms will be aligned on a lattice, and can be randomized.</html>");
	}

	/** action ID for filling an area with Ck atoms */
	public final static short FILD_ID = 0x1f78;
	static {
		Short i = new Short(FILD_ID);
		name.put(i, "Fill Area with Ck Atoms");
		icon.put(i, new ImageIcon(UserAction.class.getResource("images/editor/FillAreaD.gif")));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("FillAreaWithD");
		description.put(i, s != null ? s : "Select an area and fill it with Ck(#4 fictitious element) atoms");
		longDescription
				.put(
						i,
						"<html><p><b><i>Specify a rectangular area and fill it with Ck(#4 fictitious element) atoms.</i></b></p>Atoms will be aligned on a lattice, and can be randomized.</html>");
	}

	/** action ID for drawing a triangle */
	public final static short TRIA_ID = 0x1f79;
	static {
		Short i = new Short(TRIA_ID);
		name.put(i, "Draw Triangle");
		icon.put(i, IconPool.getIcon("triangletool"));
		cursor.put(i, Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		String s = getInternationalText("DrawTriangle");
		description.put(i, s != null ? s : "Draw a triangle");
		longDescription.put(i, "<html><p><b><i>Draw a triangle.</i></b></p></html>");
	}

}