package liquibase.ext.mongodb.database

import com.mongodb.ConnectionString
import com.mongodb.MongoCredential
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import liquibase.Scope
import liquibase.exception.DatabaseException
import org.bson.codecs.configuration.CodecRegistry
import spock.lang.Specification

import java.sql.Driver

class MongoConnectionTest extends Specification {
    
    def "should support mongodb urls"() {
        given:
        def connection = new MongoConnection()
        
        expect:
        connection.supports("mongodb://localhost:27017/test")
        connection.supports("mongodb+srv://user:password@cluster.mongodb.net/test")
        !connection.supports("jdbc:mysql://localhost:3306/test")
        !connection.supports(null)
    }
    
    def "should detect required dependencies"() {
        when:
        MongoConnection.showErrorMessageIfSomeRequiredDependenciesAreNotPresent(true)
        
        then:
        // This test should pass if all MongoDB dependencies are available in the classpath
        noExceptionThrown()
    }
    
    def "should get catalog name from database"() {
        given:
        def connection = new MongoConnection()
        def mongoDatabase = Mock(MongoDatabase)
        connection.mongoDatabase = mongoDatabase
        
        when:
        def catalog = connection.getCatalog()
        
        then:
        1 * mongoDatabase.getName() >> "testDB"
        catalog == "testDB"
    }
    
    def "should handle exception when getting catalog"() {
        given:
        def connection = new MongoConnection()
        def mongoDatabase = Mock(MongoDatabase)
        connection.mongoDatabase = mongoDatabase
        
        when:
        connection.getCatalog()
        
        then:
        1 * mongoDatabase.getName() >> { throw new RuntimeException("Test exception") }
        thrown(DatabaseException)
    }
    
    def "should return MongoDB as database product name"() {
        given:
        def connection = new MongoConnection()
        
        when:
        def productName = connection.getDatabaseProductName()
        
        then:
        productName == "MongoDB"
    }
    
    def "should return URL from connection string"() {
        given:
        def connection = new MongoConnection()
        def connectionString = Mock(ConnectionString)
        connectionString.getHosts() >> ["host1:27017", "host2:27017"]
        connection.connectionString = connectionString
        
        when:
        def url = connection.getURL()
        
        then:
        url == "host1:27017,host2:27017"
    }
    
    def "should return empty URL when connection string is null"() {
        given:
        def connection = new MongoConnection()
        connection.connectionString = null
        
        when:
        def url = connection.getURL()
        
        then:
        url == ""
    }
    
    def "should return username from connection string"() {
        given:
        def connection = new MongoConnection()
        def connectionString = Mock(ConnectionString)
        // We use spy instead of mock because MongoCredential is a final class
        def credential = MongoCredential.createCredential("testUser", "admin", "password".toCharArray())
        connectionString.getCredential() >> credential
        connection.connectionString = connectionString
        
        when:
        def username = connection.getConnectionUserName()
        
        then:
        username == "testUser"
    }
    
    def "should return empty username when connection string is null"() {
        given:
        def connection = new MongoConnection()
        connection.connectionString = null
        
        when:
        def username = connection.getConnectionUserName()
        
        then:
        username == ""
    }
    
    def "should check if connection is closed"() {
        given:
        def connection = new MongoConnection()
        
        when:
        def closed = connection.isClosed()
        
        then:
        closed == true
        
        when:
        connection.mongoClient = Mock(MongoClient)
        closed = connection.isClosed()
        
        then:
        closed == false
    }
    
    def "should open connection"() {
        given:
        def connection = new MongoConnection()
        def driver = Mock(MongoClientDriver)
        def mongoClient = Mock(MongoClient)
        def mongoDatabase = Mock(MongoDatabase)
        def properties = new Properties()
        
        when:
        connection.open("mongodb://localhost:27017/testDB", driver, properties)
        
        then:
        1 * driver.connect(_, _) >> mongoClient
        1 * mongoClient.getDatabase("testDB") >> mongoDatabase
        1 * mongoDatabase.withCodecRegistry(_) >> mongoDatabase
        connection.mongoClient == mongoClient
        connection.mongoDatabase == mongoDatabase
    }
    
    def "should close connection"() {
        given:
        def connection = new MongoConnection()
        def mongoClient = Mock(MongoClient)
        connection.mongoClient = mongoClient
        
        when:
        connection.close()
        
        then:
        1 * mongoClient.close()
        connection.mongoClient == null
    }
    
    def "should handle exception when closing connection"() {
        given:
        def connection = new MongoConnection()
        def mongoClient = Mock(MongoClient)
        connection.mongoClient = mongoClient
        
        when:
        connection.close()
        
        then:
        1 * mongoClient.close() >> { throw new RuntimeException("Test exception") }
        thrown(DatabaseException)
    }
    
    def "should inject credentials to URL"() {
        given:
        def connection = new MongoConnection()
        def properties = new Properties()
        properties.setProperty("user", "testUser")
        properties.setProperty("password", "testPassword")
        
        when:
        def result = connection.injectCredentials("mongodb://localhost:27017/testDB", properties)
        
        then:
        result == "mongodb://testUser:testPassword@localhost:27017/testDB"
    }
}