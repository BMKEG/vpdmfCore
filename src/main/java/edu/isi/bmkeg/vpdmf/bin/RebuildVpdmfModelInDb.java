package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.VPDMfArchiveFileBuilder;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

public class RebuildVpdmfModelInDb {

	public static String USAGE = "arguments: <archive-file> <dbname> <login> <password>"; 

	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

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
						
			builder.buildVpdmfModelInDatabaseFromArchive();

			System.out.println("Build Complete: " + buildFile.getPath());			
		
		} catch (Exception e) {
			
			e.printStackTrace();
			
			System.err.println("Build Failed: " + buildFile.getPath());
		
		}

	}
	

}
