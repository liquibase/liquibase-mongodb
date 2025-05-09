package liquibase.ext.mongodb.database

import com.mongodb.ConnectionString
import com.mongodb.client.MongoClient
import spock.lang.Specification

class MongoClientDriverTest extends Specification {
    
    def "should accept MongoDB URLs"() {
        given:
        def driver = new MongoClientDriver()
        
        expect:
        driver.acceptsURL("mongodb://localhost:27017/test")
        driver.acceptsURL("mongodb+srv://username:password@cluster.mongodb.net/test")
        !driver.acceptsURL("jdbc:mysql://localhost:3306/test")
        !driver.acceptsURL(null)
        !driver.acceptsURL("")
    }
    
    def "should throw UnsupportedOperationException when connect with JDBC URL"() {
        given:
        def driver = new MongoClientDriver()
        def url = "mongodb://localhost:27017/test"
        def info = new Properties()
        
        when:
        driver.connect(url, info)
        
        then:
        thrown(UnsupportedOperationException)
    }
    
    def "should return empty property info"() {
        given:
        def driver = new MongoClientDriver()
        def url = "mongodb://localhost:27017/test"
        def info = new Properties()
        
        when:
        def propertyInfo = driver.getPropertyInfo(url, info)
        
        then:
        propertyInfo.length == 0
    }
    
    def "should return version information"() {
        given:
        def driver = new MongoClientDriver()
        
        expect:
        driver.getMajorVersion() == 0
        driver.getMinorVersion() == 0
    }
    
    def "should not be JDBC compliant"() {
        given:
        def driver = new MongoClientDriver()
        
        expect:
        !driver.jdbcCompliant()
    }
    
    
    def "should connect with connection string and default app name"() {
        given:
        def driver = Spy(MongoClientDriver)
        def connectionString = Mock(ConnectionString)
        def mongoClient = Mock(MongoClient)
        
        when:
        def result = driver.connect(connectionString)
        
        then:
        1 * driver.connect(connectionString, "Liquibase") >> mongoClient
        result == mongoClient
    }
}