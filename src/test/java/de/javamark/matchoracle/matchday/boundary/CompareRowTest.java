package de.javamark.matchoracle.matchday.boundary;

import de.javamark.matchoracle.matchday.boundary.MatchdayPageModels.CompareRow;
import de.javamark.matchoracle.matchday.entity.Balance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Story r026: the mirrored bars of the Vereinsvergleich. */
class CompareRowTest {

    @Test
    void theBetterValueOfEachPairIsMarkedAndFewerGoalsAgainstAreBetter() {
        // 4 wins, 1 draw, 3 losses, 18:16 against 5 wins, 3 draws, 0 losses, 27:8
        List<CompareRow> rows = CompareRow.of(new Balance(4, 1, 3, 18, 16), new Balance(5, 3, 0, 27, 8));

        assertEquals(List.of("Punkte", "Siege", "Tore", "Gegentore", "Tordifferenz"), rows.stream().map(CompareRow::label).toList());
        assertEquals("13", rows.get(0).left());
        assertEquals(1, rows.get(0).better());
        CompareRow against = rows.get(3);
        assertEquals(1, against.better());                   // 8 against beats 16
        assertEquals(100, against.leftWidth());
        assertEquals(50, against.rightWidth());
        assertEquals("+2", rows.get(4).left());
        assertEquals("+19", rows.get(4).right());
    }

    @Test
    void equalValuesMarkNeitherSideAndZeroDrawsNoBar() {
        CompareRow draw = CompareRow.of("Siege", 0, 0, false, false);
        assertEquals(0, draw.better());
        assertEquals(0, draw.leftWidth());
        assertEquals("0", draw.right());

        CompareRow negative = CompareRow.of("Tordifferenz", -6, 3, false, true);
        assertEquals("−6", negative.left());
        assertEquals(100, negative.leftWidth());
        assertEquals(1, negative.better());
    }
}
