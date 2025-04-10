package liquibase.ext.mongodb.change

import liquibase.ChecksumVersion
import liquibase.database.Database
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.ext.mongodb.statement.AdminCommandStatement
import spock.lang.Specification

class AdminCommandChangeTest extends Specification {
    
    def database = Mock(MongoLiquibaseDatabase)
    
    def "should generate AdminCommandStatement"() {
        given:
        def change = new AdminCommandChange()
        change.setCommand('{"buildInfo": 1}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof AdminCommandStatement
        def statement = (AdminCommandStatement) statements[0]
        statement.getCommand().getInteger("buildInfo") == 1
    }
    
    def "should generate AdminCommandStatement with complex command"() {
        given:
        def change = new AdminCommandChange()
        change.setCommand('{"getCmdLineOpts": 1}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof AdminCommandStatement
        def statement = (AdminCommandStatement) statements[0]
        statement.getCommand().getInteger("getCmdLineOpts") == 1
    }
    
    def "should provide confirmation message"() {
        given:
        def change = new AdminCommandChange()
        change.setCommand('{"listDatabases": 1}')
        
        when:
        def message = change.getConfirmationMessage()
        
        then:
        message == "Admin Command run"
    }
    
    def "should generate consistent checksum"() {
        given:
        def change1 = new AdminCommandChange()
        change1.setCommand('{"listDatabases": 1}')
        
        def change2 = new AdminCommandChange()
        change2.setCommand('{"listDatabases": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 == checksum2
    }
    
    def "should generate different checksums for different commands"() {
        given:
        def change1 = new AdminCommandChange()
        change1.setCommand('{"listDatabases": 1}')
        
        def change2 = new AdminCommandChange()
        change2.setCommand('{"buildInfo": 1}')
        
        when:
        def checksum1 = change1.generateCheckSum()
        def checksum2 = change2.generateCheckSum()
        
        then:
        checksum1 != checksum2
    }
    
    def "should extend AbstractMongoChange"() {
        expect:
        new AdminCommandChange() instanceof AbstractMongoChange
    }
    
    def "should have AdminCommandStatement as commandName"() {
        given:
        def change = new AdminCommandChange()
        change.setCommand('{"listDatabases": 1}')
        
        when:
        def statements = change.generateStatements(database)
        
        then:
        statements.length == 1
        statements[0] instanceof AdminCommandStatement
        def statement = (AdminCommandStatement) statements[0]
        statement.getCommandName() == "adminCommand"
    }
}