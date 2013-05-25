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

public class DataHolderFactory extends DatabaseEngine {

	private static Logger logger = Logger.getLogger(DataHolderFactory.class);

	public DataHolderFactory(String login, String password, String uri) {
		super(login, password, uri);
	}

	public DataHolderFactory() {
		super();
	}
	
	protected DataHolder executeSimpleSelect(ViewInstance vi, Vector addrVec)
			throws Exception {

		clearQuery();
		buildSelectHeader(vi, addrVec);
		buildSqlConditions(vi, NOPK);
		buildTableAliases(vi);

		String sql = buildSQLSelectStatement();
		prettyPrintSQL(sql);

		long t = System.currentTimeMillis();
		ResultSet rs = stat.executeQuery(sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DataHolder, SimpleSelect: " + deltaT / 1000.0
					+ " s\n");

		return this.simpleRS2DH(vi, addrVec, rs);

	}

	protected DataHolder simpleRS2DH(ViewInstance vi, Vector addrVec,
			ResultSet rs) throws Exception {

		boolean nav = rs.last();
		int rowCount = rs.getRow();

		if (rowCount == 0)
			return null;

		ObjectMatrix2D data = ObjectFactory2D.dense.make(rowCount,
				selectHeader.size());

		TOPLOOP: for (int i = 0; i < rowCount; i++) {
			nav = rs.absolute(i + 1);

			for (int j = 0; j < selectHeader.size(); j++) {
				String addr = (String) addrVec.get(j);

				AttributeInstance ai = vi.readAttributeInstance(addr, 0);

				Object o = getDataFromRS(rs, ai);
				if (o != null) {
					data.set(i, j, o);
					// DEBUG
					// System.out.println( "[" + i + "," + j + "]=" +
					// o.toString() );
				}

			}

		}

		return new ObjectDataHolder(data, addrVec);

	}

	protected DataHolder executeCompositeSelect(ViewInstance vi1,
			String pvName1, Vector addrVec1, ViewInstance vi2, String pvName2,
			Vector addrVec2, String linkingClassName) throws Exception {

		PrimitiveDefinition pd1 = (PrimitiveDefinition) vi1.getDefinition()
				.getSubGraph().getNodes().get(pvName1);

		PrimitiveDefinition pd2 = (PrimitiveDefinition) vi2.getDefinition()
				.getSubGraph().getNodes().get(pvName2);

		Iterator it = vi1.getDefinition().getTop().getUmlModel().listClasses()
				.values().iterator();
		UMLclass linkingClass = null;
		while (it.hasNext()) {
			UMLclass c = (UMLclass) it.next();
			if (c.getBaseName().equals(linkingClassName)) {
				linkingClass = c;
			}
		}

		if (linkingClass == null)
			throw new Exception("Can't find class: " + linkingClassName);

		clearQuery();
		buildSelectHeader(vi1, addrVec1);
		buildSqlConditions(vi1, NOPK);
		buildTableAliases(vi1);

		HashMap<String, ArrayList<String>> queryState = saveQueryState();

		ViewDefinition vd2 = (ViewDefinition) vi2.getDefinition();

		HashMap<String, ArrayList<String>> newQs = substituteFromQueryState(
				queryState, linkingClass.getBaseName(), pd1.getName(),
				pd2.getName());

		restoreQueryState(newQs);
		buildSelectHeader(vi2, addrVec2);
		buildSqlConditions(vi2, NOPK);
		buildTableAliases(vi2);

		String sql = buildSQLSelectStatement();
		prettyPrintSQL(sql);

		long t = System.currentTimeMillis();
		ResultSet rs = stat.executeQuery(sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DataHolder, Composite Select: " + deltaT
					/ 1000.0 + " s\n");

		return this.compositeRS2DH(vi1, pd1, addrVec1, vi2, pd2, addrVec2,
				linkingClassName, rs);

	}

	protected DataHolder compositeRS2DH(ViewInstance vi1,
			PrimitiveDefinition pd1, Vector addrVec1, ViewInstance vi2,
			PrimitiveDefinition pd2, Vector addrVec2, String linkingClassName,
			ResultSet rs) throws Exception {

		boolean nav = rs.last();
		int rowCount = rs.getRow();

		Vector addrVec = new Vector();

		if (rowCount == 0)
			return null;

		ObjectMatrix2D data = ObjectFactory2D.dense.make(rowCount,
				selectHeader.size());

		Vector replAddrVec = new Vector();
		Iterator it = addrVec1.iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			String r = s.replaceAll("\\]" + pd1.getName() + "\\|"
					+ linkingClassName, "]" + pd2.getName() + "|"
					+ linkingClassName);
			replAddrVec.add(r);
		}

		for (int j = 0; j < selectHeader.size(); j++) {
			String sH = (String) selectHeader.get(j);

			Pattern regexToRepl = Pattern
					.compile("(\\S+)_\\d+__(\\S+)\\.(\\S+)");
			Matcher m = regexToRepl.matcher(sH);
			String addr = "";
			if (m.find()) {

				String pvName = m.group(1);
				String cName = m.group(2);
				String aName = m.group(3);
				addr = "]" + pvName + "|" + cName + "." + aName;

			}

			String mAddr = null;
			String mAddr1 = getMatchingAddr(replAddrVec, addr);
			String mAddr2 = getMatchingAddr(addrVec2, addr);
			if (mAddr1 != null) {
				mAddr = mAddr1;
			} else if (mAddr2 != null) {
				mAddr = mAddr2;
			} else {
				throw new Exception("ARGH");
			}

			addrVec.add(mAddr);

		}

		TOPLOOP: for (int i = 0; i < rowCount; i++) {
			nav = rs.absolute(i + 1);

			for (int j = 0; j < selectHeader.size(); j++) {
				String addr = (String) addrVec.get(j);

				ViewInstance vi = null;
				String mAddr = null;

				String mAddr1 = null;
				String lookupAddr1 = getMatchingAddr(replAddrVec, addr);
				if (lookupAddr1 != null)
					mAddr1 = lookupAddr1.replaceAll("\\]" + pd2.getName()
							+ "\\|", "]" + pd1.getName() + "|");

				String mAddr2 = getMatchingAddr(addrVec2, addr);

				if (mAddr1 != null) {
					vi = vi1;
					mAddr = mAddr1;
				} else if (mAddr2 != null) {
					vi = vi2;
					mAddr = mAddr2;
				} else {
					throw new Exception("ARGH");
				}

				AttributeInstance ai = vi.readAttributeInstance(mAddr, 0);

				Object o = getDataFromRS(rs, ai);
				if (o != null) {
					data.set(i, j, o);
					// DEBUG
					// System.out.println( "[" + i + "," + j + "]=" +
					// o.toString() );
				}

			}

		}

		return new ObjectDataHolder(data, addrVec);

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
