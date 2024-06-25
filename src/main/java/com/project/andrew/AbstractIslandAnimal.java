package com.project.andrew;

import com.project.andrew.interfaces.Eater;
import com.project.andrew.interfaces.Moveable;
import lombok.Getter;
import lombok.Setter;

public abstract class AbstractIslandAnimal extends AbstractIslandOrganism implements Moveable, Eater<AbstractIslandOrganism> {
    //  Максимальная скорость перемещения (число клеток за ход)
    @Getter
    private int maxSpeed = 1;

    //  Число попыток охоты за ход
    @Getter
    private int huntTryCount = 1;

    //  Сколько килограммов пищи нужно животному для полного насыщения
    @Getter
    private double foodRequirement = 1;

    //  Затрата энергии (уменьшение текущего уровня насыщения ) за ход, по умолчанию
    //  Если энергия уменьшается до 0, то животное считется умершим
    @Getter
    private double defaultFoodConsumption = 1;

    //  Затрата энергии (уменьшение текущего уровня насыщения ) на передвижение на клетку.
    //  Если энергия уменьшается до 0, то животное считется умершим
    @Getter
    private double movementFoodConsumption = 1;

    //  Текущий уровень насыщения. Не может быть больше, чем значение foodRequirement
    private double currentSatiationLevel;

    public boolean eat(AbstractIslandOrganism prey) {
        //  Смотрим, чтобы не съел сам себя или "пустышку"
        if (prey == null || prey == this) {
            return false;
        }
        if (!prey.isDead()) {
            //  Увеличивам текущий уровень насыщения currentSatiationLevel особи
            //  на вес съеденной жертвы
            this.incSatiationLevel(prey.getWeight());
            prey.setDead();
            //  Убираем съеденную особь с клетки.
            synchronized (this.getCurrentCell()) {
                this.getCurrentCell().removeOrganism(prey);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void incAge() {
        super.incAge();
        //  Обычный расход энергии за ход (уменьшение уровня насыщения)
        this.decSatiationLevel(defaultFoodConsumption);
    }

    /**
     * Уменьшение уровня насыщения, например после хода, (и) передвижения
     *
     * @param value
     */
    private void decSatiationLevel(double value) {
        this.currentSatiationLevel -= value;
        if (this.currentSatiationLevel <= 0) {
            isDead = true;
        }
    }

    /**
     * Увеличивает уровень насыщения, например после еды
     *
     * @param value
     */
    private void incSatiationLevel(double value) {
        this.currentSatiationLevel += value;
        if (this.currentSatiationLevel > foodRequirement) {
            this.currentSatiationLevel = foodRequirement;
        }
    }

    @Override
    public void init() {
        super.init();
        //  При создании новой особи предполагаем, что она частично сыта
        this.currentSatiationLevel = this.foodRequirement * 0.75;
    }

    @Override
    public boolean move(Cell targetCell) {
        //  В результате перемещения уменьшается энергия
        this.decSatiationLevel(movementFoodConsumption);
        if (isDead) {
            return false;
        }

        this.getCurrentCell().removeOrganism(this);
        //  Если в клетке назначения нет места для данной особи, то она погибает
        if (targetCell.getOrganismPerCell(this.getClass()).size() >= getMaxNumberIndividualsInCell()) {
            isDead = true;
            return false;
        } else {
            targetCell.addOrganism(this);
            return true;
        }

    }
}