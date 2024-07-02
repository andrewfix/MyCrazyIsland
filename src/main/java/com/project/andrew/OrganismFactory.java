package com.project.andrew;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.Getter;
import net.bytebuddy.ByteBuddy;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class OrganismFactory {
    //  Возвращает все типы организмов
    private final Set<Class<? extends AbstractIslandOrganism>> TYPES = new HashSet<>();
    //  Возвращает карту прототипов организмов (применяется для их размножения)
    private final Map<Class<? extends AbstractIslandOrganism>, AbstractIslandOrganism> PROTOTYPES = new HashMap<>();

    //  Возвращает карту карт организма и его потенциальных жертв
    @Getter
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
        //  Игнорируем свойства в файле, которые не описаны в классе
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AbstractIslandOrganism organism = yamlMapper.readValue(resource, type);
        organism.init();
        return organism;
    }

    /**
     * Читает yaml-файл и возвращает карту параметр-значение
     *
     * @param resource
     * @return
     * @throws IOException
     */
    private Map<String, String> getDataFromYaml(URL resource) throws IOException {
        YAMLMapper yamlMapper = new YAMLMapper();
        Map<String, String> map = yamlMapper.readValue(resource, new TypeReference<Map<String, String>>() {
        });
        return map;
    }

    /**
     * Динамически создает и возвращает класс
     *
     * @param className
     * @param baseClassName
     * @return
     * @throws ClassNotFoundException
     */
    private Class<? extends AbstractIslandOrganism> createClassFromName(String className, String baseClassName) throws ClassNotFoundException {
        Class<?> baseClass = Class.forName(baseClassName);
        return (Class<? extends AbstractIslandOrganism>) new ByteBuddy()
                .subclass(baseClass)
                .name(className)
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();
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
                Class<? extends AbstractIslandOrganism> clazz = null;
                String className = organismClassPackageName + "." + FilenameUtils.getBaseName(entry.toString());
                try {
                    clazz = (Class<? extends AbstractIslandOrganism>) Class.forName(className);
                } catch (ClassNotFoundException e) {
                    //  если класс не найден, создаем его динамически
                    var map = getDataFromYaml(entry.toUri().toURL());
                    String parentClassName = OrganismFactory.class.getPackage().getName() + "." + map.get("baseClassName");
                    clazz = createClassFromName(className, parentClassName);
                }
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
                //  Т.к классы могут быть созданны динамически, а ClassLoader для них не определен,
                //  то используем для создания объектов наши TYPES, тем самым не нарушая безопасность и
                //  не усложняя проект. (вместо Class.forName)
                keyClass = TYPES.stream().filter(x -> x.getName().equals(organismClassPackageName + "." + entry.getKey())).findFirst().get();
            } catch (Exception/*ClassNotFoundException*/ e) {
                //  Если данный класс не найден, игнорируем его
                continue;
            }
            Map<Class<? extends AbstractIslandOrganism>, Integer> innerMap = new HashMap<>();
            for (var innerEntry : entry.getValue().entrySet()) {
                Class<? extends AbstractIslandOrganism> innerKeyClass = null;
                try {
                    innerKeyClass = TYPES.stream().filter(x -> x.getName().equals(organismClassPackageName + "." + innerEntry.getKey())).findFirst().get();
                } catch (Exception/*ClassNotFoundException*/ e) {
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
     * Возвращает прототип организма
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
}