package com.akto.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.akto.dao.APISpecDao;
import com.akto.dao.ApiCollectionsDao;
import com.akto.dao.MarkovDao;
import com.akto.dao.RelationshipDao;
import com.akto.dao.SensitiveParamInfoDao;
import com.akto.dao.SingleTypeInfoDao;
import com.akto.dao.context.Context;
import com.akto.dto.ApiCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.BasicDBObject;
import com.opensymphony.xwork2.Action;

public class ApiCollectionsAction extends UserAction {
    
    List<ApiCollection> apiCollections = new ArrayList<>();
    int apiCollectionId;

    public String fetchAllCollections() {
        this.apiCollections = ApiCollectionsDao.instance.findAll(new BasicDBObject());
        return Action.SUCCESS.toUpperCase();

    }

    private String collectionName;
    public String createCollection() {
        ApiCollection apiCollection = new ApiCollection(Context.now(), collectionName,Context.now(),new HashSet<>());
        ApiCollectionsDao.instance.insertOne(apiCollection);
        this.apiCollections = new ArrayList<>();
        this.apiCollections.add(apiCollection);
        return Action.SUCCESS.toUpperCase();
    }

    public String deleteCollection() {
        if(apiCollectionId == 0) {
            return Action.SUCCESS.toUpperCase();
        }
        ApiCollectionsDao.instance.deleteAll(Filters.eq("_id", apiCollectionId));
        SingleTypeInfoDao.instance.deleteAll(Filters.eq("apiCollectionId", apiCollectionId));
        APISpecDao.instance.deleteAll(Filters.eq("apiCollectionId", apiCollectionId));
        SensitiveParamInfoDao.instance.deleteAll(Filters.eq("apiCollectionId", apiCollectionId));
        // TODO : Markov and Relationship
        // MarkovDao.instance.deleteAll()
        // RelationshipDao.instance.deleteAll();
        return Action.SUCCESS.toUpperCase();
    } 

    public List<ApiCollection> getApiCollections() {
        return this.apiCollections;
    }

    public void setApiCollections(List<ApiCollection> apiCollections) {
        this.apiCollections = apiCollections;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getApiCollectionId() {
        return this.apiCollectionId;
    }
  
    public void setApiCollectionId(int apiCollectionId) {
        this.apiCollectionId = apiCollectionId;
    } 

}
