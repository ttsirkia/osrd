package fr.sncf.osrd.external_generated_inputs;

import fr.sncf.osrd.Helpers;
import fr.sncf.osrd.infra.implementation.tracks.undirected.UndirectedInfraBuilder;
import fr.sncf.osrd.reporting.warnings.DiagnosticRecorderImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ElectricalProfileMappingTest {
    public static void verifyProfileMap(ElectricalProfileMapping profileMap) {
        assertNotEquals(0, profileMap.mapping.size());
        for (var entry : profileMap.mapping.entrySet()) {
            var trackSection = entry.getKey();
            var profile = entry.getValue();
            assertNotNull(profile);
            assertNotEquals(0, profile.asMapOfRanges().size());
        }
    }

    @Test
    public void testRJSParsing() throws InterruptedException, IOException, URISyntaxException {
        var infra = UndirectedInfraBuilder.parseInfra(Helpers.getExampleInfra("small_infra/infra.json"),
                new DiagnosticRecorderImpl(false));
        var profiles = Helpers.getExampleElectricalProfiles("small_infra/external_generated_inputs.json");
        assert profiles.size() > 0;

        var profileMap = new ElectricalProfileMapping();
        profileMap.parseRJS(profiles, infra);

        verifyProfileMap(profileMap);
    }
}
