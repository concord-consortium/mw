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

package org.concord.modeler.chemistry;

interface ElementData {

	public final static int NUMBEROFELEMENTS = 112;

	public final static String[] elements = { "H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al",
			"Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga",
			"Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In",
			"Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy", "Ho",
			"Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At",
			"Rn", "Fr", "Ra", "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr",
			"Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Uub" };

	public final static String[] fullNames = { "Hydrogen", "Helium", "Lithium", "Beryllium", "Boron", "Carbon",
			"Nitrogen", "Oxygen", "Fluorine", "Neon", "Sodium", "Magnesium", "Aluminum", "Silicon", "Phosphorus",
			"Sulfur", "Chlorine", "Argon", "Potassium", "Calcium", "Scandium", "Titanium", "Vanadium", "Chromium",
			"Manganese", "Iron", "Cobalt", "Nickel", "Copper", "Zinc", "Gallium", "Germanium", "Arsenic", "Selenium",
			"Bromine", "Krypton", "Rubidium", "Strontium", "Yttrium", "Zirconium", "Niobium", "Molybdenum",
			"Technetium", "Ruthenium", "Rhodium", "Palladium", "Silver", "Cadmium", "Indium", "Tin", "Antimony",
			"Tellurium", "Iodine", "Xenon", "Cesium", "Barium", "Lanthanum", "Cerium", "Praseodymium", "Neodymium",
			"Pormethium", "Samarium", "Europium", "Gadolinium", "Terbium", "Dysprosium", "Holmium", "Erbium",
			"Thulium", "Ytterbium", "Lutetium", "Hafnium", "Tantalum", "Tungsten", "Rhenium", "Osmium", "Iridium",
			"Platinum", "Gold", "Mercury", "Thallium", "Lead", "Bismuth", "Polonium", "Astatine", "Radon", "Francium",
			"Radium", "Actinium", "Thorium", "Protactinium", "Uranium", "Neptunium", "Plutonium", "Americium",
			"Curium", "Berkelium", "Californium", "Einsteinium", "Fermium", "Mendelevium", "Nobelium", "Lawrencium",
			"Rutherfordium", "Dubnium", "Seaborgium", "Bohrium", "Hassium", "Meitnerium", "Darmstadtium",
			"Roentgenium", "Ununbium" };

	public final static String[] crystals = { "HCP", "HCP", "BCC", "HCP", "RHOMBOHEDRAL", "HCP", "HCP", "SC", "SC",
			"FCC", "BCC", "HCP", "FCC", "FCC", "MONOCLINIC", "ORTHORHOMBIC", "ORTHORHOMBIC", "FCC", "BCC", "FCC",
			"HCP", "HCP", "BCC", "BCC", "BCC", "BCC", "HCP", "FCC", "FCC", "HCP", "ORTHORHOMBIC", "FCC",
			"RHOMBOHEDRAL", "HCP", "ORTHORHOMBIC", "FCC", "BCC", "FCC", "HCP", "HCP", "BCC", "BCC", "HCP", "HCP",
			"FCC", "FCC", "FCC", "HCP", "TETRAGONAL", "TETRAGONAL", "RHOMBOHEDRAL", "HCP", "ORTHORHOMBIC", "FCC",
			"BCC", "BCC", "HCP", "FCC", "HCP", "HCP", "HCP", "RHOMBOHEDRAL", "BCC", "HCP", "HCP", "HCP", "HCP", "HCP",
			"HCP", "FCC", "HCP", "HCP", "BCC", "BCC", "HCP", "HCP", "FCC", "FCC", "FCC", "RHOMBOHEDRAL", "HCP", "FCC",
			"RHOMBOHEDRAL", "MONOCLINIC", "----", "FCC", "BCC", "BCC", "FCC", "FCC", "ORTHORHOMBIC", "ORTHORHOMBIC",
			"ORTHORHOMBIC", "MONOCLINIC", "HCP", "----", "----", "----", "----", "----", "----", "----", "----",
			"----", "----", "----", "----", "----", "----", "----", "----", "----" };

