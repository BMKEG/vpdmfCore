package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import cern.colt.matrix.ObjectFactory2D;
import cern.colt.matrix.ObjectMatrix2D;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.DataHolder;
import edu.isi.bmkeg.vpdmf.model.instances.ObjectDataHolder;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public class DataHolderFactory extends DatabaseEngineImpl {

	private static Logger logger = Logger.getLogger(DataHolderFactory.class);

	public DataHolderFactory(String login, String password, String uri) {
		super(login, password, uri);
	}

	public DataHolderFactory() {
		super();
	}

	protected String getMatchingAddr(Vector hs, String s) {
		if (hs.contains(s))
			return s;
		Iterator it = hs.iterator();
		while (it.hasNext()) {
			String ss = (String) it.next();
			if (ss.equalsIgnoreCase(s))
				return ss;
		}
		return null;
	}

}
