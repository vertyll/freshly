package com.vertyll.freshly.infra.mongo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import lombok.extern.slf4j.Slf4j;

/**
 * The indexes of one collection, brought in line with the ones its document declares: an index on the same keys under
 * another name is dropped before the declared one is created, as {@link MongoIndexes} explains.
 */
@Slf4j
final class CollectionIndexes {

    private static final String NAME = "name";
    private static final String KEY = "key";

    private final MongoOperations mongo;
    private final String collection;

    CollectionIndexes(MongoOperations mongo, String collection) {
        this.mongo = mongo;
        this.collection = collection;
    }

    void ensure(Iterable<? extends IndexDefinition> declared) {
        IndexOperations indexes = mongo.indexOps(collection);
        List<Document> existing = new ArrayList<>();
        mongo.getCollection(collection).listIndexes().forEach(existing::add);
        for (IndexDefinition index : declared) {
            String name = index.getIndexOptions().getString(NAME);
            if (name != null) {
                for (Document old : existing) {
                    String oldName = old.getString(NAME);
                    if (!name.equals(oldName) && sameKeys(old.get(KEY, Document.class), index.getIndexKeys())) {
                        log.info("Dropping index {} of {}: its keys are now indexed as {}", oldName, collection, name);
                        indexes.dropIndex(oldName);
                    }
                }
            }
            indexes.createIndex(index);
        }
    }

    private static boolean sameKeys(Document existing, Document declared) {
        if (existing.size() != declared.size()) {
            return false;
        }
        Iterator<Map.Entry<String, Object>> declaredKeys = declared.entrySet().iterator();
        for (Map.Entry<String, Object> key : existing.entrySet()) {
            Map.Entry<String, Object> expected = declaredKeys.next();
            if (!key.getKey().equals(expected.getKey()) || !sameDirection(key.getValue(), expected.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameDirection(Object existing, Object declared) {
        if (existing instanceof Number stored && declared instanceof Number wanted) {
            return stored.intValue() == wanted.intValue();
        }
        return Objects.equals(existing, declared);
    }
}
