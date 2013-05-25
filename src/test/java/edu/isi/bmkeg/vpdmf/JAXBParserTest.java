package edu.isi.bmkeg.vpdmf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.TextUtils;
import edu.isi.bmkeg.utils.springContext.AppContext;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.FormDesignSpec;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ViewSpec;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/edu/isi/bmkeg/vpdmf/appCtx-VPDMfTest.xml" })
public class JAXBParserTest {

	ApplicationContext ctx;

	UMLmodel m;
	UMLModelSimpleParser p;

	File resourceUmlFile;
	File vpdmfUserUmlFile;
	File specsDir;
	File workingDir;
	File formDesignSpec;
	File viewSpec;

	@Before
	public void setUp() throws Exception {

		ctx = AppContext.getApplicationContext();

		vpdmfUserUmlFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/vpdmfSystem/vpdmfSystem.xml").getFile();
		specsDir = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource/specs").getFile();

		workingDir = ctx.getResource("classpath:edu/isi/bmkeg/vpdmf").getFile();

		resourceUmlFile = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource/resource.xml")
				.getFile();
		
		formDesignSpec = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource/specs/article-fd.xml")
				.getFile();

		viewSpec = ctx.getResource(
				"classpath:edu/isi/bmkeg/vpdmf/resource/specs/article-vw.xml")
				.getFile();

		p = new UMLModelSimpleParser(UMLmodel.XMI_MAGICDRAW);
		p.parseUMLModelFile(resourceUmlFile);

		m = p.getUmlModels().get(0);

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testReadForm() throws Exception {

	    String xml = TextUtils.readFileToString(formDesignSpec);

	    // Strip out all the tabs and newlines
	    xml = xml.replaceAll("[\\t\\n]", "");
	    
		StringReader reader = new StringReader(xml);
		FormDesignSpec xmlTest = XmlBindingTools.parseXML(reader, FormDesignSpec.class);
		xmlTest.cleanUpElements();
		
	    assertEquals("Expect to find 7 elements in the form ", 7, 
	    		xmlTest.getElements().size());
		
	}

	@Test
	public final void testReadView() throws Exception {

		String xml = TextUtils.readFileToString(viewSpec);
	    xml = xml.replaceAll("[\\t\\n]", "");
	    
		StringReader reader = new StringReader(xml);
		ViewSpec xmlTest = XmlBindingTools.parseXML(reader, ViewSpec.class);

	    assertEquals("Expect to find 7 primitives in this view", 7, 
	    		xmlTest.getPrimitives().size());
		
	}

	
}
