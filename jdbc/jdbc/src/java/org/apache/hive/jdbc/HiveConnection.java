/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hive.jdbc;

import org.apache.hive.jdbc.Utils.JdbcConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * HiveConnection.
 *
 */
public class HiveConnection implements java.sql.Connection {
  public static final Logger LOG = LoggerFactory.getLogger(HiveConnection.class.getName());

  private String jdbcUriString;
  private String host;
  private int port;
  private final Map<String, String> sessConfMap;
  private JdbcConnectionParams connParams;

  private boolean isClosed = false;
  private SQLWarning warningChain = null;

  private int fetchSize = HiveStatement.DEFAULT_FETCH_SIZE;


  public HiveConnection(String uri, Properties info) throws SQLException {

    try {
      connParams = Utils.parseURL(uri, info);

      jdbcUriString = connParams.getJdbcUriString();
      // JDBC URL: jdbc:hive2://<host>:<port>/dbName;sess_var_list?hive_conf_list#hive_var_list
      // each list: <key1>=<val1>;<key2>=<val2> and so on
      // sess_var_list -> sessConfMap
      // hive_conf_list -> hiveConfMap
      // hive_var_list -> hiveVarMap
      host = connParams.getHost();
      port = connParams.getPort();
      sessConfMap = connParams.getSessionVars();


      if (sessConfMap.containsKey(JdbcConnectionParams.FETCH_SIZE)) {
        fetchSize = Integer.parseInt(sessConfMap.get(JdbcConnectionParams.FETCH_SIZE));
      }
    } catch (Exception e) {

      throw new SQLException(e.getMessage());
    }


  }


  public String getConnectedUrl() {
    return jdbcUriString;
  }

  /**
   * @return username from sessConfMap
   */
  private String getUserName() {
    return getSessionValue(JdbcConnectionParams.AUTH_USER, JdbcConnectionParams.ANONYMOUS_USER);
  }

  /**
   * @return password from sessConfMap
   */
  private String getPassword() {
    return getSessionValue(JdbcConnectionParams.AUTH_PASSWD, JdbcConnectionParams.ANONYMOUS_PASSWD);
  }

  private boolean isSslConnection() {
    return "true".equalsIgnoreCase(sessConfMap.get(JdbcConnectionParams.USE_SSL));
  }

  private boolean isKerberosAuthMode() {
    return !JdbcConnectionParams.AUTH_SIMPLE.equals(sessConfMap.get(JdbcConnectionParams.AUTH_TYPE))
        && sessConfMap.containsKey(JdbcConnectionParams.AUTH_PRINCIPAL);
  }

  private boolean isHttpTransportMode() {
    String transportMode = sessConfMap.get(JdbcConnectionParams.TRANSPORT_MODE);
    if(transportMode != null && (transportMode.equalsIgnoreCase("http"))) {
      return true;
    }
    return false;
  }

  private boolean isZkDynamicDiscoveryMode() {
    return (sessConfMap.get(JdbcConnectionParams.SERVICE_DISCOVERY_MODE) != null)
      && (JdbcConnectionParams.SERVICE_DISCOVERY_MODE_ZOOKEEPER.equalsIgnoreCase(sessConfMap
      .get(JdbcConnectionParams.SERVICE_DISCOVERY_MODE)));
  }

  private void logZkDiscoveryMessage(String message) {
    if (isZkDynamicDiscoveryMode()) {
      LOG.info(message);
    }
  }

  /**
   * Lookup varName in sessConfMap, if its null or empty return the default
   * value varDefault
   * @param varName
   * @param varDefault
   * @return
   */
  private String getSessionValue(String varName, String varDefault) {
    String varValue = sessConfMap.get(varName);
    if ((varValue == null) || varValue.isEmpty()) {
      varValue = varDefault;
    }
    return varValue;
  }


  public void abort(Executor executor) throws SQLException {
    // JDK 1.7
    throw new SQLFeatureNotSupportedException("Method not supported");
  }


  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#clearWarnings()
   */

