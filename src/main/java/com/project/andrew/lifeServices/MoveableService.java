package com.project.andrew.lifeServices;

import com.project.andrew.*;
import com.project.andrew.AbstractIslandAnimal;
import com.project.andrew.exceptions.OrganismlNotInCellException;
import com.project.andrew.exceptions.OrganismlTypeMismatchException;
import com.project.andrew.interfaces.Moveable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class MoveableService implements Consumer<Moveable> {
    //  Карта острова
    private Field field;

    public MoveableService(Field field) {
        this.field = field;
    }

    /**
     * Возвращает список список соседних клеток, возможных для перемещения
     *
     * @param x
     * @param field
     * @return
     */
    private List<Cell> getAdjacentCells(Moveable x, Field field) {
        List<Cell> adjacentCells = new CopyOnWriteArrayList<>();
        int row = ((AbstractIslandAnimal) x).getCurrentCell().getRow();
        int col = ((AbstractIslandAnimal) x).getCurrentCell().getCol();

        if (row > 0) {
            adjacentCells.add(field.getCell(row - 1, col));
        }
        if (row < field.getRowCount() - 1) {
            adjacentCells.add(field.getCell(row + 1, col));
        }
        if (col > 0) {
            adjacentCells.add(field.getCell(row, col - 1));
        }
        if (col < field.getColCount() - 1) {
            adjacentCells.add(field.getCell(row, col + 1));
        }

        return adjacentCells;
    }


    private void moveTask(AbstractIslandAnimal x) {
        if (!(x instanceof Moveable)) {
            throw new OrganismlTypeMismatchException();
        }
        if (x.getMaxSpeed() == 0) {
            return;
        }

        if (x.tryLock()) {
            try {
                if (x.isDead()) {
                    return;
                }
                if (x.getCurrentCell() == null) {
                    throw new OrganismlNotInCellException();
                }

                Utils.showText(x.getName() + " заблокирован для передвижения");

                //  Число перемещений (от 0 до MAX_SPEED-1)
                int stepCount = ThreadLocalRandom.current().nextInt(x.getMaxSpeed());

                if (stepCount == 0) {
                    Utils.showText(x.getName() + " передумал перемещаться");
                }

                List<Cell> adjacentCells;
                for (int i = 0; i < stepCount; i++) {
                    adjacentCells = getAdjacentCells(x, field);
                    if (adjacentCells.isEmpty()) {
                        return;
                    }

                    Utils.showText(x.getName() + " решил переместиться из клетки " + x.getCurrentCell().showCellPosition());

                    Cell targetCell = adjacentCells.get(ThreadLocalRandom.current().nextInt(adjacentCells.size()));
                    synchronized (targetCell) {
                        if (x.move(targetCell)) {
                            Utils.showText(x.getName() + " переместился в клетку " + x.getCurrentCell().showCellPosition());
                        } else {
                            Utils.showText(x.getName() + " нее смог переместиться в клетку " + targetCell.showCellPosition() + (x.isDead() ? " (умер)" : ""));
                            break;
                        }
                    }
                }
            } finally {
                x.unlock();
                Utils.showText(x.getName() + " разблокирован после передвижения");
            }
        }
    }

    @Override
    public void accept(Moveable moveable) {
        moveTask((AbstractIslandAnimal) moveable);
    }
}
