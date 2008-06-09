/* $RCSfile: MouseManager14.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:12 $
 * $Revision: 1.11 $
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


import java.awt.Component;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

class MouseManager14 extends MouseManager11
  implements MouseWheelListener {

  MouseManager14(Component display, Viewer viewer) {
    super(display, viewer);
    //Logger.debug("MouseManager14 implemented");
    display.addMouseWheelListener(this);
  }

  void removeMouseListeners14() {
    viewer.getAwtComponent().removeMouseWheelListener(this);
  }

 public void mouseWheelMoved(MouseWheelEvent e) {
    mouseWheel(e.getWhen(), e.getWheelRotation(), e.getModifiers());
  }
}
