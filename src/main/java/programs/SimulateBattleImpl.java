package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Реализация симулятора боя между армиями.
 * <p>
 * Проводит пошаговый бой с раундовой системой, где юниты атакуют
 * в порядке убывания силы атаки.
 * <p>
 * Алгоритмическая сложность: O(n² × log n), где n — общее количество юнитов.
 *
 * @see SimulateBattle
 */
public class SimulateBattleImpl implements SimulateBattle {
    private PrintBattleLog printBattleLog; // Позволяет логировать. Использовать после каждой атаки юнита

    /**
     * Симулирует бой между армией игрока и армией компьютера.
     * <p>
     * Правила боя:
     * 1. На каждом раунде юниты сортируются по убыванию значения атаки
     * 2. Юниты атакуют друг друга по очереди
     * 3. Погибшие юниты (isAlive == false) пропускаются
     * 4. Бой завершается, когда в одной из армий не остаётся живых юнитов
     * <p>
     * Сложность: O(n² * log n), где n — общее количество юнитов
     * - O(n) раундов в худшем случае (каждый раунд погибает минимум 1 юнит)
     * - O(n log n) сортировка в каждом раунде
     * - O(n) атак в каждом раунде
     */
    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        // Пока в обеих армиях есть живые юниты
        while (hasAliveUnits(playerArmy) && hasAliveUnits(computerArmy)) {
            // 1. Собираем всех живых юнитов из обеих армий
            List<Unit> allUnits = new ArrayList<>();
            allUnits.addAll(getAliveUnits(playerArmy));
            allUnits.addAll(getAliveUnits(computerArmy));

            // 2. Сортируем по убыванию базовой атаки (самые сильные ходят первыми)
            allUnits.sort(Comparator.comparingInt(Unit::getBaseAttack).reversed());

            // 3. Каждый юнит по очереди атакует
            for (Unit unit : allUnits) {
                // Проверяем, что юнит ещё жив (мог погибнуть в этом раунде от предыдущей атаки)
                if (!unit.isAlive()) {
                    continue;
                }

                // Юнит атакует через свою программу
                Unit target = unit.getProgram().attack();

                // Логируем результат атаки
                printBattleLog.printBattleLog(unit, target);
            }
        }
    }

    /**
     * Проверяет, есть ли в армии живые юниты.
     * Сложность: O(n)
     */
    private boolean hasAliveUnits(Army army) {
        if (army == null || army.getUnits() == null) {
            return false;
        }
        for (Unit unit : army.getUnits()) {
            if (unit.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает список живых юнитов армии.
     * Сложность: O(n)
     */
    private List<Unit> getAliveUnits(Army army) {
        List<Unit> aliveUnits = new ArrayList<>();
        if (army == null || army.getUnits() == null) {
            return aliveUnits;
        }
        for (Unit unit : army.getUnits()) {
            if (unit.isAlive()) {
                aliveUnits.add(unit);
            }
        }
        return aliveUnits;
    }
}