	public final static float[] weights = { 1.00794f, 4.0026f, 6.941f, 9.01218f, 10.811f, 12.011f, 14.0067f, 15.9994f,
			18.9984f, 20.1797f, 22.98977f, 24.305f, 26.98154f, 28.0855f, 30.97376f, 32.066f, 35.4527f, 39.948f,
			39.0983f, 40.078f, 44.9559f, 47.88f, 50.9415f, 51.996f, 54.938f, 55.847f, 58.9332f, 58.6934f, 63.546f,
			65.39f, 69.723f, 72.61f, 74.9216f, 78.96f, 79.904f, 83.8f, 85.4678f, 87.62f, 88.9059f, 91.224f, 92.9064f,
			95.94f, 98.0f, 101.07f, 102.9055f, 106.42f, 107.868f, 112.41f, 114.82f, 118.71f, 121.757f, 127.6f,
			126.9045f, 131.29f, 132.9054f, 137.33f, 138.9055f, 140.12f, 140.9077f, 144.24f, 145.0f, 150.36f, 151.965f,
			157.25f, 158.9253f, 162.5f, 164.9303f, 167.26f, 168.9342f, 173.04f, 174.967f, 178.49f, 180.9479f, 183.85f,
			186.207f, 190.2f, 192.22f, 195.08f, 196.9665f, 200.59f, 204.383f, 207.2f, 208.9804f, 209f, 210f, 222f,
			223.0f, 226.0254f, 227f, 232.0381f, 231.0359f, 238.029f, 237.0482f, 244f, 243f, 247f, 247f, 251f, 252f,
			257f, 258f, 259f, 262f, 261f, 262f, 263f, 262f, 265f, 266f, 269f, 272f, 277f };

