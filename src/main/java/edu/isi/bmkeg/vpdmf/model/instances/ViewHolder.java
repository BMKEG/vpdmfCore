package edu.isi.bmkeg.vpdmf.model.instances;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.vpdmf.model.definitions.IndexElement;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;

public class ViewHolder {

	private Map<Long, Map<String, List<Object>>> hhv;
	private Map<String, Long> idxHash;
	private List<Long> order;
	private ViewDefinition vd;
	private Map<Long, Integer> pCount;

	public List<Integer> getRowBorderIndices() {

		List<Integer> rowBorderIndices = new ArrayList<Integer>();
		List<Long> uids = this.getUIDs();
		int c = 0;
		for (int i = 0; i < uids.size(); i++) {
			Long s = uids.get(i);
			c += (pCount.get(s)).intValue();
			rowBorderIndices.add(new Integer(c));
		}

		return rowBorderIndices;

	}

	public ViewHolder(Map<Long, Map<String, List<Object>>> hhv, 
			List<Long> order,
			ViewDefinition vd, 
			Map<Long,Integer> pCount,
			Map<String,Long> idxHash) {

		this.hhv = hhv;
		this.order = order;
		this.vd = vd;
		this.pCount = pCount;
		this.idxHash = idxHash;

	}

	public List<Long> getUIDs() {
		Iterator<String> en = this.idxHash.keySet().iterator();
		ArrayList<String> keys = new ArrayList<String>();
		while (en.hasNext()) {
			keys.add(en.next());
		}
		String[] a = keys.toArray(new String[0]);
		Arrays.sort(a);
		List<Long> uids = new ArrayList<Long>();
		for (int i = 0; i < keys.size(); i++) {
			Long l = this.idxHash.get(a[i]);
			uids.add(l);
		}
		return uids;
	}

	public int getAttributeCount() {
		Iterator<Map<String, List<Object>>> en = this.hhv.values().iterator();
		Map<String, List<Object>> ht = null;
		while (en.hasNext()) {
			ht =  en.next();
			break;
		}
		return ht.size();
	}

	/*
	 * public int getColumnCount() { return this.getAllFormControls().size(); }
	 */

	public int getViewCount() {
		return this.hhv.size();
	}

	public int getRowCount() {
		int c = 0;
		Iterator<Integer> it = pCount.values().iterator();
		while (it.hasNext())
			c += (it.next()).intValue();
		return c;
	}

	public ViewDefinition get_vd() {
		return this.vd;
	}

	public Set<Long> getViewUIDs() {
		return new HashSet<Long>(this.hhv.keySet());
	}

	public ViewInstance getHeavyViewInstance(Long vpdmfId) throws Exception {

		if (!this.hhv.containsKey(vpdmfId))
			throw new Exception("Can't find view with uid: " + vpdmfId);

		Map<String, List<Object>> hv = this.hhv.get(vpdmfId);

		LightViewInstance lvi = this.getLightViewInstance(vpdmfId);
		ViewInstance vi = new ViewInstance(this.get_vd());

		Iterator<String> en = hv.keySet().iterator();
		while (en.hasNext()) {
			String addr = en.next();

			String pvName = addr.substring(addr.indexOf("]") + 1,
					addr.indexOf("|"));

			List<Object> v = hv.get(addr);

			if (vi.countPrimitives(addr) == 1) {

				for (int i = 1; i < v.size(); i++) {
					vi.addNewPrimitiveInstance(pvName, i);
				}

				PrimitiveDefinition pd = (PrimitiveDefinition) this.get_vd()
						.getSubGraph().getNodes().get(pvName);

				vi.computePrimitiveInstanceTotal(pd, v.size());

			}

			for (int i = 0; i < v.size(); i++) {

				AttributeInstance ai = vi.readAttributeInstance(addr, i);
				ai.setValue(v.get(i));

			}

		}

		vi.trimRepeatedPrimitives();

		vi.fillInExtraPrimitiveLinks();

		vi.setVpdmfLabel(lvi.getVpdmfLabel());
		vi.setVpdmfId(lvi.getVpdmfId());
		vi.setUIDString(lvi.getUIDString());
		vi.setVpdmfUri(lvi.getVpdmfUri());

		return vi;

	}

