import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;

public class ItunesTest {

        private static final Logger logger = LogManager.getLogger(ItunesTest.class);
        private ExtentReports extent;
        private ExtentTest test;

        @BeforeClass
        public void loggerConfig() {
                PropertyConfigurator.configure(getClass().getClassLoader().getResource("log4j.properties"));
                ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("test-output/ExtentReport.html");
                extent = new ExtentReports();
                extent.attachReporter(htmlReporter);
        }

        @BeforeMethod
        public static void setUp() {
                RestAssured.baseURI = "https://itunes.apple.com";
        }

    @Test
    public void testSearchMusicByArtist() { 
        try{
        test = extent.createTest("testSearchMusicByArtist");

        Response response = given()
                .queryParam("term", "Jack Johnson")
                .queryParam("entity", "allArtist")
                .queryParam("attribute", "allArtistTerm")
                .when()
                .get("/search")
                .then()
                .log().all()
                .extract().response();

        response.then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("results[0].artistName", equalTo("Jack Johnson"))
                .body("results[0].wrapperType", equalTo("artist"));
        
        test.pass("All assertions passed");
        }
        catch(AssertionError e){
                test.fail("Assertion fail "+e.getMessage());
                throw e;
        }
                
    }

        @Test(dataProvider = "lookUpIDRequests", dataProviderClass = SearchScenariosDataProvider.class)
        public void testLookupRequestByITunesID(String id, String media, String entity, String idType) {
                String parameterName;
                switch (idType) {
                        case "id":
                                parameterName = "id";
                                break;
                        case "amgId":
                                parameterName = "amgArtistId";
                                break;
                        case "upc":
                                parameterName = "upc";
                                break;
                        case "amgAlbumId":
                                parameterName = "amgAlbumId";
                                break;
                        case "amgVideoId":
                                parameterName = "amgVideoId";
                                break;
                        case "isbn":
                                parameterName = "isbn";
                                break;
                        default:
                                parameterName = "";
                                break;
                }

                Response response = given()
                                .queryParam(parameterName, id)
                                .queryParam("media", media)
                                .queryParam("entity", entity)
                                .when()
                                .get("/lookup")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("results.size()", greaterThan(0));
        }

        @Test
        public void testSearchByApp() {

                String appName = "Yelp";
                Response response = given()
                                .queryParam("term", appName)
                                .queryParam("entity", "software")
                                .queryParam("country", "us")
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .body("results.size()", greaterThan(0))
                                .body("results[0].wrapperType", equalTo("software"))
                                .body("results[0].trackName", containsString(appName));

        }

        @Test
        public void testSearchContentWithVersion() {

                String term = "jack+johnson";
                String media = "music";
                int limit = 25;
                int version = 2;

                Response response = given()
                                .queryParam("term", term)
                                .queryParam("media", media)
                                .queryParam("limit", limit)
                                .queryParam("version", version)
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("results.size()", equalTo(limit))
                                .body("results.every { it -> it.wrapperType == 'track' && it.kind == 'song' }",
                                                is(true));

        }

        @Test
        public void testSearchContentWithCountry() {

                String term = "jim+jones";
                String country = "CA";

                Response response = given()
                                .queryParam("term", term)
                                .queryParam("country", country)
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                JsonPath jsonPath = response.jsonPath();
                List<Map<String, ?>> results = jsonPath.getList("results");

                // Filter results with kind other than "song" and "feature-movie"
                List<Map<String, ?>> otherResults = results.stream()
                                .filter(result -> !"feature-movie".equals(result.get("kind"))
                                                && !"song".equals(result.get("kind")))
                                .collect(Collectors.toList());

                for (Map<String, ?> otherResult : otherResults) {
                        System.out.println("Other Result: " + otherResult);
                }

                response.then()
                                .statusCode(200)
                                .body("results.every { it -> (it.wrapperType == 'track' || it.wrapperType == 'audiobook') && it.country == 'CAN' }",
                                                is(true));

        }

