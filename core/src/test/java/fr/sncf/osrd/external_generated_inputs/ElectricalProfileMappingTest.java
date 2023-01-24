package fr.sncf.osrd.external_generated_inputs;

import static fr.sncf.osrd.Helpers.infraFromRJS;
import static fr.sncf.osrd.external_generated_inputs.ExternalGeneratedInputsHelpers.getRjsElectricalProfiles;
import static fr.sncf.osrd.infra.InfraHelpers.makeSingleTrackRJSInfra;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import fr.sncf.osrd.Helpers;
import fr.sncf.osrd.infra_state.implementation.TrainPathBuilder;
import fr.sncf.osrd.railjson.schema.common.graph.EdgeDirection;
import fr.sncf.osrd.railjson.schema.schedule.RJSTrainPath;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;

public class ElectricalProfileMappingTest {

    /**
     * Check that a profile map is coherent
     */
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
        profileMap.parseRJS(profiles);

        verifyProfileMap(profileMap);
        assertEquals(5, profileMap.mapping.size()); // 5 power classes
    }

    @Test
    public void testGetProfileByPath() {
        var rjsElectricalProfiles = getRjsElectricalProfiles();

        var profileMap = new ElectricalProfileMapping();
        profileMap.parseRJS(rjsElectricalProfiles);

        var rjsInfra = makeSingleTrackRJSInfra();
        var infra = infraFromRJS(rjsInfra);
        var rjsPath = new RJSTrainPath(List.of(new RJSTrainPath.RJSRoutePath("route_forward",
                List.of(new RJSTrainPath.RJSDirectionalTrackRange("track", 20, 80, EdgeDirection.START_TO_STOP)),
                "BAL3")));
        var path = TrainPathBuilder.from(infra, rjsPath);

        var profiles = profileMap.getProfilesOnPath(path, new HashSet<>(asList("1", "2")));
        assertEquals(profiles.keySet(), new HashSet<>(singletonList("1")));
        var profileRangeMap = profiles.get("1");
        assertEquals("22500", profileRangeMap.get(0.));
        assertEquals("22500", profileRangeMap.get(9.5));
        assertEquals("20000", profileRangeMap.get(10.));
        assertEquals("20000", profileRangeMap.get(30.));
        assertEquals("20000", profileRangeMap.get(49.5));
        assertEquals("22500", profileRangeMap.get(50.));
        assertEquals("22500", profileRangeMap.get(59.5));
    }
}
