package liquibase.ext.mongodb.database.adapters;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import liquibase.database.LiquibaseExtDriver;
import liquibase.resource.ResourceAccessor;

/**
 * Implements the standard java.sql.Driver interface to allow Mongo integration to better fit into what Liquibase expects.
 *
 * Based off of: https://github.com/liquibase/liquibase-hibernate/pull/159/files#diff-22d7c3872ad37b667ff062dd26a7b4c3
 */
public class MongoDriver implements Driver, LiquibaseExtDriver {

	private ResourceAccessor resourceAccessor;

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		return new MongoConnection();
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return url.startsWith("mongodb:");
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	@Override
	public int getMajorVersion() {
		return 0;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void setResourceAccessor(ResourceAccessor accessor) {
		this.resourceAccessor = accessor;
	}
}
