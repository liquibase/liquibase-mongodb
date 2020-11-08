package liquibase.ext.mongodb.configuration;

import liquibase.configuration.ConfigurationProperty;
import liquibase.configuration.ConfigurationValueProvider;
import liquibase.configuration.LiquibaseConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static liquibase.ext.mongodb.configuration.MongoConfiguration.ADJUST_TRACKING_TABLES_ON_STARTUP;
import static liquibase.ext.mongodb.configuration.MongoConfiguration.SUPPORTS_VALIDATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MongoConfigurationTest {

    protected MongoConfiguration configuration;

    @Mock
    protected ConfigurationValueProvider providerMock;

    @BeforeEach
    void setUp() {
        LiquibaseConfiguration.getInstance().reset();
        configuration = LiquibaseConfiguration.getInstance()
                .getConfiguration(MongoConfiguration.class);
    }

    @AfterEach
    void tearDown() {
        LiquibaseConfiguration.getInstance().reset();
    }

    @Test
    void testGetAdjustTrackingTablesOnStartup() {
        assertThat(configuration.getAdjustTrackingTablesOnStartup()).isTrue();

        assertThat(configuration.getProperty(ADJUST_TRACKING_TABLES_ON_STARTUP))
                .returns(TRUE, ConfigurationProperty::getValue)
                .returns(FALSE, ConfigurationProperty::getWasOverridden);
    }

    @Test
    void testSetAdjustTrackingTablesOnStartup() {
        configuration.setAdjustTrackingTablesOnStartup(FALSE);
        assertThat(configuration.getAdjustTrackingTablesOnStartup()).isFalse();

        assertThat(configuration.getProperty(ADJUST_TRACKING_TABLES_ON_STARTUP))
                .returns(FALSE, ConfigurationProperty::getValue)
                .returns(TRUE, ConfigurationProperty::getWasOverridden);
    }

    @Test
    void testGetSupportsValidator() {
        assertThat(configuration.getSupportsValidator()).isTrue();

        assertThat(configuration.getProperty(SUPPORTS_VALIDATOR))
                .returns(TRUE, ConfigurationProperty::getValue)
                .returns(FALSE, ConfigurationProperty::getWasOverridden);
    }

    @Test
    void testSetSupportsValidator() {
        configuration.setSupportsValidator(FALSE);
        assertThat(configuration.getSupportsValidator()).isFalse();

        assertThat(configuration.getProperty(SUPPORTS_VALIDATOR))
                .returns(FALSE, ConfigurationProperty::getValue)
                .returns(TRUE, ConfigurationProperty::getWasOverridden);
    }

    @Test
    void testLiquibaseConfigurationInit() {

        when(providerMock.getValue(MongoConfiguration.LIQUIBASE_MONGO_NAMESPACE, ADJUST_TRACKING_TABLES_ON_STARTUP)).thenReturn("false");
        when(providerMock.getValue(MongoConfiguration.LIQUIBASE_MONGO_NAMESPACE, SUPPORTS_VALIDATOR)).thenReturn("false");

        LiquibaseConfiguration.getInstance().init(providerMock);
        configuration = LiquibaseConfiguration.getInstance().getConfiguration(MongoConfiguration.class);

        assertThat(configuration.getAdjustTrackingTablesOnStartup()).isFalse();
        assertThat(configuration.getSupportsValidator()).isFalse();

        assertThat(configuration.getProperty(ADJUST_TRACKING_TABLES_ON_STARTUP))
                .returns(FALSE, ConfigurationProperty::getValue)
                .returns(TRUE, ConfigurationProperty::getWasOverridden);

        assertThat(configuration.getProperty(SUPPORTS_VALIDATOR))
                .returns(FALSE, ConfigurationProperty::getValue)
                .returns(TRUE, ConfigurationProperty::getWasOverridden);
    }
}