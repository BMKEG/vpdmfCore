package edu.isi.bmkeg.vpdmf.bin;

import edu.isi.bmkeg.utils.mvnRunner.LocalMavenInstall;

public class RemoveEmbeddedMaven {

	public static String USAGE = "no arguments";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		LocalMavenInstall.removeLocalMaven();

	}

}

