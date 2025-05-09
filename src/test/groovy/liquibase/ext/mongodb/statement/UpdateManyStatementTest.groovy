package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class UpdateManyStatementTest extends Specification {
    
    def "should create statement with filter and document"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("status", "pending")
        def setOperator = '$set'
        def update = new Document(setOperator, new Document("status", "complete"))
        
        when:
        def statement = new UpdateManyStatement(collectionName, filter, update)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getCommandName() == "updateMany"
        statement.getFilter() instanceof Document
        statement.getFilter().getString("status") == "pending"
        statement.getDocument() instanceof Document
        statement.getDocument().get(setOperator) instanceof Document
        statement.getDocument().get(setOperator).getString("status") == "complete"
    }
    
    def "should generate JavaScript representation"() {
        given:
        def collectionName = "testCollection"
        def ltOperator = '$lt'
        def setOperator = '$set'
        def currentDateOperator = '$currentDate'
        def filter = new Document("age", new Document(ltOperator, 18))
        def update = new Document(setOperator, new Document("status", "minor"))
                .append(currentDateOperator, new Document("lastModified", true))
        
        when:
        def statement = new UpdateManyStatement(collectionName, filter, update)
        def js = statement.toJs()
        
        then:
        js.startsWith("db.testCollection.updateMany(")
        js.contains("Document{{age=Document{{")
        js.contains("Document{{" + setOperator + "=Document{{status=minor}}")
        js.contains(currentDateOperator + "=Document{{lastModified=true}}")
        js.endsWith(");")
    }
    
    def "should extend AbstractCollectionStatement"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document()
        def update = new Document()
        
        when:
        def statement = new UpdateManyStatement(collectionName, filter, update)
        
        then:
        statement instanceof AbstractCollectionStatement
    }
    
    def "should handle null filter and update"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new UpdateManyStatement(collectionName, null, null)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getFilter() == null
        statement.getDocument() == null
        statement.toJs().contains("db.testCollection.updateMany(null, null);")
    }
    
    def "should support complex update operations"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("status", "active")
        def incOperator = '$inc'
        def pushOperator = '$push'
        def update = new Document(incOperator, new Document("count", 1))
                .append(pushOperator, new Document("logs", "Updated via MongoDB"))
        
        when:
        def statement = new UpdateManyStatement(collectionName, filter, update)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getFilter() instanceof Document
        statement.getFilter().getString("status") == "active"
        statement.getDocument() instanceof Document
        statement.getDocument().get(incOperator) instanceof Document
        statement.getDocument().get(incOperator).getInteger("count") == 1
        statement.getDocument().get(pushOperator) instanceof Document
        statement.getDocument().get(pushOperator).getString("logs") == "Updated via MongoDB"
    }
}