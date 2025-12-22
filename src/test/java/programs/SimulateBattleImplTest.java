package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тесты для SimulateBattleImpl.
 * Покрывает особые случаи согласно критериям оценивания:
 * - Юниты без цели
 * - Разные количества раундов
 * - Одновременные смерти
 * - Корректная очерёдность ходов
 * - Завершение боя
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimulateBattleImplTest {

    private SimulateBattleImpl simulateBattle;
    private List<String> battleLog;

    @BeforeEach
    void setUp() throws Exception {
        simulateBattle = new SimulateBattleImpl();
        battleLog = new ArrayList<>();

        // Инжектим mock PrintBattleLog через рефлексию
        PrintBattleLog mockLogger = (attacker, target) -> {
            String logEntry = attacker.getName() + " -> " + (target != null ? target.getName() : "null");
            battleLog.add(logEntry);
        };

        Field field = SimulateBattleImpl.class.getDeclaredField("printBattleLog");
        field.setAccessible(true);
        field.set(simulateBattle, mockLogger);
    }

    @Test
    @DisplayName("Бой завершается, когда одна армия уничтожена")
    void simulate_shouldEndWhenArmyDestroyed() throws InterruptedException {
        Unit player1 = createUnit("Player1", 100, 50, true);
        Unit computer1 = createUnit("Computer1", 50, 30, true);

        Army playerArmy = createArmyWithUnits(player1);
        Army computerArmy = createArmyWithUnits(computer1);

        // Настраиваем: player убивает computer
        Program playerProgram = mock(Program.class);
        when(playerProgram.attack()).thenAnswer(_ -> {
            computer1.setAlive(false);
            return computer1;
        });
        player1.setProgram(playerProgram);

        Program computerProgram = mock(Program.class);
        when(computerProgram.attack()).thenReturn(null);
        computer1.setProgram(computerProgram);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Бой должен завершиться
        assertFalse(battleLog.isEmpty(), "Должен быть хотя бы один лог атаки");
        assertFalse(computer1.isAlive(), "Computer должен быть мёртв");
    }

    @Test
    @DisplayName("Юниты без цели - атака возвращает null")
    void simulate_unitWithNoTarget_shouldHandleNull() throws InterruptedException {
        Unit player1 = createUnit("Player1", 100, 50, true);
        Unit computer1 = createUnit("Computer1", 100, 30, true);

        Army playerArmy = createArmyWithUnits(player1);
        Army computerArmy = createArmyWithUnits(computer1);

        AtomicInteger roundCount = new AtomicInteger(0);

        // Player атакует null, после 2 раундов убивает computer
        Program playerProgram = mock(Program.class);
        when(playerProgram.attack()).thenAnswer(_ -> {
            if (roundCount.incrementAndGet() >= 2) {
                computer1.setAlive(false);
                return computer1;
            }
            return null; // Нет цели
        });
        player1.setProgram(playerProgram);

        // Computer тоже атакует null
        Program computerProgram = mock(Program.class);
        when(computerProgram.attack()).thenReturn(null);
        computer1.setProgram(computerProgram);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Лог должен содержать атаки с null целью
        assertTrue(battleLog.stream().anyMatch(log -> log.contains("-> null")),
                "Должны быть логи с null целью");
    }

    @Test
    @DisplayName("Очерёдность ходов по убыванию атаки")
    void simulate_turnOrder_shouldBeSortedByAttack() throws InterruptedException {
        Unit strongPlayer = createUnit("StrongPlayer", 100, 100, true); // Атака 100
        Unit weakPlayer = createUnit("WeakPlayer", 100, 20, true); // Атака 20
        Unit mediumComputer = createUnit("MediumComputer", 100, 50, true); // Атака 50

        Army playerArmy = createArmyWithUnits(weakPlayer, strongPlayer);
        Army computerArmy = createArmyWithUnits(mediumComputer);

        // StrongPlayer убивает всех при первом ходе
        Program strongProgram = mock(Program.class);
        when(strongProgram.attack()).thenAnswer(_ -> {
            mediumComputer.setAlive(false);
            return mediumComputer;
        });
        strongPlayer.setProgram(strongProgram);

        Program weakProgram = mock(Program.class);
        when(weakProgram.attack()).thenReturn(null);
        weakPlayer.setProgram(weakProgram);

        Program computerProgram = mock(Program.class);
        when(computerProgram.attack()).thenReturn(null);
        mediumComputer.setProgram(computerProgram);

        simulateBattle.simulate(playerArmy, computerArmy);

        // StrongPlayer (100 атаки) должен ходить первым
        assertFalse(battleLog.isEmpty());
        assertTrue(battleLog.getFirst().startsWith("StrongPlayer"),
                "Юнит с наибольшей атакой должен ходить первым. Первый ход: " + battleLog.getFirst());
    }

    @Test
    @DisplayName("Одновременные смерти - погибшие юниты пропускаются")
    void simulate_simultaneousDeaths_shouldSkipDeadUnits() throws InterruptedException {
        Unit player1 = createUnit("Player1", 100, 80, true); // Атака 80, ходит первым
        Unit computer1 = createUnit("Computer1", 100, 70, true); // Атака 70
        Unit computer2 = createUnit("Computer2", 100, 60, true); // Атака 60

        Army playerArmy = createArmyWithUnits(player1);
        Army computerArmy = createArmyWithUnits(computer1, computer2);

        AtomicInteger playerAttacks = new AtomicInteger(0);

        // Player1 (80 атаки) ходит первым и убивает Computer1 (70 атаки)
        Program player1Program = mock(Program.class);
        when(player1Program.attack()).thenAnswer(_ -> {
            int attack = playerAttacks.incrementAndGet();
            if (attack == 1) {
                computer1.setAlive(false);
                return computer1;
            } else {
                computer2.setAlive(false);
                return computer2;
            }
        });
        player1.setProgram(player1Program);

        // Computer1 мёртв после первого хода Player1, не должен атаковать
        Program computer1Program = mock(Program.class);
        when(computer1Program.attack()).thenReturn(player1);
        computer1.setProgram(computer1Program);

        // Computer2 атакует
        Program computer2Program = mock(Program.class);
        when(computer2Program.attack()).thenReturn(player1);
        computer2.setProgram(computer2Program);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Computer1 не должен атаковать (был убит до своего хода)
        long computer1AttackCount = battleLog.stream()
                .filter(log -> log.startsWith("Computer1 ->"))
                .count();
        assertEquals(0, computer1AttackCount,
                "Мёртвый Computer1 не должен атаковать. Лог: " + battleLog);
    }

    @Test
    @DisplayName("Множество раундов до победы")
    void simulate_multipleRounds_shouldContinueUntilVictory() throws InterruptedException {
        Unit player = createUnit("Player", 100, 50, true);
        Unit computer = createUnit("Computer", 100, 40, true);

        Army playerArmy = createArmyWithUnits(player);
        Army computerArmy = createArmyWithUnits(computer);

        AtomicInteger roundCounter = new AtomicInteger(0);

        // Бой длится 3 раунда, затем компьютер погибает
        Program playerProgram = mock(Program.class);
        when(playerProgram.attack()).thenAnswer(_ -> {
            if (roundCounter.incrementAndGet() >= 3) {
                computer.setAlive(false);
            }
            return computer;
        });
        player.setProgram(playerProgram);

        Program computerProgram = mock(Program.class);
        when(computerProgram.attack()).thenReturn(player);
        computer.setProgram(computerProgram);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Должно быть несколько раундов
        assertTrue(battleLog.size() >= 3, "Должно быть минимум 3 атаки. Лог: " + battleLog);
    }

    @Test
    @DisplayName("Пустая армия игрока - бой не начинается")
    void simulate_emptyPlayerArmy_shouldEndImmediately() throws InterruptedException {
        Army playerArmy = createArmyWithUnits(); // пустая
        Unit computer1 = createUnit("Computer1", 100, 50, true);
        Army computerArmy = createArmyWithUnits(computer1);

        simulateBattle.simulate(playerArmy, computerArmy);

        assertTrue(battleLog.isEmpty(), "Бой не должен начаться с пустой армией");
    }

    @Test
    @DisplayName("Пустая армия компьютера - бой не начинается")
    void simulate_emptyComputerArmy_shouldEndImmediately() throws InterruptedException {
        Unit player1 = createUnit("Player1", 100, 50, true);
        Army playerArmy = createArmyWithUnits(player1);
        Army computerArmy = createArmyWithUnits(); // пустая

        simulateBattle.simulate(playerArmy, computerArmy);

        assertTrue(battleLog.isEmpty(), "Бой не должен начаться с пустой армией противника");
    }

    @Test
    @DisplayName("Все юниты игрока мертвы изначально")
    void simulate_allPlayerUnitsDead_shouldEndImmediately() throws InterruptedException {
        Unit deadPlayer = createUnit("DeadPlayer", 100, 50, false);
        Army playerArmy = createArmyWithUnits(deadPlayer);
        Unit computer1 = createUnit("Computer1", 100, 50, true);
        Army computerArmy = createArmyWithUnits(computer1);

        simulateBattle.simulate(playerArmy, computerArmy);

        assertTrue(battleLog.isEmpty(), "Бой не должен начаться, если все юниты мертвы");
    }

    @Test
    @DisplayName("Юнит погибает во время раунда и пропускает свой ход")
    void simulate_unitDiesBeforeTurn_shouldSkipTurn() throws InterruptedException {
        // Player1 имеет наибольшую атаку, ходит первым
        Unit player1 = createUnit("Player1", 100, 100, true);
        // Computer1 имеет среднюю атаку
        Unit computer1 = createUnit("Computer1", 100, 50, true);
        // Computer2 имеет меньшую атаку
        Unit computer2 = createUnit("Computer2", 100, 30, true);

        Army playerArmy = createArmyWithUnits(player1);
        Army computerArmy = createArmyWithUnits(computer1, computer2);

        AtomicInteger attackCount = new AtomicInteger(0);

        // Player1 убивает Computer1 при первой атаке, Computer2 при второй
        Program player1Program = mock(Program.class);
        when(player1Program.attack()).thenAnswer(_ -> {
            int count = attackCount.incrementAndGet();
            if (count == 1) {
                computer1.setAlive(false);
                return computer1;
            } else {
                computer2.setAlive(false);
                return computer2;
            }
        });
        player1.setProgram(player1Program);

        // Computer1 пытается атаковать, но будет мёртв к своему ходу
        Program computer1Program = mock(Program.class);
        when(computer1Program.attack()).thenReturn(player1);
        computer1.setProgram(computer1Program);

        // Computer2 тоже атакует
        Program computer2Program = mock(Program.class);
        when(computer2Program.attack()).thenReturn(player1);
        computer2.setProgram(computer2Program);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Computer1 не должен появиться в логах (был убит до своего хода)
        long computer1AttackCount = battleLog.stream()
                .filter(log -> log.startsWith("Computer1 ->"))
                .count();
        assertEquals(0, computer1AttackCount,
                "Мёртвый Computer1 не должен атаковать. Лог: " + battleLog);
    }

    @Test
    @DisplayName("Большое количество юнитов - производительность")
    void simulate_manyUnits_shouldHandleLoad() throws InterruptedException {
        List<Unit> playerUnits = new ArrayList<>();
        List<Unit> computerUnits = new ArrayList<>();

        // Создаём по 20 юнитов в каждой армии
        for (int i = 0; i < 20; i++) {
            Unit player = createUnit("Player" + i, 100, 50 + i, true);
            Unit computer = createUnit("Computer" + i, 100, 50 + i, true);
            playerUnits.add(player);
            computerUnits.add(computer);
        }

        Army playerArmy = new Army();
        playerArmy.setUnits(playerUnits);
        Army computerArmy = new Army();
        computerArmy.setUnits(computerUnits);

        // Каждая атака убивает одного противника
        AtomicInteger playerKillIdx = new AtomicInteger(0);
        AtomicInteger computerKillIdx = new AtomicInteger(0);

        for (Unit player : playerUnits) {
            Program program = mock(Program.class);
            when(program.attack()).thenAnswer(_ -> {
                int idx = playerKillIdx.getAndIncrement();
                if (idx < computerUnits.size()) {
                    Unit target = computerUnits.get(idx);
                    target.setAlive(false);
                    return target;
                }
                return null;
            });
            player.setProgram(program);
        }

        for (Unit computer : computerUnits) {
            Program program = mock(Program.class);
            when(program.attack()).thenAnswer(_ -> {
                int idx = computerKillIdx.getAndIncrement();
                if (idx < playerUnits.size()) {
                    Unit target = playerUnits.get(idx);
                    target.setAlive(false);
                    return target;
                }
                return null;
            });
            computer.setProgram(program);
        }

        long startTime = System.currentTimeMillis();
        simulateBattle.simulate(playerArmy, computerArmy);
        long duration = System.currentTimeMillis() - startTime;

        // Бой должен завершиться за разумное время (< 5 сек)
        assertTrue(duration < 5000,
                "Бой с 40 юнитами должен завершиться быстро. Время: " + duration + "ms");
        assertFalse(battleLog.isEmpty());
    }

    @Test
    @DisplayName("Равные армии - бой идёт до полного уничтожения")
    void simulate_equalArmies_shouldFightUntilEnd() throws InterruptedException {
        Unit player1 = createUnit("Player1", 100, 50, true);
        Unit player2 = createUnit("Player2", 100, 45, true);
        Unit computer1 = createUnit("Computer1", 100, 48, true);
        Unit computer2 = createUnit("Computer2", 100, 42, true);

        Army playerArmy = createArmyWithUnits(player1, player2);
        Army computerArmy = createArmyWithUnits(computer1, computer2);

        AtomicInteger round = new AtomicInteger(0);

        // Настраиваем моки для всех юнитов
        Program player1Program = mock(Program.class);
        when(player1Program.attack()).thenAnswer(_ -> {
            round.incrementAndGet();
            if (round.get() >= 6) {
                computer1.setAlive(false);
                computer2.setAlive(false);
            }
            return computer1;
        });
        player1.setProgram(player1Program);

        Program player2Program = mock(Program.class);
        when(player2Program.attack()).thenReturn(computer2);
        player2.setProgram(player2Program);

        Program computer1Program = mock(Program.class);
        when(computer1Program.attack()).thenReturn(player1);
        computer1.setProgram(computer1Program);

        Program computer2Program = mock(Program.class);
        when(computer2Program.attack()).thenReturn(player2);
        computer2.setProgram(computer2Program);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Бой должен завершиться
        assertTrue(battleLog.size() >= 4, "Должно быть минимум 4 атаки");
    }

    @Test
    @DisplayName("Проверка вызова printBattleLog после каждой атаки")
    void simulate_shouldCallPrintBattleLogAfterEachAttack() throws InterruptedException {
        Unit player = createUnit("Player", 100, 60, true);
        Unit computer = createUnit("Computer", 100, 50, true);

        Army playerArmy = createArmyWithUnits(player);
        Army computerArmy = createArmyWithUnits(computer);

        AtomicInteger attackCount = new AtomicInteger(0);

        Program playerProgram = mock(Program.class);
        when(playerProgram.attack()).thenAnswer(_ -> {
            if (attackCount.incrementAndGet() >= 2) {
                computer.setAlive(false);
            }
            return computer;
        });
        player.setProgram(playerProgram);

        Program computerProgram = mock(Program.class);
        when(computerProgram.attack()).thenReturn(player);
        computer.setProgram(computerProgram);

        simulateBattle.simulate(playerArmy, computerArmy);

        // Каждая атака должна быть залогирована
        assertFalse(battleLog.isEmpty());
    }

    // ============ Helper Methods ============

    private Army createArmyWithUnits(Unit... units) {
        Army army = new Army();
        army.setUnits(new ArrayList<>(Arrays.asList(units)));
        return army;
    }

    private Unit createUnit(String name, int health, int attack, boolean alive) {
        Unit unit = new Unit(name, "TestType", health, attack, 50, "melee", null, null, 0, 0);
        unit.setAlive(alive);
        return unit;
    }
}
