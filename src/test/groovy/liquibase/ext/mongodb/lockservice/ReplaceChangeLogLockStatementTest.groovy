package liquibase.ext.mongodb.lockservice

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.UpdateResult
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class ReplaceChangeLogLockStatementTest extends Specification {
    
    def "should execute replace change log lock statement with upsert"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new ReplaceChangeLogLockStatement(collectionName, true)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        mongoCollection.countDocuments() >> 0L
        mongoCollection.findOneAndReplace(_ as org.bson.conversions.Bson, _ as Document, _) >> null
        
        when:
        def result = statement.update(database)
        
        then:
        result == 0
    }
    
    def "should execute replace change log lock statement with update"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new ReplaceChangeLogLockStatement(collectionName, true)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        def lockDocument = new Document("id", 1)
            .append("locked", true)
            .append("lockedBy", "testhost")
            .append("lockGranted", new Date())
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName) >> mongoCollection
        mongoCollection.countDocuments() >> 1L
        mongoCollection.findOneAndReplace(_ as org.bson.conversions.Bson, _ as Document, _) >> lockDocument
        
        when:
        def result = statement.update(database)
        
        then:
        result == 1
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new ReplaceChangeLogLockStatement("DATABASECHANGELOGLOCK", true)
        statement.getCommandName() == "updateLock"
    }
    
    def "should build correct JS command"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new ReplaceChangeLogLockStatement(collectionName, true)
        
        when:
        def js = statement.toJs()
        
        then:
        js == "db.DATABASECHANGELOGLOCK.updateLock(true);"
    }
    
    def "should get locked status"() {
        expect:
        def statement = new ReplaceChangeLogLockStatement("DATABASECHANGELOGLOCK", true)
        statement.isLocked()
        
        !new ReplaceChangeLogLockStatement("DATABASECHANGELOGLOCK", false).isLocked()
    }
}