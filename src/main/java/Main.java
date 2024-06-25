import com.project.andrew.Game;
import com.project.andrew.Utils;

import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {

    static Game game;
    static int numRows = 10;
    static int numCols = 10;
    static int stepCount = 21;
    static boolean showAdvancedInfo = true;

    private static void showInputValues() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите число строк поля: ");
        while (!scanner.hasNextInt()) {
            System.out.print("Ошибка! Введите целое число: ");
            scanner.next();
        }
        numRows = scanner.nextInt();

        System.out.print("Введите число столбцов поля: ");
        while (!scanner.hasNextInt()) {
            System.out.print("Ошибка! Введите целое число: ");
            scanner.next();
        }
        numCols = scanner.nextInt();

        System.out.print("Введите количество тактов (годов жизни): ");
        while (!scanner.hasNextInt()) {
            System.out.print("Ошибка! Введите целое число: ");
            scanner.next();
        }
        stepCount = scanner.nextInt();

        System.out.print("Показывать расширенную информацию особей (все действия) (y/n): ");
        while (!scanner.hasNext(Pattern.compile("[yn]", Pattern.CASE_INSENSITIVE))) {
            System.out.print("Ошибка! Введите 'y' или 'n': ");
            scanner.next();
        }
        String s = scanner.next();
        showAdvancedInfo = (s.equalsIgnoreCase("Y") ? true : false);
    }

    public static void main(String[] args) {
        System.out.println("Добро пожаловать на сумашедший остров!");
        showInputValues();
        Utils.showAdvancedInfo = showAdvancedInfo;
        try {
            game = new Game(numRows, numCols);
            game.start(stepCount);
        } catch (Exception e) {
            System.err.println(e);
        }

    }
}