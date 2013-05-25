package edu.isi.bmkeg.vpdmf.model.instances;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewLink;

public class ViewLinkInstance extends SuperGraphEdge {
	static final long serialVersionUID = 2593554832580697147L;

	private ClassInstance linkClass;
	private ViewLink definition;

	public ViewLinkInstance() {
		super();
	}

	public ViewLinkInstance(ViewLink viewLink) {
		super();
		
		/*String attLink = viewLink.getAttributeLinkage();
		if (attLink == null)
			attLink = "";
		String[] connxArray = attLink.split(",");

		//
		// Does the view link use a linking class that needs to be
		// incorporated into queries ????
		//
		String addr = "";
		Pattern patt = Pattern.compile("=].*?\\|(.*):(.*?)=");
		Matcher matcher = null;
		if (connxArray.length > 0) {
			addr = connxArray[0];
			matcher = patt.matcher(addr);
		}
		if (matcher.find()) {
			String cName = matcher.group(1);
			String rName = matcher.group(2);

			UMLmodel m = ((ViewDefinition) viewLink.getInEdgeNode()).getTop()
					.getUmlModel();

			UMLclass c = m.lookupClass(cName).iterator().next();
			UMLrole r = (UMLrole) c.getAssociateRoles().get(rName);

			UMLclass lc = r.getAss().getLinkClass();
			if (lc != null) {
				this.setLinkClass(new ClassInstance(lc));
			}

		}*/

		this.definition = viewLink;

	}

	public void destroy() {
		this.linkClass = null;
		this.definition = null;

		super.destroy();
	}

	public ViewLink getDefinition() {
		return this.definition;
	}

	public ClassInstance getLinkClass() {
		return this.linkClass;
	}

	public void setDefinition(ViewLink definition) {
		this.definition = definition;
	}

	public void setLinkClass(ClassInstance linkClass) {
		this.linkClass = linkClass;
	}

};
