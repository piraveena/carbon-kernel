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
package org.wso2.carbon.server.admin.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing service level authentication configuration
 */
public class ServiceAuthentication {

    private String serviceName;
    private boolean authenticationEnabled;
    private Map<String, OperationAuthorization> operationAuthMap = new HashMap<>();
    private List<String> permissions = new ArrayList<>();

    public String getServiceName() {

        return serviceName;
    }

    public void setServiceName(String serviceName) {

        this.serviceName = serviceName;
    }

    public boolean isAuthenticationEnabled() {

        return authenticationEnabled;
    }

    public void setAuthenticationEnabled(boolean authenticationEnabled) {

        this.authenticationEnabled = authenticationEnabled;
    }

    public Map<String, OperationAuthorization> getOperationAuthMap() {

        return operationAuthMap;
    }

    public void setOperationAuthMap(
            Map<String, OperationAuthorization> operationAuthMap) {

        this.operationAuthMap = operationAuthMap;
    }

    public List<String> getPermissions() {

        return permissions;
    }

    public void setPermissions(List<String> permissions) {

        this.permissions = permissions;
    }
}
