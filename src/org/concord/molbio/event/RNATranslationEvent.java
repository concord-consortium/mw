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

package org.concord.molbio.event;

import java.util.EventObject;
import java.awt.Point;
import java.awt.Rectangle;
import org.concord.molbio.engine.Aminoacid;
import org.concord.molbio.engine.Codon;

public class RNATranslationEvent extends EventObject {

	private Point where;
	private Rectangle ribosomeRect;
	private int mode = RNATranslationListener.MODE_UNKNOWN;
	private Aminoacid aminoacid;
	private boolean consumed;
	private Codon codon;

	public RNATranslationEvent(Object src, Aminoacid aminoacid, Codon codon, Point where, Rectangle ribosomeRect,
			int mode) {
		super(src);
		this.where = where;
		this.ribosomeRect = ribosomeRect;
		this.mode = mode;
		this.aminoacid = aminoacid;
		this.codon = codon;
		consumed = false;
	}

	public Point getWhere() {
		return where;
	}

	public Rectangle getRibosomeRect() {
		return ribosomeRect;
	}

	public int getMode() {
		return mode;
	}

	public Aminoacid getAminoacid() {
		return aminoacid;
	}

	public Codon getCodon() {
		return codon;
	}

	public void setRibosomeRect(Rectangle ribosomeRect) {
		this.ribosomeRect = ribosomeRect;
	}

	public void setWhere(Point where) {
		this.where = where;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setAminoacid(Aminoacid aminoacid) {
		this.aminoacid = aminoacid;
	}

	public void setCodon(Codon codon) {
		this.codon = codon;
	}

	public boolean isConsumed() {
		return consumed;
	}

	public void setConsumed(boolean consumed) {
		this.consumed = consumed;
	}

}