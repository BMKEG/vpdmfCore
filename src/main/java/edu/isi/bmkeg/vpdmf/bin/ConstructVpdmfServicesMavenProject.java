package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.mvnRunner.LocalMavenInstall;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.ActionscriptVpdmfInterface;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.JavaVpdmfServicesConstructor;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

public class ConstructVpdmfServicesMavenProject {

	Logger log = Logger
			.getLogger("edu.isi.bmkeg.vpdmf.bin.ConstructVpdmfServicesMavenProject");

	public static String USAGE = "arguments: [<spec1> <spec2> ... <specN>] <directory> <repoId> <repoUrl>\n";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		if( args.length < 4 ) {
			System.err.println(USAGE);
			System.exit(-1);
		}
		
		List<File> viewFiles = new ArrayList<File>();
		List<File> dataFiles = new ArrayList<File>();
		List<String> solrViews = new ArrayList<String>();
		UMLmodel model = null;

		VpdmfSpec firstSpecs = VPDMfConverters.readVpdmfSpecFromFile(new File(
				args[0]));

		List<File> specsFiles = new ArrayList<File>();
		for (int i = 0; i < args.length - 3; i++) {
			File specsFile = new File(args[i]);
			specsFiles.add(specsFile);
		}

		File dir = new File(args[args.length - 3]);
		dir.mkdirs();
		
		String repoId = args[args.length - 2];
		String repoUrl = args[args.length - 1];
		
		Iterator<File> it = specsFiles.iterator();
		while (it.hasNext()) {
			File specsFile = it.next();

			//
			// parse the specs files
			//
			VpdmfSpec vpdmfSpec = VPDMfConverters
					.readVpdmfSpecFromFile(specsFile);

			// Model file
			String modelPath = vpdmfSpec.getModel().getPath();
			String modelType = vpdmfSpec.getModel().getType();
			File modelFile = new File(specsFile.getParent() + "/" + modelPath);

			// View directory
			String viewsPath = vpdmfSpec.getViewsPath();
			File viewsDir = new File(specsFile.getParent() + "/" + viewsPath);
			viewFiles.addAll(VPDMfParser.getAllSpecFiles(viewsDir));http://trailers.apple.com/movies/independent/56up/56up-tlr1_h720p.mov

			// solr views
			solrViews.addAll(vpdmfSpec.getSolrViews());

			// Data file
			File data = null;
			if (vpdmfSpec.getData() != null) {
				String dataPath = vpdmfSpec.getData().getPath();
				data = new File(dataPath);
				if (!data.exists())
					data = null;
				else
					dataFiles.add(data);
			}

			if (data != null)
				System.out.println("Data File: " + data.getPath());

			UMLModelSimpleParser p = new UMLModelSimpleParser(
					UMLmodel.XMI_MAGICDRAW);
			p.parseUMLModelFile(modelFile);
			UMLmodel m = p.getUmlModels().get(0);

			if (model == null) {
				model = m;
			} else {
				model.mergeModel(m);
			}

		}

		VPDMfParser vpdmfP = new VPDMfParser();
		VPDMf top = vpdmfP.buildAllViews(firstSpecs, model, viewFiles,
				solrViews);

		if( firstSpecs.getUimaPackagePattern() != null && firstSpecs.getUimaPackagePattern().length() > 0 ) {
			top.setUimaPkgPattern(firstSpecs.getUimaPackagePattern());
		}
		
		String group = top.getGroupId();
		String artifactId = top.getArtifactId();
		String version = top.getVersion();

		// ~~~~~~~~~~~~~~~~~~~~
		// Java component build
		// ~~~~~~~~~~~~~~~~~~~~
		
		JavaVpdmfServicesConstructor java = new JavaVpdmfServicesConstructor(top);

		String dAddr = dir.getAbsolutePath();
		File srcJar = new File(dAddr + "/" + artifactId +"-services-src.jar");
		
		java.buildServiceMavenProject(srcJar, null, group, artifactId, version, repoId, repoUrl);

		File srcDir = new File(dAddr + "/" + artifactId +"-services");
		srcDir.mkdirs();
		Converters.unzipIt(srcJar , srcDir);

		
		// ~~~~~~~~~~~~~~~~~~~~
		// Flex component build
		// ~~~~~~~~~~~~~~~~~~~~

		ActionscriptVpdmfInterface as = new ActionscriptVpdmfInterface(top);

		File zip = new File(dAddr + "/" + artifactId +"-as-services-src.zip");

		as.buildServiceMavenProject(zip, null, group, artifactId, version, repoId, repoUrl);

		File zipDir = new File(dAddr + "/" + artifactId +"-as-services");
		zipDir.mkdirs();
		Converters.unzipIt(zip , zipDir);
		
		System.out.println("Service libraries built:" + dir);

		
	}

}
