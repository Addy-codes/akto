package com.akto.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;
import java.util.Collections;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akto.dao.ApiInfoDao;
import com.akto.dao.FilterSampleDataDao;
import com.akto.dao.MCollection;
import com.akto.dao.SampleDataDao;
import com.akto.dao.SensitiveParamInfoDao;
import com.akto.dao.SensitiveSampleDataDao;
import com.akto.dao.SingleTypeInfoDao;
import com.akto.dao.TrafficInfoDao;
import com.akto.dao.context.Context;
import com.akto.dao.demo.VulnerableRequestForTemplateDao;
import com.akto.dao.testing_run_findings.TestingRunIssuesDao;
import com.akto.dto.ApiInfo.ApiInfoKey;
import com.akto.dto.type.SingleTypeInfo;
import com.akto.util.Constants;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

public class ApiCollectionUsers {

    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static final Logger logger = LoggerFactory.getLogger(ApiCollectionUsers.class);

    public enum CollectionType {
        ApiCollectionId, Id_ApiCollectionId, Id_ApiInfoKey_ApiCollectionId
    }

    public static final Map<CollectionType, MCollection<?>[]> COLLECTIONS_WITH_API_COLLECTION_ID = 
        Collections.unmodifiableMap(new HashMap<CollectionType, MCollection<?>[]>() {{
            put(CollectionType.ApiCollectionId, new MCollection[] {
                    SingleTypeInfoDao.instance,
                    SensitiveParamInfoDao.instance
            });
            put(CollectionType.Id_ApiCollectionId, new MCollection[] {
                    ApiInfoDao.instance,
                    TrafficInfoDao.instance,
                    SampleDataDao.instance,
                    SensitiveSampleDataDao.instance,
                    VulnerableRequestForTemplateDao.instance,
                    FilterSampleDataDao.instance
            });
            put(CollectionType.Id_ApiInfoKey_ApiCollectionId, new MCollection[] {
                    TestingRunIssuesDao.instance
            });
        }});

    public static Bson getApiFilters(CollectionType type, ApiInfoKey api) {

        String prefix = "";
        switch (type) {
            case Id_ApiCollectionId:
                prefix = "_id.";
                break;

            case Id_ApiInfoKey_ApiCollectionId:
                prefix = "_id.apiInfoKey.";
                break;

            case ApiCollectionId:
            default:
                break;
        }

        return Filters.and(
                Filters.eq(prefix + SingleTypeInfo._API_COLLECTION_ID, api.getApiCollectionId()),
                Filters.eq(prefix + SingleTypeInfo._METHOD, api.getMethod().toString()),
                Filters.eq(prefix + SingleTypeInfo._URL, api.getUrl()));

    }

    public static void addApisInCollectionsForCollectionId(Set<ApiInfoKey> apis, int apiCollectionId) {
        Bson update = Updates.addToSet(SingleTypeInfo._COLLECTION_IDS, apiCollectionId);
        updateCollectionsForCollectionId(apis, apiCollectionId, update);
    }

    public static void removeApisFromCollectionsForCollectionId(Set<ApiInfoKey> apis, int apiCollectionId) {
        Bson update = Updates.pull(SingleTypeInfo._COLLECTION_IDS, apiCollectionId);
        updateCollectionsForCollectionId(apis, apiCollectionId, update);
    }

    private static void updateCollectionsForCollectionId(Set<ApiInfoKey> apis, int apiCollectionId, Bson update) {
        int accountId = Context.accountId.get();
        executorService.schedule(new Runnable() {
            public void run() {
                Context.accountId.set(accountId);

                Map<CollectionType, List<Bson>> filtersMap = new HashMap<>();
                filtersMap.put(CollectionType.ApiCollectionId, new ArrayList<>());
                filtersMap.put(CollectionType.Id_ApiCollectionId, new ArrayList<>());
                filtersMap.put(CollectionType.Id_ApiInfoKey_ApiCollectionId, new ArrayList<>());

                for (ApiInfoKey api : apis) {
                    for(Map.Entry<CollectionType, List<Bson>> filters: filtersMap.entrySet()){
                        filters.getValue().add(getApiFilters(filters.getKey(), api));
                    }
                }

                Map<CollectionType, MCollection<?>[]> collectionsMap = COLLECTIONS_WITH_API_COLLECTION_ID;

                for (Map.Entry<CollectionType, List<Bson>> filters : filtersMap.entrySet()) {
                    Bson filter = Filters.or(filters.getValue());
                    updateCollections(collectionsMap.get(filters.getKey()), filter, update);
                }

            }
        }, 0, TimeUnit.SECONDS);
    }

    public static void updateCollections(MCollection<?>[] collections, Bson filter, Bson update) {
        for (MCollection<?> collection : collections) {
            collection.getMCollection().updateMany(filter, update);
        }
    }

    public static void updateCollections(MCollection<?>[] collections, Bson filter, List<Bson> update) {
        for (MCollection<?> collection : collections) {
            collection.getMCollection().updateMany(filter, update);
        }
    }

    public static void updateCollectionsInBatches(MCollection<?>[] collections, Bson filter, List<Bson> update) {
        for (MCollection<?> collection : collections) {
            updateCollectionInBatches(collection, filter, update);
        }
    }

    static final int UPDATE_LIMIT = 50;

    // NOTE: This update only works with collections which have ObjectId as id.
    private static void updateCollectionInBatches(MCollection<?> collection, Bson filter, List<Bson> update) {
        boolean doUpdate = true;
        int c = 0;
        int time = Context.now();
        while (doUpdate) {

            List<Bson> pipeline = new ArrayList<>();
            pipeline.add(Aggregates.match(filter));
            pipeline.add(Aggregates.project(Projections.include(Constants.ID)));
            pipeline.add(Aggregates.limit(UPDATE_LIMIT));

            MongoCursor<BasicDBObject> cursor = collection.getMCollection()
                    .aggregate(pipeline, BasicDBObject.class).cursor();

            ArrayList<ObjectId> ret = new ArrayList<>();

            while (cursor.hasNext()) {
                BasicDBObject elem = cursor.next();
                ret.add((ObjectId) elem.get(Constants.ID));
            }

            UpdateResult res = collection.getMCollection().updateMany(
                    Filters.in(Constants.ID, ret), update);

            if (res.getMatchedCount() == 0) {
                doUpdate = false;
            }
            c += Math.min(UPDATE_LIMIT , res.getModifiedCount());
            logger.info("updated " + c + " " + collection.getCollName());
        }
        logger.info("Total time taken : " + (Context.now() - time));
    }

}