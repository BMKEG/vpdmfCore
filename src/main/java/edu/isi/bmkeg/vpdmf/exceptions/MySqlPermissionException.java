package edu.isi.bmkeg.vpdmf.exceptions;

import java.sql.*;

public class MySqlPermissionException extends SQLException {
  private String errorCode;
  private String suggestion;

  public MySqlPermissionException(String message,
                                  String errorCode,
                                  String suggestion) {
    super(message);

    this.errorCode = errorCode;
    this.suggestion = suggestion;
  }

  public String get_errorCode() {
    return this.errorCode;
  }

  public String get_suggestion() {
    return this.suggestion;
  }
}
