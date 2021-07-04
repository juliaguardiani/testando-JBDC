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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * HiveStatement.
 *
 */
public class HiveStatement implements java.sql.Statement {
  public static final Logger LOG = LoggerFactory.getLogger(HiveStatement.class.getName());
  public static final int DEFAULT_FETCH_SIZE = 1000;
  private final HiveConnection connection;
  Map<String,String> sessConf = new HashMap<String,String>();
  private int fetchSize = DEFAULT_FETCH_SIZE;
  private boolean isScrollableResultset = false;
  private boolean isOperationComplete = false;
  /**
   * We need to keep a reference to the result set to support the following:
   * <code>
   * statement.execute(String sql);
   * statement.getResultSet();
   * </code>.
   */
  private ResultSet resultSet = null;

  /**
   * Sets the limit for the maximum number of rows that any ResultSet object produced by this
   * Statement can contain to the given number. If the limit is exceeded, the excess rows
   * are silently dropped. The value must be >= 0, and 0 means there is not limit.
   */
  private int maxRows = 0;

  /**
   * Add SQLWarnings to the warningChain if needed.
   */
  private SQLWarning warningChain = null;

  /**
   * Keep state so we can fail certain calls made after close().
   */
  private boolean isClosed = false;

  /**
   * Keep state so we can fail certain calls made after cancel().
   */
  private boolean isCancelled = false;

  /**
   * Keep this state so we can know whether the query in this statement is closed.
   */
  private boolean isQueryClosed = false;

  /**
   * Keep this state so we can know whether the query logs are being generated in HS2.
   */
  private boolean isLogBeingGenerated = true;

  /**
   * Keep this state so we can know whether the statement is submitted to HS2 and start execution
   * successfully.
   */
  private boolean isExecuteStatementFailed = false;

  private int queryTimeout = 0;

  public HiveStatement(HiveConnection connection ) {
    this(connection, false, DEFAULT_FETCH_SIZE);
  }

  public HiveStatement(HiveConnection connection, int fetchSize) {
    this(connection,  false, fetchSize);
  }

  public HiveStatement(HiveConnection connection, boolean isScrollableResultset) {
    this(connection, isScrollableResultset, DEFAULT_FETCH_SIZE);
  }

