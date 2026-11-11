package com.unibague.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de conexión con Keycloak (único emisor de identidad).
 * Los secretos (client-secret, admin-client-secret) se inyectan por variable
 * de entorno / Secret de Kubernetes y nunca se versionan.
 */
@ConfigurationProperties("keycloak")
public class KeycloakProperties {

    private String serverUrl = "http://localhost:8080";
    private String realm = "tienda-deportiva";
    private String clientId = "tienda-auth";
    private String clientSecret;
    private String adminClientId = "tienda-admin";
    private String adminClientSecret;
    private String adminUser = "admin";
    private String adminPassword;

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getAdminClientId() { return adminClientId; }
    public void setAdminClientId(String adminClientId) { this.adminClientId = adminClientId; }
    public String getAdminClientSecret() { return adminClientSecret; }
    public void setAdminClientSecret(String adminClientSecret) { this.adminClientSecret = adminClientSecret; }
    public String getAdminUser() { return adminUser; }
    public void setAdminUser(String adminUser) { this.adminUser = adminUser; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public String tokenEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String masterTokenEndpoint() {
        return serverUrl + "/realms/master/protocol/openid-connect/token";
    }

    public String adminUsersEndpoint() {
        return serverUrl + "/admin/realms/" + realm + "/users";
    }
}