	public final static String[] shells = { "1s(1)", "1s(2)", "[He]2s(1)", "[He]2s(2)", "[He]2s(2)2p(1)",
			"[He]2s(2)2p(2)", "[He]2s(2)2p(3)", "[He]2s(2)2p(4)", "[He]2s(2)2p(5)", "[He]2s(2)2p(6)", "[Ne]3s(1)",
			"[Ne]3s(2)", "[Ne]3s(2)3p(1)", "[Ne]3s(2)3p(2)", "[Ne]3s(2)3p(3)", "[Ne]3s(2)3p(4)", "[Ne]3s(2)3p(5)",
			"[Ne]3s(2)3p(6)", "[Ar]4s(1)", "[Ar]4s(2)", "[Ar]3d(1)4s(2)", "[Ar]3d(2)4s(2)", "[Ar]3d(3)4s(2)",
			"[Ar]3d(5)4s(1)", "[Ar]3d(5)4s(2)", "[Ar]3d(6)4s(2)", "[Ar]3d(7)4s(2)", "[Ar]3d(8)4s(2)",
			"[Ar]3d(10)4s(1)", "[Ar]3d(10)4s(2)", "[Ar]3d(10)4s(2)4p(1)", "[Ar]3d(10)4s(2)4p(2)",
			"[Ar]3d(10)4s(2)4p(3)", "[Ar]3d(10)4s(2)4p(4)", "[Ar]3d(10)4s(2)4p(5)", "[Ar]3d(10)4s(2)4p(6)",
			"[Kr]5s(1)", "[Kr]5s(2)", "[Kr]4d(1)5s(2)", "[Kr]4d(2)5s(2)", "[Kr]4d(4)5s(1)", "[Kr]4d(5)5s(1)",
			"[Kr]4d(5)5s(2)", "[Kr]4d(7)5s(1)", "[Kr]4d(8)5s(1)", "[Kr]4d(10)", "[Kr]4d(10)5s(1)", "[Kr]4d(10)5s(2)",
			"[Kr]4d(10)5s(2)5p(1)", "[Kr]4d(10)5s(2)5p(2)", "[Kr]4d(10)5s(2)5p(3)", "[Kr]4d(10)5s(2)5p(4)",
			"[Kr]4d(10)5s(2)5p(5)", "[Kr]4d(10)5s(2)5p(6)", "[Xe]6s(1)", "[Xe]6s(2)", "[Xe]5d(1)6s(2)",
			"[Xe]5d(1)6s(2)", "[Xe]4f(1)5d(1)6s(2)", "[Xe]4f(3)6s(2)", "[Xe]4f(4)6s(2)", "[Xe]4f(5)6s(2)",
			"[Xe]4f(6)6s(2)", "[Xe]4f(7)6s(2)", "[Xe]4f(7)5d(1)6s(2)", "[Xe]4f(9)6s(2)", "[Xe]4f(10)6s(2)",
			"[Xe]4f(11)6s(2)", "[Xe]4f(12)6s(2)", "[Xe]4f(13)6s(2)", "[Xe]4f(14)6s(2)", "[Xe]4f(14)5d(2)6s(2)",
			"[Xe]4f(14)5d(3)6s(2)", "[Xe]4f(14)5d(4)6s(2)", "[Xe]4f(14)5d(5)6s(2)", "[Xe]4f(14)5d(6)6s(2)",
			"[Xe]4f(14)5d(7)6s(2)", "[Xe]4f(14)5d(9)6s(1)", "[Xe]4f(14)5d(10)6s(1)", "[Xe]4f(14)5d(10)6s(2)",
			"[Xe]4f(14)5d(10)6s(2)6p(1)", "[Xe]4f(14)5d(10)6s(2)6p(2)", "[Xe]4f(14)5d(10)6s(2)6p(3)",
			"[Xe]4f(14)5d(10)6s(2)6p(4)", "[Xe]4f(14)5d(10)6s(2)6p(5)", "[Xe]4f(14)5d(10)6s(2)6p(6)", "[Rn]7s(1)",
			"[Rn]7s(2)", "[Rn]6d(1)7s(2)", "[Rn]6d(2)7s(2)", "[Rn]5f(2)6d(1)7s(2)", "[Rn]5f(3)6d(1)7s(2)",
			"[Rn]5f(4)6d(1)7s(2)", "[Rn]5f(6)7s(2)", "[Rn]5f(7)7s(2)", "[Rn]5f(7)6d(1)7s(2)", "[Rn]5f(9)7s(2)",
			"[Rn]5f(10)7s(2)", "[Rn]5f(11)7s(2)", "[Rn]5f(12)7s(2)", "[Rn]5f(13)7s(2)", "[Rn]5f(14)7s(2)",
			"[Rn]5f(14)6d(1)7s(2)", "[Rn]5f(14)6d(2)7s(2)", "[Rn]5f(14)6d(3)7s(2)", "[Rn]5f(14)6d(4)7s(2)",
			"[Rn]5f(14)6d(5)7s(2)", "[Rn]5f(14)6d(6)7s(2)", "[Rn]5f(14)6d(7)7s(2)", "[Rn]5f(14)6d(8)7s(2)",
			"[Rn]5f(14)6d(9)7s(2)", "[Rn]5f(14)6d(10)7s(2)" };

	public final static float[] covalentRadii = { 0.32f, 0.93f, 1.23f, 0.9f, 0.82f, 0.77f, 0.75f, 0.73f, 0.72f, 0.71f,
			1.54f, 1.36f, 1.18f, 1.11f, 1.06f, 1.02f, 0.99f, 0.98f, 2.03f, 1.74f, 1.44f, 1.32f, 1.22f, 1.18f, 1.17f,
			1.17f, 1.16f, 1.15f, 1.17f, 1.25f, 1.26f, 1.22f, 1.2f, 1.16f, 1.14f, 1.89f, 2.16f, 1.91f, 1.62f, 1.45f,
			1.34f, 1.3f, 1.27f, 1.25f, 1.25f, 1.28f, 1.34f, 1.41f, 1.44f, 1.41f, 1.4f, 1.36f, 1.33f, 1.31f, 2.35f,
			1.98f, 1.25f, 1.65f, 1.65f, 1.64f, 1.63f, 1.62f, 1.85f, 1.61f, 1.59f, 1.59f, 1.58f, 1.57f, 1.56f, 1.7f,
			1.56f, 1.44f, 1.34f, 1.3f, 1.28f, 1.26f, 1.27f, 1.3f, 1.34f, 1.49f, 1.48f, 1.47f, 1.46f, 1.53f, 1.47f, -1f,
			-1f, -1f, -1f, 1.65f, -1f, 1.42f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f,
			-1f, -1f, -1f, -1f, -1f, -1f };

