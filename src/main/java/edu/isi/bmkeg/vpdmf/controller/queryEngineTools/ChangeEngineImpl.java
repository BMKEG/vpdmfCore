package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.uml.utils.UMLDataConverters;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.exceptions.InterruptException;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveLink;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewLink;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstanceGraph;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveLinkInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

/**
 * Class that makes changes to the contents of the database, by inserting,
 * editing or deleting data.
 * 
 * @author Gully APC Burns
 * @version 1.0.7
 */
public class ChangeEngineImpl extends QueryEngineImpl implements
		ChangeEngine {

	private static Logger logger = Logger.getLogger(ChangeEngineImpl.class);

	protected GarbageCollector garbageCol;

	public ChangeEngineImpl(String login, String password, String uri) {

		super(login, password, uri);

	}

	public ChangeEngineImpl() {
		super();
	}

	// ____________________________________________________________________________
	// Part XXX: ViewSpec-level queries
	//
	// These queries opeate on views as a whole. They all assume that a
	// jdbc connection has been established and a transaction initiated.
	// They are all designed to be called from within Swingworkers and
	// subthreads internal to the DatabaseEngine.
	//

	/**
	 * Deletes the current view from the database and returns a Vector of
	 * indexes of all Views that were deleted.
	 */
	public boolean executeDeleteQuery(ViewInstance vi) throws Exception {

		this.queryType = DELETE;

		if (vi.getDefinition().getType() == ViewDefinition.EXTERNAL
				|| vi.getDefinition().getType() == ViewDefinition.LOOKUP)
			return false;

		boolean commitFlag = true;
		PrimitiveInstance pi = null;

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi.getSubGraph();
		SuperGraphTraversal pigTraversal = pig.readTraversal();

		// Deletion loop, based on arrays to permit removal of items from
		// the underlying data structures.
		Object[] piArray = pigTraversal.nodeTraversal.toArray();
		for (int i = (piArray.length - 1); i >= 0; i--) {
			pi = (PrimitiveInstance) piArray[i];

			if (!pi.getDefinition().isEditable()) {
				continue;
			}

			if (pi.getDefinition().isNullable() && pi.isNull()) {
				continue;
			}

			deletePrimitiveLinksFromDB(pi);

			//
			// We put another try-catch block here to keep track of the
			// status of the deletion.
			//
			// For the deletion of the PRIMARY primitive, if an exception
			// of the foreign key constraint is thrown, then the whole
			// transaction needs to be rolled back.
			//
			// For those NON-PRIMARY primitives, we skip the exception and
			// continue the process without rolling back the transaction. At
			// the same time, a report will be printed out.
			//
			try {

				deletePrimitiveFromDB(pi);

			} catch (Exception ex) {

				if (pi.getDefinition().isPrimaryPrimitive()) {

					throw ex;

				} else {

					logger.debug("Con't delete ["
							+ pi.getDefinition().getView().getName() + "]"
							+ pi.getName());

				}
			}

		}

		return commitFlag;
	}

	protected void deletePrimitiveLinksFromDB(PrimitiveInstance pi)
			throws Exception {

		Set<SuperGraphEdge> edges = new HashSet<SuperGraphEdge>(pi.getOutgoingEdges()
				.values());
		edges.addAll(pi.getIncomingEdges().values());
		Iterator<SuperGraphEdge> pliIt = edges.iterator();

		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();
			this.deletePrimitiveLinkFromDB(pli);
		}

	}

	protected void deletePrimitiveLinkFromDB(PrimitiveLinkInstance pli)
			throws Exception {
		String deleteSql = this.buildSQLDeleteStatement(pli);

		if (deleteSql == null) {
			return;
		}

		this.prettyPrintSQL(deleteSql);

		if (this.lc)
			this.executeOnStatement(uStat, deleteSql.toLowerCase());
		else
			this.executeOnStatement(uStat, deleteSql);

	}

	protected void deletePrimitiveFromDB(PrimitiveInstance pi) throws Exception {
		Object[] ciArray = pi.readOrderedObjects().toArray();

		for (int i = ciArray.length - 1; i >= 0; i--) {
			ClassInstance ci = (ClassInstance) ciArray[i];

			deleteObject(ci);

		}

	}

	public void deleteObject(ClassInstance ci) throws VPDMfException, Exception {
		String deleteSql = null;
		if (ci.checkIsPkNull()) {
			deleteSql = this
					.buildSQLDeleteStatement(ci, DatabaseEngineImpl.ALL);
		} else {
			deleteSql = this.buildSQLDeleteStatement(ci);
		}

		if (deleteSql == null) {
			throw new VPDMfException(
					"Error generating sql to perform delete");
		}

		this.prettyPrintSQL(deleteSql);

		if (this.lc)
			this.executeOnStatement(uStat, deleteSql.toLowerCase());
		else
			this.executeOnStatement(uStat, deleteSql);
	
	}

	public void turnOffAutoCommit() throws Exception {

		this.dbConnection.setAutoCommit(false);

	}

	public void turnOnAutoCommit() throws Exception {

		this.dbConnection.setAutoCommit(true);

	}

	public void commitTransaction() throws Exception {

		this.dbConnection.commit();
		logger.info("*** transaction committed ***");

	}

	public void rollbackTransaction() throws Exception {

		this.dbConnection.rollback();
		logger.info("*** transaction rolled-back ***");

	}

	public boolean connectToDB() throws Exception {

		if (getUri() == null)
			throw new Exception("No database specified");
		else if (!getUri().startsWith("jdbc:mysql://localhost/"))
			setUri("jdbc:mysql://localhost/"
					+ getUri().substring(getUri().lastIndexOf("/") + 1,
							getUri().length()));

		Class.forName("com.mysql.jdbc.Driver").newInstance();

		dbConnection = DriverManager.getConnection(getUri() + "?user="
				+ getLogin() + "&password=" + getPassword()
				// this is included to permit use of aliases within
				// ResultSetMetaData processing (which was changed since
				// VPDMf was originally developed).
				+ "&useOldAliasMetadataBehavior=true");

		if (dbConnection == null) {
			throw new VPDMfException("Can't connect to db: " + getUri());
		}

		this.stat = dbConnection.createStatement(
				ResultSet.TYPE_FORWARD_ONLY, 
				ResultSet.CONCUR_READ_ONLY);
		this.stat.setFetchSize(Integer.MIN_VALUE);
		
		this.uStat = dbConnection.createStatement(
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

		this.stat.execute("SET FOREIGN_KEY_CHECKS = 1");
		this.uStat.execute("SET FOREIGN_KEY_CHECKS = 1");

		return true;

	}

	public boolean connectToDB(String l, String p, String uri) throws Exception {

		if (l == null)
			l = VPDMf.vpdmfLogin;

		if (p == null)
			p = VPDMf.vpdmfPassword;

		Class.forName("com.mysql.jdbc.Driver").newInstance();

		// @todo: when connectivity problems fixed with MySQL 4.1
		// reinstate remote connectivity, until then just use 'localhost'
		// if( this.checkRoot(login, password, uri) ) {
		uri = "localhost/"
				+ uri.substring(uri.lastIndexOf("/") + 1, uri.length());
		// }

		dbConnection = DriverManager.getConnection("jdbc:mysql://" + uri
				+ "?user=" + l + "&password=" + p
				// this is included to permit use of aliases within
				// ResultSetMetaData processing (which was changed since
				// VPDMf was originally developed).
				+ "&useOldAliasMetadataBehavior=true");

		if (dbConnection == null) {
			throw new VPDMfException("Can't connect to db: " + uri);
		}
		
		this.stat = dbConnection.createStatement(
				ResultSet.TYPE_FORWARD_ONLY, 
				ResultSet.CONCUR_READ_ONLY);
		this.stat.setFetchSize(Integer.MIN_VALUE);

		this.uStat = dbConnection.createStatement(
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

		this.stat.execute("SET FOREIGN_KEY_CHECKS = 1");
		this.uStat.execute("SET FOREIGN_KEY_CHECKS = 1");

		return true;

	}

	/**
	 * Inserts vi into the database.
	 */
	public long executeInsertQuery(ViewInstance vi) throws Exception {

		vi.updateIndexes();
		vi.reconstructIndexStrings();

		this.set_queryType(DatabaseEngineImpl.INSERT);
		this.setDoPagingInQuery(false);
		this.setListOffset(0);

		return this.executeQuery(vi);

	}

	/**
	 * @todo ChangeEngine
	 * 
	 * @param vi
	 *            ViewInstance
	 * @param login
	 *            String
	 * @param password
	 *            String
	 * @param uri
	 *            String
	 * @throws Exception
	 */
	protected long executeQuery(ViewInstance vi) throws Exception {

		PrimitiveInstance workInstance;
		PrimitiveLinkInstance workEdge;
		
		//
		// some primitive links are ordered...
		// keep track of this with a hashtable
		this.pvLinkOrder = new HashMap<PrimitiveLink, Integer>();

		int queryType = get_queryType();

		clearQuery();

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi.getSubGraph();

		SuperGraphTraversal pigTraversal = pig.readTraversal();

		Iterator<SuperGraphNode> piIt = pigTraversal.nodeTraversal.iterator();
		while (piIt.hasNext()) {

			workInstance = (PrimitiveInstance) piIt.next();

			if (workInstance.getDefinition().isNullable()
					&& workInstance.isNull() ) {
				continue;
			}
			
			updatePrimitive(workInstance);

			// check if the vpdmfId is set for this primitive.
			AttributeInstance ai = workInstance.readPrimaryObject().attributes.get("vpdmfId");
			if( ai.getValue() == null ) 
				continue;
			
			Iterator<SuperGraphEdge> pliIt = workInstance.getIncomingEdges().values().iterator();
			while (pliIt.hasNext()) {
				workEdge = (PrimitiveLinkInstance) pliIt.next();

				PrimitiveInstance nextPi = (PrimitiveInstance) workEdge
						.getOutEdgeNode();

				if (nextPi.getDefinition().isNullable() && nextPi.isNull()) {
					continue;
				}

				updatePrimitiveLinkInstance(workEdge);

			}

		}
		
		this.clearQuery();

		//
		// Bugfix:
		//
		// This bug primarily occurs when going from 'INSERT' to 'DISPLAY'
		// state. The 'edit' button is always disabled because the check
		// of view completion returns false. It is because the 'piTotals'
		// object in the view instance is not updated with the latest PI
		// information after the view instance is inserted into DB.
		//
		// ================================================================
		vi.reconstructPiTotals();
		// ================================================================

		vi.updateVpdmfId();

		//
		// Final changes involved in the update / insert
		// - Collect the garbage
		//
		if (queryType == UPDATE) {

			garbageCol.executeCleanup(vi);

		}
		
		return vi.getVpdmfId();

	}

	protected void buildPKLookupTable(PrimitiveInstance pi) throws Exception {
		Iterator pkStringIt = this.getPkStrings(pi).iterator();
		while (pkStringIt.hasNext()) {
			String pkString = (String) pkStringIt.next();
			if (pkString.length() > 0) {
				this.pigLookup.put(pkString, pi);
			}
		}
	}

	protected void buildLookupTable(PrimitiveLinkInstance pli) throws Exception {
		Vector piVec = new Vector();
		piVec.add((PrimitiveInstance) pli.getOutEdgeNode());
		piVec.add((PrimitiveInstance) pli.getInEdgeNode());

		Iterator piIt = piVec.iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();

			Iterator pkStringIt = this.getPkStrings(pi).iterator();
			while (pkStringIt.hasNext()) {
				String pkString = (String) pkStringIt.next();
				this.pliLookup.put(pkString, pli);
			}

			Iterator fkStringIt = this.getFkStrings(pi).iterator();
			while (fkStringIt.hasNext()) {
				String fkString = (String) fkStringIt.next();
				this.pliLookup.put(fkString, pli);
			}

		}

	}

	protected String buildSQLDeleteStatement(ClassInstance ci) throws Exception {
		return buildSQLDeleteStatement(ci, this.PKONLY);
	}

	protected String buildSQLDeleteStatement(ClassInstance ci, int type)
			throws VPDMfException {
		if (ci.getDefinition().getBaseName().equals("p")
				&& !this.checkRoot(getLogin(), getPassword(), getUri())) {
			return null;
		}

		String deleteSql = "DELETE FROM ";

		deleteSql += ci.getDefinition().getBaseName();

		deleteSql += " WHERE ";

		int conditionCount = 0;

		Iterator it = null;
		if (type == this.PKONLY) {
			it = ci.getDefinition().getPkArray().iterator();
		} else {
			it = ci.getDefinition().getAttributes().iterator();
		}

		while (it.hasNext()) {
			UMLattribute ad = (UMLattribute) it.next();
			AttributeInstance ai = (AttributeInstance) ci.attributes.get(ad
					.getBaseName());

			if (ai.getValue() != null) {

				if (!deleteSql.endsWith("WHERE ")) {
					deleteSql += " AND ";
				}

				// deleteSql +=
				// ai.get_definition().get_ParentClass().getBaseName() + ".";
				deleteSql += ai.getDefinition().getBaseName();
				deleteSql += " = ";
				String q = UMLDataConverters.getQuote(ai.getDefinition());
				deleteSql += q + ai.readValueString() + q;
				conditionCount++;
			}

		}

		if (conditionCount == 0) {
			return null;
		} else {
			return deleteSql;
		}

	}

	protected String buildSQLDeleteStatement(PrimitiveInstance pi)
			throws VPDMfException {

		String deleteSql = "DELETE ";

		if (pi.getObjects().size() > 1) {

			Iterator ciIt = pi.getObjects().values().iterator();
			while (ciIt.hasNext()) {
				ClassInstance ci = (ClassInstance) ciIt.next();

				if (!deleteSql.endsWith("DELETE ")) {
					deleteSql += ", ";
				}

				deleteSql += ci.getDefinition().getBaseName();

			}

		}

		deleteSql += " FROM ";

		Iterator ciIt = pi.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();

			if (!deleteSql.endsWith("FROM ")) {
				deleteSql += ", ";
			}

			deleteSql += ci.getDefinition().getBaseName();

		}

		deleteSql += " WHERE ";

		int conditionCount = 0;

		ciIt = pi.getObjects().values().iterator();
		while (ciIt.hasNext()) {
			ClassInstance ci = (ClassInstance) ciIt.next();

			Iterator pkIt = ci.getDefinition().getPkArray().iterator();
			while (pkIt.hasNext()) {
				UMLattribute pk = (UMLattribute) pkIt.next();
				AttributeInstance ai = (AttributeInstance) ci.attributes.get(pk
						.getBaseName());

				if (ai.getValue() != null) {

					if (!deleteSql.endsWith("WHERE ")) {
						deleteSql += " AND ";
					}

					deleteSql += ai.getDefinition().getParentClass()
							.getBaseName()
							+ ".";
					deleteSql += ai.getDefinition().getBaseName();
					deleteSql += " = ";
					String q = UMLDataConverters.getQuote(ai.getDefinition());
					deleteSql += q + ai.readValueString() + q;
					conditionCount++;
				}

			}

			Iterator joinIt = pi.getDefinition().getInternalRoles().iterator();
			while (joinIt.hasNext()) {
				UMLrole join = (UMLrole) joinIt.next();

				Iterator fkIt = join.getFkArray().iterator();
				while (fkIt.hasNext()) {
					UMLattribute fk = (UMLattribute) fkIt.next();
					UMLattribute pk = fk.getPk();

					deleteSql += " AND ";
					deleteSql += fk.getParentClass().getBaseName() + ".";
					deleteSql += fk.getBaseName();
					deleteSql += " = ";
					deleteSql += pk.getParentClass().getBaseName() + ".";
					deleteSql += pk.getBaseName();

				}
			}

			Iterator cdIt = pi.getDefinition().getClasses().iterator();
			while (cdIt.hasNext()) {
				UMLclass cd = (UMLclass) cdIt.next();

				if (cd.getParent() == null) {
					continue;
				} else if (pi.getDefinition().getClasses()
						.contains(cd.getParent())) {

					Object[] pks1 = cd.getPkArray().toArray();
					Object[] pks2 = cd.getParent().getPkArray().toArray();

					for (int i = 0; i < pks1.length; i++) {
						UMLattribute pk1 = (UMLattribute) pks1[i];
						UMLattribute pk2 = (UMLattribute) pks2[i];

						deleteSql += " AND ";
						deleteSql += pk1.getParentClass().getBaseName() + ".";
						deleteSql += pk1.getBaseName();
						deleteSql += " = ";
						deleteSql += pk2.getParentClass().getBaseName() + ".";
						deleteSql += pk2.getBaseName();

					}
				}
			}

		}

		if (conditionCount == 0) {
			return null;
		} else {
			return deleteSql;
		}

	}

	protected String buildSQLDeleteStatement(PrimitiveLinkInstance pli)
			throws VPDMfException {
		ClassInstance ci = pli.getLinkClass();

		if (ci == null) {
			return null;
		}

		String deleteSql = "DELETE FROM ";

		if (!deleteSql.endsWith("FROM ")) {
			deleteSql += ", ";
		}

		deleteSql += ci.getDefinition().getBaseName();

		deleteSql += " WHERE ";

		Iterator aiIt = ci.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();

			if (ai.getDefinition().getPk() == null) {
				continue;
			}

			if (ai.getValue() != null) {

				if (!deleteSql.endsWith("WHERE ")) {
					deleteSql += " AND ";
				}

				deleteSql += ai.getDefinition().getParentClass().getBaseName()
						+ ".";
				deleteSql += ai.getDefinition().getBaseName();
				deleteSql += " = ";
				String q = UMLDataConverters.getQuote(ai.getDefinition());
				deleteSql += q + ai.readValueString() + q;

			}

		}

		if (deleteSql.endsWith("WHERE ")) {
			return null;
		} else {
			return deleteSql;
		}

	}

	protected void buildUpdateSet(ClassInstance ci) throws VPDMfException {
		if (ci.getDefinition().getBaseName().equals("p")
				&& !this.checkRoot(getLogin(), getPassword(), getUri())) {
			return;
		}

		String tableName = ci.getDefinition().getBaseName();
		String aliasName = ci.getPrimitive().getName() + "__" + tableName;

		if (lc)
			aliasName = aliasName.toLowerCase();

		Iterator aDIt = ci.getDefinition().getAttributes().iterator();

		while (aDIt.hasNext()) {
			UMLattribute aD = (UMLattribute) aDIt.next();

			if (!aD.getToImplement())
				continue;

			AttributeInstance aI = (AttributeInstance) ci.attributes.get(aD
					.getBaseName());

			if (aI.getValue() != null) {

				String quote = UMLDataConverters.getQuote(aD);
				String condData = aI.readValueString();

				if (condData != null) {

					String cond = aliasName + "."
							+ UMLDataConverters.getQuote(aD) + "=" + quote
							+ this.validateDataStatement(condData) + quote;

					if (!this.sqlConditions.contains(cond)) {
						this.sqlConditions.add(cond);
					}
				}

			}

		}

	}

	protected void buildUpdateSet(PrimitiveInstance pi) throws VPDMfException {

		if (pi.isNull()) {
			return;
		}

		Vector objVec = null;
		objVec = pi.readOrderedObjects();

		for (int i = 0; i < objVec.size(); i++) {
			ClassInstance cI = (ClassInstance) objVec.get(i);
			this.buildUpdateSet(cI);
		}
	}

	protected void buildUpdateSet(PrimitiveLinkInstance pli)
			throws VPDMfException {
		ClassInstance linkClassInstance = pli.getLinkClass();

		if (pli.getLinkClass() != null) {
			this.buildUpdateSet(linkClassInstance);
		}
	}

	protected void clearNonKeys(PrimitiveInstance pi) {
		Iterator it = pi.readAttributes().iterator();
		while (it.hasNext()) {
			AttributeInstance ai = (AttributeInstance) it.next();
			String s = ai.getDefinition().getStereotype();
			if (s == null) {
				continue;
			}
			if (s.indexOf("PK") == -1 && s.indexOf("FK") == -1) {
				ai.setValue(null);
			}
		}

	}

	public long executeUpdateQuery(ViewInstance vi) throws Exception {

		if (this.garbageCol == null)
			throw new Exception(
					"Can't perform update, did not initialize garbage collector");

		vi.updateIndexes();

		vi.reconstructIndexStrings();

		Set<ViewDefinition> spawnVds = new HashSet<ViewDefinition>();

		ViewDefinition vd = this.vpdmf.getViews().get(vi.getDefName());
		Iterator<SuperGraphEdge> outLinkIt = vd.getOutgoingEdges().values()
				.iterator();
		while (outLinkIt.hasNext()) {
			ViewLink outLink = (ViewLink) outLinkIt.next();
			spawnVds.add((ViewDefinition) outLink.getOutEdgeNode());
		}

		Iterator<SuperGraphEdge> inLinkIt = vd.getIncomingEdges().values().iterator();
		while (inLinkIt.hasNext()) {
			ViewLink inLink = (ViewLink) inLinkIt.next();
			spawnVds.add((ViewDefinition) inLink.getInEdgeNode());
		}

		this.set_queryType(DatabaseEngineImpl.UPDATE);
		this.setDoPagingInQuery(false);
		this.setListOffset(0);

		return this.executeQuery(vi);

	}

	public void initGarbageCollector(ViewInstance vi) {
		this.garbageCol = new GarbageCollector(vi);
	}

	public boolean insertObjectIntoDB(ClassInstance obj) throws Exception {

		this.clearQuery();
		
		String insertSql = "INSERT INTO " + obj.getDefinition().getBaseName();
		List<AttributeInstance> pks = new ArrayList<AttributeInstance>();
		List<AttributeInstance> attVec = new ArrayList<AttributeInstance>();

		for( AttributeInstance ai : obj.attributes.values()) {
			UMLattribute aD = ai.getDefinition();
			
			if (ai.getValue() == null) {
				continue;
			}

			if (aD.getType().getBaseName().equals("serial")) {

				pks.add(ai);

			} else {

				attVec.add(ai);

			}

		}

		String attNames = "";
		String questions = "";
		for (int i = 0; i < attVec.size(); i++) {
			AttributeInstance att = (AttributeInstance) attVec.get(i);
			UMLattribute aD = att.getDefinition();
			if (attNames.length() > 0) {
				attNames += ",";
				questions += ",";
			}
			attNames += aD.getBaseName();
			questions += "?";
		}

		insertSql += " (" + attNames + ") VALUES (" + questions + ");";

		this.prettyPrintSQL(insertSql);

		if (this.lc)
			insertSql = insertSql.toLowerCase();

		PreparedStatement psmt = this.dbConnection.prepareStatement(insertSql);

		for (int i = 1; i <= attVec.size(); i++) {
			AttributeInstance ai = (AttributeInstance) attVec.get(i - 1);
			

	        //=========================================================
	        //  Bugfix:
	        //
	        //    The way to check if the current attribute data can be
	        //    converted into a string is different. A binary object
	        //    can return a string-based representation.
			if( !ai.readValueString().startsWith("BLOB:") &&
	            !ai.readValueString().startsWith("IMAGE:")) {
	        //=========================================================

				psmt.setString(i, ai.readValueString());

			} else {

				InputStream is = ai.readValueInputStream();
				psmt.setBinaryStream(i, is, is.available());

			}
		}

		psmt.execute();
		psmt.close();

		this.clearQuery();

		this.buildSelectHeader(obj, UID);

		//
		// If current object(class instance) has parent class,
		// which would be 'ViewTable', then we build sql condition
		// statement based on 'ALL' fields. Otherwise, we build
		// it based on 'NOPK' fields.
		//
		if (obj.getDefinition().getParent() != null) {
			this.buildSqlConditions(obj, ALL);
		} else {
			this.buildSqlConditions(obj, NOPK);
		}

		this.buildTableAliases(obj);

		String countSql = "SELECT COUNT(*) " + 
				this.buildNonSelectPartOfSQLStatement(false);
		int count = this.executeCountSql(countSql);

		if( count == 1 ) {
		
			String checkSql = buildSQLSelectClause() + 
				buildNonSelectPartOfSQLStatement(true);
		
			this.prettyPrintSQL(checkSql);

			if (this.lc)
			checkSql = checkSql.toLowerCase();

			ResultSet rs = this.executeQueryOnStatement(stat, checkSql);

			rs.next();
			this.updateObjectFromDb(obj, rs, PKONLY);
		
			rs.close();
			
		} else if( count > 1) {

			throw new VPDMfException("Ambiguous data in DB after insertion");
			
		} else {

			throw new VPDMfException("Insertion failed");

		} 
		
		return true;

	}

	protected boolean updateDbFromObject(ClassInstance obj) throws Exception {
		ResultSet rs = this.queryObject(obj);

		return this.updateDbFromObject(obj, rs);

	}

	protected boolean updateDbFromObject(ClassInstance obj, ResultSet rs)
			throws Exception {

		Iterator aiIt = obj.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			UMLattribute atDef = ai.getDefinition();

		}

		if (this.isCancelled())
			throw new InterruptException("Thread has been cancelled");

		rs.updateRow();

		return true;
	}

	protected boolean updateDbFromPrimitive(PrimitiveInstance pi)
			throws Exception {

		this.clearQuery();
		this.buildTableAliases(pi, true);
		this.buildUpdateSet(pi);
		this.buildSqlConditions(pi);

		Object[] objArray = pi.readOrderedObjects().toArray();
		for (int i = objArray.length - 1; i >= 0; i--) {
			ClassInstance obj = (ClassInstance) objArray[i];
			this.updateObjectInDB(obj);
		}

		return true;

	}

	protected void updateObjectInDB(ClassInstance obj) throws Exception {
		boolean go = false;
		String sql = "";
		String temp = "";
		Vector pks = new Vector();
		Vector attVec = new Vector();

		sql = "UPDATE " + obj.getDefinition().getBaseName() + " SET ";

		Iterator aiIt = obj.attributes.values().iterator();
		while (aiIt.hasNext()) {
			AttributeInstance ai = (AttributeInstance) aiIt.next();
			UMLattribute aD = ai.getDefinition();

			if (ai.getValue() == null) {
				continue;
			}

			go = true;

			if (!sql.endsWith(" SET ")) {
				sql += ",";
			}

			sql += aD.getBaseName() + "=?";

			attVec.add(ai);
		}

		if (!go) {
			return;
		}

		sql += " WHERE ";

		Iterator adIt = obj.getDefinition().getPkArray().iterator();
		while (adIt.hasNext()) {
			UMLattribute aD = (UMLattribute) adIt.next();
			AttributeInstance ai = (AttributeInstance) obj.attributes.get(aD
					.getBaseName());

			String c = ai.readValueString();
			if (c == null) {
				continue;
			}

			String q = UMLDataConverters.getQuote(aD);

			if (!sql.endsWith(" WHERE ")) {
				sql += " AND ";
			}

			sql += aD.getBaseName() + "=" + q + validateDataStatement(c) + q;

		}

		if (this.lc)
			sql = sql.toLowerCase();

		this.prettyPrintSQL(sql);

		PreparedStatement psmt = this.dbConnection.prepareStatement(sql);

		try {

			for (int i = 1; i <= attVec.size(); i++) {
				AttributeInstance att = (AttributeInstance) attVec.get(i - 1);
				String type = att.getDefinition().getType().getBaseName();

				if (!type.equals("image") && !type.equals("blob")) {

					psmt.setString(i, att.readValueString());
					logger.debug(att.getDefinition().getBaseName() + ":" + type
							+ "=" + att.readValueString());

				} else {

					try {

						InputStream is = att.readValueInputStream();
						psmt.setBinaryStream(i, is, is.available());

					} catch (Exception ex) {

						throw new VPDMfException(ex.getMessage());

					}

				}
			}

			if (this.isCancelled())
				throw new InterruptException("Thread has been cancelled");

			psmt.executeUpdate();

		} finally {

			psmt.close();

		}
		
	}

	protected void updateObjectInDB(ClassInstance obj, Vector addrVec,
			Vector valueVec) throws Exception {

		boolean go = false;
		String sql = "";

		Vector aiVec = new Vector();
		Vector avVec = new Vector();

		sql = "UPDATE " + obj.getDefinition().getBaseName() + " SET ";

		if (obj.getPrimitive() == null)
			throw new Exception("Can't update objects in linking tables");

		for (int i = 0; i < addrVec.size(); i++) {
			String addr = (String) addrVec.get(i);

			int brk = addr.indexOf("]");
			int ln = addr.indexOf("|");
			int pt = addr.indexOf(".");
			String pName = addr.substring(brk + 1, ln);
			String cName = addr.substring(ln + 1, pt);
			String aName = addr.substring(pt + 1, addr.length());

			if (!obj.getPrimitive().getDefinition().getName().equals(pName)
					|| !obj.getDefinition().getBaseName().equals(cName)
					|| !obj.attributes.containsKey(aName))
				continue;

			go = true;

			if (!sql.endsWith(" SET ")) {
				sql += ",";
			}

			sql += aName + "=?";

			AttributeInstance ai = (AttributeInstance) obj.attributes
					.get(aName);

			aiVec.add(ai);
			avVec.add(valueVec.get(i));

			logger.debug("        " + aName + " = "
					+ valueVec.get(i).toString());

		}

		if (!go) {
			return;
		}

		sql += " WHERE ";

		Iterator adIt = obj.getDefinition().getAttributes().iterator();
		while (adIt.hasNext()) {
			UMLattribute aD = (UMLattribute) adIt.next();
			AttributeInstance ai = (AttributeInstance) obj.attributes.get(aD
					.getBaseName());

			Object o = ai.getValue();
			if (o == null)
				continue;

			String q = UMLDataConverters.getQuote(aD);

			if (!sql.endsWith(" WHERE ")) {
				sql += " AND ";
			}

			if (o instanceof Vector) {

				Vector v = (Vector) o;
				sql += " ( ";
				for (int i = 0; i < v.size(); i++) {
					if (!sql.endsWith(" ( "))
						sql += " OR ";
					sql += aD.getBaseName() + "=" + q
							+ validateDataStatement((String) v.get(i)) + q;
				}
				sql += " ) ";

			} else {

				String c = ai.readValueString();
				if (c == null)
					continue;

				sql += aD.getBaseName() + "=" + q + validateDataStatement(c)
						+ q;

			}
		}

		if (this.lc)
			sql = sql.toLowerCase();

		PreparedStatement psmt = this.dbConnection.prepareStatement(sql);

		for (int i = 0; i < aiVec.size(); i++) {
			AttributeInstance ai = (AttributeInstance) aiVec.get(i);
			ai.setValue(avVec.get(i));
			String type = ai.getDefinition().getType().getBaseName();

			if (!type.equals("image") && !type.equals("blob")) {

				psmt.setString(i + 1, ai.readValueString());

			} else {

				try {

					InputStream is = ai.readValueInputStream();
					psmt.setBinaryStream(i + 1, is, is.available());

				} catch (Exception ex) {

					throw new VPDMfException(ex.getMessage());

				}

			}
		}

		this.prettyPrintSQL(sql);

		psmt.executeUpdate();
		psmt.close();

	}

	protected boolean updatePrimitive(PrimitiveInstance pi) throws Exception {

		PrimitiveInstance copyPi = new PrimitiveInstance(pi.getDefinition());
		copyPi.suckInData(pi);

		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) pi.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();
		Vector objVec = pi.readOrderedObjects();

		this.clearQuery();
		this.buildSelectHeader(pi, DatabaseEngineImpl.UID);

		boolean isNull = pi.isNull();
		boolean isNullable = pi.getDefinition().isNullable();
		
		//
		// If we are currently updating the primary primitive of the view,
		// AND this is an update query,
		// then the entry in the database is identified by the PK and
		// the PK is the only data that we update in the returned primitive.
		//
		// TODO We might be able to expand the behavior of the query here.
		//     - we might be able to reason over the logic of how the data
		//       in the system is organized so that we can run other types
		//       of query.
		//
		boolean dataSet = false;
		if (this.get_queryType() == DatabaseEngineImpl.UPDATE
				&& vi.getPrimaryPrimitive() == pi) {

			dataSet = this.buildSqlConditions(pi, DatabaseEngineImpl.PKONLY);

		}
		//
		// If it does not contain any index elements
		// we need to build SQL conditions with 'NOPK' queryType.
		//
		else if (!pi.getDefinition().containsIndexElements()) {

			dataSet = this.buildSqlConditions(pi, DatabaseEngineImpl.NOPK);

		}
		//
		// Otherwise we identify the primitive in the database from the basis
		// the data's inclusion in the label of the view...
		//
		else {

			dataSet = this.buildSqlConditions(pi, DatabaseEngineImpl.INDEXONLY);

		}

		if( pi.isNullExceptForFks() ) {
			
			if (isNullable) {			
				if (!pi.getDefinition().arePrimitiveConditionsIndexElements()) {
					return false;
				}
			} else {
				throw new VPDMfException("no data is set in update query: "
						+ pi.getName());
			}
		}

		int pvCount = 0;

		this.buildTableAliases(pi, true);
		String sql = buildSQLSelectClause() + 
				buildNonSelectPartOfSQLStatement(true);

		ResultSet rs = null;
		if (dataSet) {
			
			String countSql = "SELECT COUNT(*) " + 
					this.buildNonSelectPartOfSQLStatement(false);
			pvCount = this.executeCountSql(countSql);
						
		}

		if (pvCount == 0) {

			logger.debug("        Inserting primitive " + pi.getName());

			rs = this.executeQueryOnStatement(stat, sql);
			rs.next();

			OBJECTLOOP: for (int i = 0; i < objVec.size(); i++) {
				ClassInstance obj = (ClassInstance) objVec.get(i);

				//
				// We only insert data if we are allowed to
				//
				if (pi.getDefinition().isEditable()) {
					this.insertObjectIntoDB(obj);
				} else if (!pi.getDefinition().isNullable()) {
					throw new VPDMfException(
							"Attempting to change a non editable primitive");
				}
			}
			
			rs.close();

		} else if (pvCount == 1) {

			rs = this.executeQueryOnStatement(stat, sql);
			rs.next();
			
			boolean changesMade = false;
			OBJECTLOOP: for (int i = 0; i < objVec.size(); i++) {
				ClassInstance obj = (ClassInstance) objVec.get(i);

				boolean b = false;
				if (this.get_queryType() == UPDATE) {

					b = this.updateObjectFromDb(obj, rs, PKONLY);

				} else {

					b = this.updateObjectFromDb(obj, rs, ALL);

				}
				if (b)
					changesMade = true;

			}
			
			rs.close();

			//
			// The data in this primitive has changed and
			// if we are allowed, we update it
			//
			if (changesMade) {
				if (pi.getDefinition().isEditable()) {
					this.updateDbFromPrimitive(pi);

					logger.debug("        Updating primitive " + pi.getName());
				}

			} else {

				if (this.verbose) {
					logger.debug("        Skipping primitive " + pi.getName());
				}

			}

		} else {

			throw new VPDMfException(
					"Database update ambiguous, more than one "
							+ "primitive has this index. pV => "
							+ pi.getDefinition().getName());

		}

		return true;

	}

	public boolean updatePrimitiveLinkInstance(PrimitiveLinkInstance pli)
			throws Exception
	{

		String sqlString;
		PrimitiveLink pvLink = pli.getPVLinkDef();
		ClassInstance lci = pli.getLinkClass();

		if (lci == null) {
			return false;
		}

		//
		// This primitive link is ordered.
		//
		// ~start~ Setting PvLink order
		String stereotype = pli.getPVLinkDef().getRole().getAss()
				.getStereotype();
		if (stereotype == null) {
			stereotype = "";
		}

		if (stereotype.indexOf("ordered") != -1) {

			AttributeInstance oai = (AttributeInstance) lci.attributes
					.get("vpdmfOrder");

			if( oai.getValue() == null ) {
				
				int order = 0;
				if (this.pvLinkOrder.containsKey(pli.getPVLinkDef())) {
					order = this.pvLinkOrder.get(pli.getPVLinkDef()).intValue();
					order++;
					
				}
				//
				// the direction of the primitive link should follow dependency
				// PK-to-FK
				// thus, the name of the appropriate linking attribute is :
				//
				// pvLink.get_role().get_name().
				//
				UMLrole r = pvLink.getRole();
				if (r.getImplementedBy().size() > 0) {
					r = (UMLrole) r.getImplementedBy().iterator().next();
				}
	
				oai.writeValueString(order + "");

				this.pvLinkOrder.put(pli.getPVLinkDef(), new Integer(order));
			
			}

		} // ~end~ Setting PvLink order

		// ________________________________________________________________________
		// HACK: completely remove all trace of the lci pk
		Iterator it = lci.getAttributes().values().iterator();
		while (it.hasNext()) {
			AttributeInstance ai = (AttributeInstance) it.next();
			if (ai.isPrimaryKey()) {
				ai.setValue(null);
			}
		}
		// ________________________________________________________________________

		this.clearQuery();
		this.buildSelectHeader(lci, this.UID);
		this.buildSqlConditions(lci, this.FKONLY);
		this.buildTableAliases(lci);
		String countSql = "SELECT COUNT(*) " + 
				buildNonSelectPartOfSQLStatement(false);
		String sql = buildSQLSelectClause() + 
				buildNonSelectPartOfSQLStatement(true);
		
		int count = this.executeCountSql(countSql);
		

		if ( count == 0 ) {

			this.insertObjectIntoDB(lci);

		} else if(count == 1) {

			ResultSet rs = this.executeQueryOnStatement(stat, sql);
			rs.next();
			this.updateObjectFromDb(lci, rs);
			rs.close();

			this.updateObjectInDB(lci);

			//
			// This primitive link instance has been updated,
			// so remove it from the garbage collector's
			// lookup table.
			//
			if (this.garbageCol != null) {
				// NEED TO CHECK THIS!!!
				// this.garbageCol.removeFromLookup(pli);
			}
		
		} else {
		
			throw new VPDMfException(
					"More than one entry in Linking table in DB");
		
		}	
		
		return true;

	}

	protected void updateDataInRS(ResultSet rs, UMLattribute ad, Object data_obj)
			throws Exception {

		String type = ad.getType().getBaseName();

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

			if (columnName.equalsIgnoreCase(attName)
					&& columnTableName.equalsIgnoreCase(ad.getParentClass()
							.getBaseName())) {
				columnNum = i;
				break;
			}
		}

		if (columnNum == 0) {
			return;
		}

		if (type.equals("int")) {

			int data_int = ((Integer) data_obj).intValue();
			rs.updateInt(columnNum, data_int);

		} else if (type.equals("long") || type.equals("serial")) {

			long data_long = ((Long) data_obj).longValue();
			rs.updateLong(columnNum, data_long);

		} else if (type.equals("float")) {

			float data_float = ((Float) data_obj).floatValue();
			rs.updateFloat(columnNum, data_float);

		} else if (type.equals("double")) {

			double data_double = ((Double) data_obj).doubleValue();
			rs.updateDouble(columnNum, data_double);

		} else if (type.equals("boolean")) {

			boolean data_boolean = ((Boolean) data_obj).booleanValue();
			rs.updateBoolean(columnNum, data_boolean);

		} else if (type.equals("int")) {

			short data_short = ((Short) data_obj).shortValue();
			rs.updateShort(columnNum, data_short);

		} else if (type.equals("char")) {

			String data = (String) data_obj;
			rs.updateString(columnNum, data.substring(0, 0));

		} else if (type.equals("longString") || type.equals("string")
				|| type.equals("url")) {

			String data = (String) data_obj;
			rs.updateString(columnNum, data);

		} else if (type.equals("date") || type.equals("year")
				|| type.equals("time") || type.equals("timestamp")) {

			/*
			 * @todo GULLY: need to fix this. SimpleDateFormat df = null;
			 * ParsePosition pos = new ParsePosition(0);
			 * 
			 * if( type.equals("timestamp") ){
			 * 
			 * // // Note // // in MySQL, timestampe values IN THE DATABASE have
			 * the format // 'yyyyMMddhhmmss' // // - but - rs.getString for
			 * this data type // returns data in the format 'yyyy-MM-dd
			 * hh:mm:ss' // // Also, output needs to be 24 hour format. // //
			 * CHECK EACH DATA TYPE INPUT & OUTPUT // // BUT... with the MySQL
			 * JDBC connection, timestamp values are // returned in the native
			 * yyyyMMddhhmmss state... // // df = new SimpleDateFormat
			 * ("yyyy-MM-dd HH:mm:ss"); df = new SimpleDateFormat
			 * ("yyyyMMddHHmmss"); } else if( type.equals("time") ) { df = new
			 * SimpleDateFormat ("yyyy-MM-dd HH:mm:ss"); } else if(
			 * type.equals("date") ) { df = new SimpleDateFormat ("yyyy-MM-dd");
			 * } else if( type.equals("year") ) { df = new SimpleDateFormat
			 * ("yyyy"); }
			 * 
			 * java.util.Date data_date = (java.util.Date) data_obj;
			 * rs.updateDate( columnNum, data_date);
			 * 
			 * String data = rs.getString(columnNum);
			 * 
			 * if( data == null ) { data_obj = null; } else {
			 * 
			 * 
			 * if( data_date == null ) { throw new VPDMfException( "Data in" +
			 * ad.get_ParentClass().getBaseName() + "." + ad.getBaseName() +
			 * " cannot be parsed as date"); }
			 * 
			 * }
			 */

		} else if (type.equals("image") || type.equals("blob")) {

		} else {

			throw new VPDMfException("Data type " + type + " not supported");

		}

	}

	public boolean executeDeleteQuery(String viewType, Long id) throws Exception {

		VPDMf top = this.readTop();

		ViewDefinition vd = top.getViews().get(viewType);

		this.stat.execute("set autocommit=0;");

		this.setDoPagingInQuery(false);
		this.setListOffset(0);
	
		ViewInstance vi = this.executeUIDQuery(vd, id);

		if( vi == null ) 
			return false;
		
		this.setListOffset(0);
		this.vpdmf = top;

		return this.executeDeleteQuery(vi);

	}

	/**
	 * (1) Check if the statement contains special character. (2) Add escape
	 * character if necessary.
	 */
	protected class GarbageCollector {

		protected ViewInstance copyVi;

		public void destroy() {
		}

		protected void executeCleanup(ViewInstance vi) throws Exception {

			logger.debug("\n        Update clean up.");

			Hashtable oldPigLookup = pigLookup;
			Hashtable oldPliLookup = pliLookup;

			//
			// Build pi list to delete... we remove things from this...
			//
			Enumeration piToDeleteEn = oldPigLookup.elements();
			HashSet piToDelete = new HashSet();
			while (piToDeleteEn.hasMoreElements()) {
				PrimitiveInstance pi = (PrimitiveInstance) piToDeleteEn
						.nextElement();
				piToDelete.add(getPkString(pi));
			}

			//
			// Start with an empty pli list for deletion
			//
			HashSet pliToDelete = new HashSet();

			//
			// Build the lookup tables for the current cleanup:
			// ... first the primitive instances
			pigLookup = new Hashtable();
			Iterator piIt = vi.getSubGraph().getNodes().values().iterator();
			while (piIt.hasNext()) {
				PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
				buildPKLookupTable(pi);
			}

			//
			// ... then the primitive link instances
			pliLookup = new Hashtable();
			Iterator pliIt = vi.getSubGraph().getEdges().iterator();
			while (pliIt.hasNext()) {
				PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt
						.next();
				buildLookupTable(pli);
			}

			//
			// Loop over the keys of the current pigLookup table
			// - if the key exists in the oldlookup table remove
			// that pi from the toDelete vector
			//
			Enumeration keyEn = pigLookup.keys();
			while (keyEn.hasMoreElements()) {
				String key = (String) keyEn.nextElement();

				if (oldPigLookup.containsKey(key)) {
					PrimitiveInstance pi = (PrimitiveInstance) oldPigLookup
							.get(key);
					String toBeRemoved = getPkString(pi);
					piToDelete.remove(toBeRemoved);
				}

			}

			//
			// Loop over the links in the pig.
			// - if either one of the primitives are not in the current view,
			// delete the link.
			//
			Enumeration linkKeyEn = oldPliLookup.keys();
			while (linkKeyEn.hasMoreElements()) {
				String linkKey = (String) linkKeyEn.nextElement();

				if (linkKey.endsWith("=null") || linkKey.equals(""))
					continue;

				if (!pliLookup.containsKey(linkKey)) {
					pliToDelete.add(linkKey);
				}

			}

			//
			// From oldPliLookup, we can use different keys to get the same
			// PLI!?
			// A duplicate deletions of PLI might occur ... Therefore, we keep
			// track
			// of the deletion of PLI to avoid this error!
			//
			// For ex: When updating an article view by deleting an existing
			// author
			// primitive instance, we will get the same PLI from oldPiLookup
			// object
			// by using two different keys 'Author|ViewTable_id=599' and
			// 'ViewTable|Viewtable_id=599'. This will make the system to delete
			// the
			// same PLI multiple times....!??
			//
			Vector pliDeleteRecord = new Vector();
			Iterator delIt = pliToDelete.iterator();
			while (delIt.hasNext()) {
				String linkKey = (String) delIt.next();
				PrimitiveLinkInstance pli = (PrimitiveLinkInstance) oldPliLookup
						.get(linkKey);

				PrimitiveInstance pi1 = (PrimitiveInstance) pli.getInEdgeNode();
				PrimitiveInstance pi2 = (PrimitiveInstance) pli.getOutEdgeNode();
				
				if( !pliDeleteRecord.contains(pli) && 
						pi1.getDefinition().isEditable() && 
						pi2.getDefinition().isEditable() ) {

					//
					// Try to delete the primitive... If we can't, too bad, move
					// on...
					//
					try {
						deletePrimitiveLinkFromDB(pli);
					} catch (SQLException sqlEx) {
						// Woo hoo... can't delete this one... let it go...
					}

					pliDeleteRecord.add(pli);
				}

			}

			//
			// THIS IS WHERE THE ACTUAL DELETION PROCESS HAPPENS
			//
			delIt = piToDelete.iterator();
			while (delIt.hasNext()) {
				String key = (String) delIt.next();
				PrimitiveInstance pi = (PrimitiveInstance) oldPigLookup
						.get(key);
				clearNonKeys(pi);

				if( !pi.getDefinition().isEditable()  )
					continue;
				
				//
				// Try to delete the primitive... If we can't, too bad, move
				// on...
				//
				try {

					deletePrimitiveFromDB(pi);

					//
					// Bugfix:
					//
					// Need to move the following code within the comment
					// outside
					// of the try catch block. Otherwise, when a PI cannot be
					// deleted
					// from the DB, the following tasks within the comment will
					// be
					// skip. This would cause an error of incorrect VIG update.
					//
					// Although the PI cannot be deleted from the DB, it sould
					// be
					// removed from the VIG if it is a view instance!
					//
					/*
					 * AttributeInstance ai =
					 * pi.getAttribute("|ViewTable.ViewTable_id");
					 * 
					 * ViewInstance vi = (ViewInstance) vGInstance.nodes.
					 * readFromCollection( "ViewTable_id=" + ai.get_value() );
					 * 
					 * addViewToBeRemoved( vi );
					 */
				} catch (Exception e) {
					// Woo hoo... can't delete this one... let it go...
				}

			}

			this.destroy();

		}

		protected GarbageCollector(ViewInstance vi) {

			try {

				pigLookup = new Hashtable();
				pliLookup = new Hashtable();

				copyVi = vi.deepCopy();

				Iterator piIt = copyVi.getSubGraph().getNodes().values()
						.iterator();
				while (piIt.hasNext()) {
					PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
					buildPKLookupTable(pi);
				}

				Iterator pliIt = copyVi.getSubGraph().getEdges().iterator();
				while (pliIt.hasNext()) {
					PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt
							.next();
					buildLookupTable(pli);
				}

			} catch (Exception e) {
				logger.info("Can't collect garbage");
				e.printStackTrace();
			}

		}

	}

	public void storeViewInstanceForUpdate(ViewInstance vi) throws Exception {

		this.initGarbageCollector(vi);
		this.garbageCol.copyVi = vi.deepCopy();

	};
	
	/**
	 * Executes raw SQL on the database, USE WITH CAUTION
	 * returns the number of rows altered in the database.
	 */
	public int executeRawUpdateQuery(String sql) throws SQLException {

		return this.uStat.executeUpdate(sql);

	
	}

};