        @Test
        public void testSearchWithInvalidParameterName() {

                Response response = given()
                                .queryParam("InvalidKey", "music")
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(400)
                                .body("errorMessage", containsString("Invalid key"));

        }

        @Test(dataProvider = "invalidAttributeParameters", dataProviderClass = SearchScenariosDataProvider.class)
        public void testSearchWithInvalidAttributeParameter(String media, String attribute) {

                Response response = given()
                                .queryParam("media", media)
                                .queryParam("attribute", attribute)
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(400)
                                .body("errorMessage", containsString("Invalid value(s) for key(s): [attributeType]"));

        }

        @Test(dataProvider = "invalidEntityParameters", dataProviderClass = SearchScenariosDataProvider.class)
        public void testSearchWithInvalidEntityParameter(String media, String entity) {

                Response response = given()
                                .queryParam("media", media)
                                .queryParam("entity", entity)
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(400)
                                .body("errorMessage", containsString("Invalid value(s) for key(s): [resultEntity]"));

        }

        @Test
        public void testSearchWithNoParameters() {

                Response response = given()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(400)
                                .body("errorMessage", containsString("Missing parameter"));
        }

        @Test
        public void testSearchWithInvalidRequestMethod() {

                Response response = given()
                                .queryParam("term", "jack+jasckson")
                                .when()
                                .post("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(405)
                                .body("errorMessage", containsString("Method Not allowed"));
        }

        @Test
        public void testSearchWithNoInternet() {

                Response response;

                try {
                        response = given()
                                        .queryParam("term", "Jack+Johnson")
                                        .get("/search")
                                        .then()
                                        .log().all()
                                        .extract().response();

                        response.then()
                                        .statusCode(200);

                }

                catch (Exception e) {
                        logger.error("No internet connectivity");
                }

        }

        @Test
        public void testLookupWithInvalidID() {

                String invalidID = "000000";

                Response response = given()
                                .queryParam("id", invalidID)
                                .when()
                                .get("/lookup")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .body("results.size()", equalTo(0));

        }

        @Test
        public void testSearchWithCallback() {

                String term = "jack+johnson";
                String callbackFunction = "handleSearchResults";

                Response response = given()
                                .queryParam("term", term)
                                .queryParam("callback", callbackFunction)
                                .when()
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body(startsWith(callbackFunction + "("))
                                .body(containsString("\"results\":"))
                                .body(endsWith(");"));
        }

        @Test
        public void testSearchForExplicitContent() {

                String term = "explicit+content";

                Response response = given()
                                .queryParam("term", term)
                                .queryParam("explicit", "Yes")
                                .get("/search");

                response.then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("results.size()", greaterThan(0))
                                .body("results.any { it -> it.trackExplicitness == 'explicit' }", is(true));
        }

        @Test
        public void testSearchForContentInDifferentLanguage() {

                String term = "music";
                String language = "ja_jp";

                Response response = given()
                                .queryParam("term", term)
                                .queryParam("lang", language)
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .body("results.size()", greaterThan(0))
                                .body("results.every { it -> it.country == 'JPN' }", is(true));

        }

        @Test
        public void testPagination() {

                String term = "music";
                int limit = 10;

                Response response = given()
                                .queryParam("term", term)
                                .queryParam("limit", limit)
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(200)
                                .body("resultCount", equalTo(limit))
                                .body("results.size()", equalTo(limit));
        }

        @Test(dataProvider = "variousTermAndLimitScenarios", dataProviderClass = SearchScenariosDataProvider.class)
        public void testSearchVariousScenarios(String scenarioDescription, String term, int limit,
                        int expectedStatusCode,
                        int size) {
                Response response = given()
                                .queryParam("term", term)
                                .queryParam("limit", limit)
                                .get("/search")
                                .then()
                                .log().all()
                                .extract().response();

                response.then()
                                .statusCode(expectedStatusCode)
                                .body("results.size()", equalTo(size));
        }

        @AfterClass
        public void tearDown() {
                extent.flush();
        }

}
