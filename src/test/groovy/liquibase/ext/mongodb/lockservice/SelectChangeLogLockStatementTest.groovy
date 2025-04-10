package liquibase.ext.mongodb.lockservice

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class SelectChangeLogLockStatementTest extends Specification {
    
    def "should execute select change log lock statement"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new SelectChangeLogLockStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        def findIterable = Mock(FindIterable)
        def lockDocument = new Document("id", 1)
            .append("locked", true)
            .append("lockedBy", "testhost")
            .append("lockGranted", new Date())
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName, Document.class) >> mongoCollection
        mongoCollection.find(_ as org.bson.conversions.Bson) >> findIterable
        findIterable.first() >> lockDocument
        
        when:
        def result = statement.queryForObject(database, Document.class)
        
        then:
        result instanceof Document
        result.getInteger("id") == 1
        result.getBoolean("locked") == true
        result.getString("lockedBy") == "testhost"
    }
    
    def "should handle empty result"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new SelectChangeLogLockStatement(collectionName)
        
        def database = Mock(MongoLiquibaseDatabase)
        def mongoDatabase = Mock(MongoDatabase)
        def mongoCollection = Mock(MongoCollection)
        def findIterable = Mock(FindIterable)
        
        database.getMongoDatabase() >> mongoDatabase
        mongoDatabase.getCollection(collectionName, Document.class) >> mongoCollection
        mongoCollection.find(_ as org.bson.conversions.Bson) >> findIterable
        findIterable.first() >> null
        
        when:
        def result = statement.queryForObject(database, Document.class)
        
        then:
        result == null
    }
    
    def "should have correct command name"() {
        expect:
        def statement = new SelectChangeLogLockStatement("DATABASECHANGELOGLOCK")
        statement.getCommandName() == "findLock"
    }
    
    def "should build correct JS command"() {
        given:
        def collectionName = "DATABASECHANGELOGLOCK"
        def statement = new SelectChangeLogLockStatement(collectionName)
        
        when:
        def js = statement.toJs()
        
        then:
        js == "db.DATABASECHANGELOGLOCK.findLock();"
    }
}