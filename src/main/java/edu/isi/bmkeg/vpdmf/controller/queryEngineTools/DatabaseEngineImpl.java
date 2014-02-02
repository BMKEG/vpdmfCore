package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.apache.log4j.Logger;

import com.google.common.io.Files;

import cern.colt.matrix.ObjectMatrix1D;
import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLpackage;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.uml.utils.UMLDataConverters;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.exceptions.InterruptException;
import edu.isi.bmkeg.vpdmf.exceptions.MySqlNotAvailableException;
import edu.isi.bmkeg.vpdmf.exceptions.MySqlPermissionException;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.IndexElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.specs.VpdmfSpec;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstanceGraph;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveLinkInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;

public class DatabaseEngineImpl implements DatabaseEngine {

	private static Logger logger = Logger.getLogger(DatabaseEngineImpl.class);

	// types of query
	protected int queryType;
	protected static int INSERT = 1;
	protected static int LIST = 2;
	protected static int UID = 3;
	protected static int UPDATE = 4;
	protected static int DELETE = 5;
	protected static int COUNT = 6;
	protected static int SUMMARY = 7;
	protected static int RANK = 8;

	// flags for internal processing
	protected static int ALL = 9;
	protected static int NOPK = 10;
	protected static int INDEXONLY = 11;
	protected static int PKONLY = 12;
	protected static int FKONLY = 13;
	protected static int NOALIAS = 14;

	// jdbc variables
	public Connection dbConnection;
	protected Set<String> drivers = new HashSet<String>();
	protected Statement stat;
	protected Statement uStat;

	// internal variables for query construction
	private boolean cancelled = false;
	protected boolean reducible = false;
	protected boolean lc = false;

	protected boolean verbose = true;
	protected Map<PrimitiveLink, Integer> pvLinkOrder;
	protected ArrayList<String> orderBy = new ArrayList<String>();
	protected SuperGraphTraversal pigTraversal;
	protected Hashtable pigLookup;
	protected Hashtable pliLookup;
	protected Vector classesToFillIn;

	protected List<String> tableAlias;
	protected List<String> sqlConditions;
	protected List<String> selectHeader;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// variables starting with 'local_' are copied
	// from the vsm to permit independent operation
	// in a separate thread.
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// local login variables
	private String login;
	private String password;
	private String uri;

	// local top + cl
	protected VPDMf vpdmf;
	protected ClassLoader cl;

	// jar file containing all pojo definitions.
	protected File vpdmfJarFile;

	private List<ViewInstance> viewList = new ArrayList<ViewInstance>();
	private ViewInstance viToCenterOn;

	// local lookup variables
	private String lookupIndex;
	private List<String> lookupResults;
	private ViewInstance lookupView;
	private JComponent lookupComponent;
	
	//
	// Setting this value so Effectively disable 'data striping' 
	// unless a developer changes this value in their code. Thus
	// any query that returns less than 100 million records is 
	// never going to be striped. 
	//
	private int maxReturnedInQuery = 100000000; 
	
	private int maxNeighbors;
	private Hashtable lookupAliases;

	// local paging variables for views
	private int listPageSize = 20;
	private int listOffset;
	private int listCount;
	private boolean doPagingInQuery;

	// ____________________________________________________________________________

	public DatabaseEngineImpl(String login, String password, String uri) {

		this.setLogin(login);
		this.setPassword(password);
		this.setUri(uri);

		this.drivers.add("com.mysql.jdbc.Driver");

		this.selectHeader = new ArrayList<String>();
		this.sqlConditions = new ArrayList<String>();
		this.tableAlias = new ArrayList<String>();

	}

	public DatabaseEngineImpl() {

		this.drivers.add("com.mysql.jdbc.Driver");

		this.selectHeader = new ArrayList<String>();
		this.sqlConditions = new ArrayList<String>();
		this.tableAlias = new ArrayList<String>();

	}

	// ____________________________________________________________________________

	protected ResultSet executeQueryOnStatement(Statement stat, String sql)
			throws Exception {
		ResultSet rs = null;

		if (this.isCancelled())
			throw new InterruptException("Thread has been cancelled");

		rs = stat.executeQuery(sql);

		return rs;
	}

	protected void executeOnStatement(Statement stat, String sql)
			throws Exception {

		if (this.isCancelled())
			throw new InterruptException("Thread has been cancelled");

		stat.execute(sql);

	}

	// ____________________________________________________________________________
	// Part XXX: ViewSpec-level queries
	//
	// These queries opeate on views as a whole. They all assume that a
	// jdbc connection has been established and a transaction initiated.
	// They are all designed to be called from within Swingworkers and
	// subthreads internal to the DatabaseEngine.
	//

	protected HashMap<String, ArrayList<String>> saveQueryState() {

		HashMap<String, ArrayList<String>> ht = new HashMap<String, ArrayList<String>>();

		ht.put("selectHeader", new ArrayList(this.selectHeader));
		ht.put("sqlConditions", new ArrayList(this.sqlConditions));
		ht.put("tableAlias", new ArrayList(this.tableAlias));

		return ht;

	}

	protected void restoreQueryState(HashMap<String, ArrayList<String>> ht) {

		this.selectHeader = (ArrayList) ht.get("selectHeader");
		this.sqlConditions = (ArrayList) ht.get("sqlConditions");
		this.tableAlias = (ArrayList) ht.get("tableAlias");

	}

	protected HashMap<String, ArrayList<String>> substituteFromQueryState(
			HashMap<String, ArrayList<String>> queryState, String tableName,
			String primitiveToReplace, String replacingPrimitive) {

		HashMap<String, ArrayList<String>> newHt = new HashMap<String, ArrayList<String>>();

		if (lc) {
			tableName = tableName.toLowerCase();
			primitiveToReplace = primitiveToReplace.toLowerCase();
			replacingPrimitive = replacingPrimitive.toLowerCase();
		}

		String[] ss = new String[] { "selectHeader", "sqlConditions",
				"tableAlias" };

		for (int i = 0; i < 3; i++) {
			ArrayList<String> v = queryState.get(ss[i]);

			HashSet<String> newHs = new HashSet<String>();

			Iterator<String> it = v.iterator();
			while (it.hasNext()) {
				String s = it.next();

				Pattern regexToRepl = Pattern.compile(primitiveToReplace
						+ "_(\\d+)__" + tableName);
				Matcher m = regexToRepl.matcher(s);
				if (m.find()) {

					String newS = m.replaceAll(replacingPrimitive + "_"
							+ m.group(1) + "__" + tableName);
					newHs.add(newS);

				} else {

					newHs.add(s);

				}

			}

			ArrayList<String> newV = new ArrayList<String>(newHs);
			newHt.put(ss[i], newV);

		}

		return newHt;

	}

	public Vector getLinkKeyString(ClassInstance ci, int type)
			throws VPDMfException {
		Vector attrs = new Vector();
		attrs.add(0, "");
		attrs.add(1, "");

		Vector roles = new Vector();
		roles.add(ci.getDefinition().getLinkAssociation().getRole1());
		roles.add(ci.getDefinition().getLinkAssociation().getRole2());
		UMLclass c1 = ci.getDefinition().getLinkAssociation().getRole1()
				.getDirectClass();
		UMLclass c2 = ci.getDefinition().getLinkAssociation().getRole2()
				.getDirectClass();

		Iterator roleIt = roles.iterator();
		while (roleIt.hasNext()) {
			UMLrole role = (UMLrole) roleIt.next();

			Vector fksVec = new Vector();
			if (role.getImplementedBy().size() > 0) {
				Iterator implIt = role.getImplementedBy().iterator();
				while (implIt.hasNext()) {
					UMLrole r = (UMLrole) implIt.next();
					fksVec.addAll(r.getFkArray());
				}
			} else {
				fksVec.addAll(role.getFkArray());
			}

			Iterator fksIt = fksVec.iterator();
			while (fksIt.hasNext()) {

				UMLattribute fk = (UMLattribute) fksIt.next();
				UMLattribute pk = fk.getPk();

				//
				// Only interested in PKs since the FKS are in the classInstance
				String kString = "|" + pk.getParentClass().getBaseName() + "."
						+ pk.getBaseName();

				AttributeInstance ai = null;
				try {
					ai = (AttributeInstance) ci.attributes
							.get(fk.getBaseName());
				} catch (Exception e) {
					e.printStackTrace();
				}
				kString += "=" + ai.readValueString();

				if (c1.equals(pk.getParentClass())) {

					String temp = (String) attrs.get(0);
					if (temp.length() != 0) {
						temp += "&";
					}
					attrs.add(0, temp + kString);

				} else if (c2.equals(pk.getParentClass())) {

					String temp = (String) attrs.get(1);
					if (temp.length() != 0) {
						temp += "&";
					}
					attrs.add(1, temp + kString);

				} else {
					throw new VPDMfException(
							"Problems retrieving data from links");
				}

			}

		}

		return attrs;

	}

	/**
	 * @todo ???
	 * @param pli
	 *            PrimitiveLinkInstance
	 * @param type
	 *            int
	 * @return Vector
	 */
	public Vector getLinkKeyString(PrimitiveLinkInstance pli, int type) 
			throws VPDMfException {

		Vector attrs = new Vector();
		attrs.add(0, "");
		attrs.add(1, "");

		ViewInstance vi = (ViewInstance) pli.getGraph().getSubGraphNode();
		PrimitiveLink pl = pli.getPVLinkDef();
		PrimitiveDefinition pv1 = (PrimitiveDefinition) pl.getOutEdgeNode();
		PrimitiveDefinition pv2 = (PrimitiveDefinition) pl.getInEdgeNode();
		ArrayList<UMLattribute> attDefLookup_pv1 = pv1.readAttributes();
		ArrayList<UMLattribute> attDefLookup_pv2 = pv2.readAttributes();
		PrimitiveInstance pi1 = (PrimitiveInstance) pli.getOutEdgeNode();
		PrimitiveInstance pi2 = (PrimitiveInstance) pli.getInEdgeNode();

		UMLrole role = pl.getRole();
		Set<UMLrole> roles = new HashSet<UMLrole>();
		if (role.getImplementedBy().size() > 0) {
			roles = role.getImplementedBy();
		} else {
			roles.add(role);
		}

		Iterator roleIt = roles.iterator();
		while (roleIt.hasNext()) {
			role = (UMLrole) roleIt.next();

			Vector fksVec = new Vector();
			if (role.getImplementedBy().size() > 0) {
				Iterator implIt = role.getImplementedBy().iterator();
				while (implIt.hasNext()) {
					UMLrole r = (UMLrole) implIt.next();
					fksVec.addAll(r.getFkArray());
				}
			} else {
				fksVec.addAll(role.getFkArray());
			}

			Iterator fksIt = fksVec.iterator();
			while (fksIt.hasNext()) {

				UMLattribute fk = (UMLattribute) fksIt.next();
				UMLattribute pk = fk.getPk();
				UMLattribute k1 = null;
				UMLattribute k2 = null;

				if (attDefLookup_pv1.contains(pk)) {
					k1 = pk;
				}

				if (attDefLookup_pv1.contains(fk)) {
					k1 = fk;
				}

				if (attDefLookup_pv2.contains(pk)) {
					k2 = pk;
				}

				if (attDefLookup_pv2.contains(fk)) {
					k2 = fk;
				}

				if (k1 != null) {

					String p1Name = pi1.getDefinition().getName();
					String k1String = "]" + p1Name + "|"
							+ k1.getParentClass().getBaseName() + "."
							+ k1.getBaseName();

					AttributeInstance ai1 = null;
					try {
						ai1 = vi.readAttributeInstance(k1String,
								pi1.readIndex());
					} catch (Exception e) {
						e.printStackTrace();
					}
					k1String += "=" + ai1.readValueString();

					String temp = (String) attrs.get(0);
					if (temp.length() != 0) {
						temp += "&";
					}
					attrs.add(0, temp + k1String);

				}

				if (k2 != null) {

					String p2Name = pi2.getDefinition().getName();

					String k2String = "]" + p2Name + "|"
							+ k2.getParentClass().getBaseName() + "."
							+ k2.getBaseName();

					AttributeInstance ai2 = null;
					try {
						ai2 = vi.readAttributeInstance(k2String,
								pi2.readIndex());
					} catch (Exception e) {
						e.printStackTrace();
					}
					k2String += "=" + ai2.readValueString();

					String temp = (String) attrs.get(1);
					if (temp.length() != 0) {
						temp += "&";
					}
					attrs.add(1, temp + k2String);
				}

			}

		}

		return attrs;

	}

