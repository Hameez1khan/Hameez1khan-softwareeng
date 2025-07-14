package org.openmetromaps.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmetromaps.maps.model.Coordinate;
import org.openmetromaps.maps.model.Line;
import org.openmetromaps.maps.model.ModelData;
import org.openmetromaps.maps.model.Station;
import org.openmetromaps.maps.model.Stop;

import com.google.common.collect.Lists;

public class ReplacementServicesUnitTests {
    ModelData model;

    Station stationA;
    Station stationB;
    Station stationC;

    Line line1;

    /*
     * 1 X----X----X
     *   A    B    C
     */
    @Before
    public void createMap() {
        // Arrange

        List<Stop> stationAStops = new ArrayList<>();
        stationA = new Station(0, "A", new Coordinate(47.4891, 19.0614), stationAStops);

        List<Stop> stationBStops = new ArrayList<>();
        stationB = new Station(1, "B", new Coordinate(47.4891, 19.0714), stationBStops);

        List<Stop> stationCStops = new ArrayList<>();
        stationC = new Station(2, "C", new Coordinate(47.4891, 19.0814), stationCStops);

        List<Stop> line1Stops = new ArrayList<>();
        line1 = new Line(3, "1", "#009EE3", false, line1Stops);

        Stop line1AStop = new Stop(stationA, line1);
        stationAStops.add(line1AStop);
        line1Stops.add(line1AStop);

        Stop line1BStop = new Stop(stationB, line1);
        stationBStops.add(line1BStop);
        line1Stops.add(line1BStop);

        Stop line1CStop = new Stop(stationC, line1);
        stationCStops.add(line1CStop);
        line1Stops.add(line1CStop);

        model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC)));
    }

    /*
     * Starting from:
     *
     * 1 X----X----X
     *   A    B    C
     *
     * CLOSE B:
     *
     * 1 X---------X
     *   A         C
     */
    @Test
    public void checkCloseStation() {
        // Act
        ReplacementServices.closeStation(model, stationB, List.of(line1));

        // Assert
        assertModel(model)
            .hasLines(1)
            .hasExactStations("A", "C")
            .hasLineWithExactStations("1", "A", "C");
    }


    @Test
    public void testCloseStationWithDifferentLineConfigurations() {
        /*
         * Initial Setup (Scenario 1):
         * Line 1: A-B
         * Attempt to close station B (present in the line):
         * Expected Result: No changes, as Line 1 would be invalid with less than two stops.
         *
         * Scenario 2 Setup:
         * Line 1: A-B-C
         * Attempt to close station D (not present in the line):
         * Expected Result: No changes, as station D is not in Line 1.
         */
    
        // Arrange: Set up ModelData with a single line having only two stops
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
    
        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB = new Stop(stationB, line1);
        line1.getStops().addAll(List.of(stopA, stopB));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
    
        ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB)));
    
        // Act: Scenario 1 - Attempt to close station B
        ReplacementServices.closeStation(model, stationB, List.of(line1));
    
        // Assert: Scenario 1 - No changes should occur
        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B") // Both stations remain
            .hasLineWithExactStations("1", "A", "B"); // Line 1 unchanged
    
        // Add a new station to Line 1
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
        Stop stopC = new Stop(stationC, line1);
        line1.getStops().add(stopC);
        stationC.getStops().add(stopC);
        model.stations.add(stationC);
    
        // Act: Scenario 2 - Attempt to close station D (not present in Line 1)
        Station stationD = new Station(3, "D", new Coordinate(47.50, 19.09), new ArrayList<>()); // Station not in Line 1
        ReplacementServices.closeStation(model, stationD, List.of(line1));
    
        // Assert: Scenario 2 - No changes should occur
        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C") // Stations A, B, and C remain; D is not added
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged
    }
    



    @Test
    public void closeStationAndRemoveFromModel() {
        /*
        * Initial Setup:
        *
        * Line 1: A-B-C
        * Line 2: B-D-E
        *
        * Expected After Closing B:
        * 
        * Line 1: A-C
        * Line 2: D-E
        *
        * Station B should be removed from the model.
        */

        // Arrange: Set up ModelData with two lines and shared station B
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
        Station stationD = new Station(3, "D", new Coordinate(47.50, 19.07), new ArrayList<>());
        Station stationE = new Station(4, "E", new Coordinate(47.50, 19.08), new ArrayList<>());

        // Line 1: A-B-C
        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB1 = new Stop(stationB, line1);
        Stop stopC = new Stop(stationC, line1);
        line1.getStops().addAll(List.of(stopA, stopB1, stopC));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB1);
        stationC.getStops().add(stopC);

        // Line 2: B-D-E
        Line line2 = new Line(1, "2", "#FF5733", false, new ArrayList<>());
        Stop stopB2 = new Stop(stationB, line2);
        Stop stopD = new Stop(stationD, line2);
        Stop stopE = new Stop(stationE, line2);
        line2.getStops().addAll(List.of(stopB2, stopD, stopE));
        stationB.getStops().add(stopB2);
        stationD.getStops().add(stopD);
        stationE.getStops().add(stopE);

        // ModelData with Line 1 and Line 2
        ModelData model = new ModelData(new ArrayList<>(List.of(line1, line2)), new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationE)));

        // Act: Close station B on both lines
        ReplacementServices.closeStation(model, stationB, List.of(line1, line2));

        // Assert: Verify the station and lines after operation
        assertModel(model)
            .hasLines(2) // Both lines should still exist
            .hasExactStations("A", "C", "D", "E") // Station B should be removed
            .hasLineWithExactStations("1", "A", "C") // Line 1 updated
            .hasLineWithExactStations("2", "D", "E"); // Line 2 updated
    }




    /*
     * Starting from:
     *
     * 1 X----X----X
     *   A    B    C
     *
     * ALTERNATIVE A-C:
     *
     * P-1  ---------
     *     |         |
     * 1   X----X----X
     *     A    B    C
     */
    @Test
    public void checkAlternativeService() {
        // Act
        ReplacementServices.createAlternativeService(model, stationA, stationC);

        // Assert
        assertModel(model)
            .hasLines(2)
            .hasExactStations("A", "B", "C")
            .hasLineWithExactStations("1", "A", "B", "C")
            .hasLineWithExactStations("P-1", "A", "C");
    }


    @Test
    public void testCreateAlternativeServiceWithInvalidStations() {
        /*
        * Initial Setup:
        *
        * Line 1: A-B-C
        *
        * Invalid Input Cases:
        * Case 1: Same station provided (A, A).
        * Case 2: One station is null (A, null).
        *
        * Expected Result:
        * - No replacement line should be created.
        * - The model should remain unchanged.
        */

        // Arrange: Set up ModelData with a single line
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());

        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB = new Stop(stationB, line1);
        Stop stopC = new Stop(stationC, line1);
        line1.getStops().addAll(List.of(stopA, stopB, stopC));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
        stationC.getStops().add(stopC);

        ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC)));

        // Act: Attempt to create an alternative service with invalid inputs
        ReplacementServices.createAlternativeService(model, stationA, stationA); // Same stations
        ReplacementServices.createAlternativeService(model, stationA, null);     // One station is null

        // Assert: Ensure the model remains unchanged
        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged
    }





    /*
     * Starting from:
     *
     * 1 X----X----X
     *   A    B    C
     *
     * REPLACE B-C, 1:
     *
     * 1   X----X
     *     A    B
     *
     * P1       X----X
     *          B    C
     */
    @Test
    public void checkReplacement() {
        // Act
        ReplacementServices.createReplacementService(model, List.of(stationB, stationC), List.of(line1));

        // Assert
        assertModel(model)
            .hasLines(2)
            .hasExactStations("A", "B", "C")
            .hasLineWithExactStations("1", "A", "B")
            .hasLineWithExactStations("P1", "B", "C")
            .hasLine("P1", (line) -> {
                Assert.assertEquals("#009EE3", line.getColor());
                return null;
            });
    }


