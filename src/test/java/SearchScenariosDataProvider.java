import org.testng.annotations.DataProvider;

public class SearchScenariosDataProvider {

    @DataProvider(name = "invalidAttributeParameters")
    public static Object[][] invalidAttributeParameters() {
        return new Object[][] {
            {"music", "actorTerm"},
            {"audiobook","softwareDeveloper"}
        };
    } 

    @DataProvider(name = "lookUpIDRequests")
    public static Object[][] lookUpIDRequests() {
        return new Object[][] {
            {"909253","music","musicVideo","id"},
            {"284910350","software","software","id"},
            {"468749","","","amgId"},
            {"468749,5723","","","amgId"},
            {"909253","music","album","id"},
            {"720642462928","music","song","upc"},
            {"15175,15176,15177,15178,15183,15184,15187,1519,15191,15195,15197,15198","","","amgAlbumId"},
            // {"17120","","","amgVideoId"},
            // {"9780316069359","","","isbn"},
        };
    }


    @DataProvider(name = "invalidEntityParameters")
    public static Object[][] invalidEntityParameters() {
        return new Object[][] {
            {"software", "titleTerm"},
            {"shortFilm","audiobookAuthor"}
        };
    }

    @DataProvider(name = "variousTermAndLimitScenarios")
    public static Object[][] variousTermAndLimitScenarios() {
        return new Object[][] {
            { "Valid Term", "music", 10, 200, 10 },
            { "Empty Term", "", 10, 200, 0 },
            { "Maximum Limit", "music", 200, 200, 200 },
            { "Exceeds Limit", "movie", 201, 200, 200 },
            { "Multiple Terms", "jack+johnson movie", 1, 200, 1 },
            { "Term with special characters", "!£$%^&*()_+", 1, 200, 0 }
        };
    }
}
