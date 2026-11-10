package com.vertyll.freshly.infra.mongo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates the indexes the documents declare, under the names they declare, before the
 * application takes its first request.
 *
 * <p>
 * Spring Data's {@code auto-index-creation} does the creating while the mapping context
 * starts, before any code can run, and MongoDB refuses an index whose keys an index of
 * another name already covers. A renamed index would therefore stop the application from
 * starting. This step runs once every bean exists: for each declared index it drops an
 * index on the same keys under another name — one an earlier release created — and then
 * creates the declared one, which is a no-op when it is already there. A database
 * converges on what the code declares at every start, so nothing needs to be recorded.
 *
 * <p>
 * It covers every {@code @Document} in the mapping context, so a module declares an index
 * on its document and nothing here changes.
 */
@Slf4j
@Component
public class MongoIndexes implements SmartInitializingSingleton {
    private static final String NAME = "name";
    private static final String KEY = "key";

    private final MongoOperations mongo;

    public MongoIndexes(MongoOperations mongo) {
        this.mongo = mongo;
    }

    @Override
    public void afterSingletonsInstantiated() {
        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext =
                mongo.getConverter().getMappingContext();
        MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
        for (MongoPersistentEntity<?> entity : mappingContext.getPersistentEntities()) {
            if (entity.isAnnotationPresent(org.springframework.data.mongodb.core.mapping.Document.class)) {
                ensure(entity.getCollection(), resolver.resolveIndexFor(entity.getTypeInformation()));
            }
        }
    }

    private void ensure(String collection, Iterable<? extends IndexDefinition> declared) {
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
