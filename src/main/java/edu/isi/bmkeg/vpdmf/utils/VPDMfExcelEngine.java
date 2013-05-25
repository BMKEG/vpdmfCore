package edu.isi.bmkeg.vpdmf.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import cern.colt.matrix.ObjectMatrix2D;
import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.utils.excel.ExcelEngine;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;

public class VPDMfExcelEngine extends ExcelEngine {
	
	Logger logger = Logger.getLogger(VPDMfExcelEngine.class);

	private UMLmodel m;
	private Map<String,UMLattribute> lu = new HashMap<String,UMLattribute>();
	
    private short bStyle = HSSFCellStyle.BORDER_THIN;
    private short bStyle2 = HSSFCellStyle.BORDER_MEDIUM;
    private String currSheet;
    private int sheetnum;
    private String currentExptName;
    private SSTRecord sstrec;
    
    public VPDMfExcelEngine() {
    	super(false);    		
    }

    @Override
	public String getData(int r, int c, String sheetName) throws Exception {
    	
		ObjectMatrix2D mat = this.getData().get(sheetName);

		String dat = (String) mat.get(r, c);
		
    	return dat;

	}
 	
	public HSSFWorkbook generateBlankExcelSpreadsheetFromVPDMf(VPDMf top, String pkgPattern) throws Exception {
		
        // create a new workbook
		setWb(new HSSFWorkbook());

        setCs(getWb().createCellStyle());
        setF(getWb().createFont());
        getF().setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        getCs().setFont(getF());

        setSheetnum(0);
        buildEmptyDataSheetsForVPDMf(top.getUmlModel(), pkgPattern);
        
		return getWb();
		
	}
	
    private void buildEmptyDataSheetsForVPDMf(UMLmodel m, String pkgPattern) throws Exception {

    	HSSFWorkbook wb = getWb();
    	HSSFCellStyle cs = getCs();
    	
    	Iterator<UMLclass> cIt = m.listClasses(pkgPattern).values().iterator();
    	while( cIt.hasNext() ) {
    		UMLclass c = cIt.next();
    		
            HSSFSheet s = wb.createSheet();

            //
            // We might get into trouble with long class names
            // Such as those generated for n-to-n associations...
            //
            String sName = c.getBaseName();
            if(sName.length()>31)
            	sName = sName.substring(0,31);
            wb.setSheetName(getSheetnum(), sName);

            setSheetnum(getSheetnum()+1);
    		
    		// declare a row object reference
            HSSFRow row = null;
            // declare a cell object reference
            HSSFCell cell = null;
            
            // Insert column headings for cell attributes.
            short rCount = 0;
            row = s.createRow(rCount);
            
            ArrayList<UMLattribute> aImpList = c.listImplementedAttributes();
            for(int i=0; i<aImpList.size(); i++) {
            	UMLattribute a = aImpList.get(i);
                cell = row.createCell((short) i);
                cell.setCellValue(a.getBaseName());
                cell.setCellStyle(cs);              	
            }
            
    	}
    	
    }    
    