//------------------------------------------------------------------------------
//NEW ADDED LINES FOR CREATE REPLACEMENT SERVICE

    @Test
    public void testReplacementServiceInvalidScenarios() {
        /*
        * Initial Setup:
        *
        * Line 1: A-B-C
        *
        * Scenarios:
        * 1. Station list includes a station not in the line (B, D)
        * 2. Station list is empty
        * 3. Line list is empty
        * 4. Station list has fewer than 2 stations (only B)
        *
        * Expected Result:
        * - No replacement line should be created.
        * - Line 1 should remain unchanged.
        * - The model should remain unchanged.
        */

        // Arrange: Set up ModelData with a single line
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
        Station stationD = new Station(3, "D", new Coordinate(47.50, 19.09), new ArrayList<>()); // Not part of Line 1

        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB = new Stop(stationB, line1);
        Stop stopC = new Stop(stationC, line1);
        line1.getStops().addAll(List.of(stopA, stopB, stopC));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
        stationC.getStops().add(stopC);

        ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC, stationD)));

        // Act & Assert: Scenario 1 - Station list includes a station not in the line
        ReplacementServices.createReplacementService(model, List.of(stationB, stationD), List.of(line1));

        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C", "D") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged

        // Act & Assert: Scenario 2 - Empty station list
        ReplacementServices.createReplacementService(model, List.of(), List.of(line1));

        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C", "D") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged

        // Act & Assert: Scenario 3 - Empty line list
        ReplacementServices.createReplacementService(model, List.of(stationB, stationC), List.of());

        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C", "D") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged

        // Act & Assert: Scenario 4 - Station list with fewer than 2 stations
        ReplacementServices.createReplacementService(model, List.of(stationB), List.of(line1));

        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C", "D") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged
    }


    @Test
    public void testReplacementServiceWithNonConsecutiveStations() {
        /*
        * Initial Setup:
        *
        * Line 1: A-B-C
        *
        * Scenarios:
        * 1. Non-consecutive stations selected (A, C)
        * 2. Non-consecutive stations in reverse order (C, A)
        *
        * Expected Result:
        * - No replacement line should be created in either case.
        * - Line 1 should remain unchanged.
        */

        // Arrange: Set up ModelData with a single line
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());

        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB = new Stop(stationB, line1);
        Stop stopC = new Stop(stationC, line1);
        line1.getStops().addAll(List.of(stopA, stopB, stopC));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
        stationC.getStops().add(stopC);

        ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC)));

        // Act & Assert: Scenario 1 - Non-consecutive stations (A, C)
        ReplacementServices.createReplacementService(model, List.of(stationA, stationC), List.of(line1));

        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged

        // Act & Assert: Scenario 2 - Non-consecutive stations in reverse order (C, A)
        ReplacementServices.createReplacementService(model, List.of(stationC, stationA), List.of(line1));

        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged
    }



    @Test
    public void testReplacementServiceWithTerminalStations() {
        /*
        * Initial Setup:
        *
        * Line 1: A-B-C
        *
        * Attempt to create a replacement service with stations A and C:
        * - Both stations are terminal stations in Line 1.
        *
        * Expected Result:
        * - No replacement line should be created.
        * - Line 1 should remain unchanged.
        */

        // Arrange: Set up ModelData with a single line
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());

        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB = new Stop(stationB, line1);
        Stop stopC = new Stop(stationC, line1);
        line1.getStops().addAll(List.of(stopA, stopB, stopC));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
        stationC.getStops().add(stopC);

        ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC)));

        // Act: Attempt to create a replacement service with terminal stations A and C
        ReplacementServices.createReplacementService(model, List.of(stationA, stationB, stationC), List.of(line1));

        // Assert: Ensure the model remains unchanged
        assertModel(model)
            .hasLines(1) // Only Line 1 should exist
            .hasExactStations("A", "B", "C") // All original stations remain
            .hasLineWithExactStations("1", "A", "B", "C"); // Line 1 unchanged
    }


    @Test
    public void testReplacementServiceWithConsecutiveStationsSplittingLine() {
        /*
         * Initial Setup:
         *
         * Line 1: A-B-C-D-E-F-G
         *
         * Attempt to create a replacement service with stations D, E, F:
         * - The selected stations are consecutive and non-terminal.
         *
         * Expected Result:
         * - Three lines are created:
         *   1. Original Line Split 1 (<originallinename>-1): A-B-C-D
         *   2. Replacement Line (P<originallinename>): D-E-F
         *   3. Original Line Split 2 (<originallinename>-2): F-G
         */
    
        // Arrange: Set up ModelData with a single line
        Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
        Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
        Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
        Station stationD = new Station(3, "D", new Coordinate(47.50, 19.06), new ArrayList<>());
        Station stationE = new Station(4, "E", new Coordinate(47.50, 19.07), new ArrayList<>());
        Station stationF = new Station(5, "F", new Coordinate(47.50, 19.08), new ArrayList<>());
        Station stationG = new Station(6, "G", new Coordinate(47.51, 19.08), new ArrayList<>());
    
        Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
        Stop stopA = new Stop(stationA, line1);
        Stop stopB = new Stop(stationB, line1);
        Stop stopC = new Stop(stationC, line1);
        Stop stopD = new Stop(stationD, line1);
        Stop stopE = new Stop(stationE, line1);
        Stop stopF = new Stop(stationF, line1);
        Stop stopG = new Stop(stationG, line1);
    
        line1.getStops().addAll(List.of(stopA, stopB, stopC, stopD, stopE, stopF, stopG));
        stationA.getStops().add(stopA);
        stationB.getStops().add(stopB);
        stationC.getStops().add(stopC);
        stationD.getStops().add(stopD);
        stationE.getStops().add(stopE);
        stationF.getStops().add(stopF);
        stationG.getStops().add(stopG);
    
        ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG)));
    
        // Act: Create a replacement service with consecutive stations D, E, F
        ReplacementServices.createReplacementService(model, List.of(stationD, stationE, stationF), List.of(line1));
    
        // Assert: Ensure the lines are split correctly and the replacement line is created
        assertModel(model)
            .hasLines(3) // Three lines should exist
            .hasExactStations("A", "B", "C", "D", "E", "F", "G") // All original stations remain
            .hasLineWithExactStations("1-1", "A", "B", "C", "D") // First part of the original line
            .hasLineWithExactStations("P1", "D", "E", "F") // Replacement line
            .hasLineWithExactStations("1-2", "F", "G"); // Remaining part of the original line
    }
    


