/* $RCSfile: DefaultLogger.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:17 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.myjmol.util;

import java.io.PrintStream;

/**
 * Default implementation of the logger.
 */
public class DefaultLogger implements LoggerInterface {

	// XIE
	private static boolean silent;
	
	//XIE
	public void setSilent(boolean b) {
		silent = b;
	}
	
  /**
   * Method to output a log.
   * 
   * @param out Output stream.
   * @param level Log level.
   * @param txt Text to log.
   * @param e Exception.
   */
  public static void log(PrintStream out, int level, String txt, Throwable e) {
	  if(silent) return; // XIE
    if ((out != null) && ((txt != null) || (e != null))) {
      txt = (txt != null ? txt : "");
      out.println(
          (Logger.logLevel() ? "[" + Logger.getLevel(level) + "] " : "") +
          txt +
          (e != null ? ": " + e.getMessage() : ""));
      if (e != null) {
        StackTraceElement[] elements = e.getStackTrace();
        if (elements != null) {
          for (int i = 0; i < elements.length; i++) {
            out.println(
                elements[i].getClassName() + " - " +
                elements[i].getLineNumber() + " - " +
                elements[i].getMethodName());
          }
        }
      }
    }
  }

  public void debug(String txt) {
    log(System.out, Logger.LEVEL_DEBUG, txt, null);
  }

  public void info(String txt) {
    log(System.out, Logger.LEVEL_INFO, txt, null);
  }

  public void warn(String txt) {
    log(System.out, Logger.LEVEL_WARN, txt, null);
  }

  public void warn(String txt, Throwable e) {
    log(System.out, Logger.LEVEL_WARN, txt, e);
  }

  public void error(String txt) {
    log(System.err, Logger.LEVEL_ERROR, txt, null);
  }

  public void error(String txt, Throwable e) {
    log(System.err, Logger.LEVEL_ERROR, txt, e);
  }

  public void fatal(String txt) {
    log(System.err, Logger.LEVEL_FATAL, txt, null);
  }

  public void fatal(String txt, Throwable e) {
    log(System.err, Logger.LEVEL_FATAL, txt, e);
  }
}