	public boolean connectToDB(String l, String p, String uri) throws Exception {

		this.setLogin(l);
		this.setPassword(p);
		this.setUri(uri);

		return this.connectToDB();

	}

	public boolean connectToDB() throws Exception {

		Class.forName("com.mysql.jdbc.Driver").newInstance();

		if (getUri() == null)
			throw new Exception("No database specified");
		else if( !getUri().startsWith("jdbc:mysql://localhost/"))
			setUri("jdbc:mysql://localhost/"
					+ getUri().substring(getUri().lastIndexOf("/") + 1,
							getUri().length()));

		Class.forName("com.mysql.jdbc.Driver").newInstance();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// NOTE:
		// The 'useOldAliasMetadataBehavior=true' setting
		// is included to permit use of aliases within
		// ResultSetMetaData processing (which was changed since
		// VPDMf was originally developed).
		dbConnection = DriverManager.getConnection(getUri()
				+ "?user=" + this.getLogin() + "&password="
				+ this.getPassword() + "&useOldAliasMetadataBehavior=true");

		if (dbConnection == null) {
			throw new VPDMfException("Can't connect to db: " + getUri());
		}
		
		this.stat = dbConnection.createStatement(
				ResultSet.TYPE_FORWARD_ONLY, 
				ResultSet.CONCUR_READ_ONLY);
		this.stat.setFetchSize(Integer.MIN_VALUE);
		
		return true;

	}

	public void closeDbConnection() throws Exception {

		if( stat != null ) {
			
			this.stat.close();
			this.stat = null;
		
		} else {
			
			logger.warn("Closing connection when the 'stat' is null, probably has an error in the query.");
			
		}
		
		this.dbConnection.close();

	}

	/**
	 * @todo ???
	 * @param cI
	 *            ClassInstance
	 * @param conditionString
	 *            String
	 * @throws SQLException
	 * @return ResultSet
	 */
	protected ResultSet ExistInDB(ClassInstance cI, String conditionString)
			throws Exception {
		String selectString = "SELECT";

		Iterator attIt = cI.getDefinition().getAttributes().iterator();
		while (attIt.hasNext()) {
			UMLattribute aD = (UMLattribute) attIt.next();

			if (selectString == "") {
				selectString = "SELECT ";
			} else {
				selectString += ",";
			}
			selectString += aD.getBaseName();

		}

		selectString += " FROM " + cI.getDefinition().getBaseName()
				+ conditionString;

		long t = System.currentTimeMillis();
		ResultSet rs = this.executeQueryOnStatement(stat, selectString);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DatabaseEngine, Existence check: " + deltaT
					/ 1000.0 + " s\n");

