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

package org.concord.modeler.event;

import java.util.EventListener;

import javax.swing.event.HyperlinkEvent;

/**
 * This is to replace Swing's HyperlinkListener, which does not distinguish different mouse button clicks. By using this
 * listener, right-clicking a hyperlink will prompt a pop-up menu, instead of jumping to the linked target.
 * 
 * @author Charles Xie
 */

public interface HotlinkListener extends EventListener {

	public void hotlinkUpdate(HyperlinkEvent e);

}
