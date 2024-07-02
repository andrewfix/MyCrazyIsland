package com.project.andrew;

import com.project.andrew.exceptions.OrganismlIsDieException;
import com.project.andrew.interfaces.Lockable;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractIslandOrganism implements Lockable, Cloneable {
    private ReentrantLock lock = new ReentrantLock();

    private static long organismNumber = 0;

    //  Мертва ли особь
    protected volatile boolean isDead = false;

    //  Иконка организма
    @Getter
    private String icon = "X";

    //  Название организма (животного, растения и т.п.)
    private String organismName = this.getClass().getSimpleName();

    //  Время жизни (Через сколько ходов наступить смерть)
    @Getter
    private int lifeSpan = 10;

    //  Вес организма
    @Getter
    private double weight = 10;

    //  Минимальный возраст для размножения
    @Getter
    private int minimumReproductiveAge = 2;

    //  Число потомков, получаемых в результате размножения
    @Getter
    private int offspringCount = 1;

    //  Число особей данного организма на клетке при создании клетки
    @Getter
    private int defaultNumberIndividualsInCell = 10;

    //  Максимальное число особей данного организма на клетке
    @Getter
    private int maxNumberIndividualsInCell = 20;

    //  Имя особи
    @Getter
    @Setter
    private String name;

    //  Возраст особи. Должна умереть, если возраст превышает lifeSpan
    @Getter
    private int age = 0;

    //  Местоположение особи
    @Getter
    @Setter
    private Cell currentCell;

    public AbstractIslandOrganism() {
        this.setName();
    }

    public void init() {
        /*
          Не допускаем, чтобы число особей данного организма при инициализации
          привысило максимальное число
         */
        if (defaultNumberIndividualsInCell > maxNumberIndividualsInCell) {
            defaultNumberIndividualsInCell = maxNumberIndividualsInCell;
        }
    }

    @Override
    public AbstractIslandOrganism clone() throws CloneNotSupportedException {
        AbstractIslandOrganism organism = (AbstractIslandOrganism) super.clone();
        organism.lock = new ReentrantLock();
        organism.currentCell = this.currentCell;
        organism.setName();
        return organism;
    }

    /**
     * Устанавливаем имя особи (по молчанию)
     */
    private void setName() {
        this.name = this.organismName + "-" + (++organismNumber);
    }

    /**
     * Прибавляет "год" жизни. Инкримент за ход
     */
    public void incAge() {
        this.age++;
        if (age > lifeSpan) {
            isDead = true;
        }
    }

    /**
     * Размножение. Добавляет детей на клетку.
     * Обычное клонирование данного объекта не подходит, т.к. нужен "чистый" объект, полученный из прототипов "фабрики"
     *
     * @param list
     * @return
     */
    public int reproduction(List<AbstractIslandOrganism> list) {
        int count = 0;
        synchronized (this.currentCell) {
            if (this.isDead) {
                throw new OrganismlIsDieException();
            }
            //  Возвращает число особей, которые должны "умереть",
            //  чтобы общее число особей данного вида на клетке не превышало maxNumberIndividualsInCell
            long mustDieCount = this.currentCell.getOrganismPerCell(this.getClass()).size() + list.size() - maxNumberIndividualsInCell;
            if (mustDieCount > 0) {
                //  Если такие есть, то они "умирают"
                list.subList((int) (list.size() - mustDieCount), list.size()).clear();
            }
            currentCell.addOrganismList(list);
            count = list.size();
        }
        return count;
    }

    @Override
    public String toString() {
        return "AbstractIslandOrganism{" + "organismName='" + organismName + '\'' + '\n' + ", lifeSpan=" + lifeSpan + '\n' + ", weight=" + weight + '\n' + ", offspringCount=" + offspringCount + '\n' + ", defaultNumberIndividualsInCell=" + defaultNumberIndividualsInCell + '\n' + ", maxNumberIndividualsInCell=" + maxNumberIndividualsInCell + '\n' + ", name='" + name + '\'' + '\n' + ", age=" + age + '\n' + ", currentCell=(" + currentCell.getRow() + "," + currentCell.getCol() + "," + currentCell.showOrganismStatistic() + ")" + '\n' + '}';
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    @Override
    public boolean tryLock() {
        //  Специально переопределял методы блокировки.
        // Варианты блокировки между lock и tryLock с параметром и без оного

        return lock.tryLock();

        /*try {
            return (lock.tryLock(100, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
        /*lock.lock();
        return true;*/
    }

    @Override
    public boolean isLocked() {
        return lock.isLocked();
    }


    /**
     * Живой или мертвый
     *
     * @return
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * Лишить особь жизни :-((
     */
    public void setDead() {
        isDead = true;
    }
}
