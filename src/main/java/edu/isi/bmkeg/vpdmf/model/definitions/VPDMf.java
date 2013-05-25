package edu.isi.bmkeg.vpdmf.model.definitions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.darwinsys.util.FileIO;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.uml.sources.UMLModelSimpleParser;
import edu.isi.bmkeg.utils.TextUtils;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.ViewSpec;

public class VPDMf implements Serializable {
	static final long serialVersionUID = 8047039304729208683L;

	public static String vpdmfLogin = "kmrg";
	public static String vpdmfPassword = "dancer";

	private String groupId;
	private String artifactId;
	private String version;
	
	private List<String> viewsToIndex = new ArrayList<String>();
	private String uimaPkgPattern;
	
//	private ViewStyle style;
	private ViewGraphDefinition vGraphDef;
	private HashMap<String, ViewDefinition> views = new HashMap<String, ViewDefinition>();
	private UMLmodel umlModel;
	private UMLmodel vpdmfModel;
	private String startView;
	
	public VPDMf(String groupId, String artifactId, String version) throws Exception {
		
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		
	}
	
	public String[] listViewTrees() {

		ArrayList<String> views = new ArrayList<String>();
		Object[] array = this.getViews().values().toArray();
		for (int i = 0; i < array.length; i++) {
			ViewDefinition vd = (ViewDefinition) array[i];
			views.add(vd.readTree());
		}

		array = views.toArray();
		Arrays.sort(array);

		String[] viewArray = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			viewArray[i] = (String) array[i];
		}

		return viewArray;

	}

	public Object[] indentedViewList() {

		ArrayList views = new ArrayList();
		Object[] array = this.getViews().values().toArray();
		for (int i = 0; i < array.length; i++) {
			ViewDefinition vd = (ViewDefinition) array[i];
			views.add(vd.readTree());
		}

		array = views.toArray();
		Arrays.sort(array);

		return array;

	}

	public Map<UMLclass, List<PrimitiveDefinition>> readClassPrimitiveLookupTable() {

		Map<UMLclass, List<PrimitiveDefinition>> lookup = new HashMap<UMLclass, List<PrimitiveDefinition>>();

		Object[] vdArray = this.getViews().values().toArray();
		for (int i = 0; i < vdArray.length; i++) {
			ViewDefinition vd = (ViewDefinition) vdArray[i];

			Object[] pdArray = vd.getSubGraph().getNodes().values().toArray();
			for (int j = 0; j < pdArray.length; j++) {
				PrimitiveDefinition pd = (PrimitiveDefinition) pdArray[j];

				Object[] cArray = pd.getClasses().toArray();
				for (int k = 0; k < cArray.length; k++) {
					UMLclass c = (UMLclass) cArray[k];
					if (!lookup.containsKey(c)) {
						List<PrimitiveDefinition> list = new ArrayList<PrimitiveDefinition>();
						list.add(pd);
						lookup.put(c, list);
					} else {
						List<PrimitiveDefinition> list = lookup.get(c);
						list.add(pd);
						lookup.put(c, list);
					}
				}

			}

		}

		return lookup;

	}

	@Deprecated
	public Object[] readDataViewDefinitions() {

		ArrayList al2 = new ArrayList();
		/*
		 * for( int i=0,j=0; i<al1.size(); i++) { ViewDefinition vd =
		 * (ViewDefinition) al1.get(i); if( vd.get_Type() == ViewDefinition.DATA
		 * ) al2.add(vd); }
		 */

		return al2.toArray();

	}

	@Deprecated
	public void printPrimtiveEditable() {

		System.out.println("PRINTOUT TO SHOW WHICH PRIMITIVES ARE EDITABLE IN "
				+ "A VIEW. THESE WILL BE REMOVED WHEN DELETIONS OCCUR");

		Object[] vds = this.readDataViewDefinitions();
		for (int i = 0; i < vds.length; i++) {
			ViewDefinition vd = (ViewDefinition) vds[i];

			System.out.println(vd.getName() + ": " + vd.isEditable());

			Iterator pdIt = vd.getSubGraph().getNodes().values().iterator();
			while (pdIt.hasNext()) {
				PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();
				System.out.println("    " + pd.getName() + ": "
						+ pd.isEditable());
			}

		}

	}

	public void setUmlModel(UMLmodel umlModel) {
		this.umlModel = umlModel;
	}

	public UMLmodel getUmlModel() {
		return umlModel;
	}

//	public void setStyle(ViewStyle style) {
//		this.style = style;
//	}

