package edu.isi.bmkeg.vpdmf.controller.queryEngineTools;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.darwinsys.util.FileIO;

import edu.isi.bmkeg.uml.model.UMLattribute;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.utils.Converters;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;
import edu.isi.bmkeg.vpdmf.utils.VPDMfConverters;

public class AdminEngineImpl extends DatabaseEngineImpl implements AdminEngine {

	private static Logger logger = Logger.getLogger(AdminEngineImpl.class);

	public AdminEngineImpl(String login, String password, String uri) {
		super(login, password, uri);
	}

	public void destroyKB() throws Exception {

		// @todo: when connectivity problems fixed with MySQL 4.1
		// reinstate remote connectivity, until then just use 'localhost'
		String localHostName = InetAddress.getLocalHost().getHostName();

		if (!getUri().startsWith(localHostName + "/"))
			return;

		String kbName = getUri().substring(getUri().indexOf("/") + 1,
				getUri().length());

		if (!this.checkRoot(getLogin(), getPassword(), getUri()))
			return;

		//
		// Log on to local system.
		//
		Class.forName("com.mysql.jdbc.Driver").newInstance();

		//
		// Global database operations
		//
		Connection dbConnection = DriverManager.getConnection("jdbc:mysql://"
				+ localHostName + ":3306/", getLogin(), getPassword());

		if (dbConnection == null) {
			throw new Exception("Can't connect!");
		}

		Statement quickStat = dbConnection.createStatement(
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

		//
		// Builds the knowledge base from the selected input scripts
		//
		String sql = "drop database " + kbName;

		if (this.lc)
			sql = sql.toLowerCase();

		quickStat.execute(sql);
		dbConnection.close();

	}

	/**
	 * @todo AdminEngine
	 * 
	 * @param modelName
	 *            String
	 * @throws IOException
	 * @return String
	 */
	protected String getBuildScript(String modelName) throws IOException {

		String c = "";
		String p = VPDMfKnowledgeBaseBuilder.SCRIPT_DIR + "/" + modelName
				+ ".sql";
		try {

			Reader r = new FileReader(p);
			c = FileIO.readerToString(r);

		} catch (Exception e1) {

			String thisLine;
			InputStream is = this.getClass().getClassLoader()
					.getResourceAsStream(p);

			if (is == null)
				return "";

			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while ((thisLine = br.readLine()) != null) {
				if (c.length() > 0)
					c += "\n";
				c += thisLine;
			}

		}

		c = c.replaceAll("\\r", "");
		return c;

	}

	public void renewKbBuild() {

		if (!this.checkRoot(getLogin(), getPassword(), getUri()))
			return;

		try {

			String localHostName = InetAddress.getLocalHost().getHostName();

			String kbName = getUri().substring(getUri().indexOf("/") + 1,
					getUri().length());

			dbConnection = DriverManager.getConnection("jdbc:mysql://"
					+ localHostName + ":3306/" + kbName + "?user="
					+ getLogin() + "&password=" + getPassword()
					);

			if (dbConnection == null) {
				return;
			}

			stat = dbConnection
					.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
							ResultSet.CONCUR_UPDATABLE);

			//
			// Update the model in the database
			//
			String sql = "UPDATE KnowledgeBase SET build=?, buildScript=?, "
					+ "namespace=? WHERE isRoot=1;";

			if (this.lc)
				sql = sql.toLowerCase();

			this.prettyPrintSQL(sql);

			PreparedStatement psmt = dbConnection.prepareStatement(sql);

			UMLmodel m = this.vpdmf.getUmlModel();

			// TODO namespace is not defined in our current view.
			String namespace = m.getTopPackage().getPkgAddress();

			InputStream is = new ByteArrayInputStream(
					VPDMfConverters.vpdmfObjectToByteArray(m));
			psmt.setBinaryStream(1, is, is.available());

			psmt.setString(2, this.getBuildScript(m.getName()));
			psmt.setString(3, namespace);

			psmt.executeUpdate();
			psmt.close();

		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("Can't build this model");
		}

	}

