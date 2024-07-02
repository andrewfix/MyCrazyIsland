package com.project.andrew.lifeServices;

import com.project.andrew.AbstractIslandOrganism;
import com.project.andrew.Utils;
import com.project.andrew.exceptions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ReproductionService implements Consumer<AbstractIslandOrganism> {
    //  Карта прототипов организмов для воспроизводства
    private final Map<Class<? extends AbstractIslandOrganism>, AbstractIslandOrganism> prototypes;

    public ReproductionService(Map<Class<? extends AbstractIslandOrganism>, AbstractIslandOrganism> prototypes) {
        this.prototypes = prototypes;
    }

    /**
     * Создает организм по переданному типу
     *
     * @param type
     * @return
     * @throws Exception
     */
    private AbstractIslandOrganism createOrganism(Class<? extends AbstractIslandOrganism> type) throws OrganismClassIsNullException, OrganismPrototypeNotFound, CloneNotSupportedException {
        if (type == null) {
            throw new OrganismClassIsNullException();
        }
        var organism = prototypes.get(type);
        if (organism == null) {
            throw new OrganismPrototypeNotFound();
        }
        return organism.clone();
    }

    /**
     * Размножение. Возвращает список детенышей, в количестве OFFSPRING_COUNT, определенных в свойствах данного организма
     *
     * @param prototype
     * @return
     * @throws CloneNotSupportedException
     */
    private List<AbstractIslandOrganism> getOffspringList(AbstractIslandOrganism prototype) throws CloneNotSupportedException {

        //  Число детенышей (от 0 до OFFSPRING_COUNT)
        int offspringCount = ThreadLocalRandom.current().nextInt(prototype.getOffspringCount() + 1);
        ArrayList<AbstractIslandOrganism> children = new ArrayList<>();
        for (int i = 0; i < offspringCount; i++) {
            children.add(prototype.clone());
        }
        return children;
    }

    private void reproductionTask(AbstractIslandOrganism x) {
        if (x.getAge() < x.getMinimumReproductiveAge()) {
            return;
        }
        if (x.tryLock()) {
            try {
                if (x.isDead()) {
                    return;
                }
                Utils.showText(x.getName() + " заблокирован для размножения и ищет партнера...");
                AbstractIslandOrganism partner = null;
                //  Ищем партнера для размножения
                synchronized (x.getCurrentCell()) {
                    //  Создаем список особей данного типа, живых и незаблокированных (не занятых)
                    try {
                        var partnerList = x.getCurrentCell().getOrganismPerCell(x.getClass())
                                .stream()
                                .filter(s -> (s != x && !s.isDead() && (s.getAge() >= s.getMinimumReproductiveAge()) && !s.isLocked())).toList();
                        if (partnerList == null || partnerList.isEmpty()) {
                            Utils.showText(x.getName() + " не нашел партнера для размножения");
                            return;
                        }
                        //  И выбираем случайным образом партнера
                        partner = partnerList.get(ThreadLocalRandom.current().nextInt(partnerList.size()));

                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }

                if (partner.tryLock()) {
                    try {
                        Utils.showText(x.getName() + " нашел " + partner.getName() + " и начинает размножение");
                        Utils.showText(partner.getName() + " заблокирован как партнер");

                        var prototype = createOrganism(x.getClass());
                        List<AbstractIslandOrganism> list = getOffspringList(prototype);
                        int offspringListCount = list.size();
                        int count = x.reproduction(list);
                        if (count == 0) {
                            Utils.showText(x.getName() + " потерял потомков при размножении");
                        } else {
                            Utils.showText(x.getName() + " воспроизвел " + offspringListCount + " потомков, выжило - " + count);
                        }
                    } finally {
                        partner.unlock();
                        Utils.showText(partner.getName() + " разблокирован как партнер");
                    }
                } else {
                    Utils.showText(partner.getName() + " не сможет участвовать в размножении в качестве партнера для " + x.getName());
                }
            } catch (Exception e) {
                Utils.showText(x.getName() + " что-то не так с размножением....");
                throw new ReproductionException(x.getName());
            } finally {
                Utils.showText(x.getName() + " разблокирован после размножения");
                x.unlock();
            }
        } else {
            Utils.showText(x.getName() + " уже кем-то заблокирован для каких-то дел");
        }
    }

    @Override
    public void accept(AbstractIslandOrganism organism) {
        reproductionTask(organism);
    }
}
