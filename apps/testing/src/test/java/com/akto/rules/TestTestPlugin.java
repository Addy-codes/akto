package com.akto.rules;

import com.akto.MongoBasedTest;
import com.akto.dao.context.Context;
import com.akto.dao.testing.TestingRunResultDao;
import com.akto.dto.*;
import com.akto.dto.testing.*;
import com.akto.dto.type.SingleTypeInfo;
import com.akto.dto.type.URLMethods;
import com.akto.store.SampleMessageStore;
import com.akto.types.CappedSet;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TestTestPlugin extends MongoBasedTest {

    @Test
    public void testIsStatusGood() {
        boolean result = TestPlugin.isStatusGood(200);
        assertTrue(result);
        result = TestPlugin.isStatusGood(300);
        assertFalse(result);
        result = TestPlugin.isStatusGood(299);
        assertTrue(result);
        result = TestPlugin.isStatusGood(100);
        assertFalse(result);
    }

    @Test
    public void testCompareWithOriginalResponse() {
//        {"name": "Ankush", "age": 100, "friends": [{"name": "Avneesh", "stud": true}, {"name": "ankita", "stud": true}], "jobs": ["MS", "CT"]}
        String originalPayload = "{\"name\": \"Ankush\", \"age\": 100, \"friends\": [{\"name\": \"Avneesh\", \"stud\": true}, {\"name\": \"ankita\", \"stud\": true}], \"jobs\": [\"MS\", \"CT\"]}";
        String currentPayload = "{\"name\": \"Vian\", \"age\": 1, \"friends\": [{\"name\": \"Avneesh\", \"stud\": true}, {\"name\": \"Ankita\", \"stud\": true}, {\"name\": \"Ankush\", \"stud\": true}], \"jobs\": []}";
        double val = TestPlugin.compareWithOriginalResponse(originalPayload, currentPayload);
        assertEquals(val,20.0, 0.0);

        // {"nestedObject": {"keyA":{"keyB":"B", "keyC": ["A", "B"]}}}
        originalPayload = "{\"nestedObject\": {\"keyA\":{\"keyB\":\"B\", \"keyC\": [\"A\", \"B\"]}}}";
        currentPayload = "{\"nestedObject\": {\"keyA\":{\"keyB\":\"B\", \"keyC\": [\"A\"]}}}";
        val = TestPlugin.compareWithOriginalResponse(originalPayload, currentPayload);
        assertEquals(val,50.0, 0.0);

        // [{"name": "A", "age": 10},{"name": "B", "age": 10},{"name": "C", "age": 10}]
        originalPayload = "[{\"name\": \"A\", \"age\": 10},{\"name\": \"B\", \"age\": 10},{\"name\": \"C\", \"age\": 10}]";
        currentPayload = "[{\"name\": \"B\", \"age\": 10},{\"name\": \"B\", \"age\": 10},{\"name\": \"C\", \"age\": 10}]";
        val = TestPlugin.compareWithOriginalResponse(originalPayload, currentPayload);
        assertEquals(val,50.0, 0.0);
    }

    @Test
    public void testAddWithoutRequestError() {
        TestingRunResultDao.instance.getMCollection().drop();
        TestPlugin bolaTest = new BOLATest();
        TestPlugin noAuthTest = new NoAuthTest();

        ApiInfo.ApiInfoKey apiInfoKey1 = new ApiInfo.ApiInfoKey(0,"url1", URLMethods.Method.GET);
        ObjectId testRunId1 = new ObjectId();
        bolaTest.addWithoutRequestError(apiInfoKey1, testRunId1,"", TestResult.TestError.API_REQUEST_FAILED);
        noAuthTest.addWithoutRequestError(apiInfoKey1, testRunId1, "",TestResult.TestError.NO_AUTH_MECHANISM);

        ApiInfo.ApiInfoKey apiInfoKey2 = new ApiInfo.ApiInfoKey(0,"url1", URLMethods.Method.POST);
        ObjectId testRunId2 = new ObjectId();
        bolaTest.addWithoutRequestError(apiInfoKey2, testRunId2, "",TestResult.TestError.NO_PATH);

        ObjectId testRunId3 = new ObjectId();
        noAuthTest.addWithoutRequestError(apiInfoKey1, testRunId3, "",TestResult.TestError.NO_PATH);

        List<TestingRunResult> testingRunResultList = TestingRunResultDao.instance.findAll(new BasicDBObject());
        assertEquals(testingRunResultList.size(),3);

        TestingRunResult testingRunResult1 = TestingRunResultDao.instance.findOne(TestingRunResultDao.generateFilter(testRunId1,apiInfoKey1));
        assertEquals(testingRunResult1.getResultMap().size(),2);
        assertFalse(testingRunResult1.getResultMap().get(bolaTest.testName()).isVulnerable());
        assertEquals(testingRunResult1.getResultMap().get(bolaTest.testName()).getErrors().get(0), TestResult.TestError.API_REQUEST_FAILED);
        assertEquals(testingRunResult1.getResultMap().get(noAuthTest.testName()).getErrors().get(0), TestResult.TestError.NO_AUTH_MECHANISM);

        TestingRunResult testingRunResult2 = TestingRunResultDao.instance.findOne(TestingRunResultDao.generateFilter(testRunId2,apiInfoKey2));
        assertEquals(testingRunResult2.getResultMap().size(),1);
        assertEquals(testingRunResult2.getResultMap().get(bolaTest.testName()).getErrors().get(0), TestResult.TestError.NO_PATH);
        assertNull(testingRunResult2.getResultMap().get(noAuthTest.testName()));

        TestingRunResult testingRunResult3 = TestingRunResultDao.instance.findOne(TestingRunResultDao.generateFilter(testRunId3,apiInfoKey1));
        assertEquals(testingRunResult3.getResultMap().size(),1);
        assertEquals(testingRunResult3.getResultMap().get(noAuthTest.testName()).getErrors().get(0), TestResult.TestError.NO_PATH);
        assertNull(testingRunResult3.getResultMap().get(bolaTest.testName()));
    }

    @Test
    public void testContainsPrivateResource() {
        SampleMessageStore.singleTypeInfos = new HashMap<>();
        BOLATest bolaTest = new BOLATest();

        // FIRST (Contains only private resources)
        ApiInfo.ApiInfoKey apiInfoKey1 = new ApiInfo.ApiInfoKey(123, "/api/books", URLMethods.Method.GET);

        insertIntoStiMap(apiInfoKey1,"param1", SingleTypeInfo.EMAIL, false, true);
        insertIntoStiMap(apiInfoKey1,"param2", SingleTypeInfo.GENERIC, false, true);
        insertIntoStiMap(apiInfoKey1,"param3", SingleTypeInfo.GENERIC, false, true);

        String payload1 = "{\"param1\": \"avneesh@akto.io\", \"param2\": \"ankush\"}";
        OriginalHttpRequest originalHttpRequest1 = new OriginalHttpRequest("/api/books", "param3=ankita", apiInfoKey1.getMethod().name(), payload1, new HashMap<>(), "");
        TestPlugin.ContainsPrivateResourceResult result1 = bolaTest.containsPrivateResource(originalHttpRequest1, apiInfoKey1);
        assertEquals(3, result1.singleTypeInfos.size());
        assertEquals(3, result1.findPrivateOnes().size());
        assertTrue(result1.isPrivate);

        // SECOND (Contains 2 public resources)
        ApiInfo.ApiInfoKey apiInfoKey2 = new ApiInfo.ApiInfoKey(123, "api/INTEGER/cars/STRING", URLMethods.Method.GET);

        insertIntoStiMap(apiInfoKey2,"1", SingleTypeInfo.INTEGER_32, true, true);
        insertIntoStiMap(apiInfoKey2,"3", SingleTypeInfo.GENERIC, true,false);
        insertIntoStiMap(apiInfoKey2,"param1", SingleTypeInfo.GENERIC, false, true);
        insertIntoStiMap(apiInfoKey2,"param2", SingleTypeInfo.GENERIC, false,false);
        String payload2 = "{\"param1\": \"Ronaldo\", \"param2\": \"Messi\"}";
        OriginalHttpRequest originalHttpRequest2 = new OriginalHttpRequest("/api/INTEGER/cars/STRING", null ,apiInfoKey2.getMethod().name(), payload2, new HashMap<>(), "");
        TestPlugin.ContainsPrivateResourceResult result2 = bolaTest.containsPrivateResource(originalHttpRequest2, apiInfoKey2);
        assertEquals(4, result2.singleTypeInfos.size());
        assertFalse(result2.isPrivate);
        assertEquals(2, result2.findPrivateOnes().size());

        // THIRD (All missing) [We give missing STI benefit of doubt and consider it to be private]
        ApiInfo.ApiInfoKey apiInfoKey3 = new ApiInfo.ApiInfoKey(123, "/api/bus", URLMethods.Method.GET);

        String payload3 = "{\"param1\": \"Ronaldo\", \"param2\": \"Messi\"}";
        OriginalHttpRequest originalHttpRequest3 = new OriginalHttpRequest("/api/bus", null, apiInfoKey3.method.name(), payload3, new HashMap<>(), "");
        TestPlugin.ContainsPrivateResourceResult result3 = bolaTest.containsPrivateResource(originalHttpRequest3, apiInfoKey3);
        assertEquals(0, result3.singleTypeInfos.size());
        assertTrue(result3.isPrivate);
        assertEquals(0, result3.findPrivateOnes().size());

        // FOURTH (Empty payload)
        ApiInfo.ApiInfoKey apiInfoKey4 = new ApiInfo.ApiInfoKey(123, "/api/toys", URLMethods.Method.GET);

        String payload4 = "{}";
        OriginalHttpRequest originalHttpRequest4 = new OriginalHttpRequest("/api/toys",null, apiInfoKey4.getMethod().name(), payload4, new HashMap<>(), "");
        TestPlugin.ContainsPrivateResourceResult result4 = bolaTest.containsPrivateResource(originalHttpRequest4, apiInfoKey4);
        assertEquals(0, result4.singleTypeInfos.size());
        assertFalse(result4.isPrivate);
        assertEquals(0, result4.findPrivateOnes().size());

    }

    private HttpRequestParams buildHttpReq(String url, String method, int apiCollectionId, String payload) {
        return new HttpRequestParams(
                method, url, "", new HashMap<>(), payload, apiCollectionId
        );
    }

    private void insertIntoStiMap(ApiInfo.ApiInfoKey apiInfoKey, String param, SingleTypeInfo.SubType subType, boolean isUrlParam, boolean isPrivate)  {
        int apiCollectionId = apiInfoKey.getApiCollectionId();
        String url = apiInfoKey.getUrl();
        String method = apiInfoKey.getMethod().name();

        SingleTypeInfo.ParamId paramId = new SingleTypeInfo.ParamId(
                url, method,-1, false, param, subType, apiCollectionId, isUrlParam
        );

        SingleTypeInfo singleTypeInfo = new SingleTypeInfo(
                paramId,new HashSet<>(), new HashSet<>(), 0, Context.now(), 0, new CappedSet<>(), SingleTypeInfo.Domain.ENUM, SingleTypeInfo.ACCEPTED_MAX_VALUE, SingleTypeInfo.ACCEPTED_MIN_VALUE
        );

        singleTypeInfo.setPublicCount(10);
        if (isPrivate) {
            singleTypeInfo.setUniqueCount(1000000);
        } else {
            singleTypeInfo.setUniqueCount(5);
        }

        SampleMessageStore.singleTypeInfos.put(singleTypeInfo.composeKeyWithCustomSubType(SingleTypeInfo.GENERIC), singleTypeInfo);
    }

    @Test
    public void testFetchMessagesWithAuthToken() {
        SampleMessageStore.sampleDataMap = new HashMap<>();
        TestingRunResultDao.instance.getMCollection().drop();

        AuthMechanism authMechanism = new AuthMechanism(
                Collections.singletonList(new HardcodedAuthParam(AuthParam.Location.HEADER, "akto", "something"))
        );

        ApiInfo.ApiInfoKey apiInfoKey1 = new ApiInfo.ApiInfoKey(0, "https://petstore.swagger.io/v2/pet/2", URLMethods.Method.GET);
        List<String> values = new ArrayList<>();
        // both values don't contain auth token
        values.add("{ \"method\": \"GET\", \"requestPayload\": \"{}\", \"responsePayload\": \"{\\\"id\\\":2,\\\"category\\\":{\\\"id\\\":0},\\\"name\\\":\\\"teste\\\",\\\"photoUrls\\\":[],\\\"tags\\\":[]}\", \"ip\": \"null\", \"source\": \"HAR\", \"type\": \"HTTP/2\", \"akto_vxlan_id\": \"1661807253\", \"path\": \"https://petstore.swagger.io/v2/pet/2\", \"requestHeaders\": \"{\\\"TE\\\":\\\"trailers\\\",\\\"Accept\\\":\\\"application/json\\\",\\\"User-Agent\\\":\\\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:95.0) Gecko/20100101 Firefox/95.0\\\",\\\"Referer\\\":\\\"https://petstore.swagger.io/\\\",\\\"Connection\\\":\\\"keep-alive\\\",\\\"Sec-Fetch-Dest\\\":\\\"empty\\\",\\\"Sec-Fetch-Site\\\":\\\"same-origin\\\",\\\"Host\\\":\\\"petstore.swagger.io\\\",\\\"Accept-Language\\\":\\\"en-US,en;q=0.5\\\",\\\"Accept-Encoding\\\":\\\"gzip, deflate, br\\\",\\\"Sec-Fetch-Mode\\\":\\\"cors\\\", \\\"Origin\\\" : \\\"dddd\\\"}\", \"responseHeaders\": \"{\\\"date\\\":\\\"Tue, 04 Jan 2022 20:12:27 GMT\\\",\\\"access-control-allow-origin\\\":\\\"*\\\",\\\"server\\\":\\\"Jetty(9.2.9.v20150224)\\\",\\\"access-control-allow-headers\\\":\\\"Content-Type, api_key, Authorization\\\",\\\"X-Firefox-Spdy\\\":\\\"h2\\\",\\\"content-type\\\":\\\"application/json\\\",\\\"access-control-allow-methods\\\":\\\"GET, POST, DELETE, PUT\\\"}\", \"time\": \"1641327147\", \"contentType\": \"application/json\", \"akto_account_id\": \"1000000\", \"statusCode\": \"200\", \"status\": \"OK\" }");
        values.add("{ \"method\": \"GET\", \"requestPayload\": \"{}\", \"responsePayload\": \"{\\\"id\\\":2,\\\"category\\\":{\\\"id\\\":0},\\\"name\\\":\\\"teste\\\",\\\"photoUrls\\\":[],\\\"tags\\\":[]}\", \"ip\": \"null\", \"source\": \"HAR\", \"type\": \"HTTP/2\", \"akto_vxlan_id\": \"1661807253\", \"path\": \"https://petstore.swagger.io/v2/pet/2\", \"requestHeaders\": \"{\\\"TE\\\":\\\"trailers\\\",\\\"Accept\\\":\\\"application/json\\\",\\\"User-Agent\\\":\\\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:95.0) Gecko/20100101 Firefox/95.0\\\",\\\"Referer\\\":\\\"https://petstore.swagger.io/\\\",\\\"Connection\\\":\\\"keep-alive\\\",\\\"Sec-Fetch-Dest\\\":\\\"empty\\\",\\\"Sec-Fetch-Site\\\":\\\"same-origin\\\",\\\"Host\\\":\\\"petstore.swagger.io\\\",\\\"Accept-Language\\\":\\\"en-US,en;q=0.5\\\",\\\"Accept-Encoding\\\":\\\"gzip, deflate, br\\\",\\\"Sec-Fetch-Mode\\\":\\\"cors\\\", \\\"Origin\\\" : \\\"dddd\\\"}\", \"responseHeaders\": \"{\\\"date\\\":\\\"Tue, 04 Jan 2022 20:12:27 GMT\\\",\\\"access-control-allow-origin\\\":\\\"*\\\",\\\"server\\\":\\\"Jetty(9.2.9.v20150224)\\\",\\\"access-control-allow-headers\\\":\\\"Content-Type, api_key, Authorization\\\",\\\"X-Firefox-Spdy\\\":\\\"h2\\\",\\\"content-type\\\":\\\"application/json\\\",\\\"access-control-allow-methods\\\":\\\"GET, POST, DELETE, PUT\\\"}\", \"time\": \"1641327147\", \"contentType\": \"application/json\", \"akto_account_id\": \"1000000\", \"statusCode\": \"200\", \"status\": \"OK\" }");

        BOLATest bolaTest = new BOLATest();

        ObjectId testRunId1 = new ObjectId();
        bolaTest.fetchMessagesWithAuthToken(apiInfoKey1, testRunId1 , authMechanism);
        TestingRunResult testingRunResult1 = TestingRunResultDao.instance.findOne(Filters.eq("testRunId", testRunId1));
        assertEquals(TestResult.TestError.NO_PATH, testingRunResult1.getResultMap().get("BOLA").getErrors().get(0));

        SampleMessageStore.sampleDataMap.put(apiInfoKey1, values);

        ObjectId testRunId2 = new ObjectId();
        bolaTest.fetchMessagesWithAuthToken(apiInfoKey1, testRunId2 , authMechanism);
        TestingRunResult testingRunResult2 = TestingRunResultDao.instance.findOne(Filters.eq("testRunId", testRunId2));
        assertEquals(TestResult.TestError.NO_AUTH_TOKEN_FOUND, testingRunResult2.getResultMap().get("BOLA").getErrors().get(0));

        // this value contains auth token so no errors
        values.add("{ \"method\": \"GET\", \"requestPayload\": \"{}\", \"responsePayload\": \"{\\\"id\\\":2,\\\"category\\\":{\\\"id\\\":0},\\\"name\\\":\\\"teste\\\",\\\"photoUrls\\\":[],\\\"tags\\\":[]}\", \"ip\": \"null\", \"source\": \"HAR\", \"type\": \"HTTP/2\", \"akto_vxlan_id\": \"1661807253\", \"path\": \"https://petstore.swagger.io/v2/pet/2\", \"requestHeaders\": \"{\\\"TE\\\":\\\"trailers\\\",\\\"Accept\\\":\\\"application/json\\\",\\\"User-Agent\\\":\\\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:95.0) Gecko/20100101 Firefox/95.0\\\",\\\"Referer\\\":\\\"https://petstore.swagger.io/\\\",\\\"Connection\\\":\\\"keep-alive\\\",\\\"Sec-Fetch-Dest\\\":\\\"empty\\\",\\\"Sec-Fetch-Site\\\":\\\"same-origin\\\",\\\"Host\\\":\\\"petstore.swagger.io\\\",\\\"Accept-Language\\\":\\\"en-US,en;q=0.5\\\",\\\"Accept-Encoding\\\":\\\"gzip, deflate, br\\\",\\\"Sec-Fetch-Mode\\\":\\\"cors\\\", \\\"Origin\\\" : \\\"dddd\\\", \\\"akto\\\" : \\\"blah\\\"}\", \"responseHeaders\": \"{\\\"date\\\":\\\"Tue, 04 Jan 2022 20:12:27 GMT\\\",\\\"access-control-allow-origin\\\":\\\"*\\\",\\\"server\\\":\\\"Jetty(9.2.9.v20150224)\\\",\\\"access-control-allow-headers\\\":\\\"Content-Type, api_key, Authorization\\\",\\\"X-Firefox-Spdy\\\":\\\"h2\\\",\\\"content-type\\\":\\\"application/json\\\",\\\"access-control-allow-methods\\\":\\\"GET, POST, DELETE, PUT\\\"}\", \"time\": \"1641327147\", \"contentType\": \"application/json\", \"akto_account_id\": \"1000000\", \"statusCode\": \"200\", \"status\": \"OK\" }");
        SampleMessageStore.sampleDataMap.put(apiInfoKey1, values);

        ObjectId testRunId3 = new ObjectId();
        bolaTest.fetchMessagesWithAuthToken(apiInfoKey1, testRunId3 , authMechanism);
        TestingRunResult testingRunResult3 = TestingRunResultDao.instance.findOne(Filters.eq("testRunId", testRunId3));
        assertNull(testingRunResult3);

    }

    @Test
    public void testAddTestSuccessResult() {
        TestingRunResultDao.instance.getMCollection().drop();

        BOLATest bolaTest = new BOLATest();

        ApiInfo.ApiInfoKey apiInfoKey1 = new ApiInfo.ApiInfoKey(0, "https://petstore.swagger.io/v2/pet/2", URLMethods.Method.GET);

        String originalMessage = "{\"method\":\"POST\",\"requestPayload\":\"{\\\"firstName\\\":\\\"string\\\",\\\"lastName\\\":\\\"string\\\",\\\"password\\\":\\\"string\\\",\\\"userStatus\\\":0,\\\"phone\\\":\\\"string\\\",\\\"id\\\":0,\\\"email\\\":\\\"string\\\",\\\"username\\\":\\\"string\\\"}\",\"responsePayload\":\"{\\\"code\\\":200,\\\"type\\\":\\\"unknown\\\",\\\"message\\\":\\\"9223372036854775807\\\"}\",\"ip\":\"null\",\"source\":\"HAR\",\"type\":\"HTTP/2\",\"akto_vxlan_id\":\"1661807253\",\"path\":\"https://petstore.swagger.io/v2/user\",\"requestHeaders\":\"{\\\"Origin\\\":\\\"https://petstore.swagger.io\\\",\\\"Accept\\\":\\\"application/json\\\",\\\"User-Agent\\\":\\\"Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:95.0) Gecko/20100101 Firefox/95.0\\\",\\\"Referer\\\":\\\"https://petstore.swagger.io/\\\",\\\"Connection\\\":\\\"keep-alive\\\",\\\"Sec-Fetch-Dest\\\":\\\"empty\\\",\\\"Sec-Fetch-Site\\\":\\\"same-origin\\\",\\\"Host\\\":\\\"petstore.swagger.io\\\",\\\"Accept-Encoding\\\":\\\"gzip, deflate, br\\\",\\\"Sec-Fetch-Mode\\\":\\\"cors\\\",\\\"TE\\\":\\\"trailers\\\",\\\"Accept-Language\\\":\\\"en-US,en;q=0.5\\\",\\\"Content-Length\\\":\\\"171\\\",\\\"Content-Type\\\":\\\"application/json\\\"}\",\"responseHeaders\":\"{\\\"date\\\":\\\"Tue, 04 Jan 2022 20:14:55 GMT\\\",\\\"access-control-allow-origin\\\":\\\"*\\\",\\\"server\\\":\\\"Jetty(9.2.9.v20150224)\\\",\\\"access-control-allow-headers\\\":\\\"Content-Type, api_key, Authorization\\\",\\\"X-Firefox-Spdy\\\":\\\"h2\\\",\\\"content-type\\\":\\\"application/json\\\",\\\"access-control-allow-methods\\\":\\\"GET, POST, DELETE, PUT\\\"}\",\"time\":\"1641327295\",\"contentType\":\"application/json\",\"akto_account_id\":\"1000000\",\"statusCode\":\"200\",\"status\":\"OK\"}";

        OriginalHttpRequest originalHttpRequest = new OriginalHttpRequest();
        originalHttpRequest.buildFromSampleMessage(originalMessage);
        OriginalHttpResponse originalHttpResponse = new OriginalHttpResponse();
        originalHttpResponse.buildFromSampleMessage(originalMessage);

        ObjectId testRunId = new ObjectId();
        boolean vulnerable = true;
        double percentageMatch = 20.9d;

        List<SingleTypeInfo> singleTypeInfos = new ArrayList<>();
        singleTypeInfos.add(new SingleTypeInfo());

        TestResult.Confidence confidence = TestResult.Confidence.HIGH;

        bolaTest.addTestSuccessResult(
                apiInfoKey1, originalHttpRequest, originalHttpResponse, originalMessage, testRunId,
                vulnerable, percentageMatch, singleTypeInfos, confidence
        );

        TestingRunResult testingRunResult = TestingRunResultDao.instance.findOne(Filters.eq("testRunId", testRunId));

        TestResult testResult = testingRunResult.getResultMap().get("BOLA");
        assertEquals(0, testResult.getErrors().size());
        assertTrue(testResult.getMessage().length() > 100);
        assertTrue(testResult.getOriginalMessage().length() > 100);
        assertEquals(vulnerable, testResult.isVulnerable());
        assertEquals(percentageMatch, testResult.getPercentageMatch(), 0.0d);
        assertEquals(confidence, testResult.getConfidence());
    }
}
