/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.server.admin.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.base.CarbonBaseUtils;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.core.services.authentication.BasicAccessAuthenticator;
import org.wso2.carbon.core.services.authentication.CookieAuthenticator;
import org.wso2.carbon.core.services.authentication.ServerAuthenticator;
import org.wso2.carbon.registry.core.dataaccess.DataAccessManager;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDataAccessManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.server.admin.auth.AuthenticatorServerRegistry;
import org.wso2.carbon.server.admin.common.IServerAdmin;
import org.wso2.carbon.server.admin.model.OperationAuthorization;
import org.wso2.carbon.server.admin.model.ServiceAuthentication;
import org.wso2.carbon.server.admin.service.ServerAdmin;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.MBeanRegistrar;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Hashtable;

/**
 * @scr.component name="serveradmin.service.component"" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic"  bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="server.configuration"
 * interface="org.wso2.carbon.base.api.ServerConfigurationService"
 * cardinality="1..1" policy="dynamic" bind="setServerConfigurationService" unbind="unsetServerConfigurationService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 */
public class ServerAdminServiceComponent {

    private static final Log log = LogFactory.getLog(ServerAdminServiceComponent.class);

    private boolean registeredMBeans;
    public static final String SERVER_ADMIN_MODULE_NAME = "ServerAdminModule";
    private ConfigurationContext configContext;
    private ServerAdminDataHolder dataHolder = ServerAdminDataHolder.getInstance();