  @Override
  public void clearWarnings() throws SQLException {
    warningChain = null;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#close()
   */

  @Override
  public void close() throws SQLException {
    if (!isClosed) {

    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#commit()
   */

  @Override
  public void commit() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createArrayOf(java.lang.String,
   * java.lang.Object[])
   */

  @Override
  public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createBlob()
   */

  @Override
  public Blob createBlob() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createClob()
   */

  @Override
  public Clob createClob() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createNClob()
   */

  @Override
  public NClob createNClob() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createSQLXML()
   */

  @Override
  public SQLXML createSQLXML() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /**
   * Creates a Statement object for sending SQL statements to the database.
   *
   * @throws SQLException
   *           if a database access error occurs.
   * @see java.sql.Connection#createStatement()
   */

  @Override
  public Statement createStatement() throws SQLException {
    if (isClosed) {
      throw new SQLException("Can't create Statement, connection is closed");
    }
    return new HiveStatement(this, fetchSize);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createStatement(int, int)
   */

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency)
      throws SQLException {
    if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
      throw new SQLException("Statement with resultset concurrency " +
          resultSetConcurrency + " is not supported", "HYC00"); // Optional feature not implemented
    }
    if (resultSetType == ResultSet.TYPE_SCROLL_SENSITIVE) {
      throw new SQLException("Statement with resultset type " + resultSetType +
          " is not supported", "HYC00"); // Optional feature not implemented
    }
    return new HiveStatement(this,
        resultSetType == ResultSet.TYPE_SCROLL_INSENSITIVE, fetchSize);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createStatement(int, int, int)
   */

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#createStruct(java.lang.String, java.lang.Object[])
   */

  @Override
  public Struct createStruct(String typeName, Object[] attributes)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getAutoCommit()
   */

  @Override
  public boolean getAutoCommit() throws SQLException {
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getCatalog()
   */

  @Override
  public String getCatalog() throws SQLException {
    return "";
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getClientInfo()
   */

  @Override
  public Properties getClientInfo() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getClientInfo(java.lang.String)
   */

  @Override
  public String getClientInfo(String name) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getHoldability()
   */

  @Override
  public int getHoldability() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getMetaData()
   */

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    if (isClosed) {
      throw new SQLException("Connection is closed");
    }
    return new HiveDatabaseMetaData(this);
  }

  public int getNetworkTimeout() throws SQLException {
    // JDK 1.7
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  public String getSchema() throws SQLException {
    if (isClosed) {
      throw new SQLException("Connection is closed");
    }
    try (Statement stmt = createStatement();
         ResultSet res = stmt.executeQuery("SELECT current_database()")) {
      if (!res.next()) {
        throw new SQLException("Failed to get schema information");
      }
      return res.getString(1);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getTransactionIsolation()
   */

  @Override
  public int getTransactionIsolation() throws SQLException {
    return Connection.TRANSACTION_NONE;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getTypeMap()
   */

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#getWarnings()
   */

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return warningChain;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#isClosed()
   */

  @Override
  public boolean isClosed() throws SQLException {
    return isClosed;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#isReadOnly()
   */

  @Override
  public boolean isReadOnly() throws SQLException {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#isValid(int)
   */

  @Override
  public boolean isValid(int timeout) throws SQLException {
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#nativeSQL(java.lang.String)
   */

  @Override
  public String nativeSQL(String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareCall(java.lang.String)
   */

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
   */

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType,
      int resultSetConcurrency) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
   */

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType,
      int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareStatement(java.lang.String)
   */

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return new HivePreparedStatement(this, sql);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareStatement(java.lang.String, int)
   */

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
      throws SQLException {
    return new HivePreparedStatement(this, sql);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
   */

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareStatement(java.lang.String,
   * java.lang.String[])
   */

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames)
      throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
   */

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType,
      int resultSetConcurrency) throws SQLException {
    return new HivePreparedStatement(this, sql);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
   */

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType,
      int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
   */

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#rollback()
   */

  @Override
  public void rollback() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#rollback(java.sql.Savepoint)
   */

  @Override
  public void rollback(Savepoint savepoint) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setAutoCommit(boolean)
   */

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    // Per JDBC spec, if the connection is closed a SQLException should be thrown.
    if(isClosed) {
      throw new SQLException("Connection is closed");
    }
    // The auto-commit mode is always enabled for this connection. Per JDBC spec,
    // if setAutoCommit is called and the auto-commit mode is not changed, the call is a no-op.
    if (!autoCommit) {
      LOG.warn("Request to set autoCommit to false; Hive does not support autoCommit=false.");
      SQLWarning warning = new SQLWarning("Hive does not support autoCommit=false");
      if (warningChain == null) warningChain = warning;
      else warningChain.setNextWarning(warning);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setCatalog(java.lang.String)
   */

  @Override
  public void setCatalog(String catalog) throws SQLException {
    // Per JDBC spec, if the driver does not support catalogs,
    // it will silently ignore this request.
    if (isClosed) {
      throw new SQLException("Connection is closed");
    }
    return;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setClientInfo(java.util.Properties)
   */

  @Override
  public void setClientInfo(Properties properties)
      throws SQLClientInfoException {
    // TODO Auto-generated method stub
    throw new SQLClientInfoException("Method not supported", null);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setClientInfo(java.lang.String, java.lang.String)
   */

  @Override
  public void setClientInfo(String name, String value)
      throws SQLClientInfoException {
    // TODO Auto-generated method stub
    throw new SQLClientInfoException("Method not supported", null);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setHoldability(int)
   */

  @Override
  public void setHoldability(int holdability) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    // JDK 1.7
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setReadOnly(boolean)
   */

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {
    // Per JDBC spec, if the connection is closed a SQLException should be thrown.
    if (isClosed) {
      throw new SQLException("Connection is closed");
    }
    // Per JDBC spec, the request defines a hint to the driver to enable database optimizations.
    // The read-only mode for this connection is disabled and cannot be enabled (isReadOnly always returns false).
    // The most correct behavior is to throw only if the request tries to enable the read-only mode.
    if(readOnly) {
      throw new SQLException("Enabling read-only mode not supported");
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setSavepoint()
   */

  @Override
  public Savepoint setSavepoint() throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setSavepoint(java.lang.String)
   */

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  public void setSchema(String schema) throws SQLException {
    // JDK 1.7
    if (isClosed) {
      throw new SQLException("Connection is closed");
    }
    if (schema == null || schema.isEmpty()) {
      throw new SQLException("Schema name is null or empty");
    }
    Statement stmt = createStatement();
    stmt.execute("use " + schema);
    stmt.close();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setTransactionIsolation(int)
   */

  @Override
  public void setTransactionIsolation(int level) throws SQLException {
    // TODO: throw an exception?
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Connection#setTypeMap(java.util.Map)
   */

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
   */

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Wrapper#unwrap(java.lang.Class)
   */

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    // TODO Auto-generated method stub
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

}
