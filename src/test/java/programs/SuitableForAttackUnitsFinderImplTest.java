package programs;

import com.battle.heroes.army.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для SuitableForAttackUnitsFinderImpl.
 * Проверяем корректность определения доступных для атаки юнитов:
 * - Для левой армии (isLeftArmyTarget=true) - юнит с минимальным y в ряду
 * - Для правой армии (isLeftArmyTarget=false) - юнит с максимальным y в ряду
 */
class SuitableForAttackUnitsFinderImplTest {

    private SuitableForAttackUnitsFinderImpl finder;

    @BeforeEach
    void setUp() {
        finder = new SuitableForAttackUnitsFinderImpl();
    }

    @Test
    @DisplayName("Атака левой армии - выбирает юнитов с минимальным y")
    void getSuitableUnits_leftArmy_shouldSelectMinY() {
        Unit unit1 = createAliveUnit("Unit1", 0, 2); // y = 2
        Unit unit2 = createAliveUnit("Unit2", 0, 0); // y = 0 - должен быть выбран
        Unit unit3 = createAliveUnit("Unit3", 0, 1); // y = 1

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Arrays.asList(unit1, unit2, unit3));

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertEquals(1, result.size());
        assertEquals("Unit2", result.getFirst().getName());
    }

    @Test
    @DisplayName("Атака правой армии - выбирает юнитов с максимальным y")
    void getSuitableUnits_rightArmy_shouldSelectMaxY() {
        Unit unit1 = createAliveUnit("Unit1", 0, 24); // y = 24
        Unit unit2 = createAliveUnit("Unit2", 0, 26); // y = 26 - должен быть выбран
        Unit unit3 = createAliveUnit("Unit3", 0, 25); // y = 25

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Arrays.asList(unit1, unit2, unit3));

        List<Unit> result = finder.getSuitableUnits(unitsByRow, false);

        assertEquals(1, result.size());
        assertEquals("Unit2", result.getFirst().getName());
    }

    @Test
    @DisplayName("Несколько рядов - по одному юниту из каждого")
    void getSuitableUnits_multipleRows_shouldSelectOnePerRow() {
        Unit row0Unit1 = createAliveUnit("Row0_1", 0, 1);
        Unit row0Unit2 = createAliveUnit("Row0_2", 0, 0); // min y

        Unit row1Unit1 = createAliveUnit("Row1_1", 1, 2);
        Unit row1Unit2 = createAliveUnit("Row1_2", 1, 0); // min y

        Unit row2Unit1 = createAliveUnit("Row2_1", 2, 1);
        Unit row2Unit2 = createAliveUnit("Row2_2", 2, 0); // min y

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Arrays.asList(row0Unit1, row0Unit2));
        unitsByRow.add(Arrays.asList(row1Unit1, row1Unit2));
        unitsByRow.add(Arrays.asList(row2Unit1, row2Unit2));

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertEquals(3, result.size());
        assertTrue(result.contains(row0Unit2));
        assertTrue(result.contains(row1Unit2));
        assertTrue(result.contains(row2Unit2));
    }

    @Test
    @DisplayName("Пустой ряд - пропускается")
    void getSuitableUnits_emptyRow_shouldSkip() {
        Unit unit = createAliveUnit("Unit1", 0, 0);

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Collections.emptyList()); // пустой ряд
        unitsByRow.add(Collections.singletonList(unit));
        unitsByRow.add(null); // null ряд

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertEquals(1, result.size());
        assertEquals("Unit1", result.getFirst().getName());
    }

    @Test
    @DisplayName("Мёртвые юниты - игнорируются")
    void getSuitableUnits_deadUnits_shouldIgnore() {
        Unit deadUnit = createDeadUnit("Dead", 0, 0); // y = 0, но мёртв
        Unit aliveUnit = createAliveUnit("Alive", 0, 1); // y = 1, но жив

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Arrays.asList(deadUnit, aliveUnit));

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertEquals(1, result.size());
        assertEquals("Alive", result.getFirst().getName());
    }

    @Test
    @DisplayName("Все юниты в ряду мертвы - ряд не добавляет результат")
    void getSuitableUnits_allDeadInRow_shouldReturnEmpty() {
        Unit dead1 = createDeadUnit("Dead1", 0, 0);
        Unit dead2 = createDeadUnit("Dead2", 0, 1);

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Arrays.asList(dead1, dead2));

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Пустой список рядов - возвращает пустой результат")
    void getSuitableUnits_emptyInput_shouldReturnEmpty() {
        List<List<Unit>> unitsByRow = new ArrayList<>();

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Один юнит в ряду - он и выбирается")
    void getSuitableUnits_singleUnit_shouldSelect() {
        Unit unit = createAliveUnit("Single", 0, 5);

        List<List<Unit>> unitsByRow = new ArrayList<>();
        unitsByRow.add(Collections.singletonList(unit));

        List<Unit> result = finder.getSuitableUnits(unitsByRow, true);

        assertEquals(1, result.size());
        assertEquals("Single", result.getFirst().getName());
    }

    private Unit createAliveUnit(String name, int x, int y) {
        Unit unit = new Unit(name, "TestType", 100, 20, 50, "melee", null, null, x, y);
        unit.setAlive(true);
        return unit;
    }

    private Unit createDeadUnit(String name, int x, int y) {
        Unit unit = new Unit(name, "TestType", 100, 20, 50, "melee", null, null, x, y);
        unit.setAlive(false);
        return unit;
    }
}

