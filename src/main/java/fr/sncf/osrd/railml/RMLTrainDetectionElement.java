package fr.sncf.osrd.railml;

import fr.sncf.osrd.infra.InvalidInfraException;
import fr.sncf.osrd.infra.railjson.schema.RJSTrackSection;
import fr.sncf.osrd.infra.railjson.schema.trackobjects.RJSTrainDetector;
import fr.sncf.osrd.utils.graph.ApplicableDirections;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.Map;

public class RMLTrainDetectionElement {
    static void parse(
            Map<String, NetElement> netElements,
            Document document,
            HashMap<String, RJSTrackSection> rjsTrackSections
    ) throws InvalidInfraException {
        var xpath = "/railML/infrastructure/functionalInfrastructure/trainDetectionElements/trainDetectionElement";
        for (var trainDetectionElementNode : document.selectNodes(xpath)) {
            var trainDetectionElement = (Element) trainDetectionElementNode;
            // locate the track netElement the buffer stop is on
            var id = trainDetectionElement.attributeValue("id");

            // parse the location, which should only reference a single element
            var location = SpotLocation.parseSingle(netElements, trainDetectionElement);
            if (location == null)
                throw new InvalidInfraException(String.format("missing spotLocation on trainDetectionElement %s", id));

            // TODO: support directional train detection elements
            if (location.appliesTo != ApplicableDirections.BOTH)
                throw new InvalidInfraException("directional train detection elements aren't supported");

            // add the buffer stop to the RJSTrackSection
            var rjsTrackSection = rjsTrackSections.get(location.netElement.id);
            rjsTrackSection.trainDetectors.add(new RJSTrainDetector(id, location.position));
        }
    }
}
