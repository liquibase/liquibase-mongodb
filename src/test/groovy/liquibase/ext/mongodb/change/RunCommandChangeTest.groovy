package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.RunCommandStatement
import spock.lang.Specification

class RunCommandChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate RunCommandStatement"() {
        given:
        def change = new RunCommandChange()
        change.setCommand('{"ping": 1}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof RunCommandStatement
        def statement = (RunCommandStatement) statements[0]
        statement.getCommand().getInteger("ping") == 1
    }
    
    def "should generate RunCommandStatement with complex command"() {
        given:
        def change = new RunCommandChange()
        change.setCommand('{"aggregate": "myCollection", "pipeline": [{"$match": {"status": "active"}}, {"$group": {"_id": "$category", "count": {"$sum": 1}}}], "cursor": {}}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof RunCommandStatement
        def statement = (RunCommandStatement) statements[0]
        statement.getCommand().getString("aggregate") == "myCollection"
        statement.getCommand().get("pipeline") instanceof List
        statement.getCommand().getList("pipeline", Object.class).size() == 2
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new RunCommandChange()
        change.setCommand('{"ping": 1}')
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Command run"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new RunCommandChange()
        change1.setCommand('{"ping": 1}')
        
        def change2 = new RunCommandChange()
        change2.setCommand('{"ping": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different commands"() {
        given:
        def change1 = new RunCommandChange()
        change1.setCommand('{"ping": 1}')
        
        def change2 = new RunCommandChange()
        change2.setCommand('{"dbStats": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new RunCommandChange() instanceof AbstractMongoChange
    }
}