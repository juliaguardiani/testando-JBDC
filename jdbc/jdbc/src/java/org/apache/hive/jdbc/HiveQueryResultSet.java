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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * HiveQueryResultSet.
 *
 */
public class HiveQueryResultSet extends HiveBaseResultSet {

  public static final Logger LOG = LoggerFactory.getLogger(HiveQueryResultSet.class);


  private int fetchSize;
  private int rowsFetched = 0;

  private Iterator<Object[]> fetchedRowsItr;
  private boolean isClosed = false;
  private boolean emptyResultSet = false;
  private boolean isScrollable = false;
  private boolean fetchFirst = false;



  public static class Builder {

    private final Connection connection;
    private final Statement statement;

    /**
     * Sets the limit for the maximum number of rows that any ResultSet object produced by this
     * Statement can contain to the given number. If the limit is exceeded, the excess rows
     * are silently dropped. The value must be >= 0, and 0 means there is not limit.
     */
    private int maxRows = 0;
    private boolean retrieveSchema = true;
    private List<String> colNames =  new ArrayList<>();
    private List<String> colTypes = new ArrayList<>();
    private List<JdbcColumnAttributes> colAttributes;
    private int fetchSize = 50;
    private boolean emptyResultSet = false;
    private boolean isScrollable = false;
    private ReentrantLock transportLock = null;

    public Builder(Statement statement) throws SQLException {
      this.statement = statement;
      this.connection = statement.getConnection();
      colNames.add("valor");
      colNames.add("tempo");
      colNames.add("qualidade");
      colTypes.add("String");
      colTypes.add("String");
      colTypes.add("String");



    }

    public Builder(Connection connection) {
      this.statement = null;
      this.connection = connection;
    }

    public Builder setMaxRows(int maxRows) {
      this.maxRows = maxRows;
      return this;
    }

    public Builder setSchema(List<String> colNames, List<String> colTypes) {
      // no column attributes provided - create list of null attributes.
      List<JdbcColumnAttributes> colAttributes =
          new ArrayList<JdbcColumnAttributes>();
      for (int idx = 0; idx < colTypes.size(); ++idx) {
        colAttributes.add(null);
      }
      return setSchema(colNames, colTypes, colAttributes);
    }

    public Builder setSchema(List<String> colNames, List<String> colTypes,
        List<JdbcColumnAttributes> colAttributes) {
      this.colNames = new ArrayList<String>();
      this.colNames.addAll(colNames);
      this.colTypes = new ArrayList<String>();
      this.colTypes.addAll(colTypes);
      this.colAttributes = new ArrayList<JdbcColumnAttributes>();
      this.colAttributes.addAll(colAttributes);
      this.retrieveSchema = false;
      return this;
    }

    public Builder setFetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
      return this;
    }

    public Builder setEmptyResultSet(boolean emptyResultSet) {
      this.emptyResultSet = emptyResultSet;
      return this;
    }

    public Builder setScrollable(boolean setScrollable) {
      this.isScrollable = setScrollable;
      return this;
    }

    public Builder setTransportLock(ReentrantLock transportLock) {
      this.transportLock = transportLock;
      return this;
    }

    public HiveQueryResultSet build() throws SQLException {
      return new HiveQueryResultSet(this);
    }
  }

  protected HiveQueryResultSet(Builder builder) throws SQLException {
    this.statement = builder.statement;
    this.fetchSize = builder.fetchSize;
    columnNames = new ArrayList<String>();
    normalizedColumnNames = new ArrayList<String>();
    columnTypes = new ArrayList<String>();
    columnAttributes = new ArrayList<JdbcColumnAttributes>();

    this.setSchema(builder.colNames, builder.colTypes, builder.colAttributes);

    this.emptyResultSet = builder.emptyResultSet;

    this.isScrollable = builder.isScrollable;
  }



  /**
   * Set the specified schema to the resultset
   * @param colNames
   * @param colTypes
   */
  private void setSchema(List<String> colNames, List<String> colTypes,
      List<JdbcColumnAttributes> colAttributes) {
    columnNames.addAll(colNames);
    columnTypes.addAll(colTypes);
    columnAttributes.addAll(colAttributes);

    for (String colName : colNames) {
      normalizedColumnNames.add(colName.toLowerCase());
    }
  }

  @Override
  public void close() throws SQLException {
    if (this.statement != null && (this.statement instanceof HiveStatement)) {
      HiveStatement s = (HiveStatement) this.statement;
      s.closeClientOperation();
    }

    isClosed = true;
  }


  /**
   * Moves the cursor down one row from its current position.
   *
   * @see java.sql.ResultSet#next()
   * @throws SQLException
   *           if a database access error occurs.
   */
  public boolean next() throws SQLException {
    return true;
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException {
    if (isClosed) {
      throw new SQLException("Resultset is closed");
    }
    return super.getMetaData();
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    if (isClosed) {
      throw new SQLException("Resultset is closed");
    }
    fetchSize = rows;
  }

  @Override
  public int getType() throws SQLException {
    if (isClosed) {
      throw new SQLException("Resultset is closed");
    }
    if (isScrollable) {
      return ResultSet.TYPE_SCROLL_INSENSITIVE;
    } else {
      return ResultSet.TYPE_FORWARD_ONLY;
    }
  }

  @Override
  public int getFetchSize() throws SQLException {
    if (isClosed) {
      throw new SQLException("Resultset is closed");
    }
    return fetchSize;
  }

  public <T> T getObject(String columnLabel, Class<T> type)  throws SQLException {
    //JDK 1.7
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  public <T> T getObject(int columnIndex, Class<T> type)  throws SQLException {
    //JDK 1.7
    throw new SQLFeatureNotSupportedException("Method not supported");
  }

  /**
   * Moves the cursor before the first row of the resultset.
   *
   * @see java.sql.ResultSet#next()
   * @throws SQLException
   *           if a database access error occurs.
   */
  @Override
  public void beforeFirst() throws SQLException {
    if (isClosed) {
      throw new SQLException("Resultset is closed");
    }
    if (!isScrollable) {
      throw new SQLException("Method not supported for TYPE_FORWARD_ONLY resultset");
    }
    fetchFirst = true;
    rowsFetched = 0;
  }

  @Override
  public boolean isBeforeFirst() throws SQLException {
    if (isClosed) {
      throw new SQLException("Resultset is closed");
    }
    return (rowsFetched == 0);
  }

  @Override
  public int getRow() throws SQLException {
    return rowsFetched;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }
}
