package liquibase.ext.mongodb.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.util.LiquibaseUtil;
import liquibase.util.StringUtil;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Logger;

public class MongoClientDriver implements Driver {

    @Override
    public Connection connect(final String url, final Properties info) {
        //Not applicable for non JDBC DBs
        throw new UnsupportedOperationException("Cannot initiate a SQL Connection for a NoSql DB");
    }

    public MongoClient connect(final ConnectionString connectionString) throws DatabaseException {
        final MongoClient client;

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applicationName(getFullApplicationName())
                .build();

        try {
            client = MongoClients.create(settings);
        } catch (final Exception e) {
            throw new DatabaseException("Connection could not be established to: "
                    + connectionString.getConnectionString(), e);
        }
        return client;
    }

    private String getFullApplicationName() {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader("pom.xml"));

            boolean isCommercial = model.getArtifactId().contains("commercial");
            String buildVersion = LiquibaseUtil.getBuildVersion();
            String extVersion = model.getVersion();
            String appType = isCommercial ? "PRO" : "OSS";
            String extType = isCommercial ? "ProExt" : "OssExt";
            URL url = Scope.getCurrentScope().getClassLoader().getResource("META-INF/MANIFEST.MF");
            Manifest manifest = new Manifest(url.openStream());
            Attributes attr = manifest.getMainAttributes();

            return String.join("_", "Liquibase", appType, buildVersion, extType, extVersion);
        } catch (XmlPullParserException | IOException e) {
            Scope.getCurrentScope().getLog(this.getClass()).warning("Failed to extract application full name for current connection.");
        }
        return "";
    }

    @Override
    public boolean acceptsURL(final String url) {
        final String trimmedUrl = StringUtil.trimToEmpty(url);
        return trimmedUrl.startsWith(MongoConnection.MONGO_DNS_PREFIX) || trimmedUrl.startsWith(MongoConnection.MONGO_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
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
    public Logger getParentLogger() {
        return (Logger) Scope.getCurrentScope().getLog(getClass());
    }
}
