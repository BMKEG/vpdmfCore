package edu.isi.bmkeg.vpdmf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.vpdmf.controller.archiveBuilder.VPDMfArchiveFileBuilder;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;
import edu.isi.bmkeg.vpdmf.utils.VPDMfParser;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public abstract class VPMDfModelTest {

	ApplicationContext ctx;

	VPDMfArchiveFileBuilder vafb;
	
	VPDMf top;
	UMLmodel m;
	UMLModelSimpleParser p;

	File vpdmfUserUmlFile;
	
	File vpdmfFile;
	File umlFile;
	File specsDir;
	File sheetsDir;
	File excelFile;
	File xlArchiveFile;
	File sheetsArchiveFile;
	
	String sql;

	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		vafb = new VPDMfArchiveFileBuilder();

	}

	@After
	public void tearDown() throws Exception {
	}
	
	protected void runTestBuildArchiveFileFromExcelFile() throws Exception {

		VpdmfSpec vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(vpdmfFile);

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(umlFile);
		
		UMLmodel m = p.getUmlModels().get(0);
					
		List<File> views = VPDMfParser.getAllSpecFiles(specsDir);
		
		VPDMfParser vpdmfP = new VPDMfParser();
		top = vpdmfP.buildAllViews(vpdmfSpec, m, views);		
		
		VpdmfSpec s = VPDMfConverters.readVpdmfSpecFromFile(vpdmfFile);
		
		List<File> dataFiles = new ArrayList<File>();
		dataFiles.add(excelFile);

		//vafb.buildArchiveFile(s, top, dataFiles, xlArchiveFile);
						
	}

	// Stub method to be overloaded for specific model tests
	protected void runChecksForExcelData() {}

	protected void runTestBuildArchiveFileFromSheetDir(String groupId, String artifactId, String version) throws Exception {		
		
		if( this.sheetsDir == null ) 
			return;

		VpdmfSpec vpdmfSpec = VPDMfConverters.readVpdmfSpecFromFile(vpdmfFile);

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(umlFile);
		
		UMLmodel m = p.getUmlModels().get(0);

		List<File> views = VPDMfParser.getAllSpecFiles(specsDir);

		VPDMfParser vpdmfP = new VPDMfParser();
		top = vpdmfP.buildAllViews(vpdmfSpec, m, views);			

		VpdmfSpec s = VPDMfConverters.readVpdmfSpecFromFile(vpdmfFile);

		List<File> dataFiles = new ArrayList<File>();
		dataFiles.add(sheetsDir);
		
		//vafb.buildArchiveFile(s, top, dataFiles, xlArchiveFile);

		this.runChecksForSheetsData();
		
	}

	// Stub method to be overloaded for specific model tests
	protected void runChecksForSheetsData() {}	
	
}
