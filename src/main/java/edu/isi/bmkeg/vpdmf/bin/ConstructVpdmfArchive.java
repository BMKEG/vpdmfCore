package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.mvnRunner.LocalMavenInstall;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.VPDMfArchiveFileBuilder;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

public class ConstructVpdmfArchive {

	public static String USAGE = "arguments: [<spec1> <spec2> ... <specN>] <zip-file> <repoId> <repoUrl>"; 

	private VPDMf top;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if( args.length < 2) {
			System.err.println(USAGE);
			System.exit(-1);
		}
		
		String group = "";
		String artifactId = "";
		String version = "";
		
		List<File> viewFiles = new ArrayList<File>();
		List<File> dataFiles = new ArrayList<File>();
		List<String> solrViews = new ArrayList<String>();
		UMLmodel model = null;
		
		VpdmfSpec firstSpecs = VPDMfConverters.readVpdmfSpecFromFile(new File(args[0]));	
		
		List<File> specsFiles = new ArrayList<File>();
		for (int i=0; i<args.length-3; i++) {
			File specsFile = new File(args[i]);	
			specsFiles.add(specsFile);
		}
		
		File zip = new File(args[args.length-3]);	
		String repoId = args[args.length-2];	
		String repoUrl = args[args.length-1];	
			
		Iterator<File> it = specsFiles.iterator();
		while( it.hasNext() ) {
			File specsFile = it.next();
			
			//
			// parse the specs files
			//
			VpdmfSpec vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(specsFile);
			
			// Model file
			String modelPath = vpdmfSpec.getModel().getPath();
			String modelType = vpdmfSpec.getModel().getType();
			File modelFile = new File(specsFile.getParent() + "/" + modelPath);	

			// View directory
			String viewsPath = vpdmfSpec.getViewsPath();
			File viewsDir = new File(specsFile.getParent() + "/" + viewsPath);	
			viewFiles.addAll( VPDMfParser.getAllSpecFiles(viewsDir) );

			// solr views 
			solrViews.addAll( vpdmfSpec.getSolrViews() );

			// Data file
			File data = null;
			if( vpdmfSpec.getData() != null ) {
				String dataPath = vpdmfSpec.getData().getPath();
				data = new File(specsFile.getParent() + "/" + dataPath);
				if( !data.exists() )
					data = null;
				else 
					dataFiles.add(data);
			}
			
			if( data != null ) 
				System.out.println("Data File: " + data.getPath());
		
			UMLModelSimpleParser p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
			p.parseUMLModelFile(modelFile);
			UMLmodel m = p.getUmlModels().get(0);
			
			if( model == null ) {
				
				group = vpdmfSpec.getGroupId();
				artifactId = vpdmfSpec.getArtifactId();
				version = vpdmfSpec.getVersion();
				model = m;
				
			} else {
				model.mergeModel(m);
			}
			
		}			
		
		model.checkForProxy();
		
		VPDMfParser vpdmfP = new VPDMfParser();
		VPDMf top = vpdmfP.buildAllViews(firstSpecs, model, viewFiles, solrViews);	
		
		if( firstSpecs.getUimaPackagePattern() != null && firstSpecs.getUimaPackagePattern().length() > 0 ) {
			top.setUimaPkgPattern(firstSpecs.getUimaPackagePattern());
		}
		
		if( zip.exists() ) {
			System.err.println( zip.getPath()+ " already exists. Overwriting old version.");
			zip.delete();
		}
		
		// Added by MT to support creating archives in a project 'target' directory
		// when it hasn't been created yet.
		File zipdir = zip.getParentFile();
		if (!zipdir.exists()) {
			System.err.println("directory " + zipdir.getPath() + " doesn't exist. Creating it");
			zipdir.mkdirs();
		}
		// end code added by MT
		
		VPDMfArchiveFileBuilder vafb = new VPDMfArchiveFileBuilder();
		vafb.buildArchiveFile(firstSpecs, top, dataFiles, zip, repoId, repoUrl);

		System.out.println("VPDMf archive generated: " + zip.getPath());
		
		System.out.println("Attempting to deploy archive to " + repoId);
		String vpdmfBuildResponse = LocalMavenInstall
				.runMavenCommand("deploy:deploy-file -Dfile=" + zip.getPath() 
						+ " -DrepositoryId=" + repoId + " -Durl=" + repoUrl 
						+ " -DgroupId=" + group + " -DartifactId=" + artifactId 
						+ " -Dversion=" + version + " -Dpackaging=zip");
		System.err.println(vpdmfBuildResponse);		
		
	}
	

}
