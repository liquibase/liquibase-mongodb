package liquibase.ext.mongodb.statement

import org.bson.Document
import spock.lang.Specification

class DeleteManyStatementTest extends Specification {
    
    def "should create statement with filter"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document("status", "inactive")
        
        when:
        def statement = new DeleteManyStatement(collectionName, filter)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getCommandName() == "deleteMany"
        statement.getFilter() instanceof Document
        statement.getFilter().getString("status") == "inactive"
    }
    
    def "should generate JavaScript representation"() {
        given:
        def collectionName = "testCollection"
        def ltOperator = '$lt'
        def filter = new Document("age", new Document(ltOperator, 18))
        
        when:
        def statement = new DeleteManyStatement(collectionName, filter)
        def js = statement.toJs()
        
        then:
        js.startsWith("db.testCollection.deleteMany(")
        js.contains("Document{{age=Document{{")
        js.endsWith(");")
    }
    
    def "should extend AbstractCollectionStatement"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document()
        
        when:
        def statement = new DeleteManyStatement(collectionName, filter)
        
        then:
        statement instanceof AbstractCollectionStatement
    }
    
    def "should handle null filter"() {
        given:
        def collectionName = "testCollection"
        
        when:
        def statement = new DeleteManyStatement(collectionName, null)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getFilter() == null
        statement.toJs().contains("db.testCollection.deleteMany(null);")
    }
    
    def "should support complex filter expressions"() {
        given:
        def collectionName = "testCollection"
        def orOperator = '$or'
        def ltOperator = '$lt'
        def filter = new Document(orOperator, [
                new Document("status", "inactive"),
                new Document("lastLogin", new Document(ltOperator, "2020-01-01"))
        ])
        
        when:
        def statement = new DeleteManyStatement(collectionName, filter)
        
        then:
        statement.getCollectionName() == collectionName
        statement.getFilter() instanceof Document
        statement.getFilter().get(orOperator) instanceof List
        statement.getFilter().get(orOperator).size() == 2
    }
    
    def "should implement NoSqlExecuteStatement and NoSqlUpdateStatement"() {
        given:
        def collectionName = "testCollection"
        def filter = new Document()
        
        when:
        def statement = new DeleteManyStatement(collectionName, filter)
        
        then:
        statement instanceof liquibase.nosql.statement.NoSqlExecuteStatement
        statement instanceof liquibase.nosql.statement.NoSqlUpdateStatement
    }
}