package edu.isi.bmkeg.vpdmf.utils;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.utils.TextUtils;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public class VPDMfConverters {

	public static byte[] vpdmfObjectToByteArray(Object obj) throws VPDMfException {

		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;
		ViewInstance vi = null;
		Iterator aiIt = null;
		AttributeInstance ai = null;
		BufferedImage img = null;
		ByteArrayOutputStream baos = null;

		try {

			//
			// Caveat for ViewInstances...
			// Just in case we're trying to serialize a ViewInstance
			// with an attribute set to BufferedImage
			//
			if (obj instanceof ViewInstance) {
				vi = (ViewInstance) obj;
				aiIt = vi.readAttributes().iterator();
				while (aiIt.hasNext()) {
					ai = (AttributeInstance) aiIt.next();
					if (ai.getValue() instanceof BufferedImage) {
						img = (BufferedImage) ai.getValue();
						baos = new ByteArrayOutputStream();
						ImageIO.write(img, "png", baos);
						ai.setValue(baos.toByteArray());
					}
				}
			}

			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			return bos.toByteArray();

		} catch (IOException e) {

			e.printStackTrace();
			throw new VPDMfException(e.getClass().getName() + ": "
					+ e.getMessage());

		} finally {

			try {
				bos.close();
				oos.close();
			} catch (Exception ex) {
				throw new VPDMfException(ex.getMessage());
			}

			bos = null;
			oos = null;
			vi = null;
			aiIt = null;
			ai = null;
			img = null;
			baos = null;

		}

	}

	public static Object byteArrayToVPDMfObject(byte[] byteArray)
			throws VPDMfException {

		//
		// Remember: do not leave local variables in the static
		// method especially when they store large data.
		//
		ByteArrayInputStream bis = null;
		ObjectInputStream ois = null;

		try {

			bis = new ByteArrayInputStream(byteArray);
			ois = new ObjectInputStream(bis);
			return ois.readObject();

		} catch (IOException e) {
			throw new VPDMfException(e.getMessage());
		} catch (ClassNotFoundException cnfe) {
			throw new VPDMfException(cnfe.getMessage());
		} finally {

			try {
				bis.close();
				ois.close();
			} catch (Exception ex) {
				throw new VPDMfException(ex.getMessage());
			}

			bis = null;
			ois = null;

		}

	}
	
	public static VPDMf ReadTopFromArchive(File vpdmfArchiveFile) throws Exception {
						
		File temp = Files.createTempDir();
		temp.deleteOnExit();

		Map<String, File> cleanUp = Converters.unzipIt(vpdmfArchiveFile, temp);
		cleanUp.put(temp.getPath(), temp);
		
		String p = temp.getAbsolutePath();
		p = p.replace("\\", "/");

		File topFile = new File(p + "/" + VPDMfKnowledgeBaseBuilder.TOP_FILE);		
		
		byte[] bArray = Converters.fileContentsToBytesArray(topFile);
		VPDMf top = (VPDMf) VPDMfConverters.byteArrayToVPDMfObject(bArray);
		
		Converters.recursivelyDeleteFiles(temp);
				
		return top;
		
	}	
	
	public static File unzipBuildFile(File vpdmfArchiveFile) throws Exception {
		
		File temp = Files.createTempDir();
		temp.deleteOnExit();

		Converters.unzipIt(vpdmfArchiveFile, temp);

		String p = temp.getAbsolutePath();
		p = p.replace("\\", "/");

		Converters.recursivelyDeleteFiles(temp);

		return temp;
		
	}
	
	public static VpdmfSpec readVpdmfSpecFromFile(File specsFile) throws Exception {
		
		String specString = TextUtils.readFileToString(specsFile);
		specString = specString.replaceAll("[\\t\\n]", "");
		StringReader reader = new StringReader(specString);
		VpdmfSpec vpdmfSpec = XmlBindingTools.parseXML(reader, VpdmfSpec.class);
	
		return vpdmfSpec;
		
	}
	
}
