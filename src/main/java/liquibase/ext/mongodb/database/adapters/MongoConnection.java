package liquibase.ext.mongodb.database.adapters;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Implements java.sql.Connection in order to pretend a Mongo connection is a database in order to fit into the Liquibase framework.
 *
 * Based off of: https://github.com/bernerbrau/liquibase-hibernate/blob/283195633916c3faca5db96f7f90082cf789e089/src/main/java/liquibase/ext/hibernate/database/connection/HibernateConnection.java
 */

public class MongoConnection implements Connection {
	@Override
	public Statement createStatement() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public String nativeSQL(String sql) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void commit() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void rollback() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void close() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public boolean isClosed() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setCatalog(String catalog) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public String getCatalog() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void clearWarnings() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setHoldability(int holdability) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public int getHoldability() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Clob createClob() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Blob createBlob() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public NClob createNClob() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setSchema(String schema) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public String getSchema() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void abort(Executor executor) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new IllegalStateException("This method should not be called");
	}
}
