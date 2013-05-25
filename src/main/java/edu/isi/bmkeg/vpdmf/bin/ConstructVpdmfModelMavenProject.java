package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.io.Files;

import edu.isi.bmkeg.uml.interfaces.ActionscriptInterface;
import edu.isi.bmkeg.uml.interfaces.JavaUmlInterface;
import edu.isi.bmkeg.uml.interfaces.UimaUMLInterface;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.mvnRunner.LocalMavenInstall;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

public class ConstructVpdmfModelMavenProject {

	public static String USAGE = "arguments: [<spec1> <spec2> ... <specN>] <dir> <repoId> <repoUrl>"; 
	
	/**
	 * @param args
	 * @throws Exception 
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
		List<String> specsPaths = new ArrayList<String>();
		for (int i = 0; i < args.length - 3; i++) {
			specsPaths.add(args[i]);
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
			viewFiles.addAll(VPDMfParser.getAllSpecFiles(viewsDir));

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
		
		JavaUmlInterface java = new JavaUmlInterface();
		if( top.getUimaPkgPattern() != null && top.getUimaPkgPattern().length() > 0 ) {
			java = new UimaUMLInterface(top.getUimaPkgPattern());
		}
		java.setUmlModel(model);
		java.setBuildQuestions(true);

		File tempDir = Files.createTempDir();
		tempDir.deleteOnExit();
		String dAddr = tempDir.getAbsolutePath();
		File zip = new File(dAddr + "/temp.zip");

		java.buildJpaMavenProject(zip, null, 
				group, artifactId + "-jpa", version, 
				repoId, repoUrl);

		File buildDir = new File(dAddr + "/jpaModel");
		Converters.unzipIt(zip, buildDir);

		String srcFileName = artifactId + "-jpa-" + version + "-src.jar";
		String srcDirName = artifactId + "-jpa";
		
		String srcFileAddr = dAddr + "/jpaModel/target/" + srcFileName;
		File srcFile = new File(srcFileAddr);
		
		Converters.copyFile(zip, new File(dir.getPath() + "/" + srcFileName));
		Converters.unzipIt(zip, new File(dir.getPath() + "/" + srcDirName));

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Flex component build and deploy
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		ActionscriptInterface as = new ActionscriptInterface();
		as.setUmlModel(model);

		zip = new File(dAddr + "/temp2.zip");
		as.buildFlexMojoMavenProject(zip, null, group, 
				artifactId + "-as", version,
				repoId, repoUrl);
		
		Converters.unzipIt(zip, tempDir);
		
		srcFileName = artifactId + "-as-" + version + "-src.zip";
		srcDirName = artifactId + "-as-" + version + "-src";
		srcFile = new File(dir.getPath() + "/" + srcFileName);
		
		Converters.copyFile(zip, srcFile);
		Converters.unzipIt(srcFile, new File(dir.getPath() ));
		
		Converters.recursivelyDeleteFiles(tempDir);
		
		System.out.println("Model libraries built:" + dir);
		
	}

}
