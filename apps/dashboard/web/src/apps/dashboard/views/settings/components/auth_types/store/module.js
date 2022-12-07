import Vue from 'vue'
import Vuex from 'vuex'
import func from '@/util/func'
import api from '../api'

Vue.use(Vuex)

const auth_types = {
    namespaced: true,
    state: {
        auth_types: null,
        auth_type: null,
    },
    getters: {
        getAuthTypes: (state) => state.auth_types,
        getAuthType: (state) => state.auth_type
    },
    mutations: {
        SET_AUTH_TYPES(state, result) {
            state.auth_types = result["customAuthTypes"]
            state.usersMap = result["usersMap"]
            state.auth_types = func.prepareAuthTypes(state.auth_types)
            if (state.auth_types && state.auth_types.length > 0) {
                state.auth_type = state.auth_types[0]
            }
        },
        SET_NEW_AUTH_TYPE(state) {
            state.auth_type = {
                "name": "",
                "keyConditions": {"operator": "AND", "predicates": []},
                "active": true,
                "createNew": true
            }

        },
        UPDATE_AUTH_TYPES(state,result) {
            state.auth_types=func.prepareAuthTypes(result)
            state.auth_types.forEach((auth_type) => {
                if(auth_type["name"]==state.auth_type["name"]){
                    state.auth_type=auth_type
                }
            });
        }
    },
    actions: {
        setNewAuthType({commit, dispatch}) {
            commit("SET_NEW_AUTH_TYPE")
        },
        toggleActivateAuthType({commit, dispatch}, item) {
            return api.updateCustomAuthTypeStatus(item.name, !item.active).then((resp) => {
                commit("UPDATE_AUTH_TYPES", resp["customAuthTypes"]);
            })
        },
        fetchCustomAuthTypes({commit, dispatch}) {
            return api.fetchCustomAuthTypes().then((resp) => {
                commit('SET_AUTH_TYPES', resp)
            })
        },
        async createAuthType({commit, dispatch, state}, { auth_type, save}) {
            let name = auth_type["name"]
            let operator = auth_type["keyConditions"]["operator"]
            let keys = func.generateKeysForApi(auth_type["keyConditions"]["predicates"])
            let createNew = auth_type["createNew"] ? auth_type["createNew"] : false
            let active = auth_type["active"] ? auth_type["active"] : true 
            if (createNew) {
                return api.addCustomAuthType(name, operator, keys, active).then((resp) => {
                    commit("UPDATE_AUTH_TYPES", resp["customAuthTypes"]);
                })
            } else {
                return api.updateCustomAuthType(name,operator,keys,active).then((resp)=>{
                    commit("UPDATE_AUTH_TYPES", resp["customAuthTypes"]);
                })
            }
        },
    }
}

export default auth_types