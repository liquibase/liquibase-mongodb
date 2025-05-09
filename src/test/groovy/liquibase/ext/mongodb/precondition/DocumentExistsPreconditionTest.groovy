package liquibase.ext.mongodb.precondition

import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.changelog.visitor.ChangeExecListener
import liquibase.exception.PreconditionErrorException
import liquibase.exception.PreconditionFailedException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.CountDocumentsInCollectionStatement
import spock.lang.Specification

class DocumentExistsPreconditionTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def changeLog = Mock(DatabaseChangeLog)
    def changeSet = Mock(ChangeSet)
    def changeExecListener = Mock(ChangeExecListener)
    
    def "should have correct name"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        
        expect:
        precondition.getName() == "documentExists"
    }
    
    def "should return empty warnings"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        
        when:
        def warnings = precondition.warn(database)
        
        then:
        warnings.getMessages().isEmpty()
    }
    
    def "should return empty validation errors"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        
        when:
        def validationErrors = precondition.validate(database)
        
        then:
        validationErrors.getErrorMessages().isEmpty()
    }
    
    def "should pass when document exists"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        precondition.setCollectionName("users")
        precondition.setFilter('{"username": "johndoe"}')
        database.execute(_ as CountDocumentsInCollectionStatement) >> 1L
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        noExceptionThrown()
    }
    
    def "should fail when document does not exist"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        precondition.setCollectionName("users")
        precondition.setFilter('{"username": "nonexistent"}')
        database.execute(_ as CountDocumentsInCollectionStatement) >> 0L
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionFailedException)
    }
    
    def "should handle empty filter"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        precondition.setCollectionName("users")
        precondition.setFilter(null)
        database.execute(_ as CountDocumentsInCollectionStatement) >> 5L
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        noExceptionThrown()
    }
    
    def "should handle exceptions"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        precondition.setCollectionName("users")
        precondition.setFilter('{"username": "error"}')
        database.execute(_ as CountDocumentsInCollectionStatement) >> { throw new Exception("Test exception") }
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionErrorException)
    }
    
    def "should return correct serialized object namespace"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        
        expect:
        precondition.getSerializedObjectNamespace() == "http://www.liquibase.org/xml/ns/dbchangelog"
    }
    
    def "should set and get collection name"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        def collectionName = "testCollection"
        
        when:
        precondition.setCollectionName(collectionName)
        
        then:
        precondition.getCollectionName() == collectionName
    }
    
    def "should set and get filter"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        def filter = '{"age": {"$gt": 18}}'
        
        when:
        precondition.setFilter(filter)
        
        then:
        precondition.getFilter() == filter
    }
}