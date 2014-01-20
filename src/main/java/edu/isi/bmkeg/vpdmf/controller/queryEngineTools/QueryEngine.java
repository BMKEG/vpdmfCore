package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;

import cern.colt.matrix.ObjectFactory2D;
import cern.colt.matrix.ObjectMatrix1D;
import cern.colt.matrix.ObjectMatrix2D;
import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.vpdmf.exceptions.InterruptException;
import edu.isi.bmkeg.vpdmf.exceptions.VPDMfException;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinitionGraph;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstanceGraph;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveLinkInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewHolder;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public class QueryEngine extends DataHolderFactory implements VPDMfQueryEngineInterface {

	private static Logger logger = Logger.getLogger(QueryEngine.class);

	//
	// internal attributes for the select query
	//
	protected ViewInstance input;
	protected ObjectMatrix2D data;
	protected Map<String,Integer> lookup;
	protected boolean striped;
	protected boolean complete;

	//
	// global internal attributes
	//
	protected List<Long> order;
	protected Map<Long, Map<String, List<Object>>> hhm;
	protected Map<Long, Integer>  pCountHash;
	protected Map<String, Long> idxHash;
	protected int r;
	protected Set<String> pkHash;

	public QueryEngine(String login, String password, String uri) {
		super(login, password, uri);
	}

	public QueryEngine() {
		super();
	}
	
	private int getColumnNumber(String addr) {
		Integer i = (Integer) this.lookup.get(addr);
		if (i != null)
			return i.intValue();
		else
			return -1;
	}

	private int getDataRows() {
		//
		// Modified by Weicheng: The 'data' object might be null!
		// Return zero if the 'data' object is null to avoid the
		// NullPointerException thrown later.
		//
		if (data == null) {
			return 0;
		}

		if (striped)
			return data.rows() / 2;
		else
			return data.rows();

	}

	private Object getData(int i, int j) {

		Object d = null;

		if (striped) {

			Object o1 = data.get(2 * i, j);
			if (o1 instanceof String) {
				Object o2 = data.get(2 * i + 1, j);

				String s1 = (String) o1;
				String s2 = (String) o2;

				d = s1 + " ... to ... " + s2;

			} else {
				d = o1;
			}

		} else {

			d = data.get(i, j);

		}

		return d;

	}

	private String getVpdmfLabel(int i) {

		String idx = "";
		ViewDefinition vd = input.getDefinition();

		String addr = "]" + vd.getPrimaryPrimitive().getName() + "|"
				+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName()
				+ ".vpdmfLabel";

		int j = this.getColumnNumber(addr);

		if (j != -1)
			idx = (String) getData(i, j);

		return idx;

	}

	private Long getVpdmfId(int i) {

		Long key = 0L;
		ViewDefinition vd = input.getDefinition();

		Iterator<UMLattribute> pkIt = input.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getPkArray().iterator();
		UMLattribute pk = pkIt.next();

		String addr = "]" + vd.getPrimaryPrimitive().getName() + "|"
				+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName()
				+ "." + pk.getBaseName();

		int j = this.getColumnNumber(addr);

		if (j != -1) {
			Object o = getData(i, j);
			key = (Long) o;
		}

		return key;

	}

	private void init(ViewInstance input) {

		this.input = input;

		ViewDefinition vd = input.getDefinition();
		this.pkHash = new HashSet<String>();

		Iterator<UMLattribute> pkIt = input.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getPkArray().iterator();
		while (pkIt.hasNext()) {
			UMLattribute pk = pkIt.next();
			String addr = "]" + vd.getPrimaryPrimitive().getName() + "|"
					+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName()
					+ "." + pk.getBaseName();
			pkHash.add(addr);
		}

	}

	private void loadPrimaryPrimitive() throws Exception {
		PrimitiveDefinition ppd = this.input.getDefinition()
				.getPrimaryPrimitive();

		List<String> addrHash = new ArrayList<String>();
		addrHash.addAll(ppd.readAttributeAddresses());
		loadPrimaryPrimitive(addrHash);

	}

	private void loadPrimaryPrimitive(List<String> addrHash) throws Exception {

		//
		// Is this a UID query or a LIST query?
		//
		executeSelect(this.input.getPrimaryPrimitive(), addrHash);

		buildHHM(addrHash);

	}
	
	private void loadIndexedPrimitives(List<String> selectAddresses, 
			List<String> sortAddresses) 
					throws Exception {

		executeSelect(this.input, selectAddresses, sortAddresses);

		buildHHM(selectAddresses);

	}

	private void buildHHM(List<String> addrHash) throws InterruptException {
		hhm = new HashMap<Long, Map<String, List<Object>>>();
		order = new ArrayList<Long>();
		idxHash = new HashMap<String, Long>();
		pCountHash = new HashMap<Long, Integer>();

		for (int i = 0; i < getDataRows(); i++) {

			if (this.isCancelled())
				throw new InterruptException("Thread has been cancelled");

			Long key = this.getVpdmfId(i);
			String idx = this.getVpdmfLabel(i);
			hhm.put(key, new HashMap<String, List<Object>>());
			idxHash.put(idx, key);
			order.add(key);

		}

		//
		// Build the hhm here.
		//
		for (int i = 0; i < getDataRows(); i++) {

			if (this.isCancelled())
				throw new InterruptException("Thread has been cancelled");

			Long key = this.getVpdmfId(i);
			pCountHash.put(key, new Integer(1));

			Map<String, List<Object>> ht = hhm.get(key);

			Iterator<String> addrIt = addrHash.iterator();
			while (addrIt.hasNext()) {
				String addr = addrIt.next();

				int j = this.getColumnNumber(addr);
				if (j != -1) {
					Object o = getData(i, j);
					List<Object> v = new ArrayList<Object>();
					v.add(o);
					ht.put(addr, v);
				}

			}

		}

	}

	private void loadAllRemainingPrimitives() throws Exception {

		List<String> addrHash = new ArrayList<String>();
		addrHash.addAll(this.input.readAttributeAddresses());
		loadAllRemainingPrimitives(addrHash);

	}

	private void loadAllRemainingPrimitives(List<String> allAddresses) throws Exception {

		ViewDefinition vd = this.input.getDefinition();

		UndirectedGraph<SuperGraphNode, DefaultEdge> gg = vd.getSubGraph()
				.dumpToJGraphT();

		boolean paging = isDoPagingInQuery();
		setDoPagingInQuery(false);

		//
		// Setting up the query with the appropriate OR conditions
		//
		Iterator<UMLattribute> pkIt = input.getDefinition().getPrimaryPrimitive()
				.getPrimaryClass().getPkArray().iterator();
		while (pkIt.hasNext()) {
			UMLattribute pk = (UMLattribute) pkIt.next();
			String addr = "]" + vd.getPrimaryPrimitive().getName() + "|"
					+ vd.getPrimaryPrimitive().getPrimaryClass().getBaseName()
					+ "." + pk.getBaseName();
			int i = this.getColumnNumber(addr);

			if (i != -1) {
				AttributeInstance ai = input.readAttributeInstance(addr, 0);
				ObjectMatrix1D col = data.viewColumn(i);
				ai.setValue(col);
			}

		}

		Iterator<SuperGraphNode> piIt = input.getSubGraph().getNodes().values().iterator();
		PILOOP: while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
			PrimitiveDefinition pd = pi.getDefinition();
			if (pd.equals(vd.getPrimaryPrimitive()))
				continue;

			ArrayList<String> trimmedHash = new ArrayList<String>(allAddresses);
			ArrayList<String> allAddrList = new ArrayList<String>();

			allAddrList.addAll(pd.readAttributeAddresses());
			allAddrList.addAll(pkHash);
			trimAddresses(allAddrList, trimmedHash);

			if (trimmedHash.size() == 0)
				continue PILOOP;

			//
			// BUGFIX
			//
			// Use JGraphT to trace the path between the primary primitive
			// and the current primitive and add the primary keys to the select
			// clause to make sure that we get the correct cardinality.
			//
			DijkstraShortestPath<SuperGraphNode, DefaultEdge> dij = new DijkstraShortestPath<SuperGraphNode, DefaultEdge>(
					gg, vd.getPrimaryPrimitive(), pd);

			GraphPath<SuperGraphNode, DefaultEdge> path = dij.getPath();

			if (path == null) {
				continue PILOOP;
			}

			List<SuperGraphNode> list = Graphs.getPathVertexList(dij.getPath());
			for(int i=1; i<list.size(); i++) {
				PrimitiveDefinition source = (PrimitiveDefinition) list.get(i);

				if (source.equals(vd.getPrimaryPrimitive())
						|| source.equals(pd))
					continue;

				pkIt = source.getPrimaryClass().getPkArray().iterator();
				while (pkIt.hasNext()) {
					UMLattribute pk = (UMLattribute) pkIt.next();
					trimmedHash.add("]" + source.getName() + "|"
									+ source.getPrimaryClass().getBaseName() + "."
									+ pk.getBaseName());
				}
			}

			if (this.input.isUIDSet()) {
				executeSelect(pi, trimmedHash);
			} else {
				executeSelect(this.input, trimmedHash);
			}

			for (int i = 0; i < getDataRows(); i++) {

				if (this.isCancelled())
					throw new InterruptException("Thread has been cancelled");

				Long key = this.getVpdmfId(i);
				Map<String, List<Object>> ht = hhm.get(key);

				Iterator<String> addrIt = trimmedHash.iterator();
				while (addrIt.hasNext()) {
					String addr = (String) addrIt.next();

					if (pkHash.contains(addr))
						continue;

					//
					// Bugfix:
					//
					// We include the any primary keys that link this primitive
					// to the primary primitive of the view to make sure that
					// the
					// cardinality is correct for repeated data. This causes an
					// error since we are adding data within those linking
					// primitives
					// as well. Here we check to see that the data we're
					// updating
					// is confined to the primitive of interest.
					//
					if (!input.readPrimitiveDefinition(addr).equals(pd))
						continue;

					int j = this.getColumnNumber(addr);

					if (j != -1) {
						Object o = getData(i, j);

						List<Object> v = null;
						if (ht.get(addr) == null) {
							v = new ArrayList<Object>();
							ht.put(addr, v);
						} else {
							v = ht.get(addr);
						}

						v.add(o);

						int pCount = ((Integer) pCountHash.get(key)).intValue();
						if (pCount < v.size())
							pCountHash.put(key, new Integer(v.size()));

					}

				}

			}

		}

		setDoPagingInQuery(paging);

	}

	private List<String> trimAddresses(List<String> container, List<String> toTrim) {

		if (!container.equals(toTrim)) {
			List<String> toTrimVec = new ArrayList<String>();
			toTrimVec.addAll(toTrim);
			for (int i = 0; i < toTrimVec.size(); i++) {
				String addr = (String) toTrimVec.get(i);
				if (!container.contains(addr)) {
					toTrim.remove(addr);
				}
			}
		}

		return toTrim;

	}

	private Map<String, List<Object>> getTable(ViewInstance input, List<String> addrList)
			throws Exception {

		this.init(input);
		if (this.input.isUIDSet()) {
			executeSelect(this.input.getPrimaryPrimitive(), addrList);
		} else {
			executeSelect(this.input, addrList);
		}

		if (data == null)
			return null;

		Map<String, List<Object>> ht = new HashMap<String, List<Object>>();

		for (int i = 0; i < getDataRows(); i++) {

			if (this.isCancelled())
				throw new InterruptException("Thread has been cancelled");

			Iterator<String> addrIt = addrList.iterator();
			while (addrIt.hasNext()) {
				String addr = addrIt.next();

				List<Object> v = null;
				if (ht.containsKey(addr)) {
					v = ht.get(addr);
				} else {
					v = new ArrayList<Object>();
					ht.put(addr, v);
				}

				int j = this.getColumnNumber(addr);

				if (j != -1) {
					Object o = getData(i, j);
					v.add(o);
				}
			}
		}

		return ht;

	}

	/**
	 * Performs a partial query of a view to generate a ViewHolder containing
	 * some specified subset of the data from the query. The hashtable is given 
	 * the name 'hhm' and is a hashtable of hashtables of matrices of objects with the format:
	 * 
	 * Hashtable ht = (Hashtable) hhm.get( UIDString ); // UIDString e.g.,
	 * "ViewTable_id=137"
	 * 
	 * ObjectMatrix1d m = ht.get( attributeAddress ); // attributeAddress e.g.,
	 * "]Author|ViewTable.vpdmfLabel"
	 * 
	 * Object data = m.get( pvIndex ); // pvIndex e.g., 3
	 * 
	 * The example gives the index of the third author of an Article ViewSpec with
	 * the unique identifer '137'
	 * 
	 * @param vi
	 *            ViewInstance
	 * @throws Exception
	 */
	public ViewHolder querySpecifiedColumns(ViewInstance input, List<String> addrHash)
			throws Exception {

		ViewDefinition vd = input.getDefinition();

		init(input);

		loadPrimaryPrimitive(addrHash);

		loadAllRemainingPrimitives(addrHash);

		return new ViewHolder(this.hhm, this.order, vd, this.pCountHash, this.idxHash);

	}
	
	public ViewHolder querySpecifiedColumnsFromPrimaryPrimitive(ViewInstance input, List<String> addrHash)
			throws Exception {

		ViewDefinition vd = input.getDefinition();

		init(input);

		loadPrimaryPrimitive(addrHash);

		return new ViewHolder(this.hhm, this.order, vd, this.pCountHash, this.idxHash);

	}

	/**
	 * Performs a complete query of a view to generate a ViewHolder containing
	 * all the data from the query. The hashtable is given the name 'hhm' and is
	 * a hashtable of hashtables of matrices of objects with the format:
	 * 
	 * Hashtable ht = (Hashtable) hhm.get( UIDString ); // UIDString e.g.,
	 * "ViewTable_id=137"
	 * 
	 * ObjectMatrix1d m = ht.get( attributeAddress ); // attributeAddress e.g.,
	 * "]Author|ViewTable.vpdmfLabel"
	 * 
	 * Object data = m.get( pvIndex ); // pvIndex e.g., 3
	 * 
	 * The example gives the index of the third author of an Article ViewSpec with
	 * the unique identifer '137'
	 * 
	 * @param vi
	 *            ViewInstance
	 * @throws Exception
	 */
	protected ViewHolder getAllCompleteViews(ViewInstance input) throws Exception {

		ViewDefinition vd = input.getDefinition();

		init(input);

		loadPrimaryPrimitive();
		
		if( this.hhm.size() == 0 )
			return null;
			
		loadAllRemainingPrimitives();

		return new ViewHolder(this.hhm, this.order, vd, this.pCountHash, this.idxHash);

	}

	/**
	 * Performs a 'light' query of a view to generate a ViewHolder containing
	 * only data from the primary primitive of the view.
	 * 
	 * The hashtable is given the name 'hhm' and is a hashtable of hashtables of
	 * matrices of objects with the format:
	 * 
	 * Hashtable ht = (Hashtable) hhm.get( UIDString ); // UIDString e.g.,
	 * "ViewTable_id=137"
	 * 
	 * ObjectMatrix1d m = ht.get( attributeAddress ); // attributeAddress e.g.,
	 * "]Author|ViewTable.vpdmfLabel"
	 * 
	 * Object data = m.get( pvIndex ); // pvIndex e.g., 3
	 * 
	 * The example gives the index of the third author of an Article ViewSpec with
	 * the unique identifer '137'
	 * 
	 * @param vi
	 *            ViewInstance
	 * @throws Exception
	 */
	protected ViewHolder getOnlyPrimaryPrimitives(ViewInstance input)
			throws Exception {

		ViewDefinition vd = input.getDefinition();

		init(input);

		loadPrimaryPrimitive();

		return new ViewHolder(this.hhm, this.order, vd, this.pCountHash, this.idxHash);

	}

	/**
	 * Performs a 'light' query of a view to generate a ViewHolder containing
	 * only the indexing data from the primary primitive of the view. This
	 * includes only the uid values and label of the view.
	 * 
	 * The hashtable is given the name 'hhm' and is a hashtable of hashtables of
	 * matrices of objects with the format:
	 * 
	 * Hashtable ht = (Hashtable) hhm.get( UIDString ); // UIDString e.g.,
	 * "ViewTable_id=137"
	 * 
	 * ObjectMatrix1d m = ht.get( attributeAddress ); // attributeAddress e.g.,
	 * "]Author|ViewTable.vpdmfLabel"
	 * 
	 * Object data = m.get( pvIndex ); // pvIndex e.g., 3
	 * 
	 * The example gives the index of the third author of an Article ViewSpec with
	 * the unique identifer '137'
	 * 
	 * @param vi
	 *            ViewInstance
	 * @throws Exception
	 */
	protected ViewHolder getOnlyIndexColumns(ViewInstance input, List<String> sortAddr) 
			throws Exception {

		ViewDefinition vd = input.getDefinition();

		init(input);

		ArrayList<String> addrHash = new ArrayList<String>();
		Iterator<UMLattribute> it = vd.getPrimaryPrimitive().getPrimaryClass().getPkArray()
				.iterator();
		while (it.hasNext()) {
			UMLattribute att = it.next();
			addrHash.add("]" + vd.getPrimaryPrimitive().getName() + "|"
					+ att.getParentClass().getBaseName() + "."
					+ att.getBaseName());
		}

		PrimitiveDefinition ppd = vd.getPrimaryPrimitive();
		UMLclass pcd = ppd.getPrimaryClass();

		addrHash.add("]" + ppd.getName() + "|" + pcd.getBaseName()
				+ ".vpdmfLabel");

		Iterator<UMLattribute> aIt = pcd.getAttributes().iterator();
		while (aIt.hasNext()) {
			UMLattribute a = aIt.next();
			String addr = "]" + ppd.getName() + "|" + pcd.getBaseName()
					+ "." + a.getBaseName();
			if ( (addr.endsWith(".viewType") || addr.endsWith(".thumbnail")) 
					&& !addrHash.contains(addr)  ) {
				addrHash.add(addr);
			}
		}
			
		for(UMLclass c: ppd.getClasses()) {
			if( c != pcd ) {
				String addr = "]" + ppd.getName() + "|" + c.getBaseName()
						+ ".vpdmfId";
				addrHash.add(addr);
			}
		}
		
		//
		// Additional columns to accommodate indexTuples in the database.
		//
		PrimitiveDefinitionGraph pdg = (PrimitiveDefinitionGraph) vd.getSubGraph();
		Iterator<SuperGraphNode> pdIt = pdg.getNodes().values().iterator();
		while( pdIt.hasNext() ) {
			PrimitiveDefinition pd = (PrimitiveDefinition) pdIt.next();
			int nc = pdg.countNCardinalitiesToTarget(pd);
			if( nc == 0 && pd.getPrimaryClass().getBaseName().equals("ViewTable")) {				
				addrHash.add("]" + pd.getName() + "|ViewTable.vpdmfLabel");
				addrHash.add("]" + pd.getName() + "|ViewTable.indexTuple");
				addrHash.add("]" + pd.getName() + "|ViewTable.viewType");
			}
		}

		loadIndexedPrimitives(addrHash, sortAddr);

		return new ViewHolder(this.hhm, this.order, vd, this.pCountHash, this.idxHash);

	}
	
	public ResultSet executeRawSqlQuery(String sql) throws SQLException {

		return stat.executeQuery(sql);
	
	}

	private void executeSelect(ViewInstance vi, 
			List<String> selectAddresses)
			throws Exception {

		this.executeSelect(vi, selectAddresses, new ArrayList<String>());

	}


	private void executeSelect(ViewInstance vi, 
			List<String> selectAddresses, 
			List<String> sortAddresses)
			throws Exception {

		String sql = buildSql(vi, selectAddresses, sortAddresses);

		long t = System.currentTimeMillis();
		ResultSet rs = stat.executeQuery(sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    ViewHolder, ViewSpec-Based Select: " + deltaT
					/ 1000.0 + " s\n");

		RS2HHM(rs, selectAddresses);

	}

	public String buildSql(ViewInstance vi, 
			List<String> selectAddresses)
			throws Exception, VPDMfException {
		
		String vpdmfAddr = "]" + 
				vi.getPrimaryPrimitive().getDefinition().getName() +
				"|ViewTable.vpdmfLabel";
		List<String> sortAddresses = new ArrayList<String>();
		sortAddresses.add(vpdmfAddr);
		
		return buildSql(vi, selectAddresses, sortAddresses);
		
	}
	
	public String buildSql(ViewInstance vi, 
			List<String> selectAddresses, 
			List<String> sortAddresses)
			throws Exception, VPDMfException {
		
		Object[] addrArray = selectAddresses.toArray();
		Arrays.sort(addrArray);

		lookup = new HashMap<String,Integer>();

		for (int i = 0; i < addrArray.length; i++) {
			String addr = (String) addrArray[i];
			lookup.put(addr, new Integer(i));
		}

		clearQuery();
		buildSelectHeader(vi, selectAddresses);

		//
		// Build the 'ORDER BY' clause of the query.
		//
		for(String sortAddr : sortAddresses) {
			boolean revFlag = false;
			if( sortAddr.startsWith("-") ) {
				revFlag = true;
				sortAddr = sortAddr.substring(1, sortAddr.length());
			}
			AttributeInstance ai = vi.readAttributeInstance(sortAddr, 0);
			String sortKey = getAlias(ai.get_object()) + "." + ai.getDefinition().getBaseName();
			if( revFlag )
				sortKey += " DESC";
			orderBy.add(sortKey);
		}
		
		//
		// TODO: NEED TO UPDATE THIS. WE ARE NOW DOING 
		// MORE SOPHISTICATED STUFF WITH THE QUERYENGINE
		//
		// When building the 'SqlConditions' and 'TableAliases', we need
		// to check if the current view instance is allowed to skip those
		// null primitive instances or not.
		//
		// If the type of the current view instance is 'LOOKUP', then
		// it is not allowed to skip those null primitive instances.
		// Otherwise, it is allowed to skip.
		// 
		/*if (vi.getDefinition().getType() == ViewDefinition.LOOKUP) {

			buildSqlConditions(vi, DatabaseEngine.ALL, false);

			buildTableAliases(vi, false);

		} else {*/

			buildSqlConditions(vi, DatabaseEngine.ALL, true);

			buildTableAliases(vi, true);

		//}

		String sql = buildSQLSelectStatement();

		if (isDoPagingInQuery()) {
			sql = buildListPageStatement(sql);
		}

		this.prettyPrintSQL(sql);

		if (lc)
			sql = sql.toLowerCase();
		return sql;
	}

	private void executeSelect(PrimitiveInstance pi, List<String> addresses)
			throws Exception {

		String sql = buildSql(pi, addresses);

		long time= System.currentTimeMillis();
		ResultSet rs = stat.executeQuery(sql);
		long deltaT = System.currentTimeMillis() - time;

		logger.debug("    ViewHolder, ViewSpec-Based Select: " + deltaT
					/ 1000.0 + " s\n");

		RS2HHM(rs, addresses);
	}

	public String buildSql(PrimitiveInstance pi, List<String> addresses)
			throws VPDMfException, Exception {
		
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) pi.getGraph();
		ViewInstance vi = (ViewInstance) pig.getSubGraphNode();
		PrimitiveInstance ppi = vi.getPrimaryPrimitive();
		PrimitiveInstance l = ppi;
		Object[] addrArray = addresses.toArray();
		Arrays.sort(addrArray);

		lookup = new HashMap<String,Integer>();

		for (int i = 0; i < addrArray.length; i++) {
			String addr = (String) addrArray[i];
			lookup.put(addr, new Integer(i));
		}

		//
		// Run the query
		//

		clearQuery();

		//
		// Here we only want to execute the query over the primary 
		// primitive's attributes in the viewtable.
		// If not then all columns in the primary primitive get repeated 
		// in all subsequent primitive queries, which can cause 
		// a memory leak
		// - NEED TO GO OVER THE VPDMf AND DESIGN IT AS A PROPER API.
		buildSelectHeader(ppi, addresses);
		
		buildSqlConditions(ppi, DatabaseEngine.ALL);
		buildTableAliases(ppi, false);

		UndirectedGraph<SuperGraphNode, DefaultEdge> gg = pi.getGraph()
				.dumpToJGraphT();

		DijkstraShortestPath<SuperGraphNode, DefaultEdge> dij = new DijkstraShortestPath<SuperGraphNode, DefaultEdge>(
				gg, vi.getPrimaryPrimitive(), pi);

		List<DefaultEdge> pathEdgeList = dij.getPathEdgeList();
		
		GraphPath<SuperGraphNode, DefaultEdge> path = dij.getPath();
		List<SuperGraphNode> ll = Graphs.getPathVertexList(path);

		for(int i = 1; i<ll.size(); i++) {
			PrimitiveInstance s = (PrimitiveInstance) ll.get(i-1);
			PrimitiveInstance t = (PrimitiveInstance) ll.get(i);
	
			if(s==null)
				continue;
			
			//
			// Note that this procedure just builds an SQL query 
			// based on the shortest linkage to the primitive. 
			// This assumption works quite well since we simply want 
			// to establish the existence of data within the graph, 
			// not the extra convoluted connections when things join up.
			//
			PrimitiveLinkInstance pli = null;
			for( SuperGraphEdge e : s.getOutgoingEdges().values() ) {
				if( (e.getOutEdgeNode() == s && e.getInEdgeNode() == t) ||
						(e.getInEdgeNode() == s && e.getOutEdgeNode() == t)) {
					pli = (PrimitiveLinkInstance) e;
					break;
				}
			}
			if( pli == null ) {
				for( SuperGraphEdge e : s.getIncomingEdges().values() ) {
					if( (e.getOutEdgeNode() == s && e.getInEdgeNode() == t) ||
							(e.getInEdgeNode() == s && e.getOutEdgeNode() == t)) {
						pli = (PrimitiveLinkInstance) e;
						break;
					}
				}
				if( pli == null ) 
					throw new Exception("Error: can't link" + s.getName() + " to " + t.getName());
			}
			buildSqlConditions(pli, ALL);
			buildTableAliases(pli, false);

			PrimitiveInstance tpi = null;
			if (s.equals(l)) {
				tpi = t;
			} else if (t.equals(l)) {
				tpi = s;
			}

			if (s.equals(pi) || t.equals(pi))
				buildSelectHeader(tpi, addresses);

			buildSqlConditions(tpi, DatabaseEngine.ALL);
			buildTableAliases(tpi, false);

			l = tpi;

		}

		String primaryClassName = ppi.getDefinition().getPrimaryClass()
				.getBaseName();
		ClassInstance pci = (ClassInstance) ppi.getObjects().get(primaryClassName);

		//
		// NOTE:
		//
		// In general, do we use label to order the
		// query. We SHOULD use vpdmfLabel since the order the user will
		// see nodes and views is usually determined by the vpdmfLabel.
		//
		orderBy.add(getAlias(pci) + ".vpdmfLabel");
		String sql = buildSQLSelectStatement();

		if (isDoPagingInQuery()) {
			sql = buildListPageStatement(sql);
		}

		if (lc)
			sql = sql.toLowerCase();
		return sql;
	}

	private void RS2HHM(ResultSet rs, List<String> addresses) throws Exception {

		boolean nav = rs.last();
		int rowCount = rs.getRow();

		if (rowCount == 0) {
			this.data = null;
			return;
		}

		//
		// Build a vector that determines which items in the result set
		// will be mentioned in the returned list. This is calculated so that
		// the number of items at the lowest level is equal to
		// maxReturnedInQuery
		//
		List<Integer> stopPoints = new ArrayList<Integer>();

		//
		// the number of items at this level.
		double levelRatio = (double) rowCount;
		double power = 0.0;

		//
		// Note:
		//
		// Need to make sure that maxReturnedInQuery is never zero,
		// since it will causes this part of the code to hang.
		//
		if (getMaxReturnedInQuery() == 0)
			setMaxReturnedInQuery(1000);

		while (levelRatio > (double) getMaxReturnedInQuery()) {
			levelRatio = levelRatio / getMaxReturnedInQuery();
			power = power + 1.0;
		}
		int step = (int) Math.pow(getMaxReturnedInQuery(), power);

		for (int i = 1; i < rowCount; i = i + step) {
			stopPoints.add(new Integer(i));
		}

		if (step == 1) {

			stopPoints.add(new Integer(rowCount));
			data = ObjectFactory2D.dense.make(stopPoints.size(),
					addresses.size());
			this.striped = false;

		} else {

			data = ObjectFactory2D.dense.make((stopPoints.size() * 2),
					addresses.size());
			this.striped = true;

		}

		//
		// ***** Note *****
		// i = the row in the data matrix that this data is being set to
		// ii = the row in the ResultSet retrieved from the database
		//
		TOPLOOP: for (int ii = 0; ii < stopPoints.size(); ii++) {
			int i = ((Integer) stopPoints.get(ii)).intValue();

			if (step == 1) {

				nav = rs.absolute(i);
				this.setDataFromRS(rs, ii);

			} else {

				nav = rs.absolute(i);
				this.setDataFromRS(rs, 2 * ii);
				// DEBUG
				logger.debug("RS: " + i + ", M: " + (2 * ii));

				int nextButOne = i + step - 1;
				if (nextButOne > rowCount)
					nextButOne = rowCount;
				nav = rs.absolute(nextButOne);
				this.setDataFromRS(rs, (2 * ii) + 1);

				// DEBUG
				logger.debug("RS: " + nextButOne + ", M: "
						+ ((2 * ii) + 1));

			}

		}

	}


	public ViewInstance executeUIDQuery(LightViewInstance lvi) throws Exception {

		return executeUIDQuery(lvi.getDefName(), lvi.getVpdmfId());
	
	}
	
	/**
	 * Runs a query for a specific view based on the name of the view.
	 * @param vdName - name of the view
	 * @param uidString - uidString of the view to be found.
	 * @return
	 * @throws Exception
	 */
	public ViewInstance executeUIDQuery(String vdName,
			Long vpdmfId) throws Exception {

		ViewDefinition vd = (ViewDefinition) this.vpdmf.getViews().get(vdName);
		
		return executeUIDQuery(vd, vpdmfId);

	}

	/**
	 * Runs a query for a specific view based on the view definition iself.
	 * 
	 * @param vd
	 * @param uidString
	 * @return
	 * @throws Exception
	 */
	protected ViewInstance executeUIDQuery(ViewDefinition vd,
			Long vpdmfId) throws Exception {

		ViewInstance vi = new ViewInstance(vd);
		vi.writeVpdmfId(vpdmfId);
		
		ViewHolder vh = this.getAllCompleteViews(vi);
		
		if( vh == null )
			return null;
		
		return vh.getHeavyViewInstance(vpdmfId);

	}

	
	
	/**
	 * Executes a view-level query based on a ViewInstance set in 
	 * and returns a full list of data. A greedy appraoch. 
	 * 
	 * @throws Exception
	 */
	public List<ViewInstance> executeFullQuery(ViewInstance qVi) throws Exception {

		return this.executeFullQuery(qVi, false, 0, 0);
		
	}

	/**
	 * We need to query all dependent views for the view instance provided. This
	 * query will have to be built *outside* of existing Views since we are
	 * looking for the crossover between views. This is essential for being able
	 * to effectively delete data from the system.
	 * 
	 * @param vi - the view instance being queried
	 * @return a set of dependent ViewInstances 
	 * @throws Exception
	 */
	public HashSet<LightViewInstance> queryDependentViews(ViewInstance vi) throws Exception {
		
		Map<Long, LightViewInstance> dependentViews = new HashMap<Long, LightViewInstance>();

		ViewDefinition vd = vi.getDefinition();
		PrimitiveDefinition pd = vd.getPrimaryPrimitive();

		Map<UMLclass, List<PrimitiveDefinition>> lookup = vd.getTop().readClassPrimitiveLookupTable();

		Object[] cArray = pd.getClasses().toArray();
		for (int i = 0; i < cArray.length; i++) {
			UMLclass c = (UMLclass) pd.getClasses().get(i);

			UMLattribute pk = (UMLattribute) c.getPkArray().get(0);

			String pkAddr = "]" + pd.getName() + "|"
					+ pk.getParentClass().getBaseName() + "."
					+ pk.getBaseName();

			AttributeInstance ai = vi.readAttributeInstance(pkAddr, 0);
			String value = ai.readValueString();

			Object[] fkArray = pk.getFk().toArray();

			for (int j = 0; j < fkArray.length; j++) {
				UMLattribute fk = (UMLattribute) fkArray[j];
				UMLclass fkClass = (UMLclass) fk.getParentClass();

				if (lookup.containsKey(fkClass)) {

					Object[] pvArray = lookup.get(fkClass).toArray();
					for (int k = 0; k < pvArray.length; k++) {
						PrimitiveDefinition depPd = (PrimitiveDefinition) pvArray[k];
						ViewDefinition depVd = depPd.getView();

						if (vd.getType() != ViewDefinition.DATA
								|| vd.checkIsAChildOf(depVd) || vd.equals(depVd)
								|| depVd.getType() == ViewDefinition.LOOKUP
								|| depVd.getType() == ViewDefinition.EXTERNAL
								|| depVd.getType() == ViewDefinition.SYSTEM
								|| depVd.getName().equals("FromView")
								|| depVd.getName().equals("ToView")
								|| depVd.getName().equals("link"))
							continue;

						ViewInstance depVi = new ViewInstance(depVd);

						String fkAddr = "]" + depPd.getName() + "|"
								+ fk.getParentClass().getBaseName() + "."
								+ fk.getBaseName();

						AttributeInstance fkAi = depVi.readAttributeInstance(
								fkAddr, 0);
						fkAi.writeValueString(value);

						if (depVd.getType() == ViewDefinition.DATA
								|| depVd.getType() == ViewDefinition.COLLECTION) {

							String typeAddr = "]"
									+ depVd.getPrimaryPrimitive().getName()
									+ "|ViewTable.viewType";
							AttributeInstance typeAi = depVi
									.readAttributeInstance(typeAddr, 0);
							typeAi.writeValueString("%." + depVd.getName() + ".%");

						} else if (depVd.getType() == ViewDefinition.LINK) {

							String typeAddr = "]"
									+ depVd.getPrimaryPrimitive().getName()
									+ "|ViewLinkTable.linkType";
							AttributeInstance typeAi = depVi
									.readAttributeInstance(typeAddr, 0);
							typeAi.writeValueString(depVd.getName());

						}

						ViewHolder vh = this.getOnlyIndexColumns(depVi, new ArrayList<String>());
						Object[] uidArray = vh.getUIDs().toArray();
						for (int l = 0; l < uidArray.length; l++) {
							Long uid = (Long) uidArray[l];
							LightViewInstance lVi = vh.getLightViewInstance(uid);

							if (dependentViews.containsKey(uid)) {
								LightViewInstance oldVi = dependentViews.get(uid);
								if (depVd.checkIsAChildOf(oldVi.getDefinition()))
									dependentViews.put(uid, lVi);
							} else {
								dependentViews.put(uid, lVi);
							}

						}

					}

				}

			}

		}

		HashSet<LightViewInstance> hs = new HashSet<LightViewInstance>();
		hs.addAll(dependentViews.values());
		return hs;

	}

	
	/**
	 * Executes a view-level query based on a ViewInstance set in 
	 * and returns a full list of data. A greedy approach. 
	 * 
	 * @throws Exception
	 */
	public List<ViewInstance> executeFullQuery(ViewInstance qVi, boolean paging, int listOffset, int pageSize) throws Exception {

		List<ViewInstance> lVi = new ArrayList<ViewInstance>();
		
		logger.debug("        EXECUTING FULL QUERY \n");
		
		this.setDoPagingInQuery(paging);		
		this.setListOffset(listOffset);
		this.setListPageSize(pageSize);

		ViewHolder vh = this.getAllCompleteViews(qVi);
		
		Iterator<Long> uidIt = vh.getUIDs().iterator();
		while( uidIt.hasNext() ) {
			Long uid = uidIt.next();
			ViewInstance rVi = vh.getHeavyViewInstance(uid);
			lVi.add(rVi);
		}
		
		return lVi;
		
	}

	private ClassInstance getLinkToAdd(Integer vltId, String fromId,
			String toId, String linkType, String vltHi, String vltMi) throws Exception {

		UMLclass cd = this.vpdmf.getUmlModel().lookupClass("ViewLinkTable")
				.iterator().next();
		ClassInstance ci = new ClassInstance(cd);

		AttributeInstance ai = (AttributeInstance) ci.attributes
				.get("ViewLinkTable_id");
		ai.setValue(vltId);

		ai = (AttributeInstance) ci.attributes.get("from_id");
		ai.writeValueString(fromId);

		ai = (AttributeInstance) ci.attributes.get("to_id");
		ai.writeValueString(toId);

		ai = (AttributeInstance) ci.attributes.get("linkType");
		ai.setValue(linkType);

		ai = (AttributeInstance) ci.attributes.get("indexString");
		ai.setValue(vltHi);

		return ci;

	}

	private void setDataFromRS(ResultSet rs, int i) throws Exception {
		Iterator<String> addrEn = this.lookup.keySet().iterator();
		while (addrEn.hasNext()) {
			String addr = (String) addrEn.next();
			Integer jj = (Integer) this.lookup.get(addr);
			int j = jj.intValue();
			AttributeInstance ai = this.input.readAttributeInstance(addr, 0);
			Object o = getDataFromRS(rs, ai);
			if (o != null) {
				data.set(i, j, o);
			}
		}
	}
	
	public int executeCountQuery(ViewInstance vi) throws Exception {

		AttributeInstance ai = vi.getPrimaryPrimitive()
				.readPrimaryObject().getAttributes().get("vpdmfId");
		List<String> addresses = new ArrayList<String>();
		addresses.add(ai.getAddress());
		
		clearQuery();
		buildSelectHeader(vi, addresses);
		buildSqlConditions(vi, DatabaseEngine.ALL, true);
		buildTableAliases(vi, true);
		String sql = buildSQLSelectStatement();

		this.prettyPrintSQL(sql);

		if (lc)
			sql = sql.toLowerCase();

		long t = System.currentTimeMillis();
		ResultSet rs = stat.executeQuery(sql);
		long deltaT = System.currentTimeMillis() - t;

		logger.debug("    DatabaseEngine, Count query: " + deltaT
					/ 1000.0 + " s\n");

		rs.last();
		setListCount(rs.getRow());

		return getListCount();

	}

	public List<LightViewInstance> executeListQuery(ViewInstance vi, 
			List<String> sortAddresses, 
			boolean paging, int listOffset, int pageSize) 
					throws Exception {

		ArrayList<LightViewInstance> viewList = null;
		
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Hack solution for a serialization bug.
		// - the subGraph object is repeated with a value set to null. 
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) 
				vi.getPrimaryPrimitive().getGraph();
		vi.setSubGraph(pig);
			
		vi.instantiateDefinition(this.vpdmf);

		this.stat.execute("set autocommit=0;");		
		this.setDoPagingInQuery(paging);
		this.setListOffset(listOffset);
		this.setListPageSize(pageSize);
		
		if( sortAddresses.size() == 0) {
			sortAddresses.add("]" + vi.getPrimaryPrimitive().getDefName() + 
					"|" + vi.getPrimaryPrimitive().getDefinition().getPrimaryClass().getImplName() 
					+ ".vpdmfLabel");
		}
		
		ViewHolder vh = this.getOnlyIndexColumns(vi, sortAddresses);

		viewList = vh.getViewList();

		return viewList;
		
	}	

	public List<LightViewInstance> executeListQuery(ViewInstance vi, 
			boolean paging, int listOffset, int pageSize) 
					throws Exception {

		return this.executeListQuery(vi, new ArrayList<String>(), paging, listOffset, pageSize);
		
	}	

	
	public List<LightViewInstance> executeListQuery(ViewInstance vi, 
			List<String> sortAddresses) 
					throws Exception {

		return this.executeListQuery(vi, sortAddresses, false, 0, 0);
		
	}	

	public List<LightViewInstance> executeListQuery(ViewInstance vi)
			throws Exception {

		return this.executeListQuery(vi, new ArrayList<String>(), false, 0, 0);
		
	}	
	
	public List<ClassInstance> executeClassQuery(String queryString) throws Exception {

		List<ClassInstance> ciResult = null;

	      //
	      //  The structure of the address:
	      //
	      //    [viewName]primitiveName|className?condition1=value1&condition2=value2....
	      //
	      //  We are trying to query the class based on the above address string.
	      //
	      String viewName = queryString.substring(1, queryString.indexOf("]"));

	      String className = queryString.substring(queryString.indexOf("|"),
	                                              queryString.indexOf("?"));
	      
	      String attrQueryString = queryString.substring(queryString.indexOf(".") + 1,
                  queryString.indexOf("?"));


	      VPDMf top = this.readTop();
	      
	      ViewDefinition vd = top.getViews().get(viewName);

	      UMLclass cd = top.getUmlModel().listClasses().get(className);

	      ClassInstance ci = new ClassInstance(cd);

	      AttributeInstance ai = null;

	      String condStr = queryString.substring(queryString.indexOf("?") + 1,
	                                              queryString.length());

	      String[] strArray = condStr.split("&");
	      for (int i = 0; i < strArray.length; i++) {
	        String condition = strArray[i].substring(0, strArray[i].indexOf("="));

	        String value = strArray[i].substring(strArray[i].indexOf("=") + 1,
	                                             strArray[i].length());

	        ai = (AttributeInstance) ci.attributes.get(condition);

	        ai.writeValueString(value);
	      }

	      ciResult = this.queryClass(ci, attrQueryString);
	      
	     return ciResult;
	      
	}

	public Map<Long, Object> executeAttributeQuery(ViewInstance vi, 
			String pName, String cName, String aName, 
			boolean paging, int listOffset, int pageSize)
			throws Exception {
		
		List<String> addrHash = new ArrayList<String>();

		String addr = "]" + pName + "|" + cName + "." + aName;
		addrHash.add(addr);
	
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Hack solution for a serialization bug.
		// - the subGraph object is repeated with a value set to null. 
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) 
				vi.getPrimaryPrimitive().getGraph();
		vi.setSubGraph(pig);
			
		vi.instantiateDefinition(this.vpdmf);

		String primaryPvName = vi.getDefinition().getPrimaryPrimitive().getName();
		addrHash.add("]" + primaryPvName + "|ViewTable.vpdmfId");
		
		this.stat.execute("set autocommit=0;");
		this.setDoPagingInQuery(paging);
			
		this.setDoPagingInQuery(paging);
		this.setListOffset(listOffset);
		this.setListPageSize(pageSize);
		
		ViewHolder vh = this.querySpecifiedColumnsFromPrimaryPrimitive(vi, addrHash);

		Map<Long, Object> atMap = vh.getAttributeList(pName, cName, aName);

		return atMap;
		
	}

	
}
