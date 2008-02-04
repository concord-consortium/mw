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

package org.concord.jmol;

/** This class provides random scripts that can be used in conjunction with the Molecular Workbench to make it
 ** a screensaver.
 **
 ** @author Charles Xie
 **/

class Screensaver {

    private Screensaver(){}

    private final static String[] COMMANDS=new String[]
    {
	"cartoon on;",
	"cartoon off;",
	"color atoms none;",
	"color bonds none;",
	"color chain;",
	"color structure;",
	"hbonds on;",
	"hbonds off;",
	"meshribbon on;",
	"meshribbon off;",
	"ribbon on;",
	"ribbon off;",
	"rocket on;",
	"rocket off;",
	"set axes 0.2;",
	"set axes 0;",
	"set boundbox 0.1;",
	"set boundbox 0;",
	"set solvent on;",
	"set solvent off;",
	"ssbonds on;",
	"ssbonds off;",
	"strands on;",
	"strands off;",
	"trace on;",
	"trace off;",
	"wireframe 0.15;",
	"wireframe off;"
    };

    static String getRandomScript(String delay){

	StringBuffer sb=new StringBuffer("spin on;cpk 20%;wireframe 0.15;dots off;");

	int i=0;
	int n=COMMANDS.length;

	while(i<n){
	    sb.append(COMMANDS[(int)(Math.random()*n)]);
	    sb.append("delay "+delay+";");
	    if(i!=0) {
		if(i%7==0) {
		    sb.append("color atoms ["
			      +(short)(Math.random()*255)+","+
			      +(short)(Math.random()*255)+","+
			      +(short)(Math.random()*255)+"];");
		}
		else if(i%8==0) {
		    sb.append("color bonds ["
			      +(short)(Math.random()*255)+","+
			      +(short)(Math.random()*255)+","+
			      +(short)(Math.random()*255)+"];");
		}
	    }
	    i++;
	}
	
	sb.append("loop "+delay+";");
	
	return sb.toString();

    }

}
