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
import java.util.List;

/**
 * Model class representing operation level authorization configuration
 */
public class OperationAuthorization {

    private String resource;
    private Boolean authenticationEnabled;
    private List<String> permissions = new ArrayList<>();

    public Boolean isAuthenticationEnabled() {

        return authenticationEnabled;
    }

    public void setAuthenticationEnabled(Boolean authenticationEnabled) {

        this.authenticationEnabled = authenticationEnabled;
    }

    public String getResource() {

        return resource;
    }

    public void setResource(String resource) {

        this.resource = resource;
    }

    public List<String> getPermissions() {

        return permissions;
    }

    public void setPermissions(List<String> permissions) {

        this.permissions = permissions;
    }
}
