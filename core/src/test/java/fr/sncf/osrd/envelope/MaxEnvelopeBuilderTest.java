package fr.sncf.osrd.envelope;

import static org.junit.jupiter.api.Assertions.*;

import fr.sncf.osrd.envelope.EnvelopeTestUtils.EnvelopeTestMeta;
import org.junit.jupiter.api.Test;

class MaxEnvelopeBuilderTest {
    public static final EnvelopePartMeta meta1 = new EnvelopeTestMeta();
    public static final EnvelopePartMeta meta2 = new EnvelopeTestMeta();

    @Test
    void parabolaCeiling() {
        /*
         *       ,--------,
         * ----+-----------+----------
         *    /             \
         */
        var ep1 = EnvelopePart.generateTimes(
                meta1,
                new double[]{0, 2, 4, 8, 10, 12},
                new double[]{0, 3, 4, 4, 3, 0}
        );
        var ep2 = EnvelopePart.generateTimes(
                meta2,
                new double[]{0, 12},
                new double[]{1.5, 1.5}
        );

        var builder = new MaxEnvelopeBuilder();
        builder.addPart(ep1);
        builder.addPart(ep2);
        var envelope = builder.build();

        assertEquals(3, envelope.size());
        assertEquals(meta2, envelope.get(0).meta);
        assertEquals(2, envelope.get(0).pointCount());
        assertEquals(0.5, envelope.get(0).getPointPos(1), 0.2);
        assertEquals(1.5, envelope.get(0).getPointSpeed(1), 0.2);

        assertEquals(meta1, envelope.get(1).meta);
        assertEquals(6, envelope.get(1).pointCount());

        assertEquals(meta2, envelope.get(2).meta);
        assertEquals(2, envelope.get(2).pointCount());
        assertEquals(11.5, envelope.get(2).getPointPos(0), 0.2);
        assertEquals(1.5, envelope.get(2).getPointSpeed(0), 0.2);
    }

    @Test
    void disjointEnvelopePartsSameOutput() {
        var ep1 = EnvelopePart.generateTimes(
                meta1,
                new double[]{0, 1},
                new double[]{3, 2}
        );
        var ep2 = EnvelopePart.generateTimes(
                meta1,
                new double[]{4, 6},
                new double[]{0, 2}
        );

        var builder = new MaxEnvelopeBuilder();
        builder.addPart(ep1);
        builder.addPart(ep2);
        var envelope = builder.build();

        assertEquals(2, envelope.size());
        assertEquals(ep1, envelope.get(0));
        assertEquals(ep2, envelope.get(1));
    }

    @Test
    void disjointContinuation() {
        /*
         *  simultaneous start and disjoint continuation
         *    /
         *   /
         *  /
         *  ------------
         */

        var ep1 = EnvelopePart.generateTimes(
                meta1,
                new double[]{0, 2},
                new double[]{2, 4}
        );
        var ep2 = EnvelopePart.generateTimes(
                meta2,
                new double[]{0, 8},
                new double[]{1, 1}
        );

        var builder = new MaxEnvelopeBuilder();
        builder.addPart(ep1);
        builder.addPart(ep2);
        var envelope = builder.build();

        assertEquals(2, envelope.size());
        assertEquals(ep1, envelope.get(0));

        var part2Expected = EnvelopePart.generateTimes(
                meta2,
                new double[]{2, 8},
                new double[]{1, 1}
        );
        assertEquals(part2Expected, envelope.get(1));
    }

    @Test
    void crossCommonPoint() {
        /*
         *  simultaneous start and disjoint continuation
         *         /
         *  ------x-----
         *       /
         */

        var ep1 = EnvelopePart.generateTimes(
                meta1,
                new double[]{0, 1, 3},
                new double[]{0, 1, 2}
        );
        var ep2 = EnvelopePart.generateTimes(
                meta2,
                new double[]{0, 1, 3},
                new double[]{1, 1, 1}
        );

        var builder = new MaxEnvelopeBuilder();
        builder.addPart(ep1);
        builder.addPart(ep2);
        var envelope = builder.build();

        assertEquals(2, envelope.size());
        assertEquals(meta2, envelope.get(0).meta);
        assertEquals(2, envelope.get(0).pointCount());

        assertEquals(meta1, envelope.get(1).meta);
        assertEquals(2, envelope.get(1).pointCount());
    }

    @Test
    void envelopeCrossing() {
        /*
         *         /
         *  ------/-----
         *       /
         */

        var ep1 = EnvelopePart.generateTimes(
                meta1,
                new double[]{1, 3},
                new double[]{1, 3}
        );
        var ep2 = EnvelopePart.generateTimes(
                meta2,
                new double[]{0, 5},
                new double[]{2, 2}
        );

        var builder = new MaxEnvelopeBuilder();
        builder.addPart(ep1);
        builder.addPart(ep2);
        var envelope = builder.build();

        assertEquals(3, envelope.size());

        var exectedEp1 = EnvelopePart.generateTimes(
                meta2,
                new double[]{0, 2},
                new double[]{2, 2}
        );
        var exectedEp2 = EnvelopePart.generateTimes(
                meta1,
                new double[]{2, 3},
                new double[]{2, 3}
        );
        var exectedEp3 = EnvelopePart.generateTimes(
                meta2,
                new double[]{3, 5},
                new double[]{2, 2}
        );
        EnvelopeTestUtils.assertEquals(exectedEp1, envelope.get(0), 0.3);
        EnvelopeTestUtils.assertEquals(exectedEp2, envelope.get(1), 0.3);
        EnvelopeTestUtils.assertEquals(exectedEp3, envelope.get(2), 0.3);
    }
}