package com.project.andrew;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class Cell {
    //  Координата Y (ордината)
    @Getter
    private int row;
    //  Координата X (абцисса)
    @Getter
    private int col;
    //  Возвращает список особей на данной клетке
    @Getter
    private ConcurrentLinkedQueue<AbstractIslandOrganism> organismList = new ConcurrentLinkedQueue<>();
    //private final List<AbstractIslandOrganism> organismList = new CopyOnWriteArrayList<>();
    //private List<AbstractIslandOrganism> organismList = Collections.synchronizedList(new ArrayList<>());

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * Возвращает текст координаты ячейки
     *
     * @return
     */
    public String showCellPosition() {
        return "(" + row + "," + col + ")";
    }

    /**
     * Добавляет особь на клетку
     *
     * @param organism
     * @return
     */
    protected boolean addOrganism(AbstractIslandOrganism organism) {
        organism.setCurrentCell(this);
        return organismList.add(organism);
    }

    /**
     * Добавляет список особей на клетку.
     *
     * @param list
     */
    protected void addOrganismList(List<AbstractIslandOrganism> list) {
        list.forEach(this::addOrganism);
    }

    /**
     * Удаляет особь с клетки
     *
     * @param organism
     * @return
     */
    protected boolean removeOrganism(AbstractIslandOrganism organism) {
        organism.setDead();
        organism.setCurrentCell(null);
        return organismList.remove(organism);
    }

    /**
     * Обновляет состояние поля. Возраст всех особей увеличивается на единицу.
     * Удаляет особи которые мертвы (isDead == true)
     */
    public void updateBeforeEvent() {
        synchronized (organismList) {
            for (var obj : organismList) {
                obj.incAge();
                if (obj.isDead()) {
                    organismList.remove(obj);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Cell{" + "row=" + row + ", col=" + col + '\n' + ", organismList=" + organismList.toString() + '}';
    }

    /**
     * Возвращает информацию о каждом типе организма и его количестве на данной клетке
     *
     * @return
     */
    public Map<Class<? extends AbstractIslandOrganism>, Long> showOrganismStatistic() {
        final Map<Class<? extends AbstractIslandOrganism>, Long> typeCount = organismList.stream().collect(Collectors.groupingBy(AbstractIslandOrganism::getClass, Collectors.counting()));
        return typeCount;
    }

    /**
     * Возвращает список особей типа type на данной клетке
     *
     * @param type
     * @return
     */
    public List<AbstractIslandOrganism> getOrganismPerCell(Class<? extends AbstractIslandOrganism> type) {
        return organismList.stream().filter(s -> s.getClass().equals(type)).toList();
    }

}