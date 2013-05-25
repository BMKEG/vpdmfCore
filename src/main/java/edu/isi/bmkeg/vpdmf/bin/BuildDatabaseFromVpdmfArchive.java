package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

public class BuildDatabaseFromVpdmfArchive {

	public static String USAGE = "arguments: <archive-file> <dbname> <login> <password>"; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
				
		Map<String, File> cleanup;
		
		if( args.length != 4 ) {
			System.err.println(USAGE);
			System.exit(-1);
		}
		
		File buildFile = new File(args[0]);
		
		if( !buildFile.exists() ) {
			System.err.println("Can't find " + args[0]);
			System.exit(-1);		
		}
		
		String dbName = args[1];
		int l = dbName.lastIndexOf("/");
		if( l != -1 )
			dbName = dbName.substring(l+1, dbName.length());
		
		String login = args[2];
		String password =  args[3];
		
		System.out.println("VPDMf Archive: " + buildFile.getPath());
		System.out.println("Database: " + dbName);
		System.out.println("Login: " + login);
		
		try {
			
			VPDMfKnowledgeBaseBuilder builder = new VPDMfKnowledgeBaseBuilder(buildFile, login, password, dbName);
			
			// Gully: Make sure that this runs, avoid silly issues.
			try {
					builder.destroyDatabase(dbName);
			} catch (SQLException sqlE) {		
				
				if( !sqlE.getMessage().contains("database doesn't exist") ) {
					sqlE.printStackTrace();
				}
				
			} 
			
			builder.buildDatabaseFromArchive();

			System.out.println("Build Complete: " + buildFile.getPath());			
		
		} catch (Exception e) {
			
			e.printStackTrace();
			
			System.err.println("Build Failed: " + buildFile.getPath());
		
		}
		
	}

}
