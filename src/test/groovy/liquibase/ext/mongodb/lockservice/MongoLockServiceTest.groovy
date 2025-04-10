package liquibase.ext.mongodb.lockservice

import liquibase.Scope
import liquibase.exception.DatabaseException
import liquibase.exception.LockException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CountCollectionByNameStatement
import liquibase.ext.mongodb.statement.DropCollectionStatement
import liquibase.ext.mongodb.statement.FindAllStatement
import liquibase.lockservice.DatabaseChangeLogLock
import liquibase.nosql.executor.NoSqlExecutor
import org.bson.Document
import spock.lang.Specification

class MongoLockServiceTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def executor = Mock(NoSqlExecutor)
    def mockScope = Mock(Scope)
    def mockLogger = Mock(liquibase.logging.Logger)
    
    def lockService = new MongoLockService()
    
    def setup() {
        lockService.setDatabase(database)
        database.getDatabaseChangeLogLockTableName() >> "DATABASECHANGELOGLOCK"
        database.getDatabaseProductName() >> MongoLiquibaseDatabase.MONGODB_PRODUCT_NAME
        database.getConnection() >> Mock(liquibase.database.DatabaseConnection) {
            getCatalog() >> "testdb"
        }
        
        // Mock scope and logger
        Scope.getCurrentScope() >> mockScope
        mockScope.getLog(_ as Class) >> mockLogger
        
        // Mock executor access through NoSqlService
        database.getNoSqlExecutor() >> executor
    }
    
    def "should support MongoDB database"() {
        expect:
        lockService.supports(database)
    }
    
    def "should check if repository exists"() {
        given:
        executor.queryForLong(_ as CountCollectionByNameStatement) >> 1L
        
        when:
        def result = lockService.existsRepository()
        
        then:
        result
    }
    
    def "should create repository"() {
        when:
        lockService.createRepository()
        
        then:
        1 * executor.execute(_ as CreateChangeLogLockCollectionStatement)
    }
    
    def "should drop repository"() {
        when:
        lockService.dropRepository()
        
        then:
        1 * executor.execute(_ as DropCollectionStatement)
    }
    
    def "should adjust repository when enabled"() {
        given:
        database.getAdjustTrackingTablesOnStartup() >> true
        
        when:
        lockService.adjustRepository()
        
        then:
        1 * executor.execute(_ as AdjustChangeLogLockCollectionStatement)
    }
    
    def "should skip repository adjustment when disabled"() {
        given:
        database.getAdjustTrackingTablesOnStartup() >> false
        
        when:
        lockService.adjustRepository()
        
        then:
        0 * executor.execute(_ as AdjustChangeLogLockCollectionStatement)
    }
    
    def "should check if lock is held"() {
        given:
        def lockDocument = new Document("locked", true)
                .append("lockedBy", "testhost")
                .append("lockGranted", new Date())
        
        executor.queryForObject(_ as SelectChangeLogLockStatement, Document.class) >> lockDocument
        
        when:
        def result = lockService.isLocked()
        
        then:
        result
    }
    
    def "should replace lock"() {
        when:
        lockService.replaceLock(true)
        
        then:
        1 * executor.update(_ as ReplaceChangeLogLockStatement)
    }
    
    def "should query locks"() {
        given:
        def lockDocument = new Document("locked", true)
                .append("lockedBy", "testhost")
                .append("lockGranted", new Date())
        
        executor.queryForList(_ as FindAllStatement, Document.class) >> [lockDocument]
        
        when:
        def result = lockService.queryLocks()
        
        then:
        result.size() == 1
        result[0] instanceof DatabaseChangeLogLock
    }
    
    def "should handle database exceptions"() {
        given:
        executor.queryForObject(_ as SelectChangeLogLockStatement, Document.class) >> { throw new DatabaseException("Test exception") }
        
        when:
        lockService.isLocked()
        
        then:
        thrown(DatabaseException)
    }
    
    def "should handle MongoDB interrupts"() {
        given:
        def mongoException = new RuntimeException("Interrupted waiting for lock")
        def dbException = new DatabaseException(mongoException)
        executor.update(_ as ReplaceChangeLogLockStatement) >> { throw dbException }
        
        when:
        lockService.replaceLock(false)
        
        then:
        thrown(DatabaseException)
    }
}