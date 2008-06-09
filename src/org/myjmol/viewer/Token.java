/* $RCSfile: Token.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
 * $Revision: 1.12 $
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

package org.myjmol.viewer;

import org.myjmol.util.Logger;

import java.util.Hashtable;

@SuppressWarnings("unchecked")
class Token {

  int tok;
  Object value;
  int intValue = Integer.MAX_VALUE;

  Token(int tok, int intValue, Object value) {
    this.tok = tok;
    this.intValue = intValue;
    this.value = value;
  }

  Token(int tok, int intValue) {
    this.tok = tok;
    this.intValue = intValue;
  }

  Token(int tok) {
    this.tok = tok;
  }

  Token(int tok, Object value) {
    this.tok = tok;
    this.value = value;
  }

  final static int nada              =  0;
  final static int identifier        =  1;
  final static int integer           =  2;
  final static int decimal           =  3;
  final static int string            =  4;
  final static int seqcode           =  5;
  final static int unknown           =  6;
  final static int keyword           =  7;
  final static int whitespace        =  8;
  final static int comment           =  9;
  final static int endofline         = 10;
  final static int endofstatement    = 11;

  final static String[] astrType = {
    "nada", "identifier", "integer", "decimal", "string",
    "seqcode",  "unknown", "keyword"
  };

  final static int command           = (1 <<  8);
  final static int expressionCommand = (1 <<  9); // expression command
  final static int embeddedExpression= (1 << 10); // embedded expression
  final static int setparam          = (1 << 11); // parameter to set command
  final static int showparam         = (1 << 12); // parameter to show command
  final static int bool              = (1 << 13);
  final static int misc              = (1 << 14); // misc parameter
  final static int expression        = (1 << 15); /// expression term
  // every property is also valid in an expression context
  final static int atomproperty      = (1 << 16) | expression;
  // every predefined is also valid in an expression context
  final static int comparator        = (1 << 17) | expression;
  final static int predefinedset     = (1 << 18) | expression;
  final static int colorparam        = (1 << 19);
  final static int specialstring     = (1 << 20); // echo, label
  // generally, the minus sign is used to denote atom ranges
  // this property is used for the few commands which allow negative integers
  final static int negnums           = (1 << 21);
  // for some commands the 'set' is optional
  // so, just delete the set command from the token list
  // but not for hbonds nor ssbonds
  final static int setspecial        = (1 << 22);
  final static int objectid          = (1 << 23);
  
  final static int coordOrSet = negnums | embeddedExpression; 

  // These are unrelated
  final static int varArgCount       = (1 << 4);
  final static int onDefault1        = (1 << 5) | 1;
  
  // rasmol commands
  final static int backbone     = command |  0 | bool | predefinedset;
  final static int background   = command |  1 | colorparam | setspecial;
  final static int bond         = command |  2 | setparam | bool;
  final static int cartoon      = command |  3 | setparam;
  final static int center       = command |  4 | setparam | showparam | expressionCommand;
  final static int clipboard    = command |  5;
  final static int color        = command |  6 | colorparam | setparam;
  final static int connect      = command |  7 | embeddedExpression;
  final static int data         = command |  8 | showparam;
  final static int define       = command |  9 | expressionCommand;
  final static int dots         = command | 10 | embeddedExpression | bool;
  final static int echo         = command | 11 | setparam | specialstring;
  final static int exit         = command | 12;
  final static int hbond        = command | 13 | setparam | bool;
  final static int help         = command | 14 | setparam | specialstring;
  final static int label        = command | 15 | specialstring;
  final static int load         = command | 16 | negnums;
  final static int monitor      = command | 18 | setparam | showparam | bool | embeddedExpression | expression;
  final static int pause        = command | 19 | misc;
  final static int print        = command | 20;
  final static int quit         = command | 21;
  final static int refresh      = command | 22;
  final static int renumber     = command | 23 | negnums;
  final static int reset        = command | 24;
  final static int restrict     = command | 25 | expressionCommand;
  final static int ribbon       = command | 26 | bool;
  final static int rotate       = command | 27 | bool | coordOrSet;
  final static int save         = command | 28 | showparam;
  final static int script       = command | 29 | specialstring;
  final static int select       = command | 30 | expressionCommand;
  final static int set          = command | 31 | bool | negnums | embeddedExpression;
  final static int show         = command | 32;
  final static int slab         = command | 33 | bool;
  final static int cpk          = command | 35 | setparam | bool | negnums;
  final static int ssbond       = command | 36 | setparam | bool;
  final static int stereo       = command | 38 | colorparam | negnums;// | setspecial | bool | negnums ;
  final static int strands      = command | 39 | setparam | bool;
  final static int structure    = command | 40;
  final static int trace        = command | 41 | bool;
  final static int translate    = command | 42 | negnums;
  final static int unbond       = command | 43;
  final static int wireframe    = command | 44 | bool;
  final static int write        = command | 45 | setparam;
  final static int zap          = command | 46;
  final static int zoom         = command | 47 | showparam | negnums | embeddedExpression;
  final static int zoomTo       = command | 48 | showparam | negnums | embeddedExpression;
  final static int initialize   = command | 49;
  // openrasmol commands
  final static int depth        = command | 50;
  final static int star         = command | 51;
  // chime commands
  final static int delay        = command | 60;
  final static int loop         = command | 61;
  final static int move         = command | 62 | negnums;
  final static int view         = command | 63;
  final static int spin         = command | 64 | setparam | showparam | bool | coordOrSet;
  final static int list         = command | 65 | showparam;
  final static int display3d    = command | 66;
  final static int animation    = command | 67;
  final static int frame        = command | 68;
  // jmol commands
  final static int hide         = command | 79 | expressionCommand;
  final static int font         = command | 80;
  final static int hover        = command | 81 | specialstring;
  final static int vibration    = command | 82;
  final static int vector       = command | 83 | negnums;
  final static int meshRibbon   = command | 84;
  final static int halo         = command | 85;
  final static int rocket       = command | 86;
  final static int geosurface   = command | 87 | embeddedExpression;
  final static int moveto       = command | 88 | negnums | embeddedExpression;
  final static int bondorder    = command | 89;
  final static int console      = command | 90;
  final static int pmesh        = command | 91;
  final static int polyhedra    = command | 92 | embeddedExpression | colorparam;
  final static int centerAt     = command | 93;
  final static int isosurface   = command | 94 | showparam | colorparam | coordOrSet;
  final static int draw         = command | 95 | coordOrSet | showparam | colorparam;
  final static int getproperty  = command | 96;
  final static int dipole       = command | 97 | coordOrSet;
  final static int configuration = command | 98;
  final static int mo           = command | 99 | showparam | colorparam | negnums;
  final static int lcaocartoon  = command | 100| colorparam | embeddedExpression;
  final static int message      = command | 101 | specialstring;
  final static int translateSelected = command | 102 | negnums;
  final static int calculate    = command | 103;
  final static int restore      = command | 104;
  final static int selectionHalo = command | 105 | setparam;
  final static int history       = command | 106 | setparam | showparam;
  final static int display       = command | 107 | setparam | expressionCommand;
  final static int ifcmd         = command | 108;
  final static int elsecmd       = command | 109;
  final static int endifcmd      = command | 110;
  final static int subset        = command | 111 | expressionCommand | predefinedset;
  
  // parameters
  final static int ambient      = setparam |  0;
  final static int axes         = setparam |  1 | command;
  // background
  final static int backfade     = setparam |  2;
  final static int bondmode     = setparam |  3;
  final static int bonds        = setparam |  4 | expression;
  final static int boundbox     = setparam |  5 | showparam | command;
  // cartoon
  final static int cisangle     = setparam |  6;
  final static int fontsize     = setparam |  8;
  final static int fontstroke   = setparam |  9;
  // hbonds
  // hetero
  final static int hourglass    = setparam | 10;
  // hydrogen
  final static int kinemage     = setparam | 11;
  final static int menus        = setparam | 12;
  // monitor
  final static int mouse        = setparam | 13;
  final static int picking      = setparam | 14;
  final static int shadow       = setparam | 15;
  final static int slabmode     = setparam | 16;
  // solvent
  final static int specular     = setparam | 17;
  final static int specpower    = setparam | 18;
  // ssbonds
  // stereo
  // strands
  final static int transparent  = setparam | 19;
  final static int unitcell     = setparam | 20 | expression | predefinedset | showparam | command;
  final static int vectps       = setparam | 21;
  
  // write

  // chime set parameters
  final static int clear        = setparam | 22;
  final static int gaussian     = setparam | 23;
  // load
  final static int mep          = setparam | 24;
  final static int mlp          = setparam | 25 | showparam;
  final static int molsurface   = setparam | 26;
  final static int debugscript  = setparam | 27;
  final static int scale3d      = setparam | 28;
  // jmol extensions
  final static int property     = setparam | 29;
  final static int diffuse      = setparam | 30;
  final static int frank        = setparam | 31 | command;
  final static int partialCharge= setparam | 32;
  final static int pickingStyle = setparam | 33;
  final static int spacegroup   = setparam | 34 | showparam;


  final static int information  = showparam |  0;
  final static int phipsi       = showparam |  1;
  final static int ramprint     = showparam |  2;
  final static int rotation     = showparam |  3;
  final static int group        = showparam |  4 | expression;
  final static int chain        = showparam |  5 | expression;
  final static int atom         = showparam |  6;
  final static int sequence     = showparam |  7 | expression;
  final static int symmetry     = showparam |  8 | expression | predefinedset;
  final static int translation  = showparam |  9;
  // chime show parameters
  final static int residue      = showparam | 10;
  final static int url          = showparam | 11;
  //special positions not yet implemented as show specialpositions
  final static int specialposition = showparam |  12 | expression | predefinedset;
  // mlp
  // list
  // spin
  //except for "symmetry", above are not implemented
  // additional show parameters:
  // selected
  // center 
  // boundbox
  // monitor
  // model
  // zoom
  
  final static int all          = showparam | 11 | expression;
  final static int pdbheader    = showparam | 12 | expression;
  final static int axisangle    = showparam | 13;
  final static int transform    = showparam | 14;
  final static int orientation  = showparam | 15;
  final static int file         = showparam | 16;
  final static int state        = showparam | 17;

  // of the above, only pdbheader, orientation, and file are implemented
  // axisangle is used in the spin command, not the show command
  
  // atom expression operators
  final static int leftparen    = expression |  0;
  final static int rightparen   = expression |  1;
  final static int hyphen       = expression |  2;
  final static int opAnd        = expression |  3;
  final static int opOr         = expression |  4;
  final static int opNot        = expression |  5;
  final static int within       = expression |  6;
  final static int plus         = expression |  7;
  final static int pick         = expression |  8;
  final static int asterisk     = expression |  9;
  final static int dot          = expression | 11;
  final static int leftsquare   = expression | 12;
  final static int rightsquare  = expression | 13;
  final static int colon        = expression | 14;
  final static int slash        = expression | 15;
  final static int substructure = expression | 16;
  final static int leftbrace    = expression | 17;
  final static int rightbrace   = expression | 18;
  final static int dollarsign   = objectid   | 19 | showparam;
  final static int connected    = expression | 20;
  final static int altloc       = expression | 21;
  final static int insertion    = expression | 22;
  final static int opXor        = expression | 23;
  final static int opToggle     = expression | 24;

  

  // miguel 2005 01 01
  // these are used to demark the beginning and end of expressions
  // they do not exist in the source code, but are emitted by the
  // expression compiler
  final static int expressionBegin = expression | 100;
  final static int expressionEnd   = expression | 101;

  final static int atomno       = atomproperty | 0;
  final static int elemno       = atomproperty | 1;
  final static int resno        = atomproperty | 2;
  final static int radius       = atomproperty | 3 | setparam;
  final static int temperature  = atomproperty | 4;
  final static int model        = atomproperty | 5 | showparam | command;
  
  final static int _bondedcount = atomproperty | 6;
  final static int _groupID     = atomproperty | 7;
  final static int _atomID      = atomproperty | 8;
  final static int _structure   = atomproperty | 9;
  final static int occupancy    = atomproperty | 10;
  
  final static int polymerLength= atomproperty | 11;
  final static int molecule     = atomproperty | 12 | command;
  final static int cell         = atomproperty | 13;
  final static int site         = atomproperty | 14;
  final static int element      = atomproperty | 15;
  final static int symop        = atomproperty | 16;
  final static int surfacedistance = atomproperty | 17;
  final static int atomIndex    = atomproperty | 18;
  final static int formalCharge = atomproperty | 19 | setparam ;
  final static int phi          = atomproperty | 20;
  final static int psi          = atomproperty | 21;

  final static int opGT         = comparator |  0;
  final static int opGE         = comparator |  1;
  final static int opLE         = comparator |  2;
  final static int opLT         = comparator |  3;
  final static int opEQ         = comparator |  4;
  final static int opNE         = comparator |  5;

  // misc
  final static int off          = bool |  0;
  final static int on           = bool |  1;

  final static int dash         = misc |  0; //backbone
  final static int user         = misc |  1; //cpk & star
//  final static int x            = misc |  2 | expression;
//  final static int y            = misc | 3 | expression | predefinedset;
//  final static int z            = misc |  4 | expression;
  final static int none         = misc |  5 | expression;
  final static int normal       = misc |  7;
  final static int rasmol       = misc |  8;
  final static int insight      = misc |  9;
  final static int quanta       = misc | 10;
  final static int ident        = misc | 11;
  final static int distance     = misc | 12;
  final static int angle        = misc | 13;
  final static int torsion      = misc | 14;
  final static int coord        = misc | 15;
  final static int shapely      = misc | 18;
//  final static int restore      = misc | 19; // chime extended
  final static int colorRGB     = misc | 20 | colorparam;
  final static int spec_resid           = misc | 21;
  final static int spec_name_pattern    = misc | 22;
  final static int spec_seqcode         = misc | 23;
  final static int spec_seqcode_range   = misc | 24;
  final static int spec_chain           = misc | 25;
  final static int spec_alternate       = misc | 26;
  final static int spec_model           = misc | 27;
  final static int spec_atom            = misc | 28;
  final static int percent      = misc | expression | 29;
  final static int dotted       = misc | 30;
  final static int mode         = misc | 31;
  final static int direction    = misc | 32;
//  final static int fps          = misc | 33;
  final static int displacement = misc | 34;
  final static int type         = misc | 35;
  final static int fixedtemp    = misc | 36;
  final static int rubberband   = misc | 37;
  final static int monomer      = misc | 38;
  final static int defaultColors= misc | 39 | setparam;
  final static int opaque       = misc | 40;
  final static int translucent  = misc | 41;
  final static int delete       = misc | 42;
  final static int solid        = misc | 45;
  final static int jmol         = misc | 46;
  final static int absolute     = misc | 47;
  final static int average      = misc | 48;
  final static int nodots       = misc | 49;
  final static int mesh         = misc | 50;
  final static int nomesh       = misc | 51;
  final static int fill         = misc | 52;
  final static int nofill       = misc | 53;
  final static int vanderwaals  = misc | 54;
  final static int ionic        = misc | 55;
  final static int resume       = misc | 56;
  final static int play         = misc | 57;
  final static int next         = misc | 58;
  final static int prev         = misc | 59;
  final static int rewind       = misc | 60;
  final static int playrev      = misc | 61;
  final static int range        = misc | 62;
  final static int point3f      = misc | 63;
  final static int sasurface    = misc | 64;
  final static int left         = misc | 65;
  final static int right        = misc | 66;
  final static int front        = misc | 67;
  final static int back         = misc | 68;
  final static int top          = misc | 69;
  final static int bottom       = misc | 70;
  final static int bitset       = misc | 71;
  
 
  final static int amino       = predefinedset |  0;
  final static int hetero      = predefinedset |  1 | setparam;
  final static int hydrogen    = predefinedset |  2 | setparam;
  final static int selected    = predefinedset |  3 | showparam;
  final static int solvent     = predefinedset |  4 | setparam;
  final static int sidechain   = predefinedset |  5;
  final static int protein     = predefinedset |  6;
  final static int nucleic     = predefinedset |  7;
  final static int dna         = predefinedset |  8;
  final static int rna         = predefinedset |  9;
  final static int purine      = predefinedset | 10;
  final static int pyrimidine  = predefinedset | 11;
  final static int surface     = predefinedset | 12;
  final static int visible     = predefinedset | 13;
  final static int clickable   = predefinedset | 14;
  final static int carbohydrate = predefinedset | 15;
  final static int hidden      = predefinedset | 16;
  final static int displayed   = predefinedset | 17;
   

  final static Token tokenOn  = new Token(on, 1, "on");
  final static Token tokenAll = new Token(all, "all");
  final static Token tokenAnd = new Token(opAnd, "and");
  final static Token tokenExpressionBegin =
    new Token(expressionBegin, "expressionBegin");
  final static Token tokenExpressionEnd =
    new Token(expressionEnd, "expressionEnd");
  

  /*
    Note that the Jmol scripting language is case-insensitive.
    So, the compiler turns all identifiers to lower-case before
    looking up in the hash table. 
    Therefore, the left column of this array *must* be lower-case
  */

  final static Object[] arrayPairs  = {
    // commands
    "backbone",          new Token(backbone,  onDefault1, "backbone"),
    "background",      new Token(background, varArgCount, "background"),
    "bond",              new Token(bond,     varArgCount, "bond"),
    "cartoon",           new Token(cartoon,   onDefault1, "cartoon"),
    "cartoons",          null,
    "center",            new Token(center,   varArgCount, "center"),
    "centre",            null,
    "clipboard",         new Token(clipboard,          0, "clipboard"),
    "color",             new Token(color,    varArgCount, "color"),
    "colour",            null,
    "connect",           new Token(connect,  varArgCount, "connect"),
    "data",              new Token(data,     varArgCount, "data"),
    "define",            new Token(define,   varArgCount, "define"),
    "@",                 null,
    "dots",              new Token(dots,     varArgCount, "dots"),
    "echo",              new Token(echo,     varArgCount, "echo"),
    "exit",              new Token(exit,               0, "exit"),
    "hbond",             new Token(hbond,     onDefault1, "hbond"),
    "hbonds",            null,
    "help",              new Token(help,     varArgCount, "help"),
    "label",             new Token(label,     onDefault1, "label"),
    "labels",            null,
    "load",              new Token(load,     varArgCount, "load"),
    "molecule",          new Token(molecule, "molecule"),
    "molecules",         null,
    "altloc",            new Token(altloc, "altloc"),
    "altlocs",           null,
    "insertion",         new Token(insertion, "insertion"),
    "insertions",        null,
    "monitor",           new Token(monitor,  varArgCount, "measure"),
    "monitors",          null,
    "measure",           null,
    "measures",          null,
    "measurement",       null,
    "measurements",      null,
    "pause",             new Token(pause,              0, "pause"),
    "wait",              null,
    "print",             new Token(print,              0, "print"),
    "quit",              new Token(quit,               0, "quit"),
    "refresh",           new Token(refresh,            0, "refresh"),
    "renumber",          new Token(renumber,  onDefault1, "renumber"),
    "reset",             new Token(reset,              0, "reset"),
    "restore",           new Token(restore,  varArgCount, "restore"),
    "restrict",          new Token(restrict, varArgCount, "restrict"),
    "hide",              new Token(hide,     varArgCount, "hide"),
    "ribbon",            new Token(ribbon,    onDefault1, "ribbon"),
    "ribbons",           null,
    "rotate",            new Token(rotate,   varArgCount, "rotate"),
    "save",              new Token(save,     varArgCount, "save"),
    "script",            new Token(script,             1, "script"),
    "source",            null,
    "select",            new Token(select,   varArgCount, "select"),
    "set",               new Token(set,      varArgCount, "set"),
    "show",              new Token(show,     varArgCount, "show"),
    "slab",              new Token(slab,      onDefault1, "slab"),
    "cpk",               new Token(cpk,      varArgCount, "spacefill"),
    "spacefill",         null,
    "ssbond",            new Token(ssbond,    onDefault1, "ssbond"),
    "ssbonds",           null,
    "stereo",            new Token(stereo,   varArgCount, "stereo"),
    "strand",            new Token(strands,   onDefault1, "strand"),
    "strands",           null,
    "structure",         new Token(structure,          0, "structure"),
    "trace",             new Token(trace,     onDefault1, "trace"),
    "translate",         new Token(translate,varArgCount, "translate"),
    "unbond",            new Token(unbond,   varArgCount, "unbond"),
    "wireframe",         new Token(wireframe, onDefault1, "wireframe"),
    "write",             new Token(write,    varArgCount, "write"),
    "zap",               new Token(zap,                0, "zap"),
    "zoom",              new Token(zoom,     varArgCount, "zoom"),
    "zoomto",            new Token(zoomTo,   varArgCount, "zoomTo"),
    "initialize",        new Token(initialize,         0, "initialize"),
    // openrasmol commands
    "depth",             new Token(depth,              1, "depth"),
    "star",              new Token(star,     varArgCount, "star"),
    "stars",             null,
    // chime commands
    "delay",             new Token(delay,     onDefault1, "delay"),
    "loop",              new Token(loop,      onDefault1, "loop"),
    "move",              new Token(move,     varArgCount, "move"),
    "view",              new Token(view,     varArgCount, "view"),
    "spin",              new Token(spin,     varArgCount, "spin"),
    "list",              new Token(list,     varArgCount, "list"),
    "display3d",         new Token(display3d,  "display3d"),
    "animation",         new Token(animation,  "animation"),
    "anim",              null,
    "frame",             new Token(frame,      "frame"),
    // jmol commands
    "centerat",          new Token(centerAt, varArgCount, "centerat"),
    "font",              new Token(font,       "font"),
    "hover",             new Token(hover,      "hover"),
    "vibration",         new Token(vibration,  "vibration"),
    "vector",            new Token(vector,   varArgCount, "vector"),
    "vectors",           null,
    "meshribbon",        new Token(meshRibbon,onDefault1, "meshribbon"),
    "meshribbons",       null,
    "halo",              new Token(halo,     varArgCount, "halo"),
    "halos",             null,
    "rocket",            new Token(rocket,    onDefault1, "rocket"),
    "rockets",           null,
    "moveto",            new Token(moveto,   varArgCount, "moveto"),
    "bondorder",         new Token(bondorder,          1, "bondorder"),
    "console",           new Token(console,   onDefault1, "console"),
    "pmesh",             new Token(pmesh,    varArgCount, "pmesh"),
    "draw",              new Token(draw,     varArgCount, "draw"),
    "dipole",            new Token(dipole,   varArgCount, "dipole"),
    "dipoles",           null,
    "polyhedra",         new Token(polyhedra,varArgCount, "polyhedra"),
    "mo",                new Token(mo,       varArgCount, "mo"),
    "isosurface",        new Token(isosurface,varArgCount,"isosurface"),
    "geosurface",        new Token(geosurface,varArgCount, "geosurface"),
    "getproperty",       new Token(getproperty,varArgCount, "getproperty"),
    "configuration",     new Token(configuration,varArgCount, "configuration"),
    "config",            null,
    "conformation",      null,
    "lcaocartoon",       new Token(lcaocartoon,varArgCount, "lcaocartoon"),
    "lcaocartoons",      null,
    "message",           new Token(message,     varArgCount, "message"),
    "if",                new Token(ifcmd,       varArgCount, "if"),
    "else",              new Token(elsecmd,               0, "else"),
    "endif",             new Token(endifcmd,              0, "endif"),
    "translateselected", new Token(translateSelected,varArgCount, "translateSelected"),
    "calculate",         new Token(calculate,varArgCount, "calculate"),
    "selectionhalo",     new Token(selectionHalo,     onDefault1, "selectionHalos"),
    "selectionhalos",    null,
    "history",           new Token(history,     varArgCount, "history"),
    "subset",            new Token(subset,      varArgCount, "subset"),


    // setparams
    "ambient",      new Token(ambient,         "ambient"),
    "axes",         new Token(axes, varArgCount,    "axes"),
    "backfade",     new Token(backfade,        "backfade"),
    "bondmode",     new Token(bondmode,        "bondmode"),
    "bonds",        new Token(bonds,           "bonds"),
    "boundbox",     new Token(boundbox, onDefault1, "boundbox"),
    "cisangle",     new Token(cisangle,        "cisangle"),
    "display",      new Token(display,         "display"),
    "fontsize",     new Token(fontsize,        "fontsize"),
    "fontstroke",   new Token(fontstroke,      "fontstroke"),
    // hetero
    "hourglass",    new Token(hourglass,       "hourglass"),
    // hydrogen
    "kinemage",     new Token(kinemage,        "kinemage"),
    "menus",        new Token(menus,           "menus"),
    "mouse",        new Token(mouse,           "mouse"),
    "picking",      new Token(picking,         "picking"),
    "pickingstyle", new Token(pickingStyle,    "pickingStyle"),
    "radius",       new Token(radius,          "radius"),
    "shadow",       new Token(shadow,          "shadow"),
    "slabmode",     new Token(slabmode,        "slabmode"),
    // solvent
    "specular",     new Token(specular,        "specular"),
    "specpower",    new Token(specpower,       "specpower"),
    "transparent",  new Token(transparent,     "transparent"),
    "unitcell",     new Token(unitcell, onDefault1, "unitcell"),
    "cell",         new Token(cell,            "cell"),
    "vectps",       new Token(vectps,          "vectps"),
    // chime setparams
    "clear",        new Token(clear,           "clear"),
    "gaussian",     new Token(gaussian,        "gaussian"),
    "mep",          new Token(mep,             "mep"),
    "mlp",          new Token(mlp,             "mlp"),
    "molsurface",   new Token(molsurface,      "molsurface"),
    "debugscript",  new Token(debugscript,     "debugscript"),
//    "fps",          new Token(fps,             "fps"),
    "scale3d",      new Token(scale3d,         "scale3d"),

    // jmol extensions
    "property",     new Token(property,        "property"),
    "diffuse",      new Token(diffuse,         "diffuse"),
    "frank",        new Token(frank, onDefault1, "frank"),
    // must be lower case - see comment above
    "formalcharge", new Token(formalCharge,    "formalcharge"),
    "charge",       null,
    "partialcharge",new Token(partialCharge,   "partialcharge"),
    "phi",          new Token(phi,             "phi"),
    "psi",          new Token(psi,             "psi"),
  
    // show parameters
    "information",  new Token(information,     "information"),
    "info",         null,
    "phipsi",       new Token(phipsi,          "phipsi"),
    "ramprint",     new Token(ramprint,        "ramprint"),
    "rotation",     new Token(rotation,        "rotation"),
    "group",        new Token(group,           "group"),
    "chain",        new Token(chain,           "chain"),
    "atom",         new Token(atom,            "atom"),
    "atoms",        null,
    "sequence",     new Token(sequence,        "sequence"),
    "specialposition", new Token(specialposition, "specialPosition"),
    "symmetry",     new Token(symmetry,        "symmetry"),
    "spacegroup",   new Token(spacegroup,      "spacegroup"),
    "translation",  new Token(translation,     "translation"),
    // chime show parameters
    "residue",      new Token(residue,         "residue"),
    "model",        new Token(model,           "model"),
    "models",       null,
    "pdbheader",    new Token(pdbheader,       "pdbheader"),

    "axisangle",    new Token(axisangle,       "axisangle"),
    "transform",    new Token(transform,       "transform"),
    "orientation",  new Token(orientation,     "orientation"),
    "file",         new Token(file,            "file"),
    "state",        new Token(state,           "state"),
    "url",          new Token(url,             "url"),

    // atom expressions
    "(",            new Token(leftparen, "("),
    ")",            new Token(rightparen, ")"),
    "-",            new Token(hyphen, "-"),
    "and",          tokenAnd,
    "&",            null,
    "&&",           null,
    "or",           new Token(opOr, "or"),
    ",",            null,
    "|",            null,
    "||",            null,
    "not",          new Token(opNot, "not"),
    "!",            null,
    "xor",          new Token(opXor, "xor"),
//no-- don't do this; it interferes with define
//  "~",            null,
    "tog",          new Token(opToggle, "tog"),
    ",|",           null,
    "<",            new Token(opLT, "<"),
    "<=",           new Token(opLE, "<="),
    ">=",           new Token(opGE, ">="),
    ">",            new Token(opGT, ">="),
    "==",           new Token(opEQ, "=="),
    "=",            null,
    "!=",           new Token(opNE, "!="),
    "<>",           null,
    "/=",           null,
    "within",       new Token(within, "within"),
    "+",            new Token(plus, "+"),
    "pick",         new Token(pick, "pick"),
    ".",            new Token(dot, "."),
    "[",            new Token(leftsquare,  "["),
    "]",            new Token(rightsquare, "]"),
    "{",            new Token(leftbrace,  "{"),
    "}",            new Token(rightbrace, "}"),
    "$",            new Token(dollarsign, "$"),
    ":",            new Token(colon, ":"),
    "/",            new Token(slash, "/"),
    "substructure", new Token(substructure, "substructure"),
    "connected",    new Token(connected, "connected"),
    "atomindex",    new Token(atomIndex, "atomIndex"),
    "atomno",       new Token(atomno, "atomno"),
    "elemno",       new Token(elemno, "elemno"),
    "_e",           null,
    "element",      new Token(element, "element"),
    "resno",        new Token(resno, "resno"),
    "temperature",  new Token(temperature, "temperature"),
    "relativetemperature",  null,
    "_bondedcount", new Token(_bondedcount, "_bondedcount"),
    "_groupID",     new Token(_groupID, "_groupID"),
    "_g",           null,
    "_atomID",      new Token(_atomID, "_atomID"),
    "_a",           null,
    "_structure",   new Token(_structure, "_structure"),
    "occupancy",    new Token(occupancy, "occupancy"),
    "polymerlength",new Token(polymerLength, "polymerlength"),
    "site",         new Token(site, "site"),
    "symop",        new Token(symop, "symop"),
    "off",          new Token(off, 0, "off"),
    "false",        null,
    "on",           tokenOn,
    "true",         null,

    "dash",         new Token(dash, "dash"),
    "user",         new Token(user, "user"),
//    "x",            new Token(x, "x"),
//    "y",            new Token(y, "y"),
//    "z",            new Token(z, "z"),
    "*",            new Token(asterisk, "*"),
    "all",          tokenAll,
    "none",         new Token(none, "none"),
    "null",         null,
    "inherit",      null,
    "normal",       new Token(normal, "normal"),
    "rasmol",       new Token(rasmol, "rasmol"),
    "insight",      new Token(insight, "insight"),
    "quanta",       new Token(quanta, "quanta"),
    "ident",        new Token(ident, "ident"),
    "distance",     new Token(distance, "distance"),
    "angle",        new Token(angle, "angle"),
    "torsion",      new Token(torsion, "torsion"),
    "coord",        new Token(coord, "coord"),
    "shapely",      new Token(shapely,         "shapely"),

//    "restore",           new Token(restore,    "restore"),
  
    "amino",        new Token(amino,           "amino"),
    "hetero",       new Token(hetero,          "hetero"),
    "hydrogen",     new Token(hydrogen,        "hydrogen"),
    "hydrogens",    null,
    "selected",     new Token(selected,        "selected"),
    "hidden",       new Token(hidden,          "hidden"),
    "displayed",    new Token(displayed,       "displayed"),
    "solvent",      new Token(solvent,         "solvent"),
    "%",            new Token(percent,         "%"),
    "dotted",       new Token(dotted,          "dotted"),
    "sidechain",    new Token(sidechain,       "sidechain"),
    "protein",      new Token(protein,         "protein"),
    "carbohydrate", new Token(carbohydrate,    "carbohydrate"),
    "nucleic",      new Token(nucleic,         "nucleic"),
    "dna",          new Token(dna,             "dna"),
    "rna",          new Token(rna,             "rna"),
    "purine",       new Token(purine,          "purine"),
    "pyrimidine",   new Token(pyrimidine,      "pyrimidine"),
    "surface",      new Token(surface,         "surface"),
    "surfacedistance", new Token(surfacedistance, "surfacedistance"),
    "visible",      new Token(visible,         "visible"),
    "clickable",    new Token(clickable,       "clickable"),
    "mode",         new Token(mode,            "mode"),
    "direction",    new Token(direction,       "direction"),
    "jmol",         new Token(jmol,            "jmol"),
    "displacement", new Token(displacement,    "displacement"),
    "type",         new Token(type,            "type"),
    "fixedtemperature", new Token(fixedtemp,   "fixedtemperature"),
    "rubberband",   new Token(rubberband,      "rubberband"),
    "monomer",      new Token(monomer,         "monomer"),
    "defaultcolors",new Token(defaultColors,   "defaultColors"),
    "opaque",       new Token(opaque,          "opaque"),
    "translucent",  new Token(translucent,     "translucent"),
    "delete",       new Token(delete,          "delete"),
    "solid",        new Token(solid,           "solid"),
    "absolute",     new Token(absolute,        "absolute"),
    "average",      new Token(average,         "average"),
    "nodots",       new Token(nodots,          "nodots"),
    "mesh",         new Token(mesh,            "mesh"),
    "nomesh",       new Token(nomesh,          "nomesh"),
    "fill",         new Token(fill,            "fill"),
    "nofill",       new Token(nofill,          "nofill"),
    "vanderwaals",  new Token(vanderwaals,     "vanderwaals"),
    "vdw",          null,
    "ionic",        new Token(ionic,           "ionic"),
    "resume",       new Token(resume,          "resume"),
    "next",         new Token(next,            "next"),
    "prev",         new Token(prev,            "previous"),
    "previous",     null,
    "rewind",       new Token(rewind,          "rewind"),
    "playrev",      new Token(playrev,         "playrev"),
    "play",         new Token(play,            "play"),
    "range",        new Token(range,           "range"),
    "point3f",      new Token(point3f,         "point3f"),
    "sasurface",    new Token(sasurface,       "sasurface"),
    "top",          new Token(top,             "top"),    
    "bottom",       new Token(bottom,          "bottom"),    
    "left",         new Token(left,            "left"),    
    "right",        new Token(right,           "right"),    
    "front",        new Token(front,           "front"),    
    "back",         new Token(back,            "back"),    
  };

  static Hashtable map = new Hashtable();
  static {
    Token tokenLast = null;
    String stringThis;
    Token tokenThis;
    for (int i = 0; i + 1 < arrayPairs.length; i += 2) {
      stringThis = (String) arrayPairs[i];
      tokenThis = (Token) arrayPairs[i + 1];
      if (tokenThis == null)
        tokenThis = tokenLast;
      if (map.get(stringThis) != null)
        Logger.error("duplicate token definition:" + stringThis);
      map.put(stringThis, tokenThis);
      tokenLast = tokenThis;
    }
  }

  public String toString() {
    return "Token[" + astrType[tok<=keyword ? tok : keyword] +
      "-" + tok +
      ((intValue == Integer.MAX_VALUE) ? "" : ":" + intValue) +
      ((value == null) ? "" : ":" + value) + "]";
  }
}
