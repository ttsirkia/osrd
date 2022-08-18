package fr.sncf.osrd.train;

import com.squareup.moshi.Json;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fr.sncf.osrd.envelope_sim.PhysicsRollingStock;
import fr.sncf.osrd.railjson.schema.rollingstock.RJSLoadingGaugeType;
import java.util.Collection;
import java.util.HashMap;


/**
 * The immutable characteristics of a specific train.
 * There must be a RollingStock instance per train on the network.
 */
@SuppressFBWarnings({"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public class RollingStock implements PhysicsRollingStock {
    public final String id;

    public final double A; // in newtons
    public final double B; // in newtons / (m/s)
    public final double C; // in newtons / (m/s^2)

    /** the kind of deceleration input of the train. It can be:
     * a constant value
     * the maximum possible deceleration value */
    public final RollingStock.GammaType gammaType;

    /** the deceleration of the train, in m/s² */
    public final double gamma;

    /** A_brake_emergency: the emergency braking decelerations for ERTMS ETCS 2, in m/s² */
    public final HashMap<Double, Double> gammaBrakeEmergency;

    /** A_brake_service: the service braking decelerations for ERTMS ETCS 2, in m/s² */
    public final HashMap<Double, Double> gammaBrakeService;

    /** A_brake_normal_service: the service braking decelerations used to compute guidance curve in ETCS 2, in m/s² */
    public final HashMap<Double, Double> gammaBrakeNormalService;

    /** Kdry_rst: the rolling stock deceleration correction factors for dry rails, used in ETCS 2 */
    public final HashMap<Double, Double> kDry;

    /** Kwet_rst: the rolling stock deceleration correction factors for wet rails, used in ETCS 2 */
    public final HashMap<Double, Double> kWet;

    /** the length of the train, in meters. */
    public final double length;

    /** The max speed of the train, in meters per seconds. */
    public final double maxSpeed;

    /**
     * The time the train takes to start up, in seconds.
     * During this time, the train's maximum acceleration is limited.
     */
    public final double startUpTime;

    /** The acceleration to apply during the startup state. */
    public final double startUpAcceleration;

    /** The maximum acceleration when the train is in its regular operating mode. */
    public final double comfortAcceleration;

    /** The mass of the train, in kilograms. */
    public final double mass;

    /** Defined as mass * inertiaCoefficient */
    public final double inertia;

    /**
     * Inertia coefficient.
     * The mass alone isn't sufficient to compute accelerations, as the wheels and internals
     * also need force to get spinning. This coefficient can be used to account for the difference.
     * It's without unit.
     */
    public final double inertiaCoefficient;

    /**
     * Associates a speed to a force.
     * https://en.wikipedia.org/wiki/Tractive_force#Tractive_effort_curves
     */
    public final TractiveEffortPoint[] tractiveEffortCurve;

    public final RJSLoadingGaugeType loadingGaugeType;

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getInertia() {
        return inertia;
    }

    @Override
    public double getLength() {
        return length;
    }

    @Override
    public double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * Gets the rolling resistance at a given speed, which is a force that always goes
     * opposite to the train's movement direction
     */
    @Override
    public double getRollingResistance(double speed) {
        speed = Math.abs(speed);
        // this formula is called the Davis equation.
        // it's completely empirical, and models the drag and friction forces
        return A + B * speed + C * speed * speed;
    }

    @Override
    public double getRollingResistanceDeriv(double speed) {
        speed = Math.abs(speed);
        return B + 2 * C * speed;
    }

    @Override
    public double getMaxBrakingForce(double speed) {
        return gamma * inertia;
    }

    @Override
    public double getEmergencyBrakingForce(double speed) {
        // TODO : build a Sorted Double Map and call it here
        return gammaBrakeEmergency * inertia;
    }

    @Override
    public GammaType getGammaType() {
        return gammaType;
    }

    public enum GammaType {
        CONST,
        MAX
    }

    public static final class TractiveEffortPoint {
        public final double speed;
        public final double maxEffort;

        public TractiveEffortPoint(double speed, double maxEffort) {
            this.speed = speed;
            this.maxEffort = maxEffort;
        }
    }

    /** Returns Gamma */
    public double getDeceleration() {
        return - gamma;
    }

    /**
     * Returns the max tractive effort at a given speed.
     * @param speed the speed to compute the max tractive effort for
     * @return the max tractive effort
     */
    public double getMaxEffort(double speed) {
        double previousEffort = 0.0;
        double previousSpeed = 0.0;
        for (var dataPoint : tractiveEffortCurve) {
            if (previousSpeed <= Math.abs(speed) && Math.abs(speed) < dataPoint.speed) {
                var coeff = (previousEffort - dataPoint.maxEffort) / (previousSpeed - dataPoint.speed);
                return previousEffort + coeff * (Math.abs(speed) - previousSpeed);
            }
            previousEffort = dataPoint.maxEffort;
            previousSpeed = dataPoint.speed;
        }
        return previousEffort;
    }

    // TODO masses

    /** Creates a new rolling stock (a physical train inventory item). */
    public RollingStock(
            String id,
            double length,
            double mass,
            double inertiaCoefficient,
            double a,
            double b,
            double c,
            double maxSpeed,
            double startUpTime,
            double startUpAcceleration,
            double comfortAcceleration,
            double gamma,
            GammaType gammaType,
            TractiveEffortPoint[] tractiveEffortCurve,
            RJSLoadingGaugeType loadingGaugeType
    ) {
        this.id = id;
        this.A = a;
        this.B = b;
        this.C = c;
        this.length = length;
        this.maxSpeed = maxSpeed;
        this.startUpTime = startUpTime;
        this.startUpAcceleration = startUpAcceleration;
        this.comfortAcceleration = comfortAcceleration;
        this.gamma = gamma;
        this.gammaType = gammaType;
        this.gammaBrakeEmergency = null;
        this.gammaBrakeService = null;
        this.gammaBrakeNormalService = null;
        this.kDry = null;
        this.kWet = null;
        this.mass = mass;
        this.inertiaCoefficient = inertiaCoefficient;
        this.tractiveEffortCurve = tractiveEffortCurve;
        this.inertia = mass * inertiaCoefficient;
        this.loadingGaugeType = loadingGaugeType;
    }

    /** Creates a new rolling stock adapted to ERTMS ETCS 2. */
    public RollingStock(
            String id,
            double length,
            double mass,
            double inertiaCoefficient,
            double a,
            double b,
            double c,
            double maxSpeed,
            double startUpTime,
            double startUpAcceleration,
            double comfortAcceleration,
            double gamma,
            GammaType gammaType,
            double[] gammaBrakeEmergency,
            double[] gammaBrakeService,
            double[] gammaBrakeNormalService,
            double[] kDry,
            double[] kWet,
            TractiveEffortPoint[] tractiveEffortCurve,
            RJSLoadingGaugeType loadingGaugeType
    ) {
        this.id = id;
        this.A = a;
        this.B = b;
        this.C = c;
        this.length = length;
        this.maxSpeed = maxSpeed;
        this.startUpTime = startUpTime;
        this.startUpAcceleration = startUpAcceleration;
        this.comfortAcceleration = comfortAcceleration;
        this.gamma = gamma;
        this.gammaType = gammaType;
        this.gammaBrakeEmergency = gammaBrakeEmergency;
        this.gammaBrakeService = gammaBrakeService;
        this.gammaBrakeNormalService = gammaBrakeNormalService;
        this.kDry = kDry;
        this.kWet = kWet;
        this.mass = mass;
        this.inertiaCoefficient = inertiaCoefficient;
        this.tractiveEffortCurve = tractiveEffortCurve;
        this.inertia = mass * inertiaCoefficient;
        this.loadingGaugeType = loadingGaugeType;
    }
}