@Test
public void testReplacementServiceAcrossMultipleLines() {
    /*
     * Initial Setup:
     *
     * Line 1: A-B-C-D-E-F-G
     * Line 2: H-I-D-E-F-J
     *
     * Attempt to create a replacement service with stations D, E, F across Line 1 and Line 2.
     * - The selected stations are consecutive in both lines.
     *
     * Expected Result:
     * - Three lines are created per original line:
     *   1. Line 1-1: A-B-C-D
     *   2. Replacement Line (P-1): D-E-F (Line 1 replacement)
     *   3. Line 1-2: F-G
     *   4. Line 2-1: H-I-D
     *   5. Replacement Line (P-2): D-E-F (Line 2 replacement)
     *   6. Line 2-2: F-J
     */

    // Arrange: Set up ModelData with two lines
    Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
    Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
    Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
    Station stationD = new Station(3, "D", new Coordinate(47.50, 19.06), new ArrayList<>());
    Station stationE = new Station(4, "E", new Coordinate(47.50, 19.07), new ArrayList<>());
    Station stationF = new Station(5, "F", new Coordinate(47.50, 19.08), new ArrayList<>());
    Station stationG = new Station(6, "G", new Coordinate(47.51, 19.08), new ArrayList<>());
    Station stationH = new Station(7, "H", new Coordinate(47.52, 19.06), new ArrayList<>());
    Station stationI = new Station(8, "I", new Coordinate(47.52, 19.07), new ArrayList<>());
    Station stationJ = new Station(9, "J", new Coordinate(47.52, 19.08), new ArrayList<>());

    // Line 1: A-B-C-D-E-F-G
    Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
    line1.getStops().addAll(List.of(
        new Stop(stationA, line1),
        new Stop(stationB, line1),
        new Stop(stationC, line1),
        new Stop(stationD, line1),
        new Stop(stationE, line1),
        new Stop(stationF, line1),
        new Stop(stationG, line1)
    ));

    // Line 2: H-I-D-E-F-J
    Line line2 = new Line(1, "2", "#FF5733", false, new ArrayList<>());
    line2.getStops().addAll(List.of(
        new Stop(stationH, line2),
        new Stop(stationI, line2),
        new Stop(stationD, line2),
        new Stop(stationE, line2),
        new Stop(stationF, line2),
        new Stop(stationJ, line2)
    ));

    // Add stations to stops
    stationA.getStops().add(new Stop(stationA, line1));
    stationB.getStops().add(new Stop(stationB, line1));
    stationC.getStops().add(new Stop(stationC, line1));
    stationD.getStops().add(new Stop(stationD, line1));
    stationE.getStops().add(new Stop(stationE, line1));
    stationF.getStops().add(new Stop(stationF, line1));
    stationG.getStops().add(new Stop(stationG, line1));
    stationH.getStops().add(new Stop(stationH, line2));
    stationI.getStops().add(new Stop(stationI, line2));
    stationJ.getStops().add(new Stop(stationJ, line2));

    ModelData model = new ModelData(
        new ArrayList<>(List.of(line1, line2)),
        new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH, stationI, stationJ))
    );

    // Act: Create a replacement service with stations D, E, F across Line 1 and Line 2
    ReplacementServices.createReplacementService(model, List.of(stationD, stationE, stationF), List.of(line1, line2));

    // Assert: Ensure the lines are split correctly and the replacement lines are created with appropriate names
    assertModel(model)
        .hasLines(5) // Six lines should exist
        .hasExactStations("A", "B", "C", "D", "E", "F", "G", "H", "I", "J") // All original stations remain
        .hasLineWithExactStations("1-1", "A", "B", "C", "D") // First part of Line 1
        .hasLineWithExactStations("P-1", "D", "E", "F") // Replacement line for Line 1
        .hasLineWithExactStations("1-2", "F", "G") // Remaining part of Line 1
        .hasLineWithExactStations("2-1", "H", "I", "D") // First part of Line 2
        .hasLineWithExactStations("2-2", "F", "J"); // Remaining part of Line 2
}



