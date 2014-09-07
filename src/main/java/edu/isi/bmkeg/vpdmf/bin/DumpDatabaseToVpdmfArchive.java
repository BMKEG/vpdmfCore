package edu.isi.bmkeg.vpdmf.bin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

public class DumpDatabaseToVpdmfArchive {

	public static String USAGE = "arguments: <old-archive-file> <dbname> <login> <password> <new-archive-file> ";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 5) {
			System.err.println(args.length + " " + USAGE);
			System.exit(-1);
		}

		File inFile = new File(args[0]);

		if (!inFile.exists()) {
			System.err.println("Can't find " + args[0]);
			System.exit(-1);
		}

		String dbName = args[1];
		int l = dbName.lastIndexOf("/");
		if (l != -1)
			dbName = dbName.substring(l + 1, dbName.length());

		String login = args[2];
		String password = args[3];

		File outFile = new File(args[4]);
		
		VPDMfKnowledgeBaseBuilder builder = new VPDMfKnowledgeBaseBuilder(
				inFile, login, password, dbName);

		builder.refreshDataToNewArchive(outFile);
		
	}


}