	/**
	 * Dumps the data from the repository to a VPDMf archive
	 * @param targetZip
	 * @throws Exception
	 */
	public void exportData(File targetZip) throws Exception {

		// List classes in model
		UMLmodel m = this.vpdmf.getUmlModel();
		Collection<UMLclass> cVec = m.listClasses().values();

		File dir = new File("temp");
		File commands = new File("temp/commands.txt");
		FileWriter out = new FileWriter(commands);

		File sqlCmdFile = new File("temp/sysgen.sql");
		FileWriter sqlOut = new FileWriter(sqlCmdFile);

		//
		// Make a list of all tables in the database
		//
		Set<String> tableLookup = new HashSet<String>();

		long t = System.currentTimeMillis();
		ResultSet rs = this.executeQueryOnStatement(this.stat, "show tables");
		long deltaT = System.currentTimeMillis() - t;

		if (this.get_verbose())
			System.out
					.print("    Admin Engine, Show tables: " + deltaT + " ms");

		while( rs.next() ) {
			String tName = rs.getString(1);
			tableLookup.add(tName);
		}

		Map<String,File> filesToZip = new HashMap<String,File>();
		for( UMLclass c : cVec ) {
			
			String atts = "";
			
			for( UMLattribute a : c.getAttributes() ) {
				if (a.getToImplement()) {
					if (atts.length() > 0)
						atts += ",";
					atts += a.getBaseName();
				}
			}

			//
			// UGH: to make sure that MySQL uses the order of columns we provide
			// - this works by adding an extra column to the backup text file so
			// there is a forced mismatch between our load statement and the
			// file.
			// In this case MySQL will use the column names in the order that we
			// specify, otherwise it will use it's own order and misassign
			// columns.
			//
			String rAtts = atts;
			UMLattribute a = (UMLattribute) c.getPkArray().get(0);
			atts += "," + a.getBaseName();

			String filepath = dir.getCanonicalPath() + "/" + c.getBaseName()
					+ ".dat";
			filepath = filepath.replaceAll("\\\\", "/");
			File datFile = new File(filepath);
			if (datFile.exists())
				datFile.delete();

			//
			// We count the data to see if there's any rows in this table if
			//
			String sqlCheck = "SELECT count(*) from " + c.getBaseName() + ";";

			this.prettyPrintSQL(sqlCheck);

			if (this.lc)
				sqlCheck = sqlCheck.toLowerCase();

			try {

				rs = this.executeQueryOnStatement(this.stat, sqlCheck);
				rs.absolute(1);
				int count = rs.getInt(1);
				if (count == 0)
					continue;

			} catch (SQLException e) {

				Pattern patt = Pattern.compile("Table '.*' doesn't exist");
				Matcher match = patt.matcher(e.getMessage());
				if (match.find())
					continue;
				else
					e.printStackTrace();
			}

			String sqlDump = "SELECT " + atts + " INTO OUTFILE '" + filepath
					+ "' FIELDS TERMINATED BY '\\t\\t\\t' LINES "
					+ "TERMINATED BY '\\n\\n\\n' FROM " + c.getBaseName() + ";";

			this.prettyPrintSQL(sqlDump);

			if (this.lc)
				sqlDump = sqlDump.toLowerCase();

			boolean fileOK = true;

			try {

				this.executeOnStatement(this.stat, sqlDump);

			} catch (SQLException e) {

				if (e.getMessage().indexOf("doesn't exist") != -1)
					continue;

				logger.debug("WARNING, error dumping data in "
						+ c.getBaseName() + ", skipping this file");
				fileOK = false;

				// throw e;

			}

			if (fileOK) {

				filesToZip.put(datFile.getName(), datFile);

				String restoreSQL = "LOAD DATA LOCAL INFILE 'SUB_FILEPATH_HERE/"
						+ c.getBaseName()
						+ ".dat' REPLACE INTO TABLE "
						+ c.getBaseName()
						+ " FIELDS TERMINATED BY '\\t\\t\\t' LINES TERMINATED BY "
						+ "'\\n\\n\\n' (" + rAtts + ");";

				out.write(restoreSQL + "\n");

			}

		}
		out.close();
		filesToZip.put(commands.getName(), commands);

		//
		// Save the mysql script that
		// generated the database into a file
		// for subsequent retrieval
		//

		String sql = "SELECT buildScript FROM KnowledgeBase WHERE isRoot=1;";

		if (this.lc)
			sql = sql.toLowerCase();

		rs = this.executeQueryOnStatement(this.stat, sql);
		rs.next();
		String scriptsql = rs.getString(1) + "\n";
		sqlOut.write(scriptsql);
		sqlOut.close();
		
		filesToZip.put(sqlCmdFile.getName(),sqlCmdFile);

		if (targetZip.getName().indexOf(".") == -1) {
			targetZip = new File(targetZip.getAbsolutePath() + ".nar");
		}

		Converters.zipIt(filesToZip, targetZip);

	}

};
