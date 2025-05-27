package liquibase.ext.mongodb.precondition

import com.mongodb.client.MongoDatabase
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.changelog.visitor.ChangeExecListener
import liquibase.exception.PreconditionErrorException
import liquibase.exception.PreconditionFailedException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class CollectionExistsPreconditionTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    def changeLog = Mock(DatabaseChangeLog)
    def changeSet = Mock(ChangeSet)
    def changeExecListener = Mock(ChangeExecListener)
    def mongoDatabase = Mock(MongoDatabase)
    
    def setup() {
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.runCommand(_ as Document) >> { Document command ->
            if (command.get("listCollections") == 1) {
                def filter = command.get("filter", Document.class)
                def collectionName = filter?.getString("name")
                
                if (collectionName == "existingCollection") {
                    return new Document("cursor", new Document("firstBatch", [new Document("name", "existingCollection")]))
                        .append("ok", 1)
                } else if (collectionName == "nonExistingCollection") {
                    return new Document("cursor", new Document("firstBatch", []))
                        .append("ok", 1)
                } else if (collectionName == "errorCollection") {
                    throw new Exception("Test exception")
                }
                return new Document("cursor", new Document("firstBatch", []))
                    .append("ok", 1)
            }
            return new Document("ok", 1)
        }
    }
    
    def "should have correct name"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        expect:
        precondition.getName() == "collectionExists"
    }
    
    def "should return empty warnings"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        when:
        def warnings = precondition.warn(database)
        
        then:
        warnings.getMessages().isEmpty()
    }
    
    def "should return empty validation errors"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        when:
        def validationErrors = precondition.validate(database)
        
        then:
        validationErrors.getErrorMessages().isEmpty()
    }
    
    def "should pass when collection exists"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        precondition.setCollectionName("existingCollection")
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        noExceptionThrown()
    }
    
    def "should fail when collection does not exist"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        precondition.setCollectionName("nonExistingCollection")
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionFailedException)
    }
    
    def "should handle exceptions"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        precondition.setCollectionName("errorCollection")
        
        when:
        precondition.check(database, changeLog, changeSet, changeExecListener)
        
        then:
        thrown(PreconditionErrorException)
    }
    
    def "should return correct serialized object namespace"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        
        expect:
        precondition.getSerializedObjectNamespace() == precondition.GENERIC_CHANGELOG_EXTENSION_NAMESPACE
    }
    
    def "should set and get collection name"() {
        given:
        def precondition = new CollectionExistsPrecondition()
        def collectionName = "testCollection"
        
        when:
        precondition.setCollectionName(collectionName)
        
        then:
        precondition.getCollectionName() == collectionName
    }
}