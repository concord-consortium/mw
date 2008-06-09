/* $RCSfile: Point3fi.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:18 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.myjmol.vecmath;
import javax.vecmath.Point3f;

public class Point3fi extends Point3f {
  public int screenX;
  public int screenY;
  public int screenZ;

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof Point3fi)) {
      return false;
    }
    Point3fi other = (Point3fi) obj;
    if ((screenX != other.screenX) ||
        (screenY != other.screenY) ||
        (screenZ != other.screenZ)) {
      return false;
    }
    return super.equals(other);
  }
}