@Test
public void testReplacementServiceWithConsecutiveStationsInReverseOrder() {
    /*
     * Initial Setup:
     *
     * Line 1: A-B-C-D-E-F-G
     *
     * Attempt to create a replacement service with stations F, E, D (in reverse order):
     * - The selected stations are consecutive.
     *
     * Expected Result:
     * - Three lines are created:
     *   1. Line 1-2: A-B-C-D
     *   2. Replacement Line (P1): D-E-F
     *   3. Line 1-1: F-G
     */

    // Arrange: Set up ModelData with a single line
    Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
    Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
    Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
    Station stationD = new Station(3, "D", new Coordinate(47.50, 19.06), new ArrayList<>());
    Station stationE = new Station(4, "E", new Coordinate(47.50, 19.07), new ArrayList<>());
    Station stationF = new Station(5, "F", new Coordinate(47.50, 19.08), new ArrayList<>());
    Station stationG = new Station(6, "G", new Coordinate(47.51, 19.08), new ArrayList<>());

    Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
    Stop stopA = new Stop(stationA, line1);
    Stop stopB = new Stop(stationB, line1);
    Stop stopC = new Stop(stationC, line1);
    Stop stopD = new Stop(stationD, line1);
    Stop stopE = new Stop(stationE, line1);
    Stop stopF = new Stop(stationF, line1);
    Stop stopG = new Stop(stationG, line1);

    line1.getStops().addAll(List.of(stopA, stopB, stopC, stopD, stopE, stopF, stopG));
    stationA.getStops().add(stopA);
    stationB.getStops().add(stopB);
    stationC.getStops().add(stopC);
    stationD.getStops().add(stopD);
    stationE.getStops().add(stopE);
    stationF.getStops().add(stopF);
    stationG.getStops().add(stopG);

    ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG)));

    // Act: Create a replacement service with stations F, E, D (in reverse order)
    ReplacementServices.createReplacementService(model, List.of(stationF, stationE, stationD), List.of(line1));

    // Assert: Ensure the lines are split correctly and the replacement line is created
    assertModel(model)
        .hasLines(3) // Three lines should exist
        .hasExactStations("A", "B", "C", "D", "E", "F", "G") // All original stations remain
        .hasLineWithExactStations("1-2", "A", "B", "C", "D") // First part of Line 1
        .hasLineWithExactStations("P1", "D", "E", "F") // Replacement line
        .hasLineWithExactStations("1-1", "F", "G"); // Remaining part of Line 1
}