    /**
     * Given a spreadsheet corresponding to a model, dump the files needed for batch uploading. 
     * This function then calls generateSQLDataFiles(UMLmodel, File) to do the heavy lifting.
     * @param m
     * @param resourceXlFile
     * @param tempDIr
     * @return
     * @throws Exception
     */
    public Map<String,File> generateSQLDataFilesFromExcel(UMLmodel m, File xlFile, File tempDir) throws Exception {

		this.readFile(xlFile);
		
		String batchSql = "";

		Map<String,File> filesToZip = new HashMap<String,File>();

		Iterator<String> sIt = this.getData().keySet().iterator();
		SHEET_LOOP: while (sIt.hasNext()) {
			String sName = sIt.next();
			
			UMLclass c = lookupClassFromSheetName(m, sName);
			
			ObjectMatrix2D mat = this.getMatrixForSheet(sName);
			
			ArrayList<String> attrNames = new ArrayList<String>();
			
			for(int topC=0; topC<mat.columns(); topC++) {
				attrNames.add((String) mat.get(0, topC));
			}
			
			// only consider the attributes that are *implemented* 
			// (i.e. keys rather than the base attribute)
			ArrayList<UMLattribute> aImpList = c.listImplementedAttributes();
			
			if( aImpList.size() != attrNames.size() ) {
				throw new Exception(c.getBaseName()+ " number of attributes mismatch");
			}

			StringBuffer s = new StringBuffer();
			
			for(int i=0; i<attrNames.size(); i++) {
				String attrName = attrNames.get(i);
				UMLattribute a = aImpList.get(i);
				if( !a.getBaseName().equals(attrName) ) {
					throw new Exception(c.getBaseName()+ "." + a.getBaseName() + 
							" != " + attrName + " name mismatch");
				}
				
			}
			//
			// Iterate over the model and generate the data files.
			//
			ROW: for(int row=1; row<mat.rows(); row++) {				
				
				boolean emptyRow = true;
				for(int col=0; col<attrNames.size()-1; col++) {
					String entry = (String) mat.get(row, col);
					if( entry != null && entry.length() > 0 ) {
						emptyRow = false;
					}
				}
				if( emptyRow )
					continue ROW;
				
				if( row > 0 && row % 1000 == 0) {
					System.err.println(c.getBaseName() + ", row: " + row + "/" + mat.rows());
				}
				
				for(int col=0; col<attrNames.size()-1; col++) {
				
					UMLattribute a = aImpList.get(col);

					String entry = (String) mat.get(row, col);
					if( entry.indexOf(".0") != -1 ) {
						entry = entry.substring(0,entry.indexOf("."));
					}					
					s.append( entry + "\t\t\t");
			
				}
				
				String entry = (String) mat.get(row, attrNames.size()-1);
				if( entry.indexOf(".0") != -1 ) {
					entry = entry.substring(0,entry.indexOf("."));
				}		
				
				s.append( entry );
				if( row < mat.rows()-1 )
					s.append( "\n\n\n");
			
			}
			
			File datFile = new File(tempDir.getAbsolutePath() + "/sqlFiles/" + c.getBaseName() + ".dat");
			FileUtils.writeStringToFile(datFile, s.toString());
			
			filesToZip.put("sqlFiles/"+datFile.getName(), datFile);			
			batchSql += "LOAD DATA LOCAL INFILE 'SUB_FILEPATH_HERE/" +
					c.getBaseName() + ".dat' REPLACE INTO TABLE " + c.getBaseName() + 
					" FIELDS TERMINATED BY '\\t\\t\\t' LINES TERMINATED BY '\\n\\n\\n' (";

			ArrayList<UMLattribute> attrs = c.listImplementedAttributes();
			for(int i=0; i<attrs.size()-1; i++) {
				batchSql += attrs.get(i).getBaseName() + ",";
			}
			batchSql +=  attrs.get(attrs.size()-1).getBaseName() + ");\n";			
			
		}

		File sqlFile = new File(tempDir.getAbsolutePath() + "/sqlFiles/batch_upload.sql");
		FileUtils.writeStringToFile(sqlFile, batchSql);
		filesToZip.put("sqlFiles/"+sqlFile.getName(), sqlFile);

		return filesToZip;
	
    }
    