	public final static float[] atomicRadii = { 0.79f, 0.49f, 1.55f, 1.12f, 0.98f, 0.91f, 0.92f, 0.65f, 0.57f, 0.51f,
			1.9f, 1.6f, 1.43f, 1.32f, 1.28f, 1.27f, 0.97f, 0.88f, 2.35f, 1.97f, 1.62f, 1.45f, 1.34f, 1.3f, 1.35f,
			1.26f, 1.25f, 1.24f, 1.28f, 1.38f, 1.41f, 1.37f, 1.39f, 1.4f, 1.12f, 1.03f, 2.48f, 2.15f, 1.78f, 1.6f,
			1.46f, 1.39f, 1.36f, 1.34f, 1.34f, 1.37f, 1.44f, 1.71f, 1.66f, 1.62f, 1.59f, 1.42f, 1.32f, 1.24f, 2.67f,
			2.22f, 1.38f, 1.81f, 1.82f, 1.82f, -1f, 1.81f, 1.99f, 1.8f, 1.8f, 1.8f, 1.79f, 1.78f, 1.77f, 1.94f, 1.75f,
			1.67f, 1.49f, 1.41f, 1.37f, 1.35f, 1.36f, 1.39f, 1.46f, 1.6f, 1.71f, 1.75f, 1.7f, 1.67f, 1.45f, 1.34f,
			2.7f, 2.33f, 1.88f, 1.8f, 1.61f, 1.38f, 1.3f, 1.51f, 1.84f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f,
			-1f, -1f, -1f, -1f, -1f, -1f, -1f, -1f };

	public final static float[] electroNegativityPauling = { 2.20f, -1.0f, 0.98f, 1.57f, 2.04f, 2.55f, 3.04f, 3.44f,
			3.98f, -1.0f, 0.93f, 1.31f, 1.61f, 1.90f, 2.19f, 2.58f, 3.16f, -1.0f, 0.82f, 1.00f, 1.36f, 1.54f, 1.63f,
			1.66f, 1.55f, 1.83f, 1.88f, 1.91f, 1.90f, 1.65f, 1.81f, 2.01f, 2.18f, 2.55f, 2.96f, -1.0f, 0.82f, 0.95f,
			1.22f, 1.33f, 1.60f, 2.16f, 1.90f, 2.20f, 2.28f, 2.20f, 1.93f, 1.69f, 1.78f, 1.96f, 2.05f, 2.10f, 2.66f,
			2.60f, 0.79f, 0.89f, 1.10f, 1.12f, 1.13f, 1.14f, 0.94f, 1.17f, 1.20f, 0.94f, 1.22f, 1.23f, 1.24f, 1.25f,
			0.96f, 1.27f, 1.30f, 1.50f, 2.36f, 1.90f, 2.20f, 2.20f, 2.28f, 2.54f, 2.00f, 1.80f, 2.33f, 2.02f, 2.00f,
			2.20f, -1.0f, 0.70f, 0.89f, 1.10f, 1.30f, 1.50f, 1.38f, 1.36f, 1.28f, 1.30f, 1.30f, 1.30f, 1.30f, 1.30f,
			1.30f, 1.30f, 1.30f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f };

	public final static byte[] sounds = { 24, 35, 36, 38, 39, 41, 43, 44, 46, 47, 48, 50, 51, 53, 55, 56, 58, 59, 60,
			62, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 63, 65, 67, 68, 70, 71, 72, 74, 10, 10, 10, 10, 10, 10, 10, 10,
			10, 10, 75, 77, 79, 80, 82, 83, 84, 86, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
			10, 10, 10, 10, 10, 10, 10, 87, 89, 91, 92, 94, 95, 96, 98, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
			10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };

}