  public HiveStatement(HiveConnection connection, boolean isScrollableResultset, int fetchSize) {
    this.connection = connection;
    this.isScrollableResultset = isScrollableResultset;
    this.fetchSize = fetchSize;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#addBatch(java.lang.String)
   */

  @Override
  public void addBatch(String sql) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#cancel()
   */

  @Override
  public void cancel() throws SQLException {
    checkConnection("cancel");
    if (isCancelled) {
      return;
    }

//    try {
//      if (stmtHandle != null) {
//        TCancelOperationReq cancelReq = new TCancelOperationReq(stmtHandle);
//        TCancelOperationResp cancelResp = client.CancelOperation(cancelReq);
//        Utils.verifySuccessWithInfo(cancelResp.getStatus());
//      }
//    } catch (SQLException e) {
//      throw e;
//    } catch (Exception e) {
//      throw new SQLException(e.toString(), "08S01", e);
//    }
    isCancelled = true;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#clearBatch()
   */

  @Override
  public void clearBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#clearWarnings()
   */

  @Override
  public void clearWarnings() throws SQLException {
    warningChain = null;
  }

  void closeClientOperation() throws SQLException {
//    try {
//      if (stmtHandle != null) {
//        TCloseOperationReq closeReq = new TCloseOperationReq(stmtHandle);
//        TCloseOperationResp closeResp = client.CloseOperation(closeReq);
//        Utils.verifySuccessWithInfo(closeResp.getStatus());
//      }
//    } catch (SQLException e) {
//      throw e;
//    } catch (Exception e) {
//      throw new SQLException(e.toString(), "08S01", e);
//    }
    isQueryClosed = true;
    isExecuteStatementFailed = false;
//    stmtHandle = null;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#close()
   */
  @Override
  public void close() throws SQLException {
    if (isClosed) {
      return;
    }
    closeClientOperation();
    if (resultSet != null) {
      resultSet.close();
      resultSet = null;
    }
    isClosed = true;
  }

  // JDK 1.7
  public void closeOnCompletion() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#execute(java.lang.String)
   */

  @Override
  public boolean execute(String sql) throws SQLException {
    runAsyncOnServer(sql);
//    TGetOperationStatusResp status = waitForOperationToComplete();
//
//    // The query should be completed by now
//    if (!status.isHasResultSet() && !stmtHandle.isHasResultSet()) {
//      return false;
//    }
//    resultSet =  new HiveQueryResultSet.Builder(this).setClient(client).setSessionHandle(sessHandle)
//        .setStmtHandle(stmtHandle).setMaxRows(maxRows).setFetchSize(fetchSize)
//        .setScrollable(isScrollableResultset)
//        .build();
    return true;
  }

  /**
   * Starts the query execution asynchronously on the server, and immediately returns to the client.
   * The client subsequently blocks on ResultSet#next or Statement#getUpdateCount, depending on the
   * query type. Users should call ResultSet.next or Statement#getUpdateCount (depending on whether
   * query returns results) to ensure that query completes successfully. Calling another execute*
   * method, or close before query completion would result in the async query getting killed if it
   * is not already finished.
   * Note: This method is an API for limited usage outside of Hive by applications like Apache Ambari,
   * although it is not part of the interface java.sql.Statement.
   *
   * @param sql
   * @return true if the first result is a ResultSet object; false if it is an update count or there
   *         are no results
   * @throws SQLException
   */
  public boolean executeAsync(String sql) throws SQLException {
    runAsyncOnServer(sql);
//    TGetOperationStatusResp status = waitForResultSetStatus();
//    if (!status.isHasResultSet()) {
//      return false;
//    }
//    resultSet =
//        new HiveQueryResultSet.Builder(this).setClient(client).setSessionHandle(sessHandle)
//            .setStmtHandle(stmtHandle).setMaxRows(maxRows).setFetchSize(fetchSize)
//            .setScrollable(isScrollableResultset).build();
    return true;
  }

  private void runAsyncOnServer(String sql) throws SQLException {
    checkConnection("execute");

    closeClientOperation();
    initFlags();

//    TExecuteStatementReq execReq = new TExecuteStatementReq(sessHandle, sql);
//    /**
//     * Run asynchronously whenever possible
//     * Currently only a SQLOperation can be run asynchronously,
//     * in a background operation thread
//     * Compilation can run asynchronously or synchronously and execution run asynchronously
//     */
//    execReq.setRunAsync(true);
//    execReq.setConfOverlay(sessConf);
//    execReq.setQueryTimeout(queryTimeout);
//    try {
//      TExecuteStatementResp execResp = client.ExecuteStatement(execReq);
//      Utils.verifySuccessWithInfo(execResp.getStatus());
//      stmtHandle = execResp.getOperationHandle();
//      isExecuteStatementFailed = false;
//    } catch (SQLException eS) {
//      isExecuteStatementFailed = true;
//      isLogBeingGenerated = false;
//      throw eS;
//    } catch (Exception ex) {
//      isExecuteStatementFailed = true;
//      isLogBeingGenerated = false;
//      throw new SQLException(ex.toString(), "08S01", ex);
//    }
  }

  private void checkConnection(String action) throws SQLException {
    if (isClosed) {
      throw new SQLException("Can't " + action + " after statement has been closed");
    }
  }

  private void initFlags() {
    isCancelled = false;
    isQueryClosed = false;
    isLogBeingGenerated = true;
    isExecuteStatementFailed = false;
    isOperationComplete = false;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#execute(java.lang.String, int)
   */

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#execute(java.lang.String, int[])
   */

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
   */

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#executeBatch()
   */

  @Override
  public int[] executeBatch() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#executeQuery(java.lang.String)
   */

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    if (!execute(sql)) {
      throw new SQLException("The query did not generate a result set!");
    }
    return resultSet;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#executeUpdate(java.lang.String)
   */

  @Override
  public int executeUpdate(String sql) throws SQLException {
    execute(sql);
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#executeUpdate(java.lang.String, int)
   */

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
   */

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
   */

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getConnection()
   */

  @Override
  public Connection getConnection() throws SQLException {
    checkConnection("getConnection");
    return this.connection;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getFetchDirection()
   */

  @Override
  public int getFetchDirection() throws SQLException {
    checkConnection("getFetchDirection");
    return ResultSet.FETCH_FORWARD;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getFetchSize()
   */

  @Override
  public int getFetchSize() throws SQLException {
    checkConnection("getFetchSize");
    return fetchSize;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getGeneratedKeys()
   */

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getMaxFieldSize()
   */

  @Override
  public int getMaxFieldSize() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getMaxRows()
   */

  @Override
  public int getMaxRows() throws SQLException {
    checkConnection("getMaxRows");
    return maxRows;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getMoreResults()
   */

  @Override
  public boolean getMoreResults() throws SQLException {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getMoreResults(int)
   */

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getQueryTimeout()
   */

  @Override
  public int getQueryTimeout() throws SQLException {
    checkConnection("getQueryTimeout");
    return 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getResultSet()
   */

  @Override
  public ResultSet getResultSet() throws SQLException {
    checkConnection("getResultSet");
    return resultSet;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getResultSetConcurrency()
   */

  @Override
  public int getResultSetConcurrency() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getResultSetHoldability()
   */

  @Override
  public int getResultSetHoldability() throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getResultSetType()
   */

  @Override
  public int getResultSetType() throws SQLException {
    checkConnection("getResultSetType");
    return ResultSet.TYPE_FORWARD_ONLY;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getUpdateCount()
   */
  @Override
  public int getUpdateCount() throws SQLException {
    checkConnection("getUpdateCount");
    return -1;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#getWarnings()
   */

  @Override
  public SQLWarning getWarnings() throws SQLException {
    checkConnection("getWarnings");
    return warningChain;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#isClosed()
   */

  @Override
  public boolean isClosed() throws SQLException {
    return isClosed;
  }

  // JDK 1.7
  public boolean isCloseOnCompletion() throws SQLException {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#isPoolable()
   */

  @Override
  public boolean isPoolable() throws SQLException {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setCursorName(java.lang.String)
   */

  @Override
  public void setCursorName(String name) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setEscapeProcessing(boolean)
   */

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    if (enable) {
      throw new SQLFeatureNotSupportedException("Method not supported");
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setFetchDirection(int)
   */

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    checkConnection("setFetchDirection");
    if (direction != ResultSet.FETCH_FORWARD) {
      throw new SQLException("Not supported direction " + direction);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setFetchSize(int)
   */

  @Override
  public void setFetchSize(int rows) throws SQLException {
    checkConnection("setFetchSize");
    if (rows > 0) {
      fetchSize = rows;
    } else if (rows == 0) {
      // Javadoc for Statement interface states that if the value is zero
      // then "fetch size" hint is ignored.
      // In this case it means reverting it to the default value.
      fetchSize = DEFAULT_FETCH_SIZE;
    } else {
      throw new SQLException("Fetch size must be greater or equal to 0");
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setMaxFieldSize(int)
   */

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setMaxRows(int)
   */

  @Override
  public void setMaxRows(int max) throws SQLException {
    checkConnection("setMaxRows");
    if (max < 0) {
      throw new SQLException("max must be >= 0");
    }
    maxRows = max;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setPoolable(boolean)
   */

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Statement#setQueryTimeout(int)
   */

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    this.queryTimeout = seconds;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
   */

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.sql.Wrapper#unwrap(java.lang.Class)
   */

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLException("Cannot unwrap to " + iface);
  }



}
