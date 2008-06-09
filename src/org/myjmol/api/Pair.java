package org.myjmol.api;

public class Pair {

	private int i;
	private int j;

	public Pair(int i, int j) {
		setIndices(i, j);
	}

	public void setIndices(int i, int j) {
		this.i = i;
		this.j = j;
	}

	public int getIndex1() {
		return i;
	}

	public int getIndex2() {
		return j;
	}

}