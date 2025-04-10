package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class BsonUtilsTest extends Specification {
    
    def "should parse JSON to Document"() {
        given:
        def json = '{"name": "test", "value": 123}'
        
        when:
        def document = BsonUtils.orEmptyDocument(json)
        
        then:
        document instanceof Document
        document.getString("name") == "test"
        document.getInteger("value") == 123
    }
    
    def "should return empty Document for null or empty JSON"() {
        expect:
        BsonUtils.orEmptyDocument(null) instanceof Document
        BsonUtils.orEmptyDocument(null).isEmpty()
        BsonUtils.orEmptyDocument("") instanceof Document
        BsonUtils.orEmptyDocument("").isEmpty()
        BsonUtils.orEmptyDocument("  ") instanceof Document
        BsonUtils.orEmptyDocument("  ").isEmpty()
    }
    
    def "should parse JSON array to List of Documents"() {
        given:
        def jsonArray = '[{"name": "doc1"}, {"name": "doc2"}]'
        
        when:
        def documents = BsonUtils.orEmptyList(jsonArray)
        
        then:
        documents instanceof List
        documents.size() == 2
        documents[0].getString("name") == "doc1"
        documents[1].getString("name") == "doc2"
    }
    
    def "should return empty List for null or empty JSON array"() {
        expect:
        BsonUtils.orEmptyList(null) instanceof List
        BsonUtils.orEmptyList(null).isEmpty()
        BsonUtils.orEmptyList("") instanceof List
        BsonUtils.orEmptyList("").isEmpty()
        BsonUtils.orEmptyList("  ") instanceof List
        BsonUtils.orEmptyList("  ").isEmpty()
    }
    
    def "should convert Document to JSON string"() {
        given:
        def document = new Document("name", "test").append("value", 123)
        
        when:
        def json = BsonUtils.toJson(document)
        
        then:
        json instanceof String
        // O formato pode variar, então vamos verificar apenas se contém os valores
        json.contains("name")
        json.contains("test")
        json.contains("value")
        json.contains("123")
    }
    
    def "should return null when converting null document to JSON"() {
        expect:
        BsonUtils.toJson(null) == null
    }
    
    def "should create command document with options"() {
        given:
        def commandName = "find"
        def commandValue = "collection"
        def options = new Document("limit", 10).append("sort", new Document("name", 1))
        
        when:
        def command = BsonUtils.toCommand(commandName, commandValue, options)
        
        then:
        command instanceof Document
        command.getString(commandName) == commandValue
        command.getInteger("limit") == 10
        command.get("sort") instanceof Document
        command.get("sort").getInteger("name") == 1
    }
    
    def "should create command document without options"() {
        given:
        def commandName = "find"
        def commandValue = "collection"
        
        when:
        def command = BsonUtils.toCommand(commandName, commandValue, null)
        
        then:
        command instanceof Document
        command.getString(commandName) == commandValue
        command.size() == 1
    }
}