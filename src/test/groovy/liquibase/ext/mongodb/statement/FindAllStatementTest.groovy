package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class FindAllStatementTest extends Specification {
    
    def "should create statement with default parameters"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new FindAllStatement(collectionName)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getCommandName() == "find"
        statement.getFilter() instanceof Document
        statement.getFilter().isEmpty()
        statement.getSort() instanceof Document
        statement.getSort().isEmpty()
    }
    
    def "should create statement with filter and sort"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("status", "active")
        def sort = new Document("createdAt", -1)
        
        when:
        def statement = new FindAllStatement(collectionName, filter, sort)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getCommandName() == "find"
        statement.getFilter() instanceof Document
        statement.getFilter().getString("status") == "active"
        statement.getSort() instanceof Document
        statement.getSort().getInteger("createdAt") == -1
    }
    
    def "should generate JavaScript representation"() {
        given:
        def collectionName = "testCollection"
        def gtOperator = '$gt'
        def filter = new Document("age", new Document(gtOperator, 18))
        def sort = new Document("name", 1)
        
        when:
        def statement = new FindAllStatement(collectionName, filter, sort)
        def js = statement.toJs()
        
        then:
        js.startsWith("db.testCollection.find(")
        js.contains("Document{{age=Document{{")
        js.contains("Document{{name=1}}")
        js.endsWith(");")
    }
    
    def "should extend AbstractCollectionStatement"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new FindAllStatement(collectionName)
        
        then:
        statement instanceof AbstractCollectionStatement
    }
    
    def "should generate JavaScript representation with empty parameters"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new FindAllStatement(collectionName)
        def js = statement.toJs()
        
        then:
        js.contains("db.testCollection.find(")
        js.contains("Document{{}}")
        js.endsWith(");")
    }
}