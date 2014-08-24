/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import java.sql.*;
import java.util.*;
import java.util.Date;

import mongodb.jdbc.MongoPreparedStatement;

public class NamedParameterStatement {

    /**
     * Native statement
     */
    private MongoPreparedStatement statement;

    /**
     * Index mapping
     */
    private final Map<String, List<Integer>> indexMap;

    /**
     * Query string
     */
    private final String parsedQuery;

    /**
     * Database connection
     */
    private final AdvancedConnection connection;

    private int autoGeneratedKeys;
    
    private boolean failed;

    /**
     * Initialize statement
     */
    public NamedParameterStatement(AdvancedConnection connection, String query) {

        indexMap = new HashMap<String, List<Integer>>();
        parsedQuery = parse(query, indexMap);
        this.connection = connection;                
    }

    /**
     * Parse query
     */
    static String parse(String query, Map<String, List<Integer>> paramMap) {

        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        for(int i = 0; i < length; i++) {

            char c = query.charAt(i);

            // String end
            if (inSingleQuote) {
                if (c == '\'') inSingleQuote = false;
            } else if (inDoubleQuote) {
                if (c == '"') inDoubleQuote = false;
            } else {

                // String begin
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length &&
                        Character.isJavaIdentifierStart(query.charAt(i + 1))) {

                    // Identifier name
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) j++;

                    String name = query.substring(i + 1, j);
                    c = '?';
                    i += name.length();

                    // Add to list
                    List<Integer> indexList = paramMap.get(name);
                    if (indexList == null) {
                        indexList = new LinkedList<Integer>();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }

            parsedQuery.append(c);
        }

        return parsedQuery.toString();
    }

    public void reset(boolean resetConnection) throws SQLException {
        if (statement != null) {        	
            statement.close();
            statement = null;
        }
        if (resetConnection) {        	
            connection.reset();            
        }
        statement =(MongoPreparedStatement) connection.getInstance().prepareStatement(parsedQuery);         		
        //statement = connection.getInstance().prepareStatement(parsedQuery, autoGeneratedKeys);
        
        failed = false;
    }

    public void prepare(int autoGeneratedKeys) throws SQLException {
        this.autoGeneratedKeys = autoGeneratedKeys;
        try {
            if (statement == null) {            	
                reset(false);
            } else if (failed || statement.getWarnings() != null) {
                reset(true);
            }
        } catch (SQLException firstError) {
            try {
                reset(true);
            } catch (SQLException secondError) {
                Log.warning(secondError);
                failed = true;
                throw secondError;
            }
        }
    }

    public void prepare() throws SQLException {
        prepare(Statement.NO_GENERATED_KEYS);
    }

    /**
     * Execute query with result
     */
    public ResultSet executeQuery() throws SQLException {
        try {
            return statement.executeQuery();
        } catch (SQLException error) {
            failed = true;
            throw error;
        }
    }

    /**
     * Executes query without result
     */
    public int executeUpdate() throws SQLException {
        try {
            return statement.executeUpdate();
        } catch (SQLException error) {
            failed = true;
            throw error;
        }
    }

    /**
     * Return generated keys
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    /**
     * Immediately closes the statement
     */
    public void close() throws SQLException {
        statement.close();
    }

    public void setInt(String name, Integer value) throws SQLException {

        List<Integer> indexList = indexMap.get(name);
        if (indexList != null) for (Integer index: indexList) {
            if (value != null) {
                statement.setInt(index, value);
            } else {
                statement.setNull(index, Types.INTEGER);
            }
        }
    }

    public void setLong(String name, Long value) throws SQLException {

        List<Integer> indexList = indexMap.get(name);
        if (indexList != null) for (Integer index: indexList) {
            if (value != null) {
                statement.setLong(index, value);
            } else {
                statement.setNull(index, Types.INTEGER);
            }
        }
    }

    public void setBoolean(String name, Boolean value) throws SQLException {

        List<Integer> indexList = indexMap.get(name);
        if (indexList != null) for (Integer index: indexList) {
            if (value != null) {
                statement.setBoolean(index, value);
            } else {
                statement.setNull(index, Types.BOOLEAN);
            }
        }
    }

    public void setDouble(String name, Double value) throws SQLException {

        List<Integer> indexList = indexMap.get(name);
        if (indexList != null) for (Integer index: indexList) {
            if (value != null) {
                statement.setDouble(index, value);
            } else {
                statement.setNull(index, Types.DOUBLE);
            }
        }
    }

    public void setTimestamp(String name, Date value) throws SQLException {

        List<Integer> indexList = indexMap.get(name);
        if (indexList != null) for (Integer index: indexList) {
            if (value != null) {
                statement.setTimestamp(index, new Timestamp(value.getTime()));
            } else {
                statement.setNull(index, Types.TIMESTAMP);
            }
        }
    }

    public void setString(String name, String value) throws SQLException {

        List<Integer> indexList = indexMap.get(name);
        if (indexList != null) for (Integer index: indexList) {
            if (value != null) {
                statement.setString(index, value);
            } else {
                statement.setNull(index, Types.VARCHAR);
            }
        }
    }

}
