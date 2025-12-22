package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.ArrayList;
import java.util.List;

public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    /**
     * Находит юнитов, подходящих для атаки.
     * <p>
     * Для isLeftArmyTarget == true (атакуем левую армию компьютера):
     * - Юнит доступен, если слева от него нет союзника (минимальный y в ряду)
     * <p>
     * Для isLeftArmyTarget == false (атакуем правую армию игрока):
     * - Юнит доступен, если справа от него нет союзника (максимальный y в ряду)
     * <p>
     * Сложность: O(n), где n — общее количество юнитов
     */
    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        List<Unit> result = new ArrayList<>();

        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) {
                continue;
            }

            Unit suitableUnit = null;

            for (Unit unit : row) {
                // Пропускаем мёртвых юнитов
                if (!unit.isAlive()) {
                    continue;
                }

                if (isLeftArmyTarget) {
                    // Атакуем левую армию -> ищем юнита с минимальным y (самый левый, "открытый")
                    if (suitableUnit == null || unit.getyCoordinate() < suitableUnit.getyCoordinate()) {
                        suitableUnit = unit;
                    }
                } else {
                    // Атакуем правую армию -> ищем юнита с максимальным y (самый правый, "открытый")
                    if (suitableUnit == null || unit.getyCoordinate() > suitableUnit.getyCoordinate()) {
                        suitableUnit = unit;
                    }
                }
            }

            if (suitableUnit != null) {
                result.add(suitableUnit);
            }
        }

        return result;
    }
}
