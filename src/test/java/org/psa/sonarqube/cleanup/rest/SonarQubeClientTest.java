package org.psa.sonarqube.cleanup.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.psa.sonarqube.cleanup.AbstractWireMock;
import org.psa.sonarqube.cleanup.config.Config;
import org.psa.sonarqube.cleanup.rest.model.Component;
import org.psa.sonarqube.cleanup.rest.model.License;
import org.psa.sonarqube.cleanup.rest.model.SearchProjects;

public class SonarQubeClientTest extends AbstractWireMock {

    @Test
    public void testShowLicence() {
        stubFor(get("/api/editions/show_license").willReturn(aResponse().withHeader(HCTKEY, HCTJSON).withBodyFile("editions.show_license.json")));
        SonarQubeClient client = mockClient();
        License license = client.getLicence();

        Assert.assertEquals(10000000, license.getMaxLoc());
        Assert.assertEquals(9000000, license.getLoc());
        Assert.assertEquals(500000, license.getRemainingLocThreshold());
    }

    @Test
    public void testSearchProjects() {
        stubFor(get(urlMatching("/api/components/search_projects.*"))
                .willReturn(aResponse().withHeader(HCTKEY, HCTJSON).withBodyFile("components.search_projects.json")));
        SonarQubeClient client = mockClient();
        SearchProjects searchProjects = client.getProjectsOldMax500();

        Assert.assertEquals(9, searchProjects.getComponents().size());
        Date previous = new Date(0);
        for (Component c : searchProjects.getComponents()) {
            Date current = c.getAnalysisDate();
            Assert.assertTrue(String.format("Previous: %s / Current: %s", previous, current), current.after(previous));
            previous = current;
        }
    }

    @Test
    public void testComponentDetail() {
        stubFor(get(urlMatching("/api/measures/component.*"))
                .willReturn(aResponse().withHeader(HCTKEY, HCTJSON).withBodyFile("measures.component.1.json")));
        SonarQubeClient client = mockClient();
        Component project = client.getProject("com.company:project1");
        Assert.assertEquals("AVxxxxxxxxxxxxxxxxx1", project.getId());
        Assert.assertEquals("com.company:project1", project.getKey());
        Assert.assertEquals("Mock project 1", project.getName());
        Assert.assertEquals("Mock project 1 description", project.getDescription());
        Assert.assertEquals(1042, project.getNcloc());
    }

    private SonarQubeClient mockClient() {
        Config config = new Config(new String[] { "-h", "http://localhost:" + server.port(), "-l", "admin" });
        return SonarQubeClient.build(config);
    }

}