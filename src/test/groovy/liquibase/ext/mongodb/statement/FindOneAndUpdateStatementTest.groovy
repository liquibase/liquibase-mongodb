package liquibase.ext.mongodb.statement

import liquibase.ext.mongodb.database.MongoConnection
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import org.bson.Document
import spock.lang.Specification

class FindOneAndUpdateStatementTest extends Specification {
    
    def "should create FindOneAndUpdateStatement with filter, update and sort"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("name", "test")
        def update = new Document("\$set", new Document("value", 123))
        def sort = new Document("_id", 1)
        
        when:
        def statement = new FindOneAndUpdateStatement(collectionName, filter, update, sort)
        
        then:
        statement.getCommand().getString("findAndModify") == collectionName
        statement.getCommand().get("query") instanceof Document
        statement.getCommand().get("query").getString("name") == "test"
        statement.getCommand().get("update") instanceof Document
        statement.getCommand().get("update").get("\$set") instanceof Document
        statement.getCommand().get("update").get("\$set").getInteger("value") == 123
        statement.getCommand().get("sort") instanceof Document
        statement.getCommand().get("sort").getInteger("_id") == 1
    }
    
    def "should create FindOneAndUpdateStatement with filter and update without sort"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("name", "test")
        def update = new Document("\$set", new Document("value", 123))
        
        when:
        def statement = new FindOneAndUpdateStatement(collectionName, filter, update, null)
        
        then:
        statement.getCommand().getString("findAndModify") == collectionName
        statement.getCommand().get("query") instanceof Document
        statement.getCommand().get("query").getString("name") == "test"
        statement.getCommand().get("update") instanceof Document
        statement.getCommand().get("update").get("\$set") instanceof Document
        statement.getCommand().get("update").get("\$set").getInteger("value") == 123
        !statement.getCommand().containsKey("sort")
    }
    
    def "should create FindOneAndUpdateStatement with options document"() {
        given:
        def collectionName = "testCollection"
        def options = new Document("query", new Document("name", "test"))
            .append("update", new Document("\$set", new Document("value", 123)))
            .append("sort", new Document("_id", 1))
            .append("new", true)
        
        when:
        def statement = new FindOneAndUpdateStatement(collectionName, options)
        
        then:
        statement.getCommand().getString("findAndModify") == collectionName
        statement.getCommand().get("query") instanceof Document
        statement.getCommand().get("query").getString("name") == "test"
        statement.getCommand().get("update") instanceof Document
        statement.getCommand().get("update").get("\$set") instanceof Document
        statement.getCommand().get("update").get("\$set").getInteger("value") == 123
        statement.getCommand().get("sort") instanceof Document
        statement.getCommand().get("sort").getInteger("_id") == 1
        statement.getCommand().getBoolean("new") == true
    }
    
    def "should have correct command name"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("name", "test")
        def update = new Document("\$set", new Document("value", 123))
        
        when:
        def statement = new FindOneAndUpdateStatement(collectionName, filter, update, null)
        
        then:
        statement.getRunCommandName() == "findAndModify"
    }
    
    def "should extend AbstractRunCommandStatement"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("name", "test")
        def update = new Document("\$set", new Document("value", 123))
        
        when:
        def statement = new FindOneAndUpdateStatement(collectionName, filter, update, null)
        
        then:
        statement instanceof AbstractRunCommandStatement
    }
}