//	public ViewStyle getStyle() {
//		return style;
//	}

	public void setvGraphDef(ViewGraphDefinition vGraphDef) {
		this.vGraphDef = vGraphDef;
	}

	public ViewGraphDefinition getvGraphDef() {
		return vGraphDef;
	}

	public void setViews(HashMap<String, ViewDefinition> views) {
		this.views = views;
	}

	public HashMap<String, ViewDefinition> getViews() {
		return views;
	}

	public void loadAllViewsFromSpecDir(File specDirectory) throws Exception {

		UMLmodel model = this.getUmlModel();

		if (!specDirectory.exists() || !specDirectory.isDirectory())
			throw new Exception(
					"specification directory for model does not exist");

		String rootPath = model.getTopPackage().getPkgAddress();

		if (model.checkClassExistence("vpdmfUser")) {
			// generateVpdmfUserSpec(specDirectory);
		}

		loadViewsInDirectory(specDirectory);

	}

	public void linkSystemClasses() throws Exception {

		UMLclass stringType = this.umlModel.lookupClass("String").iterator()
				.next();

		// Link this view to the System views
		Iterator<ViewDefinition> it = this.getViews().values().iterator();
		while (it.hasNext()) {
			ViewDefinition vw = it.next();

			// link the primary class of the view to the ViewTable classes
			if (vw.getType() == ViewDefinition.DATA
					|| vw.getType() == ViewDefinition.COLLECTION
					|| vw.getType() == ViewDefinition.EXTERNAL) {

				vw.buildViewsSystemViews();
			
			}
			// link the primary class of the view to the ViewLinkTable classes
			else if (vw.getType() == ViewDefinition.LINK) {
			
				vw.buildViewsSystemLinkViews();
			
			}
			// Add an vpdmfLabel attribute to the primary class of the view
			// For LookupSpec views.
			else if (vw.getType() == ViewDefinition.LOOKUP) {

				UMLclass primaryClass = vw.getPrimaryPrimitive()
						.getPrimaryClass();

				/*
				 * if( !defined $primaryClass ) { confess
				 * "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
				 * . "You have an error in your ViewSpec Specification for " .
				 * $vw->{Name} .
				 * " in the specification of the primary primitive $vw->{primaryPrimitive}->{Name}\n\n"
				 * . "We cannot locate the primary class of this primitive. \n"
				 * . "Please check the ViewSpec Specificiation .\n".
				 * "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
				 * ; }
				 */

				if (primaryClass.lookupAttributeByName("vpdmfLabel") == null) {

					UMLattribute att = new UMLattribute();
					this.getUmlModel().addItem(att);

					att.setImplName("vpdmfLabel");
					att.setBaseName("vpdmfLabel");
					att.setType(stringType);

					primaryClass.getAttributes().add(att);
					att.setParentClass(primaryClass);

				}
				
				if (primaryClass.lookupAttributeByName("indexTuple") == null) {

					UMLattribute att = new UMLattribute();
					this.getUmlModel().addItem(att);

					att.setImplName("indexTuple");
					att.setBaseName("indexTuple");
					att.setType(stringType);

					primaryClass.getAttributes().add(att);
					att.setParentClass(primaryClass);

				}


			}

		}

	}	
	
	private void loadViewsInDirectory(File specDirectory) throws Exception {

		File[] files = specDirectory.listFiles();

		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			if (file.isDirectory()) {
				loadViewsInDirectory(file);
			}

			if (file.getName().endsWith(".xml")) {

				ViewDefinition vd = new ViewDefinition();
				vd.setTop(this);

				Reader r = new FileReader(file);
				String spec = FileIO.readerToString(r);
				vd.setSpecification(spec);

				Pattern patt = Pattern.compile("^(.+)_l?view\\.xml");
				StringBuffer sb = new StringBuffer();
				Matcher m = patt.matcher(file.getName());
				while (m.find()) {
					m.appendReplacement(sb, "");
				}
				m.appendTail(sb);
				vd.setName(sb.toString());

				this.views.put(vd.getName(), vd);

			}

		}

	}

	public ViewDefinition readViewDefinitionFromViewTypeString(String viewType) {
		
		String vt = viewType.replaceAll("\\.\\%$", "");
		if (vt.indexOf(".%") != -1)
			vt = vt.substring(vt.lastIndexOf(".%") + 3, vt.length());
		else
			vt = vt.substring(1, vt.length());			
		if (this.getViews().containsKey(vt)) {
			return (ViewDefinition) this.getViews().get(vt);
		} else {
			return null;
		}
		
	}
	
	public void setStartView(String startView) {
		this.startView = startView;
	}

	public String getStartView() {
		return startView;
	}

	
	
	public UMLmodel getVpdmfModel() {
		return vpdmfModel;
	}

	public void setVpdmfModel(UMLmodel vpdmfModel) {
		this.vpdmfModel = vpdmfModel;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getViewsToIndex() {
		return viewsToIndex;
	}

	public void setViewsToIndex(List<String> viewsToIndex) {
		this.viewsToIndex = viewsToIndex;
	}

	public String getUimaPkgPattern() {
		return uimaPkgPattern;
	}

	public void setUimaPkgPattern(String uimaPkgPattern) {
		this.uimaPkgPattern = uimaPkgPattern;
	}

}
