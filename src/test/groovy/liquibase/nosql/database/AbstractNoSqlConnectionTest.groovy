package liquibase.nosql.database

import liquibase.database.Database
import liquibase.exception.DatabaseException
import spock.lang.Specification

class AbstractNoSqlConnectionTest extends Specification {
    
    def "should return PRIORITY_DEFAULT + 500 for getPriority"() {
        given:
        def connection = new TestNoSqlConnection()
        
        expect:
        connection.getPriority() == connection.PRIORITY_DEFAULT + 500
    }
    
    def "should return false for getAutoCommit"() {
        given:
        def connection = new TestNoSqlConnection()
        
        expect:
        !connection.getAutoCommit()
    }
    
    def "should not throw exception for setAutoCommit"() {
        given:
        def connection = new TestNoSqlConnection()
        
        when:
        connection.setAutoCommit(true)
        
        then:
        noExceptionThrown()
    }
    
    def "should return null for nativeSQL"() {
        given:
        def connection = new TestNoSqlConnection()
        
        expect:
        connection.nativeSQL("sql") == null
    }
    
    def "should return '0' for getDatabaseProductVersion"() {
        given:
        def connection = new TestNoSqlConnection()
        
        expect:
        connection.getDatabaseProductVersion() == "0"
    }
    
    def "should return 0 for getDatabaseMajorVersion"() {
        given:
        def connection = new TestNoSqlConnection()
        
        expect:
        connection.getDatabaseMajorVersion() == 0
    }
    
    def "should return 0 for getDatabaseMinorVersion"() {
        given:
        def connection = new TestNoSqlConnection()
        
        expect:
        connection.getDatabaseMinorVersion() == 0
    }
    
    def "should not throw exception for attached"() {
        given:
        def connection = new TestNoSqlConnection()
        def database = Mock(Database)
        
        when:
        connection.attached(database)
        
        then:
        noExceptionThrown()
    }
    
    def "should not throw exception for commit"() {
        given:
        def connection = new TestNoSqlConnection()
        
        when:
        connection.commit()
        
        then:
        noExceptionThrown()
    }
    
    def "should not throw exception for rollback"() {
        given:
        def connection = new TestNoSqlConnection()
        
        when:
        connection.rollback()
        
        then:
        noExceptionThrown()
    }
    
    private static class TestNoSqlConnection extends AbstractNoSqlConnection {
        @Override
        boolean supports(String url) {
            return url != null && url.startsWith("test://")
        }

        @Override
        String getURL() {
            return "test://localhost/db"
        }

        @Override
        String getCatalog() throws DatabaseException {
            return "testCatalog"
        }

        @Override
        String getDatabaseProductName() throws DatabaseException {
            return "TestDB"
        }

        @Override
        String getConnectionUserName() {
            return "testUser"
        }

        @Override
        boolean isClosed() throws DatabaseException {
            return false
        }

        @Override
        void open(String url, java.sql.Driver driverObject, Properties driverProperties) throws DatabaseException {
            // Do nothing for test
        }

        @Override
        void close() throws DatabaseException {
            // Do nothing for test
        }
    }
}