package fr.sncf.osrd.external_generated_inputs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import fr.sncf.osrd.Helpers;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;


public class ElectricalProfileMappingTest {

    /** Check that a profile map is coherent */
    public static void verifyProfileMap(ElectricalProfileMapping profileMap) {
        assertNotEquals(0, profileMap.mapping.size());
        for (var byTrack : profileMap.mapping.entrySet()) {
            assertNotEquals(0, byTrack.getValue().size());
            for (var byRange : byTrack.getValue().entrySet()) {
                assertNotEquals(0, byRange.getValue().asMapOfRanges().size());
            }
        }
    }

    @Test
    public void testRJSParsing() throws IOException, URISyntaxException {
        var profiles = Helpers.getExampleElectricalProfiles("small_infra/external_generated_inputs.json");
        assert profiles.size() > 0;

        var profileMap = new ElectricalProfileMapping();
        profileMap.parseRJS(profiles);

        verifyProfileMap(profileMap);
        assertEquals(5, profileMap.mapping.size()); // 5 power classes
    }
}
