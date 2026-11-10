package com.vertyll.freshly.infra.mongo;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.stereotype.Component;

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
@Component
public class MongoIndexes implements SmartInitializingSingleton {
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
            if (entity.isAnnotationPresent(Document.class)) {
                new CollectionIndexes(mongo, entity.getCollection())
                    .ensure(resolver.resolveIndexFor(entity.getTypeInformation()));
            }
        }
    }
}
