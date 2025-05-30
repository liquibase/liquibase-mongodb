package liquibase.ext.mongodb.statement

import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import spock.lang.Specification

class DropAllCollectionsStatementTest extends Specification {
    
    def "should have correct command name"() {
        given:
        def statement = new DropAllCollectionsStatement()
        
        expect:
        statement.getCommandName() == "dropAll"
    }
    
    def "should extend AbstractMongoStatement"() {
        given:
        def statement = new DropAllCollectionsStatement()
        
        expect:
        statement instanceof AbstractMongoStatement
    }
}