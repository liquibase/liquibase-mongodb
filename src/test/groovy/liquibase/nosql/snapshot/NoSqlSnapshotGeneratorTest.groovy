package liquibase.nosql.snapshot

import liquibase.database.Database
import liquibase.exception.DatabaseException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.snapshot.DatabaseSnapshot
import liquibase.snapshot.InvalidExampleException
import liquibase.snapshot.SnapshotGeneratorChain
import liquibase.structure.DatabaseObject
import liquibase.structure.core.Table
import spock.lang.Specification

class NoSqlSnapshotGeneratorTest extends Specification {
    
    NoSqlSnapshotGenerator generator
    
    def setup() {
        generator = new NoSqlSnapshotGenerator()
    }
    
    def "should return specialized priority for MongoDB database"() {
        given:
        def database = Mock(MongoLiquibaseDatabase)
        def objectType = Table.class
        
        when:
        def result = generator.getPriority(objectType, database)
        
        then:
        result == 10 // PRIORITY_SPECIALIZED is 10
    }
    
    def "should return no priority for non-MongoDB database"() {
        given:
        def database = Mock(Database)
        def objectType = Table.class
        
        when:
        def result = generator.getPriority(objectType, database)
        
        then:
        result == -1 // PRIORITY_NONE is -1
    }
    
    def "should throw DatabaseException for snapshot operation"() {
        given:
        def example = Mock(DatabaseObject)
        def snapshot = Mock(DatabaseSnapshot)
        def chain = Mock(SnapshotGeneratorChain)
        
        when:
        generator.snapshot(example, snapshot, chain)
        
        then:
        thrown(DatabaseException)
    }
    
    def "should return empty array for addsTo"() {
        when:
        def result = generator.addsTo()
        
        then:
        result != null
        result.length == 0
    }
    
    def "should return empty array for replaces"() {
        when:
        def result = generator.replaces()
        
        then:
        result != null
        result.length == 0
    }
    
    def "should verify exception message contains correct command text"() {
        given:
        def example = Mock(DatabaseObject)
        def snapshot = Mock(DatabaseSnapshot)
        def chain = Mock(SnapshotGeneratorChain)
        
        when:
        generator.snapshot(example, snapshot, chain)
        
        then:
        def exception = thrown(DatabaseException)
        exception.message.contains("db-doc, diff*, generate-changelog, and snapshot*")
        exception.message.contains("Liquibase MongoDB Extension does not support")
    }
}