package liquibase.nosql.parser.json

import liquibase.ContextExpression
import liquibase.LabelExpression
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.DatabaseChangeLog
import liquibase.exception.ChangeLogParseException
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.ResourceAccessor
import spock.lang.Specification
import spock.lang.Unroll

class JsonNoSqlChangeLogParserTest extends Specification {
    
    def parser = new JsonNoSqlChangeLogParser()
    def resourceAccessor = Mock(ResourceAccessor)
    def changeLogParameters = new ChangeLogParameters()
    def database = Mock(MongoLiquibaseDatabase)
    
    def setup() {
        database.getDatabaseProductName() >> "MongoDB"
    }
    
    def "supports method should return true for json files"() {
        expect:
        parser.supports("changelog.json", resourceAccessor)
        parser.supports("path/to/changelog.json", resourceAccessor)
        !parser.supports("changelog.xml", resourceAccessor)
        !parser.supports("changelog.yaml", resourceAccessor)
    }
    
    def "should handle not found file"() {
        given:
        resourceAccessor.openStream(null, "non-existent.json") >> null
        
        when:
        parser.parse("non-existent.json", changeLogParameters, resourceAccessor)
        
        then:
        thrown(ChangeLogParseException)
    }
    
    def "should handle missing databaseChangeLog node"() {
        given:
        def json = '{"something": "else"}'
        def inputStream = new ByteArrayInputStream(json.getBytes())
        resourceAccessor.openStream(null, "missing-root.json") >> inputStream
        
        when:
        parser.parse("missing-root.json", changeLogParameters, resourceAccessor)
        
        then:
        thrown(ChangeLogParseException)
    }
    
    def "should handle non-array databaseChangeLog"() {
        given:
        def json = '{"databaseChangeLog": "not-an-array"}'
        def inputStream = new ByteArrayInputStream(json.getBytes())
        resourceAccessor.openStream(null, "non-array.json") >> inputStream
        
        when:
        parser.parse("non-array.json", changeLogParameters, resourceAccessor)
        
        then:
        thrown(ChangeLogParseException)
    }
    
    def "should handle empty databaseChangeLog"() {
        given:
        def json = '{"databaseChangeLog": []}'
        def inputStream = new ByteArrayInputStream(json.getBytes())
        resourceAccessor.openStream(null, "empty.json") >> inputStream
        
        when:
        def changeLog = parser.parse("empty.json", changeLogParameters, resourceAccessor)
        
        then:
        changeLog != null
        changeLog.getChangeSets().size() == 0
    }
    
    def "should throw exception on malformed JSON"() {
        given:
        def json = '{"databaseChangeLog": [{'
        def inputStream = new ByteArrayInputStream(json.getBytes())
        resourceAccessor.openStream(null, "malformed.json") >> inputStream
        
        when:
        parser.parse("malformed.json", changeLogParameters, resourceAccessor)
        
        then:
        thrown(ChangeLogParseException)
    }
    
    def "should prioritize with specialized priority"() {
        expect:
        parser.getPriority() == 10  // PRIORITY_SPECIALIZED value
    }
    
    def "should have correct supported file extensions"() {
        expect:
        parser.getSupportedFileExtensions() == ["json"] as String[]
    }
}