@Test
public void testReplacementServiceWithTerminalStation() {
    /*
     * Initial Setup:
     *
     * Line 1: A-B-C-D-E-F-G
     *
     * Attempt to create a replacement service with stations D, E, F, G:
     * - The selected stations are consecutive.
     * - G is a terminal station (last stop of the line).
     *
     * Expected Result:
     * - Two lines are created:
     *   1. Line 1: A-B-C-D
     *   2. Replacement Line (P1): D-E-F-G
     */

    // Arrange: Set up ModelData with a single line
    Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
    Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
    Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
    Station stationD = new Station(3, "D", new Coordinate(47.50, 19.06), new ArrayList<>());
    Station stationE = new Station(4, "E", new Coordinate(47.50, 19.07), new ArrayList<>());
    Station stationF = new Station(5, "F", new Coordinate(47.50, 19.08), new ArrayList<>());
    Station stationG = new Station(6, "G", new Coordinate(47.51, 19.08), new ArrayList<>());

    Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
    Stop stopA = new Stop(stationA, line1);
    Stop stopB = new Stop(stationB, line1);
    Stop stopC = new Stop(stationC, line1);
    Stop stopD = new Stop(stationD, line1);
    Stop stopE = new Stop(stationE, line1);
    Stop stopF = new Stop(stationF, line1);
    Stop stopG = new Stop(stationG, line1);

    line1.getStops().addAll(List.of(stopA, stopB, stopC, stopD, stopE, stopF, stopG));
    stationA.getStops().add(stopA);
    stationB.getStops().add(stopB);
    stationC.getStops().add(stopC);
    stationD.getStops().add(stopD);
    stationE.getStops().add(stopE);
    stationF.getStops().add(stopF);
    stationG.getStops().add(stopG);

    ModelData model = new ModelData(new ArrayList<>(List.of(line1)), new ArrayList<>(List.of(stationA, stationB, stationC, stationD, stationE, stationF, stationG)));

    // Act: Create a replacement service with stations D, E, F, G
    ReplacementServices.createReplacementService(model, List.of(stationD, stationE, stationF, stationG), List.of(line1));

    // Assert: Ensure the lines are split correctly and the replacement line is created
    assertModel(model)
        .hasLines(2) // Two lines should exist
        .hasExactStations("A", "B", "C", "D", "E", "F", "G") // All original stations remain
        .hasLineWithExactStations("1", "A", "B", "C", "D") // Original line ending at D
        .hasLineWithExactStations("P1", "D", "E", "F", "G"); // Replacement line from D to G
}

