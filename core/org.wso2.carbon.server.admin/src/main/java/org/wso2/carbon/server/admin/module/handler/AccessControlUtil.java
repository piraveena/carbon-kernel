/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 * <p>
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.server.admin.module.handler;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.wso2.carbon.server.admin.internal.ServerAdminDataHolder;
import org.wso2.carbon.server.admin.model.OperationAuthorization;
import org.wso2.carbon.server.admin.model.ServiceAuthentication;

import java.util.List;
import java.util.Map;

/**
 * Utility class for authentication and authorization related operations.
 */
public class AccessControlUtil {

    private AccessControlUtil() {

    }

    /**
     * Get the service name from the message context.
     *
     * @param msgContext The message context of the request
     * @return The service name
     */
    public static String getServiceName(MessageContext msgContext) {

        AxisService service = msgContext.getAxisService();
        return service.getName();
    }

    /**
     * Get the authorization permissions for the operation from the configuration level.
     *
     * @param serviceName   service name of the operation
     * @param operationName operation name
     * @return List of permissions configured for the operation, null if not configured
     */
    public static List<String> getAuthorizationPermissionsFromConfigLevel(String serviceName, String operationName) {

        if (ServerAdminDataHolder.getInstance().isServiceAccessControlEnabled()) {
            ServiceAuthentication serviceAuth =
                    ServerAdminDataHolder.getInstance().getServiceAuthenticationMap().get(serviceName);
            if (serviceAuth != null) {
                Map<String, OperationAuthorization> operationAuthMap = serviceAuth.getOperationAuthMap();
                if (operationAuthMap != null) {
                    OperationAuthorization operationAuthorization = operationAuthMap.get(operationName);
                    if (operationAuthorization != null) {
                        return operationAuthorization.getPermissions();
                    } else {
                        return serviceAuth.getPermissions();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the authorization permissions for the operation from the configuration level.
     * This method checks at both service and operation level
     *
     * @param msgContext MessageContext
     * @return true if authentication is enabled, false if disabled and null if not configured
     */
    public static Boolean isAuthenticationEnabledAtConfigurationLevel(MessageContext msgContext) {

        String serviceName = getServiceName(msgContext);
        String operationName = msgContext.getAxisOperation().getName().getLocalPart();
        if (ServerAdminDataHolder.getInstance().isServiceAccessControlEnabled()) {
            ServiceAuthentication serviceAuth =
                    ServerAdminDataHolder.getInstance().getServiceAuthenticationMap().get(serviceName);
            if (serviceAuth != null) {
                Map<String, OperationAuthorization> operationAuthMap = serviceAuth.getOperationAuthMap();
                if (operationAuthMap != null) {
                    OperationAuthorization operationAuthorization = operationAuthMap.get(operationName);
                    if (operationAuthorization != null && operationAuthorization.isAuthenticationEnabled() != null) {
                        return operationAuthorization.isAuthenticationEnabled();
                    }
                }
                return serviceAuth.isAuthenticationEnabled();
            }
        }
        return null;
    }
}
