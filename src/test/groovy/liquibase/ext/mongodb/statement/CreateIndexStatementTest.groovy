package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class CreateIndexStatementTest extends Specification {
    
    def "should create statement with string keys and options"() {
        given:
        def collectionName = "testCollection"
        def keys = '{"name": 1, "email": 1}'
        def options = '{"unique": true, "background": true}'
        
        when:
        def statement = new CreateIndexStatement(collectionName, keys, options)
        
        then:
        statement.getCommand() instanceof Document
        statement.getCommand().getString("createIndexes") == collectionName
        def indexes = statement.getCommand().getList("indexes", Document.class)
        indexes.size() == 1
        indexes[0].get("key") instanceof Document
        indexes[0].get("key").getInteger("name") == 1
        indexes[0].get("key").getInteger("email") == 1
        indexes[0].getBoolean("unique")
        indexes[0].getBoolean("background")
        statement.getRunCommandName() == "createIndexes"
    }
    
    def "should create statement with Document keys and options"() {
        given:
        def collectionName = "testCollection"
        def keys = new Document("name", 1).append("email", 1)
        def options = new Document("unique", true).append("background", true)
        
        when:
        def statement = new CreateIndexStatement(collectionName, keys, options)
        
        then:
        statement.getCommand() instanceof Document
        statement.getCommand().getString("createIndexes") == collectionName
        def indexes = statement.getCommand().getList("indexes", Document.class)
        indexes.size() == 1
        indexes[0].get("key") instanceof Document
        indexes[0].get("key").getInteger("name") == 1
        indexes[0].get("key").getInteger("email") == 1
        indexes[0].getBoolean("unique")
        indexes[0].getBoolean("background")
    }
    
    def "should handle empty keys and options"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new CreateIndexStatement(collectionName, (String)null, null)
        
        then:
        statement.getCommand() instanceof Document
        statement.getCommand().getString("createIndexes") == collectionName
        def indexes = statement.getCommand().getList("indexes", Document.class)
        indexes.size() == 1
        indexes[0].get("key") instanceof Document
        indexes[0].get("key").isEmpty()
    }
    
    def "should extend AbstractRunCommandStatement"() {
        given:
        def collectionName = "testCollection"
        def keys = new Document("name", 1)
        
        when:
        def statement = new CreateIndexStatement(collectionName, keys, null)
        
        then:
        statement instanceof AbstractRunCommandStatement
    }
    
    def "should handle complex index keys"() {
        given:
        def collectionName = "testCollection"
        def keys = '{"location": "2dsphere", "timestamp": -1}'
        def options = '{"name": "location_timestamp_idx", "sparse": true}'
        
        when:
        def statement = new CreateIndexStatement(collectionName, keys, options)
        
        then:
        statement.getCommand() instanceof Document
        statement.getCommand().getString("createIndexes") == collectionName
        def indexes = statement.getCommand().getList("indexes", Document.class)
        indexes.size() == 1
        indexes[0].get("key") instanceof Document
        indexes[0].get("key").getString("location") == "2dsphere"
        indexes[0].get("key").getInteger("timestamp") == -1
        indexes[0].getString("name") == "location_timestamp_idx"
        indexes[0].getBoolean("sparse")
    }
}