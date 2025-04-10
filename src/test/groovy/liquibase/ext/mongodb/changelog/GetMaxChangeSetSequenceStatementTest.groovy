package liquibase.ext.mongodb.changelog

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Sorts
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class GetMaxChangeSetSequenceStatementTest extends Specification {
    
    def "should execute get max change set sequence statement with existing records"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new GetMaxChangeSetSequenceStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        def findIterable = Mock(FindIterable)
        def document = new Document("orderExecuted", 5)
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        mongoCollection.find() >> findIterable
        findIterable.sort(_ as Document) >> findIterable
        findIterable.limit(1) >> findIterable
        findIterable.first() >> document
        
        when:
        def result = statement.queryForLong(database)
        
        then:
        result == 6L // Should return max + 1
    }
    
    def "should handle empty collection"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new GetMaxChangeSetSequenceStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        def findIterable = Mock(FindIterable)
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        mongoCollection.find() >> findIterable
        findIterable.sort(_ as Document) >> findIterable
        findIterable.limit(1) >> findIterable
        findIterable.first() >> null
        
        when:
        def result = statement.queryForLong(database)
        
        then:
        result == 1L // Should return 1 for empty collection
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new GetMaxChangeSetSequenceStatement("DATABASECHANGELOG")
        statement.getCommandName() == "getMaxChangeSetSequence"
    }
    
    def "should build correct command document"() {
        given:
        def collectionName = "DATABASECHANGELOG"
        def statement = new GetMaxChangeSetSequenceStatement(collectionName)
        
        when:
        def command = statement.getCommand()
        
        then:
        command instanceof Document
        command.getString("getMaxChangeSetSequence") == collectionName
    }
}