@Test
public void testReplacementServiceWithTerminalStationAcrossMultipleLines() {
    /*
     * Initial Setup:
     *
     * Line 1: A-B-C-D-E-F-G
     * Line 2: H-I-D-E-F-G
     *
     * Attempt to create a replacement service with stations D, E, F, G:
     * - The selected stations are consecutive.
     * - G is a terminal station (last stop of both lines).
     *
     * Expected Result:
     * - Two lines are created for each original line:
     *   1. Line 1: A-B-C-D
     *   2. Replacement Line (P-1): D-E-F-G
     *   3. Line 2: H-I-D
     *   4. Replacement Line (P-2): D-E-F-G
     */

    // Arrange: Set up ModelData with two lines
    Station stationA = new Station(0, "A", new Coordinate(47.49, 19.06), new ArrayList<>());
    Station stationB = new Station(1, "B", new Coordinate(47.49, 19.07), new ArrayList<>());
    Station stationC = new Station(2, "C", new Coordinate(47.49, 19.08), new ArrayList<>());
    Station stationD = new Station(3, "D", new Coordinate(47.50, 19.06), new ArrayList<>());
    Station stationE = new Station(4, "E", new Coordinate(47.50, 19.07), new ArrayList<>());
    Station stationF = new Station(5, "F", new Coordinate(47.50, 19.08), new ArrayList<>());
    Station stationG = new Station(6, "G", new Coordinate(47.51, 19.08), new ArrayList<>());
    Station stationH = new Station(7, "H", new Coordinate(47.52, 19.06), new ArrayList<>());
    Station stationI = new Station(8, "I", new Coordinate(47.52, 19.07), new ArrayList<>());

    // Line 1: A-B-C-D-E-F-G
    Line line1 = new Line(0, "1", "#009EE3", false, new ArrayList<>());
    line1.getStops().addAll(List.of(
        new Stop(stationA, line1),
        new Stop(stationB, line1),
        new Stop(stationC, line1),
        new Stop(stationD, line1),
        new Stop(stationE, line1),
        new Stop(stationF, line1),
        new Stop(stationG, line1)
    ));

    // Line 2: H-I-D-E-F-G
    Line line2 = new Line(1, "2", "#FF5733", false, new ArrayList<>());
    line2.getStops().addAll(List.of(
        new Stop(stationH, line2),
        new Stop(stationI, line2),
        new Stop(stationD, line2),
        new Stop(stationE, line2),
        new Stop(stationF, line2),
        new Stop(stationG, line2)
    ));

    // Add stations to stops
    stationA.getStops().add(new Stop(stationA, line1));
    stationB.getStops().add(new Stop(stationB, line1));
    stationC.getStops().add(new Stop(stationC, line1));
    stationD.getStops().add(new Stop(stationD, line1));
    stationD.getStops().add(new Stop(stationD, line2));
    stationE.getStops().add(new Stop(stationE, line1));
    stationE.getStops().add(new Stop(stationE, line2));
    stationF.getStops().add(new Stop(stationF, line1));
    stationF.getStops().add(new Stop(stationF, line2));
    stationG.getStops().add(new Stop(stationG, line1));
    stationG.getStops().add(new Stop(stationG, line2));
    stationH.getStops().add(new Stop(stationH, line2));
    stationI.getStops().add(new Stop(stationI, line2));

    ModelData model = new ModelData(new ArrayList<>(List.of(line1, line2)), new ArrayList<>(List.of(
        stationA, stationB, stationC, stationD, stationE, stationF, stationG, stationH, stationI
    )));

    // Act: Create a replacement service with stations D, E, F, G across Line 1 and Line 2
    ReplacementServices.createReplacementService(model, List.of(stationD, stationE, stationF, stationG), List.of(line1, line2));

    // Assert: Ensure the lines are split correctly and the replacement lines are created with appropriate names
    assertModel(model)
        .hasLines(3) // Four lines should exist
        .hasExactStations("A", "B", "C", "D", "E", "F", "G", "H", "I") // All original stations remain
        .hasLineWithExactStations("1", "A", "B", "C", "D") // First part of Line 1
        .hasLineWithExactStations("P-1", "D", "E", "F", "G") // Replacement line for Line 1
        .hasLineWithExactStations("2", "H", "I", "D"); // First part of Line 2
}