    /**
     * Given a directory, we expect it to contain tab-delimited files, 
     * saved one-by-one from an Excel spreadsheet. This should correspond
     * to a model. This function then calls generateSQLDataFiles(UMLmodel, File) to
     * do the heavy lifting.
     * @param m
     * @param tabFileDir
     * @param tempDIr
     * @return
     * @throws Exception
     */
    public Map<String,File> generateSQLDataFilesFromDirectory(UMLmodel m, 
    		File tabFileDir, File tempDir) throws Exception {
				
		String batchSql = "";

		Map<String,File> filesToZip = new HashMap<String,File>();
		File[] files = tabFileDir.listFiles();
		for(int i=0; i<files.length; i++) {
			File tab = files[i];

			if( tab.getName().startsWith("."))
				continue;
			
			// leave the original data files out of the archive.
			//filesToZip.put("sheets/"+tab.getName(), tab);

			UMLclass c = lookupClassFromSheetName(m, tab.getName().replaceAll(".txt", ""));
			
			File dat = new File(tempDir.getAbsolutePath() + "/" + c.getBaseName() + ".dat");
			ArrayList<String> attrNames = convertTab2DatFile(tab, dat);

			filesToZip.put("sqlFiles/" + dat.getName() , dat);
		
			batchSql += "LOAD DATA LOCAL INFILE 'SUB_FILEPATH_HERE/" +
					c.getBaseName() + ".dat' REPLACE INTO TABLE " + c.getBaseName() + 
					" FIELDS TERMINATED BY '\\t\\t\\t' LINES TERMINATED BY '\\n\\n\\n' (";

			ArrayList<UMLattribute> attrs = c.listImplementedAttributes();
			for(int j=0; j<attrs.size()-1; j++) {
				batchSql += attrs.get(j).getBaseName() + ",";
			}
			batchSql +=  attrs.get(attrs.size()-1).getBaseName()  + ");\n";			
			
		}
		
		File sqlFile = new File(tempDir.getAbsolutePath() + "/sqlFiles/batch_upload.sql");
		FileUtils.writeStringToFile(sqlFile, batchSql);
		filesToZip.put("sqlFiles/"+sqlFile.getName(), sqlFile);
					
		return filesToZip;

	}
        	
    private UMLclass lookupClassFromSheetName(UMLmodel m, String sName) throws Exception {
        
		UMLclass c = null;
		int checkCount = 0;
		Iterator<UMLclass> cIt = m.listClasses().values().iterator();
		while( cIt.hasNext() ) {
			UMLclass cc = cIt.next();
			String nameCheck = cc.getBaseName();
			if( nameCheck.length() > 31) 
				nameCheck = nameCheck.substring(0,31);
			if( nameCheck.equals(sName) ) {
				checkCount++;
				c = cc;
			}
		}
		
		if(checkCount != 1)
			throw new Exception("Error matching Excel Sheets to Classes: Sheet " + sName +
					" has " + checkCount + " possible target classes in the model.");
		
		return c;
		
    } 

    private ArrayList<String> convertTab2DatFile(File in, File out) throws IOException {
		
		String sName = in.getName().replaceAll("\\.txt", "");

		ArrayList<String> firstRow = new ArrayList<String>();

		//
	    // Keep it really low level.
		// trying to avoid nasty memory leaks.
	    //
	    FileReader fr = new FileReader(in);
	    FileWriter fw = new FileWriter(out);
	    int row = 0, col = 0;
	    int charCode;
	    StringBuffer sb = new StringBuffer();
		while((charCode = fr.read()) != -1) {
			char ch = (char) charCode;
						
			if( ch == '\t' ) {
				
				String cell = sb.toString();
				if(cell.length() > 0) {
					if(row==0) {
						firstRow.add(cell);
					} else {
						fw.write(cell + "\t\t\t");
					}
					sb.delete(0, sb.length());
					col++;
				}
				
			} else if( ch == '\r' || ch == '\n' ) {
				
				String cell = sb.toString();
				if(cell.length() > 0) {
					if(row==0) {
						firstRow.add(cell);
					} else {
						fw.write(cell + "\n\n\n");
					}
					sb.delete(0, sb.length());
					row++;
					col = 0;	
				}
				
			} else {
				sb.append(ch);
			}
			
		}
		
		fw.write(sb.toString());
		
		fr.close();
		fw.close();
		
		return firstRow;
				
	}
    
    
	

	

}
