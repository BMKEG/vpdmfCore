package edu.isi.bmkeg.vpdmf.bin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.controller.queryEngineTools.QueryEngine;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public class SqlQueryBuilder {

	public static String USAGE = "arguments: <archive-file> <view> <addressesToReturn> <addressesWithData>"; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
						
		if( args.length != 4 ) {
			System.err.println(USAGE);
			System.exit(-1);
		}
		
		File buildFile = new File(args[0]);
		
		if( !buildFile.exists() ) {
			System.err.println("Can't find " + args[0]);
			System.exit(-1);		
		}
		
		String view = args[1];
		String toReturn = args[2];
		String dataString =  args[3];
		
		System.out.println("VPDMf Archive: " + buildFile.getPath());
		
		VPDMfKnowledgeBaseBuilder builder = new VPDMfKnowledgeBaseBuilder(buildFile, null, null, null);
		ViewDefinition vd = builder.readTop().getViews().get(view);

		if( vd == null ) {
			System.err.println("View " + view + " does not exist!");
			System.exit(-1);		
		}
		
		ViewInstance vi = new ViewInstance(vd);
		
		String[] dataEntries = dataString.split("&");
		for(int i=0; i<dataEntries.length; i++) {
			String dataEntry = dataEntries[i];

			String[] data = dataEntry.split("=");
			if( data.length != 2 ) {
				System.err.println("Data not specified correctly (]pv1|cl1.at2=XYZ&]pv2|cl2.at2=ABC&...) - " + dataString);
				System.exit(-1);		
			}

			String addr = data[0];
			String value = data[1];
			
			AttributeInstance ai = vi.readAttributeInstance(addr, 0);
			ai.writeValueString(value);
		
		}
		
		List<String> addresses = new ArrayList<String>();
		String[] toReturnArray = toReturn.split("&");
		for(int i=0; i<toReturnArray.length; i++) {
			String addr = toReturnArray[i];
			addresses.add(addr);
		}
		
		QueryEngine qe = new QueryEngine();
		String sql = qe.buildSql(vi, addresses);
		
		System.out.println(sql);		
		
	}

}
