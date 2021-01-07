package liquibase.nosql.parser.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.Scope;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.ChangeLogParseException;
import liquibase.logging.Logger;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.core.ParsedNode;
import liquibase.resource.ResourceAccessor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static liquibase.plugin.Plugin.PRIORITY_SPECIALIZED;

public class JsonNoSqlChangeLogParser implements ChangeLogParser {

    public static final String RAW_JSON_FIELD = "$rawJson";
    protected Logger log = Scope.getCurrentScope().getLog(getClass());

    protected ObjectMapper objectMapper = new JsonMapper();

    @Override
    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    /**
     * Checks if the file format is supported by an implementing ChangeLogParser and returns true if that is the case.
     *
     * @param changeLogFile    the location of the changelog file
     * @param resourceAccessor the resource accessor
     * @return true if the file format is supported, false if it is not.
     */
    @Override
    public boolean supports(String changeLogFile, ResourceAccessor resourceAccessor) {
        for (String extension : getSupportedFileExtensions()) {
            if (changeLogFile.toLowerCase().endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }

    protected String[] getSupportedFileExtensions() {
        return new String[]{"json"};
    }

    public Logger getLogger() {
        return log;
    }

    /**
     * Parses a Liquibase database changelog and returns the parsed form as an object.
     *
     * @param physicalChangeLogLocation the physical location of the changelog. The exakt file formats and locations
     *                                  where can load changelog files from depend on the implementations and capabilities of the implementing parsers.
     * @param changeLogParameters       parameters given by the end user that should be applied while parsing the changelog
     *                                  (i.e. replacement of ${placeholders} inside the changelogs with user-defined content)
     * @param resourceAccessor          a Java resource accessor
     * @return the parsed ChangeLog in object form
     * @throws ChangeLogParseException if an error occurs during parsing of the ChangeLog
     */
    @Override
    public DatabaseChangeLog parse(final String physicalChangeLogLocation, final ChangeLogParameters changeLogParameters, final ResourceAccessor resourceAccessor) throws ChangeLogParseException {
        try (InputStream changeLogStream = resourceAccessor.openStream(null, physicalChangeLogLocation)) {
            if (changeLogStream == null) {
                throw new ChangeLogParseException(physicalChangeLogLocation + " does not exist");
            }

            final JsonParser parser = objectMapper.createParser(changeLogStream);

            final TreeNode parsedJson = objectMapper.readTree(parser);

            if (isNull(parsedJson) || parsedJson.size() == 0) {
                throw new ChangeLogParseException("Empty file " + physicalChangeLogLocation);
            }

            final DatabaseChangeLog changeLog = new DatabaseChangeLog(physicalChangeLogLocation);

            final TreeNode rootList = parsedJson.get("databaseChangeLog");
            if (isNull(rootList)) {
                throw new ChangeLogParseException("Could not find databaseChangeLog node");
            }

            if (!rootList.isArray()) {
                throw new ChangeLogParseException("databaseChangeLog does not contain a list of entries. Each changeSet must begin ' - changeSet:'");
            }

            for (JsonNode obj : ((ArrayNode) rootList)) {
                if ((obj.isObject()) && obj.has("property")) {
                    final JsonNode property = obj.get("property");
                    final ContextExpression context = new ContextExpression(
                            ofNullable(property.get("context")).map(JsonNode::asText).orElse(null));
                    final Labels labels = new Labels(
                            ofNullable(property.get("labels")).map(JsonNode::asText).orElse(null));

                    final Boolean global = getGlobalParam(property);

                    if (property.has("name")) {
                        final String value = property.get("value").asText();
                        final String dbms = ofNullable(property.get("dbms")).map(JsonNode::asText).orElse(null);
                        changeLogParameters.set(property.get("name").asText(), value, context, labels, dbms, global, changeLog);
                    } else if (property.has("file")) {
                        loadChangeLogParametersFromFile(changeLogParameters, resourceAccessor, changeLog, property,
                                context, labels, global);
                    }
                }
            }


            replaceParameters(parsedJson, changeLogParameters, changeLog);

            changeLog.setChangeLogParameters(changeLogParameters);
            ParsedNode databaseChangeLogNode = new ParsedNode(null, "databaseChangeLog");

            final Map<String, Object> mapJson = objectMapper.convertValue(parsedJson, new TypeReference<Map<String, Object>>() {
            });

            databaseChangeLogNode.setValue(mapJson.get("databaseChangeLog"));
            changeLog.load(databaseChangeLogNode, resourceAccessor);

            return changeLog;
        } catch (ChangeLogParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ChangeLogParseException("Error parsing " + physicalChangeLogLocation, e);
        }
    }

    private void loadChangeLogParametersFromFile(ChangeLogParameters changeLogParameters, ResourceAccessor resourceAccessor, DatabaseChangeLog changeLog, JsonNode property, ContextExpression context, Labels labels, Boolean global) throws IOException {
        Properties props = new Properties();
        try (
                InputStream propertiesStream = resourceAccessor.openStream(null, property.get("file").asText())) {

            if (propertiesStream == null) {
                getLogger().info("Could not open properties file " + property.get("file"));
            } else {
                props.load(propertiesStream);
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    final String dbms = ofNullable(property.get("dbms")).map(JsonNode::asText).orElse(null);
                    changeLogParameters.set(entry.getKey().toString(), entry.getValue().toString(), context, labels, dbms, global, changeLog);
                }
            }
        }
    }

    /**
     * Extract the global parameter from the properties.
     *
     * @param property the map of props
     * @return the global param
     */
    private Boolean getGlobalParam(final JsonNode property) {
        final boolean global;
        final JsonNode globalObj = property.get("global");
        if (globalObj == null) {
            // default behaviour before liquibase 3.4
            global = true;
        } else {
            global = globalObj.asBoolean();
        }
        return global;
    }

    @SneakyThrows
    protected void replaceParameters(TreeNode obj, ChangeLogParameters changeLogParameters, DatabaseChangeLog changeLog) {
        if (obj.isObject()) {
            final ObjectNode objectNode = (ObjectNode) obj;
            final Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue().isValueNode()) {
                    objectNode.put(entry.getKey(),
                            changeLogParameters.expandExpressions(entry.getValue().asText(), changeLog));
                } else if (entry.getValue().isObject() && entry.getValue().has(RAW_JSON_FIELD)) {
                    final JsonNode jsonPayload = entry.getValue().get(RAW_JSON_FIELD);
                    objectNode.put(entry.getKey(),
                            changeLogParameters.expandExpressions(jsonPayload.toPrettyString(), changeLog));
                } else {
                    replaceParameters(entry.getValue(), changeLogParameters, changeLog);
                }
            }
        } else if (obj.isArray()) {
            final ArrayNode arrayNode = ((ArrayNode) obj);
            for (int i = 0; i < arrayNode.size(); i++) {
                final JsonNode child = arrayNode.get(i);
                if (child.isValueNode()) {
                    final JsonNode jsonNode = JsonNodeFactory.instance.textNode(
                            changeLogParameters.expandExpressions(child.toString(), changeLog));
                    arrayNode.set(i, jsonNode);
                } else {
                    replaceParameters(child, changeLogParameters, changeLog);
                }
            }
        }
    }
}
