package liquibase.ext.mongodb.configuration;

import liquibase.configuration.AutoloadedConfigurations;
import liquibase.configuration.ConfigurationDefinition;

import static java.lang.Boolean.TRUE;

public class MongoConfiguration implements AutoloadedConfigurations {


    public static final String LIQUIBASE_MONGO_NAMESPACE = "liquibase.mongodb";
    public static final ConfigurationDefinition<Boolean>  ADJUST_TRACKING_TABLES_ON_STARTUP;
    public static final ConfigurationDefinition<Boolean>  SUPPORTS_VALIDATOR;
    public static final ConfigurationDefinition<Boolean>  RETRY_WRITES;

    static {
        ConfigurationDefinition.Builder builder = new ConfigurationDefinition.Builder(LIQUIBASE_MONGO_NAMESPACE);


        ADJUST_TRACKING_TABLES_ON_STARTUP = builder.define("adjustTrackingTablesOnStartup", Boolean.class)
                                .setDescription("Enabling this property will validate History Change Log and Log Lock Collections " +
                "on Startup and adjust if are not up to date with current release." +
                "Worth keeping it disabled and re-enable when upgraded to a new Liquibase version.")
                .setDefaultValue(TRUE)
        .build();

        SUPPORTS_VALIDATOR = builder.define("supportsValidator", Boolean.class)
                .setDescription("Disabling this property will let create the Tracking Collections without validators." +
                        "This will permit usage on Mongo Versions not supporting Validators")
                .setDefaultValue(TRUE)
                .build();

        RETRY_WRITES = builder.define("retryWrites", Boolean.class)
                .setDescription("Setting this property to false will add retryWrites=false to connection URL." +
                        "This will permit usage on Mongo Versions not supporting retryWrites, like Amazon DocumentDB")
                .setDefaultValue(TRUE)
                .build();
    }
}
