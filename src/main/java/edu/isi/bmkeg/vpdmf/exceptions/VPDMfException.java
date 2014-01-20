package edu.isi.bmkeg.vpdmf.exceptions;

/**
 * Timestamp: Thu_Jun_19_120936_2003;
 */

import java.lang.Exception;

public class VPDMfException extends Exception {

	public VPDMfException(Exception e) {
		super(e);
	}

	public VPDMfException(String message) {
		super(message);
	}

};