//---------------------------------------------------------------------------------------
//=======================================================================================


    private static ModelAsserter assertModel(ModelData data) {
        return new ModelAsserter(data);
    }

    private static class ModelAsserter {
        private final ModelData model;

        private ModelAsserter(ModelData model) {
            this.model = model;
        }

        public ModelAsserter hasStations(int num) {
            Assert.assertEquals("There are more or fewer stations lines than expected", num, model.stations.size());
            return this;
        }

        public ModelAsserter hasExactStations(String... stationNames) {
            assertStationListEquals(getStationsFromNames(stationNames), model.stations);
            return this;
        }

        public ModelAsserter hasStations(String... stationNames) {
            for(Station station : getStationsFromNames(stationNames)) {
                Assert.assertTrue("The station was not present: " + station.getName() + ")", model.stations.contains(station));
            }
            return this;
        }

        public ModelAsserter hasStation(String stationName) {
            hasStations(stationName);
            return this;
        }

        public ModelAsserter hasStation(String stationName, Function<Station, Void> assertStation) {
            hasStations(stationName);
            assertStation.apply(getStationFromName(stationName));
            return this;
        }

        public ModelAsserter hasStationWithExactLines(String stationName, String... lineNames) {
            hasStations(stationName);

            Station station = getStationFromName(stationName);
            List<Line> lines = getLinesFromNames(lineNames);

            assertLineListEquals(lines, station.getStops().stream().map(Stop::getLine).toList());

            return this;
        }

        public ModelAsserter hasStationWithLines(String stationName, String... lineNames) {
            hasStations(stationName);

            Station station = getStationFromName(stationName);
            List<Line> lines = getLinesFromNames(lineNames);
            List<Line> stationLines = station.getStops().stream().map(Stop::getLine).toList();

            for(Line line : lines) {
                Assert.assertTrue(
                    "Line (" + line.getName() + ") does not stop at Station (" + station.getName() + ")",
                    stationLines.contains(line)
                );
            }

            return this;
        }

        public ModelAsserter hasLines(int num) {
            Assert.assertEquals("There are more or fewer lines than expected", num, model.lines.size());
            return this;
        }

        public ModelAsserter hasExactLines(String... lineNames) {
            assertLineListEquals(getLinesFromNames(lineNames), model.lines);
            return this;
        }

        public ModelAsserter hasLines(String... lineNames) {
            for(Line line : getLinesFromNames(lineNames)) {
                Assert.assertTrue("The line was not present (" + line + ")", model.lines.contains(line));
            }
            return this;
        }

        public ModelAsserter hasLine(String lineName) {
            hasLines(lineName);
            return this;
        }

        public ModelAsserter hasLine(String lineName, Function<Line, Void> assertLine) {
            hasLines(lineName);
            assertLine.apply(getLineFromName(lineName));
            return this;
        }

        public ModelAsserter hasLineWithExactStations(String lineName, String... stationNames) {
            hasLines(lineName);

            Line line = getLineFromName(lineName);
            List<Station> stations = getStationsFromNames(stationNames);
            List<Station> lineStations = line.getStops().stream().map(Stop::getStation).toList();

            String expectedStationNames = stations.stream().map(Station::getName).collect(Collectors.joining(", "));
            String actualStationNames = lineStations.stream().map(Station::getName).collect(Collectors.joining(", "));

            Assert.assertTrue(
                "Stations differ on line (" +
                    line.getName() +
                    "). Expected: " +
                    expectedStationNames +
                    ". Actual: "
                    + actualStationNames,
                stations.equals(lineStations) || stations.equals(Lists.reverse(lineStations))
            );

            return this;
        }

        private List<Station> getStationsFromNames(String... stationNames) {
            return getStationsFromNames(List.of(stationNames));
        }

        private List<Station> getStationsFromNames(List<String> stationNames) {
            return stationNames.stream()
                .map(this::getStationFromName)
                .toList();
        }

        private Station getStationFromName(String stationName) {
            return model.stations.stream()
                .filter(s -> s.getName().equals(stationName))
                .findFirst()
                .orElseThrow(() -> {
                    Assert.fail("Station with name is not in the model: " + stationName);
                    return new RuntimeException();
                });
        }

        private void assertStationListEquals(List<Station> expected, List<Station> actual) {
            String expectedStations = expected.stream().map(Station::getName).collect(Collectors.joining(", "));
            String actualStations = actual.stream().map(Station::getName).collect(Collectors.joining(", "));

            Assert.assertEquals("There are more or fewer stations than expected", expected.size(), actual.size());
            Assert.assertTrue(
                "There are stations that were not expected but are present. Expected: " +
                    expectedStations +
                    ". Actual: " +
                    actualStations,
                expected.containsAll(actual)
            );
            Assert.assertTrue(
                "There are stations that were expected but are not present. Expected: " +
                    expectedStations +
                    ". Actual: " +
                    actualStations,
                actual.containsAll(expected)
            );
        }

        private List<Line> getLinesFromNames(String... lineNames) {
            return getLinesFromNames(List.of(lineNames));
        }

        private List<Line> getLinesFromNames(List<String> lineNames) {
            return lineNames.stream()
                    .map(this::getLineFromName)
                    .toList();
        }

        private Line getLineFromName(String lineName) {
            return model.lines.stream()
                .filter(l -> l.getName().equals(lineName))
                .findFirst()
                .orElseThrow(() -> {
                    Assert.fail("Line with name is not in the model: " + lineName);
                    return new RuntimeException();
                });
        }

        private void assertLineListEquals(List<Line> expected, List<Line> actual) {
            String expectedLines = expected.stream().map(Line::getName).collect(Collectors.joining(", "));
            String actualLines = actual.stream().map(Line::getName).collect(Collectors.joining(", "));

            Assert.assertEquals("There are more or fewer lines than expected", expected.size(), actual.size());
            Assert.assertTrue(
                "There are lines that were not expected but are present. Expected: " +
                    expectedLines +
                    ". Actual: " +
                    actualLines,
                expected.containsAll(actual)
            );
            Assert.assertTrue(
                "There are lines that were expected but are not present. Expected: " +
                    expectedLines +
                    ". Actual: " +
                    actualLines,
                actual.containsAll(expected)
            );
        }
    }
}