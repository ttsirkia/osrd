package fr.sncf.osrd.envelope_sim;

import com.google.common.collect.RangeMap;
import fr.sncf.osrd.train.RollingStock;
import fr.sncf.osrd.train.RollingStock.Comfort;

public class EnvelopeSimContext {
    public final PhysicsRollingStock rollingStock;
    public final PhysicsPath path;
    public final double timeStep;
    public final RangeMap<Double, PhysicsRollingStock.TractiveEffortPoint[]> tractiveEffortCurveMap;
    public final RangeMap<Double, PhysicsPath.ModeAndProfile> modeAndProfilesUsed;

    /** Creates a context suitable to run simulations on envelopes */
    public EnvelopeSimContext(
            PhysicsRollingStock rollingStock,
            PhysicsPath path,
            double timeStep,
            RollingStock.Comfort comfort
    ) {
        this.rollingStock = rollingStock;
        this.path = path;
        this.timeStep = timeStep;
        var curvesAndConditions = rollingStock.mapTractiveEffortCurves(path, comfort);
        this.tractiveEffortCurveMap = curvesAndConditions.curves();
        this.modeAndProfilesUsed = curvesAndConditions.conditions();
    }
}
