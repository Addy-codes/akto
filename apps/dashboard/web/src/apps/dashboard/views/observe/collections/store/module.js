import Vue from 'vue'
import Vuex from 'vuex'
import api from '../api'


Vue.use(Vuex)

var state = {
    loading: false,
    fetchTs: 0,
    apiCollections:[]
}

const collections = {
    namespaced: true,
    state: state,
    mutations: {
        EMPTY_STATE (state) {
            state.loading = false
            state.fetchTs = 0
            state.apiCollections = []
        },
        SAVE_API_COLLECTION (state, {apiCollectionResponse}) {
            state.apiCollections = apiCollectionResponse
        },
        CREATE_COLLECTION (state, {apiCollections}) {
            state.apiCollections.push(apiCollections[0])
        },
        DELETE_COLLECTION (state, apiCollectionId) {
            const index = state.apiCollections.findIndex(collection => collection.id === apiCollectionId)
            state.apiCollections.splice(index,1)
        }
    },
    actions: {
        emptyState({commit}, payload, options) {
            commit('EMPTY_STATE', payload, options)
        },
        loadAllApiCollections({commit}, options) {
            commit('EMPTY_STATE')
            state.loading = true
            return api.getAllCollections().then((resp) => {
                commit('SAVE_API_COLLECTION', resp, options)
                state.loading = false
            }).catch(() => {
                state.loading = false
            })
        },
        createCollection({commit}, {name, andConditions, orConditions}, options) {
            return api.createCollection(name, andConditions, orConditions).then((resp) => {
                commit('CREATE_COLLECTION', resp, options)
                window._AKTO.$emit('SHOW_SNACKBAR', {
                    show: true,
                    text: `${name} ` +`added successfully!`,
                    color: 'green'
                })
            }).catch((err) => {})
        },
        deleteCollection({commit}, apiCollection, options) {
            return api.deleteCollection(apiCollection.apiCollection.id, apiCollection.apiCollection.isLogicalGroup).then((resp) => {
                commit('DELETE_COLLECTION', apiCollection.apiCollection.id, options)
            }).catch((err) => {})
        }
    },
    getters: {
        getFetchTs: (state) => state.fetchTs,
        getLoading: (state) => state.loading,
        getAPICollections: (state) => state.apiCollections
    }
}

export default collections