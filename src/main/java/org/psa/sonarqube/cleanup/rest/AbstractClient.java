package org.psa.sonarqube.cleanup.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractClient {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractClient.class);

    private String url;

    protected AbstractClient() {
        super();
    }

    protected void setUrl(String url) {
        this.url = url;
    }

    protected <T> T get(String path, Class<T> entityResponse) {
        return get(path, null, entityResponse);
    }

    protected <T> T get(String path, MultivaluedMap<String, Object> headers, Class<T> entityResponse) {
        return call(path, headers, null, entityResponse, false);
    }

    protected <T> T get(String path, MultivaluedMap<String, Object> headers, Class<T> entityResponse, boolean wrapRoot) {
        return call(path, headers, null, entityResponse, wrapRoot);
    }

    protected <T> T post(String path, Object entityRequest, Class<T> entityResponse) {
        return post(path, null, entityRequest, entityResponse);
    }

    protected <T> T post(String path, MultivaluedMap<String, Object> headers, Object entityRequest, Class<T> entityResponse) {
        return call(path, headers, entityRequest, entityResponse, false);
    }

    private <T> T call(String path, MultivaluedMap<String, Object> headers, Object entityRequest, Class<T> entityResponse, boolean wrapRoot) {
        long start = System.currentTimeMillis();
        try {
            LOG.debug("Call URL: {}/{}", url, path);
            if (StringUtils.isBlank(url)) {
                throw new UnsupportedOperationException("Please use 'setUrl(...)' before using this client");
            }

            Client client = ClientBuilder.newClient();
            if (wrapRoot) {
                client = client.register(ObjectMapperContextResolver.class);
            }
            WebTarget webTarget = client.target(url).path(path);
            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON).headers(headers);
            Response response = null;
            if (entityRequest == null) {
                response = invocationBuilder.get();
            } else {
                response = invocationBuilder.post(Entity.entity(entityRequest, MediaType.APPLICATION_JSON));
            }
            if (Response.Status.OK.getStatusCode() != response.getStatus()) {
                String content = IOUtils.toString((InputStream) response.getEntity(), Charset.defaultCharset());
                if (StringUtils.isNoneBlank(content)) {
                    content = " / Content: " + content;
                }
                throw new UnsupportedOperationException(
                        String.format("Unsupported status code: %s %s%s", response.getStatus(), response.getStatusInfo().getReasonPhrase(), content));
            }
            return response.readEntity(entityResponse);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        } finally {
            LOG.debug("Call URL time elaps ms: {}", System.currentTimeMillis() - start);
        }
    }

}