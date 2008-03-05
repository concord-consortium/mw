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

package org.concord.modeler;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * @author Qian Xie
 */

public class Person implements Serializable {

	private String userID;
	private String emailAddress;
	private String password;
	private String firstName;
	private String lastName;
	private String institution;
	private String state;
	private String country;
	private String teacher;
	private String[] collaborators;

	/** The default user name is <tt>Guest</tt>. */
	public Person() {
	}

	public boolean isEmpty() {
		if (userID != null && !userID.trim().equals(""))
			return false;
		if (emailAddress != null && !emailAddress.trim().equals(""))
			return false;
		return true;
	}

	public boolean hasTeacher() {
		return teacher != null && !"none".equalsIgnoreCase(teacher);
	}

	public void setFirstName(String s) {
		firstName = s;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setLastName(String s) {
		lastName = s;
	}

	public String getLastName() {
		return lastName;
	}

	public void setInstitution(String s) {
		institution = s;
	}

	public String getInstitution() {
		return institution;
	}

	public void setState(String s) {
		state = s;
	}

	public String getState() {
		return state;
	}

	public void setCountry(String s) {
		country = s;
	}

	public String getCountry() {
		return country;
	}

	public void setEmailAddress(String s) {
		emailAddress = s;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setUserID(String s) {
		userID = s;
	}

	public String getUserID() {
		return userID;
	}

	public void setPassword(String s) {
		password = s;
	}

	public String getPassword() {
		return password;
	}

	public void setTeacher(String teacher) {
		this.teacher = teacher;
	}

	public String getTeacher() {
		return teacher;
	}

	public void setCollaboratorIdArray(String[] s) {
		collaborators = s;
	}

	public String[] getCollaboratorIdArray() {
		return collaborators;
	}

	public String getCollaboratorIdString() {
		if (collaborators == null)
			return null;
		String s = "";
		for (String t : collaborators) {
			s += t + ",";
		}
		if (s.endsWith(","))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	public String getFullName() {
		String s = "";
		if (firstName != null)
			s += firstName + " ";
		if (lastName != null)
			s += lastName;
		return s;
	}

	public void setFullName(String s) {
		StringTokenizer st = new StringTokenizer(s);
		switch (st.countTokens()) {
		case 2:
			setFirstName(st.nextToken());
			setLastName(st.nextToken());
			break;
		case 3:
			setFirstName(st.nextToken());
			st.nextToken();
			setLastName(st.nextToken());
			break;
		default:
			setFirstName(null);
			setLastName(s);
		}
	}

	/**
	 * If two persons' user IDs are identical, they will be considered as the same person.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Person))
			return false;
		Person p = (Person) o;
		if (userID != null && !userID.equalsIgnoreCase(p.userID))
			return false;
		if (userID == null && p.userID != null)
			return false;
		return true;
	}

	public int hashCode() {
		if (userID != null)
			return userID.hashCode();
		return super.hashCode();
	}

	public String toString() {
		return (userID == null ? "" : userID + ", ") + (firstName == null ? "" : firstName + " ")
				+ (lastName == null ? "" : lastName) + (institution == null ? "" : ", " + institution)
				+ (state == null ? "" : ", " + state) + (country == null ? "" : ", " + country)
				+ (emailAddress == null ? "" : ", " + emailAddress);
	}

}
