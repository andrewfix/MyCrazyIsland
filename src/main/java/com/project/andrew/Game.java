package com.project.andrew;

import com.project.andrew.lifeServices.EaterService;
import com.project.andrew.lifeServices.MoveableService;
import com.project.andrew.lifeServices.ReproductionService;
import com.project.andrew.interfaces.Eater;
import com.project.andrew.interfaces.Moveable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class Game {

    private int step = 0;

    public final Field field;
    //  Инициализация прототипов организмов, карты карт организма и его потенциальных жертв и т.п.
    private final OrganismFactory factory;
    private final EaterService eaterService;
    private final MoveableService moveableService;
    private final ReproductionService reproductionService;

    public Game(int rowCount, int colCount) throws IOException, URISyntaxException, ClassNotFoundException {
        factory = new OrganismFactory();
        field = new Field(rowCount, colCount, factory::createOrganismListForCell);
        //  Инициализация "жизненных" сервисов
        eaterService = new EaterService(factory.getFoodConsumptionProbability());
        moveableService = new MoveableService(field);
        reproductionService = new ReproductionService(factory.getPrototypes());
    }

    /**
     * Увеличивает такт ("год жизни")
     */
    private void incStep() {
        this.step++;
        field.updateBeforeEvent();
    }

    /**
     * Отображает содержимое клеток на поле
     */
    private void showState() {
        //  Примерная ширина столбцов
        int width = factory.getPrototypes().size() * 6 + 4;
        System.out.println("Step " + step);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < field.getRowCount(); i++) {
            stringBuilder.append("| ");
            for (int j = 0; j < field.getColCount(); j++) {
                field.getCell(i, j).showOrganismStatistic().forEach((type, count) -> {
                    AbstractIslandOrganism obj = factory.getPrototypeByType(type);
                    stringBuilder.append(obj.getIcon()).append("-").append(count).append(" ");
                });
                System.out.print(String.format("%-" + width + "s", stringBuilder) + " | ");
                stringBuilder.setLength(0);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Запускает жизненный цикл для особи (один такт или "год жизни")
     *
     * @param x
     * @return
     */
    private Callable<Void> organismTask(AbstractIslandOrganism x) {
        return () -> {
            try {
                Utils.showText(x.getName() + " начинает свои жизненные потребности");
                //  Едим
                if (x instanceof Eater) {
                    eaterService.accept((Eater) x);
                    Thread.sleep(10);
                }
                //  Занимаемся размножением
                reproductionService.accept(x);
                Thread.sleep(10);

                //  Перемещаемся на соседние клетки
                if (x instanceof Moveable) {
                    moveableService.accept((Moveable) x);
                    Thread.sleep(10);
                }

                Utils.showText(x.getName() + " заканчивает свои жизненные потребности");
                return null;

            } catch (Exception e) {
                System.err.println(e);
                throw e;
            }
        };
    }

    /**
     * Запускает потоки для всех особей на всем поле (Такт или "год жизни" для поля)
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void makeAction() throws ExecutionException, InterruptedException, RuntimeException {
        try (ExecutorService organismListExecutorService = Executors.newFixedThreadPool(600)) {
            List<Callable<Void>> tasks = new ArrayList<>();

            try {
                for (int i = 0; i < field.getRowCount(); i++) {
                    for (int j = 0; j < field.getColCount(); j++) {
                        int finalI = i;
                        int finalJ = j;

                        var list = field.getCell(finalI, finalJ).getOrganismList();
                        //  Делаем "снимок" списка, чтобы избавиться от его изменений во время итераций
                        List<? extends AbstractIslandOrganism> snapshot;
                        synchronized (list) {
                            snapshot = new ArrayList<>(list.stream().toList());
                        }

                        for (var x : snapshot) {
                            tasks.add(organismTask(x));
                        }
                    }
                }

                //  Перемешиваем для равномерного распределения
                Collections.shuffle(tasks);

                long startTime = System.currentTimeMillis();
                List<Future<Void>> futures = organismListExecutorService.invokeAll(tasks);
                for (Future<Void> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        throw e;
                    }
                }
                long endTime = System.currentTimeMillis();
                System.out.println("Completed in " + (endTime - startTime) + " ms");
            } finally {
                organismListExecutorService.shutdown();
                try {
                    if (!organismListExecutorService.awaitTermination(800, TimeUnit.SECONDS)) {
                        organismListExecutorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Запуск игры
     */
    public void start(int stepCount) {
        try {
            System.out.println("Начальное состояние поля");
            showState();
            for (int i = 0; i < stepCount; i++) {
                makeAction();
                incStep();
                showState();
                if (field.getOrganismCount() == 0) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Поток был прерван: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