		return (rs);

	}
	
	protected String buildNonSelectPartOfSQLStatement(boolean includeLimit) throws VPDMfException {

		if (tableAlias.size() == 0) {
			throw new VPDMfException("No tables are identified");
		}

		String fromSql = " FROM " + buildSQLFromClause();

		String whereSql = "";
		if (sqlConditions.size() > 0) {
			whereSql = " WHERE " + buildSQLWhereClause();
		}

		String orderSql = "";
		if (this.orderBy.size() > 0) {
			orderSql = " ORDER BY " + (String) this.orderBy.get(0);

			for (int i = 1; i < this.orderBy.size(); i++) {
				orderSql += ", " + (String) this.orderBy.get(i);
			}

		}

		String sql = fromSql + whereSql + orderSql;

		if (this.verbose) {
			this.prettyPrintSQL(sql);
		}
		
		if (isDoPagingInQuery() && includeLimit) {
			sql += " LIMIT " + getListOffset() + ", " + getListPageSize() + "\n";		
		}

		if (lc)
			sql = sql.toLowerCase();
		
		return sql;
	
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @return String
	 */
	protected String buildSQLSelectClause() {
		
		String distinctString = "";
		if ((!reducible && tableAlias.size() > 1) || tableAlias.size() == 1)
			distinctString = "DISTINCT ";
		
		String selectString = "SELECT " + 
				distinctString;
		
		boolean comma = false;
		for( String temp : this.selectHeader ) {
			
			if(comma)
				selectString += ", ";
			
			selectString += temp;
			comma = true;

		}
		return selectString;
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @return String
	 */
	protected String buildSQLFromClause() {
		Iterator it = this.tableAlias.iterator();
		String fromString = "";
		while (it.hasNext()) {
			String alias = (String) it.next();
			if (fromString.length() > 0) {
				fromString += ", ";
			}
			fromString += alias;
		}
		return fromString;
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @return String
	 */
	protected String buildSQLWhereClause() {

		String whereSql = (String) sqlConditions.get(0);

		for (int i = 1; i < sqlConditions.size(); i++) {
			whereSql += " AND " + (String) sqlConditions.get(i);
		}

		return whereSql;

	}

	/**
	 * @todo DatabaseEngine
	 * @param ci
	 *            ClassInstance
	 * @param queryType
	 *            int
	 * @throws VPDMfException
	 */
	protected void buildSelectHeader(ClassInstance ci, int queryType)
			throws VPDMfException {
		String alias = this.getAlias(ci);

		if (ci.getDefinition().getBaseName().equals("p")
				&& !this.checkRoot(getLogin(), getPassword(), getUri())) {
			return;
		}

		if (queryType == LIST || queryType == COUNT) {

			Iterator it = ci.getDefinition().getPkArray().iterator();

			while (it.hasNext()) {
				UMLattribute ad = (UMLattribute) it.next();

				if (!ad.getToImplement())
					continue;

				AttributeInstance ai = (AttributeInstance) ci.attributes.get(ad
						.getBaseName());
				this.selectHeader.add(alias + "."
						+ ai.getDefinition().getBaseName());
			}

		} else if (queryType == UID) {

			Iterator it = ci.getDefinition().getAttributes().iterator();
			while (it.hasNext()) {
				UMLattribute ad = (UMLattribute) it.next();

				if (!ad.getToImplement())
					continue;

				AttributeInstance ai = (AttributeInstance) ci.attributes.get(ad
						.getBaseName());
				this.selectHeader.add(alias + "."
						+ ai.getDefinition().getBaseName());
			}

		}

		else if (queryType == NOALIAS) {

			Iterator it = ci.getDefinition().getAttributes().iterator();

			while (it.hasNext()) {
				UMLattribute ad = (UMLattribute) it.next();

				if (!ad.getToImplement())
					continue;

				AttributeInstance ai = (AttributeInstance) ci.attributes.get(ad
						.getBaseName());
				this.selectHeader.add(ci.getDefinition().getBaseName() + "."
						+ ai.getDefinition().getBaseName());
			}

		}

		else {

			throw new VPDMfException(
					"Can't build SelectHeaders without queryType"
							+ " set to either LIST or UID");

		}

	}

	/**
	 * Builds the SELECT clause for a VPDMf SQL statement from a HashSet of
	 * attribute addresses.
	 * 
	 * @todo DatabaseEngine
	 * 
	 * @param vi
	 *            ViewInstance
	 * @param addresses
	 *            HashSet
	 * @throws Exception
	 */
	protected void buildSelectHeader(ViewInstance vi, List addresses)
			throws Exception {

		Vector addrVec = new Vector(addresses);

		buildSelectHeader(vi, addrVec);

	}

	/**
	 * Builds the SELECT clause for a VPDMf SQL statement from a Vector of
	 * attribute addresses.
	 * 
	 * @todo DatabaseEngine
	 * 
	 * @param vi
	 *            ViewInstance
	 * @param addresses
	 *            Vector
	 * @throws Exception
	 */
	protected void buildSelectHeader(ViewInstance vi, Vector addresses)
			throws Exception {

		Iterator it = addresses.iterator();
		while (it.hasNext()) {
			String a = (String) it.next();

			if (a.length() > 0) {

				UMLattribute att = vi.readAttributeDefinition(a);
				PrimitiveDefinition pv = vi.readPrimitiveDefinition(a);

				String header = pv.getName() + "_0__"
						+ att.getParentClass().getBaseName() + "."
						+ att.getBaseName();

				if (lc)
					header = header.toLowerCase();

				this.selectHeader.add(header);

			}

		}

	}

	protected void buildSelectHeader(PrimitiveInstance pi, List addresses)
			throws VPDMfException {

		ViewInstance vi = (ViewInstance) pi.getGraph().getSubGraphNode();

		Iterator it = addresses.iterator();
		while (it.hasNext()) {
			String a = (String) it.next();

			if (a.length() > 0) {

				AttributeInstance ai = vi.readAttributeInstance(a, 0);
				String pDef1 = pi.getDefinition().getName();
				String pDef2 = ai.get_object().getPrimitive().getDefinition()
						.getName();
				if (pDef1.equals(pDef2)) {

					String header = pi.getDefinition().getName() + "_0__"
							+ ai.get_object().getDefinition().getBaseName()
							+ "." + ai.getDefinition().getBaseName();

					if (lc)
						header = header.toLowerCase();

					this.selectHeader.add(header);

				}

			}

		}

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pi
	 *            PrimitiveInstance
	 * @param queryType
	 *            int
	 * @throws VPDMfException
	 */
	protected void buildSelectHeader(PrimitiveInstance pi, int queryType)
			throws VPDMfException {

		if (queryType == this.NOPK 
				&& !this.isInSelectClause(pi)
				&& pi.isNull()) {
			return;
		}

		if (queryType == LIST || queryType == COUNT || queryType == RANK) {

			ClassInstance ci = (ClassInstance) pi.getObjects().get(
					pi.getDefinition().getPrimaryClass().getBaseName());

			Iterator it = ci.getDefinition().getPkArray().iterator();
			while (it.hasNext()) {
				UMLattribute ad = (UMLattribute) it.next();
				this.selectHeader.add(this.getAlias(ci) + "."
						+ ad.getBaseName());
			}
		} else if (queryType == UID || queryType == SUMMARY) {

			Iterator cIt = pi.getObjects().values().iterator();
			while (cIt.hasNext()) {
				ClassInstance ci = (ClassInstance) cIt.next();

				//
				// Password protection
				//
				if (ci.getDefinition().getBaseName().equals("p")
						&& !this.checkRoot(getLogin(), getPassword(), getUri())) {
					continue;
				}

				Iterator it = ci.getDefinition().getAttributes().iterator();
				while (it.hasNext()) {
					UMLattribute ad = (UMLattribute) it.next();

					if (!ad.getToImplement())
						continue;

					AttributeInstance ai = (AttributeInstance) ci.attributes
							.get(ad.getBaseName());
					this.selectHeader.add(this.getAlias(ci) + "."
							+ ai.getDefinition().getBaseName());
				}

			}

		} else {
			throw new VPDMfException(
					"Can't build SelectHeaders without queryType"
							+ " set to either LIST or UID");
		}

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pli
	 *            PrimitiveLinkInstance
	 * @param queryType
	 *            int
	 * @throws VPDMfException
	 */
	protected void buildSelectHeader(PrimitiveLinkInstance pli, int queryType)
			throws VPDMfException {
		PrimitiveInstance fromPi = (PrimitiveInstance) pli.getOutEdgeNode();
		PrimitiveInstance toPi = (PrimitiveInstance) pli.getInEdgeNode();

		this.buildSelectHeader(fromPi, queryType);
		this.buildSelectHeader(toPi, queryType);

		ClassInstance ci = pli.getLinkClass();
		if (ci != null) {

			Iterator it = ci.getDefinition().getAttributes().iterator();

			while (it.hasNext()) {
				UMLattribute ad = (UMLattribute) it.next();
				AttributeInstance ai = (AttributeInstance) ci.attributes.get(ad
						.getBaseName());
				this.selectHeader.add(this.getAlias(ci) + "."
						+ ai.getDefinition().getBaseName());
			}

		}

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param vi
	 *            ViewInstance
	 * @param extras
	 *            String
	 * @throws Exception
	 */
	protected void buildSelectHeader(ViewInstance vi, String extras)
			throws Exception {
		PrimitiveInstance pi = vi.getPrimaryPrimitive();

		buildSelectHeader(pi, DatabaseEngineImpl.LIST);

		ClassInstance ci = (ClassInstance) pi.getObjects().get(
				pi.getDefinition().getPrimaryClass().getBaseName());

		if (ci.attributes.containsKey("vpdmfLabel")) {
			this.selectHeader.add(this.getAlias(ci) + ".vpdmfLabel");
		}

		//
		// Add thumbnail as part of header statement if applicable
		//
		if (ci.attributes.containsKey("thumbnail")) {
			this.selectHeader.add(this.getAlias(ci) + ".thumbnail");
		}

		if (ci.attributes.containsKey("viewType")) {
			this.selectHeader.add(this.getAlias(ci) + ".viewType");
		}

		String[] addresses = extras.split("&");
		for (int i = 0; i < addresses.length; i++) {
			String a = addresses[i];
			if (a.length() > 0) {
				UMLattribute att = vi.readAttributeDefinition(a);
				PrimitiveDefinition pv = vi.readPrimitiveDefinition(a);

				String header = pv.getName() + "_0__"
						+ att.getParentClass().getBaseName() + "."
						+ att.getBaseName();

				if (lc)
					header = header.toLowerCase();

				this.selectHeader.add(header);
			}
		}

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param vi
	 *            ViewInstance
	 * @param queryType
	 *            int
	 * @throws VPDMfException
	 */
	protected void buildSelectHeader(ViewInstance vi, int queryType)
			throws VPDMfException {

		PrimitiveInstance pi = vi.getPrimaryPrimitive();
		this.buildSelectHeader(pi, queryType);

		if (queryType == DatabaseEngineImpl.SUMMARY) {

			Vector pVec = pi.readCardinalityOneConnectedPrimitives();
			Iterator pIt = pVec.iterator();
			while (pIt.hasNext()) {
				PrimitiveInstance p = (PrimitiveInstance) pIt.next();
				this.buildSelectHeader(p, queryType);
			}

		}

		if (queryType == LIST || queryType == COUNT) {

			ClassInstance ci = (ClassInstance) pi.getObjects().get(
					pi.getDefinition().getPrimaryClass().getBaseName());

			if (ci.attributes.containsKey("vpdmfLabel")) {
				this.selectHeader.add(this.getAlias(ci) + ".vpdmfLabel");
			}

			//
			// Add thumbnail as part of header statement if applicable
			//
			if (ci.attributes.containsKey("thumbnail")) {
				this.selectHeader.add(this.getAlias(ci) + ".thumbnail");
			}

			if (ci.attributes.containsKey("viewType")) {
				this.selectHeader.add(this.getAlias(ci) + ".viewType");
			}

		}

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 *       Builds the SQL conditions for the query engine. Returns 1 if data
	 *       was actually set (rather than just links) returns 0 if no data
	 *       conditions were placed in the query
	 * @param cI
	 *            the ClassInstance that is being processed
	 * @param queryType
	 *            the type of query being processed
	 */
	protected boolean buildSqlConditions(ClassInstance cI, int queryType) throws VPDMfException {
		boolean dataSet = false;
		String result = "";
		String temp = "";

		UMLclass cDef = cI.getDefinition();
		List<UMLattribute> pks = null;
		Vector fks = null;
		List<UMLattribute> idxVec = null;

		if (queryType == this.INDEXONLY) {
			idxVec = cI.buildIndexVector();
		} else if (queryType == this.NOPK || queryType == this.PKONLY) {
			pks = cDef.getPkArray();
		}

		String tableName = cDef.getBaseName();

		//
		// Note:
		//
		// We use aliases in all cases, except when specified by the
		// NOALIAS queryType. This is only used running a query on
		// a single table, removing ambiguity about names. This situation
		// typically occurs when we are updating data.
		//
		String aliasName = cI.getDefinition().getBaseName();
		if (queryType != this.NOALIAS)
			aliasName = this.getAlias(cI);

		if (cI.attributes.containsKey("vpdmfOrder")) {
			this.orderBy.add(aliasName + ".vpdmfOrder");
		}

		Iterator aDIt = cDef.getAttributes().iterator();
		ATTLOOP: while (aDIt.hasNext()) {
			UMLattribute aDefn = (UMLattribute) aDIt.next();

			if (!aDefn.getToImplement())
				continue;
			
			AttributeInstance ai = (AttributeInstance) cI.attributes.get(aDefn
					.getBaseName());

			// Let's cut to the chase.
			if (ai.getValue() == null) {
				continue;
			} 

			if ( !aDefn.getBaseName().equals("vpdmfLabel") &&
					aDefn.getType().getBaseName().equals("longString") ) {
				logger.debug("Ignoring query condition on long string attribute: " + aDefn.getBaseName());
				continue;				
			}
			
			if ((aDefn.getParentClass().getBaseName().equals("VpdmfUser") || aDefn
					.getParentClass().getBaseName().equals("p"))
					&& aDefn.getBaseName().equals("password")) {
				continue;
			}
			
			if ( aDefn.getStereotype() != null && 
					aDefn.getStereotype().equals("vpdmfOrder") ) {
				continue ATTLOOP;
			}

			if (queryType == this.INDEXONLY) {

				if (!idxVec.contains(aDefn))
					continue ATTLOOP;

			} else if (queryType == this.NOPK) {

				if (pks.contains(aDefn))
					continue ATTLOOP;

			} else if (queryType == this.PKONLY) {

				if (!pks.contains(aDefn))
					continue ATTLOOP;

			} else if (queryType == this.FKONLY) {

				String s = aDefn.getStereotype();

				//
				// Bugfix
				//
				if (s == null) {
					s = "";
				}

				if (!s.equals("FK"))
					continue ATTLOOP;

			}

			//
			// We introduce the situation where we can perform -OR- queries
			// by setting the value of the attribute instance to a String[] array
			// and setting the aI
			//
			// This is the only situation where this is permissible.
			//
			String cond = "";
			String[] qCodeArray = ai.getQueryCode();
			
			if (ai.getValue() instanceof String[]) {

				String[] strArray = (String[]) ai.getValue();
				
				cond = "(";
				for(int i=0; i<strArray.length; i++) {
					String c = strArray[i];
					String qc = qCodeArray[i];
					if (!cond.endsWith("(")) {
						if( ai.getAndOrCode().equals(AttributeInstance.OR) )
							cond += " OR ";
						else if( ai.getAndOrCode().equals(AttributeInstance.AND) )
							cond += " AND ";
						else 
							throw new VPDMfException("Multi-Data in " + ai.getAddress() + " incorrectly formed.");
					}
					cond += this.getSQLConditionString(aDefn, aliasName, c, qc);
				}
				cond += ")";

			} else if (ai.getValue() instanceof ObjectMatrix1D) {
			
				ObjectMatrix1D aa = (ObjectMatrix1D) ai.getValue();
				Object[] a = aa.toArray();
				
				cond = "(";
				for (int i = 0; i < a.length; i++) {
					Object o = a[i];
					String c = o.toString();
					String qc = qCodeArray[i];
					if (!cond.endsWith("(")) {
						cond += " OR ";
					}
					cond += this.getSQLConditionString(aDefn, aliasName, c, qc);
				}
				cond += ")";

			} else {

				String c = ai.readValueString();
				cond = this.getSQLConditionString(aDefn, aliasName, c, qCodeArray[0]);

			}

			if (!this.sqlConditions.contains(cond) && cond.length() > 0) {
				this.sqlConditions.add(cond);
				dataSet = true;
			}

		}

		return dataSet;

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param ad
	 *            UMLattribute
	 * @param aliasName
	 *            String
	 * @param c
	 *            String
	 * @return String
	 */
	protected String getSQLConditionString(
			UMLattribute ad, 
			String aliasName,
			String v,
			String qc) {

		String op = "=";
		if( qc.equals(AttributeInstance.GT) ) {
			op = ">";
		} else if( qc.equals(AttributeInstance.GT) ) {
			op = ">";
		} else if( qc.equals(AttributeInstance.GTEQ) ) {
			op = ">=";
		} else if( qc.equals(AttributeInstance.LT) ) {
			op = "<";
		} else if( qc.equals(AttributeInstance.LTEQ) ) {
			op = "<=";
		} else if( qc.equals(AttributeInstance.NOT) ) {
			op = "!=";
		} else if( qc.equals(AttributeInstance.LIKE) ) {
			op = "LIKE";
		} 		
				//
		// Be careful of strings that are too long
		//
		if (v == null || ad.getType().getBaseName().equalsIgnoreCase("image")
				|| ad.getType().getBaseName().equalsIgnoreCase("blob")) {
			return "";
		}

		//MT: Check to avoid errors with text truncation
		// This causes other bugs with vpdmfLabel strings... creating ambiguity.
		// need to put back the machineIndex or equivalent.
		//if (c.length() > 10000) {
		//	return "";
		//}

		String q = UMLDataConverters.getQuote(ad);

		String value = this.validateDataStatement(v);

		String eq = "";

		String cond = null;

		//
		// TODO: NEED TO CLEAN THIS UP AND GET RID OF THE INTERVAL QUERY MECHANISM.
		//
		Pattern pattern = Pattern.compile("(.*) \\.\\.\\. to \\.\\.\\. (.*)");
		Matcher matcher = pattern.matcher(value);
		if (matcher.find()) {
			String intervalFrom = matcher.group(1);
			String intervalTo = matcher.group(2);

			cond = aliasName + "." + ad.getBaseName() + ">=" + q + intervalFrom
					+ q;
			cond += " AND ";
			cond += aliasName + "." + ad.getBaseName() + "<=" + q + intervalTo
					+ "zzz" + q;

		} 
		//
		// TODO: Currently, we permit wildcard queries just by including a '%' character.
		// 			Need to formalize and improve this. 
		//
		else if (value.indexOf("%") != -1) {

			cond = aliasName + "." + ad.getBaseName() + " LIKE " + q + value + q;
		
		} else {
		
			cond = aliasName + "." + ad.getBaseName() + " " + op + " " + q + value + q;

		} 

		return cond;

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pi
	 *            PrimitiveInstance
	 * @return boolean
	 */
	protected boolean buildSqlConditions(PrimitiveInstance pi) throws VPDMfException {
		return this.buildSqlConditions(pi, this.ALL);
	}


	/**
	 * @todo DatabaseEngine
	 * @param pi
	 *            PrimitiveInstance
	 * @return boolean
	 */
	protected boolean isInSelectClause(PrimitiveInstance pi) {
		Iterator it = this.selectHeader.iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			String p = pi.getName() + "__";
			if (this.lc)
				p = p.toLowerCase();

			if (s.indexOf(p) != -1)
				return true;
		}
		return false;
	}

	protected boolean isInSelectClause(UMLclass cd) {
		Iterator it = this.selectHeader.iterator();
		while (it.hasNext()) {
			String s = (String) it.next();
			String c = "__" + cd.getBaseName();
			if (this.lc)
				c = c.toLowerCase();

			if (s.indexOf(c) != -1)
				return true;
		}
		return false;
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pI
	 *            PrimitiveInstance
	 * @param queryType
	 *            int
	 * @return boolean
	 */
	protected boolean buildSqlConditions(PrimitiveInstance pI, int queryType) throws VPDMfException {
		String tableName;
		String queryTableName;
		AttributeInstance aI;
		UMLattribute aD;
		String aliasName;
		boolean dataSet = false;

		ViewInstance vi = (ViewInstance) ((PrimitiveInstanceGraph) pI
				.getGraph()).getSubGraphNode();

/* Not sure why this was here. This skips setting the where clause 
 * connecting the INTERNAL classes within a primitive and causing errors. 
  		if (!this.isInSelectClause(pI) && !this.includePiInQuery(pI)
				&& queryType != this.PKONLY
				&& vi.getDefinition().getType() != ViewDefinition.LOOKUP)
			return false;*/

		Vector objVec = null;
		try {
			objVec = pI.readOrderedObjects();
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (int i = 0; i < objVec.size(); i++) {
			ClassInstance ci = (ClassInstance) objVec.get(i);
			boolean b1 = this.buildSqlConditions(ci, queryType);
			if (b1)
				dataSet = true;
		}

		Iterator rIt = pI.getDefinition().getInternalRoles().iterator();
		while (rIt.hasNext()) {
			UMLrole r = (UMLrole) rIt.next();

			Iterator fkIt = r.getFkArray().iterator();
			while (fkIt.hasNext()) {
				UMLattribute fk = (UMLattribute) fkIt.next();
				UMLattribute pk = fk.getPk();

				String pkAlias = pI.getName() + "__"
						+ pk.getParentClass().getBaseName();
				String fkAlias = pI.getName() + "__"
						+ fk.getParentClass().getBaseName();

				if (lc)
					pkAlias = pkAlias.toLowerCase();
				if (lc)
					fkAlias = fkAlias.toLowerCase();

				String cond = pkAlias + "." + pk.getBaseName() + "=" + fkAlias
						+ "." + fk.getBaseName();

				if (!this.sqlConditions.contains(cond)) {
					this.sqlConditions.add(cond);
				}

			}
		}

		Iterator cIt = pI.getDefinition().getClasses().iterator();
		while (cIt.hasNext()) {
			UMLclass c = (UMLclass) cIt.next();

			if (c.getParent() != null
					&& pI.getDefinition().getClasses().contains(c.getParent())) {

				List<UMLattribute> pkVec = c.getPkArray();
				List<UMLattribute> parentPkVec = c.getParent().getPkArray();

				for (int i = 0; i < pkVec.size(); i++) {
					UMLattribute pk = (UMLattribute) pkVec.get(i);
					UMLattribute parentPk = (UMLattribute) parentPkVec.get(i);

					String pkAlias = pI.getName() + "__"
							+ pk.getParentClass().getBaseName();
					String parentPkAlias = pI.getName() + "__"
							+ parentPk.getParentClass().getBaseName();

					if (lc)
						pkAlias = pkAlias.toLowerCase();
					if (lc)
						parentPkAlias = parentPkAlias.toLowerCase();

					String cond = pkAlias + "." + pk.getBaseName() + "="
							+ parentPkAlias + "." + parentPk.getBaseName();

					if (!this.sqlConditions.contains(cond)) {
						this.sqlConditions.add(cond);
					}

				}

			}

		}

		return dataSet;

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pli
	 *            PrimitiveLinkInstance
	 * @param queryType
	 *            int
	 * @throws VPDMfException
	 * @return boolean
	 */
	protected boolean buildSqlConditions(PrimitiveLinkInstance pli,
			int queryType) throws VPDMfException {
		boolean dataSet = false;
		String result = null;

		PrimitiveLink pvLink = pli.getPVLinkDef();

		PrimitiveInstance inInstance = (PrimitiveInstance) pli.getInEdgeNode();

		PrimitiveInstance outInstance = (PrimitiveInstance) pli
				.getOutEdgeNode();

		boolean outOk = this.buildSqlConditions(outInstance, queryType);
		boolean inOk = this.buildSqlConditions(inInstance, queryType);

		// Used in general query and in list query
		if (queryType == this.NOPK
				&& (!this.isInSelectClause(inInstance) || !this
						.isInSelectClause(outInstance))
				&& (inInstance.isNull() || 
						outInstance.isNull())) {
			return false;
		}

		ClassInstance linkClassInstance = pli.getLinkClass();

		if (pli.getLinkClass() != null) {
			UMLclass linkClass = linkClassInstance.getDefinition();

			this.buildSqlConditions(linkClassInstance, queryType);

			UMLrole topRole = pvLink.getRole();

			// TODO: CHECK THIS
			Iterator<UMLrole> impIt = topRole.getImplementedBy().iterator();
			UMLrole role0 = impIt.next();
			UMLrole role1 = impIt.next();

			UMLrole inLinkClassRole = null;
			UMLrole inClassRole = null;
			UMLrole outClassRole = null;
			UMLrole outLinkClassRole = null;

			if (topRole.getDirectClass().equals(role0.getDirectClass())) {

				outClassRole = role1.otherRole();
				outLinkClassRole = role1;
				inLinkClassRole = role0.otherRole();
				inClassRole = role0;

			} else {

				outClassRole = role0.otherRole();
				outLinkClassRole = role0;
				inLinkClassRole = role1.otherRole();
				inClassRole = role1;

			}

			String inTableName = inInstance.getName() + "__"
					+ inClassRole.getDirectClass().getBaseName();

			String outTableName = outInstance.getName() + "__"
					+ outClassRole.getDirectClass().getBaseName();

			String linkClassName = linkClass.getBaseName() + "__"
					+ inInstance.getName() + "__" + outInstance.getName();

			if (lc) {
				inTableName = inTableName.toLowerCase();
				outTableName = outTableName.toLowerCase();
				linkClassName = linkClassName.toLowerCase();
			}

			List<UMLattribute> linkInAttr = inLinkClassRole.getFkArray();
			List<UMLattribute> linkOutAttr = outLinkClassRole.getFkArray();

			for (int i = 0; i < linkInAttr.size(); i++) {

				result = inTableName
						+ "."
						+ ((UMLattribute) linkInAttr.get(i)).getPk()
								.getBaseName() + "=" + linkClassName + "."
						+ ((UMLattribute) linkInAttr.get(i)).getBaseName();

				if (!this.sqlConditions.contains(result)) {
					this.sqlConditions.add(result);
				}

				result = outTableName
						+ "."
						+ ((UMLattribute) linkOutAttr.get(i)).getPk()
								.getBaseName() + "=" + linkClassName + "."
						+ ((UMLattribute) linkOutAttr.get(i)).getBaseName();

				if (!this.sqlConditions.contains(result)) {
					this.sqlConditions.add(result);
				}
			}

		} else {

			UMLrole role = pvLink.getRole();
			List<UMLattribute> fkVec = role.getFkArray();
			Iterator fkIt = fkVec.iterator();
			while (fkIt.hasNext()) {
				UMLattribute fk = (UMLattribute) fkIt.next();
				UMLattribute pk = fk.getPk();

				ArrayList<UMLclass> inClasses = inInstance.getDefinition()
						.getClasses();
				ArrayList<UMLclass> outClasses = outInstance.getDefinition()
						.getClasses();

				if (outClasses.contains(fk.getParentClass())) {

					result = inInstance.getName() + "__"
							+ pk.getParentClass().getBaseName() + "."
							+ pk.getBaseName() + "=" + outInstance.getName()
							+ "__" + fk.getParentClass().getBaseName() + "."
							+ fk.getBaseName();

				} else if (inClasses.contains(fk.getParentClass())) {

					result = outInstance.getName() + "__"
							+ pk.getParentClass().getBaseName() + "."
							+ pk.getBaseName() + "=" + inInstance.getName()
							+ "__" + fk.getParentClass().getBaseName() + "."
							+ fk.getBaseName();

				} else {
					throw new VPDMfException("Error in query build");
				}

				if (lc) {
					result = result.toLowerCase();
				}

			}

			if (!this.sqlConditions.contains(result)) {
				this.sqlConditions.add(result);
			}

		}

		// Only use this check in UID Query
		// if the source primitive has no data,
		// then we return false (and no data will be searched for)
		// if( queryType == this.PKONLY && !outOk ) {
		// return false;
		// }

		return true;

	}

	/**
	 * 
	 * A view is considered 'reducible' if and only if a) it is null (i.e. no
	 * data has been set) b) it only returns data from the view's primary class
	 * 
	 * @param vi
	 *            ViewInstance
	 * @return boolean
	 * @throws VPDMfException 
	 */
	protected boolean isReducible(ViewInstance vi) throws VPDMfException {

		if (vi.readPrimaryClass().getDefinition().getBaseName()
				.equals("ViewTable")
				&& vi.isNullOutsideOfViewtable()
				&& (vi.getDefinition().getType() == ViewDefinition.DATA
						|| vi.getDefinition().getType() == ViewDefinition.COLLECTION
						|| vi.getDefinition().getType() == ViewDefinition.EXTERNAL || vi
						.getDefinition().getType() == ViewDefinition.LOOKUP)) {

			Iterator pIt = vi.getSubGraph().getNodes().values().iterator();
			while (pIt.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) pIt.next();

				if (this.isInSelectClause(pi)) {

					if (!vi.getPrimaryPrimitive().equals(pi)) {

						return false;

					} else {

						Iterator cIt = pi.getDefinition().getClasses()
								.iterator();
						while (cIt.hasNext()) {
							UMLclass c = (UMLclass) cIt.next();
							if (this.isInSelectClause(c)
									&& !pi.getDefinition().getPrimaryClass()
											.equals(c))
								return false;
						}

					}

				}

			}

		} else {
			return false;
		}

		return true;

	}

	protected boolean buildSqlConditions(ViewInstance vi, int queryType)
			throws VPDMfException {
		return this.buildSqlConditions(vi, queryType, false);
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param vI
	 *            ViewInstance
	 * @param queryType
	 *            int
	 * @throws VPDMfException
	 * @return boolean
	 */
	protected boolean buildSqlConditions(ViewInstance vi, int queryType,
			boolean skipNulls) throws VPDMfException {

		boolean dataSet = false;

		if (this.isReducible(vi)) {

			this.reducible = true;
			if (this.buildSqlConditions(vi.getPrimaryPrimitive()
					.readPrimaryObject(), queryType))
				dataSet = true;
			
		} else {

			PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi
					.getSubGraph();
			SuperGraphTraversal pigTraversal = pig.readTraversal();
			Iterator piIt = pigTraversal.nodeTraversal.iterator();
			while (piIt.hasNext()) {

				PrimitiveInstance pi = (PrimitiveInstance) piIt.next();

				//
				// Bugfix:
				// Trim down the number of the primtives used to build the sql
				// conditions.
				// If the target primitive instance is null and we are allowed
				// to
				// skip null primitive, then skip it.
				//
				// No need to build the sql conditions for the 'pi' itself and
				// it's pli(s) if the current 'pi' is not included in the query
				// nor in the select clause.
				//
				// This would keep those pli(s) from being used to create table
				// aliases if the table alias of the current 'pi' is not built.
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				if (!this.isInSelectClause(pi) 
						&& pi.isNullExceptForFks()
						&& skipNulls) {
					continue;
				}
				// End Bugfix
				// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

				if (this.buildSqlConditions(pi, queryType))
					dataSet = true;

				Iterator pliIt = pi.getIncomingEdges().values().iterator();
				while (pliIt.hasNext()) {
					PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt
							.next();
					PrimitiveInstance otherPi = (PrimitiveInstance) pli
							.getOutEdgeNode();

					if (!this.isInSelectClause(otherPi)
							&& otherPi.isNull() 
							&& skipNulls) {

						continue;
					}

					if (this.buildSqlConditions(pli, queryType))
						dataSet = true;

				}

			}

		}

		return dataSet;

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param cI
	 *            ClassInstance
	 */
	protected void buildTableAliases(ClassInstance cI) {
		if (cI.getDefinition().getBaseName().equals("p")
				&& !this.checkRoot(getLogin(), getPassword(), getUri())) {
			return;
		}

		String tableName = cI.getDefinition().getBaseName();
		String aliasName = this.getAlias(cI);

		if (!this.tableAlias.contains(tableName + " AS " + aliasName)) {
			this.tableAlias.add(tableName + " AS " + aliasName);
		}

	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pI
	 *            PrimitiveInstance
	 * @param checkNullable
	 *            boolean
	 * @throws VPDMfException 
	 */
	protected void buildTableAliases(PrimitiveInstance pI, boolean checkNullable) throws VPDMfException {
		String tableName;
		String aliasName;

		ViewInstance vi = (ViewInstance) ((PrimitiveInstanceGraph) pI
				.getGraph()).getSubGraphNode();

		if (!this.isInSelectClause(pI) &&
				pI.isNull()
				&& checkNullable) {
			return;
		}

		Vector objVec = pI.readOrderedObjects();

		for (int i = 0; i < objVec.size(); i++) {
			ClassInstance cI = (ClassInstance) objVec.get(i);
			this.buildTableAliases(cI);
		}
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param pli
	 *            PrimitiveLinkInstance
	 * @param checkNullable
	 *            boolean
	 * @throws VPDMfException 
	 */
	protected void buildTableAliases(PrimitiveLinkInstance pli,
			boolean checkNullable) throws VPDMfException {
		ClassInstance linkClassInstance = pli.getLinkClass();

		if (pli.getLinkClass() != null) {

			String linkClassName = this.getAlias(linkClassInstance);

			tableAlias.add(linkClassInstance.getDefinition().getBaseName()
					+ " AS " + linkClassName);

		}

		PrimitiveInstance fromPi = (PrimitiveInstance) pli.getOutEdgeNode();
		PrimitiveInstance toPi = (PrimitiveInstance) pli.getInEdgeNode();

		this.buildTableAliases(fromPi, checkNullable);
		this.buildTableAliases(toPi, checkNullable);

	}

	protected void buildTableAliases(ViewInstance vI) throws VPDMfException {
		buildTableAliases(vI, false);
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param vi
	 *            ViewInstance
	 * @throws VPDMfException 
	 */
	protected void buildTableAliases(ViewInstance vi, boolean skipNulls) throws VPDMfException {
		int queryType = this.get_queryType();

		if (this.isReducible(vi)) {

			this.reducible = true;
			this.buildTableAliases(vi.getPrimaryPrimitive().readPrimaryObject());

		} else {

			PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi
					.getSubGraph();
			SuperGraphTraversal pigTraversal = pig.readTraversal();

			Iterator piIt = pigTraversal.nodeTraversal.iterator();
			while (piIt.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) piIt.next();

				//
				// Bugfix:
				//
				// Trim down the number of the primtives used to build the table
				// alias.
				//
				// If the target primitive instance is null and we are allowed
				// to skip null primitive, then skip it.
				//
				// No need to build the table alias for the 'pi' itself and
				// it's pli(s) if the current 'pi' is not included in the query
				// nor in the select clause.
				//
				// This would keep those pli(s) from being used to create table
				// aliases if the table alias of the current 'pi' is not built.
				// =================================================================
				if (!this.isInSelectClause(pi) 
						&& pi.isNullExceptForFks()
						&& skipNulls) {

					continue;
				}
				// End Bugfix
				// =================================================================

				this.buildTableAliases(pi, skipNulls);

				Iterator pliIt = pi.getIncomingEdges().values().iterator();
				while (pliIt.hasNext()) {
					PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt
							.next();

					this.buildTableAliases(pli, skipNulls);

				}

			}
		}

	}

	/**
	 * @todo DatabaseEngine
	 */
	protected void clearQuery() {
		this.reducible = false;
		this.selectHeader = new ArrayList<String>();
		this.tableAlias = new ArrayList<String>();
		this.sqlConditions = new ArrayList<String>();
		this.orderBy = new ArrayList<String>();
	}

	/**
	 * @todo ???
	 * 
	 * @param conditions
	 *            Vector
	 * @param connector
	 *            String
	 * @return String
	 */
	protected String connectConditions(Vector conditions, String connector) {
		String result = "";
		if (conditions.size() > 0)
			result = (String) conditions.get(0);
		for (int i = 1; i < conditions.size(); i++) {
			result += connector + (String) conditions.get(i);
		}
		return result;

	}

	/**
	 * @todo ???
	 * @param vi
	 *            ViewInstance
	 */
	protected void dbGarbageCollection(ViewInstance vi) {

	}

	public List<ClassInstance> queryClass(ClassInstance obj, String selectHeader)
			throws Exception {

		this.clearQuery();

		ResultSet rs = this.queryObject(obj, selectHeader);
		List<ClassInstance> objects = new ArrayList<ClassInstance>();

		while( rs.next() ) {
			ClassInstance ci = new ClassInstance(obj.getDefinition());
			this.updateObjectFromDb(ci, rs);
			objects.add(ci);
		}

		return objects;

	}

	public Vector queryClass(ClassInstance obj, Vector selectHeaders)
			throws Exception {

		this.clearQuery();

		ResultSet rs = this.queryObject(obj, selectHeaders);
		Vector objects = new Vector();

		while( rs.next() ) {
			ClassInstance ci = new ClassInstance(obj.getDefinition());
			this.updateObjectFromDb(ci, rs);
			objects.add(ci);
		}

		return objects;

	}

	public List<ClassInstance> queryClass(ClassInstance obj) throws Exception {

		this.clearQuery();

		ResultSet rs = this.queryObject(obj);
		List<ClassInstance> objects = new ArrayList<ClassInstance>();

		while( rs.next() ) {
			ClassInstance ci = new ClassInstance(obj.getDefinition());
			this.updateObjectFromDb(ci, rs);
			objects.add(ci);
		}

		return objects;

	}

	protected String getAlias(ClassInstance ci) {
		String alias = null;

		if (ci.getPrimitive() != null) {

			PrimitiveInstance pi = ci.getPrimitive();
			alias = pi.getName() + "__" + ci.getDefinition().getBaseName();

		} else if (ci.getPrimitiveLinkInstance() != null) {

			PrimitiveLinkInstance pli = ci.getPrimitiveLinkInstance();

			PrimitiveInstance inInstance = (PrimitiveInstance) pli
					.getInEdgeNode();

			PrimitiveInstance outInstance = (PrimitiveInstance) pli
					.getOutEdgeNode();

			ClassInstance linkClassInstance = pli.getLinkClass();

			UMLclass linkClass = ci.getDefinition();

			alias = linkClass.getBaseName() + "__" + inInstance.getName()
					+ "__" + outInstance.getName();

		} else {

			alias = ci.getDefinition().getBaseName();

		}

		if (lc)
			alias = alias.toLowerCase();

		return alias;

	}

	protected void fillInVGILinks_local(HashSet addedNodes) throws Exception {

/*		Vector uids = new Vector();
		Iterator gviIt = addedNodes.iterator();
		while (gviIt.hasNext()) {
			ViewInstance gvi = (ViewInstance) gviIt.next();
			String s = gvi.getUIDString();
			String uid = s.substring(s.indexOf("=") + 1, s.length());
			if (uid.length() > 0)
				uids.add(uid);
		}

		//
		// Proxy nodes do not have uids set
		//
		if (uids.size() == 0) {
			return;
		}

		UMLpackage cat = this.vpdmf.getUmlModel().getTopPackage();
		String addr = cat.getPkgAddress() + ".ViewLinkTable";

		UMLclass cd = (UMLclass) this.vpdmf.getUmlModel()
				.lookupClass("ViewLinkTable").iterator().next();
		ClassInstance ci = new ClassInstance(cd);
		AttributeInstance ai = (AttributeInstance) ci.attributes.get("from_id");
		ai.setValue(uids);

		ai = (AttributeInstance) ci.attributes.get("to_id");
		ai.setValue(uids);

		List<ClassInstance> linkVec = queryClass(ci);

		linksToAdd.addAll(linkVec);*/

	}

	protected Object getDataFromRS(ResultSet rs, Object att) throws Exception {
		UMLattribute ad = null;
		if (att instanceof UMLattribute) {
			ad = (UMLattribute) att;
		} else if (att instanceof AttributeInstance) {
			ad = ((AttributeInstance) att).getDefinition();

		} else {
			throw new VPDMfException(
					"Input attribute should be UMLattribute or AttributeInstance!");
		}

		String type = ad.getType().getBaseName();
		Object data_obj = null;

		//
		// Identify the actual column to avoid the attribute naming ambiguity
		// - Input 'att' is UMLattribute : Use table and attribute name to
		// identify
		// the column within the resultset.
		// - Input 'att' is AttributeInstance : Only use attribute name to
		// identify
		// the column.
		//
		// - Find the actual column number.
		// - Retrieve the data from resultset using the column number.
		//
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnNum = 0;

		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String columnTableName = rsmd.getTableName(i);
			String columnName = rsmd.getColumnName(i);
			String attName = ad.getBaseName();

			//
			// Do not use alias to identify the attribute
			//
			if (att instanceof UMLattribute) {

				if (columnName.equalsIgnoreCase(attName)
						&& (columnTableName.endsWith("__"
								+ ad.getParentClass().getBaseName()) || columnTableName
									.endsWith("__"
											+ ad.getParentClass().getBaseName()
													.toLowerCase()))) {
					columnNum = i;
					break;
				}

				//
				// Use alias to identify the attribute
				//
			} else if (att instanceof AttributeInstance) {

				ClassInstance ci = ((AttributeInstance) att).get_object();

				// TODO THIS IS PROBABLY GOING TO CAUSE PROBLEMS
				// BUT WE CAN'T LOOK UP THE ALIAS OF THE TABLE FOR THE CLASS
				// INSTANCE
				// WE NEED TO LINK QUERIES UP TO ATTRIBUTE INSTANCES BY KEEPING
				// TRACK OF
				// ATTRIBUTE'S POSITION IN THE QUERY AND THE RESULT SET.
				String className = this.getAlias(ci);

				if (columnTableName.startsWith(className)
						&& columnName.equalsIgnoreCase(attName)) {

					columnNum = i;
					break;

				}
			}
		}

		if (columnNum == 0) {
			return null;
		}

		if (type.equals("int")) {

			Integer data_int = new Integer(rs.getInt(columnNum));
			data_obj = (Object) data_int;

		} else if (type.equals("long") || type.equals("serial")) {

			Long data_long = new Long(rs.getInt(columnNum));
			data_obj = (Object) data_long;

		} else if (type.equals("float")) {

			Float data_float = new Float(rs.getFloat(columnNum));
			data_obj = (Object) data_float;

		} else if (type.equals("double")) {

			Double data_double = new Double(rs.getDouble(columnNum));
			data_obj = (Object) data_double;

		} else if (type.equals("boolean")) {

			Boolean data_boolean = new Boolean(rs.getBoolean(columnNum));
			data_obj = (Object) data_boolean;

		} else if (type.equals("short")) {

			Short data_short = new Short(rs.getShort(columnNum));
			data_obj = (Object) data_short;

		} else if (type.equals("char")) {

			String data = rs.getString(columnNum);
			data_obj = (Object) data.substring(0, 0);

		} else if (type.equals("String") || type.equals("url")
				|| type.startsWith("longString")) {

			String data = rs.getString(columnNum);

			//
			// The database sometimes returns '\r' inside the data...
			// ...Strip this out...
			//
			if (data != null) {
				while (data.indexOf('\r') != -1) {
					int p = data.indexOf('\r');
					int ln = data.length();
					String temp = data.substring(0, p);
					if (p + 1 < ln) {
						temp += data.substring(p + 1, ln);
					}
					data = temp;
				}
			}

			data_obj = (Object) data;

		} else if (type.equals("date") || type.equals("year")
				|| type.equals("time") || type.equals("timestamp")) {

			SimpleDateFormat df = null;
			ParsePosition pos = new ParsePosition(0);

			if (type.equals("timestamp")) {

				//
				// Note
				//
				// in MySQL, timestampe values IN THE DATABASE have the format
				// 'yyyyMMddhhmmss'
				//
				// - but - rs.getString for this data type
				// returns data in the format 'yyyy-MM-dd hh:mm:ss'
				//
				// Also, output needs to be 24 hour format.
				//
				// CHECK EACH DATA TYPE INPUT & OUTPUT
				//
				// BUT... with the MySQL JDBC connection, timestamp values are
				// returned in the native yyyyMMddhhmmss state...
				//
				// For MySQL version prior to 4.1 => yyyyMMddHHmmss
				// For MySQL version 4.1 => yyyy-MM-dd HH:mm:ss
				//
				df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				// df = new SimpleDateFormat ("yyyyMMddHHmmss");
			} else if (type.equals("time")) {
				df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			} else if (type.equals("date")) {
				df = new SimpleDateFormat("yyyy-MM-dd");
			} else if (type.equals("year")) {
				df = new SimpleDateFormat("yyyy");
			}

			String data = rs.getString(columnNum);

			if (data == null) {
				data_obj = null;
			} else {

				Date data_date = df.parse(data, pos);
				data_obj = (Object) data_date;

				if (data_date == null) {
					throw new VPDMfException("Data in"
							+ ad.getParentClass().getBaseName() + "."
							+ ad.getBaseName() + " cannot be parsed as date");
				}

			}

		} else if (type.equals("image")) {

			InputStream in = rs.getBinaryStream(columnNum);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (in == null) {

				data_obj = null;

			} else {

				int c;
				while ((c = in.read()) != -1) {
					baos.write(c);
				}
				if (baos.size() == 0) {
					data_obj = null;
				} else {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							baos.toByteArray());
					BufferedImage img = ImageIO.read(bis);
					data_obj = img;
				}

			}

		} else if (type.equals("blob")) {

			InputStream in = rs.getBinaryStream(columnNum);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			if (in == null) {
				data_obj = null;
			} else {
				int c;
				while ((c = in.read()) != -1) {
					baos.write(c);
				}
				if (baos.size() == 0) {
					data_obj = null;
				} else {

					data_obj = (Object) baos.toByteArray();

				}

			}
		}
		else {

			throw new VPDMfException("Data type " + type + " not supported");

		}

		//
		// Check the recordSet we get from database is null or not.
		// (1) Doing this check will avoid the potential bug while geting
		// primitive type value from database, such as int and float ect.
		//
		// (2) For ex: rs.getInt(columnName) will return 0 if the value in that
		// column is null.
		//
		if (rs.wasNull()) {
			data_obj = null;
		}

		return data_obj;

	}

	protected Vector getFkStrings(ClassInstance ci) throws Exception {
		Vector fkStrings = new Vector();

		String ciName = ci.getDefinition().getBaseName();

		Iterator roleIt = ci.getDefinition().getDirectRoles().iterator();
		ROLELOOP: while (roleIt.hasNext()) {
			UMLrole role = (UMLrole) roleIt.next();

			String fkString = "";

			Iterator fkIt = role.getFkArray().iterator();
			while (fkIt.hasNext()) {
				UMLattribute fk = (UMLattribute) fkIt.next();

				if (fk.getParentClass() != ci.getDefinition()) {
					continue ROLELOOP;
				}

				AttributeInstance fki = (AttributeInstance) ci.attributes
						.get(fk.getBaseName());

				String fkName = fk.getBaseName();

				if (fkString != "") {
					fkString += "&";
				}
				fkString += "|" + ciName + "." + fkName + "=" + fki.getValue();
			}

			if (fkString != "") {
				fkStrings.add(fkString);
			}

		}

		return fkStrings;

	}

	protected Vector getFkStrings(PrimitiveInstance pi) throws Exception {
		Vector fkStrings = new Vector();

		Iterator ciIt = pi.readOrderedObjects().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();
			fkStrings.addAll(this.getFkStrings(ci));
		}

		return fkStrings;
	}

	protected String getPkString(ClassInstance ci) throws Exception {
		String pkString = "";

		String ciString = ci.getDefinition().getBaseName();

		Iterator pkIt = ci.getDefinition().getPkArray().iterator();
		while (pkIt.hasNext()) {
			UMLattribute pk = (UMLattribute) pkIt.next();
			AttributeInstance pki = (AttributeInstance) ci.attributes.get(pk
					.getBaseName());

			if (pki.getValue() == null) {
				return "";
			}

			String pkiString = pk.getBaseName();

			if (pkString != "") {
				pkString += "&";
			}
			pkString += "|" + ciString + "." + pkiString + "=" + pki.getValue();

		}

		return pkString;

	}

	protected String getPkString(PrimitiveInstance pi) throws Exception {
		String pkString = "";

		if (pi.getDefinition() == null) {
			ViewInstance vi = (ViewInstance) pi.getGraph().getSubGraphNode();
			String vdName = vi.getDefName();
			ViewDefinition vd = (ViewDefinition) this.vpdmf.getViews().get(
					vdName);
			pi.instantiateDefinition(vd);
		}

		String piString = pi.getDefName();

		try {

			String primaryName = pi.getDefinition().getPrimaryClass()
					.getBaseName();

			ClassInstance ci = (ClassInstance) pi.getObjects().get(primaryName);

			String ciString = ci.getDefinition().getBaseName();

			Iterator pkIt = ci.getDefinition().getPkArray().iterator();
			while (pkIt.hasNext()) {
				UMLattribute pk = (UMLattribute) pkIt.next();
				AttributeInstance pki = (AttributeInstance) ci.attributes
						.get(pk.getBaseName());

				if (pki.getValue() == null) {
					continue;
				}

				String pkiString = pk.getBaseName();

				if (pkString != "") {
					pkString += "&";
				}
				pkString += "|" + ciString + "." + pkiString + "="
						+ pki.getValue();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return pkString;

	}

	protected Vector getPkStrings(PrimitiveInstance pi) throws Exception {
		Vector pkStrings = new Vector();

		Iterator ciIt = pi.readOrderedObjects().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();
			String pks = this.getPkString(ci);
			if (pks != null) {
				pkStrings.add(pks);
			}
		}

		return pkStrings;
	}

	public Vector get_classesToFillIn() {
		return this.classesToFillIn;
	}

	public Connection get_dbConnection() {
		return this.dbConnection;
	}

	public Hashtable get_pigLookup() {
		return this.pigLookup;
	}

	public SuperGraphTraversal get_pigTraversal() {
		return this.pigTraversal;
	}

	public int get_queryType() {
		return this.queryType;
	}

	public boolean get_verbose() {
		return this.verbose;
	}

	public void prettyPrintSQL(String sql) {

		if (!logger.isDebugEnabled()) return;
		
		try {
			String s = "*** SQL CODE ***";
			Pattern patt = Pattern.compile(" AND ");
			Matcher matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" AND \n ");

			patt = Pattern.compile(" OR ");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" OR \n ");

			patt = Pattern.compile(" LIMIT ");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" \nLIMIT ");

			patt = Pattern.compile("(LIMIT \\d+\\s*),(\\s*\\d+)");
			matcher = patt.matcher(sql);
			if (matcher.find()) {
				sql = matcher.replaceAll(matcher.group(1) + "KMRGCOMMA"
						+ matcher.group(2));
			}

			// Bug workaround: [MT]
			//
			// Commented out the following bugfix because it
			// not always works and might engage in an infinity loop.
			//
			// -------------- Start of commented out bugfix
//			patt = Pattern.compile("(\\'.*?),(.*?\\')");
//			matcher = patt.matcher(sql);
//
//			//
//			// Bugfix:
//			//
//			// Replace the ',' character in the matcher object with 'KMRGCOMMA'
//			// string one by one. The replacement string for each matching should
//			// be the same as the original one (except that the ',' character will
//			// be replaced with 'KMRGCOMMA').
//			// =======================================================================
//			/*
//			 * if( matcher.find() ) { sql = matcher.replaceAll( matcher.group(1) +
//			 * "KMRGCOMMA" + matcher.group(2) ); }
//			 */
//			while (matcher.find()) {
//				sql = matcher.replaceFirst(matcher.group(1) + "KMRGCOMMA"
//						+ matcher.group(2));
//				matcher = patt.matcher(sql);
//			}
//			// =======================================================================
			// -------------- End of commented out bugfix

			patt = Pattern.compile(",");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(",\n");

			patt = Pattern.compile("KMRGCOMMA");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(",");

			patt = Pattern.compile(" WHERE ");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" \nWHERE ");

			patt = Pattern.compile(" FROM ");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" \nFROM ");

			patt = Pattern.compile(" LIMIT ");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" \nLIMIT ");

			patt = Pattern.compile(" ORDER BY ");
			matcher = patt.matcher(sql);
			sql = matcher.replaceAll(" \nORDER BY ");

			
			String[] array = sql.split("\\n");
			s += "\n";
			for (int i = 0; i < array.length; i++) {
				s += "    " + array[i] + "\n";
			}
			logger.debug(s);
			
		} catch (Throwable e) {
			logger.warn("Failed to process statement: " + sql, e);
		}

	}

	protected ResultSet queryIndexObject(ClassInstance cI) throws SQLException,
			Exception {
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) cI.getPrimitive()
				.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();

		Collection<AttributeInstance> aiVec = cI.attributes.values();
		int index = cI.getPrimitive().readIndex();
		UMLclass def = cI.getDefinition();

		String selectSql = " SELECT * FROM " + def.getBaseName();
		String whereSql = " WHERE ";

		boolean go = false;

		Iterator adIt = vi.getDefinition().getIndexElements().values()
				.iterator();
		while (adIt.hasNext()) {
			String addr = ((IndexElement) adIt.next()).getAttributeAddress();
			AttributeInstance ai = vi.readAttributeInstance(addr, index);
			UMLattribute ad = (UMLattribute) ai.getDefinition();

			if (whereSql != " WHERE ") {
				whereSql += " AND ";
			}

			if (aiVec.contains(ai)) {
				go = true;
				String q = UMLDataConverters.getQuote(ad);
				whereSql += " " + ad.getBaseName() + "=" + q
						+ ai.readValueString() + q;
			}

		}

		if (!go) {
			return null;
		}
		String sql = selectSql + whereSql + ";";

		if (this.lc)
			sql = sql.toLowerCase();

		long t = System.currentTimeMillis();
		ResultSet rs = this.executeQueryOnStatement(stat, sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DatabaseEngine, Query Index Object: "
					+ deltaT / 1000.0 + " s\n");

		return rs;

	}

	protected ResultSet queryObject(ClassInstance cI, String selectHeader)
			throws Exception {

		this.clearQuery();

		this.selectHeader.add(this.getAlias(cI) + "." + selectHeader);
		this.buildSqlConditions(cI, this.ALL);
		this.buildTableAliases(cI);
		String sql = buildSQLSelectClause() + 
				buildNonSelectPartOfSQLStatement(true);

		if (this.lc)
			sql = sql.toLowerCase();

		long t = System.currentTimeMillis();
		ResultSet rs = this.executeQueryOnStatement(stat, sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DatabaseEngine, Query Object: " + deltaT
					/ 1000.0 + " s\n");

		return rs;
	}

	/**
	 * @todo DatabaseEngine
	 * 
	 * @param cI
	 *            ClassInstance
	 * @param selectHeaders
	 *            Vector
	 * @throws Exception
	 * @return ResultSet
	 */
	protected ResultSet queryObject(ClassInstance cI, Vector selectHeaders)
			throws Exception {
		
		this.clearQuery();

		for (int i = 0; i < selectHeaders.size(); i++) {
			this.selectHeader.add(this.getAlias(cI) + "."
					+ (String) selectHeaders.get(i));
		}

		this.buildSqlConditions(cI, this.ALL);
		this.buildTableAliases(cI);
		
		String sql = buildSQLSelectClause() + 
				buildNonSelectPartOfSQLStatement(true);

		if (this.lc)
			sql = sql.toLowerCase();

		long t = System.currentTimeMillis();
		ResultSet rs = this.executeQueryOnStatement(stat, sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DatabaseEngine, Query Object: " + deltaT
					/ 1000.0 + " s\n");

		return rs;
	}

	protected ResultSet queryObject(ClassInstance cI) throws Exception {

		this.clearQuery();
		this.buildSelectHeader(cI, this.UID);
		this.buildSqlConditions(cI, this.ALL);
		this.buildTableAliases(cI);
		String sql = buildSQLSelectClause() + 
				buildNonSelectPartOfSQLStatement(true);

		if (this.lc)
			sql = sql.toLowerCase();
		
		int i=0;

		long t = System.currentTimeMillis();
		ResultSet rs = this.executeQueryOnStatement(stat, sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    Query Engine, Query Object: " + deltaT
					/ 1000.0 + " s\n");

		return rs;
	}

	public void set_classesToFillIn(Vector classesToFillIn) {
		this.classesToFillIn = classesToFillIn;
	}

	public void set_dbConnection(Connection dbConnection) {
		this.dbConnection = dbConnection;
	}

	public void set_pigLookup(Hashtable pigLookup) {
		this.pigLookup = pigLookup;
	}

	public void set_pigTraversal(SuperGraphTraversal pigTraversal) {
		this.pigTraversal = pigTraversal;
	}

	public void set_queryType(int queryType) {
		this.queryType = queryType;
	}

	public void set_stat(Statement stat) {
		this.stat = stat;
	}

	public void set_verbose(boolean verbose) {
		this.verbose = verbose;
	}

	protected void updateObjectFromDb(ClassInstance obj, ResultSet rs)
			throws Exception {
		this.updateObjectFromDb(obj, rs, this.ALL);
	}

	protected boolean updateObjectFromDb(ClassInstance obj, ResultSet rs,
			int code) throws Exception {

		boolean changesMade = false;

		UMLclass c = obj.getDefinition();

		Hashtable lclu = new Hashtable();
		Iterator it = c.getAttributes().iterator();
		while (it.hasNext()) {
			UMLattribute a = (UMLattribute) it.next();
			lclu.put(a.getBaseName().toLowerCase(), a.getBaseName());
		}

		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i = 1; i <= rsmd.getColumnCount(); i++) {
			String attName = rsmd.getColumnName(i);

			if (lclu.get(attName) != null) {
				attName = (String) lclu.get(attName);
			}

			String tableName = rsmd.getTableName(i);
			String alias = this.getAlias(obj);

			if (alias == null) {

				AttributeInstance ai = (AttributeInstance) obj.attributes
						.get(attName);

				Object o = this.getDataFromRS(rs, ai);

				if (!ai.hasValue(o))
					changesMade = true;

				UMLattribute ad = ai.getDefinition();

				if (code == this.PKONLY && !c.getPkArray().contains(ad)) {
					continue;
				}
				
				// We do not allow the value of a foreign key 
				// to be set to null here. This typically happens
				// when a foreign key is being filled in by a 
				// multi-view insert.
				if( ad.getStereotype() != null && 
						ad.getStereotype().equals("FK") && 
						o == null ) {
					continue;
				}
				
				ai.setValue(o);

			} else if (obj.attributes.containsKey(attName)
					&& tableName.startsWith(alias)) {

				AttributeInstance ai = (AttributeInstance) obj.attributes
						.get(attName);
				UMLattribute ad = ai.getDefinition();

				Object o = this.getDataFromRS(rs, ai);

				if (!ai.hasValue(o))
					changesMade = true;

				if (code == this.PKONLY && !c.getPkArray().contains(ad)) {
					continue;
				}

				// We do not allow the value of a foreign key 
				// to be set to null here. This typically happens
				// when a foreign key is being filled in by a 
				// multi-view insert.
				if( ad.getStereotype() != null && 
						ad.getStereotype().equals("FK") && 
						o == null ) {
					continue;
				}

				ai.setValue(o);

			}

		}

		return changesMade;

	}

	protected ClassInstance updatePrimitiveLinkFromDb(
			PrimitiveLinkInstance pli, ResultSet rs) throws SQLException {
		ClassInstance obj = null;

		try {

			obj = new ClassInstance(pli.getLinkClass().getDefinition());
			if (obj != null) {
				this.updateObjectFromDb(obj, rs);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return obj;

	}

	protected String validateDataStatement(String dataStatement) {

		if (dataStatement != null) {

			dataStatement = dataStatement.replaceAll("[']", "\\\\'");
			dataStatement = dataStatement.replaceAll("[\"]", "\\\\\"");

		} else {

			dataStatement = "";

		}

		return dataStatement;

	}

	public boolean checkRoot(String l, String p, String uri) {
		try {
			// @todo: when connectivity problems fixed with MySQL 4.1
			// reinstate remote connectivity, until then just use 'localhost'
			//
			String localHostName = InetAddress.getLocalHost().getHostName();

			String[] ss = uri.split("/");
			if (!ss[0].equals(localHostName) && !ss[0].equals("localhost"))
				return (false);

			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Connection testConnx = DriverManager
					.getConnection("jdbc:mysql://localhost/mysql?user=" + l
							+ "&password=" + p);
			testConnx.close();

		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public void checkLocalMySql() throws SQLException {
		String msg = null;

		try {
			// @todo: when connectivity problems fixed with MySQL 4.1
			// reinstate remote connectivity, until then just use 'localhost'
			// "jdbc:mysql://" + vsm.getLocalHostName() + ":3306/",

			Class.forName("com.mysql.jdbc.Driver").newInstance();
			Connection dbConnection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/", "", ""); // TODO:
																// VPDM.vpdmfLogin,
																// VPDM.vpdmfPassword);

		} catch (SQLException sqlE) {
			msg = sqlE.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("Serious Error checking local database");
			System.exit(-1);
		}

		if (msg == null) {
			return;
		}
		//
		// MySQL is not installed or is not running. Print a warning window with
		// instructions and quit application
		//
		// Note that the formats of these strings CHANGE with
		// different releases of MysQL.
		//
		else if (msg.toLowerCase().startsWith("access denied for user")) {

			String errorCode = "Error: MySQL permissions not synchronized";
			String suggestion = "MySQL database does not have correct permissions.\n"
					+ "Please login as root and the system will automatically\n"
					+ "install additional permissions to the database.";
			throw new MySqlPermissionException(msg, errorCode, suggestion);

		} else if (msg.toLowerCase().startsWith("communications link failure")) {

			String errorCode = "Error: MySQL database not found";
			String suggestion = "Can't locate a MySQL database.\n"
					+ "To install: please go to http://www.mysql.com/\n"
					+ "to download and install MySQL 4.1.\n"
					+ "To run MySQL from the command line, type >mysqld\n"
					+ "To install MySQL as a service, type >mysqld --install.";
			throw new MySqlNotAvailableException(msg, errorCode, suggestion);

		} else {

			logger.debug("Serious Error: checking local database..\n"
					+ msg);
			System.exit(-1);

		}

	}

	public void saveJarFromVPDMfDatabaseToFile(File targetLocation) throws Exception {

		String sql = "SELECT jar FROM KnowledgeBase where isRoot=1;";

		if (this.lc)
			sql = sql.toLowerCase();

		ResultSet rs = this.stat.executeQuery(sql);
		rs.next();		

		InputStream is = rs.getBinaryStream("jar");

		
		FileOutputStream fos = new FileOutputStream(targetLocation);
		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			fos.write(buf, 0, len);
		}
		fos.close();
		
		rs.close();

	}
	
	private VPDMf loadModelFromVPDMfDatabase() throws Exception {

		String countSql = "SELECT count(*) FROM KnowledgeBase where isRoot=1;";
		if (this.lc)
			countSql = countSql.toLowerCase();		
		
		ResultSet rs = this.stat.executeQuery(countSql);
		rs.next();
		int count = rs.getInt(1);
		if (count != 1)
			throw new VPDMfException("Non specific build");
		rs.close();
		
		String sql = "SELECT build FROM KnowledgeBase where isRoot=1;";

		if (this.lc)
			sql = sql.toLowerCase();		
		
		rs = this.stat.executeQuery(sql);
		rs.next();
		InputStream in = rs.getBinaryStream("build");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] byteArray = null;
		if (in == null) {
			byteArray = null;
		} else {
			int c;
			while ((c = in.read()) != -1) {
				baos.write(c);
			}
			if (baos.size() == 0) {
				byteArray = null;
			} else {
				byteArray = baos.toByteArray();
			}
		}

		VPDMf v = (VPDMf) VPDMfConverters.byteArrayToVPDMfObject(byteArray);

		rs.close();
		
		return v;

	}

	public VpdmfSpec loadSpecFromVPDMfDatabase() throws Exception {

		String sql = "SELECT groupId, artifactId, version FROM KnowledgeBase where isRoot=1;";

		if (this.lc)
			sql = sql.toLowerCase();

		ResultSet rs = this.stat.executeQuery(sql);
		rs.next();

		String groupId = rs.getString("groupId");
		String artifactId = rs.getString("artifactId");
		String version = rs.getString("version");

		VpdmfSpec vpdmfSpec = new VpdmfSpec();
		vpdmfSpec.setGroupId(groupId);
		vpdmfSpec.setArtifactId(artifactId);
		vpdmfSpec.setVersion(version);

		return vpdmfSpec;

	}

	public ClassLoader readClassLoader(File jarLocation) throws Exception {

		if (this.cl == null) {

			boolean needToClose = false;
			if (this.stat == null) {
				this.connectToDB();
				needToClose = true;
			}
			
			this.saveJarFromVPDMfDatabaseToFile(jarLocation);

			// Previous mechanism for loading the ClassLoader required
			URL url = jarLocation.toURI().toURL();
			URL[] urls = new URL[]{url};
			this.cl = new URLClassLoader(urls);

			if (needToClose)
				this.closeDbConnection();

		}

		return cl;

	}

	public VPDMf readTop() throws Exception {

		if (this.vpdmf == null) {

			boolean needToClose = false;
			if (this.stat == null) {
				this.connectToDB();
				needToClose = true;
			}

			this.vpdmf = loadModelFromVPDMfDatabase();

			if (needToClose)
				this.closeDbConnection();

		}

		return this.vpdmf;

	}

	public Map<String, Map<String, String>> readKbMetdata(String uri)
			throws Exception {

		Map<String, Map<String, String>> dbHash = new HashMap<String, Map<String, String>>();

		uri = uri.replaceFirst("\\/$", "");
		String dbName = uri.substring(uri.indexOf("/") + 1, uri.length());

		ResultSet rs = this.stat
				.executeQuery("select namespace, timestamp from " + dbName
						+ ".knowledgeBase;");

		rs.next();
		String namespace = rs.getString(1);
		String timestamp = rs.getString(2);
		// String description = rs.getString(3);

		rs.close();

		Map hm = new HashMap<String, String>();
		dbHash.put(".kbMetadata", hm);

		hm.put("dbName", dbName);
		hm.put("namespace", namespace);
		hm.put("timestamp", timestamp);
		// dbHash.put( "description", description);

		rs = this.stat.executeQuery("select count(*), viewtype from " + dbName
				+ ".ViewTable group by viewType order by viewType;");

		hm = new HashMap<String, String>();
		dbHash.put("viewCounts", hm);

		while( rs.next() ) {
			Integer count = new Integer(rs.getInt(1));
			String viewtype = rs.getString(2);
			hm.put(viewtype, count);
		}

		rs = this.stat.executeQuery("select count(*), linkType from " + dbName
				+ ".ViewLinkTable group by linkType order by linkType;");

		while( rs.next() ) {
			Integer count = new Integer(rs.getInt(1));
			String linktype = rs.getString(2);
			hm.put(linktype, count);
		}

		rs.close();
		
		return dbHash;

	}

	public List<Map<String, String>> listKbs() throws Exception {

		List<Map<String, String>> kbs = new ArrayList<Map<String, String>>();

		ResultSet rs = this.stat.executeQuery("show databases;");
		Vector allDbNames = new Vector();
		while( rs.next() ) {
			String dbName = rs.getString(1);
			allDbNames.add(dbName);
		}

		//
		// Find out which are legitimately VPDMf systems
		// & add it to the JComboBox
		//
		Object[] dbNameArray = allDbNames.toArray();
		Arrays.sort(dbNameArray);
		DBLOOP: for (int i = 0; i < dbNameArray.length; i++) {
			String dbName = (String) dbNameArray[i];

			Map<String, String> dbHash = new HashMap<String, String>();
			rs = this.stat.executeQuery("select namespace, timestamp from "
					+ dbName + ".knowledgeBase;");
			
			if (rs.next()) {

				String namespace = rs.getString(1);
				String timestamp = rs.getString(2);
				// String description = rs.getString(3);

				dbHash.put("dbName", dbName);
				dbHash.put("namespace", namespace);
				dbHash.put("timestamp", timestamp);

				kbs.add(dbHash);
			}
			
			rs.close();
			
		}

		return kbs;

	}

	public ViewInstance getLookupView() {
		return lookupView;
	}

	public void setLookupView(ViewInstance lookupView) {
		this.lookupView = lookupView;
	}

	public ViewInstance getViToCenterOn() {
		return viToCenterOn;
	}

	public void setViToCenterOn(ViewInstance viToCenterOn) {
		this.viToCenterOn = viToCenterOn;
	}

	public List<ViewInstance> getViewList() {
		return viewList;
	}

	public void setViewList(List<ViewInstance> viewList) {
		this.viewList = viewList;
	}

	public Hashtable getLookupAliases() {
		return lookupAliases;
	}

	public void setLookupAliases(Hashtable lookupAliases) {
		this.lookupAliases = lookupAliases;
	}

	public boolean isDoPagingInQuery() {
		return doPagingInQuery;
	}

	public void setDoPagingInQuery(boolean doPagingInQuery) {
		this.doPagingInQuery = doPagingInQuery;
	}

	public int getListOffset() {
		return listOffset;
	}

	public void setListOffset(int listOffset) {
		this.listOffset = listOffset;
	}

	public int getListPageSize() {
		return listPageSize;
	}

	public void setListPageSize(int listPageSize) {
		this.listPageSize = listPageSize;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public List<String> getLookupResults() {
		return lookupResults;
	}

	public void setLookupResults(List<String> lookupResults) {
		this.lookupResults = lookupResults;
	}

	public int getListCount() {
		return listCount;
	}

	public void setListCount(int listCount) {
		this.listCount = listCount;
	}

	public String getLookupIndex() {
		return lookupIndex;
	}

	public void setLookupIndex(String lookupIndex) {
		this.lookupIndex = lookupIndex;
	}

	public JComponent getLookupComponent() {
		return lookupComponent;
	}

	public void setLookupComponent(JComponent lookupComponent) {
		this.lookupComponent = lookupComponent;
	}

	public int getMaxReturnedInQuery() {
		return maxReturnedInQuery;
	}

	public void setMaxReturnedInQuery(int maxReturnedInQuery) {
		this.maxReturnedInQuery = maxReturnedInQuery;
	}

	public int getMaxNeighbors() {
		return maxNeighbors;
	}

	public void setMaxNeighbors(int maxNeighbors) {
		this.maxNeighbors = maxNeighbors;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

};
