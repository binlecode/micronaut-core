package org.particleframework.configuration.jdbc.tomcat

import org.apache.tomcat.jdbc.pool.DataSource
import org.particleframework.context.ApplicationContext
import org.particleframework.context.DefaultApplicationContext
import org.particleframework.context.env.MapPropertySource
import org.particleframework.inject.qualifiers.Qualifiers
import spock.lang.Specification

import java.sql.ResultSet

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(DataSource)
        !applicationContext.containsBean(DatasourceConfiguration)

        cleanup:
        applicationContext.close()
    }

    void "test blank configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'datasources.default': [:]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource)

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.poolProperties.password == ''
        dataSource.name == 'default'
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.abandonWhenPercentageFull == 0
        dataSource.accessToUnderlyingConnectionAllowed


        cleanup:
        applicationContext.close()
    }

    void "test operations with a blank connection"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'datasources.default': [:]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource)
        ResultSet resultSet = dataSource.getConnection().prepareStatement("SELECT H2VERSION() FROM DUAL").executeQuery()
        resultSet.next()
        String version = resultSet.getString(1)

        then:
        version == '1.4.196'

        cleanup:
        applicationContext.close()
    }

    void "test properties are bindable"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'datasources.default.abandonWhenPercentageFull': 99,
                'datasources.default.accessToUnderlyingConnectionAllowed': false,
                'datasources.default.alternateUsernameAllowed': true,
                'datasources.default.commitOnReturn': true,
                'datasources.default.connectionProperties': 'prop1=value1;prop2=value2',
                'datasources.default.jndiName': 'java:comp/env/FooBarPool',
                'datasources.default.dbProperties.DB_CLOSE_ON_EXIT': true,
                'datasources.default.dbProperties.DB_CLOSE_DELAY': 1,
                'datasources.default.defaultAutoCommit': true,
                'datasources.default.defaultCatalog': 'catalog',
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource)

        then:
        dataSource.abandonWhenPercentageFull == 99
        dataSource.accessToUnderlyingConnectionAllowed //Currently no-oped
        dataSource.alternateUsernameAllowed
        dataSource.commitOnReturn
        dataSource.connectionProperties == 'prop1=value1;prop2=value2'
        dataSource.dataSourceJNDI == 'java:comp/env/FooBarPool'
        dataSource.dbProperties.get('DB_CLOSE_ON_EXIT') == 'true'
        dataSource.dbProperties.get('DB_CLOSE_DELAY') == '1'
        dataSource.defaultAutoCommit
        dataSource.defaultCatalog == 'catalog'

        cleanup:
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'datasources.default': [:],
                'datasources.foo': [:]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource)

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.poolProperties.password == ''
        dataSource.name == 'default'
        dataSource.driverClassName == 'org.h2.Driver'

        when:
        dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("foo"))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.poolProperties.password == ''
        dataSource.name == 'foo'
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }
}