package com.project.andrew;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OrganismFactory {
    private final Set<Class<? extends AbstractIslandOrganism>> TYPES = new HashSet<>();
    private final Map<Class<? extends AbstractIslandOrganism>, AbstractIslandOrganism> PROTOTYPES = new HashMap<>();
    private final Map<Class<? extends AbstractIslandOrganism>, Map<Class<? extends AbstractIslandOrganism>, Integer>> foodConsumptionProbability = new HashMap<>();
    private final String organismClassPackageName = OrganismFactory.class.getPackage().getName() + ".entity";

    public OrganismFactory() throws IOException, URISyntaxException, ClassNotFoundException {
        init();
    }

    /**
     * Создает и возвращает прототип организма с свойствами из @param resource класса @param type
     *
     * @param resource
     * @param type
     * @return
     * @throws IOException
     */

    private AbstractIslandOrganism loadObject(URL resource, Class<? extends AbstractIslandOrganism> type) throws IOException {
        YAMLMapper yamlMapper = new YAMLMapper();
        AbstractIslandOrganism organism = yamlMapper.readValue(resource, type);
        organism.init();
        return organism;
    }

    /**
     * Инициализация прототипов организмов.
     * Создаются прототипы организмов с свойствами описанных в organism/config
     * Создается карта карт для, с какой вероятностью животное съедает "пищу", если они находятся на одной клетке
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws URISyntaxException
     */
    private void init() throws IOException, ClassNotFoundException, URISyntaxException {
        Path dir = Path.of(OrganismFactory.class.getClassLoader().getResource("organism/config").toURI());
        Path foodConsumptionProbabilityFile = Path.of(OrganismFactory.class.getClassLoader().getResource("organism/foodConsumptionProbability.yaml").toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.{yaml,yml}")) {
            for (Path entry : stream) {
                Class<? extends AbstractIslandOrganism> clazz = (Class<? extends AbstractIslandOrganism>) Class.forName(organismClassPackageName + "." + FilenameUtils.getBaseName(entry.toString()));
                TYPES.add(clazz);
                AbstractIslandOrganism organism = loadObject(entry.toUri().toURL(), clazz);
                PROTOTYPES.put(clazz, organism);
            }
        }

        loadFoodConsumptionProbabilityFile(foodConsumptionProbabilityFile.toUri().toURL());
    }

    /**
     * Создается карта карт для, с какой вероятностью животное съедает "пищу", если они находятся на одной клетке
     *
     * @param url
     * @throws IOException
     */
    private void loadFoodConsumptionProbabilityFile(URL url) throws IOException {
        YAMLMapper yamlMapper = new YAMLMapper();
        Map<String, Map<String, Integer>> tempMap = yamlMapper.readValue(url, Map.class);
        for (var entry : tempMap.entrySet()) {
            Class<? extends AbstractIslandOrganism> keyClass = null;
            try {
                keyClass = (Class<? extends AbstractIslandOrganism>) Class.forName(organismClassPackageName + "." + entry.getKey());
            } catch (ClassNotFoundException e) {
                //  Если данный класс не найден, игнорируем его
                continue;
            }
            Map<Class<? extends AbstractIslandOrganism>, Integer> innerMap = new HashMap<>();
            for (var innerEntry : entry.getValue().entrySet()) {
                Class<? extends AbstractIslandOrganism> innerKeyClass = null;
                try {
                    innerKeyClass = (Class<? extends AbstractIslandOrganism>) Class.forName(organismClassPackageName + "." + innerEntry.getKey());
                } catch (ClassNotFoundException e) {
                    //  Если данный класс не найден, игнорируем его
                    continue;
                }
                innerMap.put(innerKeyClass, innerEntry.getValue());
            }
            foodConsumptionProbability.put(keyClass, innerMap);
        }
    }

    /**
     * Формирует список особей одного типа организма в количестве определенном по умолчанию для инициализации
     *
     * @param type
     * @return
     * @throws CloneNotSupportedException
     */
    private List<AbstractIslandOrganism> createOrganismListPerType(Class<? extends AbstractIslandOrganism> type) throws CloneNotSupportedException {
        List<AbstractIslandOrganism> list = new ArrayList<>();
        var organism = PROTOTYPES.get(type);
        for (int i = 0; i < organism.getDefaultNumberIndividualsInCell(); i++) {
            list.add(organism.clone());
        }
        return list;
    }

    /**
     * Возвоащает карту прототипов организмов (применяется для их размножения)
     *
     * @return
     */
    public Map<Class<? extends AbstractIslandOrganism>, AbstractIslandOrganism> getPrototypes() {
        return PROTOTYPES;
    }

    /**
     * Возвоащает прототип организма
     *
     * @param type
     * @return
     */
    public AbstractIslandOrganism getPrototypeByType(Class<? extends AbstractIslandOrganism> type) {
        return PROTOTYPES.get(type);
    }

    /**
     * Формирует список особей ВСЕХ типов организмов
     *
     * @return
     */
    protected List<AbstractIslandOrganism> createOrganismListForCell() {
        List<AbstractIslandOrganism> list = new ArrayList<>();
        TYPES.stream().forEach(type -> {
            try {
                list.addAll(createOrganismListPerType(type));
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        });
        return list;
    }

    /**
     * Возвращает карту карт организма и его потенциальных жертв
     *
     * @return
     */
    public Map<Class<? extends AbstractIslandOrganism>, Map<Class<? extends AbstractIslandOrganism>, Integer>> getFoodConsumptionProbability() {
        return foodConsumptionProbability;
    }

}
