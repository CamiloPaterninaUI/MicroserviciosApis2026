package com.unibague.auth.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.unibague.auth.dto.RegisterRequest;

/**
 * Cliente de Keycloak: autenticación de usuarios (concesión de contraseña
 * OIDC) y creación de usuarios (API de administración de Keycloak).
 * auth-service no emite JWTs propios; todos los tokens los expide Keycloak.
 */
@Service
public class KeycloakClient {

    private final RestTemplate rest = new RestTemplate();
    private final KeycloakProperties props;

    private String cachedAdminToken;
    private Instant adminTokenExpiry;

    public KeycloakClient(KeycloakProperties props) {
        this.props = props;
    }

    /**
     * Autentica un usuario contra Keycloak y devuelve su accessToken.
     */
    public String authenticate(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
            form.add("client_secret", props.getClientSecret());
        }
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid");

        try {
            ResponseEntity<Map> res = rest.postForEntity(props.tokenEndpoint(), form, Map.class);
            if (res.getStatusCode().isError() || res.getBody() == null) {
                throw new AuthException("Credenciales inválidas");
            }
            Object token = res.getBody().get("access_token");
            if (token == null) {
                throw new AuthException("Keycloak no devolvió access_token");
            }
            return token.toString();
        } catch (HttpClientErrorException ex) {
            throw new AuthException("Credenciales inválidas", ex);
        }
    }

    /**
     * Crea un usuario en el realm de Keycloak mediante la API de administración.
     */
    public void createUser(RegisterRequest request) {
        String adminToken = obtainAdminToken();
        String url = props.adminUsersEndpoint();

        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("type", "password");
        credentials.put("value", request.password());
        credentials.put("temporary", false);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", true);
        body.put("username", request.username());
        body.put("email", request.email());
        body.put("firstName", request.firstName());
        body.put("lastName", request.lastName());
        body.put("credentials", java.util.List.of(credentials));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            rest.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
        } catch (HttpClientErrorException ex) {
            throw new AuthException("No se pudo registrar el usuario en Keycloak: " + ex.getResponseBodyAsString(), ex);
        }
    }

    private String obtainAdminToken() {
        if (cachedAdminToken != null && adminTokenExpiry != null && Instant.now().isBefore(adminTokenExpiry)) {
            return cachedAdminToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", props.getAdminUser());
        form.add("password", props.getAdminPassword());
        form.add("scope", "openid");
        try {
            ResponseEntity<Map> res = rest.postForEntity(props.masterTokenEndpoint(), form, Map.class);
            if (res.getStatusCode().isError() || res.getBody() == null) {
                throw new AuthException("No se pudo obtener el token de administración de Keycloak");
            }
            cachedAdminToken = res.getBody().get("access_token").toString();
            Object expiresIn = res.getBody().get("expires_in");
            long seconds = expiresIn instanceof Number n ? n.longValue() : 300L;
            adminTokenExpiry = Instant.now().plusSeconds(seconds - 30);
            return cachedAdminToken;
        } catch (HttpClientErrorException ex) {
            throw new AuthException("No se pudo autenticar el administrador de Keycloak", ex);
        }
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
        public AuthException(String message, Throwable cause) { super(message, cause); }
    }
}