    protected void activate(ComponentContext ctxt) {
        try {
            dataHolder.
                    setRestartThreadContextClassloader(Thread.currentThread().getContextClassLoader());
            //Register ServerAdmin MBean
            registerMBeans(dataHolder.getServerConfig());
            // Engaging ServerAdmin as an global module
            configContext.getAxisConfiguration().engageModule(SERVER_ADMIN_MODULE_NAME);

            //setUserManagerDriver(userRealmDefault);
            setRegistryDriver(dataHolder.getRegistryService());
            

            // Registering new Authenticator. This change should be backward compatible as
            // isAuthenticated method handles the logic as same in AuthenticationAdmin.

            BasicAccessAuthenticator basicAccessAuthenticator = new BasicAccessAuthenticator();
            Hashtable<String, String> basicAuthProps = new Hashtable<String, String>();
            basicAuthProps.put(AuthenticatorServerRegistry.AUTHENTICATOR_TYPE,
                    basicAccessAuthenticator.getAuthenticatorName());

            ctxt.getBundleContext().registerService(ServerAuthenticator.class.getName(), basicAccessAuthenticator,
                    basicAuthProps);

            // Registering cookie authenticator
            CookieAuthenticator cookieAuthenticator = new CookieAuthenticator();
            Hashtable<String, String> cookieAuthProps = new Hashtable<String, String>();
            basicAuthProps.put(AuthenticatorServerRegistry.AUTHENTICATOR_TYPE,
                    cookieAuthenticator.getAuthenticatorName());

            ctxt.getBundleContext().registerService(ServerAuthenticator.class.getName(), cookieAuthenticator,
                    cookieAuthProps);


            AuthenticatorServerRegistry.init(ctxt.getBundleContext());

            ctxt.getBundleContext().registerService(IServerAdmin.class.getName(),
                    new ServerAdmin(),
                    null);
            ctxt.getBundleContext().registerService(CommandProvider.class.getName(),
                    new ServerAdminCommandProvider(),
                    null);
            initializeServiceAuthenticationConfiguration();
            log.debug("ServerAdmin bundle is activated");
        } catch (Throwable e) {
            log.error("Failed to activate ServerAdmin bundle", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        log.debug("ServerAdmin bundle is deactivated");
    }

    private void registerMBeans(ServerConfigurationService serverConfig) {
            if (registeredMBeans) {
                return;
            }
            MBeanRegistrar.registerMBean(new ServerAdmin());
            registeredMBeans = true;
    }

    private void setRegistryDriver(RegistryService registry) {
        try {
            if (registry.getConfigSystemRegistry().getRegistryContext() != null &&
                registry.getConfigSystemRegistry().getRegistryContext().getDataAccessManager()
                        != null) {
                DataAccessManager dataAccessManager =
                        registry.getConfigSystemRegistry().getRegistryContext()
                                .getDataAccessManager();
                if (!(dataAccessManager instanceof JDBCDataAccessManager)) {
                    String msg = "Failed to obtain DB connection. Invalid data access manager.";
                    log.error(msg);
                }
                Connection dbConnection = null;
                try {
                    DataSource dataSource = ((JDBCDataAccessManager)dataAccessManager).getDataSource();
                    dbConnection = dataSource.getConnection();
                    dataHolder.setRegistryDBDriver(dbConnection.getMetaData().getDriverName());
                } finally {
                    if (dbConnection != null) {
                        dbConnection.close();
                    }
                }
            }
        } catch (Exception e) {
            String msg = "Cannot get registry driver";
            log.error(msg, e);
        }
    }

    @Reference(name = "registry.service", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {
        dataHolder.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        dataHolder.setRegistryService(null);
        dataHolder.setRegistryDBDriver(null);
    }

    @Reference(name = "user.realmservice.default", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        dataHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        dataHolder.setRealmService(null);
        dataHolder.setUserManagerDBDriver(null);
    }

    @Reference(name = "config.context.service", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigurationContextService")
    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        this.configContext = contextService.getServerConfigContext();
        dataHolder.setConfigContext(contextService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        AxisConfiguration axisConf = configContext.getAxisConfiguration();
        AxisModule statModule = axisConf.getModule(ServerAdminServiceComponent.SERVER_ADMIN_MODULE_NAME);
        if (statModule != null) {
            try {
                axisConf.disengageModule(statModule);
            } catch (AxisFault axisFault) {
                log.error("Failed disengage module: " + ServerAdminServiceComponent.SERVER_ADMIN_MODULE_NAME);
            }
        }
        this.configContext = null;
        dataHolder.setConfigContext(null);
    }

    @Reference(name = "server.configuration", cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetServerConfigurationService")
    protected void setServerConfigurationService(ServerConfigurationService serverConfiguration) {
        dataHolder.setServerConfig(serverConfiguration);
    }

    protected void unsetServerConfigurationService(ServerConfigurationService serverConfiguration) {
        dataHolder.setServerConfig(null);
    }

    private static void initializeServiceAuthenticationConfiguration() throws IOException, XMLStreamException {

        File serviceConfigFile = new File(CarbonBaseUtils.getServiceAccessControlFile());
        if (serviceConfigFile.exists()) {
            try (InputStream in = new FileInputStream(serviceConfigFile)) {
                OMElement documentElement = new StAXOMBuilder(in).getDocumentElement();
                if (documentElement != null) {
                    OMElement enabled = documentElement.getFirstChildWithName(new QName("Enabled"));
                    if (enabled == null || !Boolean.parseBoolean(enabled.getText())) {
                        ServerAdminDataHolder.getInstance().setServiceAccessControlEnabled(false);
                        // If service authentication is disabled, no need to read further configurations.
                        return;
                    }
                    ServerAdminDataHolder.getInstance().setServiceAccessControlEnabled(true);
                    Map<String, ServiceAuthentication> serviceAuthenticationMap = new HashMap<>();
                    OMElement servicesElement = documentElement.getFirstChildWithName(new QName("Services"));
                    if (servicesElement != null) {
                        Iterator<OMElement> services = servicesElement.getChildrenWithLocalName("Service");
                        if (services != null) {
                            while (services.hasNext()) {
                                OMElement serviceElement = services.next();
                                List<String> serviceLevelPermissions = retrievePermissions(serviceElement);
                                ServiceAuthentication serviceAuthentication = new ServiceAuthentication();
                                serviceAuthentication.setPermissions(serviceLevelPermissions);
                                OMElement serviceNameElement = serviceElement.getFirstChildWithName(new QName("Name"));
                                serviceAuthentication.setServiceName(serviceNameElement.getText());
                                OMElement authenticationEnabledElement =
                                        serviceElement.getFirstChildWithName(new QName("AuthenticationEnabled"));
                                serviceAuthentication.setAuthenticationEnabled(
                                        Boolean.parseBoolean(authenticationEnabledElement.getText()));
                                OMElement operationsElement =
                                        serviceElement.getFirstChildWithName(new QName("Operations"));
                                if (operationsElement != null) {
                                    Iterator<OMElement> operationElements =
                                            operationsElement.getChildrenWithLocalName("Operation");
                                    if (operationElements != null) {
                                        while (operationElements.hasNext()) {
                                            OMElement operationElement = operationElements.next();
                                            OperationAuthorization operationAuthorization =
                                                    new OperationAuthorization();
                                            String operationName =
                                                    operationElement.getAttributeValue(new QName("name"));
                                            OMElement authEnabledElement = operationElement
                                                    .getFirstChildWithName(new QName("AuthenticationEnabled"));
                                            if (authEnabledElement != null) {
                                                operationAuthorization.setAuthenticationEnabled(
                                                        Boolean.parseBoolean(authEnabledElement.getText()));
                                            }
                                            operationAuthorization.setResource(operationName);
                                            List<String> permissions = retrievePermissions(operationElement);
                                            operationAuthorization.setPermissions(permissions);
                                            serviceAuthentication.getOperationAuthMap().put(
                                                    operationAuthorization.getResource(), operationAuthorization);
                                        }
                                    }
                                }
                                serviceAuthenticationMap.put(serviceAuthentication.getServiceName(),
                                        serviceAuthentication);
                            }
                        }
                    }
                    ServerAdminDataHolder.getInstance().setServiceAuthenticationMap(serviceAuthenticationMap);
                }
            } catch (IOException e) {
                log.error("Error in reading service authentication configuration from " +
                        CarbonBaseUtils.getServiceAccessControlFile(), e);
                throw e;
            } catch (XMLStreamException e) {
                log.error("Error in parsing service authentication configuration from " +
                        CarbonBaseUtils.getServiceAccessControlFile(), e);
                throw e;
            }
        }
    }

    private static List<String> retrievePermissions(OMElement operationOrServiceElement) {

        List<String> permissions = new ArrayList<>();
        OMElement permissionsElement = operationOrServiceElement.getFirstChildWithName(new QName("Permissions"));
        if (permissionsElement != null) {
            Iterator<OMElement> permissionElements = permissionsElement.getChildrenWithLocalName("Permission");
            if (permissionElements != null) {
                while (permissionElements.hasNext()) {
                    OMElement permissionElement = permissionElements.next();
                    String permission = permissionElement.getText();
                    if (permission != null && !permission.isEmpty()) {
                        permissions.add(permission);
                    }
                }
            }
        }
        return permissions;
    }
}
