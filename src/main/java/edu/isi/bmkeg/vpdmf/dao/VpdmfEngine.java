package edu.isi.bmkeg.vpdmf.dao;

public interface VpdmfEngine {

	/**
	 * This call generates the dao objects for this engine and initializes them. 
	 * 
	 * @param login
	 * @param password
	 * @param dbName
	 * @throws Exception
	 */
	public void  initializeVpdmfDao(String login, String password, String dbName, String workingDirectory) throws Exception;
	
}
