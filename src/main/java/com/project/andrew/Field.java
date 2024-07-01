package com.project.andrew;

import lombok.Getter;

import java.util.List;
import java.util.function.Supplier;

public class Field {
    //  Число строк на поле
    @Getter
    private int rowCount;
    //  Число столбцов на поле
    @Getter
    private int colCount;
    private Cell[][] grid;

    public Field(int m, int n, Supplier<List<AbstractIslandOrganism>> func) {
        rowCount = m;
        colCount = n;
        grid = new Cell[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                grid[i][j] = new Cell(i, j);
                // Инициализация поля. Заполнение ячеек (предустановка) организмами
                grid[i][j].addOrganismList(func.get());
            }
        }
    }

    /**
     * Возвращает клетку на поле
     *
     * @param row
     * @param col
     * @return
     */
    public Cell getCell(int row, int col) {
        return grid[row][col];
    }

    /**
     * Обновляет состояние поля (Должно вызывается после такта)
     */
    protected void updateBeforeEvent() {
        synchronized (grid) {
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < colCount; j++) {
                    getCell(i, j).updateBeforeEvent();
                }
            }
        }
    }

    /**
     * Возвращает число особей на поле
     *
     * @return
     */
    public int getOrganismCount() {
        int numOrganism = 0;
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColCount(); j++) {
                numOrganism += getCell(i, j).getOrganismList().size();
            }
        }
        return numOrganism;
    }

}
