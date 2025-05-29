package liquibase.ext.mongodb.precondition

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.changelog.visitor.ChangeExecListener
import liquibase.exception.PreconditionErrorException
import liquibase.exception.PreconditionFailedException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import org.bson.conversions.Bson
import spock.lang.Specification

class DocumentExistsPreconditionTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def changeLog = Mock(DatabaseChangeLog)
    def changeSet = Mock(ChangeSet)
    def changeExecListener = Mock(ChangeExecListener)
    def mongoDatabase = Mock(MongoDatabase)
    def mongoCollection = Mock(MongoCollection)
    
    def setup() {
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(_) >> mongoCollection
    }
    
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
        mongoCollection.countDocuments(_ as Bson) >> 1L
        
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
        mongoCollection.countDocuments(_ as Bson) >> 0L
        
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
        mongoCollection.countDocuments(_ as Bson) >> 5L
        
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
        mongoCollection.countDocuments(_ as Bson) >> { throw new Exception("Test exception") }
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionErrorException)
    }
    
    def "should return correct serialized object namespace"() {
        given:
        def precondition = new DocumentExistsPrecondition()
        
        expect:
        precondition.getSerializedObjectNamespace() == "http://www.liquibase.org/xml/ns/dbchangelog-ext"
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