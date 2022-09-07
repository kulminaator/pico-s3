package com.github.kulminaator.s3.integrationtest;

import com.github.kulminaator.s3.Client;
import com.github.kulminaator.s3.PicoClient;
import com.github.kulminaator.s3.S3Object;
import com.github.kulminaator.s3.auth.SimpleCredentialsProvider;
import com.github.kulminaator.s3.http.PicoHttpClient;
import com.github.kulminaator.s3.options.PutObjectOptions;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketTimeoutException;
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
    private String region;
    private String host;
    private String testObject;
    private String publicBucketName;
	private String bigListNumber;

    @Before
    public void parseEnv() {
        this.bucketName = System.getenv("PICO_TEST_BUCKET");
        if (this.bucketName == null || bucketName.trim().isEmpty()) {
            this.hasEnv = false;
            return;
        }
        this.hasEnv = true;

        this.testObject = System.getenv("PICO_TEST_OBJECT");
        this.publicBucketName = System.getenv("PICO_TEST_PUBLIC_BUCKET");
        this.accessKeyId = System.getenv("PICO_TEST_ACCESS_KEY");
        this.secretAccessKeyId = System.getenv("PICO_TEST_SECRET_KEY");
        this.sessionToken = System.getenv("PICO_TEST_SESSION_TOKEN");
        this.region = System.getenv("PICO_TEST_REGION");
        this.host = System.getenv("PICO_TEST_HOST");
        this.bigListNumber = System.getenv("PICO_TEST_BIG_LIST_NUMBER");
    }

    @Test
    public void list_big_public_folder_without_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion(region)
                .withHost(host)
                .build();

        // list the "root folder", as in no prefix
        List<S3Object> objects = pClient.listObjects(this.publicBucketName, null);

        int expectedListSize = (bigListNumber == null || bigListNumber.isBlank()) ?  1200 : Integer.parseInt(bigListNumber);
        assertTrue(objects.size() > expectedListSize);
    }

    @Test
    public void list_will_time_out() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion(region)
                .withHost(host)
                .withReadTimeout(1)
                .withConnectTimeout(1)
                .build();

        // list the "root folder", as in no prefix, will time out due to absurd timeout values
        Exception thrown = null;
        try {
           pClient.listObjects(this.publicBucketName, null);
        } catch (Exception e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(SocketTimeoutException.class, thrown.getCause().getClass());
        assertTrue(thrown.getCause().getMessage().contains("timed out"));
    }

    @Test
    public void list_only_a_subfolder() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion(region)
                .withHost(host)
                .build();

        // list the "root folder", as in no prefix
        List<S3Object> objects = pClient.listObjects(this.publicBucketName, "a-subfolder");

        assertTrue(objects.size() > 1);
        assertTrue(objects.size() < 10);
    }

    @Test
    public void handle_files_with_unicode_names() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion(region)
                .withHost(host)
                .build();

        // list the "root folder", as in no prefix
        String objectData = pClient.getObjectDataAsString(this.publicBucketName, "a-subfolder/jäääär.txt");

        assertEquals("The edge of ice in estonian language is 'jäääär'.\n", objectData);
    }

    @Test
    public void handle_file_info_requests() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion(region)
                .withHost(host)
                .build();

        // list the "root folder", as in no prefix
        S3Object objectInfo = pClient.getObject(this.publicBucketName, "a-subfolder/jäääär.txt");

        assertEquals(Long.valueOf(
                "The edge of ice in estonian language is 'jäääär'.\n"
                        .getBytes(StandardCharsets.UTF_8)
                        .length),
                objectInfo.getSize());

        assertEquals("text/plain", objectInfo.getContentType());
        assertFalse(objectInfo.getETag().isEmpty());
        assertFalse(objectInfo.getLastModified().isEmpty());
        assertEquals("a-subfolder/jäääär.txt", objectInfo.getKey());
    }

    @Test
    public void get_public_object_without_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withRegion(region)
                .withHost(host)
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
                .withRegion(region)
                .withHost(host)
                .build();

        byte[] randomData = this.buildRandomXmlData();

        pClient.putObject(this.bucketName, "put-test/uploaded-by-test.xml", randomData, "application/xml");
        // give aws some time to replicate our magnificent data.
        Thread.sleep(5000);
        String returnedData = pClient.getObjectDataAsString(this.bucketName, "put-test/uploaded-by-test.xml");

        assertArrayEquals(randomData, returnedData.getBytes(StandardCharsets.UTF_8));
    }


    @Test
    public void put_object_with_simple_credentials_and_crypto() throws Exception {
        
    	// Scaleway doesn't support server side crypto
    	if (this.noEnv() || this.host.equals("scw.cloud")) { assertTrue("Skipped", true); return;}

        final SimpleCredentialsProvider simpleCredentialsProvider = new SimpleCredentialsProvider();
        simpleCredentialsProvider.setAccessKeyId(this.accessKeyId);
        simpleCredentialsProvider.setSecretAccessKey(this.secretAccessKeyId);
        simpleCredentialsProvider.setSessionToken(this.sessionToken);

        final Client pClient = new PicoClient.Builder()
                .withHttpClient(new PicoHttpClient(true))
                .withCredentialsProvider(simpleCredentialsProvider)
                .withRegion(region)
                .withHost(host)
                .build();

        byte[] randomData = this.buildRandomXmlData();

        PutObjectOptions options = new PutObjectOptions.Builder()
                .withContentType("application/xml")
                .withServerSideEncryption(PutObjectOptions.Builder.SERVER_SIDE_ENCRYPTION_S3)
                .build();

        pClient.putObject(this.bucketName, "put-test/uploaded-by-test-encrypted.xml", randomData, options);
        // give aws some time to replicate our magnificent data.
        Thread.sleep(5000);
        String returnedData = pClient.getObjectDataAsString(this.bucketName, "put-test/uploaded-by-test-encrypted.xml");

        assertArrayEquals(randomData, returnedData.getBytes(StandardCharsets.UTF_8));

        S3Object objectData = pClient.getObject(this.bucketName, "put-test/uploaded-by-test-encrypted.xml");

        assertEquals("AES256", objectData.getServerSideEncryption());
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
                .withRegion(region)
                .withHost(host)
                .withCredentialsProvider(simpleCredentialsProvider)
                .build();
        final List<S3Object> objects = pClient.listObjects(this.bucketName);

        assertTrue(objects.size() > 0);

        System.out.println(objects);
    }


    @Test
    public void list_big_bucket_with_simple_credentials() throws Exception {
        if (this.noEnv()) { assertTrue("Skipped", true); return;}
        final SimpleCredentialsProvider simpleCredentialsProvider = new SimpleCredentialsProvider();
        simpleCredentialsProvider.setAccessKeyId(this.accessKeyId);
        simpleCredentialsProvider.setSecretAccessKey(this.secretAccessKeyId);
        simpleCredentialsProvider.setSessionToken(this.sessionToken);

        final Client pClient = new PicoClient.Builder()
                .withRegion(region)
                .withHost(host)
                .withCredentialsProvider(simpleCredentialsProvider)
                .build();
        final List<S3Object> objects = pClient.listObjects(this.publicBucketName);

        assertTrue(objects.size() > 0);

        System.out.println(objects);
    }

    private boolean noEnv() {
        return !this.hasEnv;
    }
}