	public LightViewInstance getLightViewInstance(Long vpdmfId) throws Exception {

		VPDMf top = this.get_vd().getTop();
		
		if (!this.hhv.containsKey(vpdmfId))
			throw new Exception("Can't find view with uid: " + vpdmfId);

		Map<String, List<Object>> hv = this.hhv.get(vpdmfId);
		Set<String> keys = hv.keySet();

		LightViewInstance lvi = new LightViewInstance();
		lvi.setDefName(this.get_vd().getName());
		ViewDefinition vd = this.get_vd();
		PrimitiveDefinition pd = vd.getPrimaryPrimitive();
		UMLclass c = pd.getPrimaryClass();

		String stem = "]" + pd.getName() + "|" + c.getBaseName() + ".";

		lvi.setVpdmfId(vpdmfId);
		lvi.setUIDString("vpdmfId="+vpdmfId);

		List<Object> l = hv.get(stem + "vpdmfLabel");
		String hi = (String) l.get(0);
		lvi.setVpdmfLabel(hi);

		List<Object> l2 = hv.get(stem + "vpdmfUri");
		if( l2 != null ) {
			String uri = (String) l2.get(0);
			lvi.setVpdmfUri(uri);
		}
		
		//
		// Note:
		//
		// Check to see if the viewType is not the same in the database
		// as that of the requesting view (if, for example, the data in the
		// system inherits the data view definition of the ViewHolder). If
		// there's a glitch with this process, just use the ViewHolder's
		// ViewDefinition
		//
		l = hv.get(stem + "viewType");
		if (l != null) {
			String vt = (String) l.get(0);
			ViewDefinition vd2 = top.readViewDefinitionFromViewTypeString(vt);
			if ( vd2 != null ) {
				lvi.setDefinition(vd2);
				lvi.setDefName(vd2.getName());
			}
		}

		if (lvi.getDefinition() == null) {
			lvi.setDefinition(this.vd);
			lvi.setDefName(this.vd.getName());
		}

		l = hv.get(stem + "thumbnail");
		if (l != null) {

			BufferedImage bi = (BufferedImage) l.get(0);
			if (bi != null) {
				///lvi.setThumbnail(new ImageIcon(bi));
			}
		}
		
		//
		// calculate indexTuple key
		//
		String idxTuple = "";
		String idxTupleKey = "";
		
		for( String key : hv.keySet() ) {
			if( key.endsWith(".indexTuple") || key.endsWith(".vpdmfLabel") ) {
				String pvName = key.substring(1,key.indexOf("|"));
				String key2 = "]" + pvName + "|ViewTable.viewType";

				List<Object> idxTupleList = hv.get(key);				
				
				if( idxTuple.length() != 0 )
					idxTuple += LightViewInstance.INDEX_TUPLE_SEPARATOR;
				idxTuple += (String) idxTupleList.get(0); 
				
				List<Object> viewTypeList = hv.get(key2);		
				String viewType = this.vd.getName();
				if( viewTypeList != null && viewTypeList.size() > 0 ) 
					viewType = (String) viewTypeList.get(0); 
				
				ViewDefinition vd2 = top.readViewDefinitionFromViewTypeString(viewType);
				
				if( key.endsWith(".indexTuple") ) {
				
					int ieCount = vd2.getIndexElements().size();
					for( int i=0; i<ieCount; i++) {
						Integer ii = new Integer(i+1);
						IndexElement ie = vd2.getIndexElements().get(ii);
						
						if( idxTupleKey.length() != 0 )
							idxTupleKey += LightViewInstance.INDEX_TUPLE_SEPARATOR;
						
						idxTupleKey += vd2.getName() + "_" + ii;
						
					}
					
				} else if( key.endsWith(".vpdmfLabel") ) { 
								
					if( idxTupleKey.length() != 0 )
						idxTupleKey += LightViewInstance.INDEX_TUPLE_SEPARATOR;
					
					idxTupleKey += vd2.getName();
				
				}
				
			}

		}
		
		lvi.setIndexTuple(idxTuple);
		lvi.setIndexTupleFields(idxTupleKey);

		return lvi;

	}

	public ArrayList<LightViewInstance> getViewList() throws Exception {

		ArrayList<LightViewInstance> viewList = new ArrayList<LightViewInstance>();

		Iterator<Long> it = this.order.iterator();
		while (it.hasNext()) {
			Long vpdmfId = it.next();
			LightViewInstance vi = this.getLightViewInstance(vpdmfId);
			viewList.add(vi);
		}

		return viewList;

	}
	
	public Map<Long, Object> getAttributeList(String pvName, String cName, String aName) throws Exception {

		Map<Long, Object> atMap = new HashMap<Long, Object>();

		Iterator<Long> it = this.hhv.keySet().iterator();
		while (it.hasNext()) {
			Long vpdmId = it.next();

			if (!this.hhv.containsKey(vpdmId))
				throw new Exception("Can't find view with uid: " + vpdmId);

			Map<String, List<Object>> hv = this.hhv.get(vpdmId);
			String addr = "]" + pvName + "|" + cName + "." + aName;
			
			List<Object> l = hv.get(addr);
			atMap.put( vpdmId, l.get(0) );


		}
		
		return atMap;

	}
	
	public List<String> getIndexVector() throws Exception {

		Iterator<String> en = this.idxHash.keySet().iterator();
		List<String> keys = new ArrayList<String>();
		while (en.hasNext()) {
			keys.add(en.next());
		}
		String[] a = keys.toArray(new String[0]);
		Arrays.sort(a);
		List<String> idxs = new ArrayList<String>();
		for (int i = 0; i < keys.size(); i++) {
			idxs.add(a[i]);
		}
		return idxs;

	}

}
