package fr.sncf.osrd.external_generated_inputs;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import fr.sncf.osrd.railjson.schema.external_generated_inputs.RJSElectricalProfile;
import fr.sncf.osrd.railjson.schema.infra.trackranges.RJSTrackRange;
import java.util.List;


public class ExternalGeneratedInputsHelpers {
    public static List<RJSElectricalProfile> getRjsElectricalProfiles() {
        return asList(new RJSElectricalProfile("25000", "1",
                        asList(new RJSTrackRange("track", 0, 10), new RJSTrackRange("track", 90, 100))),
                new RJSElectricalProfile("22500", "1",
                        asList(new RJSTrackRange("track", 10, 30), new RJSTrackRange("track", 70, 90))),
                new RJSElectricalProfile("20000", "1", singletonList(new RJSTrackRange("track", 30, 70))));
    }
}
