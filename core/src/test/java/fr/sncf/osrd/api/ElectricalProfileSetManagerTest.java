package fr.sncf.osrd.api;

import fr.sncf.osrd.reporting.warnings.DiagnosticRecorderImpl;
import org.junit.jupiter.api.Test;

import static fr.sncf.osrd.external_generated_inputs.ElectricalProfileMappingTest.verifyProfileMap;

public class ElectricalProfileSetManagerTest extends ApiTest {
    public static void verifyProfileMap(IdentityHashMap<TrackSection, RangeMap<Double, String>> profileMap) {
        assertNotEquals(0, profileMap.size());
        for (var entry : profileMap.entrySet()) {
            var trackSection = entry.getKey();
            var profile = entry.getValue();
            assertNotNull(profile);
            assertNotEquals(0, profile.asMapOfRanges().size());
        }
    }

    @Test
    public void testRJSParsing() throws InterruptedException, IOException, URISyntaxException {
        var infra = infraHandlerMock.load("small_infra/infra.json", null, new DiagnosticRecorderImpl(false));
        var profileMap = new IdentityHashMap<TrackSection, RangeMap<Double, String>>();
        var profiles = Helpers.getExampleElectricalProfiles("small_infra/external_generated_inputs.json");
        assert profiles.size() > 0;

        var manager = new ElectricalProfileSetManager("", "", null);
        manager.parseRJS(profiles, infra, profileMap);

        verifyProfileMap(profileMap);
    }

    @Test
    public void testLoading() throws InterruptedException {
        var infra = infraHandlerMock.load("small_infra/infra.json", null, new DiagnosticRecorderImpl(false));
        var profileMap =
                electricalProfileSetManagerMock.getProfileMap("small_infra/external_generated_inputs.json", infra);

        verifyProfileMap(profileMap);
    }
}
