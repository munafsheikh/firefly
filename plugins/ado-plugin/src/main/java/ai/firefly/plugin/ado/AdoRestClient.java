package ai.firefly.plugin.ado;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class AdoRestClient {

    private final AdoPluginProperties props;
    private final RestTemplate restTemplate;

    public AdoRestClient(AdoPluginProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
    }

    private String baseUrl() {
        String url = props.getUrl();
        if (url == null || url.isBlank()) {
            url = "https://dev.azure.com/" + props.getOrganization();
        }
        return url.replaceAll("/$", "") + "/" + props.getProject();
    }

    private String apiUrl(String path) {
        return baseUrl() + path + (path.contains("?") ? "&" : "?") + "api-version=" + props.getApiVersion();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String credentials = Base64.getEncoder().encodeToString((":" + props.getPat()).getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + credentials);
        return headers;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getWorkItem(int id) {
        String url = apiUrl("/_apis/wit/workitems/" + id);
        log.debug("GET {}", url);
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> queryWorkItems(String wiql) {
        String url = apiUrl("/_apis/wit/wiql");
        log.debug("POST {} with WIQL query", url);
        Map<String, String> body = Map.of("query", wiql);
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, authHeaders()), Map.class);

        Map<String, Object> result = response.getBody();
        if (result == null || !result.containsKey("workItems")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> ids = (List<Map<String, Object>>) result.get("workItems");
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch work items
        String idsParam = String.join(",", ids.stream().map(i -> String.valueOf(i.get("id"))).toList());
        String batchUrl = baseUrl() + "/_apis/wit/workitems?ids=" + idsParam + "&api-version=" + props.getApiVersion();
        ResponseEntity<Map> batchResponse = restTemplate.exchange(
                batchUrl, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);

        Map<String, Object> batchResult = batchResponse.getBody();
        if (batchResult == null || !batchResult.containsKey("value")) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) batchResult.get("value");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateWorkItem(int id, List<Map<String, Object>> patches) {
        String url = apiUrl("/_apis/wit/workitems/" + id);
        log.debug("PATCH {}", url);
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json-patch+json"));
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.PATCH, new HttpEntity<>(patches, headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createWorkItem(String type, List<Map<String, Object>> patches) {
        String url = apiUrl("/_apis/wit/workitems/$" + type);
        log.debug("POST {}", url);
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json-patch+json"));
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(patches, headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProjects() {
        String url = props.getUrl().replaceAll("/$", "") + "/_apis/projects?api-version=" + props.getApiVersion();
        log.debug("GET {}", url);
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> result = response.getBody();
        if (result == null || !result.containsKey("value")) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) result.get("value");
    }

    public boolean testConnection() {
        try {
            String url = props.getUrl().replaceAll("/$", "") + "/_apis/projects?api-version=" + props.getApiVersion() + "&$top=1";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("ADO connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
