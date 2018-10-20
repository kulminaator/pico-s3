package com.github.kulminaator.s3.integrationtest;

import com.github.kulminaator.s3.Client;
import com.github.kulminaator.s3.PicoClient;
import com.github.kulminaator.s3.S3Object;
import com.github.kulminaator.s3.auth.SimpleCredentialsProvider;
import com.github.kulminaator.s3.http.PicoHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests in this class are environment specific and not part of regular junit suite.
 * See the test_with_env-example.sh script for details.
 */
public class RealLifeTest {

    private String bucketName;
    private boolean hasEnv = false;

    private String accessKeyId;
    private String secretAccessKeyId;
    private String sessionToken;
    private String testObject;

    @Before
    public void parseEnv() {
        this.bucketName = System.getenv("PICO_TEST_BUCKET");
        if (this.bucketName == null || bucketName.trim().isEmpty()) {
            this.hasEnv = false;
            return;
        }
        this.hasEnv = true;

        this.testObject = System.getenv("PICO_TEST_OBJECT");
        this.accessKeyId = System.getenv("PICO_TEST_ACCESS_KEY");
        this.secretAccessKeyId = System.getenv("PICO_TEST_SECRET_KEY");
        this.sessionToken = System.getenv("PICO_TEST_SESSION_TOKEN");
    }

    @Ignore
    @Test
    public void list_folder_without_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion("eu-west-1")
                .build();
        pClient.listObjects(this.bucketName, "public-read-folder/");
    }

    @Test
    public void get_public_object_without_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion("eu-west-1")
                .build();
        final String data =
                pClient.getObjectDataAsString(this.bucketName, "public-read-folder/anyone_can_read_this.txt");

        assertEquals("anyone can read this\n", data);
    }

    @Test
    public void put_object_with_simple_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}

        final SimpleCredentialsProvider simpleCredentialsProvider = new SimpleCredentialsProvider();
        simpleCredentialsProvider.setAccessKeyId(this.accessKeyId);
        simpleCredentialsProvider.setSecretAccessKey(this.secretAccessKeyId);
        simpleCredentialsProvider.setSessionToken(this.sessionToken);

        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withCredentialsProvider(simpleCredentialsProvider)
                .withRegion("eu-west-1")
                .build();

        byte[] randomData = this.buildRandomXmlData();

        pClient.putObject(this.bucketName, "put-test/uploaded-by-test.xml", randomData, "application/xml");
        // give aws some time to replicate our magnificent data.
        Thread.sleep(5000);
        String returnedData = pClient.getObjectDataAsString(this.bucketName, "put-test/uploaded-by-test.xml");

        assertArrayEquals(randomData, returnedData.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] buildRandomXmlData() {

        StringBuilder mediumDataBuilder = new StringBuilder();
        mediumDataBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        mediumDataBuilder.append("<HereBeData>");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1024; i++) {
            mediumDataBuilder.append(start++);
            mediumDataBuilder.append(",");
        }
        mediumDataBuilder.append("</HereBeData>");
        return mediumDataBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void list_folder_with_simple_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final SimpleCredentialsProvider simpleCredentialsProvider = new SimpleCredentialsProvider();
        simpleCredentialsProvider.setAccessKeyId(this.accessKeyId);
        simpleCredentialsProvider.setSecretAccessKey(this.secretAccessKeyId);
        simpleCredentialsProvider.setSessionToken(this.sessionToken);

        final Client pClient = new PicoClient.Builder()
                .withRegion("eu-west-1")
                .withCredentialsProvider(simpleCredentialsProvider)
                .build();
        final List<S3Object> objects = pClient.listObjects(this.bucketName);

        assertTrue(objects.size() > 0);

        System.out.println(objects);
    }

    private boolean noEnv() {
        return !this.hasEnv;
    }
}