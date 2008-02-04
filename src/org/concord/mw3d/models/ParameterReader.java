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

package org.concord.mw3d.models;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

final class ParameterReader extends ColumnDataParser {

	public void read(URL url, Map<String, float[]> map) throws Exception {

		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

		String line;
		String element;
		float[] param;

		try {
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("//"))
					continue;
				element = parseToken(line);
				param = new float[4];
				param[0] = parseFloat(line, ichNextParse);
				param[1] = parseFloat(line, ichNextParse);
				param[2] = parseFloat(line, ichNextParse);
				param[3] = parseFloat(line, ichNextParse);
				map.put(element, param);
			}
		}
		catch (Exception e) {
			throw new Exception(e.getMessage());
		}
		finally {
			reader.close();
		}

	}

}
