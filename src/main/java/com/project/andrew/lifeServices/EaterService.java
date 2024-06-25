package com.project.andrew.lifeServices;

import com.project.andrew.AbstractIslandAnimal;
import com.project.andrew.AbstractIslandOrganism;
import com.project.andrew.interfaces.Eater;
import com.project.andrew.Utils;
import com.project.andrew.exceptions.OrganismlTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EaterService implements Consumer<Eater> {
    //  Карта карт организов и его потенциальных жертв
    private final Map<Class<? extends AbstractIslandOrganism>, Map<Class<? extends AbstractIslandOrganism>, Integer>> foodMap;

    public EaterService(Map<Class<? extends AbstractIslandOrganism>, Map<Class<? extends AbstractIslandOrganism>, Integer>> food) {
        this.foodMap = food;
    }

    /**
     * Возвращает "пищевую карту" для огранизма type
     *
     * @param type
     * @return
     */
    private Map<Class<? extends AbstractIslandOrganism>, Integer> getFoodConsumptionProbabilityForOrganism(Class<? extends AbstractIslandOrganism> type) {
        return foodMap.get(type);
    }

    /**
     * Создаем список потенциальных "жертв" из особей на клетке, которых можно съесть (их типы организмов содержатся в Map food)
     *
     * @param obj
     * @return
     */
    private List<? extends AbstractIslandOrganism> getPreyList(AbstractIslandOrganism obj) {
        ArrayList<AbstractIslandOrganism> preyList = obj.getCurrentCell().getOrganismList().stream().filter(item -> (foodMap.get(obj.getClass()).containsKey(item.getClass()) && !item.lock.isLocked())).collect(Collectors.toCollection(ArrayList::new));
        return preyList.isEmpty() ? null : preyList;
    }

    /**
     * Возвращает "жертву" для поедания
     *
     * @param obj
     * @return
     */
    private AbstractIslandOrganism getPrey(AbstractIslandOrganism obj) {
        var preyList = getPreyList(obj);
        //  Из этого списка случайным образом выбираем "жертву"
        if (preyList == null) {
            return null;
        }

        var prey = preyList.get(ThreadLocalRandom.current().nextInt(preyList.size()));
        Utils.showText(obj.getName() + " нашел " + prey.getName());

        int foodConsumptionProbability = ThreadLocalRandom.current().nextInt(100);
        //  Если вероятность наступила, то возвращаем "жертву для съедания"
        if (foodConsumptionProbability < getFoodConsumptionProbabilityForOrganism(obj.getClass()).get(prey.getClass())) {
            return prey;
        } else {
            Utils.showText(obj.getName() + " не смог съесть " + prey.getName() + ". Неудачная охота");
            return null;
        }
    }

    private void eatTask(AbstractIslandOrganism x) {
        if (!(x instanceof Eater)) {
            throw new OrganismlTypeMismatchException();
        }
        int attempts = 0;

        Utils.showText(x.getName() + " готовится к охоте");

        if (x.tryLock()) {
            try {
                if (x.isDead()) {
                    return;
                }
                Utils.showText(x.getName() + " заблокирован как хищник");
                //  У организма есть huntTryCount попыток поохотиться
                while (attempts < ((AbstractIslandAnimal) x).getHuntTryCount()) {
                    Utils.showText(x.getName() + " попытка-" + (attempts + 1));
                    Utils.showText(x.getName() + " ищет жертву ...");
                    var prey = getPrey(x);
                    if (prey != null && prey.tryLock()) {
                        Utils.showText(prey.getName() + " заблокирован как жертва от " + x.getName());
                        try {
                            if (!prey.isDead()) {
                                Utils.showText(x.getName() + " начинает есть " + prey.getName());

                                if (((Eater) x).eat(prey)) {
                                    Utils.showText(x.getName() + " съел " + prey.getName());
                                } else {
                                    Utils.showText(prey.getName() + " не удалось съесть " + prey.getName());
                                }
                            } else {
                                Utils.showText(prey.getName() + " уже умер. Мертвых не едим!");
                            }
                        } finally {
                            Utils.showText(prey.getName() + " разблокирован как жертва");
                            prey.unlock();
                            return;
                        }

                    }
                    attempts++;
                    try {
                        Thread.sleep(100); // Пауза между попытками
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            } finally {
                Utils.showText(x.getName() + " разблокирован как хищник");
                x.unlock();
            }

        } else {
            Utils.showText(x.getName() + " уже кем-то заблокирован для каких-то дел");
        }
    }

    @Override
    public void accept(Eater eater) {
        eatTask((AbstractIslandOrganism) eater);
    }
}
